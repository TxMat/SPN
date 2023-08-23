package com.uranus.spn

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderWorker(
    appContext: Context, workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Display the notification for the reminder

        val title = inputData.getString("title") ?: "Reminder"
        val content = inputData.getString("content") ?: "Reminder content"
        val check = inputData.getBoolean("check", false)
        val from = inputData.getString("from") ?: "NA"

        showReminderNotification(title, content)

        if (check) {
            // relaunch the worker that came from
            val workManager = WorkManager.getInstance(applicationContext)
            var workRequest: OneTimeWorkRequest? = null
            if (from == "CL"){
                workRequest = OneTimeWorkRequestBuilder<CLDataCheckWorker>().build()
            } else if (from == "SR") {
                workRequest = OneTimeWorkRequestBuilder<SRDataCheckWorker>().build()
            }
            workManager.enqueue(workRequest!!)

        }

        return Result.success()
    }

    private suspend fun showReminderNotification(title: String, content: String) {
        withContext(Dispatchers.Main) {
            val channelId = "ReminderChannelId"
            val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle(title)
                .setContentText(content).setPriority(NotificationCompat.PRIORITY_HIGH)

            val notificationManager = NotificationManagerCompat.from(applicationContext)
            val channel = NotificationChannel(
                channelId, "Reminder Channel", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)

            if (ActivityCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@withContext
            }
            notificationManager.notify(124, notificationBuilder.build())
        }
    }
}
