package com.wpf.util.common.ui.marketplace.markets

import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.marketplace.markets.base.Market
import com.wpf.util.common.ui.marketplace.markets.base.UploadData

class UnknownMarket: Market {

    override var isSelect: Boolean = false

    override val name: String = "Unknown"

    override val baseUrl: String = ""

    override fun uploadAbi(): Array<AbiType> = arrayOf(AbiType.Abi32_64)

    override fun push(uploadData: UploadData) {

    }
}