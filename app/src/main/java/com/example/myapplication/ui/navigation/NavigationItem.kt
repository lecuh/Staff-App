package com.example.myapplication.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(val route: String, val icon: ImageVector, val title: String) {
    object Table : NavigationItem("table", Icons.Default.List, "Tables")
    object Order : NavigationItem("order", Icons.Default.ShoppingCart, "Orders")
    object Reservation : NavigationItem("reservation", Icons.Default.DateRange, "Reservations")
}
