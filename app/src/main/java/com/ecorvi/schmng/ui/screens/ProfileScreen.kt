package com.ecorvi.schmng.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    personId: String,  // Firestore ID is a String
    personType: String
) {
    // State for editable fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var dateOfBirth by remember { mutableStateOf("") }
    var mobileNo by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // Fetch the Person data from Firestore when the screen is displayed
    LaunchedEffect(personId) {
        // Fetch the person from Firestore based on personType
        val personRef = if (personType == "student") {
            FirestoreDatabase.studentsCollection.document(personId)
        } else {
            FirestoreDatabase.teachersCollection.document(personId)
        }

        personRef.get().addOnSuccessListener { documentSnapshot: DocumentSnapshot ->
            if (documentSnapshot.exists()) {
                val person = documentSnapshot.toObject(Person::class.java)
                person?.let {
                    firstName = it.firstName
                    lastName = it.lastName
                    email = it.email
                    gender = it.gender
                    dateOfBirth = it.dateOfBirth
                    mobileNo = it.mobileNo
                    address = it.address
                }
            } else {
                // Handle document not found
                Toast.makeText(navController.context, "Person not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(navController.context, "Error fetching data", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${personType.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }} Profile") },
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

                // Update Button
                item {
                    Button(
                        onClick = {
                            val updatedPerson = Person(
                                id = personId.toInt().toString(), // Assuming personId is passed as String, convert it to Int
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                gender = gender,
                                dateOfBirth = dateOfBirth,
                                mobileNo = mobileNo,
                                address = address,
                                className = "Class 1", // You can modify this based on your requirements
                                sex = gender
                            )

                            // Update the person in Firestore
                            val personRef = if (personType == "student") {
                                FirestoreDatabase.studentsCollection.document(personId)
                            } else {
                                FirestoreDatabase.teachersCollection.document(personId)
                            }

                            personRef.set(updatedPerson)
                                .addOnSuccessListener {
                                    Toast.makeText(navController.context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack() // Go back to the previous screen
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(navController.context, "Failed to update profile: $e", Toast.LENGTH_SHORT).show()
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F41BB))
                    ) {
                        Text("Update", color = Color.White)
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    // Sample NavController for preview
    val navController = NavController(androidx.compose.ui.platform.LocalContext.current)

    ProfileScreen(navController, "1", "student")
}
