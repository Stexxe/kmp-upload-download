import io.ktor.http.content.*

expect suspend fun makeRequests()
expect suspend fun bodyFromFile(filepath: String): OutgoingContent
