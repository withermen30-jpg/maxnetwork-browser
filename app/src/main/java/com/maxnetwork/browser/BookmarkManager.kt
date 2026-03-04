package com.maxnetwork.browser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Bookmark(
    val id: String,
    val name: String,
    val domain: String,
    val folderId: String?,
    val createdAt: Long,
    val isFolder: Boolean
)

object BookmarkManager {

    private const val KEY = "bookmarks_data"
    private const val SORT_KEY = "bookmarks_sort"

    private fun getAll(context: Context): MutableList<Bookmark> {
        val prefs = context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
        val arr = try {
            JSONArray(prefs.getString(KEY, "[]"))
        } catch (e: Exception) { JSONArray() }

        val list = mutableListOf<Bookmark>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Bookmark(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    domain = obj.optString("domain", ""),
                    folderId = if (obj.isNull("folderId")) null else obj.getString("folderId"),
                    createdAt = obj.getLong("createdAt"),
                    isFolder = obj.getBoolean("isFolder")
                )
            )
        }
        return list
    }

    private fun saveAll(context: Context, list: List<Bookmark>) {
        val arr = JSONArray()
        list.forEach { b ->
            val obj = JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("domain", b.domain)
                if (b.folderId != null) put("folderId", b.folderId) else put("folderId", JSONObject.NULL)
                put("createdAt", b.createdAt)
                put("isFolder", b.isFolder)
            }
            arr.put(obj)
        }
        context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun add(context: Context, name: String, domain: String, folderId: String? = null) {
        val list = getAll(context)
        list.add(
            Bookmark(
                id = System.currentTimeMillis().toString(),
                name = name.ifBlank { domain },
                domain = domain,
                folderId = folderId,
                createdAt = System.currentTimeMillis(),
                isFolder = false
            )
        )
        saveAll(context, list)
    }

    fun addFolder(context: Context, name: String, parentId: String? = null) {
        val list = getAll(context)
        list.add(
            Bookmark(
                id = "folder_${System.currentTimeMillis()}",
                name = name.ifBlank { "Klasör" },
                domain = "",
                folderId = parentId,
                createdAt = System.currentTimeMillis(),
                isFolder = true
            )
        )
        saveAll(context, list)
    }

    fun delete(context: Context, id: String) {
        val list = getAll(context)
        // Klasörse içindekileri de sil
        val toDelete = mutableSetOf(id)
        var changed = true
        while (changed) {
            changed = false
            list.forEach { b ->
                if (b.folderId != null && toDelete.contains(b.folderId) && !toDelete.contains(b.id)) {
                    toDelete.add(b.id)
                    changed = true
                }
            }
        }
        saveAll(context, list.filter { !toDelete.contains(it.id) })
    }

    fun update(context: Context, id: String, newName: String) {
        val list = getAll(context)
        val idx = list.indexOfFirst { it.id == id }
        if (idx != -1) {
            val old = list[idx]
            list[idx] = old.copy(name = newName)
            saveAll(context, list)
        }
    }

    fun move(context: Context, id: String, targetFolderId: String?) {
        val list = getAll(context)
        val idx = list.indexOfFirst { it.id == id }
        if (idx != -1) {
            list[idx] = list[idx].copy(folderId = targetFolderId)
            saveAll(context, list)
        }
    }

    fun isBookmarked(context: Context, domain: String): Boolean {
        return getAll(context).any { !it.isFolder && it.domain == domain }
    }

    fun removeByDomain(context: Context, domain: String) {
        val list = getAll(context)
        saveAll(context, list.filter { it.isFolder || it.domain != domain })
    }

    fun getInFolder(context: Context, folderId: String?): List<Bookmark> {
        val list = getAll(context)
        val sort = getSortOrder(context)
        val filtered = list.filter { it.folderId == folderId }
        return when (sort) {
            "az" -> filtered.sortedWith(compareByDescending<Bookmark> { it.isFolder }.thenBy { it.name })
            else -> filtered.sortedWith(compareByDescending<Bookmark> { it.isFolder }.thenByDescending { it.createdAt })
        }
    }

    fun getFolders(context: Context): List<Bookmark> {
        return getAll(context).filter { it.isFolder }
    }

    fun setSortOrder(context: Context, order: String) {
        context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            .edit().putString(SORT_KEY, order).apply()
    }

    private fun getSortOrder(context: Context): String {
        return context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            .getString(SORT_KEY, "newest") ?: "newest"
    }
}