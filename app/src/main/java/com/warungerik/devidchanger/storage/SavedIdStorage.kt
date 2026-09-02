package com.warungerik.devidchanger.storage

import android.content.Context
import android.util.Log
import com.warungerik.devidchanger.model.SavedDevId
import org.json.JSONArray
import org.json.JSONObject

class SavedIdStorage(context: Context) {
    private val prefs = context.getSharedPreferences("saved_dev_ids", Context.MODE_PRIVATE)

    fun getSavedIds(): List<SavedDevId> {
        val jsonString = prefs.getString("ids_json", "[]") ?: "[]"
        val list = mutableListOf<SavedDevId>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SavedDevId(
                        id = obj.optString("id"),
                        label = obj.optString("label"),
                        devId = obj.optString("devId"),
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            Log.w("SavedIdStorage", "Failed to parse saved IDs", e)
        }
        return list
    }

    fun saveId(label: String, devId: String): List<SavedDevId> {
        val current = getSavedIds().toMutableList()
        val existingIndex = current.indexOfFirst { it.devId == devId }
        if (existingIndex >= 0) {
            current[existingIndex] = current[existingIndex].copy(
                label = if (label.isNotBlank()) label else current[existingIndex].label,
                timestamp = System.currentTimeMillis()
            )
        } else {
            current.add(0, SavedDevId(label = label.ifBlank { "DevID" }, devId = devId))
        }
        persist(current)
        return current
    }

    fun deleteId(id: String): List<SavedDevId> {
        val current = getSavedIds().filterNot { it.id == id }
        persist(current)
        return current
    }

    private fun persist(list: List<SavedDevId>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("devId", item.devId)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("ids_json", jsonArray.toString()).apply()
    }
}
