package com.wpf.util.common.ui.marketplace.markets

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.wpf.util.common.ui.base.AbiType

class VivoMarket: Market {

    override var isSelect = false
    @Transient
    override val isSelectState: MutableState<Boolean> = mutableStateOf(isSelect)

    override val name: String = "Vivo"

    override val baseUrl: String = ""
    override fun uploadAbi() = arrayOf(AbiType.Abi32, AbiType.Abi64)

    override fun push(uploadData: UploadData) {
    }
}