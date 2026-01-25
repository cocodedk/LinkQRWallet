package com.cocode.linkqrwallet

import android.app.Application
import com.cocode.linkqrwallet.data.LinkDatabase
import com.cocode.linkqrwallet.data.LinkRepository

class LinkQrWalletApp : Application() {
    val database: LinkDatabase by lazy {
        LinkDatabase.getInstance(this)
    }

    val repository: LinkRepository by lazy {
        LinkRepository(database.linkItemDao())
    }
}
