package com.example.bizflow.data

import com.example.bizflow.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class BackendConnectionInfo(
    val isConnectedToCloudFirestore: Boolean = false,
    val firestoreProjectId: String? = null,
    val connectionMode: String = "Local Workspace Engine (Cloud Ready)",
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val isGoogleServicesJsonDetected: Boolean = false
)

class BizFlowRepository private constructor() {

    private var appContext: android.content.Context? = null
    private val repositoryScope = CoroutineScope(Dispatchers.Default)
    private var firestoreDb: FirebaseFirestore? = null

    fun initPersistence(context: android.content.Context) {
        val app = context.applicationContext
        appContext = app
        val savedWorkspaces = SessionManager.loadCustomWorkspaces(app)
        if (savedWorkspaces.isNotEmpty()) {
            val savedMap = savedWorkspaces.associateBy { it.id }
            val merged = _workspaces.value.map { existing ->
                savedMap[existing.id] ?: existing
            }.toMutableList()
            savedWorkspaces.forEach { sw ->
                if (merged.none { it.id == sw.id }) {
                    merged.add(0, sw)
                }
            }
            _workspaces.value = merged
        }
        val savedActiveWsId = SessionManager.getActiveWorkspaceId(app)
        if (savedActiveWsId != null) {
            val found = _workspaces.value.find { it.id == savedActiveWsId }
            if (found != null) {
                _currentWorkspace.value = found
            }
        }
        val resId = app.resources.getIdentifier("firestore_database_id", "string", app.packageName)
        val dbId = if (resId != 0) app.getString(resId) else null
        if (!dbId.isNullOrBlank()) {
            try {
                firestoreDb = FirebaseFirestore.getInstance(dbId)
                _backendConnectionState.value = BackendConnectionInfo(
                    isConnectedToCloudFirestore = true,
                    firestoreProjectId = "project-9be64a52-e4d8-4221-a6d",
                    connectionMode = "Cloud Firestore Live ($dbId)",
                    lastSyncTimestamp = System.currentTimeMillis(),
                    isGoogleServicesJsonDetected = true
                )
            } catch (e: Exception) {
                android.util.Log.e("BizFlowRepo", "Failed to initialize Firestore", e)
            }
        }
        updateFilteredState()
    }

    // Pre-seeded multi-industry workspaces
    private val defaultWorkspaces = listOf(
        Workspace(
            id = "ws-retail-001",
            name = "Apex Gear & Retail",
            tagline = "Modern electronics, gear & fast delivery",
            businessType = BusinessType.RETAIL_ECOMMERCE,
            currencySymbol = "$",
            currencyCode = "USD",
            locale = "en-US",
            enabledModules = setOf(
                ModuleType.ORDERS,
                ModuleType.INVENTORY,
                ModuleType.BILLING,
                ModuleType.EXPENSES,
                ModuleType.TASKS
            ),
            customFields = listOf(
                CustomFieldDef(label = "Tracking Number", fieldType = "TEXT"),
                CustomFieldDef(label = "Shipping Carrier", fieldType = "SELECT", options = listOf("FedEx", "UPS", "DHL", "Local Courier"))
            ),
            workflowStages = listOf(
                WorkStage("stage-1", "Order Received", "#3B82F6"),
                WorkStage("stage-2", "Processing & Packing", "#8B5CF6"),
                WorkStage("stage-3", "Shipped in Transit", "#F59E0B"),
                WorkStage("stage-4", "Out for Delivery", "#EC4899"),
                WorkStage("stage-5", "Delivered", "#10B981", isCompleted = true)
            ),
            taxRatePercent = 8.5
        ),
        Workspace(
            id = "ws-agency-002",
            name = "Lumina Creative Studio",
            tagline = "Design, Branding & Full-Stack Development",
            businessType = BusinessType.PROFESSIONAL_SERVICES,
            currencySymbol = "$",
            currencyCode = "USD",
            locale = "en-US",
            enabledModules = setOf(
                ModuleType.PROJECTS,
                ModuleType.BILLING,
                ModuleType.EXPENSES,
                ModuleType.TASKS,
                ModuleType.CUSTOM_FIELDS
            ),
            customFields = listOf(
                CustomFieldDef(label = "Deliverable URL", fieldType = "TEXT"),
                CustomFieldDef(label = "Client Priority", fieldType = "SELECT", options = listOf("Standard", "High", "Critical")),
                CustomFieldDef(label = "Estimated Hours", fieldType = "NUMBER")
            ),
            workflowStages = listOf(
                WorkStage("stage-ag-1", "Brief & Discovery", "#3B82F6"),
                WorkStage("stage-ag-2", "Wireframing & Concept", "#6366F1"),
                WorkStage("stage-ag-3", "Development / Production", "#F59E0B"),
                WorkStage("stage-ag-4", "Client Review & QA", "#8B5CF6"),
                WorkStage("stage-ag-5", "Approved & Launched", "#10B981", isCompleted = true)
            ),
            taxRatePercent = 7.0
        ),
        Workspace(
            id = "ws-repair-003",
            name = "iFix Pro Diagnostic Labs",
            tagline = "Precision board repair, screens & gadgets",
            businessType = BusinessType.REPAIR_TECH,
            currencySymbol = "$",
            currencyCode = "USD",
            locale = "en-US",
            enabledModules = setOf(
                ModuleType.ORDERS,
                ModuleType.INVENTORY,
                ModuleType.BILLING,
                ModuleType.EXPENSES,
                ModuleType.TASKS,
                ModuleType.CUSTOM_FIELDS
            ),
            customFields = listOf(
                CustomFieldDef(label = "Device Serial / IMEI", fieldType = "TEXT", isRequired = true),
                CustomFieldDef(label = "Device Model", fieldType = "TEXT"),
                CustomFieldDef(label = "Reported Issue", fieldType = "TEXT"),
                CustomFieldDef(label = "Passcode / Lock PIN", fieldType = "TEXT")
            ),
            workflowStages = listOf(
                WorkStage("stage-rep-1", "Intake & Checked-In", "#3B82F6"),
                WorkStage("stage-rep-2", "Diagnostic & Quote", "#F59E0B"),
                WorkStage("stage-rep-3", "Awaiting Spare Parts", "#EC4899"),
                WorkStage("stage-rep-4", "Bench Repair", "#8B5CF6"),
                WorkStage("stage-rep-5", "QC Testing", "#06B6D4"),
                WorkStage("stage-rep-6", "Ready for Pickup", "#10B981", isCompleted = true)
            ),
            taxRatePercent = 6.0
        ),
        Workspace(
            id = "ws-boutique-004",
            name = "Zari Royal Haute Boutique",
            tagline = "Bespoke bridal, lehengas & couture tailoring",
            businessType = BusinessType.BESPOKE_BOUTIQUE,
            currencySymbol = "₹",
            currencyCode = "INR",
            locale = "en-IN",
            enabledModules = setOf(
                ModuleType.ORDERS,
                ModuleType.INVENTORY,
                ModuleType.BILLING,
                ModuleType.EXPENSES,
                ModuleType.APPOINTMENTS,
                ModuleType.TASKS,
                ModuleType.CUSTOM_FIELDS
            ),
            customFields = listOf(
                CustomFieldDef(label = "Bust / Chest (in)", fieldType = "NUMBER"),
                CustomFieldDef(label = "Waist (in)", fieldType = "NUMBER"),
                CustomFieldDef(label = "Length / Inseam (in)", fieldType = "NUMBER"),
                CustomFieldDef(label = "Fabric Code & Color", fieldType = "TEXT"),
                CustomFieldDef(label = "Embroidery Pattern", fieldType = "TEXT")
            ),
            workflowStages = listOf(
                WorkStage("stage-bt-1", "Measurement & Consultation", "#3B82F6"),
                WorkStage("stage-bt-2", "Fabric Sourcing & Cutting", "#8B5CF6"),
                WorkStage("stage-bt-3", "Hand Embroidery & Zari Work", "#F59E0B"),
                WorkStage("stage-bt-4", "First Fitting & Alteration", "#EC4899"),
                WorkStage("stage-bt-5", "Finishing & Ironing", "#06B6D4"),
                WorkStage("stage-bt-6", "Ready for Collection", "#10B981", isCompleted = true)
            ),
            taxRatePercent = 5.0
        ),
        Workspace(
            id = "ws-restaurant-005",
            name = "Urban Harvest Bistro",
            tagline = "Farm-to-table cuisine, catering & drinks",
            businessType = BusinessType.RESTAURANT_HOSPITALITY,
            currencySymbol = "$",
            currencyCode = "USD",
            locale = "en-US",
            enabledModules = setOf(
                ModuleType.ORDERS,
                ModuleType.INVENTORY,
                ModuleType.BILLING,
                ModuleType.EXPENSES,
                ModuleType.TASKS,
                ModuleType.CUSTOM_FIELDS
            ),
            customFields = listOf(
                CustomFieldDef(label = "Table Number", fieldType = "TEXT"),
                CustomFieldDef(label = "Dietary Restrictions", fieldType = "TEXT"),
                CustomFieldDef(label = "Order Type", fieldType = "SELECT", options = listOf("Dine-In", "Takeaway", "Catering Delivery"))
            ),
            workflowStages = listOf(
                WorkStage("stage-rest-1", "Order Placed", "#3B82F6"),
                WorkStage("stage-rest-2", "Kitchen Prep", "#F59E0B"),
                WorkStage("stage-rest-3", "Cooking & Plating", "#EC4899"),
                WorkStage("stage-rest-4", "Served / Packed", "#10B981", isCompleted = true)
            ),
            taxRatePercent = 9.0
        )
    )

