package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.model.CartItem
import com.example.myapplication.data.model.MenuItem
import com.example.myapplication.data.model.RestaurantTable
import com.example.myapplication.ui.navigation.NavigationItem
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var loginData by remember { mutableStateOf<Pair<String, String>?>(null) }

                if (loginData == null) {
                    LoginScreen(onLoginSuccess = { name, token ->
                        loginData = Pair(name, token)
                    })
                } else {
                    MainAppScaffold(
                        userName = loginData!!.first,
                        token = loginData!!.second,
                        onLogout = { loginData = null }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(userName: String, token: String, onLogout: () -> Unit) {
    var selectedItem by remember { mutableStateOf<NavigationItem>(NavigationItem.Table) }
    var activeTableForMenu by remember { mutableStateOf<RestaurantTable?>(null) }
    
    val quickCartItems = remember { mutableStateListOf<CartItem>() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CULINA - ${selectedItem.title.uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    NavigationItem.Table,
                    NavigationItem.Order,
                    NavigationItem.Reservation
                )
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedItem == item,
                        onClick = { selectedItem = item },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedItem) {
                NavigationItem.Table -> TableScreen(
                    token = token,
                    onTableSelected = { table ->
                        quickCartItems.clear()
                        activeTableForMenu = table
                    }
                )
                NavigationItem.Order -> OrderScreen(token = token)
                NavigationItem.Reservation -> ReservationScreen(token = token)
            }

            if (activeTableForMenu != null) {
                ModalBottomSheet(
                    onDismissRequest = { activeTableForMenu = null },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    dragHandle = null,
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    QuickMenuContent(
                        table = activeTableForMenu!!,
                        token = token,
                        cartItems = quickCartItems,
                        onAddToCart = { item ->
                            val existing = quickCartItems.find { it.menuItem.id == item.id }
                            if (existing != null) {
                                val index = quickCartItems.indexOf(existing)
                                quickCartItems[index] = existing.copy(quantity = existing.quantity + 1)
                            } else {
                                quickCartItems.add(CartItem(item))
                            }
                        },
                        onUpdateQuantity = { item, delta ->
                            val index = quickCartItems.indexOf(item)
                            if (index != -1) {
                                val newQty = item.quantity + delta
                                if (newQty > 0) {
                                    quickCartItems[index] = item.copy(quantity = newQty)
                                } else {
                                    quickCartItems.removeAt(index)
                                }
                            }
                        },
                        onClose = { activeTableForMenu = null },
                        onSendToKitchen = {
                            scope.launch {
                                try {
                                    var itemsSuccess = 0
                                    quickCartItems.forEach { cartItem ->
                                        val statusJson = "\"PREPARING\""
                                        val body = statusJson.toRequestBody("application/json".toMediaTypeOrNull())
                                        
                                        val response = RetrofitClient.orderService.updateOrderStatus(
                                            "Bearer $token",
                                            cartItem.menuItem.id,
                                            body
                                        )
                                        if (response.isSuccessful) itemsSuccess++
                                    }
                                    
                                    if (itemsSuccess > 0) {
                                        RetrofitClient.tableService.updateTableStatus(
                                            "Bearer $token",
                                            activeTableForMenu!!.id,
                                            "OCCUPIED"
                                        )
                                        snackbarHostState.showSnackbar("Table ${activeTableForMenu!!.tableNumber} sent to kitchen!")
                                        activeTableForMenu = null
                                        quickCartItems.clear()
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

@Composable
fun QuickMenuContent(
    table: RestaurantTable,
    token: String,
    cartItems: List<CartItem>,
    onAddToCart: (MenuItem) -> Unit,
    onUpdateQuantity: (CartItem, Int) -> Unit,
    onClose: () -> Unit,
    onSendToKitchen: () -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight(0.9f).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TABLE ${table.tableNumber}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(table.location, fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(Color(0xFFF5F5F5), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1.2f)) {
                MenuScreen(
                    token = token,
                    selectedTableNumber = null,
                    onAddToCart = onAddToCart
                )
            }

            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFFEEEEEE)))

            Column(
                modifier = Modifier.weight(0.8f).fillMaxHeight().padding(16.dp)
            ) {
                Text(
                    text = "DRAFT ORDER",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cartItems) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(item.menuItem.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${String.format("%,.0f", item.menuItem.price)}₫", fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "-",
                                        modifier = Modifier.clickable { onUpdateQuantity(item, -1) }.padding(8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(item.quantity.toString(), fontWeight = FontWeight.Black)
                                    Text(
                                        "+",
                                        modifier = Modifier.clickable { onUpdateQuantity(item, 1) }.padding(8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (cartItems.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    val total = cartItems.sumOf { it.menuItem.price * it.quantity }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL", fontWeight = FontWeight.Bold)
                        Text("${String.format("%,.0f", total)}₫", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onSendToKitchen,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("SEND TO KITCHEN", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
