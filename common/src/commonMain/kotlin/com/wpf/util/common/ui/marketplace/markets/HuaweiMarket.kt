package com.wpf.util.common.ui.marketplace.markets

import com.wpf.util.common.ui.base.AbiType

object HuaweiMarket: Market {

    override fun uploadAbi() = arrayOf(AbiType.Abi32_64)
}