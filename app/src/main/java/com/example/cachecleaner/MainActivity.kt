package com.example.cachecleaner

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private lateinit var sizeText: TextView
    private lateinit var clearButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(48), dp(24), dp(24))
        }

        val title = TextView(this).apply {
            text = "Dọn bộ nhớ đệm"
            textSize = 26f
            gravity = Gravity.CENTER
        }

        sizeText = TextView(this).apply {
            text = "Đang tính dung lượng…"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(24))
        }

        clearButton = Button(this).apply {
            text = "Xóa bộ nhớ đệm của app này"
            setOnClickListener { clearOwnCache() }
        }

        val note = TextView(this).apply {
            text = "Android không cho app thường xóa cache của app khác. " +
                "Để dọn app khác, dùng các nút bên dưới để mở màn hình cài đặt hệ thống, " +
                "chọn app rồi bấm \"Xóa bộ nhớ đệm\"."
            textSize = 14f
            setPadding(0, dp(32), 0, dp(12))
        }

        val storageButton = Button(this).apply {
            text = "Mở cài đặt Bộ nhớ"
            setOnClickListener {
                openSettings(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            }
        }

        val appsButton = Button(this).apply {
            text = "Mở danh sách ứng dụng"
            setOnClickListener {
                openSettings(Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS))
            }
        }

        val detailsButton = Button(this).apply {
            text = "Mở thông tin app này"
            setOnClickListener {
                openSettings(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    )
                )
            }
        }

        root.addView(title)
        root.addView(sizeText)
        root.addView(clearButton)
        root.addView(note)
        root.addView(storageButton)
        root.addView(appsButton)
        root.addView(detailsButton)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refreshSize()
    }

    private fun cacheRoots(): List<File> =
        listOfNotNull(cacheDir, externalCacheDir)

    private fun refreshSize() {
        Thread {
            val total = cacheRoots().sumOf { dirSize(it) }
            runOnUiThread {
                sizeText.text = "Bộ nhớ đệm của app: " + Formatter.formatFileSize(this, total)
            }
        }.start()
    }

    private fun clearOwnCache() {
        clearButton.isEnabled = false
        Thread {
            val before = cacheRoots().sumOf { dirSize(it) }
            cacheRoots().forEach { deleteChildren(it) }
            val after = cacheRoots().sumOf { dirSize(it) }
            val freed = (before - after).coerceAtLeast(0L)
            runOnUiThread {
                clearButton.isEnabled = true
                Toast.makeText(
                    this,
                    "Đã giải phóng " + Formatter.formatFileSize(this, freed),
                    Toast.LENGTH_SHORT
                ).show()
                refreshSize()
            }
        }.start()
    }

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Máy không hỗ trợ mở màn hình này", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dirSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { dirSize(it) } ?: 0L
    }

    private fun deleteChildren(dir: File) {
        dir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
