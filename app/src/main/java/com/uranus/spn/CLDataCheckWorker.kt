package com.uranus.spn

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
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
        var jsonResult = JSONObject(URL(url).readText())
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

        // only check the first node
        val node = jsonData.getJSONObject(0)
        val leagueMatchSetting =
            node.getJSONObject("leagueMatchSetting").getJSONObject("leagueMatchEvent")
        val name = leagueMatchSetting.getString("name")
        val desc = leagueMatchSetting.getString("desc")
        val startTime = node.getJSONArray("timePeriods").getJSONObject(0).getString("startTime")

        // calculate the time difference
        val startdate = Instant.parse(startTime)
        val now = Instant.now()
        val diff = startdate.epochSecond - now.epochSecond

        // pretty print the date
        val localDateTime = LocalDateTime.ofInstant(startdate, ZoneId.systemDefault())
        val dateStr = localDateTime.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + localDateTime.dayOfMonth.toString() + " at " + localDateTime.hour.toString() + ":00"

        if (diff < 3600 * 2) {
            showNotification("Challenge $name starts next", "Starting at the next rotation, $desc")
        } else {
            showNotification("Challenge $name soon !", "Starting $dateStr, $desc")
        }

        // send a notification a day before as a reminder if the time is not today

        val oneDayInSeconds = 24 * 60 * 60 // One day in seconds

        if (diff > oneDayInSeconds && diff < oneDayInSeconds * 2) {
            val reminderTime = now.plusSeconds(oneDayInSeconds.toLong())
            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(Duration.between(now, reminderTime))
                .build()

            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(workRequest)
        }


        return Result.success()
    }
}