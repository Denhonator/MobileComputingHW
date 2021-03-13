package com.example.hw

import android.app.ListActivity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.File
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue
import kotlin.math.pow


data class Reminder(
    val message: String = "Message",
    val location_x: Double = 0.0,
    val location_y: Double = 0.0,
    val reminder_time: String = "",
    val creation_time: String = "",
    val creator_id: Int = 0,
    val reminder_seen: Boolean = false,
    val icon: Int = 0
)

fun showNofitication(context: Context, message: String, icon: Int) {

    val CHANNEL_ID = "MCHWREMINDER"
    var notificationId = 3456
    // notificationId += Random(notificationId).nextInt(1, 500)

    var notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(icon)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setGroup(CHANNEL_ID)

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Notification chancel needed since Android 8
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.app_name)
        }
        notificationManager.createNotificationChannel(channel)
    }

    notificationManager.notify(notificationId, notificationBuilder.build())

}

fun intSign(num: Int): Int {
    if(num>0)
        return 1
    if(num==0)
        return 0
    return -1
}

class ReminderWorker(appContext: Context, workerParams: WorkerParameters):
        Worker(appContext, workerParams) {
    var con = appContext
    var params = workerParams
    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {
        val locx = inputData.getDouble("location_x", 0.0)
        val locy = inputData.getDouble("location_y", 0.0)

        inputData.getString("message")?.let { showNofitication(
            con, it, inputData.getInt(
                "icon",
                R.drawable.time
            )
        )
        return  Result.success()}

        return Result.failure()
    }
}

class MainActivity : ListActivity(), OnMapReadyCallback {
    var listItems = ArrayList<String>()
    var reminderList = ArrayList<Reminder>()
    var state = "MAIN"
    var curIcon = 0
    var reminderIndex = 0
    var gmap : GoogleMap? = null
    var circleLatLng : LatLng = LatLng(65.0, 25.5)
    var circleRad = 0.0

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    var adapter: ArrayAdapter<String>? = null

    val mapCickListener = object : GoogleMap.OnMapClickListener {
        override fun onMapClick(p0: LatLng?) {
            gmap?.clear()
            circleRad = 100.0
            if (p0 != null && state == "EDIT") {
                circleLatLng = p0
                gmap?.addCircle(CircleOptions().center(p0).radius(circleRad))
                Log.d("ME", p0.toString()+" "+circleRad.toString())
            }
            else if(p0 != null){
                gmap?.addMarker(MarkerOptions().position(p0))
                for(r in reminderList){
                    val locdif = (p0.latitude-r.location_x).absoluteValue + (p0.longitude-r.location_y).absoluteValue
                    val rtimesplit = r.reminder_time.split(".")
                    val year = rtimesplit[0].toInt()
                    val month = rtimesplit[1].toInt()
                    val day = rtimesplit[2].toInt()
                    val hour = rtimesplit[3].toInt()
                    val minute = rtimesplit[4].toInt()

                    val date: Date = Date()
                    val curMillis = date.time

                    val calendar = Calendar.getInstance()
                    calendar.set(year, month, day, hour, minute)
                    val pickedMillis = calendar.timeInMillis

                    if(locdif<0.005 && curMillis >= pickedMillis){
                        var mydatabase = openOrCreateDatabase("RemindersDB", MODE_PRIVATE, null)
                        mydatabase.delete(
                                "Reminders",
                                "creation_time = ?",
                                Array(1) { i -> r.creation_time })
                        reminderList.remove(r)
                        adapter?.clear()
                        for(r in reminderList){
                            adapter?.add(r.message)
                        }
                        showNofitication(applicationContext, r.message, if (r.icon == 0) R.drawable.time else R.drawable.night)
                    }
                }
            }
        }
    }

