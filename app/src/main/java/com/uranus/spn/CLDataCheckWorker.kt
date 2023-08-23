package com.uranus.spn

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class CLDataCheckWorker(
    appContext: Context, workerParams: WorkerParameters
) : DataCheckWorker(
    appContext, workerParams, "https://splatoon3.ink/data/schedules.json", "CL_DATA"
) {

    override suspend fun getDataFromUrl(url: String): JSONArray {
        // custom implementation
        val jsonResult = JSONObject(URL(url).readText())
        // data.eventSchedules.nodes
        return jsonResult.getJSONObject("data").getJSONObject("eventSchedules")
            .getJSONArray("nodes")
    }

    override suspend fun doWork(): Result {
        val storedData = inputData.getString(storedDataKey)
        val jsonData = getDataFromUrl(apiUrl)

        // iterate through newData nodes and check setting.weapons.name
//        for (i in 0 until jsonData.length()) {
//            val node = jsonData.getJSONObject(i)
//            val leagueMatchSetting = node.getJSONObject("leagueMatchSetting").getJSONObject("leagueMatchEvent")
//            val name = leagueMatchSetting.getString("name")
//            val desc = leagueMatchSetting.getString("desc")
//            val startTime = node.getJSONArray("timePeriods").getJSONObject(0).getString("startTime")
//            showNotification("Challenge $name soon !", "starting at $startTime, $desc")
//        }

        var name: String? = null
        var desc: String? = null
        var startdate: Instant? = null
        var enddate: Instant? = null
        val now = Instant.now()

        loop@ for (i in 0 until jsonData.length()) {
            val node = jsonData.getJSONObject(i)
            val leagueMatchSetting = node.getJSONObject("leagueMatchSetting").getJSONObject("leagueMatchEvent")
            name = leagueMatchSetting.getString("name")
            desc = leagueMatchSetting.getString("desc")

            val timePeriods = node.getJSONArray("timePeriods")
            for (j in 0 until timePeriods.length()) {
                val timePeriod = timePeriods.getJSONObject(j)
                val startTime = Instant.parse(timePeriod.getString("startTime"))

                if (startTime.isAfter(now)) {
                    startdate = startTime
                    enddate = Instant.parse(timePeriod.getString("endTime"))
                    break@loop
                }
            }
        }


        if (startdate == null || enddate == null) {
            return Result.failure()
        }


        // if the event is now, show notification
        if (startdate.epochSecond <= now.epochSecond && now.epochSecond <= enddate.epochSecond) {
            if (desc != null) {
                showNotification("Challenge $name is live !", desc)
            }
            return Result.success()
        }

        // pretty print the date
        val localDateTime = LocalDateTime.ofInstant(startdate, ZoneId.systemDefault())
        val dateStr = localDateTime.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + localDateTime.dayOfMonth.toString() + " at " + localDateTime.hour.toString() + ":00"

        val diff = Duration.between(now, startdate).seconds

        // if the event is in 2 hours, show notification
        if (diff < 3600 * 2) {
            showNotification("Challenge $name starts next", "Starting at " + localDateTime.hour.toString() + ":00, $desc")

            val inputDataNow = workDataOf("title" to "Challenge $name is live !", "content" to desc)

            val workRequestNow = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(Duration.between(now, startdate))
                .setInputData(inputDataNow)
                .build()

            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(workRequestNow)

            return Result.success()

        } else {
            // schedule a reminder
            showNotification("Challenge $name soon !", "Starting $dateStr, $desc")
            val oneDayInSeconds = 24 * 60 * 60 // One day in seconds
            val twoHoursInSeconds = 2 * 60 * 60 // Two hours in seconds

            val reminderTime1day = startdate.minusSeconds(oneDayInSeconds.toLong())
            val reminderTime2h = startdate.minusSeconds(twoHoursInSeconds.toLong())

            val inputData1day = workDataOf("title" to "Challenge $name soon !", "content" to "Starting $dateStr, $desc")
            val inputData2h = workDataOf("title" to "Challenge $name starts next", "content" to "Starting at " + localDateTime.hour.toString() + ":00, $desc")
            val inputDataNow = workDataOf("title" to "Challenge $name is live !", "content" to desc, "check now" to true, "from" to "CL")

            val workRequest1day = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(Duration.between(now, reminderTime1day))
                .setInputData(inputData1day)
                .build()
            val workRequest2h = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(Duration.between(now, reminderTime2h))
                .setInputData(inputData2h)
                .build()
            val workRequestNow = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(Duration.between(now, startdate))
                .setInputData(inputDataNow)
                .build()

            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(workRequest1day)
            workManager.enqueue(workRequest2h)
            workManager.enqueue(workRequestNow)
        }



        return Result.success()
    }
}