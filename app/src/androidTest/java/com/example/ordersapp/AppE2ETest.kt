package com.example.ordersapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log


@RunWith(AndroidJUnit4::class)
class AppE2ETest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun loginAsDirectorAndAddNewUser_CheckLoginInList() {

        Log.d("AppE2ETest", "Step 1: Logging in...")
        onView(withId(R.id.edTextLogin)).perform(typeText("schwein"), closeSoftKeyboard())
        onView(withId(R.id.edTextPassword)).perform(typeText("112233"), closeSoftKeyboard())
        onView(withId(R.id.btLogin)).perform(click())
        Log.d("AppE2ETest", "Step 1: Login successful (assumed).")

        Log.d("AppE2ETest", "Step 2: Navigating to Users list...")
        onView(withId(R.id.btnTableUsers)).perform(click())
        Log.d("AppE2ETest", "Step 2: Users button clicked.")

        Log.d("AppE2ETest", "Step 3: Clicking Add User button...")
        onView(withId(R.id.btnAddUser)).perform(click())
        Log.d("AppE2ETest", "Step 3: Add User button clicked.")

        val userLogin = "testuser_${System.currentTimeMillis()}"
        val userPassword = "password789"
        Log.d("AppE2ETest", "Step 4: Filling user data (Login: $userLogin)...")

        onView(withId(R.id.edTextUserLogin)).perform(typeText(userLogin), closeSoftKeyboard())
        onView(withId(R.id.edTextUserPassword)).perform(typeText(userPassword), closeSoftKeyboard())

        Log.d("AppE2ETest", "Step 4: Skipping role selection.")

        Log.d("AppE2ETest", "Step 5: Clicking Save User button...")
        onView(withId(R.id.btnSaveUser)).perform(click())
        Log.d("AppE2ETest", "Step 5: Save User button clicked.")

        Log.d("AppE2ETest", "Step 6: Verifying user in the list...")
        val recyclerViewMatcher = withId(R.id.rcUsers)

        Log.d("AppE2ETest", "Scrolling RecyclerView to find login: $userLogin")
        onView(recyclerViewMatcher)
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                hasDescendant(withText(userLogin))
            ))
        Log.d("AppE2ETest", "Scroll finished (or item already visible).")

        onView(withText(userLogin))
            .check(matches(isDisplayed()))
        Log.d("AppE2ETest", "CHECK PASSED: Found View with text: $userLogin")

        Log.d("AppE2ETest", "Test finished successfully.")
    }
}