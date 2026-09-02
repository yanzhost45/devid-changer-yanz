package com.warungerik.devidchanger.engine

import android.util.Base64

object StringCrypt {

    private const val SEED = 0x53
    private const val MULT = 31
    private const val STEP = 17
    private const val OFFSET = 0x2A

    fun encode(plain: String): String {
        val bytes = plain.toByteArray(Charsets.UTF_8)
        val out = ByteArray(bytes.size)
        for (i in bytes.indices) {
            val key = keyAt(i)
            out[i] = (bytes[i].toInt() xor key).toByte()
        }
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decode(encoded: String): String {
        return try {
            val bytes = try {
                Base64.decode(encoded, Base64.NO_WRAP)
            } catch (e: Throwable) {
                java.util.Base64.getDecoder().decode(encoded)
            }
            val out = ByteArray(bytes.size)
            for (i in bytes.indices) {
                out[i] = (bytes[i].toInt() xor keyAt(i)).toByte()
            }
            String(out, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private fun keyAt(index: Int): Int =
        (SEED * MULT + index * STEP + OFFSET) and 0xFF
}

