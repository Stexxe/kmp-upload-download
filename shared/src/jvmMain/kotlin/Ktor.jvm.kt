import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import java.io.File

actual suspend fun makeRequests() {
    TODO("JVM")
}

actual suspend fun bodyFromFile(filepath: String): OutgoingContent {
    return object : OutgoingContent.ReadChannelContent() {
        override val contentType: ContentType
            get() = ContentType.defaultForFilePath(filepath)
        override fun readFrom(): ByteReadChannel {
            return File(filepath).readChannel()
        }
    }
}

actual suspend fun ByteReadChannel.writeToFile(filepath: String) {
    this.copyTo(File(filepath).writeChannel())
}
