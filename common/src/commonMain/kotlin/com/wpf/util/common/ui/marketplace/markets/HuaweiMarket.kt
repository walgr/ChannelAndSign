package com.wpf.util.common.ui.marketplace.markets

import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.marketplace.UploadData

object HuaweiMarket: Market {

    override val baseUrl: String = ""
    override fun uploadAbi() = arrayOf(AbiType.Abi32_64)

    override fun push(uploadData: UploadData) {
        TODO("Not yet implemented")
    }
}