package com.example.hw

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import java.io.File
import java.io.FileWriter

class Login : AppCompatActivity() {
    private val padNums = Array(4) {0}
    private val users = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val letDirectory = File(filesDir, "LET")
        letDirectory.mkdirs()

        var userdata = File(letDirectory,"users")
        if(!userdata.exists()) {
            Log.d("ME", "Does not exist")
            val isNewFileCreated: Boolean = userdata.createNewFile()
            if (isNewFileCreated) {
                userdata.writeText("Name 1234\n")
            }
        }
        userdata = File(letDirectory,"users")
        val reader = userdata.bufferedReader()
        val text = reader.readLines()
        for(line in text){
            Log.d("ME", line)
            val splits = line.split(" ")
            users[splits[0]] = splits[1]
        }
    }

    fun KeypadPress(view: View) {
        var editTextHello = findViewById(R.id.editTextTextPersonName) as EditText
        val num = view.tag.toString().toInt()

        for(i in 1 until padNums.size){
            padNums[i-1] = padNums[i]
        }
        padNums[padNums.size-1] = num

        var code = ""
        for(n in padNums){
            code += n.toString()
        }

        Log.d("ME", code+editTextHello.text)
        if(users[editTextHello.text.toString()] == code){
            startActivity(Intent(this, MainActivity::class.java))
            //setContentView(R.layout.activity_main)
        }
    }

    fun UpdateUsers(){
        val letDirectory = File(filesDir, "LET")
        var userdata = File(letDirectory,"users")
        userdata.createNewFile()
        userdata.writeText("Name 1234\n")
        for ((key, value) in users){
            Log.d("ME", key)
            userdata.appendText(key.replace(" ", "")+" "+value+"\n")
        }
    }

    fun Register(view: View) {
        var editTextHello = findViewById(R.id.editTextTextPersonName) as EditText
        var code = ""
        for(n in padNums){
            code += n.toString()
        }
        users[editTextHello.text.toString()] = code
        Toast.makeText(this@Login, "Registered: " + editTextHello.text.toString(), Toast.LENGTH_SHORT).show()
        UpdateUsers()
    }
}