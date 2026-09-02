package com.warungerik.devidchanger.root

object RootDetector {

    fun detect(): RootInfo {
        if (!RootExecutor.isSuAvailable()) {
            return RootInfo(
                isRooted = false,
                rootType = RootType.NONE,
                seLinuxStatus = getSELinuxStatus()
            )
        }

        val magiskVer = RootExecutor.execute("magisk -v").output
        if (magiskVer.isNotEmpty() && !magiskVer.contains("not found")) {
            return RootInfo(
                isRooted = true,
                rootType = RootType.MAGISK,
                rootVersion = magiskVer,
                seLinuxStatus = getSELinuxStatus(),
                suPath = findSuPath()
            )
        }

        val magiskProp = RootExecutor.execute("getprop init.svc.magisk_service").output
        if (magiskProp.contains("running") || RootExecutor.fileExists("/data/adb/magisk/magisk")) {
            val magiskCode = RootExecutor.execute("magisk -V").output
            return RootInfo(
                isRooted = true,
                rootType = RootType.MAGISK,
                rootVersion = if (magiskCode.isNotEmpty()) "v$magiskCode" else "Magisk",
                seLinuxStatus = getSELinuxStatus(),
                suPath = findSuPath()
            )
        }

        val ksuRes = RootExecutor.execute("/data/adb/ksud -V").output
        if (ksuRes.isNotEmpty() && !ksuRes.contains("not found")) {
            return RootInfo(
                isRooted = true,
                rootType = RootType.KERNELSU,
                rootVersion = "v$ksuRes",
                seLinuxStatus = getSELinuxStatus(),
                suPath = findSuPath()
            )
        }
        val ksuProp = RootExecutor.execute("getprop ro.kernelsu.version").output
        if (ksuProp.isNotEmpty() && !ksuProp.contains("not found")) {
            return RootInfo(
                isRooted = true,
                rootType = RootType.KERNELSU,
                rootVersion = "v$ksuProp",
                seLinuxStatus = getSELinuxStatus(),
                suPath = findSuPath()
            )
        }

        val apatchProp = RootExecutor.execute("getprop ro.apatch.version").output
        if (apatchProp.isNotEmpty() && !apatchProp.contains("not found")) {
            return RootInfo(
                isRooted = true,
                rootType = RootType.APATCH,
                rootVersion = "v$apatchProp",
                seLinuxStatus = getSELinuxStatus(),
                suPath = findSuPath()
            )
        }
        if (RootExecutor.fileExists("/data/adb/apd")) {
            return RootInfo(
                isRooted = true,
                rootType = RootType.APATCH,
                rootVersion = "APatch Active",
                seLinuxStatus = getSELinuxStatus(),
                suPath = findSuPath()
            )
        }

        val superSuRes = RootExecutor.execute("su -v").output
        if (superSuRes.contains("SUPERSU") || superSuRes.contains("SuperSU")) {
            return RootInfo(
                isRooted = true,
                rootType = RootType.SUPERSU,
                rootVersion = superSuRes,
                seLinuxStatus = getSELinuxStatus(),
                suPath = findSuPath()
            )
        }

        val lineageProp = RootExecutor.execute("getprop ro.lineage.version").output
        if (lineageProp.isNotEmpty() && !lineageProp.contains("not found")) {
            return RootInfo(
                isRooted = true,
                rootType = RootType.LINEAGE,
                rootVersion = "Lineage su",
                seLinuxStatus = getSELinuxStatus(),
                suPath = findSuPath()
            )
        }

        return RootInfo(
            isRooted = true,
            rootType = RootType.GENERIC,
            rootVersion = "Standard su",
            seLinuxStatus = getSELinuxStatus(),
            suPath = findSuPath()
        )
    }

    private fun getSELinuxStatus(): String {
        val res = RootExecutor.execute("getenforce")
        return if (res.success && res.output.isNotEmpty()) {
            res.output
        } else {
            "Unknown"
        }
    }

    private fun findSuPath(): String {
        val res = RootExecutor.execute("which su")
        return if (res.success && res.output.isNotEmpty()) {
            res.output
        } else {
            "/system/bin/su"
        }
    }
}

