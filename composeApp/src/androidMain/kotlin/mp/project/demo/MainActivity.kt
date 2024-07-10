package mp.project.demo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.util.cio.readChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoroutineScope(Dispatchers.IO).launch {
            val client = HttpClient(OkHttp)

            val file = withContext(Dispatchers.IO) {
                File.createTempFile("ktor", "image.jpg", applicationContext.cacheDir)
            }
            client.prepareGet("https://httpbin.org/image/jpeg").execute {
                it.bodyAsChannel().copyTo(file.writeChannel())
            }

            Log.d("KtorClient", "Downloaded file $file with size ${file.length()}")

            val response = client.post("https://httpbin.org/post") {
                setBody(file.readChannel())
            }

            Log.d("KtorClient", "Received text body with size ${response.bodyAsText().length}")

            client.close()
        }
    }
}
