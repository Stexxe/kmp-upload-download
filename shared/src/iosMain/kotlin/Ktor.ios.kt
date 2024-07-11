import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.cinterop.*
import kotlinx.cinterop.ByteVar
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.darwin.*
import platform.posix.O_RDWR
import platform.posix.close
import platform.posix.open
import kotlin.coroutines.resume

private const val BUFFER_SIZE = 4096

@OptIn(ExperimentalForeignApi::class)
actual suspend fun makeRequests() {
    val client = HttpClient(Darwin)

    val tempName = NSUUID().UUIDString + "_image.jpg"
    val tempFile = NSTemporaryDirectory() + tempName
    NSFileManager.defaultManager()
        .createFileAtPath(tempFile, null, mapOf(NSFilePosixPermissions to "644".toInt(radix = 8)))

    val queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.convert(), 0u)

    client.prepareGet("https://httpbin.org/image/jpeg").execute {
        it.bodyAsChannel().writeToFile(tempFile, queue)
    }

    println("Downloaded file $tempName with size ${getFileSize(tempFile)}")

    val response = client.post("https://httpbin.org/post") {
        setBody(bodyFromFile(tempFile, queue))
    }

    println("Received text body with size ${response.bodyAsText().length}")

    client.close()
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun ByteReadChannel.writeToFile(path: String, queue: dispatch_queue_t) {
    val channel = this
    memScoped {
        val dst = allocArray<ByteVar>(BUFFER_SIZE)
        val fd = open(path, O_RDWR)

        try {
            while (!channel.isClosedForRead) {
                val rs = channel.readAvailable(dst, 0, BUFFER_SIZE)
                if (rs < 0) break

                val data = dispatch_data_create(dst, rs.convert(), queue) {}

                dispatch_write(fd, data, queue) { _, error ->
                    if (error != 0) {
                        channel.cancel(IllegalStateException("Unable to write data to the file $path"))
                    }
                }
            }
        } finally {
            close(fd)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun bodyFromFile(path: String, queue: dispatch_queue_t): OutgoingContent {
    suspend fun read(fd: Int): NSData? {
        return suspendCancellableCoroutine { continuation ->
            dispatch_read(fd, BUFFER_SIZE.toULong(), queue) { dispatchData, _ ->
                val data = dispatchData as NSData
                continuation.resume(if (data.bytes != null) data else null)
            }
        }
    }

    return ChannelWriterContent(contentType = ContentType.defaultForFilePath(path), body = {
        val fd = open(path, O_RDWR)
        try {
            while (true) {
                val data = read(fd) ?: break
                val bytes: CPointer<ByteVar> = data.bytes!!.reinterpret()
                writeFully(bytes, 0, data.length.toInt())
                flush()
            }
        } finally {
            close(fd)
        }
    })
}


@OptIn(ExperimentalForeignApi::class)
fun getFileSize(path: String): ULong {
    val attrs = NSFileManager.defaultManager().attributesOfItemAtPath(path, null) as NSDictionary?
    return attrs?.fileSize() ?: 0u
}
