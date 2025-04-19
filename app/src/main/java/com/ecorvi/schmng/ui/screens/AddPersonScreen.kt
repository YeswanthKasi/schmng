package com.ecorvi.schmng.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonScreen(
    navController: NavController,
    personType: String
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var dateOfBirth by remember { mutableStateOf("") }
    var mobileNo by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(Constants.CLASS_OPTIONS.first()) }
    var showClassDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add ${personType.capitalize()}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture Placeholder
                item {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color(0xFF1F41BB), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Picture",
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                // First Name and Last Name
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Email
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email ID") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                // Gender
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("Gender", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = gender == "Male",
                                    onClick = { gender = "Male" }
                                )
                                Text("Male")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = gender == "Female",
                                    onClick = { gender = "Female" }
                                )
                                Text("Female")
                            }
                        }
                    }
                }

                // Date of Birth
                item {
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = { dateOfBirth = it },
                        label = { Text("Date of Birth (mm/dd/yyyy)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        trailingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    )
                }

                // Mobile No
                item {
                    OutlinedTextField(
                        value = mobileNo,
                        onValueChange = { mobileNo = it },
                        label = { Text("Mobile No") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                // Address
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                // Add class selection
                item {
                    OutlinedTextField(
                        value = selectedClass,
                        onValueChange = { },
                        label = { Text("Class") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(56.dp),
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, "Select Class")
                        }
                    )
                }

                if (showClassDialog) {
                    item {
                        AlertDialog(
                            onDismissRequest = { showClassDialog = false },
                            title = { Text("Select Class") },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Constants.CLASS_OPTIONS.forEach { classOption ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedClass = classOption
                                                    showClassDialog = false
                                                }
                                                .padding(vertical = 12.dp, horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedClass == classOption,
                                                onClick = {
                                                    selectedClass = classOption
                                                    showClassDialog = false
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(classOption)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showClassDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }

                // Save Button
                item {
                    Button(
                        onClick = {
                            val person = Person(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                gender = gender,
                                dateOfBirth = dateOfBirth,
                                mobileNo = mobileNo,
                                address = address,
                                className = selectedClass
                            )

                            if (personType == "student") {
                                FirestoreDatabase.addStudent(
                                    person,
                                    onSuccess = {
                                        Toast.makeText(context, "Student added successfully", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                FirestoreDatabase.addTeacher(
                                    person,
                                    onSuccess = {
                                        Toast.makeText(context, "Teacher added successfully", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F41BB))
                    ) {
                        Text("Add ${personType.capitalize()}", color = Color.White)
                    }
                }
            }
        }
    )
}

@Preview
@Composable
fun PreviewAddPersonScreen() {
    val navController = rememberNavController()
    AddPersonScreen(navController, "student")
}
