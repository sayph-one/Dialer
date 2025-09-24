package com.simplemobiletools.dialer.services

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.getMyContactsCursor
import com.simplemobiletools.commons.extensions.isNumberBlocked
import com.simplemobiletools.commons.extensions.normalizePhoneNumber
import com.simplemobiletools.commons.helpers.SimpleContactsHelper
import com.simplemobiletools.dialer.helpers.ContactFiltering

@RequiresApi(Build.VERSION_CODES.N)
class SimpleCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart

        // Force flags to block unknown / hidden callers
        val blockUnknown = true
        val blockHidden  = true

        when {
            number != null && isNumberBlocked(number.normalizePhoneNumber()) -> {
                respondToCall(callDetails, isBlocked = true)
            }

            number != null && blockUnknown -> {
                // Check if contact exists and is saved to device (not SIM)
                val normalizedNumber = number.normalizePhoneNumber()
                val isDeviceContact = ContactFiltering.isContactSavedToDevice(this, normalizedNumber)

                if (isDeviceContact) {
                    // Contact exists in device storage, allow the call
                    respondToCall(callDetails, isBlocked = false)
                } else {
                    // Either no contact found, or contact is only on SIM - block the call
                    respondToCall(callDetails, isBlocked = true)
                }
            }

            number == null && blockHidden -> {
                respondToCall(callDetails, isBlocked = true)
            }

            else -> {
                respondToCall(callDetails, isBlocked = false)
            }
        }
    }

    private fun respondToCall(callDetails: Call.Details, isBlocked: Boolean) {
        val response = CallResponse.Builder()
            .setDisallowCall(isBlocked)
            .setRejectCall(isBlocked)
            .setSkipCallLog(isBlocked)
            .setSkipNotification(isBlocked)
            .build()
        respondToCall(callDetails, response)
    }
}
