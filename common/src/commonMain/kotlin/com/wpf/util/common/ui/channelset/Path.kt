package com.wpf.util.common.ui.channelset

import kotlinx.serialization.Serializable

@Serializable
data class Path(
    val name: String,
    val path: String
)