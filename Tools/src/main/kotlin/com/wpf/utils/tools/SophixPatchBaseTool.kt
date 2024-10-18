package com.wpf.utils.tools

import com.wpf.utils.ResourceManager
import java.io.File

abstract class SophixPatchBaseTool {
    open val profilesJsonFile: File? = null
        get() {
            return if (field == null || !field.exists()) {
                ResourceManager.getResourceFile("SophixPatchTool/profiles.json", overwrite = true)
            } else field
        }

    open val sophixPatchFile: File by lazy {
        File(ResourceManager.getTempPath() + "sophix-patch.jar")
    }

    open fun delJar() {
        profilesJsonFile?.path?.let {
            ResourceManager.delResourceByPath(it)
        }
    }

    abstract fun deal(configPath: String): Boolean
}