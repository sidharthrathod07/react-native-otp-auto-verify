package com.otpautoverify

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
class OtpAutoVerifyModule(reactContext: ReactApplicationContext) :
    NativeOtpAutoVerifySpec(reactContext), LifecycleEventListener {

    companion object {
        const val NAME = NativeOtpAutoVerifySpec.NAME
        private const val TAG = "OtpAutoVerifyModule"
        const val OTP_RECEIVED_EVENT = "otpReceived"
    }

    private var smsReceiver: SmsRetrieverBroadcastReceiver? = null
    private var isReceiverRegistered = false
    private var isListening = false

    init {
        reactContext.addLifecycleEventListener(this)
    }

    override fun getTypedExportedConstants(): MutableMap<String, Any> {
        return mutableMapOf("OTP_RECEIVED_EVENT" to OTP_RECEIVED_EVENT)
    }

    override fun getHash(promise: Promise) {
        try {
            val helper = AppSignatureHelper(reactApplicationContext)
            val signatures = helper.getAppSignatures()
            val arr = Arguments.createArray()
            for (s in signatures) {
                arr.pushString(s)
            }
            promise.resolve(arr)
        } catch (e: Exception) {
            Log.e(TAG, "getHash failed", e)
            promise.reject("GET_HASH_ERROR", e.message, e)
        }
    }

    override fun startSmsRetriever(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("NO_ACTIVITY", "No current activity. Ensure the app is in the foreground.")
            return
        }

        registerReceiverIfNecessary(activity)

        val client = SmsRetriever.getClient(reactApplicationContext)
        client.startSmsRetriever()
            .addOnSuccessListener(OnSuccessListener {
                Log.d(TAG, "SMS retriever started")
                isListening = true
                promise.resolve(true)
            })
            .addOnFailureListener(OnFailureListener { e ->
                Log.e(TAG, "Failed to start SMS retriever", e)
                promise.reject("START_SMS_RETRIEVER_ERROR", e.message, e)
            })
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiverIfNecessary(activity: android.app.Activity) {
        if (isReceiverRegistered) return
        try {
            smsReceiver = SmsRetrieverBroadcastReceiver(reactApplicationContext, OTP_RECEIVED_EVENT)
            val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(
                    smsReceiver,
                    filter,
                    SmsRetriever.SEND_PERMISSION,
                    null,
                    android.content.Context.RECEIVER_EXPORTED
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.registerReceiver(
                    smsReceiver,
                    filter,
                    android.content.Context.RECEIVER_EXPORTED
                )
            } else {
                activity.registerReceiver(smsReceiver, filter)
            }
            isReceiverRegistered = true
            Log.d(TAG, "SMS receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register SMS receiver", e)
        }
    }

    private fun unregisterReceiver() {
        if (!isReceiverRegistered || smsReceiver == null) {
            isListening = false
            return
        }
        val activity = currentActivity
        try {
            if (activity != null) {
                activity.unregisterReceiver(smsReceiver)
                Log.d(TAG, "SMS receiver unregistered")
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver already unregistered", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister SMS receiver", e)
        } finally {
            isReceiverRegistered = false
            smsReceiver = null
            isListening = false
        }
    }

    override fun addListener(eventName: String) {
        // Required for NativeEventEmitter; no-op.
    }

    override fun removeListeners(count: Double) {
        unregisterReceiver()
    }

    override fun onHostResume() {
        // Optionally re-register if we were listening and activity was recreated.
        if (isListening && currentActivity != null && !isReceiverRegistered) {
            currentActivity?.let { registerReceiverIfNecessary(it) }
        }
    }

    override fun onHostPause() {
        unregisterReceiver()
    }

    override fun onHostDestroy() {
        unregisterReceiver()
        reactApplicationContext.removeLifecycleEventListener(this)
    }

    override fun invalidate() {
        unregisterReceiver()
        reactApplicationContext.removeLifecycleEventListener(this)
        super.invalidate()
    }
}
