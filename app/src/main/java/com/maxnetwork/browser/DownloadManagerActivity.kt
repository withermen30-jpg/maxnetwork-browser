package com.maxnetwork.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class DownloadItem(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val downloadedAt: Long,
    val mimeType: String
)

class DownloadManagerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        val downloadList = findViewById<ListView>(R.id.downloadList)
        val emptyView = findViewById<LinearLayout>(R.id.emptyView)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnClearAll).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Tümünü Sil")
                .setMessage("Tüm indirme geçmişi silinsin mi?")
                .setPositiveButton("Sil") { _, _ ->
                    clearAllDownloads()
                    loadDownloads(downloadList, emptyView)
                }
                .setNegativeButton("İptal", null)
                .show()
        }

        loadDownloads(downloadList, emptyView)
    }

    private fun loadDownloads(listView: ListView, emptyView: LinearLayout) {
        val items = getDownloads()

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

            override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView
                    ?: layoutInflater.inflate(R.layout.item_download, parent, false)
                val item = items[pos]

                view.findViewById<TextView>(R.id.tvFileName).text = item.fileName
                view.findViewById<TextView>(R.id.tvFileSize).text = formatSize(item.fileSize)
                view.findViewById<TextView>(R.id.tvFileDate).text =
                    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                        .format(Date(item.downloadedAt))
                view.findViewById<TextView>(R.id.tvFileIcon).text = getFileIcon(item.mimeType)

                // Dosyaya tıkla — aç
                view.setOnClickListener {
                    try {
                        val file = File(item.filePath)
                        val uri = Uri.fromFile(file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, item.mimeType)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@DownloadManagerActivity,
                            "Dosya açılamadı",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // Sil
                view.findViewById<TextView>(R.id.btnDeleteItem).setOnClickListener {
                    removeDownload(item.filePath)
                    loadDownloads(listView, emptyView)
                }

                return view
            }
        }

        listView.adapter = adapter
    }

    // ==========================================
    // YARDIMCI FONKSİYONLAR
    // ==========================================
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }

    private fun getFileIcon(mimeType: String): String {
        return when {
            mimeType.startsWith("image") -> "🖼️"
            mimeType.startsWith("video") -> "🎬"
            mimeType.startsWith("audio") -> "🎵"
            mimeType.contains("pdf") -> "📕"
            mimeType.contains("zip") || mimeType.contains("rar") -> "🗜️"
            mimeType.contains("apk") -> "📦"
            else -> "📄"
        }
    }

    // ==========================================
    // VERİ YÖNETİMİ — SharedPreferences
    // ==========================================
    companion object {
        private const val KEY = "downloads_log"

        fun saveDownload(context: Context, item: DownloadItem) {
            val prefs = context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            val arr = try {
                JSONArray(prefs.getString(KEY, "[]"))
            } catch (e: Exception) { JSONArray() }

            val obj = JSONObject().apply {
                put("fileName", item.fileName)
                put("filePath", item.filePath)
                put("fileSize", item.fileSize)
                put("downloadedAt", item.downloadedAt)
                put("mimeType", item.mimeType)
            }
            arr.put(obj)
            prefs.edit().putString(KEY, arr.toString()).apply()
        }
    }

    private fun getDownloads(): List<DownloadItem> {
        val prefs = getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
        val arr = try {
            JSONArray(prefs.getString(KEY, "[]"))
        } catch (e: Exception) { JSONArray() }

        val list = mutableListOf<DownloadItem>()
        for (i in arr.length() - 1 downTo 0) {
            val obj = arr.getJSONObject(i)
            list.add(
                DownloadItem(
                    fileName = obj.getString("fileName"),
                    filePath = obj.getString("filePath"),
                    fileSize = obj.getLong("fileSize"),
                    downloadedAt = obj.getLong("downloadedAt"),
                    mimeType = obj.getString("mimeType")
                )
            )
        }
        return list
    }

    private fun removeDownload(filePath: String) {
        val prefs = getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
        val arr = try {
            JSONArray(prefs.getString(KEY, "[]"))
        } catch (e: Exception) { JSONArray() }

        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getString("filePath") != filePath) newArr.put(obj)
        }
        prefs.edit().putString(KEY, newArr.toString()).apply()
    }

    private fun clearAllDownloads() {
        getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            .edit().putString(KEY, "[]").apply()
    }
}