package com.iseokchan.dorandoran.models

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@IgnoreExtraProperties
data class ChatRoom(
    @get:Exclude var id: String? = null,
    val displayName: String = "",

    val messages: ArrayList<Chat>? = null,

    val users: List<DocumentReference>? = null,

    var seen: MutableMap<String, Int>? = null,

    @get:Exclude var userModels: ArrayList<User>? = ArrayList(),

    @ServerTimestamp
    val createdAt: Date? = null

)