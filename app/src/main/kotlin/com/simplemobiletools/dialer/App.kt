package com.simplemobiletools.dialer

import android.app.Application
import com.sayph.android.commons.SayphActivityGuard
import com.simplemobiletools.commons.extensions.checkUseEnglish
import com.simplemobiletools.dialer.activities.CallActivity
import com.simplemobiletools.dialer.helpers.Config

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SayphActivityGuard.install(this)
        // CallActivity must remain accessible during downtime so outgoing emergency
        // calls from the Lawnchair downtime overlay show the in-call screen.
        // Incoming calls are already rejected in CallService.onCallAdded.
        SayphActivityGuard.excludeActivity(CallActivity::class.java)
        checkUseEnglish()

        // Trigger tab migration early, before any Activity reads showTabs
        Config.newInstance(this).showTabs
    }
}
