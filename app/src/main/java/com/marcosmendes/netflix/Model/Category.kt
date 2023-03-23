package com.marcosmendes.netflix.Model

data class Category(
    val name: String,
    val movie: MutableList<MovieItem>
)
