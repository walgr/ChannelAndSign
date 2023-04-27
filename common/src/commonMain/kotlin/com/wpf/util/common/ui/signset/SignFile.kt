package com.wpf.util.common.ui.signset

import com.wpf.util.common.ui.base.SelectItem
import kotlinx.serialization.Serializable

@Serializable
class SignFile(
    var name: String = "",               //签名名称
    var StoreFile: String = "",          //签名文件
    var StorePass: String = "",          //密码
    var KeyAlias: String = "",           //别名
    var KeyPass: String = "",            //密码
    override var isSelect: Boolean = false
): SelectItem()