package com.uranus.spn

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.uranus.spn.ui.theme.SPNTheme
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {

    private val jsonData: MutableState<String> = mutableStateOf("")

    private val notificationPermission = "android.permission.POST_NOTIFICATIONS"
    private val notificationPermissionCode = 123 // ou tout autre code d'identification

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // La permission a été accordée
            // Vous pouvez maintenant afficher des notifications si nécessaire
        } else {
            // La permission a été refusée
            // Gérez ce cas en conséquence
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, notificationPermission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(notificationPermission)
        }

        setContent {
            SPNTheme {
                // black background and centred text that says hello world
                Surface {
                    Text(text = "SPN v0.1")
                }
            }
        }

        // Call the function asynchronously
        lifecycleScope.launch {
            val workRequest = OneTimeWorkRequestBuilder<SRDataCheckWorker>().build()
            val workRequest2 = OneTimeWorkRequestBuilder<CLDataCheckWorker>().build()
            WorkManager.getInstance(applicationContext).enqueue(workRequest)
            WorkManager.getInstance(applicationContext).enqueue(workRequest2)
        }
    }




    suspend fun getDataFrom(url: String): JSONObject {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        return client.newCall(request).await()
    }

}

@Composable
fun DataText(data: String) {
    Text(text = data)
}

// Extension function to convert Call to Response
suspend fun Call.await(): JSONObject {
    return suspendCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val res = response.body!!.string()
                val json = JSONObject(res)
                continuation.resume(json)
            }
        })
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SPNTheme {
    }
}