    // Master memory store
    private val _workspaces = MutableStateFlow(defaultWorkspaces)
    val workspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    private val _currentWorkspace = MutableStateFlow(defaultWorkspaces[0])
    val currentWorkspace: StateFlow<Workspace> = _currentWorkspace.asStateFlow()

    private val _allCustomers = MutableStateFlow<List<Customer>>(emptyList())
    private val _allWorkRecords = MutableStateFlow<List<WorkRecord>>(emptyList())
    private val _allProducts = MutableStateFlow<List<ProductItem>>(emptyList())
    private val _allInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    private val _allExpenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _allTasks = MutableStateFlow<List<TeamTask>>(emptyList())
    private val _allStockMovements = MutableStateFlow<List<StockMovement>>(emptyList())

    // Filtered by current workspace
    val customers = MutableStateFlow<List<Customer>>(emptyList())
    val workRecords = MutableStateFlow<List<WorkRecord>>(emptyList())
    val products = MutableStateFlow<List<ProductItem>>(emptyList())
    val invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val expenses = MutableStateFlow<List<Expense>>(emptyList())
    val tasks = MutableStateFlow<List<TeamTask>>(emptyList())

    private val _teamMembers = MutableStateFlow<List<TeamMember>>(emptyList())
    val teamMembers: StateFlow<List<TeamMember>> = _teamMembers.asStateFlow()

    private val _syncState = MutableStateFlow("ONLINE_SYNCED") // ONLINE_SYNCED, SYNCING, PENDING_SYNC
    val syncState: StateFlow<String> = _syncState.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _backendConnectionState = MutableStateFlow(BackendConnectionInfo())
    val backendConnectionState: StateFlow<BackendConnectionInfo> = _backendConnectionState.asStateFlow()

    init {
        seedInitialData()
        updateFilteredState()
    }

