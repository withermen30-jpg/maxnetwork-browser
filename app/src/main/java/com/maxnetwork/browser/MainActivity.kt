package com.maxnetwork.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.StrictMode
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var lockIcon: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton

    private val SERVER_IP = "45.141.148.94"
    private val SERVER_PORT = 1000
    private val SEARCH_PORT = 1002

    private val history = mutableListOf<String>()
    private var historyIndex = -1

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        webView     = findViewById(R.id.webView)
        urlBar      = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        lockIcon    = findViewById(R.id.lockIcon)
        btnBack     = findViewById(R.id.btnBack)
        btnForward  = findViewById(R.id.btnForward)
        btnRefresh  = findViewById(R.id.btnRefresh)

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val prefs = getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            navigateTo(prefs.getString("homepage", "maxnetwork.my") ?: "maxnetwork.my")
        }

        findViewById<LinearLayout>(R.id.navTabs).setOnClickListener {
            takeSnapshotAndOpenTabs()
        }

        findViewById<LinearLayout>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnRefresh.setOnClickListener {
            if (historyIndex >= 0) {
                val current = history[historyIndex]
                val (domain, port) = parseDomainPort(current)
                loadFromServer("GET", domain, "/", "", "", port)
            }
        }

        val btnStar = findViewById<ImageButton>(R.id.btnStar)
        updateStarButton(btnStar)
        btnStar.setOnClickListener { handleStarButton(btnStar) }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, false, false)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.deny()
            }
        }

        setupWebViewClient()
        setupListeners()

        TabManager.init()

        val prefs = getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)

        // Tam ekran kontrolü
        if (prefs.getBoolean("fullscreen", false)) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        when {
            intent.getBooleanExtra("new_tab", false) -> {
                val tab = TabManager.activeTab()
                val homepage = prefs.getString("homepage", "maxnetwork.my") ?: "maxnetwork.my"
                navigateTo(tab?.domain ?: homepage)
            }
            intent.getBooleanExtra("switch_tab", false) -> {
                val tab = TabManager.activeTab()
                if (tab != null && tab.domain.isNotEmpty()) navigateTo(tab.domain)
            }
            intent.getStringExtra("navigate_to") != null -> {
                navigateTo(intent.getStringExtra("navigate_to")!!)
            }
            else -> {
                val homepage = prefs.getString("homepage", "maxnetwork.my") ?: "maxnetwork.my"
                navigateTo(homepage)
            }
        }

        UpdateManager.checkForUpdate(this)
        DonationManager.checkAndShow(this)
    }

    // ==========================================
    // GÜVENLİK SCRİPTİ
    // ==========================================
    private fun getSecurityScript(): String {
        return """
            <script>
            (function() {
                try { window.RTCPeerConnection = undefined; } catch(e) {}
                try { window.webkitRTCPeerConnection = undefined; } catch(e) {}
                try { window.mozRTCPeerConnection = undefined; } catch(e) {}
                try {
                    Object.defineProperty(navigator, 'geolocation', {
                        get: function() { return undefined; }
                    });
                } catch(e) {}
                try {
                    HTMLCanvasElement.prototype.toDataURL = function() { return ''; };
                    HTMLCanvasElement.prototype.toBlob = function() {};
                    CanvasRenderingContext2D.prototype.getImageData = function() {
                        return { data: [] };
                    };
                } catch(e) {}
                try {
                    Object.defineProperty(screen, 'width', { get: function() { return 1920; } });
                    Object.defineProperty(screen, 'height', { get: function() { return 1080; } });
                    Object.defineProperty(screen, 'colorDepth', { get: function() { return 24; } });
                    Object.defineProperty(screen, 'pixelDepth', { get: function() { return 24; } });
                } catch(e) {}
                try {
                    Object.defineProperty(navigator, 'userAgent', {
                        get: function() { return 'MaxNetwork/3.0'; }
                    });
                } catch(e) {}
                try {
                    Object.defineProperty(navigator, 'plugins', {
                        get: function() { return []; }
                    });
                } catch(e) {}
                try {
                    Object.defineProperty(navigator, 'platform', {
                        get: function() { return 'MaxOS'; }
                    });
                } catch(e) {}
                try { navigator.getBattery = undefined; } catch(e) {}
                try {
                    Object.defineProperty(navigator, 'connection', {
                        get: function() { return undefined; }
                    });
                } catch(e) {}
                try {
                    console.log = function() {};
                    console.warn = function() {};
                    console.error = function() {};
                    console.debug = function() {};
                } catch(e) {}
            })();
            </script>
        """.trimIndent()
    }

    // ==========================================
    // WEBVIEW CLIENT
    // ==========================================
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViewClient() {
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()

                if (url.startsWith("max://")) {
                    val withoutScheme = url.removePrefix("max://")
                    val domainPart = withoutScheme.split("/")[0]
                    val pathPart = if (withoutScheme.contains("/"))
                        "/" + withoutScheme.substringAfter("/") else "/"

                    val domain: String
                    val port: Int
                    if (domainPart.contains(":")) {
                        domain = domainPart.split(":")[0]
                        port = domainPart.split(":")[1].toIntOrNull() ?: SERVER_PORT
                    } else {
                        domain = domainPart
                        port = SERVER_PORT
                    }

                    addToHistory(if (port != SERVER_PORT) "$domain:$port" else domain)
                    loadFromServer("GET", domain, pathPart, "", "", port)
                    return true
                }

                if (url.startsWith("http://")) {
                    val withoutHttp = url.removePrefix("http://")
                    val domainEnd = withoutHttp.indexOf("/")
                    val domainAndPort = if (domainEnd != -1) withoutHttp.substring(0, domainEnd) else withoutHttp
                    val path = if (domainEnd != -1) withoutHttp.substring(domainEnd) else "/"

                    val domain: String
                    val port: Int
                    if (domainAndPort.contains(":")) {
                        val parts = domainAndPort.split(":")
                        domain = parts[0]
                        port = parts[1].toIntOrNull() ?: SERVER_PORT
                    } else {
                        domain = domainAndPort
                        port = SERVER_PORT
                    }

                    val queryIdx = path.indexOf("?")
                    val cleanPath = if (queryIdx != -1) path.substring(0, queryIdx) else path
                    val query = if (queryIdx != -1) path.substring(queryIdx + 1) else ""

                    addToHistory(domainAndPort)
                    loadFromServer("GET", domain, cleanPath, query, "", port)
                    return true
                }

                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript("""
                    (function() {
                        try { window.RTCPeerConnection = undefined; } catch(e) {}
                        try { window.webkitRTCPeerConnection = undefined; } catch(e) {}
                    })();
                """.trimIndent(), null)
            }
        }

        // ==========================================
        // JAVASCRIPT KÖPRÜSÜ
        // ==========================================
        webView.addJavascriptInterface(object {

            // Form submit — değişmedi
            @JavascriptInterface
            fun postForm(domain: String, path: String, body: String, port: Int) {
                runOnUiThread {
                    loadFromServer("POST", domain, path, "", body, port)
                }
            }

            // Arama API — max_results ile
            // Kullanım: MaxBridge.search("sorgu", 10)
            @JavascriptInterface
            fun search(query: String, maxResults: Int): String {
                return try {
                    val socket = Socket(SERVER_IP, SEARCH_PORT)
                    socket.soTimeout = 10000
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    writer.println("SEARCH|$query|$maxResults")
                    val response = reader.readLine()
                        ?: """{"status":"error","error":"no_response","message":"Sunucu yanıt vermedi"}"""
                    socket.close()
                    response
                } catch (e: Exception) {
                    """{"status":"error","error":"connection_failed","message":"${e.message}"}"""
                }
            }

            // Arama API — varsayılan 20 sonuç
            // Kullanım: MaxBridge.searchDefault("sorgu")
            @JavascriptInterface
            fun searchDefault(query: String): String {
                return search(query, 20)
            }

        }, "MaxBridge")
    }

    // ==========================================
    // BUTON DİNLEYİCİLERİ
    // ==========================================
    private fun setupListeners() {
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                navigateTo(urlBar.text.toString().trim())
                true
            } else false
        }

        btnBack.setOnClickListener {
            if (historyIndex > 0) {
                historyIndex--
                val prev = history[historyIndex]
                urlBar.setText(prev)
                val (domain, port) = parseDomainPort(prev)
                loadFromServer("GET", domain, "/", "", "", port)
            }
        }

        btnForward.setOnClickListener {
            if (historyIndex < history.size - 1) {
                historyIndex++
                val next = history[historyIndex]
                urlBar.setText(next)
                val (domain, port) = parseDomainPort(next)
                loadFromServer("GET", domain, "/", "", "", port)
            }
        }
    }

    // ==========================================
    // SNAPSHOT & SEKMELER
    // ==========================================
    private fun takeSnapshotAndOpenTabs() {
        val tab = TabManager.activeTab()
        if (tab != null && webView.width > 0 && webView.height > 0) {
            try {
                val bitmap = Bitmap.createBitmap(
                    webView.width, webView.height, Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                webView.draw(canvas)
                tab.snapshot = bitmap
            } catch (e: Exception) { }
        }
        startActivity(Intent(this, TabsActivity::class.java))
    }

    // ==========================================
    // YER İMİ YILDIZ BUTONU
    // ==========================================
    private fun handleStarButton(btnStar: ImageButton) {
        val domain = urlBar.text.toString().trim()
        if (domain.isEmpty()) return

        if (BookmarkManager.isBookmarked(this, domain)) {
            BookmarkManager.removeByDomain(this, domain)
            btnStar.setColorFilter(0xFF445566.toInt())
            Toast.makeText(this, "Yer imi kaldırıldı", Toast.LENGTH_SHORT).show()
        } else {
            val et = EditText(this).apply {
                hint = "Yer imi adı"
                setText(domain)
                setTextColor(0xFFF0F4FF.toInt())
                setPadding(48, 24, 48, 0)
            }
            AlertDialog.Builder(this)
                .setTitle("⭐ Yer İmine Ekle")
                .setView(et)
                .setPositiveButton("Ekle") { _, _ ->
                    BookmarkManager.add(this, et.text.toString().trim(), domain)
                    updateStarButton(btnStar)
                    Toast.makeText(this, "⭐ Eklendi!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("İptal", null)
                .show()
        }
    }

    private fun updateStarButton(btn: ImageButton) {
        val domain = urlBar.text.toString().trim()
        if (BookmarkManager.isBookmarked(this, domain)) {
            btn.setColorFilter(0xFFFFD700.toInt())
        } else {
            btn.setColorFilter(0xFF445566.toInt())
        }
    }

    // ==========================================
    // YARDIMCI FONKSİYONLAR
    // ==========================================
    private fun parseDomainPort(input: String): Pair<String, Int> {
        return if (input.contains(":")) {
            val parts = input.split(":")
            Pair(parts[0], parts[1].toIntOrNull() ?: SERVER_PORT)
        } else {
            Pair(input, SERVER_PORT)
        }
    }

    private fun navigateTo(input: String) {
        val raw = input.trim()
        val cleaned = when {
            raw.startsWith("max://") -> raw.removePrefix("max://").split("/")[0]
            raw.startsWith("http://") -> raw.removePrefix("http://").split("/")[0]
            else -> raw
        }
        val (domain, port) = parseDomainPort(cleaned)
        addToHistory(cleaned)
        loadFromServer("GET", domain, "/", "", "", port)
    }

    private fun addToHistory(input: String) {
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }
        history.add(input)
        historyIndex = history.size - 1
        updateNavButtons()
        HistoryActivity.saveToHistory(this, input)
    }

    private fun updateNavButtons() {
        btnBack.alpha = if (historyIndex > 0) 1f else 0.4f
        btnForward.alpha = if (historyIndex < history.size - 1) 1f else 0.4f
    }

    // ==========================================
    // SUNUCUDAN SAYFA YÜKLEME
    // ==========================================
    private fun loadFromServer(
        method: String,
        domain: String,
        path: String,
        query: String,
        body: String,
        port: Int = SERVER_PORT
    ) {
        val displayName = if (port != SERVER_PORT) "$domain:$port" else domain

        runOnUiThread {
            urlBar.setText(displayName)
            progressBar.visibility = View.VISIBLE
            lockIcon.text = "🔒"
        }

        TabManager.activeTab()?.domain = domain

        Thread {
            val html = try {
                val raw = sendRequest(method, domain, path, query, body, port)
                injectScripts(raw, domain, port)
            } catch (e: Exception) {
                errorPage(domain, e.message ?: "Bağlantı hatası")
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                webView.loadDataWithBaseURL(
                    "http://$domain/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
                updateNavButtons()
                updateStarButton(findViewById(R.id.btnStar))
            }
        }.start()
    }

    // ==========================================
    // SCRIPT ENJEKTE ET
    // ==========================================
    private fun injectScripts(html: String, domain: String, port: Int): String {
        val securityScript = getSecurityScript()

        val formScript = """
            <script>
            document.addEventListener('DOMContentLoaded', function() {
                var forms = document.querySelectorAll('form');
                forms.forEach(function(form) {
                    form.addEventListener('submit', function(e) {
                        e.preventDefault();
                        var data = new FormData(form);
                        var body = '';
                        data.forEach(function(value, key) {
                            body += encodeURIComponent(key) + '=' + encodeURIComponent(value) + '&';
                        });
                        body = body.slice(0, -1);
                        var action = form.getAttribute('action') || '/';
                        MaxBridge.postForm('$domain', action, body, $port);
                    });
                });

                document.querySelectorAll('a[href^="max://"]').forEach(function(a) {
                    a.addEventListener('click', function(e) {
                        e.preventDefault();
                        var href = a.getAttribute('href');
                        window.location.href = 'http://' + href.replace('max://', '');
                    });
                });
            });
            </script>
        """.trimIndent()

        val allScripts = securityScript + formScript

        return if (html.contains("</head>")) {
            html.replace("</head>", "$allScripts</head>")
        } else {
            allScripts + html
        }
    }

    // ==========================================
    // TCP İSTEĞİ
    // ==========================================
    private fun sendRequest(
        method: String,
        domain: String,
        path: String,
        query: String,
        body: String,
        port: Int = SERVER_PORT
    ): String {
        val socket = Socket(SERVER_IP, port)
        val writer = PrintWriter(socket.getOutputStream(), true)
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        val request = "$method|$domain|$path|$query|$body"
        writer.println(request)

        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line).append("\n")
        }

        socket.close()
        return sb.toString()
    }

    // ==========================================
    // HATA SAYFASI
    // ==========================================
    private fun errorPage(domain: String, message: String): String {
        return """
            <html>
            <head><style>
                body { background:#070B14; color:#f0f4ff; font-family:Arial; text-align:center; padding-top:100px; }
                h1 { color:#FF5252; font-size:36px; margin-bottom:16px; }
                p { color:#8899BB; font-size:15px; }
                .domain { color:#4FC3F7; font-size:18px; margin:16px 0; }
                .icon { font-size:64px; margin-bottom:20px; }
            </style></head>
            <body>
                <div class="icon">⚠️</div>
                <h1>Bağlanamadı</h1>
                <p class="domain">$domain</p>
                <p>$message</p>
                <p style="margin-top:40px;color:#445566;font-size:13px;">MaxNetwork Browser v0.3</p>
            </body>
            </html>
        """.trimIndent()
    }

    // ==========================================
    // GERİ TUŞU
    // ==========================================
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && historyIndex > 0) {
            historyIndex--
            val prev = history[historyIndex]
            urlBar.setText(prev)
            val (domain, port) = parseDomainPort(prev)
            loadFromServer("GET", domain, "/", "", "", port)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}