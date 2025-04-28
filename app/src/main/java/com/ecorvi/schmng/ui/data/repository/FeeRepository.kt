package com.ecorvi.schmng.ui.data.repository

import com.ecorvi.schmng.ui.data.model.Fee
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

interface IFeeRepository {
    fun addFee(fee: Fee): Task<DocumentReference>
    fun fetchFees(): Task<QuerySnapshot>
}

class FeeRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) : IFeeRepository {
    override fun addFee(fee: Fee): Task<DocumentReference> =
        db.collection("fees").add(fee)

    override fun fetchFees(): Task<QuerySnapshot> =
        db.collection("fees").get()
} 