    private fun seedInitialData() {
        val seededCustomers = mutableListOf<Customer>()
        val seededWorkRecords = mutableListOf<WorkRecord>()
        val seededProducts = mutableListOf<ProductItem>()
        val seededInvoices = mutableListOf<Invoice>()
        val seededExpenses = mutableListOf<Expense>()
        val seededTasks = mutableListOf<TeamTask>()

        // Seed Retail Workspace
        val c1 = Customer(
            id = "c-retail-1",
            workspaceId = "ws-retail-001",
            name = "Sarah Jenkins",
            phone = "+1 (555) 234-8901",
            email = "sarah.j@example.com",
            address = "742 Evergreen Terrace, Springfield",
            tags = listOf("VIP", "Frequent Buyer"),
            notes = "Prefers express shipping and eco packaging.",
            totalOrdersCount = 4,
            totalSpent = 1240.0,
            outstandingBalance = 0.0
        )
        val c2 = Customer(
            id = "c-retail-2",
            workspaceId = "ws-retail-001",
            name = "Marcus Vance",
            phone = "+1 (555) 789-1234",
            email = "marcus.vance@techcorp.com",
            address = "100 Market St, Suite 400, San Francisco, CA",
            tags = listOf("B2B", "Wholesale"),
            notes = "Corporate tech supply account.",
            totalOrdersCount = 2,
            totalSpent = 3500.0,
            outstandingBalance = 850.0
        )
        seededCustomers.addAll(listOf(c1, c2))

        val p1 = ProductItem(
            id = "p-1",
            workspaceId = "ws-retail-001",
            name = "Ergonomic Mechanical Keyboard",
            sku = "KB-MECH-RGB",
            type = ItemType.PRODUCT,
            category = "Peripherals",
            unitPrice = 149.99,
            costPrice = 85.0,
            currentStock = 18.0,
            minStockThreshold = 5.0,
            supplierName = "Keyronix Supply Co"
        )
        val p2 = ProductItem(
            id = "p-2",
            workspaceId = "ws-retail-001",
            name = "USB-C Dual 4K Display Dock",
            sku = "DOCK-4K-UBC",
            type = ItemType.PRODUCT,
            category = "Accessories",
            unitPrice = 199.50,
            costPrice = 110.0,
            currentStock = 3.0, // Low stock!
            minStockThreshold = 6.0,
            supplierName = "OmniPort Logistics"
        )
        val p3 = ProductItem(
            id = "p-3",
            workspaceId = "ws-retail-001",
            name = "Express Desk Setup & Calibration",
            sku = "SRV-CALIB",
            type = ItemType.SERVICE,
            category = "Installation",
            unitPrice = 75.0,
            costPrice = 20.0,
            currentStock = 999.0
        )
        seededProducts.addAll(listOf(p1, p2, p3))

        val wr1 = WorkRecord(
            id = "wr-1",
            workspaceId = "ws-retail-001",
            recordNumber = "ORD-2026-101",
            customerId = c2.id,
            customerName = c2.name,
            customerPhone = c2.phone,
            title = "Corporate Desktop Gear Bundle",
            currentStageId = "stage-3", // Shipped in Transit
            assignedStaffName = "Alex Rivera",
            items = listOf(
                LineItem(description = "Ergonomic Mechanical Keyboard", quantity = 5.0, unitPrice = 149.99),
                LineItem(description = "USB-C Dual 4K Display Dock", quantity = 5.0, unitPrice = 199.50)
            ),
            customFieldValues = mapOf(
                "Tracking Number" to "FEDEX-982144812",
                "Shipping Carrier" to "FedEx"
            ),
            totalAmount = 1747.45,
            advancePaid = 1000.0,
            balanceDue = 747.45,
            notes = "Include bulk packing slip inside pallet."
        )
        seededWorkRecords.add(wr1)

        val inv1 = Invoice(
            id = "inv-1",
            workspaceId = "ws-retail-001",
            invoiceNumber = "INV-2026-0042",
            customerId = c2.id,
            customerName = c2.name,
            customerEmail = c2.email,
            customerAddress = c2.address,
            type = InvoiceType.INVOICE,
            status = InvoiceStatus.PARTIALLY_PAID,
            workRecordId = wr1.id,
            items = wr1.items,
            subtotal = 1747.45,
            taxRatePercent = 8.5,
            taxAmount = 148.53,
            totalAmount = 1895.98,
            totalPaid = 1000.0,
            balanceDue = 895.98,
            payments = listOf(
                Payment(
                    invoiceId = "inv-1",
                    workspaceId = "ws-retail-001",
                    amount = 1000.0,
                    method = PaymentMethod.BANK_TRANSFER,
                    referenceNumber = "WIRE-8891234",
                    paymentType = "ADVANCE",
                    notes = "Advance 50% deposit received via wire."
                )
            )
        )
        seededInvoices.add(inv1)

        val exp1 = Expense(
            id = "exp-1",
            workspaceId = "ws-retail-001",
            title = "Keyronix Wholesale Restock",
            category = ExpenseCategory.INVENTORY_MATERIALS,
            amount = 1250.0,
            paidTo = "Keyronix Supply Co",
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            referenceReceiptNo = "PO-7721",
            notes = "Bulk restock of mechanical keyboards."
        )
        val exp2 = Expense(
            id = "exp-2",
            workspaceId = "ws-retail-001",
            title = "Warehouse Internet & Fiber",
            category = ExpenseCategory.UTILITIES,
            amount = 120.0,
            paidTo = "FastNet Telecom",
            paymentMethod = PaymentMethod.CARD,
            referenceReceiptNo = "UTIL-OCT-01",
            notes = "High-speed line for automated dispatch scanner."
        )
        seededExpenses.addAll(listOf(exp1, exp2))

        val t1 = TeamTask(
            id = "t-1",
            workspaceId = "ws-retail-001",
            title = "Dispatch Order #101 via FedEx Express",
            description = "Ensure double-walled box and fragile stickers are affixed.",
            priority = TaskPriority.HIGH,
            status = TaskStatus.IN_PROGRESS,
            assignedToMemberName = "Alex Rivera",
            relatedRecordId = wr1.id
        )
        val t2 = TeamTask(
            id = "t-2",
            workspaceId = "ws-retail-001",
            title = "Reorder USB-C Display Docks (Stock < 5)",
            description = "Contact OmniPort supplier for 50 additional units.",
            priority = TaskPriority.URGENT,
            status = TaskStatus.TODO,
            assignedToMemberName = "Jordan Hayes"
        )
        seededTasks.addAll(listOf(t1, t2))

        // Seed Boutique Workspace (Bespoke Tailoring)
        val cBoutique = Customer(
            id = "c-boutique-1",
            workspaceId = "ws-boutique-004",
            name = "Priya Sharma",
            phone = "+91 98200 45678",
            email = "priya.sharma@example.in",
            address = "Bandra West, Mumbai",
            tags = listOf("Bridal", "Custom Couture"),
            notes = "Bride wedding outfit for December reception.",
            totalOrdersCount = 1,
            totalSpent = 85000.0,
            outstandingBalance = 25000.0
        )
        seededCustomers.add(cBoutique)

        val wrBoutique = WorkRecord(
            id = "wr-bt-1",
            workspaceId = "ws-boutique-004",
            recordNumber = "ORD-BT-2026-08",
            customerId = cBoutique.id,
            customerName = cBoutique.name,
            customerPhone = cBoutique.phone,
            title = "Hand-Embroidered Velvet Lehenga",
            currentStageId = "stage-bt-3", // Hand Embroidery & Zari Work
            assignedStaffName = "Master Ji Ramesh",
            items = listOf(
                LineItem(description = "Pure Raw Silk & Velvet Lehenga Skirt", quantity = 1.0, unitPrice = 55000.0),
                LineItem(description = "Zari & Dabka Hand Embroidery Work", quantity = 1.0, unitPrice = 25000.0),
                LineItem(description = "Custom Handcrafted Dupatta", quantity = 1.0, unitPrice = 5000.0)
            ),
            customFieldValues = mapOf(
                "Bust / Chest (in)" to "36.5",
                "Waist (in)" to "29.0",
                "Length / Inseam (in)" to "42.0",
                "Fabric Code & Color" to "Royal Emerald Green Silk #EG-09",
                "Embroidery Pattern" to "Floral Peacock Zardozi Motifs"
            ),
            totalAmount = 85000.0,
            advancePaid = 60000.0,
            balanceDue = 25000.0,
            notes = "Trial fitting booked for 28th October."
        )
        seededWorkRecords.add(wrBoutique)

        val invBoutique = Invoice(
            id = "inv-bt-1",
            workspaceId = "ws-boutique-004",
            invoiceNumber = "INV-ZARI-104",
            customerId = cBoutique.id,
            customerName = cBoutique.name,
            customerEmail = cBoutique.email,
            customerAddress = cBoutique.address,
            type = InvoiceType.INVOICE,
            status = InvoiceStatus.PARTIALLY_PAID,
            workRecordId = wrBoutique.id,
            items = wrBoutique.items,
            subtotal = 85000.0,
            taxRatePercent = 5.0,
            taxAmount = 4250.0,
            totalAmount = 89250.0,
            totalPaid = 60000.0,
            balanceDue = 29250.0,
            payments = listOf(
                Payment(
                    invoiceId = "inv-bt-1",
                    workspaceId = "ws-boutique-004",
                    amount = 60000.0,
                    method = PaymentMethod.UPI,
                    referenceNumber = "UPI-771928401",
                    paymentType = "ADVANCE",
                    notes = "Advance received via Google Pay / UPI."
                )
            )
        )
        seededInvoices.add(invBoutique)

        val expBoutique = Expense(
            id = "exp-bt-1",
            workspaceId = "ws-boutique-004",
            title = "Zari Gold Thread & Raw Silk Fabric Sourcing",
            category = ExpenseCategory.INVENTORY_MATERIALS,
            amount = 18500.0,
            paidTo = "Surat Textiles Mart",
            paymentMethod = PaymentMethod.UPI,
            referenceReceiptNo = "TX-88214",
            notes = "Pure silk yarn and authentic gold zari spools."
        )
        seededExpenses.add(expBoutique)

        // Seed Repair Workspace
        val cRepair = Customer(
            id = "c-rep-1",
            workspaceId = "ws-repair-003",
            name = "David Chen",
            phone = "+1 (555) 432-9087",
            email = "d.chen@designflow.co",
            address = "55 Silicon Blvd, San Jose, CA",
            tags = listOf("Walk-In", "Urgent"),
            notes = "Freelance designer needs laptop back before Friday.",
            totalOrdersCount = 1,
            totalSpent = 380.0,
            outstandingBalance = 0.0
        )
        seededCustomers.add(cRepair)

        val wrRepair = WorkRecord(
            id = "wr-rep-1",
            workspaceId = "ws-repair-003",
            recordNumber = "JOB-REP-2026-77",
            customerId = cRepair.id,
            customerName = cRepair.name,
            customerPhone = cRepair.phone,
            title = "MacBook Pro Liquid Spill & Screen Repair",
            currentStageId = "stage-rep-4", // Bench Repair
            assignedStaffName = "Elena Rostova",
            items = listOf(
                LineItem(description = "Retina Display Panel Replacement", quantity = 1.0, unitPrice = 280.0),
                LineItem(description = "Ultrasonic Logic Board Cleaning & De-oxidation", quantity = 1.0, unitPrice = 100.0)
            ),
            customFieldValues = mapOf(
                "Device Serial / IMEI" to "C02G871KMD6T",
                "Device Model" to "MacBook Pro 14 M2 Pro (2023)",
                "Reported Issue" to "Water spilled on keyboard, screen flickering, no backlight"
            ),
            totalAmount = 380.0,
            advancePaid = 150.0,
            balanceDue = 230.0,
            notes = "Board ultrasonic cleaned; testing power rails."
        )
        seededWorkRecords.add(wrRepair)

        // Team members
        _teamMembers.value = listOf(
            TeamMember(name = "Vaibhav Dubey", email = "dubey1856vaibhav@gmail.com", role = UserRole.OWNER, department = "Executive"),
            TeamMember(name = "Alex Rivera", email = "alex.r@bizflow.io", role = UserRole.MANAGER, department = "Operations"),
            TeamMember(name = "Elena Rostova", email = "elena.tech@bizflow.io", role = UserRole.STAFF, department = "Diagnostics"),
            TeamMember(name = "Master Ji Ramesh", email = "ramesh.tailor@bizflow.io", role = UserRole.STAFF, department = "Production"),
            TeamMember(name = "Jordan Hayes", email = "jordan.h@bizflow.io", role = UserRole.ADMIN, department = "Finance")
        )

        _allCustomers.value = seededCustomers
        _allWorkRecords.value = seededWorkRecords
        _allProducts.value = seededProducts
        _allInvoices.value = seededInvoices
        _allExpenses.value = seededExpenses
        _allTasks.value = seededTasks
    }

