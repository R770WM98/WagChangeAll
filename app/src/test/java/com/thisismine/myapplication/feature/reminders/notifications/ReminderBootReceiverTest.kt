package com.thisismine.myapplication.feature.reminders.notifications

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderBootReceiverTest {

    private val receiver = ReminderBootReceiver()

    @Test
    fun bootCompleted_requiresUnlockedUser() {
        assertFalse(receiver.shouldReschedule(Intent.ACTION_BOOT_COMPLETED, isUserUnlocked = false))
        assertTrue(receiver.shouldReschedule(Intent.ACTION_BOOT_COMPLETED, isUserUnlocked = true))
    }

    @Test
    fun lockedBootCompleted_isDeferred() {
        assertFalse(receiver.shouldReschedule(Intent.ACTION_LOCKED_BOOT_COMPLETED, isUserUnlocked = false))
        assertFalse(receiver.shouldReschedule(Intent.ACTION_LOCKED_BOOT_COMPLETED, isUserUnlocked = true))
    }

    @Test
    fun userUnlocked_alwaysReschedules() {
        assertTrue(receiver.shouldReschedule(Intent.ACTION_USER_UNLOCKED, isUserUnlocked = false))
        assertTrue(receiver.shouldReschedule(Intent.ACTION_USER_UNLOCKED, isUserUnlocked = true))
    }

    @Test
    fun packageReplaced_requiresUnlockedUser() {
        assertFalse(receiver.shouldReschedule(Intent.ACTION_MY_PACKAGE_REPLACED, isUserUnlocked = false))
        assertTrue(receiver.shouldReschedule(Intent.ACTION_MY_PACKAGE_REPLACED, isUserUnlocked = true))
    }

    @Test
    fun unknownAction_isIgnored() {
        assertFalse(receiver.shouldReschedule("com.example.UNKNOWN", isUserUnlocked = true))
    }
}

