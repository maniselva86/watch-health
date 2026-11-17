package com.example.watchhealth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

// v1: reads heart rate + steps while the app is open, uploads a batch every 60 s.
class MainActivity : Activity(), SensorEventListener {
    private val buf = JSONArray()
    private val h = Handler(Looper.getMainLooper())
    private lateinit var tv: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        tv = TextView(this).apply { text = "Starting..."; textSize = 16f; gravity = Gravity.CENTER }
        setContentView(tv)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS, Manifest.permission.ACTIVITY_RECOGNITION), 1)
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        listOf(Sensor.TYPE_HEART_RATE, Sensor.TYPE_STEP_COUNTER).forEach { t ->
            sm.getDefaultSensor(t)?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
        tv.text = "Waiting for sensors..."
        h.postDelayed(object : Runnable {
            override fun run() { flush(); h.postDelayed(this, 60_000) }
        }, 60_000)
    }

    override fun onSensorChanged(e: SensorEvent) {
        val isHr = e.sensor.type == Sensor.TYPE_HEART_RATE
        val v = e.values[0]
        if (isHr && v <= 0f) return
        val m = if (isHr) "heart_rate" else "steps"
        synchronized(buf) {
            buf.put(JSONObject().put("metric", m).put("value", v.toDouble()).put("ts", Instant.now().toString()))
        }
        tv.text = "$m: ${v.toInt()}"
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    private fun flush() {
        val n: Int
        val body: String
        synchronized(buf) { n = buf.length(); if (n == 0) return; body = buf.toString() }
        Thread {
            try {
                val c = URL(BuildConfig.SUPA_URL + "/rest/v1/readings").openConnection() as HttpURLConnection
                c.requestMethod = "POST"; c.doOutput = true
                c.setRequestProperty("apikey", BuildConfig.SUPA_KEY)
                c.setRequestProperty("Authorization", "Bearer " + BuildConfig.SUPA_KEY)
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray()) }
                if (c.responseCode in 200..299) synchronized(buf) { repeat(n) { buf.remove(0) } }
                c.disconnect()
            } catch (_: Exception) { /* keep buffer, retry next cycle */ }
        }.start()
    }
}
