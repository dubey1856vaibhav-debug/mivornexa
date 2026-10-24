package com.example.bizflow.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object WorkOrders : Screen("work_orders")
    object Customers : Screen("customers")
    object Finance : Screen("finance")
    object Inventory : Screen("inventory")
    object Tasks : Screen("tasks")
    object Shopify : Screen("shopify")
    object AiAutomation : Screen("ai_automation")
    object CustomerPortal : Screen("customer_portal")
    object GlobalSearch : Screen("global_search")
    object FirebaseConnection : Screen("firebase_connection")
    object SettingsAndMore : Screen("settings_and_more")
}
