package com.iseokchan.dorandoran.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class EmoticonPack(
    @get:Exclude var id: String? = null,
    val displayName: String = "",
    val author: String = "",
    @get:Exclude var emoticons: ArrayList<Emoticon> = ArrayList()
)