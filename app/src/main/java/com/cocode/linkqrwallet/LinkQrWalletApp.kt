package com.cocode.linkqrwallet

import android.app.Application
import com.cocode.linkqrwallet.data.LinkDatabase
import com.cocode.linkqrwallet.data.LinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LinkQrWalletApp : Application() {
    val database: LinkDatabase by lazy {
        LinkDatabase.getInstance(this)
    }

    val repository: LinkRepository by lazy {
        LinkRepository(database.linkItemDao())
    }

    /** Work that must finish after a screen is gone, such as reading a saved link's page title. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
