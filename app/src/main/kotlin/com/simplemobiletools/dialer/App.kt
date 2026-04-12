package com.simplemobiletools.dialer

import android.app.Application
import com.sayph.android.commons.SayphActivityGuard
import com.simplemobiletools.commons.extensions.checkUseEnglish
import com.simplemobiletools.dialer.helpers.Config

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SayphActivityGuard.install(this)
        checkUseEnglish()

        // Trigger tab migration early, before any Activity reads showTabs
        Config.newInstance(this).showTabs
    }
}
