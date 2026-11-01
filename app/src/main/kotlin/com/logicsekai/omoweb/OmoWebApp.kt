// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

package com.logicsekai.omoweb

import android.app.Application
import com.logicsekai.omoweb.update.UpdateChecker
import com.logicsekai.omoweb.update.UpdateManager
import com.logicsekai.omoweb.update.UpdateManagerOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds the app-wide [UpdateManager] (also reached by the background update worker). */
class OmoWebApp : Application(), UpdateManagerOwner {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val updateManager: UpdateManager by lazy {
        UpdateManager(
            context = this,
            checker = UpdateChecker(BuildConfig.UPDATE_REPO, BuildConfig.VERSION_NAME),
            canSelfInstall = BuildConfig.SELF_UPDATE,
            scope = appScope,
        )
    }

    override fun onCreate() {
        super.onCreate()
        updateManager.start()
    }
}
