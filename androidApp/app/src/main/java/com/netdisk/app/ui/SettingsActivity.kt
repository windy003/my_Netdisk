package com.netdisk.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.netdisk.app.R
import com.netdisk.app.storage.PreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var serverUrlInput: TextInputEditText
    private lateinit var portInput: TextInputEditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        preferencesManager = PreferencesManager(this)

        // Initialize views
        serverUrlInput = findViewById(R.id.serverUrlInput)
        portInput = findViewById(R.id.portInput)
        saveButton = findViewById(R.id.saveButton)

        // Load current settings
        loadSettings()

        // Save button click listener
        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        // 获取当前保存的完整URL
        val fullUrl = preferencesManager.getServerUrl()

        // 如果是完整URL（包含http://或https://），直接显示
        if (fullUrl.startsWith("http://") || fullUrl.startsWith("https://")) {
            serverUrlInput.setText(fullUrl)
            portInput.setText("")  // 完整URL模式下端口留空
        } else {
            // 传统模式：显示主机和端口
            serverUrlInput.setText(preferencesManager.getServerHost())
            portInput.setText(preferencesManager.getServerPort().toString())
        }
    }

    private fun saveSettings() {
        val input = serverUrlInput.text.toString().trim()
        val portStr = portInput.text.toString().trim()

        if (input.isEmpty()) {
            Toast.makeText(this, "Server URL cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // 检测是否为完整URL（包含http://或https://）
        if (input.startsWith("http://") || input.startsWith("https://")) {
            // 完整URL模式（支持cloudflare tunnel等）
            var fullUrl = input

            // 移除末尾的斜杠
            if (fullUrl.endsWith("/")) {
                fullUrl = fullUrl.substring(0, fullUrl.length - 1)
            }

            // 保存完整URL
            preferencesManager.saveFullServerUrl(fullUrl)
            Toast.makeText(this, "已保存完整URL: $fullUrl", Toast.LENGTH_SHORT).show()
        } else {
            // 传统的主机+端口模式
            var host = extractHost(input)
            serverUrlInput.setText(host)

            if (portStr.isEmpty()) {
                Toast.makeText(this, "Port cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }

            val port = portStr.toIntOrNull()
            if (port == null || port < 1 || port > 65535) {
                Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show()
                return
            }

            // Validate URL format (basic validation)
            if (!isValidHost(host)) {
                Toast.makeText(this, getString(R.string.invalid_url), Toast.LENGTH_SHORT).show()
                return
            }

            // Save settings
            preferencesManager.saveServerConfig(host, port)
            Toast.makeText(this, getString(R.string.server_config_saved), Toast.LENGTH_SHORT).show()
        }

        // Set result and go back to main activity
        setResult(RESULT_OK)
        finish()
    }

    private fun extractHost(input: String): String {
        var host = input
        // 移除 http:// 或 https:// 前缀
        if (host.startsWith("http://")) {
            host = host.substring(7)
        } else if (host.startsWith("https://")) {
            host = host.substring(8)
        }
        // 移除路径部分
        val slashIndex = host.indexOf('/')
        if (slashIndex > 0) {
            host = host.substring(0, slashIndex)
        }
        // 移除端口部分
        val colonIndex = host.indexOf(':')
        if (colonIndex > 0) {
            host = host.substring(0, colonIndex)
        }
        return host
    }

    private fun isValidHost(host: String): Boolean {
        // Simple validation - check if it's an IP address or domain name
        val ipPattern = Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
        val domainPattern = Regex("^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$")

        return host == "localhost" || ipPattern.matches(host) || domainPattern.matches(host)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
