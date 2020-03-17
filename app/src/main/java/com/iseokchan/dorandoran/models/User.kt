package com.iseokchan.dorandoran.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val displayName: String? = "",
    val email: String? = "",
    val profileImage: String? = "",
    @get:Exclude var uid: String? = ""
)