package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bizflow.data.BizFlowRepository
import com.example.bizflow.data.SessionManager
import com.example.bizflow.model.BusinessType
import com.example.bizflow.model.ModuleType
import com.example.bizflow.model.Workspace
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SessionManager.clearSession(context, forgetEmail = true)
    }

    @Test
    fun testSessionSaveAndAutoLogin() {
        assertFalse(SessionManager.isAuthenticated(context))

        SessionManager.saveSession(
            context = context,
            email = "founder@apexcorp.com",
            rememberMe = true,
            workspaceId = "ws-apex-99",
            isCustomerPortal = false
        )

        assertTrue(SessionManager.isAuthenticated(context))
        assertTrue(SessionManager.shouldRememberMe(context))
        assertEquals("founder@apexcorp.com", SessionManager.getSavedEmail(context))
        assertEquals("ws-apex-99", SessionManager.getActiveWorkspaceId(context))
        assertFalse(SessionManager.isCustomerPortal(context))

        // On explicit sign out: session is cleared, but saved email is preserved for fast login
        SessionManager.clearSession(context, forgetEmail = false)
        assertFalse(SessionManager.isAuthenticated(context))
        assertTrue(SessionManager.shouldRememberMe(context))
        assertEquals("founder@apexcorp.com", SessionManager.getSavedEmail(context))

        // When forgetting email completely:
        SessionManager.clearSession(context, forgetEmail = true)
        assertEquals("", SessionManager.getSavedEmail(context))
    }

    @Test
    fun testDarkThemePreferencePersistence() {
        // Default is null (system)
        SessionManager.setDarkThemePreference(context, true)
        assertEquals(true, SessionManager.getDarkThemePreference(context))

        SessionManager.setDarkThemePreference(context, false)
        assertEquals(false, SessionManager.getDarkThemePreference(context))
    }

    @Test
    fun testCustomWorkspacePersistence() {
        val testWorkspace = Workspace(
            id = "ws-custom-123",
            name = "Starlight Robotics",
            tagline = "Autonomous IoT solutions",
            businessType = BusinessType.PROFESSIONAL_SERVICES,
            currencySymbol = "$",
            currencyCode = "USD",
            enabledModules = setOf(ModuleType.ORDERS, ModuleType.BILLING),
            email = "team@starlight.io"
        )

        SessionManager.saveCustomWorkspaces(context, listOf(testWorkspace))

        val loaded = SessionManager.loadCustomWorkspaces(context)
        assertEquals(1, loaded.size)
        val loadedWs = loaded.first()
        assertEquals("ws-custom-123", loadedWs.id)
        assertEquals("Starlight Robotics", loadedWs.name)
        assertEquals(BusinessType.PROFESSIONAL_SERVICES, loadedWs.businessType)
        assertEquals("team@starlight.io", loadedWs.email)
    }

    @Test
    fun testRepositoryInitWithPersistentSession() {
        val repository = BizFlowRepository.getInstance()

        SessionManager.saveSession(
            context = context,
            email = "test@company.com",
            rememberMe = true,
            workspaceId = "ws-boutique-004",
            isCustomerPortal = false
        )

        repository.initPersistence(context)
        assertEquals("ws-boutique-004", repository.currentWorkspace.value.id)
    }
}
