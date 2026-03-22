package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.model.OrderDetail
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun OrderScreen(token: String) {
    val statuses = listOf("PENDING", "PREPARING", "READY", "SERVED")
    var selectedStatus by remember { mutableStateOf("PENDING") }
    var orderItems by remember { mutableStateOf<List<OrderDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun fetchItems() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.orderService.getItemsByStatus("Bearer $token", selectedStatus)
                if (response.isSuccessful) {
                    orderItems = response.body()?.data ?: emptyList()
                } else {
                    errorMessage = "Failed to load orders: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedStatus) {
        fetchItems()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "ORDER MANAGEMENT",
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
                            text = status,
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
                        Button(onClick = { fetchItems() }) { Text("Retry") }
                    }
                } else if (orderItems.isEmpty()) {
                    Text("No items in $selectedStatus", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(orderItems) { item ->
                            OrderItemCard(
                                item = item,
                                onUpdateStatus = { newStatus ->
                                    scope.launch {
                                        try {
                                            // Format the status as a JSON string: "STATUS"
                                            val statusJson = "\"$newStatus\""
                                            // Use modern extension functions to avoid deprecation
                                            val body = statusJson.toRequestBody("application/json".toMediaTypeOrNull())
                                            
                                            val response = RetrofitClient.orderService.updateOrderStatus(
                                                "Bearer $token",
                                                item.id,
                                                body
                                            )
                                            
                                            if (response.isSuccessful) {
                                                snackbarHostState.showSnackbar("Item updated to $newStatus")
                                                fetchItems()
                                            } else {
                                                snackbarHostState.showSnackbar("Failed: ${response.code()}")
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
fun OrderItemCard(item: OrderDetail, onUpdateStatus: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Quantity: ${item.quantity}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    if (!item.notes.isNullOrBlank()) {
                        Text(
                            text = "Note: ${item.notes}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Text(
                    text = "${String.format("%,.0f", item.subtotal)}₫",
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = { onUpdateStatus("CANCEL") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("CANCEL", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }

                if (item.itemStatus == "READY") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onUpdateStatus("SERVED") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("SERVE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
