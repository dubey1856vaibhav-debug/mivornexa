package com.example.bizflow.model

import java.util.UUID

enum class BusinessType(
    val displayName: String,
    val workRecordLabel: String,
    val workRecordPlural: String,
    val category: String = "Services & Tech"
) {
    RETAIL_ECOMMERCE("Retail & E-Commerce", "Order", "Orders", "Retail & Commerce"),
    BESPOKE_BOUTIQUE("Boutique & Tailoring", "Custom Order", "Custom Orders", "Retail & Commerce"),
    PROFESSIONAL_SERVICES("Professional Services / Agency", "Project", "Projects", "Services & Tech"),
    REPAIR_TECH("Electronics & Appliance Repair", "Repair Job", "Repair Jobs", "Services & Tech"),
    EDUCATION_TUTORING("Education & Academy", "Enrollment / Cohort", "Enrollments & Cohorts", "Services & Tech"),
    RESTAURANT_HOSPITALITY("Restaurant & Food Service", "Food Order", "Food Orders", "Hospitality & Wellness"),
    HEALTH_WELLNESS_SPA("Salon, Spa & Wellness", "Session / Booking", "Bookings & Sessions", "Hospitality & Wellness"),
    FITNESS_GYM_STUDIO("Gym, Fitness & Studios", "Membership / Pass", "Memberships & Passes", "Hospitality & Wellness"),
    AUTOMOTIVE_GARAGE("Automotive Garage & Detailing", "Service Order", "Service Orders", "Industrial & Trades"),
    CONSTRUCTION_CONTRACTING("Construction & Contracting", "Job / Contract", "Jobs & Contracts", "Industrial & Trades"),
    LOGISTICS_FREIGHT("Logistics & Freight Haulage", "Waybill / Shipment", "Shipments & Waybills", "Industrial & Trades"),
    MANUFACTURING_FABRICATION("Manufacturing & Fabrication", "Production Batch", "Production Batches", "Industrial & Trades"),
    REAL_ESTATE_PROPERTY("Real Estate & Property", "Listing / Deal", "Listings & Deals", "Property & Real Estate")
}

enum class ModuleType {
    ORDERS,
    PROJECTS,
    INVENTORY,
    BILLING,
    EXPENSES,
    APPOINTMENTS,
    TASKS,
    CUSTOM_FIELDS
}

enum class UserRole(val displayName: String) {
    OWNER("Owner"),
    ADMIN("Admin"),
    MANAGER("Manager"),
    STAFF("Staff"),
    VIEWER("Viewer")
}

data class CustomFieldDef(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val fieldType: String = "TEXT", // TEXT, NUMBER, DATE, SELECT
    val options: List<String> = emptyList(),
    val isRequired: Boolean = false
)

data class WorkStage(
    val id: String,
    val name: String,
    val colorHex: String,
    val isCompleted: Boolean = false
)

data class Workspace(
    val id: String,
    val name: String,
    val tagline: String,
    val businessType: BusinessType,
    val currencySymbol: String = "$",
    val currencyCode: String = "USD",
    val locale: String = "en-US",
    val enabledModules: Set<ModuleType>,
    val customFields: List<CustomFieldDef> = emptyList(),
    val workflowStages: List<WorkStage> = emptyList(),
    val currentUserRole: UserRole = UserRole.OWNER,
    val address: String = "123 Business Way",
    val phone: String = "+1 555-0199",
    val email: String = "contact@bizflow.io",
    val taxRatePercent: Double = 8.25,
    val notificationsEnabled: Boolean = true
)
