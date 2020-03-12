package com.iseokchan.dorandoran.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Chat (
    val content: String = "",
    val uid: String = "",

    @ServerTimestamp
    val createdAt: Date? = null
)