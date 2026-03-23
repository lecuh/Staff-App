package com.example.myapplication.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ReservationScreen(token: String) {
    val statuses = listOf("PENDING", "CONFIRMED", "CANCELLED", "NO_SHOW")
    var selectedStatus by remember { mutableStateOf("PENDING") }
    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var selectedReservation by remember { mutableStateOf<Reservation?>(null) }
    
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
                    errorMessage = "Failed to load: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedStatus) { fetchReservations() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = StaffLightGray
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text(
                text = "RESERVATIONS",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = StaffDarkGray
            )
            Text(
                text = "STAFF EDITION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = StaffCheese
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(statuses) { status ->
                    val isSelected = selectedStatus == status
                    val displayStatus = if (status == "NO_SHOW") "NO SHOW" else status
                    
                    Surface(
                        onClick = { selectedStatus = status },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) StaffDarkGray else Color.Transparent,
                        contentColor = if (isSelected) Color.White else StaffTextGray,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = StaffBurgundy)
                } else if (errorMessage != null) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage!!, color = StaffBurgundy, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { fetchReservations() }, 
                            colors = ButtonDefaults.buttonColors(containerColor = StaffDarkGray),
                            modifier = Modifier.padding(top = 16.dp)
                        ) { Text("RETRY", fontWeight = FontWeight.Black) }
                    }
                } else if (reservations.isEmpty()) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(64.dp), tint = StaffMediumGray)
                        Spacer(Modifier.height(16.dp))
                        Text("No $selectedStatus reservations", color = StaffTextGray, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(reservations) { reservation ->
                            ReservationCard(
                                reservation = reservation,
                                onClick = { selectedReservation = reservation }
                            )
                        }
                    }
                }
            }
        }

        selectedReservation?.let { reservation ->
            ReservationDetailsModal(
                reservation = reservation,
                onDismiss = { selectedReservation = null },
                onAction = { actionType ->
                    scope.launch {
                        try {
                            val response = when (actionType) {
                                "CONFIRM" -> RetrofitClient.reservationService.confirmReservation("Bearer $token", reservation.id)
                                "CANCEL" -> RetrofitClient.reservationService.cancelReservation("Bearer $token", reservation.id)
                                "NO_SHOW" -> RetrofitClient.reservationService.markNoShow("Bearer $token", reservation.id)
                                else -> null
                            }
                            if (response?.isSuccessful == true) {
                                snackbarHostState.showSnackbar("Success")
                                selectedReservation = null
                                fetchReservations()
                            } else {
                                snackbarHostState.showSnackbar("Failed")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ReservationCard(reservation: Reservation, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StaffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val time = try { reservation.reservationTime.substring(11, 16) } catch (e: Exception) { "??:??" }
                Text(text = time, fontWeight = FontWeight.Black, fontSize = 20.sp, color = StaffDarkGray)
                Text(text = "TIME", fontSize = 9.sp, fontWeight = FontWeight.Black, color = StaffMediumGray, letterSpacing = 2.sp)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(StaffMediumGray.copy(alpha = 0.5f)))
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reservation.customerName.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Black, color = StaffDarkGray)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Person, null, Modifier.size(12.dp), StaffTextGray)
                    Text(" ${reservation.partySize} GUESTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StaffTextGray)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.Place, null, Modifier.size(12.dp), StaffTextGray)
                    Text(" TABLE ${reservation.tableNumbers.joinToString(", ")}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StaffTextGray)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = StaffMediumGray)
        }
    }
}

@Composable
fun ReservationDetailsModal(
    reservation: Reservation,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("TABLE #${reservation.tableNumbers.joinToString(", ")}", fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text("RESERVATION DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StaffTextGray, letterSpacing = 2.sp)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = StaffTextGray) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().background(StaffLightGray, RoundedCornerShape(16.dp)).padding(16.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(reservation.customerName.uppercase(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = StaffDarkGray)
                        Text(reservation.customerPhone, color = StaffTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${reservation.customerPhone}") }
                        context.startActivity(intent)
                    }, shape = CircleShape, color = StaffWhite, shadowElevation = 2.dp) {
                        Icon(Icons.Default.Call, null, Modifier.padding(12.dp), StaffDarkGray)
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) { DetailGridItemStaff("TIME", reservation.reservationTime.replace("T", " ")) }
                    Box(Modifier.weight(1f)) { DetailGridItemStaff("GUESTS", "${reservation.partySize}") }
                }
                DetailGridItemStaff("STATUS", reservation.status)
                if (reservation.depositRequired) {
                    Surface(color = if (reservation.depositPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp),
                            Alignment.CenterVertically as Arrangement.Horizontal
                        ) {
                            Icon(if (reservation.depositPaid) Icons.Default.CheckCircle else Icons.Default.Info, null, Modifier.size(18.dp), if (reservation.depositPaid) StaffOlive else StaffOrange)
                            Spacer(Modifier.width(8.dp))
                            Text(if (reservation.depositPaid) "DEPOSIT PAID: $${reservation.depositAmount}" else "DEPOSIT REQUIRED: $${reservation.depositAmount}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (reservation.depositPaid) StaffOlive else StaffOrange)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), Arrangement.spacedBy(12.dp)) {
                if (reservation.status == "PENDING") {
                    Button(onClick = { onAction("CONFIRM") }, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(StaffOlive)) {
                        Text("CONFIRM RESERVATION", fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    if (reservation.canMarkNoShow) {
                        Button(onClick = { onAction("NO_SHOW") }, Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(StaffLightGray, StaffTextGray)) {
                            Text("NO-SHOW", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    if (reservation.canCancel) {
                        Button(onClick = { onAction("CANCEL") }, Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(StaffBurgundy)) {
                            Text("CANCEL", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = StaffWhite
    )
}

@Composable
fun DetailGridItemStaff(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = StaffTextGray, letterSpacing = 2.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = StaffDarkGray)
    }
}
