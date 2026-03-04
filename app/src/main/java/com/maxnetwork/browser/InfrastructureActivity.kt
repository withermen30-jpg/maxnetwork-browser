package com.maxnetwork.browser

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.net.InetSocketAddress
import java.net.Socket

class InfrastructureActivity : AppCompatActivity() {

    private val SERVER_IP = "45.141.148.94"
    private val SERVER_PORT = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_infrastructure)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // İlk açılışta ping ölç
        measurePing()

        // Yenile butonu
        findViewById<CardView>(R.id.btnRefreshPing).setOnClickListener {
            measurePing()
        }
    }

    private fun measurePing() {
        val tvPing    = findViewById<TextView>(R.id.tvPing)
        val tvStatus  = findViewById<TextView>(R.id.tvStatus)
        val badgeStatus = findViewById<CardView>(R.id.badgeStatus)
        val dotStatus = findViewById<android.view.View>(R.id.dotStatus)

        // Ölçülüyor durumu
        runOnUiThread {
            tvPing.text = "..."
            tvPing.setTextColor(getColor(R.color.text_secondary))
            tvStatus.text = getString(R.string.infra_checking)
            tvStatus.setTextColor(getColor(R.color.text_secondary))
            badgeStatus.setCardBackgroundColor(getColor(R.color.bg_input))
        }

        Thread {
            val pingResult = ping()

            runOnUiThread {
                if (pingResult >= 0) {
                    // Çevrimiçi
                    tvPing.text = "$pingResult"
                    tvPing.setTextColor(when {
                        pingResult < 100 -> getColor(R.color.success)
                        pingResult < 300 -> getColor(R.color.accent)
                        else -> getColor(R.color.error)
                    })
                    tvStatus.text = getString(R.string.infra_online)
                    tvStatus.setTextColor(getColor(R.color.success))
                    badgeStatus.setCardBackgroundColor(0x1A00E676.toInt())
                    dotStatus.setBackgroundResource(R.drawable.bg_dot_active)
                } else {
                    // Çevrimdışı
                    tvPing.text = "—"
                    tvPing.setTextColor(getColor(R.color.error))
                    tvStatus.text = getString(R.string.infra_offline)
                    tvStatus.setTextColor(getColor(R.color.error))
                    badgeStatus.setCardBackgroundColor(0x1AFF5252.toInt())
                    dotStatus.setBackgroundResource(R.drawable.bg_dot_offline)
                }
            }
        }.start()
    }

    private fun ping(): Long {
        return try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress(SERVER_IP, SERVER_PORT), 3000)
            val elapsed = System.currentTimeMillis() - start
            socket.close()
            elapsed
        } catch (e: Exception) {
            -1L
        }
    }
}