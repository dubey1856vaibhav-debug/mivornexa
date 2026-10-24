package com.example.bizflow.data

import android.content.Context
import android.content.SharedPreferences
import com.example.bizflow.model.*
import org.json.JSONArray
import org.json.JSONObject

object SessionManager {
    private const val PREF_NAME = "mivornexa_session_prefs"

    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_REMEMBER_ME = "key_remember_me"
    private const val KEY_SAVED_EMAIL = "key_saved_email"
    private const val KEY_ACTIVE_WORKSPACE_ID = "key_active_workspace_id"
    private const val KEY_IS_CUSTOMER_PORTAL = "key_is_customer_portal"
    private const val KEY_SAVED_CUSTOMER_ID = "key_saved_customer_id"
    private const val KEY_DARK_THEME_OVERRIDE = "key_dark_theme_override" // "system", "dark", "light"
    private const val KEY_CUSTOM_WORKSPACES = "key_custom_workspaces"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isAuthenticated(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun shouldRememberMe(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER_ME, false)
    }

    fun getSavedEmail(context: Context): String {
        return getPrefs(context).getString(KEY_SAVED_EMAIL, "") ?: ""
    }

    fun getActiveWorkspaceId(context: Context): String? {
        return getPrefs(context).getString(KEY_ACTIVE_WORKSPACE_ID, null)
    }

