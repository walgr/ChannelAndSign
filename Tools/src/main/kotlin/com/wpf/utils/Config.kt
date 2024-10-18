package com.wpf.utils

import java.io.File

val rootPath = File(".").canonicalPath + File.separator
var curPath = File(".").canonicalPath + File.separator

val isLinuxRuntime = System.getProperties().getProperty("os.name").contains("Linux")
val isWinRuntime = System.getProperties().getProperty("os.name").contains("Windows")
val isMacRuntime = System.getProperties().getProperty("os.name").contains("Mac")