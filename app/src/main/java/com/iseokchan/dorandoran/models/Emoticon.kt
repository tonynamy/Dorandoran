package com.iseokchan.dorandoran.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Emoticon(
    var emoticonPackId: String = "",
    var displayName: String = "",
    @get:Exclude var url: String = ""
)