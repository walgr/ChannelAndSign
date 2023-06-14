package com.wpf.base.dealfile.util

import cn.wjdiankong.main.Main

/**
 * 映射到命令
 */
object AXMLEditor2Util {

    /**
     * 操作属性
     * @param editType -i 新增 -r 删除 -m 更新
     * @param labelName 标签名
     * @param labelIdentification 标签唯一标识
     * @param attrName 属性名
     * @param attrValue 属性值
     * @param inputXmlPath 输入xml
     * @param outputXmlPath 输出xml
     */
    fun doCommandAttr(
        editType: String,
        labelName: String,
        labelIdentification: String,
        attrName: String,
        attrValue: String = "",
        inputXmlPath: String,
        outputXmlPath: String,
    ) {
        Main.main(
            arrayOf(
                "-attr",
                editType,
                labelName,
                labelIdentification,
                attrName,
                attrValue,
                inputXmlPath,
                outputXmlPath
            )
        )
    }

    /**
     * 操作属性
     * @param editType -i 新增 -r 删除
     * @param insertXmlPath 待插入xml
     * @param inputXmlPath 输入xml
     * @param outputXmlPath 输出xml
     */
    fun doCommandTag(
        editType: String,
        insertXmlPath: String,
        inputXmlPath: String,
        outputXmlPath: String,
    ) {
        Main.main(arrayOf("-tag", editType, insertXmlPath, inputXmlPath, outputXmlPath))
    }

    /**
     * 插入属性
     * @param insertXmlPath 待插入xml
     * @param inputXmlPath 输入xml
     * @param outputXmlPath 输出xml
     */
    fun doCommandTagInsert(
        insertXmlPath: String,
        inputXmlPath: String,
        outputXmlPath: String,
    ) {
//        Runtime.getRuntime().exec("java -jar D:\\Android\\ShareFile\\tools\\AXMLEditor2.jar -tag -i $insertXmlPath $inputXmlPath $outputXmlPath")
        Main.main(arrayOf("-tag", "-i", insertXmlPath, inputXmlPath, outputXmlPath))
    }

    /**
     * 删除属性
     * @param labelName 标签名
     * @param labelIdentification 标签唯一标识
     * @param inputXmlPath 输入xml
     * @param outputXmlPath 输出xml
     */
    fun doCommandTagDel(
        labelName: String,
        labelIdentification: String,
        inputXmlPath: String,
        outputXmlPath: String,
    ) {
        Main.main(arrayOf("-tag", "-r", labelName, labelIdentification, inputXmlPath, outputXmlPath))
    }
}