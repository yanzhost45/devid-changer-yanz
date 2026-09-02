package com.warungerik.devidchanger.engine

import com.warungerik.devidchanger.model.GameVersion
import com.warungerik.devidchanger.root.RootExecutor
import com.warungerik.devidchanger.ui.theme.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DevIdChanger {

    private fun getPossibleDataDirs(packageName: String): List<String> {
        return listOf(
            "/data/data/$packageName",
            "/data/user/0/$packageName"
        )
    }

    fun getActualDataDir(version: GameVersion): String? {
        for (dir in getPossibleDataDirs(version.packageName)) {
            if (RootExecutor.directoryExists(dir)) {
                return dir
            }
        }
        return null
    }

    fun getActualPrefsDir(version: GameVersion): String? {
        val dataDir = getActualDataDir(version) ?: return null
        val prefsDir = "$dataDir/shared_prefs"
        return if (RootExecutor.directoryExists(prefsDir)) prefsDir else dataDir
    }

    fun getActualFilesDir(version: GameVersion): String? {
        val dataDir = getActualDataDir(version) ?: return null
        val filesDir = "$dataDir/files"
        return if (RootExecutor.directoryExists(filesDir)) filesDir else dataDir
    }

    fun isGameFolderExists(version: GameVersion): Boolean {
        return getActualDataDir(version) != null
    }

    fun getPlayerPrefsFilePath(version: GameVersion): String? {
        val prefsDir = getActualPrefsDir(version) ?: return null
        val file1 = "$prefsDir/${version.packageName}.v2.playerprefs.xml"
        if (RootExecutor.fileExists(file1)) return file1

        val findRes = RootExecutor.execute("ls ${RootExecutor.shellQuote("$prefsDir/*.playerprefs.xml")} 2>/dev/null")
        if (findRes.output.isNotEmpty() && !findRes.output.contains("No such file")) {
            return findRes.output.split("\n").firstOrNull { it.trim().isNotEmpty() }?.trim()
        }
        return file1
    }

    fun getSharedPrefsFilePath(version: GameVersion): String? {
        val prefsDir = getActualPrefsDir(version) ?: return null
        return "$prefsDir/__SharedPreference__.xml"
    }

    fun getCurrentJsonId(version: GameVersion): String? {
        val prefsFile = getPlayerPrefsFilePath(version) ?: return null
        val fileContent = RootExecutor.readFile(prefsFile) ?: return null

        val keysToTry = listOf("JsonDeviceID", "__Java_JsonDeviceID__", "__CachedRealAdvertisingID__")
        for (key in keysToTry) {
            val regex = Regex("""name="${key}"[^>]*>([^<]*)<\/string>""")
            val match = regex.find(fileContent)
            if (match != null && match.groupValues[1].isNotEmpty()) {
                return match.groupValues[1]
            }
        }
        return null
    }

    fun stopGame(version: GameVersion, strings: AppStrings, logger: (String) -> Unit) {
        logger(strings.logStoppingGame(version.displayName, version.packageName))
        val qPkg = RootExecutor.shellQuote(version.packageName)
        RootExecutor.execute("am force-stop $qPkg")
        Thread.sleep(1000)

        val pidsResult = RootExecutor.execute("ps -A | grep $qPkg | awk '{print \$2}'")
        if (pidsResult.success && pidsResult.output.isNotEmpty()) {
            val pids = pidsResult.output.split("\n").map { it.trim() }.filter { it.matches(Regex("^[0-9]+$")) }
            for (pid in pids) {
                RootExecutor.execute("kill -9 $pid")
            }
        }
    }

    fun backupFiles(version: GameVersion, strings: AppStrings, logger: (String) -> Unit): String? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupBaseDir = "/storage/emulated/0/Tyasimut/${version.packageName}"
        val currentBackupDir = "$backupBaseDir/$timeStamp"

        logger(strings.logCreatingBackup)
        RootExecutor.execute("mkdir -p ${RootExecutor.shellQuote(currentBackupDir)}")

        val playerPrefsFile = getPlayerPrefsFilePath(version)
        val sharedPrefsFile = getSharedPrefsFilePath(version)

        if (playerPrefsFile != null && RootExecutor.fileExists(playerPrefsFile)) {
            RootExecutor.execute("cp ${RootExecutor.shellQuote(playerPrefsFile)} ${RootExecutor.shellQuote("$currentBackupDir/")}")
        }
        if (sharedPrefsFile != null && RootExecutor.fileExists(sharedPrefsFile)) {
            RootExecutor.execute("cp ${RootExecutor.shellQuote(sharedPrefsFile)} ${RootExecutor.shellQuote("$currentBackupDir/")}")
        }

        if (version.isUS) {
            val filesDir = getActualFilesDir(version)
            if (filesDir != null) {
                val encFile = "$filesDir/mlsdk_deviceinformation_us"
                val rawFile = "$filesDir/mlsdk_deviceinformation_us_raw"
                if (RootExecutor.fileExists(encFile)) {
                    RootExecutor.execute("cp ${RootExecutor.shellQuote(encFile)} ${RootExecutor.shellQuote("$currentBackupDir/")}")
                }
                if (RootExecutor.fileExists(rawFile)) {
                    RootExecutor.execute("cp ${RootExecutor.shellQuote(rawFile)} ${RootExecutor.shellQuote("$currentBackupDir/")}")
                }
            }
        }

        logger(strings.logBackupSuccess(currentBackupDir))
        return currentBackupDir
    }

    fun injectDeviceId(version: GameVersion, newJsonId: String, strings: AppStrings, logger: (String) -> Unit): Boolean {
        val sanitizedId = sanitizeDeviceId(newJsonId)
        if (sanitizedId.isEmpty() || sanitizedId != newJsonId) {
            logger(strings.logInputInvalid)
            return false
        }

        val extractedAdid = if (sanitizedId.length >= 36) sanitizedId.takeLast(36) else sanitizedId

        logger(strings.logTargetJsonId(sanitizedId))

        var extractedPrefix = ""
        var extractedHex = ""

        if (version.isUS) {
            if (sanitizedId.length != 84) {
                logger(strings.logWarning84Char)
            }
            extractedPrefix = if (sanitizedId.length >= 32) sanitizedId.substring(0, 32) else ""
            extractedHex = if (sanitizedId.length >= 48) sanitizedId.substring(32, 48) else ""
            logger(strings.logSplitPrefix(extractedPrefix))
            logger(strings.logSplitHex(extractedHex))
        }

        logger(strings.logTargetAdid(extractedAdid))
        logger(strings.logProcessingTargetInjection)

        val playerPrefsFile = getPlayerPrefsFilePath(version)
        val sharedPrefsFile = getSharedPrefsFilePath(version)

        if (playerPrefsFile != null) {
            replaceOrInsertXmlString(playerPrefsFile, "__CachedRealAdvertisingID__", extractedAdid)
            replaceOrInsertXmlString(playerPrefsFile, "__Java_JsonDeviceID__", sanitizedId)
            replaceOrInsertXmlString(playerPrefsFile, "JsonDeviceID", sanitizedId)
        } else {
            logger(strings.logErrPlayerPrefsNotFound)
        }

        if (sharedPrefsFile != null) {
            replaceOrInsertXmlString(sharedPrefsFile, "__GPSAdId__", extractedAdid)
        }

        if (version.isUS) {
            val filesDir = getActualFilesDir(version)
            if (filesDir != null) {
                val mlsdkEncFile = "$filesDir/mlsdk_deviceinformation_us"
                updateMlsdkJson(mlsdkEncFile, extractedPrefix, extractedHex, extractedAdid)
            }
        }

        return true
    }

    fun fixPermissions(version: GameVersion, strings: AppStrings, logger: (String) -> Unit) {
        logger(strings.logFixingPermissions)
        val prefsDir = getActualPrefsDir(version) ?: return
        val filesDir = getActualFilesDir(version)

        val qPrefs = RootExecutor.shellQuote(prefsDir)
        val statRes = RootExecutor.execute("stat -c \"%u:%g\" $qPrefs 2>/dev/null || ls -ld $qPrefs | awk '{print \$3\":\" \$4}'")
        val uidGid = statRes.output.trim()

        if (uidGid.isNotEmpty() && uidGid.matches(Regex("^[0-9]+:[0-9]+$"))) {
            RootExecutor.execute("chown -R ${RootExecutor.shellQuote(uidGid)} $qPrefs")
            RootExecutor.execute("chmod 660 ${RootExecutor.shellQuote("$prefsDir/*.xml")}")

            if (version.isUS && filesDir != null) {
                val qFiles = RootExecutor.shellQuote(filesDir)
                RootExecutor.execute("chown -R ${RootExecutor.shellQuote(uidGid)} $qFiles")
                RootExecutor.execute("chmod 440 ${RootExecutor.shellQuote("$filesDir/mlsdk_device*")}")
            }
        }
    }

    private fun replaceOrInsertXmlString(filePath: String, key: String, newValue: String) {
        if (!RootExecutor.fileExists(filePath)) return

        val qPath = RootExecutor.shellQuote(filePath)
        val safeKey = sanitizeKey(key)
        val safeValue = sanitizeDeviceId(newValue)

        val checkKey = RootExecutor.execute("grep -q 'name=\"$safeKey\"' $qPath && echo 'EXISTS'")
        if (checkKey.output.contains("EXISTS")) {
            val cmd = "sed -i \"s|name=\\\"${safeKey}\\\">[^<]*<|name=\\\"${safeKey}\\\">${safeValue}<|g\" $qPath"
            RootExecutor.execute(cmd)
        } else {
            val insertCmd = "sed -i 's|</map>|    <string name=\"$safeKey\">${safeValue}</string>\\n</map>|g' $qPath"
            RootExecutor.execute(insertCmd)
        }
    }

    private fun updateMlsdkJson(filePath: String, prefix: String, androidId: String, adid: String) {
        if (RootExecutor.fileExists(filePath)) {
            val qPath = RootExecutor.shellQuote(filePath)
            val sPrefix = sanitizeDeviceId(prefix)
            val sAndroid = sanitizeDeviceId(androidId)
            val sAdid = sanitizeDeviceId(adid)

            val cmdPrefix = "sed -i 's/\"mask_id_device_uniqueid\":\"[^\"]*\"/\"mask_id_device_uniqueid\":\"$sPrefix\"/g' $qPath"
            val cmdAndroidId = "sed -i 's/\"mask_id_android_id\":\"[^\"]*\"/\"mask_id_android_id\":\"$sAndroid\"/g' $qPath"
            val cmdAdid = "sed -i 's/\"mask_id_advertising_id\":\"[^\"]*\"/\"mask_id_advertising_id\":\"$sAdid\"/g' $qPath"

            RootExecutor.execute(cmdPrefix)
            RootExecutor.execute(cmdAndroidId)
            RootExecutor.execute(cmdAdid)
        }
    }

    private fun sanitizeDeviceId(input: String): String =
        input.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == '-' }

    private fun sanitizeKey(input: String): String =
        input.filter { it.isLetterOrDigit() || it == '_' }
}

