package com.uranus.spn

import android.content.Context
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class SRDataCheckWorker(
    appContext: Context, workerParams: WorkerParameters
) : DataCheckWorker(
    appContext, workerParams,
    "https://splatoon3.ink/data/schedules.json",
    "SR_DATA"
) {

    override suspend fun getDataFromUrl(url: String): JSONArray {
        // custom implementation
        var jsonResult = JSONObject(URL(url).readText())
        // data.coopGroupingSchedule.regularSchedules.nodes
        return jsonResult.getJSONObject("data").getJSONObject("coopGroupingSchedule").getJSONObject("regularSchedules").getJSONArray("nodes")
    }

    override suspend fun doWork(): Result {
        val storedData = inputData.getString(storedDataKey)
        val jsonData = getDataFromUrl(apiUrl)

        // iterate through newData nodes and check setting.weapons.name

        for (i in 0 until jsonData.length()) {
            val node = jsonData.getJSONObject(i)
            val weapons = node.getJSONObject("setting").getJSONArray("weapons")
            val startTime = node.getString("startTime")
            for (j in 0 until weapons.length()) {
                val weapon = weapons.getJSONObject(j)
                val name = weapon.getString("name")
                if (name == "Luna Blaster") {
                    showNotification("Luna Blaster", "Luna Blaster is available at $startTime")
                }
            }
        }

        return Result.success()
    }
}