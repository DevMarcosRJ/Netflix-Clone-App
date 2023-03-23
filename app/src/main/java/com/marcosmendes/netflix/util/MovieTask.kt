package com.marcosmendes.netflix.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.marcosmendes.netflix.Model.Category
import com.marcosmendes.netflix.Model.MovieDetail
import com.marcosmendes.netflix.Model.MovieItem
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class MovieTask(private val callback: Callback) {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()


    interface Callback {
        fun onPreExecute()
        fun onResult(movieDetail: MovieDetail)
        fun onFailure(message: String)
    }

    fun execute(url: String) {

        callback.onPreExecute()

        executor.execute {
            var urlConnection: HttpsURLConnection? = null
            var stream: InputStream? = null
            var buffer: BufferedInputStream? = null
                try {
                    val requestUrl = URL(url)

                    urlConnection = requestUrl.openConnection() as HttpsURLConnection

                    urlConnection.readTimeout = 2000
                    urlConnection.connectTimeout = 2000

                    val statusCode: Int = urlConnection.responseCode

                    if (statusCode == 400) {
                        stream = urlConnection.errorStream
                        buffer = BufferedInputStream(stream)
                        val jsonAsString = toString(buffer)

                        val json = JSONObject(jsonAsString)
                        val message = json.getString("message")
                        throw IOException(message)

                    } else if (statusCode > 400) {
                        throw IOException("Erro na comunicação com o servidor")
                    }

                    stream = urlConnection.inputStream
                  //  val jsonAsString = stream.bufferedReader().use { it.readText() }

                    //Log.i("Teste", jsonAsString)

                    buffer = BufferedInputStream(stream)
                    val jsonAsString = toString(buffer)

                    val movieDetail = toMovieDetail(jsonAsString)

                    handler.post {
                        callback.onResult(movieDetail)
                    }

                } catch (e: IOException) {
                    val message = e.message ?: "erro desconhecido"
                    Log.e("Teste", message, e)
                    handler.post {
                        callback.onFailure(message)
                    }
                } finally {
                    urlConnection?.disconnect()
                    stream?.close()
                    buffer?.close()
                }
        }
    }

    private fun toMovieDetail(jsonAsString: String) : MovieDetail {
        val json = JSONObject(jsonAsString)

        val id = json.getInt("id")
        val title = json.getString("title")
        val desc = json.getString("desc")
        val cast = json.getString("cast")
        val coverUrl = json.getString("cover_url")

        val jsonMovies = json.getJSONArray("movie")

        val similars = mutableListOf<MovieItem>()
        for (i in 0 until jsonMovies.length()) {
            val jsonMovie = jsonMovies.getJSONObject(i)

            val similarId = jsonMovie.getInt("id")
            val similarCoverUrl = jsonMovie.getString("cover_url")
            val m = MovieItem(similarId, similarCoverUrl)
            similars.add(m)
        }

        val movie = MovieItem(id, coverUrl, title, desc, cast)

        return MovieDetail(movie, similars)
    }

    private fun toString(stream: InputStream) : String {
        val bytes = ByteArray(1024)
        val baos = ByteArrayOutputStream()
        var read: Int

        while (true) {
            read = stream.read(bytes)
            if (read <= 0) {
                break
            }
            baos.write(bytes, 0, read)
        }
        return String(baos.toByteArray())
    }

}