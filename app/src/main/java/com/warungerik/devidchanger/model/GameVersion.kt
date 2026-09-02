package com.warungerik.devidchanger.model

import com.warungerik.devidchanger.engine.StringCrypt

enum class GameVersion(
    val displayName: String,
    val encPackageName: String,
    val isUS: Boolean = false
) {
    GLOBAL("Global", "VCc0RBbj/8fTtc+eZnNAWCMr"),
    INDIA("India", "VCc0RBbj/8fWvs+Vcw=="),
    USA("USA", "VCc0RBbj/8fTtc+eZnNAWCMrRw/4/Q==", isUS = true),
    VIETNAM("Vietnam", "VCc0RBPk+s/Stc+fb3ZHQCk="),
    HWAG("HWAG", "VCc0RBbj/8fTtY2XZHFLUjR2AQ3q+w=="),
    LITE("LITE", "VCc0RBbj/8fTtc+eZnNAWCMrRxbi6Mg=");

    val packageName: String get() = StringCrypt.decode(encPackageName)

    val dataDir: String get() = "/data/data/$packageName"
    val prefsDir: String get() = "$dataDir/shared_prefs"
    val filesDir: String get() = "$dataDir/files"
    val playerPrefsFile: String get() = "$prefsDir/$packageName.v2.playerprefs.xml"
    val sharedPrefsFile: String get() = "$prefsDir/__SharedPreference__.xml"
    val mlsdkEncFile: String get() = "$filesDir/mlsdk_deviceinformation_us"
    val mlsdkRawFile: String get() = "$filesDir/mlsdk_deviceinformation_us_raw"
}
