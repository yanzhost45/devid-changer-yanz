package com.warungerik.devidchanger.network

import android.util.Log
import com.warungerik.devidchanger.model.MlbbUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object MlbbApiClient {

    private const val TAG = "MlbbApiClient"
    private const val ENDPOINT = "https://gopay.co.id/games/v1/order/user-account"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
    private const val TIMEOUT_MS = 15_000
    private const val MAX_RESPONSE_BYTES = 256 * 1024

    private val USER_ID_REGEX = Regex("^[0-9]{1,20}$")
    private val SERVER_ID_REGEX = Regex("^[0-9]{1,10}$")

    private const val GENERIC_ERROR = "Gagal memeriksa ID. Periksa koneksi lalu coba lagi."
    private const val INVALID_ID_ERROR = "Invalid ID Player or Server ID"

    suspend fun checkAccount(userId: String, serverId: String): MlbbUser =
        withContext(Dispatchers.IO) {
            if (userId.isBlank() || !USER_ID_REGEX.matches(userId)) {
                throw MlbbApiException("User ID tidak valid (harus angka).")
            }
            if (serverId.isBlank() || !SERVER_ID_REGEX.matches(serverId)) {
                throw MlbbApiException("Server ID tidak valid (harus angka).")
            }

            val payload = JSONObject().apply {
                put("code", "MOBILE_LEGENDS")
                put(
                    "data", JSONObject().apply {
                        put("userId", userId)
                        put("zoneId", serverId)
                    }
                )
            }

            var conn: HttpURLConnection? = null
            try {
                conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", USER_AGENT)
                }

                OutputStreamWriterSafe(conn, payload.toString())

                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val body = readLimited(stream)

                val json = try {
                    JSONObject(body)
                } catch (e: Exception) {
                    Log.w(TAG, "Response bukan JSON valid (status=$status)")
                    throw MlbbApiException(INVALID_ID_ERROR)
                }

                val message = json.optString("message", "")
                val data = json.optJSONObject("data")
                val username = data?.optString("username", "") ?: ""

                if (message != "Success" && username.isBlank()) {
                    val serverMsg = json.optString("message", "")
                    throw MlbbApiException(serverMsg.ifBlank { INVALID_ID_ERROR })
                }

                val country = data?.optString("countryOrigin", "")
                    ?.takeIf { it.isNotBlank() }
                    ?.uppercase()
                    ?: "UNKNOWN"

                MlbbUser(username = username, country = country)
            } catch (e: MlbbApiException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "MLBB check failed", e)
                throw MlbbApiException(GENERIC_ERROR)
            } finally {
                conn?.disconnect()
            }
        }

    private fun OutputStreamWriterSafe(conn: HttpURLConnection, text: String) {
        conn.outputStream.use { os ->
            os.write(text.toByteArray(Charsets.UTF_8))
            os.flush()
        }
    }

    private fun readLimited(input: InputStream?): String {
        if (input == null) return ""
        input.use { `in` ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8 * 1024)
            var total = 0
            var read: Int
            while (`in`.read(chunk).also { read = it } != -1) {
                total += read
                if (total > MAX_RESPONSE_BYTES) {
                    throw MlbbApiException(GENERIC_ERROR)
                }
                buffer.write(chunk, 0, read)
            }
            return buffer.toString("UTF-8")
        }
    }
}

class MlbbApiException(message: String) : Exception(message)

