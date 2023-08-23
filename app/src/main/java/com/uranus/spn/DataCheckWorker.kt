package com.uranus.spn

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONArray

abstract class DataCheckWorker(
    appContext: Context, workerParams: WorkerParameters,
    protected val apiUrl: String,
    protected val storedDataKey: String
) : CoroutineWorker(appContext, workerParams) {

    abstract override suspend fun doWork(): Result // abstract function

    abstract suspend fun getDataFromUrl(url: String): JSONArray // abstract function

    protected fun showNotification(title: String, content: String) {
        val channelId = "DataCheckChannelId"
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        val channel = NotificationChannel(
            channelId, "Data Check Channel", NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        Log.w("AAA", "TRY")

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.w("AAA", "NO PERM WTF")
            return
        }
        // print AAA in log for debugging
        Log.w("showNotification", "SUCCESS")
        // generate a random notification id
        val notificationId = (0..10000).random()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }


}
