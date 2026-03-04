package com.maxnetwork.browser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private var currentTab = "history"
    private var currentFolderId: String? = null
    private val folderStack = mutableListOf<Pair<String, String>>()

    companion object {
        fun saveToHistory(context: Context, domain: String) {
            if (domain.isBlank()) return
            val prefs = context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            val arr = try {
                JSONArray(prefs.getString("history_log", "[]"))
            } catch (e: Exception) { JSONArray() }

            val entry = JSONObject().apply {
                put("domain", domain)
                put("time", System.currentTimeMillis())
            }
            arr.put(entry)

            // Max 500 kayıt tut
            val trimmed = if (arr.length() > 500) {
                val newArr = JSONArray()
                for (i in arr.length() - 500 until arr.length()) newArr.put(arr.get(i))
                newArr
            } else arr

            prefs.edit().putString("history_log", trimmed.toString()).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val btnBack       = findViewById<ImageButton>(R.id.btnBack)
        val btnAction     = findViewById<ImageButton>(R.id.btnAction)
        val tabHistory    = findViewById<TextView>(R.id.tabHistory)
        val tabBookmarks  = findViewById<TextView>(R.id.tabBookmarks)
        val searchBarCard = findViewById<CardView>(R.id.searchBarCard)
        val searchBar     = findViewById<EditText>(R.id.searchBar)
        val breadcrumb    = findViewById<HorizontalScrollView>(R.id.breadcrumbScroll)
        val listView      = findViewById<ListView>(R.id.listView)
        val emptyView     = findViewById<LinearLayout>(R.id.emptyView)
        val tvEmptyIcon   = findViewById<TextView>(R.id.tvEmptyIcon)
        val tvEmptyText   = findViewById<TextView>(R.id.tvEmptyText)

        btnBack.setOnClickListener { finish() }

        // ==========================================
        // SEKME GEÇİŞİ
        // ==========================================
        fun switchTab(tab: String) {
            currentTab = tab

            if (tab == "history") {
                // Geçmiş sekmesi görünümü
                tabHistory.setTextColor(getColor(R.color.accent))
                tabHistory.setBackgroundResource(R.drawable.bg_tab_active)
                tabBookmarks.setTextColor(getColor(R.color.text_secondary))
                tabBookmarks.setBackgroundResource(R.drawable.bg_tab_inactive)
                searchBarCard.visibility = View.VISIBLE
                breadcrumb.visibility = View.GONE
                btnAction.setImageResource(android.R.drawable.ic_menu_delete)
                btnAction.setColorFilter(getColor(R.color.error))
                tvEmptyIcon.text = "🕐"
                tvEmptyText.text = "Henüz geçmiş yok"
                loadHistory(listView, emptyView, searchBar.text.toString())
            } else {
                // Yer imleri sekmesi görünümü
                tabBookmarks.setTextColor(getColor(R.color.accent))
                tabBookmarks.setBackgroundResource(R.drawable.bg_tab_active)
                tabHistory.setTextColor(getColor(R.color.text_secondary))
                tabHistory.setBackgroundResource(R.drawable.bg_tab_inactive)
                searchBarCard.visibility = View.GONE
                breadcrumb.visibility = View.VISIBLE
                btnAction.setImageResource(android.R.drawable.ic_menu_sort_by_size)
                btnAction.setColorFilter(getColor(R.color.accent))
                tvEmptyIcon.text = "⭐"
                tvEmptyText.text = "Henüz yer imi yok"
                loadBookmarks(listView, emptyView)
            }
        }

        tabHistory.setOnClickListener { switchTab("history") }
        tabBookmarks.setOnClickListener {
            currentFolderId = null
            folderStack.clear()
            switchTab("bookmarks")
        }

        // ==========================================
        // AKSİYON BUTONU
        // ==========================================
        btnAction.setOnClickListener {
            if (currentTab == "history") {
                showDeleteHistoryDialog(listView, emptyView, searchBar)
            } else {
                showBookmarkOptions(listView, emptyView)
            }
        }

        // ==========================================
        // GEÇMİŞ ARAMA
        // ==========================================
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (currentTab == "history") loadHistory(listView, emptyView, s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // İlk yükleme
        switchTab("history")
    }

    // ==========================================
    // GEÇMİŞ YÜKLEME
    // ==========================================
    private fun loadHistory(
        listView: ListView,
        emptyView: LinearLayout,
        filter: String = ""
    ) {
        val prefs = getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
        val arr = try {
            JSONArray(prefs.getString("history_log", "[]"))
        } catch (e: Exception) { JSONArray() }

        data class HistoryEntry(val domain: String, val time: Long)

        val entries = mutableListOf<HistoryEntry>()
        for (i in arr.length() - 1 downTo 0) {
            val obj = arr.getJSONObject(i)
            val domain = obj.getString("domain")
            if (filter.isBlank() || domain.contains(filter, ignoreCase = true)) {
                entries.add(HistoryEntry(domain, obj.getLong("time")))
            }
        }

        if (entries.isEmpty()) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            return
        }

        listView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        // Gruplama
        val now = System.currentTimeMillis()
        val day = 86400000L
        data class ListItem(
            val isHeader: Boolean,
            val headerText: String = "",
            val domain: String = "",
            val time: Long = 0
        )

        val items = mutableListOf<ListItem>()
        var lastGroup = ""

        entries.forEach { entry ->
            val diff = now - entry.time
            val group = when {
                diff < day -> "BUGÜN"
                diff < 2 * day -> "DÜN"
                diff < 7 * day -> "BU HAFTA"
                diff < 30 * day -> "BU AY"
                else -> "DAHA ÖNCE"
            }
            if (group != lastGroup) {
                items.add(ListItem(isHeader = true, headerText = group))
                lastGroup = group
            }
            items.add(ListItem(isHeader = false, domain = entry.domain, time = entry.time))
        }

        val adapter = object : android.widget.BaseAdapter() {
            override fun getCount() = items.size
            override fun getItem(p: Int) = items[p]
            override fun getItemId(p: Int) = p.toLong()
            override fun getViewTypeCount() = 2
            override fun getItemViewType(p: Int) = if (items[p].isHeader) 0 else 1

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = items[position]
                if (item.isHeader) {
                    val tv = TextView(this@HistoryActivity)
                    tv.text = item.headerText
                    tv.setTextColor(getColor(R.color.accent))
                    tv.textSize = 11f
                    tv.setPadding(32, 20, 16, 8)
                    tv.letterSpacing = 0.12f
                    return tv
                }

                val view = convertView ?: layoutInflater.inflate(R.layout.item_history, parent, false)
                view.findViewById<TextView>(R.id.tvDomain).text = item.domain
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                view.findViewById<TextView>(R.id.tvTime).text = sdf.format(Date(item.time))
                return view
            }
        }

        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = items[position]
            if (!item.isHeader) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("navigate_to", item.domain)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        }
    }

    // ==========================================
    // GEÇMİŞ SİLME DİYALOĞU
    // ==========================================
    private fun showDeleteHistoryDialog(
        listView: ListView,
        emptyView: LinearLayout,
        searchBar: EditText
    ) {
        val options = arrayOf("Son 1 Hafta", "Son 1 Ay", "Tüm Geçmiş")
        AlertDialog.Builder(this)
            .setTitle("Geçmişi Temizle")
            .setItems(options) { _, which ->
                val prefs = getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
                val arr = try {
                    JSONArray(prefs.getString("history_log", "[]"))
                } catch (e: Exception) { JSONArray() }

                val cutoff = when (which) {
                    0 -> System.currentTimeMillis() - 7 * 86400000L
                    1 -> System.currentTimeMillis() - 30 * 86400000L
                    else -> Long.MAX_VALUE
                }

                val newArr = JSONArray()
                if (which != 2) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        if (obj.getLong("time") < cutoff) newArr.put(obj)
                    }
                }

                prefs.edit().putString("history_log", newArr.toString()).apply()
                loadHistory(listView, emptyView, searchBar.text.toString())
                Toast.makeText(this, "Geçmiş temizlendi", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // ==========================================
    // YER İMLERİ YÜKLEME
    // ==========================================
    private fun loadBookmarks(listView: ListView, emptyView: LinearLayout) {
        updateBreadcrumb()
        val items = BookmarkManager.getInFolder(this, currentFolderId)

        if (items.isEmpty()) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            return
        }

        listView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        val adapter = object : android.widget.BaseAdapter() {
            override fun getCount() = items.size
            override fun getItem(p: Int) = items[p]
            override fun getItemId(p: Int) = p.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = items[position]
                val view = convertView ?: layoutInflater.inflate(R.layout.item_bookmark, parent, false)
                view.findViewById<TextView>(R.id.tvIcon).text = if (item.isFolder) "📁" else "⭐"
                view.findViewById<TextView>(R.id.tvName).text = item.name
                view.findViewById<TextView>(R.id.tvDomain).text =
                    if (item.isFolder) "Klasör" else item.domain
                return view
            }
        }

        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = items[position]
            if (item.isFolder) {
                folderStack.add(Pair(item.id, item.name))
                currentFolderId = item.id
                loadBookmarks(listView, emptyView)
            } else {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("navigate_to", item.domain)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            showBookmarkItemOptions(items[position], listView, emptyView)
            true
        }
    }

    // ==========================================
    // YER İMİ SEÇENEKLER
    // ==========================================
    private fun showBookmarkOptions(listView: ListView, emptyView: LinearLayout) {
        val options = arrayOf("📁 Yeni Klasör", "🔤 A-Z Sırala", "🕐 En Yeni Sırala")
        AlertDialog.Builder(this)
            .setTitle("Yer İmleri")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val et = EditText(this).apply {
                            hint = "Klasör adı"
                            setTextColor(getColor(R.color.text_primary))
                            setPadding(48, 24, 48, 0)
                        }
                        AlertDialog.Builder(this)
                            .setTitle("Yeni Klasör")
                            .setView(et)
                            .setPositiveButton("Oluştur") { _, _ ->
                                BookmarkManager.addFolder(this, et.text.toString().trim(), currentFolderId)
                                loadBookmarks(listView, emptyView)
                            }
                            .setNegativeButton("İptal", null)
                            .show()
                    }
                    1 -> {
                        BookmarkManager.setSortOrder(this, "az")
                        loadBookmarks(listView, emptyView)
                    }
                    2 -> {
                        BookmarkManager.setSortOrder(this, "newest")
                        loadBookmarks(listView, emptyView)
                    }
                }
            }
            .show()
    }

    private fun showBookmarkItemOptions(
        item: Bookmark,
        listView: ListView,
        emptyView: LinearLayout
    ) {
        val options = arrayOf("✏️ Düzenle", "📁 Klasöre Taşı", "🗑️ Sil")
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val et = EditText(this).apply {
                            setText(item.name)
                            setTextColor(getColor(R.color.text_primary))
                            setPadding(48, 24, 48, 0)
                        }
                        AlertDialog.Builder(this)
                            .setTitle("Düzenle")
                            .setView(et)
                            .setPositiveButton("Kaydet") { _, _ ->
                                BookmarkManager.update(this, item.id, et.text.toString().trim())
                                loadBookmarks(listView, emptyView)
                            }
                            .setNegativeButton("İptal", null)
                            .show()
                    }
                    1 -> {
                        val folders = BookmarkManager.getFolders(this)
                        val names = arrayOf("Ana Dizin") + folders.map { it.name }.toTypedArray()
                        AlertDialog.Builder(this)
                            .setTitle("Klasöre Taşı")
                            .setItems(names) { _, idx ->
                                val targetId = if (idx == 0) null else folders[idx - 1].id
                                BookmarkManager.move(this, item.id, targetId)
                                loadBookmarks(listView, emptyView)
                            }
                            .show()
                    }
                    2 -> {
                        BookmarkManager.delete(this, item.id)
                        loadBookmarks(listView, emptyView)
                    }
                }
            }
            .show()
    }

    // ==========================================
    // BREADCRUMB GÜNCELLEMESİ
    // ==========================================
    private fun updateBreadcrumb() {
        val container = findViewById<LinearLayout>(R.id.breadcrumbContainer)
        container.removeAllViews()

        fun addCrumb(text: String, onClick: () -> Unit) {
            val tv = TextView(this)
            tv.text = text
            tv.setTextColor(getColor(R.color.accent))
            tv.textSize = 13f
            tv.setPadding(4, 0, 4, 0)
            tv.setOnClickListener { onClick() }
            container.addView(tv)
        }

        fun addSeparator() {
            val tv = TextView(this)
            tv.text = " › "
            tv.setTextColor(getColor(R.color.text_hint))
            tv.textSize = 13f
            container.addView(tv)
        }

        addCrumb("Ana Dizin") {
            currentFolderId = null
            folderStack.clear()
            loadBookmarks(
                findViewById(R.id.listView),
                findViewById(R.id.emptyView)
            )
        }

        folderStack.forEachIndexed { index, (_, name) ->
            addSeparator()
            addCrumb(name) {
                while (folderStack.size > index + 1) folderStack.removeAt(folderStack.size - 1)
                currentFolderId = folderStack.lastOrNull()?.first
                loadBookmarks(
                    findViewById(R.id.listView),
                    findViewById(R.id.emptyView)
                )
            }
        }
    }
}