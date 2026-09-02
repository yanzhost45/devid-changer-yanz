package com.warungerik.devidchanger.root

enum class RootType(val displayName: String) {
    MAGISK("Magisk"),
    KERNELSU("KernelSU"),
    APATCH("APatch"),
    SUPERSU("SuperSU"),
    LINEAGE("LineageOS su"),
    GENERIC("Generic su"),
    NONE("Tidak Ada Root")
}
