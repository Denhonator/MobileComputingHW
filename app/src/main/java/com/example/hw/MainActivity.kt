package com.example.hw

import android.app.ListActivity
import android.app.PendingIntent.getActivity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import java.time.LocalDateTime


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
        adapter = ArrayAdapter(this,
                android.R.layout.simple_list_item_1,
                listItems)
        listAdapter = adapter
        var mydatabase = openOrCreateDatabase("RemindersDB", MODE_PRIVATE, null)
        //mydatabase.execSQL ("DROP TABLE IF EXISTS Reminders")
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS Reminders(message VARCHAR," +
                "location_x VARCHAR,location_y VARCHAR,reminder_time VARCHAR," +
                "creation_time VARCHAR primary key,creator_id integer,reminder_seen integer, icon integer);")
        val resultSet: Cursor = mydatabase.rawQuery("Select * from Reminders", null)
        if(resultSet.moveToFirst()) {
            do {
                var newReminder = Reminder(resultSet.getString(0),
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getInt(5),
                        resultSet.getInt(6) != 0,
                        resultSet.getInt(7))
                reminderList.add(newReminder)
                adapter?.add(newReminder.message)
            } while (resultSet.moveToNext())
        }

        listView.setOnItemClickListener{parent, view, position, id ->
            //Toast.makeText(this@MainActivity, "You have Clicked $position", Toast.LENGTH_SHORT).show()
            state="MAIN"
            editItems(view, "Delete", "Confirm")
            val editReminder: EditText? = findViewById(R.id.ReminderMessage)
            editReminder?.setText(reminderList[position].message)
            curIcon = reminderList[position].icon
            val image: ImageButton? = findViewById(R.id.imageButton)
            image?.setImageResource(if(curIcon==0) R.drawable.time else R.drawable.night)
            reminderIndex = position
            mydatabase.delete("Reminders", "creation_time = ?", Array(1) { i -> reminderList[position].creation_time } )
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
        if(state=="EDIT"){
            var newReminder = Reminder(editReminder?.text.toString(), "", "",
                    "", LocalDateTime.now().toString(), 0, false, curIcon)
            reminderList.add(reminderIndex, newReminder)
            adapter?.clear()
            for(r in reminderList){
                adapter?.add(r.message)
            }
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
            val inserted = mydatabase.insert("Reminders", null, contentValues)
            //Toast.makeText(this@MainActivity, "Inserted: $inserted", Toast.LENGTH_SHORT).show()
            LogOut(v)
        }
        else {
            editReminder?.visibility = View.VISIBLE
            val NOButton: Button? = findViewById(R.id.NOButton)
            NOButton?.text = notext
            val OKButton: Button? = findViewById(R.id.OKButton)
            OKButton?.text = oktext
            listView.visibility = View.GONE
            image?.visibility = View.VISIBLE

            curIcon = 0
            reminderIndex = 0
            image?.setImageResource(if(curIcon==0) R.drawable.time else R.drawable.night)

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
        image?.setImageResource(if(curIcon==0) R.drawable.time else R.drawable.night)
    }
}