    fun isCustomerPortal(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_CUSTOMER_PORTAL, false)
    }

    fun getSavedCustomerId(context: Context): String? {
        return getPrefs(context).getString(KEY_SAVED_CUSTOMER_ID, null)
    }

    fun saveSession(
        context: Context,
        email: String,
        rememberMe: Boolean,
        workspaceId: String,
        isCustomerPortal: Boolean = false,
        customerId: String? = null
    ) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            putString(KEY_SAVED_EMAIL, email.trim())
            putString(KEY_ACTIVE_WORKSPACE_ID, workspaceId)
            putBoolean(KEY_IS_CUSTOMER_PORTAL, isCustomerPortal)
            putString(KEY_SAVED_CUSTOMER_ID, customerId)
            apply()
        }
    }

    fun clearSession(context: Context, forgetEmail: Boolean = false) {
        val keepEmail = if (!forgetEmail && shouldRememberMe(context)) getSavedEmail(context) else ""
        val keepRemember = if (!forgetEmail && shouldRememberMe(context)) true else false

        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putBoolean(KEY_REMEMBER_ME, keepRemember)
            putString(KEY_SAVED_EMAIL, keepEmail)
            remove(KEY_ACTIVE_WORKSPACE_ID)
            putBoolean(KEY_IS_CUSTOMER_PORTAL, false)
            remove(KEY_SAVED_CUSTOMER_ID)
            apply()
        }
    }

    fun getDarkThemePreference(context: Context): Boolean? {
        val value = getPrefs(context).getString(KEY_DARK_THEME_OVERRIDE, "system") ?: "system"
        return when (value) {
            "dark" -> true
            "light" -> false
            else -> null
        }
    }

    fun setDarkThemePreference(context: Context, isDark: Boolean) {
        getPrefs(context).edit().putString(KEY_DARK_THEME_OVERRIDE, if (isDark) "dark" else "light").apply()
    }

    // Persist user-created custom companies / workspaces so data is double-checked and never lost
    fun saveCustomWorkspaces(context: Context, workspaces: List<Workspace>) {
        try {
            val jsonArray = JSONArray()
            workspaces.forEach { ws ->
                val obj = JSONObject().apply {
                    put("id", ws.id)
                    put("name", ws.name)
                    put("tagline", ws.tagline)
                    put("businessType", ws.businessType.name)
                    put("currencySymbol", ws.currencySymbol)
                    put("currencyCode", ws.currencyCode)
                    put("locale", ws.locale)
                    put("email", ws.email)
                    put("address", ws.address)
                    put("phone", ws.phone)
                    put("taxRatePercent", ws.taxRatePercent)

                    val modulesArray = JSONArray()
                    ws.enabledModules.forEach { modulesArray.put(it.name) }
                    put("modules", modulesArray)

                    val stagesArray = JSONArray()
                    ws.workflowStages.forEach { stage ->
                        val stageObj = JSONObject().apply {
                            put("id", stage.id)
                            put("name", stage.name)
                            put("colorHex", stage.colorHex)
                            put("isCompleted", stage.isCompleted)
                        }
                        stagesArray.put(stageObj)
                    }
                    put("stages", stagesArray)

                    val customFieldsArray = JSONArray()
                    ws.customFields.forEach { cf ->
                        val cfObj = JSONObject().apply {
                            put("id", cf.id)
                            put("label", cf.label)
                            put("fieldType", cf.fieldType)
                            put("isRequired", cf.isRequired)
                            val optArray = JSONArray()
                            cf.options.forEach { optArray.put(it) }
                            put("options", optArray)
                        }
                        customFieldsArray.put(cfObj)
                    }
                    put("customFields", customFieldsArray)
                }
                jsonArray.put(obj)
            }
            getPrefs(context).edit().putString(KEY_CUSTOM_WORKSPACES, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCustomWorkspaces(context: Context): List<Workspace> {
        val jsonStr = getPrefs(context).getString(KEY_CUSTOM_WORKSPACES, null) ?: return emptyList()
        val result = mutableListOf<Workspace>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val bType = try {
                    BusinessType.valueOf(obj.getString("businessType"))
                } catch (e: Exception) {
                    BusinessType.RETAIL_ECOMMERCE
                }

                val modules = mutableSetOf<ModuleType>()
                val modulesArray = obj.optJSONArray("modules")
                if (modulesArray != null) {
                    for (m in 0 until modulesArray.length()) {
                        try {
                            modules.add(ModuleType.valueOf(modulesArray.getString(m)))
                        } catch (_: Exception) {}
                    }
                }

                val stages = mutableListOf<WorkStage>()
                val stagesArray = obj.optJSONArray("stages")
                if (stagesArray != null) {
                    for (s in 0 until stagesArray.length()) {
                        val sObj = stagesArray.getJSONObject(s)
                        stages.add(
                            WorkStage(
                                id = sObj.getString("id"),
                                name = sObj.getString("name"),
                                colorHex = sObj.getString("colorHex"),
                                isCompleted = sObj.optBoolean("isCompleted", false)
                            )
                        )
                    }
                }

                val customFields = mutableListOf<CustomFieldDef>()
                val cfArray = obj.optJSONArray("customFields")
                if (cfArray != null) {
                    for (c in 0 until cfArray.length()) {
                        val cObj = cfArray.getJSONObject(c)
                        val opts = mutableListOf<String>()
                        val optsArray = cObj.optJSONArray("options")
                        if (optsArray != null) {
                            for (o in 0 until optsArray.length()) {
                                opts.add(optsArray.getString(o))
                            }
                        }
                        customFields.add(
                            CustomFieldDef(
                                id = cObj.optString("id"),
                                label = cObj.getString("label"),
                                fieldType = cObj.optString("fieldType", "TEXT"),
                                isRequired = cObj.optBoolean("isRequired", false),
                                options = opts
                            )
                        )
                    }
                }

                result.add(
                    Workspace(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        tagline = obj.optString("tagline", ""),
                        businessType = bType,
                        currencySymbol = obj.optString("currencySymbol", "$"),
                        currencyCode = obj.optString("currencyCode", "USD"),
                        locale = obj.optString("locale", "en-US"),
                        enabledModules = modules,
                        customFields = customFields,
                        workflowStages = stages,
                        currentUserRole = UserRole.OWNER,
                        address = obj.optString("address", "123 Business Way"),
                        phone = obj.optString("phone", "+1 555-0199"),
                        email = obj.optString("email", ""),
                        taxRatePercent = obj.optDouble("taxRatePercent", 8.25)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
