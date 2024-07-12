import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun main() {
    val client = HttpClient(OkHttp)

    val file = withContext(Dispatchers.IO) {
        File.createTempFile("ktor", "image.jpg")
    }
    client.prepareGet("https://httpbin.org/image/jpeg").execute {
        it.bodyAsChannel().writeToFile(file.path)
    }

    println("Downloaded file $file with size ${file.length()}")

    val response = client.post("https://httpbin.org/post") {
        setBody(bodyFromFile(file.path))
    }

    println("Received text body with size ${response.bodyAsText().length}")

    client.close()
}
