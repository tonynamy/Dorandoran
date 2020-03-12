package com.iseokchan.dorandoran.models

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.*
import kotlin.collections.HashMap

@IgnoreExtraProperties
data class ChatRoom(
    @Exclude var id: String? = null,
    val displayName: String = "",

    val messages: ArrayList<Chat>? = null,

    val users: List<DocumentReference>? = null,

    @ServerTimestamp
    val createdAt: Date? = null

)