    private fun updateFilteredState() {
        val currentWsId = _currentWorkspace.value.id
        customers.value = _allCustomers.value.filter { it.workspaceId == currentWsId }
        workRecords.value = _allWorkRecords.value.filter { it.workspaceId == currentWsId }
        products.value = _allProducts.value.filter { it.workspaceId == currentWsId }
        invoices.value = _allInvoices.value.filter { it.workspaceId == currentWsId }
        expenses.value = _allExpenses.value.filter { it.workspaceId == currentWsId }
        tasks.value = _allTasks.value.filter { it.workspaceId == currentWsId }
    }

    // Workspace switching
    fun selectWorkspace(workspaceId: String) {
        val ws = _workspaces.value.find { it.id == workspaceId } ?: return
        _currentWorkspace.value = ws
        updateFilteredState()
        appContext?.let { ctx ->
            if (SessionManager.isAuthenticated(ctx)) {
                SessionManager.saveSession(
                    context = ctx,
                    email = SessionManager.getSavedEmail(ctx),
                    rememberMe = SessionManager.shouldRememberMe(ctx),
                    workspaceId = ws.id,
                    isCustomerPortal = false
                )
            }
        }
    }

    fun addWorkspace(workspace: Workspace) {
        _workspaces.value = _workspaces.value + workspace
        _currentWorkspace.value = workspace
        updateFilteredState()
        appContext?.let { ctx ->
            val customOnly = _workspaces.value.filter { ws -> !defaultWorkspaces.any { it.id == ws.id } }
            SessionManager.saveCustomWorkspaces(ctx, customOnly)
        }
    }

