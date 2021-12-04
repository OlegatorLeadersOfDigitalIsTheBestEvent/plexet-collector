package team.plexet.product.service.foreground

import android.Manifest
import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.Sensor.*
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import team.plexet.product.models.SensorDataModel
import team.plexet.product.service.net.RetrofitService
import team.plexet.product.service.net.dto.DataDTO
import team.plexet.product.service.net.dto.ReplyDTO
import java.time.Instant
import java.util.*

class ForegroundService : Service(), SensorEventListener {
    private var mSensorManager: SensorManager? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var deviceSensors: List<Sensor>? = null
    private val valuesMap: HashMap<Int, SensorDataModel?> = HashMap()
    private var sending = false
    private var timer: Timer? = null
    private var location: Location? = null
    private var requestType: Int = 2
    private val defaultPeriod = 2000L
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val builder = Notification.Builder(this)
            .setSmallIcon(R.drawable.ic_secure)
            .setPriority(Notification.PRIORITY_LOW)
        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nc = NotificationChannel(
                "ForegroundService",
                "ForegroundService",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(nc)
            builder.setChannelId("ForegroundService")
        }
        startForeground(777, notification)
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        deviceSensors = mSensorManager!!.getSensorList(Sensor.TYPE_ALL)
        startTimer(defaultPeriod)
        for (value in deviceSensors!!)
            mSensorManager?.registerListener(this, value, SensorManager.SENSOR_DELAY_FASTEST)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (sending) {
            send(getSensorDTO(valuesMap))
            sending = false
        } else {
            if (event == null)
                return
            valuesMap[event.sensor.type] = SensorDataModel(event.timestamp, event.values)
        }
    }

    private fun send(dataDTO: DataDTO) {
        val call = RetrofitService.instance.postData(dataDTO)
        call.enqueue(object : Callback<ReplyDTO> {
            override fun onResponse(
                call: Call<ReplyDTO>,
                response: Response<ReplyDTO>
            ) {
                val replyDTO = response.body()
                requestType = replyDTO?.point?.sensor ?: 2
                val period = replyDTO?.point?.period ?: defaultPeriod
                startTimer(period)
                println(replyDTO.toString())
                println(response.errorBody()?.string())
            }

            override fun onFailure(call: Call<ReplyDTO>, t: Throwable) {
                requestType = 2
                startTimer(defaultPeriod)
            }
        })
    }

    private fun getSensorDTO(values: HashMap<Int, SensorDataModel?>): DataDTO {
        val dataDTO = DataDTO()
        val accData = values[TYPE_ACCELEROMETER]?.values
        if (accData != null) {
            //ax, ay, az
            dataDTO.ax = accData[0]
            dataDTO.ay = accData[1]
            dataDTO.az = accData[2]
        } else {
            //ax, ay, az
            dataDTO.ax = 0f
            dataDTO.ay = 0f
            dataDTO.az = 0f
        }
        val gyrData = values[TYPE_GYROSCOPE]?.values
        if (gyrData != null) {
            //wx, wy, wz
            dataDTO.wx = gyrData[0]
            dataDTO.wy = gyrData[1]
            dataDTO.wz = gyrData[2]
        } else {
            //wx, wy, wz
            dataDTO.wx = 0f
            dataDTO.wy = 0f
            dataDTO.wz = 0f
        }
        val magnData = values[TYPE_MAGNETIC_FIELD]?.values
        if (magnData != null) {
            //mx, my, mz
            dataDTO.mx = magnData[0]
            dataDTO.my = magnData[1]
            dataDTO.mz = magnData[2]
        } else {
            //mx, my, mz
            dataDTO.mx = 0f
            dataDTO.my = 0f
            dataDTO.mz = 0f
        }
        val magnUncalcData = values[TYPE_MAGNETIC_FIELD_UNCALIBRATED]?.values
        if (magnUncalcData != null) {
            //mx0, my0, mz0, dmx, dmy, dmz
            dataDTO.mx0 = magnUncalcData[0]
            dataDTO.my0 = magnUncalcData[1]
            dataDTO.mz0 = magnUncalcData[2]
            dataDTO.dmx = magnUncalcData[3]
            dataDTO.dmy = magnUncalcData[4]
            dataDTO.dmz = magnUncalcData[5]
        } else {
            //mx0, my0, mz0, dmx, dmy, dmz
            dataDTO.mx0 = 0f
            dataDTO.my0 = 0f
            dataDTO.mz0 = 0f
            dataDTO.dmx = 0f
            dataDTO.dmy = 0f
            dataDTO.dmz = 0f
        }
        val gyrUncalcData = values[TYPE_GYROSCOPE_UNCALIBRATED]?.values
        if (gyrUncalcData != null) {
            //wx0, wy0, wz0, dwx, dwy, dwz
            dataDTO.wx0 = gyrUncalcData[0]
            dataDTO.wy0 = gyrUncalcData[1]
            dataDTO.wz0 = gyrUncalcData[2]
            dataDTO.dwx = gyrUncalcData[3]
            dataDTO.dwy = gyrUncalcData[4]
            dataDTO.dwz = gyrUncalcData[5]
        } else {
            //wx0, wy0, wz0, dwx, dwy, dwz
            dataDTO.wx0 = 0f
            dataDTO.wy0 = 0f
            dataDTO.wz0 = 0f
            dataDTO.dwx = 0f
            dataDTO.dwy = 0f
            dataDTO.dwz = 0f
        }
        val accUncalcData = values[TYPE_ACCELEROMETER_UNCALIBRATED]?.values
        if (accUncalcData != null) {
            //ax0, ay0, az0, dax, day, daz
            dataDTO.ax0 = accUncalcData[0]
            dataDTO.ay0 = accUncalcData[1]
            dataDTO.az0 = accUncalcData[2]
            dataDTO.dax = accUncalcData[3]
            dataDTO.day = accUncalcData[4]
            dataDTO.daz = accUncalcData[5]
        } else {
            //ax0, ay0, az0, dax, day, daz
            dataDTO.ax0 = 0f
            dataDTO.ay0 = 0f
            dataDTO.az0 = 0f
            dataDTO.dax = 0f
            dataDTO.day = 0f
            dataDTO.daz = 0f
        }

        dataDTO.long = 0.0
        dataDTO.atti = 0.0
        dataDTO.tcnt = Instant.now().toEpochMilli()
        return dataDTO
    }

    private fun getGPSDTO(location: Location): DataDTO {
        val dataDTO = DataDTO()

        //ax, ay, az
        dataDTO.ax = 0f
        dataDTO.ay = 0f
        dataDTO.az = 0f
        //wx, wy, wz
        dataDTO.wx = 0f
        dataDTO.wy = 0f
        dataDTO.wz = 0f
        //mx, my, mz
        dataDTO.mx = 0f
        dataDTO.my = 0f
        dataDTO.mz = 0f
        //mx0, my0, mz0, dmx, dmy, dmz
        dataDTO.mx0 = 0f
        dataDTO.my0 = 0f
        dataDTO.mz0 = 0f
        dataDTO.dmx = 0f
        dataDTO.dmy = 0f
        dataDTO.dmz = 0f
        //wx0, wy0, wz0, dwx, dwy, dwz
        dataDTO.wx0 = 0f
        dataDTO.wy0 = 0f
        dataDTO.wz0 = 0f
        dataDTO.dwx = 0f
        dataDTO.dwy = 0f
        dataDTO.dwz = 0f
        //ax0, ay0, az0, dax, day, daz
        dataDTO.ax0 = 0f
        dataDTO.ay0 = 0f
        dataDTO.az0 = 0f
        dataDTO.dax = 0f
        dataDTO.day = 0f
        dataDTO.daz = 0f
        dataDTO.tcnt = Instant.now().toEpochMilli()
        dataDTO.long = location.longitude
        dataDTO.atti = location.latitude
        return dataDTO
    }

    private fun floatArrayToString(values: FloatArray): String {
        var result = "{"
        for (value in values) {
            result += "$value, "
        }
        return "$result}"
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun startTimer(period: Long) {
        val newPeriod = if (period == 0L) defaultPeriod else period
        timer?.cancel()
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                when (requestType) {
                    1 -> sendLocation()
                    2 -> {
                        sending = true
                    }
                    else -> {
                        sending = true
                    }
                }
            }
        }, newPeriod)
    }

    private fun sendLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY, null
        ).addOnSuccessListener {
            send(getGPSDTO(it))
        }
    }
}