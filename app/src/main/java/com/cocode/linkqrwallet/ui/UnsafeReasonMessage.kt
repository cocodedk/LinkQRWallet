package com.cocode.linkqrwallet.ui

import androidx.annotation.StringRes
import com.cocode.linkqrwallet.R
import com.cocode.linkqrwallet.data.UnsafeReason

/** The message shown to the person when a link is refused for [this] reason. */
@StringRes
fun UnsafeReason?.messageRes(): Int = when (this) {
    UnsafeReason.Invalid -> R.string.block_invalid
    UnsafeReason.NoScheme -> R.string.block_no_scheme
    UnsafeReason.UnsafeScheme, UnsafeReason.NotHttp -> R.string.block_not_http
    UnsafeReason.NoHost -> R.string.block_no_host
    UnsafeReason.Local -> R.string.block_local
    UnsafeReason.Onion -> R.string.block_onion
    UnsafeReason.Private -> R.string.block_private
    UnsafeReason.EncodedName -> R.string.block_encoded_name
    null -> R.string.error_url_blocked
}
