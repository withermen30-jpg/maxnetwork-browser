package com.maxnetwork.browser

import android.graphics.Bitmap

data class Tab(
    val id: Int,
    var domain: String,
    var snapshot: Bitmap? = null
)