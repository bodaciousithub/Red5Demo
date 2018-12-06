package com.lite.red5demo

import android.Manifest
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.activity_main.*
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import android.widget.ArrayAdapter

class MainActivity : AppCompatActivity() {

    private var selectedDimension: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val categories = ArrayList<String>()
        categories.add("Screen Dimension")
        categories.add("1920 * 1080 (Default)")
        categories.add("640 * 480")
        categories.add("640 * 360")

        val dataAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = dataAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.e("TAG", "p2: $p2,  p3: $p3")
                if (p2 == 0)
                    selectedDimension = 1
                else
                    selectedDimension = p2
            }
        }

        askPermissions(true, true)
        publish.setOnClickListener {
            if (validateFields()) {
                askPermissions(true, false)
            }
        }
        subscribe.setOnClickListener {
            if (validateFields()) {
                askPermissions(false, false)
            }
        }
    }

    private fun validateFields(): Boolean {
        if (TextUtils.isEmpty(server_address.text))
            return false
        if (TextUtils.isEmpty(server_port.text))
            return false
        if (TextUtils.isEmpty(stream_name.text))
            return false
        return true
    }

    private fun askPermissions(publish: Boolean, init: Boolean) {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        Permissions.check(this, permissions, null, null, object : PermissionHandler() {
            override fun onGranted() {
                if (!init) {
                    if (publish) {
                        val intent = Intent(this@MainActivity, PublishActivity::class.java)
                        intent.putExtra("server", server_address.text.toString())
                        intent.putExtra("port", server_port.text.toString())
                        intent.putExtra("stream", stream_name.text.toString())
                        intent.putExtra("dimension", selectedDimension)
                        startActivity(intent)
                    } else {
                        val intent = Intent(this@MainActivity, SubscribeActivity::class.java)
                        intent.putExtra("server", server_address.text.toString())
                        intent.putExtra("port", server_port.text.toString())
                        intent.putExtra("stream", stream_name.text.toString())
                        intent.putExtra("dimension", selectedDimension)
                        startActivity(intent)
                    }
                }
            }
        })
    }

}
