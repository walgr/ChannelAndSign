package com.wpf.util.common.ui.marketplace.markets

import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.base.Apk

interface Market {

    //市场支持上传的abi
    fun uploadAbi(): Array<AbiType>
}