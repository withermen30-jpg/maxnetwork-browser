package com.maxnetwork.browser

object TabManager {

    private val tabs = mutableListOf<Tab>()
    private var activeTabIndex = 0
    private var nextId = 1

    fun init() {
        if (tabs.isEmpty()) {
            tabs.add(Tab(id = nextId++, domain = ""))
            activeTabIndex = 0
        }
    }

    fun activeTab(): Tab? {
        return if (tabs.isNotEmpty()) tabs[activeTabIndex] else null
    }

    fun allTabs(): List<Tab> = tabs.toList()

    fun addTab(domain: String = ""): Tab {
        val tab = Tab(id = nextId++, domain = domain)
        tabs.add(tab)
        activeTabIndex = tabs.size - 1
        return tab
    }

    fun switchTo(index: Int) {
        if (index in tabs.indices) {
            activeTabIndex = index
        }
    }

    fun closeTab(index: Int) {
        if (tabs.size <= 1) return
        tabs.removeAt(index)
        activeTabIndex = when {
            activeTabIndex >= tabs.size -> tabs.size - 1
            else -> activeTabIndex
        }
    }

    fun count(): Int = tabs.size

    fun activeIndex(): Int = activeTabIndex
}