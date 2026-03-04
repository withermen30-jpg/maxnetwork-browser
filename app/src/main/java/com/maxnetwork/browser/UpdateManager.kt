package com.maxnetwork.browser

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject
import java.net.URL

object UpdateManager {

    private const val VERSION_URL =
        "https://raw.githubusercontent.com/withermen30-jpg/maxnetwork-updates/main/version.json"
    private const val CURRENT_VERSION_CODE = 4

    fun checkForUpdate(context: Context) {
        Thread {
            try {
                val json = URL(VERSION_URL).readText()
                val obj = JSONObject(json)
                val latestCode = obj.getInt("version_code")
                val latestName = obj.getString("version")
                val whatsNew = obj.optString("whats_new", "")
                val apkUrl = obj.getString("apk_url")
                val forceUpdate = obj.optBoolean("force_update", false)

                if (latestCode > CURRENT_VERSION_CODE) {
                    (context as? androidx.appcompat.app.AppCompatActivity)?.runOnUiThread {
                        showUpdateDialog(context, latestName, whatsNew, apkUrl, forceUpdate)
                    }
                }
            } catch (e: Exception) {
                // Güncelleme sunucusuna ulaşılamazsa sessizce geç
            }
        }.start()
    }

    private fun showUpdateDialog(
        context: Context,
        version: String,
        whatsNew: String,
        apkUrl: String,
        forceUpdate: Boolean
    ) {
        val msg = buildString {
            append("v$version sürümü mevcut!\n\n")
            if (whatsNew.isNotBlank()) {
                append("🆕 Yenilikler:\n$whatsNew")
            }
        }

        val builder = AlertDialog.Builder(context)
            .setTitle("🚀 Güncelleme Var!")
            .setMessage(msg)
            .setPositiveButton("İndir ve Kur") { _, _ ->
                downloadApk(context, apkUrl, version)
            }

        if (!forceUpdate) {
            builder.setNegativeButton("Sonra", null)
        }

        builder.setCancelable(!forceUpdate)
        builder.show()
    }

    private fun downloadApk(context: Context, apkUrl: String, version: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                setTitle("MaxNetwork v$version")
                setDescription("Güncelleme indiriliyor...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "maxnetwork_v${version}.apk"
                )
                setMimeType("application/vnd.android.package-archive")
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            android.widget.Toast.makeText(
                context,
                "İndirme başladı, bildirimlerden takip edebilirsin.",
                android.widget.Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            // İndirme başlamazsa tarayıcıya yönlendir
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
            context.startActivity(intent)
        }
    }
}