    fun getDefaultStagesForType(businessType: BusinessType): List<WorkStage> {
        return when (businessType) {
            BusinessType.RETAIL_ECOMMERCE -> listOf(
                WorkStage("st-1", "Received", "#3B82F6"),
                WorkStage("st-2", "Processing & Packing", "#8B5CF6"),
                WorkStage("st-3", "Shipped in Transit", "#F59E0B"),
                WorkStage("st-4", "Delivered", "#10B981", isCompleted = true)
            )
            BusinessType.BESPOKE_BOUTIQUE -> listOf(
                WorkStage("st-1", "Measurement & Consultation", "#3B82F6"),
                WorkStage("st-2", "Cutting & Fabric Sourcing", "#8B5CF6"),
                WorkStage("st-3", "Embroidery & Tailoring", "#F59E0B"),
                WorkStage("st-4", "Ready for Trial & Pickup", "#10B981", isCompleted = true)
            )
            BusinessType.REPAIR_TECH -> listOf(
                WorkStage("st-1", "Intake & Check-In", "#3B82F6"),
                WorkStage("st-2", "Diagnostics & Quote", "#F59E0B"),
                WorkStage("st-3", "Bench Repair", "#8B5CF6"),
                WorkStage("st-4", "Ready for Pickup", "#10B981", isCompleted = true)
            )
            BusinessType.PROFESSIONAL_SERVICES -> listOf(
                WorkStage("st-1", "Discovery & Brief", "#3B82F6"),
                WorkStage("st-2", "Production & Execution", "#F59E0B"),
                WorkStage("st-3", "Client Review & QA", "#8B5CF6"),
                WorkStage("st-4", "Delivered & Approved", "#10B981", isCompleted = true)
            )
            BusinessType.EDUCATION_TUTORING -> listOf(
                WorkStage("st-1", "Registration & Placement", "#3B82F6"),
                WorkStage("st-2", "Active Term & Classes", "#8B5CF6"),
                WorkStage("st-3", "Mid-Term Evaluation", "#F59E0B"),
                WorkStage("st-4", "Certified & Graduated", "#10B981", isCompleted = true)
            )
            BusinessType.RESTAURANT_HOSPITALITY -> listOf(
                WorkStage("st-1", "Order Placed", "#3B82F6"),
                WorkStage("st-2", "Kitchen Prep", "#F59E0B"),
                WorkStage("st-3", "Cooking & Plating", "#EC4899"),
                WorkStage("st-4", "Served / Delivered", "#10B981", isCompleted = true)
            )
            BusinessType.HEALTH_WELLNESS_SPA -> listOf(
                WorkStage("st-1", "Intake & Consultation", "#3B82F6"),
                WorkStage("st-2", "Therapy & Treatment", "#8B5CF6"),
                WorkStage("st-3", "Recovery & Refresh", "#EC4899"),
                WorkStage("st-4", "Completed & Rebooked", "#10B981", isCompleted = true)
            )
            BusinessType.FITNESS_GYM_STUDIO -> listOf(
                WorkStage("st-1", "Assessment & Trial", "#3B82F6"),
                WorkStage("st-2", "Active Membership", "#10B981"),
                WorkStage("st-3", "Renewal Window", "#F59E0B"),
                WorkStage("st-4", "Completed / Extended", "#6366F1", isCompleted = true)
            )
            BusinessType.AUTOMOTIVE_GARAGE -> listOf(
                WorkStage("st-1", "Vehicle Check-In", "#3B82F6"),
                WorkStage("st-2", "Diagnostics & Quote", "#F59E0B"),
                WorkStage("st-3", "Mechanical Repair", "#8B5CF6"),
                WorkStage("st-4", "Road Test & Detailing", "#06B6D4"),
                WorkStage("st-5", "Ready for Release", "#10B981", isCompleted = true)
            )
            BusinessType.CONSTRUCTION_CONTRACTING -> listOf(
                WorkStage("st-1", "Site Estimate & Blueprint", "#3B82F6"),
                WorkStage("st-2", "Material Staging", "#F59E0B"),
                WorkStage("st-3", "Structural Construction", "#8B5CF6"),
                WorkStage("st-4", "Punch List & Inspection", "#EC4899"),
                WorkStage("st-5", "Handover & Sign-Off", "#10B981", isCompleted = true)
            )
            BusinessType.LOGISTICS_FREIGHT -> listOf(
                WorkStage("st-1", "Dispatch & Loading", "#3B82F6"),
                WorkStage("st-2", "In-Transit Haulage", "#F59E0B"),
                WorkStage("st-3", "Customs / Hub Cleared", "#8B5CF6"),
                WorkStage("st-4", "Delivered & POD Signed", "#10B981", isCompleted = true)
            )
            BusinessType.MANUFACTURING_FABRICATION -> listOf(
                WorkStage("st-1", "CAD & Spec Review", "#3B82F6"),
                WorkStage("st-2", "Raw Material Allocated", "#F59E0B"),
                WorkStage("st-3", "CNC / Production Run", "#8B5CF6"),
                WorkStage("st-4", "QA Tolerance Inspection", "#06B6D4"),
                WorkStage("st-5", "Crated & Dispatched", "#10B981", isCompleted = true)
            )
            BusinessType.REAL_ESTATE_PROPERTY -> listOf(
                WorkStage("st-1", "New Listing & Staging", "#3B82F6"),
                WorkStage("st-2", "Viewings & Inquiries", "#8B5CF6"),
                WorkStage("st-3", "Offer & Escrow Contract", "#F59E0B"),
                WorkStage("st-4", "Settlement & Keys Handed", "#10B981", isCompleted = true)
            )
        }
    }

