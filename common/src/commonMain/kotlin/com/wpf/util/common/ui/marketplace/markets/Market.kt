package com.wpf.util.common.ui.marketplace.markets

import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.base.Apk
import com.wpf.util.common.ui.marketplace.MarketApk
import com.wpf.util.common.ui.marketplace.UploadData

interface Market {

    val baseUrl: String
    //市场支持上传的abi
    fun uploadAbi(): Array<AbiType>

    fun push(uploadData: UploadData)
}