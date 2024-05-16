package kaf.audiobookshelfwearos.app

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.IOException

class CurlLoggingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val curlCommand = buildCurlCommand(request)
        Timber.d(curlCommand)
        return chain.proceed(request)
    }

    private fun buildCurlCommand(request: Request): String {
        val curlCommand = StringBuilder("curl -X ${request.method}")

        // Add headers
        for (headerName in request.headers.names()) {
            curlCommand.append(" -H \"$headerName: ${request.header(headerName)}\"")
        }

        // Add request body if present
        request.body?.let { body ->
            val buffer = okio.Buffer()
            body.writeTo(buffer)
            val bodyString = buffer.readUtf8()
            curlCommand.append(" -d '${bodyString}'")
        }

        // Add URL
        curlCommand.append(" \"${request.url}\"")

        return curlCommand.toString()
    }
}

fun createOkHttpClient(): OkHttpClient {
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    return OkHttpClient.Builder()
        .addInterceptor(CurlLoggingInterceptor())
        .addInterceptor(loggingInterceptor)
        .build()
}