    fun getDefaultCustomFieldsForType(businessType: BusinessType): List<CustomFieldDef> {
        return when (businessType) {
            BusinessType.RETAIL_ECOMMERCE -> listOf(
                CustomFieldDef(label = "Tracking Number", fieldType = "TEXT"),
                CustomFieldDef(label = "Shipping Carrier", fieldType = "SELECT", options = listOf("FedEx", "UPS", "DHL", "Local Courier"))
            )
            BusinessType.BESPOKE_BOUTIQUE -> listOf(
                CustomFieldDef(label = "Bust / Chest (in)", fieldType = "NUMBER"),
                CustomFieldDef(label = "Waist (in)", fieldType = "NUMBER"),
                CustomFieldDef(label = "Length (in)", fieldType = "NUMBER"),
                CustomFieldDef(label = "Fabric Code & Color", fieldType = "TEXT")
            )
            BusinessType.REPAIR_TECH -> listOf(
                CustomFieldDef(label = "Device Serial / IMEI", fieldType = "TEXT", isRequired = true),
                CustomFieldDef(label = "Device Model", fieldType = "TEXT"),
                CustomFieldDef(label = "Reported Issue", fieldType = "TEXT"),
                CustomFieldDef(label = "Passcode / PIN", fieldType = "TEXT")
            )
            BusinessType.PROFESSIONAL_SERVICES -> listOf(
                CustomFieldDef(label = "Deliverable URL", fieldType = "TEXT"),
                CustomFieldDef(label = "Client Priority", fieldType = "SELECT", options = listOf("Standard", "High", "Critical")),
                CustomFieldDef(label = "Estimated Hours", fieldType = "NUMBER")
            )
            BusinessType.EDUCATION_TUTORING -> listOf(
                CustomFieldDef(label = "Student / Learner ID", fieldType = "TEXT", isRequired = true),
                CustomFieldDef(label = "Course / Cohort Code", fieldType = "TEXT"),
                CustomFieldDef(label = "Assigned Instructor", fieldType = "TEXT"),
                CustomFieldDef(label = "Attendance Status", fieldType = "SELECT", options = listOf("Regular (90%+)", "Standard", "At Risk"))
            )
            BusinessType.RESTAURANT_HOSPITALITY -> listOf(
                CustomFieldDef(label = "Table Number / Room", fieldType = "TEXT"),
                CustomFieldDef(label = "Dietary Restrictions", fieldType = "TEXT"),
                CustomFieldDef(label = "Order Type", fieldType = "SELECT", options = listOf("Dine-In", "Takeaway", "Delivery"))
            )
            BusinessType.HEALTH_WELLNESS_SPA -> listOf(
                CustomFieldDef(label = "Lead Therapist / Stylist", fieldType = "TEXT"),
                CustomFieldDef(label = "Suite / Chair #", fieldType = "TEXT"),
                CustomFieldDef(label = "Skin & Allergy Notes", fieldType = "TEXT"),
                CustomFieldDef(label = "Next Recommended Visit", fieldType = "TEXT")
            )
            BusinessType.FITNESS_GYM_STUDIO -> listOf(
                CustomFieldDef(label = "Member Card ID", fieldType = "TEXT", isRequired = true),
                CustomFieldDef(label = "Plan Tier", fieldType = "SELECT", options = listOf("Monthly Basic", "Annual Unlimited", "VIP Coaching")),
                CustomFieldDef(label = "Primary Fitness Goal", fieldType = "TEXT"),
                CustomFieldDef(label = "Assigned Trainer", fieldType = "TEXT")
            )
            BusinessType.AUTOMOTIVE_GARAGE -> listOf(
                CustomFieldDef(label = "VIN / Chassis #", fieldType = "TEXT", isRequired = true),
                CustomFieldDef(label = "License Plate #", fieldType = "TEXT"),
                CustomFieldDef(label = "Make, Model & Year", fieldType = "TEXT"),
                CustomFieldDef(label = "Odometer Mileage", fieldType = "NUMBER"),
                CustomFieldDef(label = "Fuel Level", fieldType = "SELECT", options = listOf("1/4 Tank", "1/2 Tank", "3/4 Tank", "Full Tank"))
            )
            BusinessType.CONSTRUCTION_CONTRACTING -> listOf(
                CustomFieldDef(label = "Job Site Address", fieldType = "TEXT", isRequired = true),
                CustomFieldDef(label = "Municipal Permit #", fieldType = "TEXT"),
                CustomFieldDef(label = "Subcontractor Lead", fieldType = "TEXT"),
                CustomFieldDef(label = "Safety Inspection Status", fieldType = "SELECT", options = listOf("Pending Review", "Approved / Passed", "Conditional Fixes"))
            )
            BusinessType.LOGISTICS_FREIGHT -> listOf(
                CustomFieldDef(label = "Bill of Lading (BOL) #", fieldType = "TEXT", isRequired = true),
                CustomFieldDef(label = "Trailer / Vehicle VIN", fieldType = "TEXT"),
                CustomFieldDef(label = "Freight Weight (kg)", fieldType = "NUMBER"),
                CustomFieldDef(label = "Consignee Contact Phone", fieldType = "TEXT")
            )
            BusinessType.MANUFACTURING_FABRICATION -> listOf(
                CustomFieldDef(label = "Drawing / CAD Spec #", fieldType = "TEXT", isRequired = true),
                CustomFieldDef(label = "Batch Production Quantity", fieldType = "NUMBER"),
                CustomFieldDef(label = "Machining Cell / Station", fieldType = "TEXT"),
                CustomFieldDef(label = "QA Tolerance Status", fieldType = "SELECT", options = listOf("Within Spec (Passed)", "Re-machining Required", "Scrapped"))
            )
            BusinessType.REAL_ESTATE_PROPERTY -> listOf(
                CustomFieldDef(label = "Property MLS / Unit #", fieldType = "TEXT", isRequired = true),
                CustomFieldDef(label = "Deal Structure", fieldType = "SELECT", options = listOf("Residential Sale", "Commercial Lease", "Short-Term Rental")),
                CustomFieldDef(label = "Escrow Deposit Amount", fieldType = "NUMBER"),
                CustomFieldDef(label = "Buyer / Tenant Rep", fieldType = "TEXT")
            )
        }
    }

