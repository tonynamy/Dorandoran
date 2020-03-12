package com.iseokchan.dorandoran.models

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class ChatRoom(
    val displayName: String = "",
    val messages: List<Chat>? = null,
    val users: List<DocumentReference>? = null,
    @ServerTimestamp
    val createdAt: Date? = null

)