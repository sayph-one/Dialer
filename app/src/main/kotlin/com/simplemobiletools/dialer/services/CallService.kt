package com.simplemobiletools.dialer.services

import android.app.KeyguardManager
import android.content.Context
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.sayph.android.commons.SayphStateChecker
import com.simplemobiletools.dialer.activities.CallActivity
import com.simplemobiletools.dialer.extensions.config
import com.simplemobiletools.dialer.extensions.isOutgoing
import com.simplemobiletools.dialer.extensions.powerManager
import com.simplemobiletools.dialer.helpers.CallManager
import com.simplemobiletools.dialer.helpers.CallNotificationManager
import com.simplemobiletools.dialer.helpers.NoCall

class CallService : InCallService() {
    private val callNotificationManager by lazy { CallNotificationManager(this) }

    private val callListener = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                callNotificationManager.cancelNotification()
            } else {
                callNotificationManager.setupNotification()
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        if (shouldGateIncomingCall(call)) {
            Log.d("CallService", "Rejecting incoming call — device locked, caller not in emergency contacts")
            call.reject(false, null)
            return
        }

        CallManager.onCallAdded(call)
        CallManager.inCallService = this
        call.registerCallback(callListener)

        val isScreenLocked = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked
        if (!powerManager.isInteractive || call.isOutgoing() || isScreenLocked || config.alwaysShowFullscreen) {
            try {
                callNotificationManager.setupNotification(true)
                startActivity(CallActivity.getStartIntent(this))
            } catch (e: Exception) {
                // seems like startActivity can throw AndroidRuntimeException and ActivityNotFoundException, not yet sure when and why, lets show a notification
                callNotificationManager.setupNotification()
            }
        } else {
            callNotificationManager.setupNotification()
        }
    }

    /**
     * Should this incoming call be rejected before it rings?
     *
     * Outgoing calls (e.g. emergency contact tapped on the Lawnchair downtime or set-up
     * overlay) always proceed. Incoming calls are rejected when the device is in a locked
     * state — downtime OR set-up incomplete (missing required permissions) — UNLESS the
     * caller's number matches an emergency contact. Matching uses
     * [PhoneNumberUtils.compare] which handles country-code, formatting, and partial-match
     * differences ("+44 7123 456 789" vs "07123456789" etc.).
     *
     * Unknown callers (no handle / withheld number) are rejected when locked — the parent
     * should call from a number that's registered as an emergency contact.
     */
    private fun shouldGateIncomingCall(call: Call): Boolean {
        if (call.isOutgoing()) return false

        val state = SayphStateChecker.getState(this)
        val isLocked = state.isInDowntime || !state.permissionsOk
        if (!isLocked) return false

        val callerNumber = try {
            call.details?.handle?.schemeSpecificPart
        } catch (e: Exception) {
            null
        }
        if (callerNumber.isNullOrBlank()) {
            Log.d("CallService", "Locked + no caller number — rejecting")
            return true
        }

        val contacts = try {
            SayphStateChecker.getEmergencyContacts(this)
        } catch (e: Exception) {
            Log.w("CallService", "Failed to read emergency contacts; defaulting to reject", e)
            emptyList()
        }

        val matches = contacts.any { PhoneNumberUtils.compare(it.phone, callerNumber) }
        if (matches) {
            Log.d("CallService", "Caller $callerNumber matches an emergency contact — letting through")
            return false
        }

        Log.d("CallService", "Caller $callerNumber not in emergency contacts — rejecting")
        return true
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callListener)
        val wasPrimaryCall = call == CallManager.getPrimaryCall()
        CallManager.onCallRemoved(call)
        if (CallManager.getPhoneState() == NoCall) {
            CallManager.inCallService = null
            callNotificationManager.cancelNotification()
        } else {
            callNotificationManager.setupNotification()
            if (wasPrimaryCall) {
                startActivity(CallActivity.getStartIntent(this))
            }
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        if (audioState != null) {
            CallManager.onAudioStateChanged(audioState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callNotificationManager.cancelNotification()
    }
}
