package com.maxnetwork.browser

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DonorsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donors)

        val tvTotal    = findViewById<TextView>(R.id.tvTotal)
        val donorList  = findViewById<ListView>(R.id.donorList)
        val emptyView  = findViewById<LinearLayout>(R.id.emptyView)
        val tvEmpty    = findViewById<TextView>(R.id.tvEmptyText)
        val btnDonate  = findViewById<CardView>(R.id.btnDonate)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        btnDonate.setOnClickListener {
            DonationManager.openDonationPage(this)
        }

        // Yükleniyor durumu
        emptyView.visibility = View.VISIBLE
        donorList.visibility = View.GONE
        tvEmpty.text = "Yükleniyor..."

        DonationManager.fetchDonors { donors, total ->
            runOnUiThread {
                tvTotal.text = total

                if (donors.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    donorList.visibility = View.GONE
                    tvEmpty.text = "Henüz bağış yapılmamış\nİlk sen ol! 💙"
                } else {
                    emptyView.visibility = View.GONE
                    donorList.visibility = View.VISIBLE

                    val adapter = object : android.widget.BaseAdapter() {
                        override fun getCount() = donors.size
                        override fun getItem(p: Int) = donors[p]
                        override fun getItemId(p: Int) = p.toLong()

                        override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
                            val view = convertView
                                ?: layoutInflater.inflate(R.layout.item_donor, parent, false)
                            val donor = donors[pos]
                            view.findViewById<TextView>(R.id.tvDonorName).text = donor.name
                            view.findViewById<TextView>(R.id.tvDonorAmount).text = donor.amount
                            view.findViewById<TextView>(R.id.tvDonorDate).text = donor.date
                            return view
                        }
                    }
                    donorList.adapter = adapter
                }
            }
        }
    }
}