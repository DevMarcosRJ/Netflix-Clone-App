package com.marcosmendes.netflix.Model

data class MovieDetail(
    val movie: MovieItem,
    val similars: List<MovieItem>
)