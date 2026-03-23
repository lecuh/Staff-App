package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.model.Reservation
import com.example.myapplication.data.model.RestaurantTable
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TableScreen(
    token: String, 
    onTableSelected: (RestaurantTable) -> Unit,
    onMergeOrder: (List<RestaurantTable>) -> Unit
) {
    var tables by remember { mutableStateOf<List<RestaurantTable>>(emptyList()) }
    var activeReservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val selectedTableIds = remember { mutableStateListOf<Int>() }
    var showReservationModal by remember { mutableStateOf<Reservation?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun refreshData() {
        scope.launch {
            isLoading = true
            try {
                val tableResponse = RetrofitClient.tableService.getTables("Bearer $token")
                if (tableResponse.isSuccessful) {
                    tables = tableResponse.body()?.data ?: emptyList()
                }
                val pendingResponse = RetrofitClient.reservationService.getReservations("Bearer $token", "PENDING")
                val confirmedResponse = RetrofitClient.reservationService.getReservations("Bearer $token", "CONFIRMED")
                val combined = mutableListOf<Reservation>()
                pendingResponse.body()?.let { combined.addAll(it) }
                confirmedResponse.body()?.let { combined.addAll(it) }
                activeReservations = combined
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { refreshData() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = StaffLightGray
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(
                    text = "FLOOR STATUS",
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
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(tables) { table ->
                        val isSelected = selectedTableIds.contains(table.id)
                        val reservation = activeReservations.find { it.tableNumbers.contains(table.tableNumber) }
                        
                        TableItem(
                            table = table, 
                            isSelected = isSelected,
                            reservation = reservation,
                            onClick = {
                                if (table.status == "RESERVED" && reservation != null) {
                                    showReservationModal = reservation
                                } else if (selectedTableIds.isEmpty()) {
                                    onTableSelected(table)
                                } else {
                                    if (isSelected) selectedTableIds.remove(table.id)
                                    else selectedTableIds.add(table.id)
                                }
                            },
                            onLongClick = {
                                if (table.status != "RESERVED") {
                                    if (isSelected) selectedTableIds.remove(table.id)
                                    else selectedTableIds.add(table.id)
                                }
                            }
                        )
                    }
                }
            }

            if (selectedTableIds.isNotEmpty()) {
                Button(
                    onClick = {
                        val selectedTables = tables.filter { selectedTableIds.contains(it.id) }
                        onMergeOrder(selectedTables)
                        selectedTableIds.clear()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StaffDarkGray),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = if (selectedTableIds.size == 1) "PROCEED WITH TABLE ${tables.find { it.id == selectedTableIds.first() }?.tableNumber}" 
                               else "MERGE & ORDER (${selectedTableIds.size} TABLES)", 
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        showReservationModal?.let { reservation ->
            ReservationDetailsModal(
                reservation = reservation,
                onConfirm = {
                    scope.launch {
                        try {
                            val response = RetrofitClient.reservationService.confirmReservation("Bearer $token", reservation.id)
                            if (response.isSuccessful) {
                                snackbarHostState.showSnackbar("Confirmed")
                                showReservationModal = null
                                refreshData()
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error")
                        }
                    }
                },
                onCheckIn = {
                    scope.launch {
                        try {
                            val response = RetrofitClient.reservationService.checkInReservation("Bearer $token", reservation.id)
                            if (response.isSuccessful) {
                                snackbarHostState.showSnackbar("Checked In")
                                showReservationModal = null
                                refreshData()
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error")
                        }
                    }
                },
                onNoShow = {
                    scope.launch {
                        try {
                            val response = RetrofitClient.reservationService.markNoShow("Bearer $token", reservation.id)
                            if (response.isSuccessful) {
                                snackbarHostState.showSnackbar("No-Show")
                                showReservationModal = null
                                refreshData()
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error")
                        }
                    }
                },
                onCancel = {
                    scope.launch {
                        try {
                            val response = RetrofitClient.reservationService.cancelReservation("Bearer $token", reservation.id)
                            if (response.isSuccessful) {
                                snackbarHostState.showSnackbar("Cancelled")
                                showReservationModal = null
                                refreshData()
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error")
                        }
                    }
                },
                onDismiss = { showReservationModal = null }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableItem(
    table: RestaurantTable, 
    isSelected: Boolean, 
    reservation: Reservation? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val statusColor = when (table.status) {
        "OPEN", "AVAILABLE" -> StaffOlive
        "OCCUPIED" -> StaffBurgundy
        "RESERVED" -> StaffOrange
        else -> StaffTextGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(40.dp), // rounded-[2.5rem]
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) StaffDarkGray.copy(alpha = 0.05f) else StaffWhite
        ),
        border = if (isSelected) BorderStroke(3.dp, StaffDarkGray) else BorderStroke(1.dp, StaffMediumGray.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 12.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TABLE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = StaffTextGray,
                    letterSpacing = 2.sp
                )
                Text(
                    text = table.tableNumber,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = if (table.status == "AVAILABLE") StaffMediumGray else statusColor,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (table.status == "RESERVED" && reservation != null) {
                    Text(
                        text = reservation.reservationTime.substring(11, 16),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = StaffDarkGray
                    )
                    Text(
                        text = "${reservation.partySize} GUESTS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaffTextGray
                    )
                } else if (table.status == "AVAILABLE") {
                    Text(
                        text = "TAP TO INTERACT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = StaffTextGray.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = table.status,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ReservationDetailsModal(
    reservation: Reservation,
    onConfirm: () -> Unit,
    onCheckIn: () -> Unit,
    onNoShow: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.9f),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "TABLE #${reservation.tableNumbers.joinToString(", ")}", fontWeight = FontWeight.Black, fontSize = 28.sp, letterSpacing = (-1).sp)
                    Text(text = "RESERVATION DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StaffTextGray, letterSpacing = 2.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = StaffTextGray)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailRowStaff("Customer", reservation.customerName.uppercase())
                DetailRowStaff("Phone", reservation.customerPhone)
                DetailRowStaff("Time", reservation.reservationTime.replace("T", " "))
                DetailRowStaff("Guests", "${reservation.partySize}")
                DetailRowStaff("Status", reservation.status)
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (reservation.status == "PENDING") {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StaffOlive)
                    ) {
                        Text("CONFIRM RESERVATION", fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (reservation.canMarkNoShow) {
                        Button(
                            onClick = onNoShow,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StaffLightGray, contentColor = StaffTextGray)
                        ) {
                            Text("NO-SHOW", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                    if (reservation.canCancel) {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StaffBurgundy)
                        ) {
                            Text("CANCEL", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(40.dp),
        containerColor = StaffWhite
    )
}

@Composable
fun DetailRowStaff(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label.uppercase(), fontWeight = FontWeight.Black, fontSize = 9.sp, color = StaffTextGray, letterSpacing = 2.sp)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = StaffDarkGray)
    }
}
