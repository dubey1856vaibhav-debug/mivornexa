package com.example.bizflow.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી")
}

object LanguageManager {
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)

    fun t(key: String): String {
        return when (currentLanguage) {
            AppLanguage.ENGLISH -> englishMap[key] ?: key
            AppLanguage.HINDI -> hindiMap[key] ?: englishMap[key] ?: key
            AppLanguage.GUJARATI -> gujaratiMap[key] ?: englishMap[key] ?: key
        }
    }

    private val englishMap = mapOf(
        "nav_dashboard" to "Dashboard",
        "nav_work" to "Work",
        "nav_customers" to "Customers",
        "nav_finance" to "Finance",
        "nav_more" to "More",
        "quick_add_customer" to "Add Customer",
        "quick_new_order" to "New Order",
        "quick_record_payment" to "Record Payment",
        "quick_add_expense" to "Add Expense",
        "quick_add_task" to "Add Task",
        "stat_revenue" to "Revenue (Paid)",
        "stat_receivables" to "Receivables Due",
        "stat_expenses" to "Total Expenses",
        "stat_active_orders" to "Active Orders",
        "workspace_switch" to "Switch Workspace",
        "sync_synced" to "Synced with BizFlow",
        "sync_pending" to "Pending Sync",
        "search_hint" to "Search records, clients, invoices...",
        "inventory_catalog" to "Catalog & Stock",
        "team_tasks" to "Team & Tasks",
        "free_subscription" to "4-Year Free SaaS Tier Active",
        "cloud_sync_desc" to "Universal business operations synced safely.",
        "advance_paid" to "Advance Paid",
        "balance_due" to "Balance Due",
        "duplicate_warning" to "Potential Duplicate Detected"
    )

    private val hindiMap = mapOf(
        "nav_dashboard" to "डैशबोर्ड",
        "nav_work" to "कार्य एवं ऑर्डर्स",
        "nav_customers" to "ग्राहक",
        "nav_finance" to "वित्त एवं बिलिंग",
        "nav_more" to "अधिक",
        "quick_add_customer" to "ग्राहक जोड़ें",
        "quick_new_order" to "नया ऑर्डर",
        "quick_record_payment" to "भुगतान दर्ज करें",
        "quick_add_expense" to "खर्च जोड़ें",
        "quick_add_task" to "कार्य जोड़ें",
        "stat_revenue" to "प्राप्त आय",
        "stat_receivables" to "बकाया राशि",
        "stat_expenses" to "कुल खर्च",
        "stat_active_orders" to "सक्रिय ऑर्डर्स",
        "workspace_switch" to "कार्यक्षेत्र बदलें",
        "sync_synced" to "क्लाउड से सिंक है",
        "sync_pending" to "सिंक लंबित",
        "search_hint" to "ऑर्डर्स, ग्राहक, बिल खोजें...",
        "inventory_catalog" to "कैटलॉग और स्टॉक",
        "team_tasks" to "टीम और कार्य",
        "free_subscription" to "४-वर्षीय नि:शुल्क साफ़्टवेयर सक्रिय",
        "cloud_sync_desc" to "सुरक्षित व्यापार प्रबंधन क्लाउड सिंक",
        "advance_paid" to "अग्रिम भुगतान",
        "balance_due" to "बकाया राशि",
        "duplicate_warning" to "संभावित डुप्लिकेट ग्राहक मिला"
    )

    private val gujaratiMap = mapOf(
        "nav_dashboard" to "ડેશબોર્ડ",
        "nav_work" to "ઓર્ડર્સ અને કાર્ય",
        "nav_customers" to "ગ્રાહકો",
        "nav_finance" to "નાણાકીય અને બિલિંગ",
        "nav_more" to "વધુ",
        "quick_add_customer" to "ગ્રાહક ઉમેરો",
        "quick_new_order" to "નવો ઓર્ડર",
        "quick_record_payment" to "ચુકવણી નોંધો",
        "quick_add_expense" to "ખર્ચ ઉમેરો",
        "quick_add_task" to "કાર્ય ઉમેરો",
        "stat_revenue" to "મળેલી આવક",
        "stat_receivables" to "બાકી રકમ",
        "stat_expenses" to "કુલ ખર્ચ",
        "stat_active_orders" to "ચાલુ ઓર્ડર્સ",
        "workspace_switch" to "વર્કસ્પેસ બદલો",
        "sync_synced" to "ક્લાઉડ સાથે સિંક છે",
        "sync_pending" to "સિંક બાકી છે",
        "search_hint" to "ઓર્ડર્સ, ગ્રાહકો, બિલ શોધો...",
        "inventory_catalog" to "કેટેલોગ અને સ્ટોક",
        "team_tasks" to "ટીમ અને કાર્યો",
        "free_subscription" to "૪-વર્ષ મફત સૉફ્ટવેર સક્રિય",
        "cloud_sync_desc" to "સુરક્ષિત વ્યવસાય સંચાલન સિંક",
        "advance_paid" to "એડવાન્સ ચુકવણી",
        "balance_due" to "બાકી રકમ",
        "duplicate_warning" to "સંભવિત ડુપ્લિકેટ ગ્રાહક મળ્યો"
    )
}
