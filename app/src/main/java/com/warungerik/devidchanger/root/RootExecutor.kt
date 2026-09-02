package com.warungerik.devidchanger.root

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootExecutor {

    private var cachedSuPrefix: Array<String>? = null

    fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    fun execute(command: String): CommandResult {
        cachedSuPrefix?.let { prefix ->
            val result = runCmd(prefix, command)
            if (result.success || result.exitCode == 0) return result
        }

        val prefixes = listOf(
            arrayOf("su", "-mm", "-c"),
            arrayOf("su", "--mount-master", "-c"),
            arrayOf("su", "-c")
        )

        for (prefix in prefixes) {
            val res = runCmd(prefix, command)
            if (res.success || res.exitCode == 0) {
                cachedSuPrefix = prefix
                return res
            }
        }

        return runInteractiveSu(command)
    }


    private fun runCmd(prefix: Array<String>, command: String): CommandResult {
        return try {
            val fullCmd = ArrayList<String>()
            fullCmd.addAll(prefix)
            fullCmd.add(command)

            val process = ProcessBuilder(fullCmd).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val errorOutput = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                errorOutput.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            CommandResult(
                success = (exitCode == 0),
                output = output.toString().trim(),
                error = errorOutput.toString().trim(),
                exitCode = exitCode
            )
        } catch (e: Exception) {
            CommandResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Exec exception",
                exitCode = -1
            )
        }
    }

    private fun runInteractiveSu(command: String): CommandResult {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val errorOutput = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                errorOutput.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            CommandResult(
                success = (exitCode == 0),
                output = output.toString().trim(),
                error = errorOutput.toString().trim(),
                exitCode = exitCode
            )
        } catch (e: Exception) {
            CommandResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Interactive su exception",
                exitCode = -1
            )
        }
    }

    fun isSuAvailable(): Boolean {
        val res = execute("id")
        return res.output.contains("uid=0") || res.success
    }

    fun readFile(path: String): String? {
        val res = execute("cat \"$path\"")
        return if (res.output.isNotEmpty() && !res.output.contains("No such file")) res.output else null
    }

    fun writeFile(path: String, content: String): Boolean {
        val escaped = content.replace("'", "'\\''")
        val res = execute("printf '%s' '$escaped' > \"$path\"")
        return res.success || res.exitCode == 0
    }

    fun fileExists(path: String): Boolean {
        val res = execute("[ -f \"$path\" ] && echo 'EXISTS'")
        return res.output.contains("EXISTS")
    }

    fun directoryExists(path: String): Boolean {
        val res = execute("[ -d \"$path\" ] && echo 'EXISTS'")
        return res.output.contains("EXISTS")
    }
}

data class CommandResult(
    val success: Boolean,
    val output: String,
    val error: String,
    val exitCode: Int
)
