package com.cehpoint.netwin.data.model

import com.google.firebase.firestore.DocumentId
 
data class Wallet(
    @DocumentId
    val userId: String = "",
    val balance: Double = 0.0, // total balance (legacy)
    val withdrawableBalance: Double = 0.0,
    val bonusBalance: Double = 0.0
) 