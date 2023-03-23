package com.marcosmendes.netflix.util

import android.graphics.Movie
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.marcosmendes.netflix.Model.Category
import com.marcosmendes.netflix.Model.MovieItem
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class CategoryTask(private val callback: Callback) {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()


    interface Callback {
        fun onPreExecute()
        fun onResult(categories: List<Category>)
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
                    if (statusCode > 400) {
                        throw IOException("Erro na comunicação com o servidor")
                    }

                    stream = urlConnection.inputStream
                  //  val jsonAsString = stream.bufferedReader().use { it.readText() }

                    //Log.i("Teste", jsonAsString)

                    buffer = BufferedInputStream(stream)
                    val jsonAsString = toString(buffer)

                    val categories = toCategories(jsonAsString)

                    handler.post {
                        callback.onResult(categories)
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

    private fun toCategories(jsonAsString: String): List<Category> {
        val categories = mutableListOf<Category>()

        val jsonRoot = JSONObject(jsonAsString)
        val jsonCategories = jsonRoot.getJSONArray("category")

        for (i in 0 until jsonCategories.length()) {
            val jsonCategory = jsonCategories.getJSONObject(i)
            val title = jsonCategory.getString("title")
            val jsonMovies = jsonCategory.getJSONArray("movie")

            val movies = mutableListOf<MovieItem>()
            for (j in 0 until  jsonMovies.length()) {
                val jsonMovie = jsonMovies.getJSONObject(j)
                val id = jsonMovie.getInt("id")
                val coverUrl = jsonMovie.getString("cover_url")

                movies.add(MovieItem(id, coverUrl))
            }
            categories.add(Category(title, movies))
        }
        return categories
    }
}