package com.maxnetwork.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject
import java.net.URL

data class Donor(
    val name: String,
    val amount: String,
    val date: String
)

object DonationManager {

    private const val DONATIONS_URL =
        "https://raw.githubusercontent.com/withermen30-jpg/maxnetwork-updates/main/donations.json"
    private const val DONATION_LINK = "https://buymeacoffee.com/maxnetwork"
    private const val PREF_KEY = "donation_dialog_disabled"

    fun checkAndShow(context: Context) {
        val prefs = context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_KEY, false)) return

        (context as? androidx.appcompat.app.AppCompatActivity)?.runOnUiThread {
            showDonationDialog(context)
        }
    }

    private fun showDonationDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("💙 Projeye Destek Ol")
            .setMessage(
                "Bağış yaparak projeye destek sağlayabilir ve ilerletebilirsiniz.\n\n" +
                "Sizin bağışlarınızla ayaktayız! Her katkı MaxNetwork'ün " +
                "daha iyi olması için kullanılır. 🚀"
            )
            .setPositiveButton("💙 Bağış Yap") { _, _ ->
                openDonationPage(context)
            }
            .setNegativeButton("Sonra", null)
            .setNeutralButton("Bir Daha Gösterme") { _, _ ->
                context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean(PREF_KEY, true).apply()
            }
            .setCancelable(true)
            .show()
    }

    fun fetchDonors(onResult: (List<Donor>, String) -> Unit) {
        Thread {
            try {
                val json = URL(DONATIONS_URL).readText()
                val obj = JSONObject(json)
                val total = obj.getString("total")
                val arr = obj.getJSONArray("donors")
                val list = mutableListOf<Donor>()
                for (i in 0 until arr.length()) {
                    val d = arr.getJSONObject(i)
                    list.add(
                        Donor(
                            name = d.getString("name"),
                            amount = d.getString("amount"),
                            date = d.getString("date")
                        )
                    )
                }
                onResult(list, total)
            } catch (e: Exception) {
                onResult(emptyList(), "₺0")
            }
        }.start()
    }

    fun openDonationPage(context: Context) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_LINK)))
    }
}