package com.wpf.utils.tools

import com.wpf.utils.ResourceManager
import java.io.File

/**
 * 映射到命令
 */

object ManifestEditorUtil {
    private val manifestEditorPath: String = ""
        get() {
            return if (field.isEmpty() || !File(field).exists()) {
                ResourceManager.getResourceFile("ManifestEditor.jar").path
            } else field
        }

    fun delJar() {
        ResourceManager.delResourceByPath(manifestEditorPath)
    }

    /**
     * 操作
     */
    fun doCommand(cmd: MutableList<String>) {
        val result = Runtime.getRuntime().exec(RunJar.javaJar(manifestEditorPath, cmd.toTypedArray()))
        LogStreamThread(result.errorStream, false) {
            it.isNotEmpty() && "null" != it
        }.start()
        result.waitFor()
        result.destroy()
    }
}