    fun updateWorkspace(updated: Workspace, adaptWorkflowsAndFields: Boolean = false): Workspace {
        val finalWs = if (adaptWorkflowsAndFields) {
            updated.copy(
                workflowStages = getDefaultStagesForType(updated.businessType),
                customFields = getDefaultCustomFieldsForType(updated.businessType)
            )
        } else {
            updated
        }

        val list = _workspaces.value.toMutableList()
        val index = list.indexOfFirst { it.id == finalWs.id }
        if (index >= 0) {
            list[index] = finalWs
        } else {
            list.add(0, finalWs)
        }
        _workspaces.value = list
        if (_currentWorkspace.value.id == finalWs.id) {
            _currentWorkspace.value = finalWs
        }
        updateFilteredState()

        appContext?.let { ctx ->
            SessionManager.saveCustomWorkspaces(ctx, _workspaces.value)
        }
        return finalWs
    }

    fun createCompanyAccount(fullName: String, email: String, companyName: String, businessType: BusinessType): Workspace {
        val stages = getDefaultStagesForType(businessType)
        val customFields = getDefaultCustomFieldsForType(businessType)

        val newWs = Workspace(
            id = "ws-${UUID.randomUUID().toString().take(8)}",
            name = companyName.trim(),
            tagline = "${businessType.displayName} Workspace",
            businessType = businessType,
            currencySymbol = "$",
            currencyCode = "USD",
            enabledModules = setOf(
                ModuleType.ORDERS,
                ModuleType.BILLING,
                ModuleType.EXPENSES,
                ModuleType.TASKS,
                ModuleType.INVENTORY
            ),
            workflowStages = stages,
            customFields = customFields,
            currentUserRole = UserRole.OWNER,
            email = email.trim()
        )

        val ownerMember = TeamMember(
            name = fullName.trim().ifEmpty { "Owner" },
            email = email.trim(),
            role = UserRole.OWNER,
            department = "Executive"
        )

        _workspaces.value = listOf(newWs) + _workspaces.value
        _teamMembers.value = listOf(ownerMember)
        _currentWorkspace.value = newWs
        updateFilteredState()

        appContext?.let { ctx ->
            val customOnly = _workspaces.value.filter { ws -> !defaultWorkspaces.any { it.id == ws.id } }
            SessionManager.saveCustomWorkspaces(ctx, customOnly)
        }
        return newWs
    }

    fun signInWithEmail(email: String): Workspace {
        val existing = _workspaces.value.find { it.email.equals(email.trim(), true) }
        val target = existing ?: _workspaces.value.first()
        _currentWorkspace.value = target
        updateFilteredState()
        return target
    }

    // Customer duplicate checking
    fun findPossibleDuplicateCustomers(name: String, phone: String, email: String, excludeId: String? = null): List<Customer> {
        val currentList = customers.value
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val cleanName = name.trim().lowercase()
        val cleanEmail = email.trim().lowercase()

        return currentList.filter { c ->
            if (c.id == excludeId) return@filter false
            val cPhone = c.phone.replace(Regex("[^0-9]"), "")
            val cName = c.name.trim().lowercase()
            val cEmail = c.email.trim().lowercase()

            val phoneMatches = cleanPhone.length >= 7 && cPhone.length >= 7 &&
                    (cleanPhone.takeLast(10) == cPhone.takeLast(10) || cleanPhone.endsWith(cPhone) || cPhone.endsWith(cleanPhone))

            phoneMatches ||
            (cleanEmail.isNotEmpty() && cleanEmail == cEmail) ||
            (cleanName.isNotEmpty() && cName == cleanName)
        }
    }

