package com.bdf.saleor.core.data

import com.bdf.saleor.core.datastore.HeldCartLine

data class UserCheckoutSummary(
    val id: String,
    val quantity: Int,
)

data class CartLoginPlan(
    val targetCheckoutId: String?,
    val attachGuestCheckout: Boolean,
    val linesToMerge: List<HeldCartLine>,
)

fun planCartLogin(
    guestCheckoutId: String?,
    guestLines: List<HeldCartLine>,
    userCheckouts: List<UserCheckoutSummary>,
): CartLoginPlan {
    val userIds = userCheckouts.map { it.id }.toSet()
    val preferredUserCheckoutId = userCheckouts.firstOrNull { it.quantity > 0 }?.id
        ?: userCheckouts.firstOrNull()?.id
    val guestHasItems = guestLines.isNotEmpty()
    val guestIsUsers = guestCheckoutId != null && guestCheckoutId in userIds

    return when {
        guestIsUsers -> CartLoginPlan(
            targetCheckoutId = guestCheckoutId,
            attachGuestCheckout = false,
            linesToMerge = emptyList(),
        )
        preferredUserCheckoutId != null -> CartLoginPlan(
            targetCheckoutId = preferredUserCheckoutId,
            attachGuestCheckout = false,
            linesToMerge = if (guestHasItems) guestLines else emptyList(),
        )
        guestCheckoutId != null && guestHasItems -> CartLoginPlan(
            targetCheckoutId = guestCheckoutId,
            attachGuestCheckout = true,
            linesToMerge = emptyList(),
        )
        else -> CartLoginPlan(
            targetCheckoutId = null,
            attachGuestCheckout = false,
            linesToMerge = emptyList(),
        )
    }
}
