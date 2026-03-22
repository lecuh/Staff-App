package com.example.myapplication.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.model.Reservation
import kotlinx.coroutines.launch

@Composable
fun ReservationScreen(token: String) {
    // Cập nhật: Đổi CHECKED_IN thành ARRIVED để khớp với Enum của Server
    val statuses = listOf("PENDING", "CONFIRMED", "ARRIVED")
    var selectedStatus by remember { mutableStateOf("PENDING") }
    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun fetchReservations() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.reservationService.getReservations("Bearer $token", selectedStatus)
                if (response.isSuccessful) {
                    reservations = response.body() ?: emptyList()
                } else {
                    errorMessage = "Failed to load reservations: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedStatus) {
        fetchReservations()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "RESERVATIONS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Status Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(statuses) { status ->
                    val isSelected = selectedStatus == status
                    val displayStatus = when(status) {
                        "ARRIVED" -> "CHECKED IN"
                        else -> status
                    }
                    
                    Button(
                        onClick = { selectedStatus = status },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF5F5F5),
                            contentColor = if (isSelected) Color.White else Color.Gray
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text(
                            text = displayStatus,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (errorMessage != null) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage!!, color = Color.Red)
                        Button(onClick = { fetchReservations() }, modifier = Modifier.padding(top = 8.dp)) { Text("Retry") }
                    }
                } else if (reservations.isEmpty()) {
                    Text("No reservations in $selectedStatus", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(reservations) { reservation ->
                            ReservationCard(
                                reservation = reservation,
                                onAction = { actionType ->
                                    scope.launch {
                                        try {
                                            val response = when (actionType) {
                                                "CONFIRM" -> RetrofitClient.reservationService.confirmReservation("Bearer $token", reservation.id)
                                                "CANCEL" -> RetrofitClient.reservationService.cancelReservation("Bearer $token", reservation.id)
                                                "CHECK_IN" -> RetrofitClient.reservationService.checkInReservation("Bearer $token", reservation.id)
                                                "NO_SHOW" -> RetrofitClient.reservationService.markNoShow("Bearer $token", reservation.id)
                                                else -> null
                                            }
                                            
                                            if (response?.isSuccessful == true) {
                                                snackbarHostState.showSnackbar("Success!")
                                                fetchReservations()
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to update status")
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Error: ${e.message}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReservationCard(reservation: Reservation, onAction: (String) -> Unit) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name and Call Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = reservation.customerName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = reservation.customerPhone,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${reservation.customerPhone}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray)

            // Info: Time, Size, Tables
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "TIME", value = reservation.reservationTime.substring(11, 16))
                InfoItem(label = "GUESTS", value = "${reservation.partySize}")
                InfoItem(label = "TABLES", value = reservation.tableNumbers.joinToString(", "))
            }

            if (!reservation.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "NOTE: ${reservation.note}",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp)).padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (reservation.status) {
                    "PENDING" -> {
                        OutlinedButton(
                            onClick = { onAction("CANCEL") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CANCEL", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = { onAction("CONFIRM") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CONFIRM", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    "CONFIRMED" -> {
                        OutlinedButton(
                            onClick = { onAction("NO_SHOW") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("NO SHOW", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = { onAction("CHECK_IN") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CHECK IN", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.DarkGray)
    }
}
