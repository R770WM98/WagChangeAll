package com.thisismine.myapplication.core.ui

import android.view.View
import android.widget.DatePicker
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thisismine.myapplication.feature.motorcycles.ui.MotorcycleProfileFormFields
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.Assume
import org.junit.runner.RunWith
import org.hamcrest.Matcher

@RunWith(AndroidJUnit4::class)
class DatePickerFieldUiTest {

    // Compose rule must be applied after our compatibility check. Use a RuleChain
    // so the assumption runs before the compose rule initializes Espresso.
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val compatibilityRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                Assume.assumeTrue(
                    "Incompatible platform: android.hardware.input.InputManager.getInstance missing",
                    inputManagerHasGetInstance()
                )
                base.evaluate()
            }
        }
    }

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(compatibilityRule).around(composeRule)

    @Test
    fun datePickerField_picksAndReturnsIsoDate() {
        // compatibility is checked by the ruleChain before this test runs
        var value by mutableStateOf("2025-05-10")

        composeRule.setContent {
            MaterialTheme {
                DatePickerField(
                    value = value,
                    onValueChange = { value = it },
                    label = "Purchase date"
                )
            }
        }

        composeRule.onNodeWithText("2025-05-10").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pick date").performClick()

        onView(isAssignableFrom(DatePicker::class.java)).perform(setDate(2026, 4, 28))
        onView(withText(android.R.string.ok)).perform(click())

        composeRule.runOnIdle {
            assertEquals("2026-04-28", value)
        }
        composeRule.onNodeWithText("2026-04-28").assertIsDisplayed()
    }

    @Test
    fun motorcycleProfileFormFields_prefillsAndUpdatesPurchaseDate() {
        // Same guard as above for other UI tests that rely on Espresso input injection.
        Assume.assumeTrue(
            "Incompatible platform: android.hardware.input.InputManager.getInstance missing",
            inputManagerHasGetInstance()
        )
        var purchaseDate by mutableStateOf("2024-01-10")

        composeRule.setContent {
            MaterialTheme {
                MotorcycleProfileFormFields(
                    name = "Daily Ride",
                    plateNumber = "ABC-1234",
                    vin = "VIN-123",
                    purchaseDateIso = purchaseDate,
                    odometer = "15420",
                    notes = "Main bike",
                    onNameChange = {},
                    onPlateNumberChange = {},
                    onVinChange = {},
                    onPurchaseDateChange = { purchaseDate = it },
                    onOdometerChange = {},
                    onNotesChange = {}
                )
            }
        }

        composeRule.onNodeWithText("2024-01-10").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pick date").performClick()

        onView(isAssignableFrom(DatePicker::class.java)).perform(setDate(2026, 1, 3))
        onView(withText(android.R.string.ok)).perform(click())

        composeRule.runOnIdle {
            assertEquals("2026-01-03", purchaseDate)
        }
        composeRule.onNodeWithText("2026-01-03").assertIsDisplayed()
    }

    private fun setDate(year: Int, month: Int, dayOfMonth: Int): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(DatePicker::class.java)

        override fun getDescription(): String = "Set DatePicker date to $year-$month-$dayOfMonth"

        override fun perform(uiController: UiController, view: View) {
            (view as DatePicker).updateDate(year, month - 1, dayOfMonth)
            uiController.loopMainThreadUntilIdle()
        }
    }

    private fun inputManagerHasGetInstance(): Boolean {
        return try {
            Class.forName("android.hardware.input.InputManager").getDeclaredMethod("getInstance")
            true
        } catch (t: Throwable) {
            false
        }
    }
}

