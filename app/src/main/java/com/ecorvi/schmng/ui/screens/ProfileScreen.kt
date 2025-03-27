package com.ecorvi.schmng.ui.screens

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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.InMemoryDatabase
import com.ecorvi.schmng.ui.data.model.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    personId: Int,
    personType: String
) {
    // Find the person from the list
    val personList: MutableList<Person> = if (personType == "student") InMemoryDatabase.studentsList else InMemoryDatabase.teachersList
    val person = personList.find { it.id == personId } ?: Person(
        id = 0,
        firstName = "",
        lastName = "",
        email = "",
        gender = "",
        dateOfBirth = "",
        mobileNo = "",
        address = "",
        className = "",
        sex = ""
    )

    // State for editable fields
    var firstName by remember { mutableStateOf<String>(person.firstName) }
    var lastName by remember { mutableStateOf<String>(person.lastName) }
    var email by remember { mutableStateOf<String>(person.email) }
    var gender by remember { mutableStateOf<String>(person.gender) }
    var dateOfBirth by remember { mutableStateOf<String>(person.dateOfBirth) }
    var mobileNo by remember { mutableStateOf<String>(person.mobileNo) }
    var address by remember { mutableStateOf<String>(person.address) }

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
                            // Update the person in the list
                            val updatedPerson = person.copy(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                gender = gender,
                                dateOfBirth = dateOfBirth,
                                mobileNo = mobileNo,
                                address = address
                            )
                            if (personType == "student") {
                                val index = InMemoryDatabase.studentsList.indexOf(person)
                                if (index != -1) InMemoryDatabase.studentsList[index] = updatedPerson
                            } else {
                                val index = InMemoryDatabase.teachersList.indexOf(person)
                                if (index != -1) InMemoryDatabase.teachersList[index] = updatedPerson
                            }
                            navController.popBackStack()
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
fun ProfileScreenPreview(){
    // Sample NavController for preview
    val navController = NavController(androidx.compose.ui.platform.LocalContext.current)

    ProfileScreen(navController, 1,"student")
}
