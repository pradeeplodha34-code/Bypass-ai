package com.bypass.ide.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.bypass.ide.model.NetworkShieldStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private const val TAG = "NetworkShieldService"

class NetworkShieldService(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
) {

    suspend fun checkShieldStatus(evictSockets: Boolean = false): NetworkShieldStatus =
        withContext(Dispatchers.IO) {
            if (evictSockets) {
                runCatching {
                    client.connectionPool.evictAll()
                }.onFailure {
                    Log.w(TAG, "Failed to evict connection pool", it)
                }
            }

            val transportLabel = detectTransportLabel()

            // 1. Connectivity probe via Google generate_204
            var isConnected = false
            var latencyMs: Long? = null
            runCatching {
                val start = System.currentTimeMillis()
                val req = Request.Builder()
                    .url("https://www.google.com/generate_204")
                    .build()
                client.newCall(req).execute().use { resp ->
                    isConnected = resp.code == 204 || resp.isSuccessful
                    latencyMs = System.currentTimeMillis() - start
                }
            }.onFailure {
                Log.w(TAG, "Connectivity check failed: ${it.message}")
            }

            // 2. Public IP probe across fallback endpoints
            var publicIp = "Unavailable"
            var ipProvider = ""
            val ipEndpoints = listOf(
                "api.ipify.org" to "https://api.ipify.org",
                "icanhazip.com" to "https://icanhazip.com",
                "ifconfig.me" to "https://ifconfig.me/ip"
            )
            for ((provider, url) in ipEndpoints) {
                val fetched = runCatching {
                    val req = Request.Builder()
                        .url(url)
                        .header("User-Agent", "BypassIDE/1.0")
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) resp.body?.string()?.trim() else null
                    }
                }.getOrNull()
                if (!fetched.isNullOrBlank() && fetched.length <= 45) {
                    publicIp = fetched
                    ipProvider = provider
                    break
                }
            }

            val message = when {
                evictSockets && isConnected ->
                    "Socket pool evicted & DNS refreshed. Verified IP: $publicIp"
                isConnected ->
                    "Pipeline Online • $transportLabel • Latency ${latencyMs ?: 0}ms"
                else ->
                    "Offline or restricted network. Check connection and retry."
            }

            NetworkShieldStatus(
                publicIp = publicIp,
                ipProvider = ipProvider,
                isConnected = isConnected,
                transportLabel = transportLabel,
                latencyMs = latencyMs,
                isChecking = false,
                statusMessage = message,
                lastCheckedAt = System.currentTimeMillis()
            )
        }

    private fun detectTransportLabel(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Unknown"
        val activeNetwork = cm.activeNetwork ?: return "Disconnected"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "Disconnected"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Tunnel"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (LTE/5G)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Network Active"
        }
    }
}
