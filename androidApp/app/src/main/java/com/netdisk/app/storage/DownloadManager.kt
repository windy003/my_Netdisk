package com.netdisk.app.storage

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import com.netdisk.app.models.DownloadRecord
import com.netdisk.app.models.DownloadStatus
import java.io.File

class NetdiskDownloadManager(private val context: Context) {

    private val historyManager = DownloadHistoryManager(context)

    fun enqueueDownload(url: String, filename: String): Long {
        // Get authentication cookies
        val cookies = getCookieHeader(url)

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(filename)
            setDescription("Downloading from Netdisk")
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

            // Save to Downloads/Netdisk directory
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Netdisk/$filename"
            )

            // Add authentication cookies
            if (cookies.isNotEmpty()) {
                addRequestHeader("Cookie", cookies)
            }

            // Allow both WiFi and mobile data
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or
                DownloadManager.Request.NETWORK_MOBILE
            )

            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Save download record to history
        val filePath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Netdisk/$filename"
        ).absolutePath

        val record = DownloadRecord(
            downloadId = downloadId,
            filename = filename,
            url = url,
            filePath = filePath,
            status = DownloadStatus.PENDING
        )
        historyManager.addRecord(record)

        return downloadId
    }

    private fun getCookieHeader(url: String): String {
        return CookieManager.getInstance().getCookie(url) ?: ""
    }
}
