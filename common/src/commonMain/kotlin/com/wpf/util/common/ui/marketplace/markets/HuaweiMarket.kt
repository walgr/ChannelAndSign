package com.wpf.util.common.ui.marketplace.markets

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.marketplace.markets.base.Market
import com.wpf.util.common.ui.marketplace.markets.base.UploadData

class HuaweiMarket: Market {

    override var isSelect = false
    @Transient
    override val isSelectState: MutableState<Boolean> = mutableStateOf(isSelect)

    override val name: String = "Huawei"

    override val baseUrl: String = ""

    override fun uploadAbi() = arrayOf(AbiType.Abi32_64)

    override fun push(uploadData: UploadData) {

    }
}