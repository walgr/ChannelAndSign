package com.wpf.util.common.ui.utils

import androidx.compose.ui.awt.ComposeWindow
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

object FileSelector {

    fun showFileSelector(
        suffixList: Array<String>,
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
            fileFilter = FileNameExtensionFilter("文件过滤", *suffixList)

            val result = showOpenDialog(ComposeWindow())
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = this.selectedFile
                onFileSelected(file.absolutePath)
            }
        }
    }
}