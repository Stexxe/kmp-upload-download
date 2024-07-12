import io.ktor.http.content.*
import io.ktor.utils.io.*

expect suspend fun makeRequests()
expect suspend fun bodyFromFile(filepath: String): OutgoingContent
expect suspend fun ByteReadChannel.writeToFile(filepath: String)
