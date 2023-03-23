package com.marcosmendes.netflix.Model

import androidx.annotation.DrawableRes

data class MovieItem(
    val id: Int,
    val coverUrl: String,
    val title: String = "",
    val desc: String = "",
    val cast: String = "",
)
