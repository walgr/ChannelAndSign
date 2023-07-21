package com.wpf.util.common.ui.marketplace.markets

import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.marketplace.markets.base.Market
import com.wpf.util.common.ui.marketplace.markets.base.MarketType
import com.wpf.util.common.ui.marketplace.markets.base.UploadData
import com.wpf.util.common.ui.utils.Callback

class UnknownMarket: Market {

    override var isSelect: Boolean = false

    override val name: String = MarketType.未知.channelName

    @Transient
    override val baseUrl: String = ""

    override fun uploadAbi(): Array<AbiType> = arrayOf(AbiType.Abi32_64)

    override fun push(uploadData: UploadData, callback: Callback<MarketType>) {

    }
}