    override fun onMapReady(mMap: GoogleMap) {
        Log.d("ME", "MAP READY")
        gmap = mMap
        gmap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(65.0, 25.5), 10.0f))
        mMap.setOnMapClickListener(mapCickListener)
    }

    override fun onResume() {
        val mapView = findViewById<MapView>(R.id.mapView4);
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        val mapView = findViewById<MapView>(R.id.mapView4);
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        val mapView = findViewById<MapView>(R.id.mapView4);
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        val mapView = findViewById<MapView>(R.id.mapView4);
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        val mapView = findViewById<MapView>(R.id.mapView4);
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        val mapView = findViewById<MapView>(R.id.mapView4);
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listItems
        )

        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle("MapViewBundleKey")
        }

        val mapView = findViewById<MapView>(R.id.mapView4);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        listAdapter = adapter
        var mydatabase = openOrCreateDatabase("RemindersDB", MODE_PRIVATE, null)
        //mydatabase.execSQL ("DROP TABLE IF EXISTS Reminders")
        mydatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Reminders(message VARCHAR," +
                    "location_x DOUBLE,location_y DOUBLE,reminder_time VARCHAR," +
                    "creation_time VARCHAR primary key,creator_id integer,reminder_seen integer, icon integer);"
        )

        val resultSet: Cursor = mydatabase.rawQuery("Select * from Reminders", null)
        if(resultSet.moveToFirst()) {
            do {
                var newReminder = Reminder(
                    resultSet.getString(0),
                    resultSet.getDouble(1),
                    resultSet.getDouble(2),
                    resultSet.getString(3),
                    resultSet.getString(4),
                    resultSet.getInt(5),
                    resultSet.getInt(6) != 0,
                    resultSet.getInt(7)
                )
                reminderList.add(newReminder)
                adapter?.add(newReminder.message)
            } while (resultSet.moveToNext())
        }

        listView.setOnItemClickListener{ parent, view, position, id ->
            //Toast.makeText(this@MainActivity, "You have Clicked $position", Toast.LENGTH_SHORT).show()
            state="MAIN"
            editItems(view, "Delete", "Confirm")
            val editReminder: EditText? = findViewById(R.id.ReminderMessage)
            editReminder?.setText(reminderList[position].message)
            curIcon = reminderList[position].icon
            val image: ImageButton? = findViewById(R.id.imageButton)
            image?.setImageResource(if (curIcon == 0) R.drawable.time else R.drawable.night)
            val timePicker1: TimePicker = findViewById(R.id.timePicker1);
            val datePicker: DatePicker = findViewById(R.id.simpleDatePicker)
            val times = reminderList[position].reminder_time.split(".")
            datePicker.updateDate(times[0].toInt(), times[1].toInt(), times[2].toInt())
            timePicker1.hour = times[3].toInt()
            timePicker1.minute = times[4].toInt()
            val notif: Switch? = findViewById(R.id.switch1)
            gmap?.clear()
            circleLatLng = LatLng(reminderList[position].location_x, reminderList[position].location_y)
            gmap?.addCircle(CircleOptions().center(circleLatLng).radius(100.0))
            notif?.isChecked = false
            reminderIndex = position
            mydatabase.delete(
                "Reminders",
                "creation_time = ?",
                Array(1) { i -> reminderList[position].creation_time })
            reminderList.removeAt(position)
            adapter?.clear()
            for(r in reminderList){
                adapter?.add(r.message)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun editItems(v: View?, notext: String = "Cancel", oktext: String = "Confirm") {
        val editReminder: EditText? = findViewById(R.id.ReminderMessage)
        val image: ImageButton? = findViewById(R.id.imageButton)
        val timePicker1: TimePicker = findViewById(R.id.timePicker1);
        val datePicker: DatePicker = findViewById(R.id.simpleDatePicker)
        val notif: Switch? = findViewById(R.id.switch1)

        if(state=="EDIT"){
            var newReminder = Reminder(
                editReminder?.text.toString(), circleLatLng.latitude, circleLatLng.longitude,
                "${datePicker.year}.${datePicker.month}.${datePicker.dayOfMonth}.${timePicker1.hour}.${timePicker1.minute}",
                LocalDateTime.now().toString(), 0, false, curIcon
            )

            val contentValues = ContentValues()
            contentValues.put("message", newReminder.message)
            contentValues.put("location_x", newReminder.location_x)
            contentValues.put("location_y", newReminder.location_y)
            contentValues.put("reminder_time", newReminder.reminder_time)
            contentValues.put("creation_time", newReminder.creation_time)
            contentValues.put("creator_id", newReminder.creator_id)
            contentValues.put("reminder_seen", if (newReminder.reminder_seen) 1 else 0)
            contentValues.put("icon", newReminder.icon)
            var mydatabase = openOrCreateDatabase("RemindersDB", MODE_PRIVATE, null)
            //Toast.makeText(this@MainActivity, "Inserted: $inserted", Toast.LENGTH_SHORT).show()
            LogOut(v)

            if(notif?.isChecked == true) {
                val date: Date = Date()
                val curMillis = date.time

                val calendar = Calendar.getInstance()
                calendar.set(
                    datePicker.year,
                    datePicker.month,
                    datePicker.dayOfMonth,
                    timePicker1.hour,
                    timePicker1.minute
                )
                val pickedMillis = calendar.timeInMillis

                val reminderWorkRequest: WorkRequest =
                    OneTimeWorkRequestBuilder<ReminderWorker>()
                        .setInitialDelay(pickedMillis - curMillis, TimeUnit.MILLISECONDS)
                        .setInputData(
                            workDataOf(
                                "message" to newReminder.message,
                                "icon" to if (curIcon == 0) R.drawable.time else R.drawable.night,
                                "location_x" to newReminder.location_x,
                                "location_y" to newReminder.location_y
                            )
                        )
                        .build()
                WorkManager
                        .getInstance(this@MainActivity)
                        .enqueue(reminderWorkRequest)
            }
            else{
                reminderList.add(reminderIndex, newReminder)
                val inserted = mydatabase.insert("Reminders", null, contentValues)
            }

            adapter?.clear()
            for(r in reminderList){
                adapter?.add(r.message)
            }
        }
        else {
            editReminder?.visibility = View.VISIBLE
            editReminder?.setText("Message")
            val NOButton: Button? = findViewById(R.id.NOButton)
            NOButton?.text = notext
            val OKButton: Button? = findViewById(R.id.OKButton)
            OKButton?.text = oktext
            listView.visibility = View.GONE
            image?.visibility = View.VISIBLE
            timePicker1.visibility = View.VISIBLE
            datePicker.visibility = View.VISIBLE
            notif?.visibility = View.VISIBLE

            val timePicker1: TimePicker = findViewById(R.id.timePicker1);
            val datePicker: DatePicker = findViewById(R.id.simpleDatePicker)
            datePicker.updateDate(
                LocalDateTime.now().year,
                LocalDateTime.now().monthValue - 1,
                LocalDateTime.now().dayOfMonth
            )
            timePicker1.hour = LocalDateTime.now().hour
            timePicker1.minute = LocalDateTime.now().minute
            val notif: Switch? = findViewById(R.id.switch1)
            notif?.isChecked = false

            curIcon = 0
            reminderIndex = 0
            image?.setImageResource(if (curIcon == 0) R.drawable.time else R.drawable.night)

            state = "EDIT"
        }
    }

    fun LogOut(view: View?) {
        if(state=="MAIN")
            startActivity(Intent(this, Login::class.java))
        else {
            val editReminder: EditText? = findViewById(R.id.ReminderMessage)
            editReminder?.visibility = View.GONE
            val NOButton: Button? = findViewById(R.id.NOButton)
            NOButton?.text = "Log out"
            val OKButton: Button? = findViewById(R.id.OKButton)
            OKButton?.text = "New"
            listView.visibility = View.VISIBLE
            val image: ImageButton? = findViewById(R.id.imageButton)
            image?.visibility = View.GONE
            val timePicker1: TimePicker? = findViewById(R.id.timePicker1);
            timePicker1?.visibility = View.GONE
            val datePicker: DatePicker? = findViewById(R.id.simpleDatePicker)
            datePicker?.visibility = View.GONE
            val notif: Switch? = findViewById(R.id.switch1)
            notif?.visibility = View.GONE

            state = "MAIN"
        }
        //finish()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addItems(view: View) {
        editItems(view)
    }

    fun changeIcon(view: View) {
        curIcon = (curIcon+1)%2
        val image: ImageButton? = findViewById(R.id.imageButton)
        image?.setImageResource(if (curIcon == 0) R.drawable.time else R.drawable.night)
    }
}