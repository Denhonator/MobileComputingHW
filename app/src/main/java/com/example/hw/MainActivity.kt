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
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

data class Reminder(
    val message: String = "Message",
    val location_x: String = "",
    val location_y: String = "",
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

class MainActivity : ListActivity() {
    var listItems = ArrayList<String>()
    var reminderList = ArrayList<Reminder>()
    var state = "MAIN"
    var curIcon = 0
    var reminderIndex = 0

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    var adapter: ArrayAdapter<String>? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listItems
        )
        listAdapter = adapter
        var mydatabase = openOrCreateDatabase("RemindersDB", MODE_PRIVATE, null)
        //mydatabase.execSQL ("DROP TABLE IF EXISTS Reminders")
        mydatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS Reminders(message VARCHAR," +
                    "location_x VARCHAR,location_y VARCHAR,reminder_time VARCHAR," +
                    "creation_time VARCHAR primary key,creator_id integer,reminder_seen integer, icon integer);"
        )
        val resultSet: Cursor = mydatabase.rawQuery("Select * from Reminders", null)
        if(resultSet.moveToFirst()) {
            do {
                var newReminder = Reminder(
                    resultSet.getString(0),
                    resultSet.getString(1),
                    resultSet.getString(2),
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
                editReminder?.text.toString(), "", "",
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
                calendar.set(datePicker.year, datePicker.month, datePicker.dayOfMonth, timePicker1.hour, timePicker1.minute)
                val pickedMillis = calendar.timeInMillis

                val reminderWorkRequest: WorkRequest =
                    OneTimeWorkRequestBuilder<ReminderWorker>()
                        .setInitialDelay(pickedMillis-curMillis, TimeUnit.MILLISECONDS)
                        .setInputData(
                            workDataOf(
                                "message" to newReminder.message,
                                "icon" to if (curIcon == 0) R.drawable.time else R.drawable.night
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
            timePicker1?.visibility = View.VISIBLE
            datePicker?.visibility = View.VISIBLE
            notif?.visibility = View.VISIBLE

            val timePicker1: TimePicker = findViewById(R.id.timePicker1);
            val datePicker: DatePicker = findViewById(R.id.simpleDatePicker)
            datePicker.updateDate(LocalDateTime.now().year, LocalDateTime.now().monthValue-1, LocalDateTime.now().dayOfMonth)
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