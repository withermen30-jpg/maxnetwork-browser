package com.maxnetwork.browser

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TabsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)

        val recycler = findViewById<RecyclerView>(R.id.tabsRecycler)
        val tvTabCount = findViewById<TextView>(R.id.tvTabCount)
        val btnNewTab = findViewById<CardView>(R.id.btnNewTab)

        recycler.layoutManager = LinearLayoutManager(this)

        fun refreshList() {
            val count = TabManager.count()
            tvTabCount.text = "$count Sekme"
            recycler.adapter = TabAdapter(
                tabs = TabManager.allTabs(),
                activeIndex = TabManager.activeIndex(),
                onTabClick = { index ->
                    TabManager.switchTo(index)
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("switch_tab", true)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                },
                onTabClose = { index ->
                    TabManager.closeTab(index)
                    refreshList()
                }
            )
        }

        refreshList()

        btnNewTab.setOnClickListener {
            TabManager.addTab()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("new_tab", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    inner class TabAdapter(
        private val tabs: List<Tab>,
        private val activeIndex: Int,
        private val onTabClick: (Int) -> Unit,
        private val onTabClose: (Int) -> Unit
    ) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {

        inner class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cardTab: CardView = view.findViewById(R.id.cardTab)
            val tabSnapshot: android.widget.ImageView = view.findViewById(R.id.tabSnapshot)
            val tabPlaceholder: LinearLayout = view.findViewById(R.id.tabPlaceholder)
            val tabPlaceholderDomain: TextView = view.findViewById(R.id.tabPlaceholderDomain)
            val tabDomain: TextView = view.findViewById(R.id.tabDomain)
            val btnCloseTab: TextView = view.findViewById(R.id.btnCloseTab)
            val tabActiveIndicator: View = view.findViewById(R.id.tabActiveIndicator)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
            val view = layoutInflater.inflate(R.layout.item_tab, parent, false)
            return TabViewHolder(view)
        }

        override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
            val tab = tabs[position]
            val isActive = position == activeIndex

            holder.tabDomain.text = if (tab.domain.isEmpty()) "Yeni Sekme" else tab.domain

            // Aktif sekme vurgusu
            if (isActive) {
                holder.tabActiveIndicator.visibility = View.VISIBLE
                holder.cardTab.setCardBackgroundColor(getColor(R.color.bg_input))
            } else {
                holder.tabActiveIndicator.visibility = View.GONE
                holder.cardTab.setCardBackgroundColor(getColor(R.color.bg_card))
            }

            // Snapshot
            if (tab.snapshot != null) {
                holder.tabSnapshot.setImageBitmap(tab.snapshot)
                holder.tabSnapshot.visibility = View.VISIBLE
                holder.tabPlaceholder.visibility = View.GONE
            } else {
                holder.tabSnapshot.visibility = View.GONE
                holder.tabPlaceholder.visibility = View.VISIBLE
                holder.tabPlaceholderDomain.text = if (tab.domain.isEmpty()) "Yeni Sekme" else tab.domain
            }

            holder.cardTab.setOnClickListener { onTabClick(position) }
            holder.btnCloseTab.setOnClickListener { onTabClose(position) }
        }

        override fun getItemCount() = tabs.size
    }
}