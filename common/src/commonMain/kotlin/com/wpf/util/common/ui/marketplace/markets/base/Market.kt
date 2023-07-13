package com.wpf.util.common.ui.marketplace.markets.base

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.base.SelectInterface
import io.ktor.http.*
import java.io.File


interface Market: SelectInterface {

    override val isSelectState: MutableState<Boolean>
        get() = mutableStateOf(false)

    val name: String

    val baseUrl: String

    //市场支持上传的abi
    fun uploadAbi(): Array<AbiType>

    @Composable
    fun dispositionView(market: Market) {
        if (!market.isSelectState.value) return
        Box(
            modifier = if (market.isSelectState.value) Modifier.fillMaxWidth() else Modifier.height(0.dp)
        ) {
            dispositionViewInBox(market)
        }
    }

    @Composable
    fun dispositionViewInBox(market: Market) {

    }

    fun query(uploadData: UploadData) {

    }

    fun push(uploadData: UploadData)


    fun pngHeader(screenShotPath: String): Headers {
        return Headers.build {
            append(HttpHeaders.ContentType, "image/png")
            append(HttpHeaders.ContentDisposition, "filename=\"${File(screenShotPath).name}\"")
        }
    }

    fun apkHeader(apkPath: String): Headers {
        return Headers.build {
            append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
            append(HttpHeaders.ContentDisposition, "filename=\"${File(apkPath).name}\"")
        }
    }

}