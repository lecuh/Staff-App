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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.example.myapplication.data.model.*
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
    
    // Merge Flow States
    var mergedTables by remember { mutableStateOf<List<RestaurantTable>>(emptyList()) }
    var orderStep by remember { mutableIntStateOf(1) } // 1: Table Confirm, 2: Party Size, 3: Menu
    var partySize by remember { mutableIntStateOf(1) }
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
                        // Standard selection: Treat as merge of 1 table
                        mergedTables = listOf(table)
                        orderStep = 2 // Skip Step 1
                        quickCartItems.clear()
                    },
                    onMergeOrder = { tables ->
                        mergedTables = tables
                        orderStep = 1
                        quickCartItems.clear()
                    }
                )
                NavigationItem.Order -> OrderScreen(token = token)
                NavigationItem.Reservation -> ReservationScreen(token = token)
            }

            // The Merge Order Modal (3 STEPS)
            if (mergedTables.isNotEmpty()) {
                ModalBottomSheet(
                    onDismissRequest = { mergedTables = emptyList() },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    dragHandle = null,
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    MergeOrderFlowContent(
                        tables = mergedTables,
                        step = orderStep,
                        partySize = partySize,
                        cartItems = quickCartItems,
                        token = token,
                        onNext = { orderStep++ },
                        onBack = { if (orderStep > 1) orderStep-- else mergedTables = emptyList() },
                        onUpdatePartySize = { partySize = it },
                        onAddToCart = { item ->
                            val existing = quickCartItems.find { it.menuItem.id == item.id }
                            if (existing != null) {
                                val index = quickCartItems.indexOf(existing)
                                quickCartItems[index] = existing.copy(quantity = existing.quantity + 1)
                            } else {
                                quickCartItems.add(CartItem(item))
                            }
                        },
                        onUpdateCartQuantity = { item, delta ->
                            val index = quickCartItems.indexOf(item)
                            if (index != -1) {
                                val newQty = item.quantity + delta
                                if (newQty > 0) quickCartItems[index] = item.copy(quantity = newQty)
                                else quickCartItems.removeAt(index)
                            }
                        },
                        onFinalize = {
                            scope.launch {
                                try {
                                    // 1. Create Bill for merged tables
                                    val billReq = CreateBillRequest(
                                        tableIds = mergedTables.map { it.id },
                                        partySize = partySize
                                    )
                                    val billRes = RetrofitClient.tableService.createBill("Bearer $token", billReq)
                                    
                                    if (billRes.isSuccessful) {
                                        val billId = billRes.body()?.data?.id ?: return@launch
                                        
                                        // 2. Send items to kitchen
                                        var success = true
                                        quickCartItems.forEach { cartItem ->
                                            val statusJson = "\"PREPARING\""
                                            val body = statusJson.toRequestBody("application/json".toMediaTypeOrNull())
                                            val orderRes = RetrofitClient.orderService.updateOrderStatus("Bearer $token", cartItem.menuItem.id, body)
                                            if (!orderRes.isSuccessful) success = false
                                        }
                                        
                                        if (success) {
                                            snackbarHostState.showSnackbar("Order sent & Bill #$billId created!")
                                            mergedTables = emptyList()
                                            selectedItem = NavigationItem.Table
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to create bill: ${billRes.code()}")
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
fun MergeOrderFlowContent(
    tables: List<RestaurantTable>,
    step: Int,
    partySize: Int,
    cartItems: List<CartItem>,
    token: String,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onUpdatePartySize: (Int) -> Unit,
    onAddToCart: (MenuItem) -> Unit,
    onUpdateCartQuantity: (CartItem, Int) -> Unit,
    onFinalize: () -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight(0.9f).fillMaxWidth()) {
        // Modal Header with Progress
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STEP $step OF 3",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when(step) {
                        1 -> "CONFIRM TABLES"
                        2 -> "PARTY SIZE"
                        else -> "SELECT MENU"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color(0xFFF5F5F5), CircleShape)
            ) {
                Icon(if (step == 1) Icons.Default.Close else Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                1 -> Step1ConfirmTables(tables, onNext)
                2 -> Step2PartySize(partySize, onUpdatePartySize, onNext)
                3 -> Step3MenuSelection(token, cartItems, onAddToCart, onUpdateCartQuantity, onFinalize)
            }
        }
    }
}

@Composable
fun Step1ConfirmTables(tables: List<RestaurantTable>, onNext: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
        Text("You are merging the following tables:", color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        tables.forEach { table ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                        Text(table.tableNumber, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Table ${table.tableNumber} - ${table.location}", fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("CONFIRM & NEXT", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun Step2PartySize(partySize: Int, onUpdate: (Int) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("How many guests?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = { if (partySize > 1) onUpdate(partySize - 1) },
                modifier = Modifier.size(64.dp)
            ) { Text("-", fontSize = 24.sp) }
            
            Text(
                text = partySize.toString(),
                modifier = Modifier.padding(horizontal = 48.dp),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            
            FilledIconButton(
                onClick = { onUpdate(partySize + 1) },
                modifier = Modifier.size(64.dp)
            ) { Text("+", fontSize = 24.sp) }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("NEXT: SELECT MENU", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun Step3MenuSelection(
    token: String,
    cartItems: List<CartItem>,
    onAddToCart: (MenuItem) -> Unit,
    onUpdateQty: (CartItem, Int) -> Unit,
    onFinalize: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1.2f)) {
            MenuScreen(token = token, selectedTableNumber = null, onAddToCart = onAddToCart)
        }
        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFFEEEEEE)))
        Column(modifier = Modifier.weight(0.8f).padding(16.dp)) {
            Text("CART SUMMARY", fontWeight = FontWeight.Black, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cartItems) { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.menuItem.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${String.format("%,.0f", item.menuItem.price)}₫", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("-", modifier = Modifier.clickable { onUpdateQty(item, -1) }.padding(8.dp))
                            Text(item.quantity.toString(), fontWeight = FontWeight.Black)
                            Text("+", modifier = Modifier.clickable { onUpdateQty(item, 1) }.padding(8.dp))
                        }
                    }
                }
            }
            if (cartItems.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                val total = cartItems.sumOf { it.menuItem.price * it.quantity }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", fontWeight = FontWeight.Bold)
                    Text("${String.format("%,.0f", total)}₫", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onFinalize, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("SEND TO KITCHEN", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
