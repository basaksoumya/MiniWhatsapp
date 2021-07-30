package com.example.miniwhatsapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.example.miniwhatsapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    private lateinit var phoneNumber:String
    private lateinit var countryCode:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        phoneNumberEt.addTextChangedListener {
            nextBtn.isEnabled = !(it.isNullOrEmpty() || it.length < 10)
        }

        nextBtn.setOnClickListener{
            checkNumber()
        }
    }

    private fun checkNumber() {
        countryCode = ccp.selectedCountryCodeWithPlus
        phoneNumber = countryCode + phoneNumberEt.text.toString()

        notifyUser()
    }

    private fun notifyUser() {
        MaterialAlertDialogBuilder(this).apply {
            setMessage("We will be verifying the phone number : $phoneNumber\n" +
            "Is this OK,or would you like to edit the number?")
            setPositiveButton("OK") { _,_->
                showOtpActivity();
            }
            setNegativeButton("EDIT") {dialog, which->
                dialog.dismiss()
            }
            setCancelable(false)
            create()
            show()
        }
    }

    private fun showOtpActivity() {
        startActivity(
            Intent(this, OtpActivity::class.java).putExtra(PHONE_NUMBER, phoneNumber)
        )
        finish()
    }
}
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}