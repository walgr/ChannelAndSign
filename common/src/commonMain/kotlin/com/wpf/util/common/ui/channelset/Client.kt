package com.wpf.util.common.ui.channelset

import com.wpf.util.common.ui.base.Group
import kotlinx.serialization.Serializable

@Serializable
class Client(
    override var name: String = "",
    override var isSelect: Boolean = false,
    var channelPath: String = ""
) : Group(name, isSelect)