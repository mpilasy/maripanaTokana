import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

val threadLocalSdf = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
}

fun main() = runBlocking {
    val jobs = List(100) {
        launch(Dispatchers.IO) {
            val formatter = threadLocalSdf.get()!!
            // simulate suspension point
            delay((Math.random() * 10).toLong())
            val date = formatter.format(Date())
        }
    }
    jobs.forEach { it.join() }
    println("Done")
}
