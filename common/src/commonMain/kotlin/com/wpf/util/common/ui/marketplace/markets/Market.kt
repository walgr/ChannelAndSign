package com.wpf.util.common.ui.marketplace.markets

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.base.SelectInterface
import kotlinx.serialization.Serializable


interface Market: SelectInterface {

    override val isSelectState: MutableState<Boolean>
        get() = mutableStateOf(false)

    val name: String

    val baseUrl: String
    //市场支持上传的abi
    fun uploadAbi(): Array<AbiType>

    @Composable
    fun dispositionView(market: Market) {
        Box(
            modifier = if (market.isSelectState.value) Modifier.fillMaxWidth() else Modifier.height(
                0.dp
            )
        ) {

        }
    }

    fun push(uploadData: UploadData)

}