package com.bdf.saleor.core.data

import com.bdf.saleor.core.datastore.HeldCartLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartLoginPlanTest {
    private val guestLines = listOf(HeldCartLine(variantId = "v-guest", quantity = 2))

    @Test
    fun guestItems_mergeIntoExistingUserCheckout() {
        val plan = planCartLogin(
            guestCheckoutId = "guest-1",
            guestLines = guestLines,
            userCheckouts = listOf(
                UserCheckoutSummary(id = "user-a", quantity = 1),
            ),
        )

        assertEquals("user-a", plan.targetCheckoutId)
        assertFalse(plan.attachGuestCheckout)
        assertEquals(guestLines, plan.linesToMerge)
    }

    @Test
    fun guestItems_attachWhenUserHasNoCheckout() {
        val plan = planCartLogin(
            guestCheckoutId = "guest-1",
            guestLines = guestLines,
            userCheckouts = emptyList(),
        )

        assertEquals("guest-1", plan.targetCheckoutId)
        assertTrue(plan.attachGuestCheckout)
        assertTrue(plan.linesToMerge.isEmpty())
    }

    @Test
    fun emptyGuest_loadsUserCheckout() {
        val plan = planCartLogin(
            guestCheckoutId = null,
            guestLines = emptyList(),
            userCheckouts = listOf(
                UserCheckoutSummary(id = "user-b", quantity = 3),
            ),
        )

        assertEquals("user-b", plan.targetCheckoutId)
        assertFalse(plan.attachGuestCheckout)
        assertTrue(plan.linesToMerge.isEmpty())
    }

    @Test
    fun prefersUserCheckoutWithItems() {
        val plan = planCartLogin(
            guestCheckoutId = "guest-1",
            guestLines = guestLines,
            userCheckouts = listOf(
                UserCheckoutSummary(id = "empty", quantity = 0),
                UserCheckoutSummary(id = "user-a", quantity = 2),
            ),
        )

        assertEquals("user-a", plan.targetCheckoutId)
        assertEquals(guestLines, plan.linesToMerge)
    }

    @Test
    fun alreadyOwnedCheckout_doesNotMergeAgain() {
        val plan = planCartLogin(
            guestCheckoutId = "user-a",
            guestLines = guestLines,
            userCheckouts = listOf(
                UserCheckoutSummary(id = "user-a", quantity = 2),
            ),
        )

        assertEquals("user-a", plan.targetCheckoutId)
        assertFalse(plan.attachGuestCheckout)
        assertTrue(plan.linesToMerge.isEmpty())
    }

    @Test
    fun noGuestAndNoUserCheckout_clearsLocal() {
        val plan = planCartLogin(
            guestCheckoutId = null,
            guestLines = emptyList(),
            userCheckouts = emptyList(),
        )

        assertNull(plan.targetCheckoutId)
        assertFalse(plan.attachGuestCheckout)
        assertTrue(plan.linesToMerge.isEmpty())
    }

    @Test
    fun usersStayIsolated_userBDoesNotReceiveUserACheckout() {
        val plan = planCartLogin(
            guestCheckoutId = null,
            guestLines = emptyList(),
            userCheckouts = listOf(
                UserCheckoutSummary(id = "user-b", quantity = 4),
            ),
        )

        assertEquals("user-b", plan.targetCheckoutId)
        assertTrue(plan.linesToMerge.isEmpty())
    }
}
