package com.uranus.spn

import android.content.Context
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class CLDataCheckWorker(
    appContext: Context, workerParams: WorkerParameters
) : DataCheckWorker(
    appContext, workerParams,
    "https://splatoon3.ink/data/schedules.json",
    "CL_DATA"
) {

    override suspend fun getDataFromUrl(url: String): JSONArray {
        // custom implementation
        var jsonResult = JSONObject(URL(url).readText())
        // data.eventSchedules.nodes
        return jsonResult.getJSONObject("data").getJSONObject("eventSchedules").getJSONArray("nodes")
    }

    override suspend fun doWork(): Result {
        val storedData = inputData.getString(storedDataKey)
        val jsonData = getDataFromUrl(apiUrl)

        // iterate through newData nodes and check setting.weapons.name
        for (i in 0 until jsonData.length()) {
            val node = jsonData.getJSONObject(i)
            val leagueMatchSetting = node.getJSONObject("leagueMatchSetting").getJSONObject("leagueMatchEvent")
            val name = leagueMatchSetting.getString("name")
            val desc = leagueMatchSetting.getString("desc")
            val startTime = node.getJSONArray("timePeriods").getJSONObject(0).getString("startTime")
            showNotification("Challenge $name soon !", "starting at $startTime, $desc")
        }

        return Result.success()
    }
}