package com.wpf.util.common.ui.marketplace.markets.base

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.base.SelectInterface
import com.wpf.util.common.ui.utils.Callback
import io.ktor.http.*
import java.io.File


interface Market : SelectInterface {

    override val isSelectState: MutableState<Boolean>
        get() = mutableStateOf(false)

    val name: String

    val baseUrl: String

    //市场支持上传的abi
    fun uploadAbi(): Array<AbiType>

    @Composable
    fun dispositionView(market: Market) {
        if (!market.isSelect) return
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            dispositionViewInBox(market)
        }
    }

    @Composable
    fun dispositionViewInBox(market: Market) {

    }

    fun query(uploadData: UploadData, callback: Callback<MarketType>) {

    }

    fun push(uploadData: UploadData, callback: Callback<MarketType>)

    fun jpgHeader(screenShotPath: String): Headers {
        return Headers.build {
            append(HttpHeaders.ContentType, ContentType.Image.JPEG)
            append(HttpHeaders.ContentDisposition, "filename=\"${File(screenShotPath).name}\"")
        }
    }

    fun pngHeader(screenShotPath: String): Headers {
        return Headers.build {
            append(HttpHeaders.ContentType, ContentType.Image.PNG)
            append(HttpHeaders.ContentDisposition, "filename=\"${File(screenShotPath).name}\"")
        }
    }

    fun apkHeader(filePath: String): Headers {
        return Headers.build {
            append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
            append(HttpHeaders.ContentDisposition, "filename=\"${File(filePath).name}\"")
        }
    }

    fun fileHeader(filePath: String): Headers {
        return Headers.build {
            append(HttpHeaders.ContentDisposition, "filename=\"${File(filePath).name}\"")
        }
    }

    fun initByData() {

    }

    fun clearInitData() {
        clearCache()
    }

    fun clearCache() {

    }
}