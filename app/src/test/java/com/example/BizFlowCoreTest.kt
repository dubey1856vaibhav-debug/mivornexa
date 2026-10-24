package com.example

import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.data.EmailDispatcher
import com.example.bizflow.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BizFlowCoreTest {

    private lateinit var repository: BizFlowRepository

    @Before
    fun setUp() {
        BizFlowRepository.resetInstanceForTesting()
        repository = BizFlowRepository.getInstance()
    }

    @Test
    fun testWorkspaceSwitching() {
        // Switch to Boutique workspace
        repository.selectWorkspace("ws-boutique-004")
        val currentWs = repository.currentWorkspace.value
        assertEquals(BusinessType.BESPOKE_BOUTIQUE, currentWs.businessType)
        assertEquals("₹", currentWs.currencySymbol)
        assertEquals("INR", currentWs.currencyCode)

        // Verify filtered work records only contain records for this workspace
        val orders = repository.workRecords.value
        assertTrue(orders.all { it.workspaceId == "ws-boutique-004" })

        // Switch back to Retail workspace
        repository.selectWorkspace("ws-retail-001")
        assertEquals(BusinessType.RETAIL_ECOMMERCE, repository.currentWorkspace.value.businessType)
        assertEquals("$", repository.currentWorkspace.value.currencySymbol)
    }

    @Test
    fun testDuplicateCustomerDetection() {
        repository.selectWorkspace("ws-retail-001")
        // Existing customer: Sarah Jenkins (+1 (555) 234-8901, sarah.j@example.com)
        val dupByPhone = repository.findPossibleDuplicateCustomers(
            name = "Sarah J.",
            phone = "555-234-8901",
            email = "different@email.com"
        )
        assertFalse(dupByPhone.isEmpty())
        assertEquals("Sarah Jenkins", dupByPhone.first().name)

        val dupByEmail = repository.findPossibleDuplicateCustomers(
            name = "Another Name",
            phone = "1234567890",
            email = "sarah.j@example.com"
        )
        assertFalse(dupByEmail.isEmpty())

        val noDup = repository.findPossibleDuplicateCustomers(
            name = "Completely New User",
            phone = "9998887776",
            email = "brandnew@test.com"
        )
        assertTrue(noDup.isEmpty())
    }

    @Test
    fun testFinancialCalculationAndPaymentRecording() {
        repository.selectWorkspace("ws-retail-001")
        val initialInvoices = repository.invoices.value
        val testInvoice = initialInvoices.first()
        val originalBalance = testInvoice.balanceDue

        assertTrue("Balance due should be positive", originalBalance > 0.0)

        val paymentAmount = 200.0
        val payment = Payment(
            invoiceId = testInvoice.id,
            workspaceId = testInvoice.workspaceId,
            amount = paymentAmount,
            method = PaymentMethod.CASH,
            referenceNumber = "TEST-REC-01"
        )

        repository.recordPayment(testInvoice.id, payment)

        val updatedInvoice = repository.invoices.value.find { it.id == testInvoice.id }!!
        assertEquals(originalBalance - paymentAmount, updatedInvoice.balanceDue, 0.01)
        assertEquals(testInvoice.totalPaid + paymentAmount, updatedInvoice.totalPaid, 0.01)
    }

    @Test
    fun testInventoryStockAdjustment() {
        repository.selectWorkspace("ws-retail-001")
        val product = repository.products.value.first { it.type == ItemType.PRODUCT }
        val startingStock = product.currentStock

        repository.adjustStock(product.id, 10.0, "Restock shipment")
        val updatedProduct = repository.products.value.find { it.id == product.id }!!
        assertEquals(startingStock + 10.0, updatedProduct.currentStock, 0.01)
    }

    @Test
    fun testTaskStatusCycle() {
        repository.selectWorkspace("ws-retail-001")
        val task = repository.tasks.value.first()
        val initialStatus = task.status

        repository.toggleTaskStatus(task.id)
        val updatedTask = repository.tasks.value.find { it.id == task.id }!!
        assertNotEquals(initialStatus, updatedTask.status)
    }

    @Test
    fun testEmailDispatcherDraftIntegrity() {
        val ws = repository.currentWorkspace.value
        val record = WorkRecord(
            workspaceId = ws.id,
            recordNumber = "ORD-TEST-99",
            customerId = "c-1",
            customerName = "Jane Doe",
            title = "Test Fabrication Order",
            currentStageId = "stage-1",
            totalAmount = 500.0,
            balanceDue = 250.0
        )

        val draft = EmailDispatcher.composeWorkStatusEmail(record, "Processing", ws)
        assertEquals("Jane Doe", draft.recipientName)
        assertTrue(draft.subject.contains("ORD-TEST-99"))
        assertTrue(draft.bodyHtml.contains("Processing"))
        assertTrue(draft.bodyHtml.contains("Jane Doe"))
    }

    @Test
    fun testUpdateWorkspaceAndBusinessTypeChange() {
        val testWs = repository.createCompanyAccount(
            fullName = "Tester",
            email = "tester@test.com",
            companyName = "Initial Retail Co",
            businessType = BusinessType.RETAIL_ECOMMERCE
        )
        assertEquals(BusinessType.RETAIL_ECOMMERCE, testWs.businessType)

        val updated = testWs.copy(
            name = "iFix Express Diagnostic Labs",
            tagline = "Ultra-fast board repair and gadget fixes",
            businessType = BusinessType.REPAIR_TECH,
            currencySymbol = "£",
            currencyCode = "GBP",
            taxRatePercent = 20.0,
            enabledModules = setOf(ModuleType.ORDERS, ModuleType.INVENTORY, ModuleType.BILLING)
        )

        val resultWs = repository.updateWorkspace(updated, adaptWorkflowsAndFields = true)

        assertEquals("iFix Express Diagnostic Labs", resultWs.name)
        assertEquals(BusinessType.REPAIR_TECH, resultWs.businessType)
        assertEquals("£", resultWs.currencySymbol)
        assertEquals("GBP", resultWs.currencyCode)
        assertEquals(20.0, resultWs.taxRatePercent, 0.01)

        // Check stages were adapted to Repair Tech stages
        val stageNames = resultWs.workflowStages.map { it.name }
        assertTrue(stageNames.contains("Intake & Check-In"))
        assertTrue(stageNames.contains("Diagnostics & Quote"))
        assertTrue(stageNames.contains("Bench Repair"))

        // Check custom fields were adapted
        val fieldLabels = resultWs.customFields.map { it.label }
        assertTrue(fieldLabels.contains("Device Serial / IMEI"))

        // Check active workspace in repository is also updated
        assertEquals("iFix Express Diagnostic Labs", repository.currentWorkspace.value.name)
        assertEquals(BusinessType.REPAIR_TECH, repository.currentWorkspace.value.businessType)

        // Restore active workspace to ws-retail-001
        repository.selectWorkspace("ws-retail-001")
    }

    @Test
    fun testAllBusinessTypesHaveValidConfigurations() {
        assertEquals(13, BusinessType.values().size)
        BusinessType.values().forEach { bType ->
            val stages = repository.getDefaultStagesForType(bType)
            val fields = repository.getDefaultCustomFieldsForType(bType)
            assertTrue("Stages for ${bType.name} should have at least 4 pipeline stages", stages.size >= 4)
            assertTrue("Stages for ${bType.name} should include a completed stage", stages.any { it.isCompleted })
            assertTrue("Fields for ${bType.name} should not be empty", fields.isNotEmpty())
            assertTrue("Plural record name for ${bType.name} should not be empty", bType.workRecordPlural.isNotBlank())
            assertTrue("Singular record name for ${bType.name} should not be empty", bType.workRecordLabel.isNotBlank())
        }
    }
}
