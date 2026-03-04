package com.maxnetwork.browser

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class WelcomeActivity : AppCompatActivity() {

    private var selectedLang = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Daha önce dil seçildiyse direkt ana sayfaya git
        if (!LocaleManager.isFirstLaunch(this)) {
            LocaleManager.applyLocale(this, LocaleManager.getLanguage(this))
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_welcome)

        val langTR = findViewById<CardView>(R.id.langTR)
        val langEN = findViewById<CardView>(R.id.langEN)
        val langZH = findViewById<CardView>(R.id.langZH)
        val checkTR = findViewById<TextView>(R.id.checkTR)
        val checkEN = findViewById<TextView>(R.id.checkEN)
        val checkZH = findViewById<TextView>(R.id.checkZH)
        val btnContinue = findViewById<CardView>(R.id.btnContinue)

        // Varsayılan seçim EN
        selectLang("en", langTR, langEN, langZH, checkTR, checkEN, checkZH)

        langTR.setOnClickListener {
            selectLang("tr", langTR, langEN, langZH, checkTR, checkEN, checkZH)
        }
        langEN.setOnClickListener {
            selectLang("en", langTR, langEN, langZH, checkTR, checkEN, checkZH)
        }
        langZH.setOnClickListener {
            selectLang("zh", langTR, langEN, langZH, checkTR, checkEN, checkZH)
        }

        btnContinue.setOnClickListener {
            LocaleManager.setLocale(this, selectedLang)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun selectLang(
        lang: String,
        langTR: CardView, langEN: CardView, langZH: CardView,
        checkTR: TextView, checkEN: TextView, checkZH: TextView
    ) {
        selectedLang = lang

        // Hepsini sıfırla
        langTR.setCardBackgroundColor(getColor(R.color.bg_input))
        langEN.setCardBackgroundColor(getColor(R.color.bg_input))
        langZH.setCardBackgroundColor(getColor(R.color.bg_input))
        checkTR.visibility = View.GONE
        checkEN.visibility = View.GONE
        checkZH.visibility = View.GONE

        // Seçileni vurgula
        when (lang) {
            "tr" -> {
                langTR.setCardBackgroundColor(getColor(R.color.bg_card))
                checkTR.visibility = View.VISIBLE
            }
            "en" -> {
                langEN.setCardBackgroundColor(getColor(R.color.bg_card))
                checkEN.visibility = View.VISIBLE
            }
            "zh" -> {
                langZH.setCardBackgroundColor(getColor(R.color.bg_card))
                checkZH.visibility = View.VISIBLE
            }
        }
    }
}