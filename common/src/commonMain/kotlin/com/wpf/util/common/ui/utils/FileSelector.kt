package com.wpf.util.common.ui.utils

import androidx.compose.ui.awt.ComposeWindow
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter


object FileSelector {

    fun showFileSelector(
        suffixList: Array<String> = arrayOf(),
        selectionMode: Int = JFileChooser.FILES_ONLY,
        onFileSelected: (String) -> Unit
    ) {
        JFileChooser().apply {
            //设置页面风格
            try {
                val lookAndFeel = UIManager.getSystemLookAndFeelClassName()
                UIManager.setLookAndFeel(lookAndFeel)
                SwingUtilities.updateComponentTreeUI(this)
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            fileSelectionMode = selectionMode
            isMultiSelectionEnabled = false
            if (suffixList.isNotEmpty()) {
                fileFilter = FileNameExtensionFilter("文件过滤", *suffixList)
            }

            val result = showOpenDialog(ComposeWindow())
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = this.selectedFile
                onFileSelected(file.absolutePath)
            }
        }
    }

    fun saveFile(currentDirectoryPath: String = File("").absolutePath, extension: String, info: String) {
        JFileChooser(currentDirectoryPath).apply {
            //设置页面风格
            runCatching {
                val lookAndFeel = UIManager.getSystemLookAndFeelClassName()
                UIManager.setLookAndFeel(lookAndFeel)
                SwingUtilities.updateComponentTreeUI(this)
            }
            dialogTitle = "保存"
            fileFilter = FileNameExtensionFilter(".$extension", extension)

            val result = showSaveDialog(ComposeWindow())
            if (result == JFileChooser.APPROVE_OPTION) {
                var outFile = selectedFile
                if (outFile.extension.isEmpty()) {
                    outFile = File(selectedFile.absolutePath + "." + extension)
                }
                FileUtil.save2File(info.byteInputStream(), outFile)
            }
        }
    }
}