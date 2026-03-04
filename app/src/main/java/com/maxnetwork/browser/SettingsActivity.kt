package com.maxnetwork.browser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
        val tvHomepage = findViewById<TextView>(R.id.tvHomepage)
        val tvTheme = findViewById<TextView>(R.id.tvTheme)
        val tvLanguage = findViewById<TextView>(R.id.tvLanguage)
        val switchFullscreen = findViewById<Switch>(R.id.switchFullscreen)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Mevcut değerleri göster
        tvHomepage.text = prefs.getString("homepage", "maxnetwork.my")
        tvTheme.text = if (ThemeManager.getTheme(this) == ThemeManager.DARK) "🌙 Koyu" else "☀️ Açık"
        tvLanguage.text = when (LocaleManager.getLanguage(this)) {
            "tr" -> "🇹🇷 Türkçe"
            "zh" -> "🇨🇳 简体中文"
            else -> "🇬🇧 English"
        }
        switchFullscreen.isChecked = prefs.getBoolean("fullscreen", false)

        // Ana sayfa
        findViewById<LinearLayout>(R.id.btnHomepage).setOnClickListener {
            val et = EditText(this).apply {
                hint = "örn: maxnetwork.my"
                setText(prefs.getString("homepage", "maxnetwork.my"))
                setTextColor(getColor(R.color.text_primary))
                setHintTextColor(getColor(R.color.text_hint))
                setPadding(48, 24, 48, 0)
            }
            AlertDialog.Builder(this)
                .setTitle("🏠 Ana Sayfayı Değiştir")
                .setView(et)
                .setPositiveButton("Kaydet") { _, _ ->
                    val newHome = et.text.toString().trim()
                    if (newHome.isNotBlank()) {
                        prefs.edit().putString("homepage", newHome).apply()
                        tvHomepage.text = newHome
                        Toast.makeText(this, "Ana sayfa güncellendi!", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("İptal", null)
                .show()
        }

        // Geçmişi temizle
        findViewById<LinearLayout>(R.id.btnClearHistory).setOnClickListener {
            val options = arrayOf("Son 1 Hafta", "Son 1 Ay", "Tüm Geçmiş")
            AlertDialog.Builder(this)
                .setTitle("Geçmişi Temizle")
                .setItems(options) { _, which ->
                    val arr = org.json.JSONArray()
                    if (which != 2) {
                        val cutoff = when (which) {
                            0 -> System.currentTimeMillis() - 7 * 86400000L
                            else -> System.currentTimeMillis() - 30 * 86400000L
                        }
                        val old = try {
                            org.json.JSONArray(prefs.getString("history_log", "[]"))
                        } catch (e: Exception) { org.json.JSONArray() }
                        for (i in 0 until old.length()) {
                            val obj = old.getJSONObject(i)
                            if (obj.getLong("time") < cutoff) arr.put(obj)
                        }
                    }
                    prefs.edit().putString("history_log", arr.toString()).apply()
                    Toast.makeText(this, "Geçmiş temizlendi!", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Tema değiştir
        findViewById<LinearLayout>(R.id.btnTheme).setOnClickListener {
            val options = arrayOf("🌙  Koyu", "☀️  Açık")
            AlertDialog.Builder(this)
                .setTitle("Tema Seç")
                .setItems(options) { _, which ->
                    val theme = if (which == 0) ThemeManager.DARK else ThemeManager.LIGHT
                    ThemeManager.setTheme(this, theme)
                    tvTheme.text = if (which == 0) "🌙 Koyu" else "☀️ Açık"
                    Toast.makeText(this, "Tema değiştirildi, uygulamayı yeniden başlatın.", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Tam ekran
        switchFullscreen.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("fullscreen", isChecked).apply()
            if (isChecked) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
            Toast.makeText(this, "Tam ekran ${if (isChecked) "açıldı" else "kapatıldı"}", Toast.LENGTH_SHORT).show()
        }

        // Dil değiştir
        findViewById<LinearLayout>(R.id.btnLanguage).setOnClickListener {
            val options = arrayOf("🇬🇧  English", "🇹🇷  Türkçe", "🇨🇳  简体中文")
            val codes = arrayOf("en", "tr", "zh")
            AlertDialog.Builder(this)
                .setTitle("Dil Seç")
                .setItems(options) { _, which ->
                    LocaleManager.setLocale(this, codes[which])
                    tvLanguage.text = options[which]
                    Toast.makeText(this, "Dil değiştirildi, uygulamayı yeniden başlatın.", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // İndirmeler
        findViewById<LinearLayout>(R.id.btnDownloads).setOnClickListener {
            startActivity(Intent(this, DownloadManagerActivity::class.java))
        }

        // Bağış yap
        findViewById<LinearLayout>(R.id.btnGoDonate).setOnClickListener {
            DonationManager.openDonationPage(this)
        }

        // Bağışçılar
        findViewById<LinearLayout>(R.id.btnDonors).setOnClickListener {
            startActivity(Intent(this, DonorsActivity::class.java))
        }

        // Altyapı
        findViewById<LinearLayout>(R.id.btnInfrastructure).setOnClickListener {
            startActivity(Intent(this, InfrastructureActivity::class.java))
        }

        // GitHub
        findViewById<LinearLayout>(R.id.btnDevGithub).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://github.com/withermen30-jpg")))
        }

        // Instagram
        findViewById<LinearLayout>(R.id.btnDevInstagram).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://www.instagram.com/maxnetwork_tarayici")))
        }

        // E-posta
        findViewById<LinearLayout>(R.id.btnDevEmail).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("mailto:kerem@kerem.site")))
        }
    }
}