    fun saveCustomer(customer: Customer) {
        val updated = _allCustomers.value.toMutableList()
        val index = updated.indexOfFirst { it.id == customer.id }
        if (index >= 0) {
            updated[index] = customer.copy(updatedAt = System.currentTimeMillis(), syncState = "SYNCED")
        } else {
            updated.add(0, customer.copy(updatedAt = System.currentTimeMillis(), syncState = "SYNCED"))
        }
        _allCustomers.value = updated
        updateFilteredState()

        val user = Firebase.auth.currentUser
        val db = firestoreDb
        if (user != null && db != null) {
            repositoryScope.launch {
                try {
                    db.collection("users").document(user.uid).collection("customers").document(customer.id).set(
                        mapOf(
                            "userId" to user.uid,
                            "workspaceId" to customer.workspaceId,
                            "name" to customer.name,
                            "phone" to customer.phone,
                            "email" to customer.email,
                            "company" to customer.companyName
                        )
                    )
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, "users/${user.uid}/customers/${customer.id}")
                }
            }
        }
    }

    fun deleteCustomer(id: String) {
        _allCustomers.value = _allCustomers.value.filter { it.id != id }
        updateFilteredState()
    }

    // Work Orders
    fun saveWorkRecord(record: WorkRecord) {
        val updated = _allWorkRecords.value.toMutableList()
        val index = updated.indexOfFirst { it.id == record.id }
        if (index >= 0) {
            updated[index] = record.copy(updatedAt = System.currentTimeMillis(), syncState = "SYNCED")
        } else {
            updated.add(0, record.copy(updatedAt = System.currentTimeMillis(), syncState = "SYNCED"))
        }
        _allWorkRecords.value = updated
        updateFilteredState()

        val user = Firebase.auth.currentUser
        val db = firestoreDb
        if (user != null && db != null) {
            repositoryScope.launch {
                try {
                    db.collection("users").document(user.uid).collection("workRecords").document(record.id).set(
                        mapOf(
                            "userId" to user.uid,
                            "workspaceId" to record.workspaceId,
                            "customerId" to record.customerId,
                            "title" to record.title,
                            "status" to record.recordNumber,
                            "currentStageId" to record.currentStageId,
                            "price" to record.totalAmount,
                            "paidAmount" to record.advancePaid
                        )
                    )
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, "users/${user.uid}/workRecords/${record.id}")
                }
            }
        }
    }

    fun updateWorkStage(recordId: String, newStageId: String) {
        val updated = _allWorkRecords.value.map { wr ->
            if (wr.id == recordId) {
                wr.copy(currentStageId = newStageId, updatedAt = System.currentTimeMillis())
            } else wr
        }
        _allWorkRecords.value = updated
        updateFilteredState()
    }

    // Products and Inventory
    fun saveProduct(item: ProductItem) {
        val updated = _allProducts.value.toMutableList()
        val index = updated.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            updated[index] = item.copy(syncState = "SYNCED")
        } else {
            updated.add(0, item.copy(syncState = "SYNCED"))
        }
        _allProducts.value = updated
        updateFilteredState()
    }

    fun adjustStock(productId: String, quantityChange: Double, reason: String) {
        val currentItem = _allProducts.value.find { it.id == productId } ?: return
        val newStock = (currentItem.currentStock + quantityChange).coerceAtLeast(0.0)
        val movement = StockMovement(
            productId = productId,
            quantityChange = quantityChange,
            reason = reason,
            previousStock = currentItem.currentStock,
            newStock = newStock
        )
        _allStockMovements.value = _allStockMovements.value + movement
        saveProduct(currentItem.copy(currentStock = newStock))
    }

    // Invoices and Payments
    fun saveInvoice(invoice: Invoice) {
        val updated = _allInvoices.value.toMutableList()
        val index = updated.indexOfFirst { it.id == invoice.id }
        if (index >= 0) {
            updated[index] = invoice.copy(syncState = "SYNCED")
        } else {
            updated.add(0, invoice.copy(syncState = "SYNCED"))
        }
        _allInvoices.value = updated
        updateFilteredState()
    }

    fun recordPayment(invoiceId: String, payment: Payment) {
        val invoice = _allInvoices.value.find { it.id == invoiceId } ?: return
        val newPayments = invoice.payments + payment
        val newTotalPaid = newPayments.sumOf { it.amount }
        val newBalance = (invoice.totalAmount - newTotalPaid).coerceAtLeast(0.0)
        val newStatus = when {
            newBalance <= 0.001 -> InvoiceStatus.PAID
            newTotalPaid > 0.0 -> InvoiceStatus.PARTIALLY_PAID
            else -> invoice.status
        }
        val updatedInvoice = invoice.copy(
            payments = newPayments,
            totalPaid = newTotalPaid,
            balanceDue = newBalance,
            status = newStatus
        )
        saveInvoice(updatedInvoice)

        // Also update corresponding customer's spent / balance
        val cust = _allCustomers.value.find { it.id == invoice.customerId }
        if (cust != null) {
            val updatedCust = cust.copy(
                outstandingBalance = (cust.outstandingBalance - payment.amount).coerceAtLeast(0.0)
            )
            saveCustomer(updatedCust)
        }
    }

    // Expenses
    fun saveExpense(expense: Expense) {
        val updated = _allExpenses.value.toMutableList()
        val index = updated.indexOfFirst { it.id == expense.id }
        if (index >= 0) {
            updated[index] = expense.copy(syncState = "SYNCED")
        } else {
            updated.add(0, expense.copy(syncState = "SYNCED"))
        }
        _allExpenses.value = updated
        updateFilteredState()
    }

    fun deleteExpense(id: String) {
        _allExpenses.value = _allExpenses.value.filter { it.id != id }
        updateFilteredState()
    }

    // Tasks
    fun saveTask(task: TeamTask) {
        val updated = _allTasks.value.toMutableList()
        val index = updated.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            updated[index] = task.copy(syncState = "SYNCED")
        } else {
            updated.add(0, task.copy(syncState = "SYNCED"))
        }
        _allTasks.value = updated
        updateFilteredState()
    }

    fun toggleTaskStatus(taskId: String) {
        val updated = _allTasks.value.map { t ->
            if (t.id == taskId) {
                val next = when (t.status) {
                    TaskStatus.TODO -> TaskStatus.IN_PROGRESS
                    TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                    TaskStatus.COMPLETED -> TaskStatus.TODO
                }
                t.copy(status = next)
            } else t
        }
        _allTasks.value = updated
        updateFilteredState()
    }

    // Sync simulation & verification
    fun triggerSync() {
        repositoryScope.launch {
            _syncState.value = "SYNCING"
            val user = Firebase.auth.currentUser
            val db = firestoreDb
            if (user != null && db != null) {
                try {
                    val uid = user.uid
                    val ws = _currentWorkspace.value
                    db.collection("users").document(uid).collection("workspaces").document(ws.id).set(
                        mapOf(
                            "userId" to uid,
                            "name" to ws.name,
                            "tagline" to ws.tagline,
                            "businessType" to ws.businessType.name,
                            "currencySymbol" to ws.currencySymbol,
                            "currencyCode" to ws.currencyCode,
                            "taxRatePercent" to ws.taxRatePercent
                        )
                    )
                    customers.value.forEach { c ->
                        db.collection("users").document(uid).collection("customers").document(c.id).set(
                            mapOf(
                                "userId" to uid,
                                "workspaceId" to c.workspaceId,
                                "name" to c.name,
                                "phone" to c.phone,
                                "email" to c.email,
                                "company" to c.companyName
                            )
                        )
                    }
                    workRecords.value.forEach { wr ->
                        db.collection("users").document(uid).collection("workRecords").document(wr.id).set(
                            mapOf(
                                "userId" to uid,
                                "workspaceId" to wr.workspaceId,
                                "customerId" to wr.customerId,
                                "title" to wr.title,
                                "status" to wr.recordNumber,
                                "currentStageId" to wr.currentStageId,
                                "price" to wr.totalAmount,
                                "paidAmount" to wr.advancePaid
                            )
                        )
                    }
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, "users/${user.uid}")
                }
            }
            kotlinx.coroutines.delay(600)
            _syncState.value = "ONLINE_SYNCED"
            _pendingSyncCount.value = 0
            _backendConnectionState.value = _backendConnectionState.value.copy(
                lastSyncTimestamp = System.currentTimeMillis()
            )
        }
    }

    companion object {
        @Volatile
        private var instance: BizFlowRepository? = null

        fun getInstance(): BizFlowRepository {
            return instance ?: synchronized(this) {
                instance ?: BizFlowRepository().also { instance = it }
            }
        }

        fun resetInstanceForTesting() {
            synchronized(this) {
                instance = null
            }
        }
    }
}
