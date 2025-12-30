package com.ecorvi.schmng.ui.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecorvi.schmng.ui.data.model.ExamTypes
import com.ecorvi.schmng.ui.data.model.GradeCalculator
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.model.StandardSubjects
import com.ecorvi.schmng.ui.data.model.SubjectGrade
import com.ecorvi.schmng.ui.data.model.StudentGrade
import com.ecorvi.schmng.ui.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GradesViewModel(
    private val repo: GradesRepository = GradesRepository()
) : ViewModel() {
    private fun normalizeClassName(raw: String): String {
        val t = raw.trim()
        if (t.startsWith("Class ")) return t
        val num = t.replace(Regex("[^0-9]"), "")
        return if (num.isNotBlank()) "Class $num" else "Class $t"
    }

    private val _students = MutableStateFlow<List<Person>>(emptyList())
    val students: StateFlow<List<Person>> = _students

    private val _existing = MutableStateFlow<Map<String, StudentGrade>>(emptyMap())
    val existing: StateFlow<Map<String, StudentGrade>> = _existing

    private val _subjects = MutableStateFlow<Set<String>>(emptySet())
    val subjects: StateFlow<Set<String>> = _subjects

    private val _gradeBaseline = MutableStateFlow<Map<String, MutableMap<String, String>>>(emptyMap())
    val gradeBaseline: StateFlow<Map<String, MutableMap<String, String>>> = _gradeBaseline

    private val _dirty = MutableStateFlow<Map<String, MutableMap<String, String>>>(emptyMap())
    val dirty: StateFlow<Map<String, MutableMap<String, String>>> = _dirty

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedClass = MutableStateFlow(Constants.CLASS_OPTIONS.first())
    val selectedClass: StateFlow<String> = _selectedClass

    private val _examType = MutableStateFlow(ExamTypes.FA1)
    val examType: StateFlow<String> = _examType

    private val _academicYear = MutableStateFlow("2025-26")
    val academicYear: StateFlow<String> = _academicYear

    private val _examDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    val examDate: StateFlow<String> = _examDate

    private var listener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun defaultSubjectsForClass(cls: String): List<String> {
        return when {
            cls.contains("1") || cls.contains("2") || cls.contains("3") || cls.contains("4") || cls.contains("5") -> StandardSubjects.PRIMARY
            cls.contains("6") || cls.contains("7") || cls.contains("8") || cls.contains("9") -> StandardSubjects.SECONDARY
            else -> StandardSubjects.HIGHER_SECONDARY
        }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val cls = normalizeClassName(_selectedClass.value)
            val people = repo.listStudents(cls).sortedBy { it.rollNumber.toIntOrNull() ?: 999 }
            _students.value = people
            val grades = repo.listGrades(cls, _examType.value, _academicYear.value)
            _existing.value = grades.associateBy { it.studentId }
            val defaultSubjects = defaultSubjectsForClass(cls).toSet()
            val cloudSubjects = grades.flatMap { it.subjects.keys }.toSet()
            val union = _subjects.value.union(defaultSubjects).union(cloudSubjects)
            _subjects.value = union
            val base = mutableMapOf<String, MutableMap<String, String>>()
            people.forEach { st ->
                val row = mutableMapOf<String, String>()
                _subjects.value.forEach { subj ->
                    val g = _existing.value[st.id]
                    val m = g?.subjects?.get(subj)?.obtainedMarks
                    row[subj] = if (m != null) { if (m % 1.0 == 0.0) m.toInt().toString() else m.toString() } else ""
                }
                base[st.id] = row
            }
            _gradeBaseline.value = base
            _dirty.value = emptyMap()
            _isLoading.value = false
            attachListener()
        }
    }

    fun setClass(value: String) {
        _selectedClass.value = normalizeClassName(value)
        _subjects.value = defaultSubjectsForClass(_selectedClass.value).toSet()
        load()
    }

    fun setExamType(value: String) {
        _examType.value = value
        load()
    }

    fun setAcademicYear(value: String) {
        _academicYear.value = value
    }

    fun setExamDate(value: String) {
        _examDate.value = value
    }

    fun addSubject(subject: String) {
        val cleaned = subject.trim()
        if (cleaned.isNotBlank()) {
            _subjects.value = _subjects.value + cleaned
            val updated = _gradeBaseline.value.toMutableMap()
            _students.value.forEach { st ->
                val row = updated[st.id]?.toMutableMap() ?: mutableMapOf()
                if (!row.containsKey(cleaned)) row[cleaned] = ""
                updated[st.id] = row
            }
            _gradeBaseline.value = updated
        }
    }

    fun applyFilters() {
        load()
    }

    fun updateMark(studentId: String, subject: String, value: String) {
        val m = _dirty.value.toMutableMap()
        val row = m[studentId]?.toMutableMap() ?: mutableMapOf()
        row[subject] = value
        m[studentId] = row
        _dirty.value = m
    }

    suspend fun deleteGrade(studentId: String): Result<Unit> {
        val existing = _existing.value[studentId] ?: return Result.failure(IllegalStateException("No saved grade"))
        val res = repo.deleteGrade(existing.id)
        if (res.isSuccess) {
            val newExisting = _existing.value.toMutableMap(); newExisting.remove(studentId); _existing.value = newExisting
            val updatedBaseline = _gradeBaseline.value.toMutableMap()
            updatedBaseline[studentId] = _subjects.value.associateWith { "" }.toMutableMap()
            _gradeBaseline.value = updatedBaseline
            val newDirty = _dirty.value.toMutableMap(); newDirty.remove(studentId); _dirty.value = newDirty
        }
        return res
    }

    private fun attachListener() {
        listener?.remove()
        listener = repo.listenGrades(
            normalizeClassName(_selectedClass.value), _examType.value, _academicYear.value,
            onUpdate = { grades ->
                _existing.value = grades.associateBy { it.studentId }
                val cloudSubjects = grades.flatMap { it.subjects.keys }.toSet()
                val union = _subjects.value.union(cloudSubjects)
                _subjects.value = union
                val updated = _gradeBaseline.value.toMutableMap()
                _students.value.forEach { st ->
                    val row = updated[st.id]?.toMutableMap() ?: mutableMapOf()
                    union.forEach { subj ->
                        if (_dirty.value[st.id]?.containsKey(subj) != true) {
                            val m = _existing.value[st.id]?.subjects?.get(subj)?.obtainedMarks
                            row[subj] = if (m != null) { if (m % 1.0 == 0.0) m.toInt().toString() else m.toString() } else ""
                        }
                    }
                    updated[st.id] = row
                }
                _gradeBaseline.value = updated
            },
            onError = { /* swallow to keep UI responsive */ }
        )
    }

    fun buildGradesForSave(): List<StudentGrade> {
        val out = mutableListOf<StudentGrade>()
        _students.value.forEach { st ->
            val subjectGrades = mutableMapOf<String, SubjectGrade>()
            var total = 0.0; var obtained = 0.0
            _subjects.value.forEach { subj ->
                val raw = effectiveMarks(st.id, subj).trim()
                val v = raw.toDoubleOrNull()
                if (v != null && v in 0.0..100.0) {
                    total += 100.0; obtained += v
                    subjectGrades[subj] = SubjectGrade(
                        subjectName = subj, maxMarks = 100.0, obtainedMarks = v,
                        grade = GradeCalculator.calculateGrade(v), remarks = ""
                    )
                }
            }
            if (subjectGrades.isNotEmpty()) {
                val pct = if (total > 0) (obtained / total) * 100 else 0.0
                val overallGrade = GradeCalculator.calculateGrade(pct)
                val existing = _existing.value[st.id]
                out.add(
                    StudentGrade(
                        id = existing?.id ?: "",
                        studentId = st.id,
                        studentName = "${st.firstName} ${st.lastName}",
                        className = normalizeClassName(_selectedClass.value),
                        academicYear = _academicYear.value,
                        examType = _examType.value,
                        examDate = _examDate.value,
                        subjects = subjectGrades,
                        totalMarks = total,
                        obtainedMarks = obtained,
                        percentage = pct,
                        grade = overallGrade,
                        createdBy = "",
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                )
            }
        }
        return out
    }

    fun effectiveMarks(studentId: String, subject: String): String {
        val d = _dirty.value[studentId]?.get(subject)
        return d ?: _gradeBaseline.value[studentId]?.get(subject) ?: ""
    }

    suspend fun save(createdBy: String): Result<Unit> {
        val toSave = buildGradesForSave()
        if (toSave.isEmpty()) return Result.failure(IllegalStateException("No valid grades"))
        val res = repo.saveGradesBatch(toSave, createdBy)
        if (res.isSuccess) {
            _dirty.value = emptyMap()
        }
        return res
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
