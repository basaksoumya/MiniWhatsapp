package com.example.miniwhatsapp

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import kotlinx.android.synthetic.main.activity_otp.*
import java.util.concurrent.TimeUnit
import android.text.style.ClickableSpan as ClickableSpan

const val PHONE_NUMBER = "phoneNumber";

class OtpActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var callbacks:PhoneAuthProvider.OnVerificationStateChangedCallbacks
    var phoneNumber :String? = null
    var mVerificationId:String ? = null;
    var mResendToken:PhoneAuthProvider.ForceResendingToken ? = null;
    val auth = FirebaseAuth.getInstance()
    private lateinit var progressDialog: ProgressDialog;
    private var mCounterDown : CountDownTimer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.miniwhatsapp.R.layout.activity_otp)
        initView()
        startVerify()

    }

    private fun startVerify() {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber!!)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        showTimer(60000)
        //progressDialog = createProgressDialog("sending a verification code",false);
       // progressDialog.show();
    }

    private fun showTimer(milliSecInFuture: Long) {
        resendBtn.isEnabled = false;
        mCounterDown = object : CountDownTimer(milliSecInFuture,1000) {
            override fun onTick(milliSecUntilFinished: Long) {
                counterTv.isVisible = true;
                val counterValue = milliSecUntilFinished/1000;
                counterTv.text = "Second Remaining :$counterValue"
            }

            override fun onFinish() {
                resendBtn.isEnabled = true;
                counterTv.isVisible = false;
            }

        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(mCounterDown != null) {
            mCounterDown!!.cancel()
        }
    }

    private fun initView() {
        phoneNumber = intent.getStringExtra(PHONE_NUMBER)
        verifyTv.text = "Verify $phoneNumber";
        setSpannableString()

        verificationBtn.setOnClickListener(this)
        resendBtn.setOnClickListener(this)


        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                //Log.d(TAG, "onVerificationCompleted:$credential")

//                if(::progressDialog.isInitialized) {
//                    progressDialog.dismiss()
//                }

                val smsCode = credential.smsCode
                if(!smsCode.isNullOrBlank())
                    sentcodeEt.setText(smsCode);

                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                //Log.w(TAG, "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }

                // Show a message and update the UI
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                //Log.d(TAG, "onCodeSent:$verificationId")

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId
                mResendToken = token
            }
        }

    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener{
                    if(it.isSuccessful) {
                        startActivity(
                            Intent(this, SignUpActivity::class.java)
                        )
                        finish()
                    }else{
                        notifyUserAndRetry("Your Phone Number Verification is failed.Retry again!")
                    }
            }
    }

    private fun notifyUserAndRetry(message: String) {
        MaterialAlertDialogBuilder(this).apply {
            setMessage(message)
            setPositiveButton("Ok") {_,_->
                showLoginActivity()
            }
            setNegativeButton("Cancel") {dialog, _->
                dialog.dismiss()
            }
            setCancelable(false);
            create()
            show()
        }
    }

    private fun setSpannableString() {
        val span = SpannableString("Waiting to automatically detect an sms sent to\n $phoneNumber Wrong Number?")
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(p0: View) {
                //send back;
                showLoginActivity()
            }


            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false;
                ds.color = ds.linkColor
            }
        }
        span.setSpan(clickableSpan,span.length - 13,span.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        waitingTv.movementMethod = LinkMovementMethod.getInstance()
        waitingTv.text = span;
    }

    private fun showLoginActivity() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }


    override fun onBackPressed() {

    }

    override fun onClick(p0: View?) {
        when(p0) {
            verificationBtn -> {

                val code = sentcodeEt.text.toString()
                if(code.isNotEmpty() && !mVerificationId.isNullOrBlank()) {
                    progressDialog = createProgressDialog("Please wait...",false)
                    progressDialog.show()

                    val credential = PhoneAuthProvider.getCredential(mVerificationId!!,code)
                    signInWithPhoneAuthCredential(credential);
                }

            }

            resendBtn -> {

                val code = sentcodeEt.text.toString()
                if(mResendToken != null) {
                    showTimer(60000);
                    progressDialog = createProgressDialog("sending a verification code ",false)
                    progressDialog.show()

                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNumber!!)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                        .setForceResendingToken(mResendToken!!)
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                }

            }
        }
    }
}

fun Context.createProgressDialog(message: String,isCancelable : Boolean):ProgressDialog {
    return ProgressDialog(this).apply {
        setCancelable(false)
        setMessage(message);
        setCanceledOnTouchOutside(false);
    }
}