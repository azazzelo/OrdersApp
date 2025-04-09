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
class AppE2ETest2 {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun loginAsDirectorAndAddProduct_CheckNameInList() { // Уточнил имя теста
        Log.d("AppE2ETest_Product", "Step 1: Logging in...")
        onView(withId(R.id.edTextLogin)).perform(typeText("schwein"), closeSoftKeyboard())
        onView(withId(R.id.edTextPassword)).perform(typeText("112233"), closeSoftKeyboard())
        onView(withId(R.id.btLogin)).perform(click())
        Log.d("AppE2ETest_Product", "Step 1: Login successful (assumed).")

        Log.d("AppE2ETest_Product", "Step 2: Navigating to Products list...")
        onView(withId(R.id.btnTableProducts)).perform(click())
        Log.d("AppE2ETest_Product", "Step 2: Products button clicked.")

        Log.d("AppE2ETest_Product", "Step 3: Clicking Add Product button...")
        onView(withId(R.id.btnAddProductt)).perform(click())
        Log.d("AppE2ETest_Product", "Step 3: Add Product button clicked.")

        val productName = "Simple Test Product ${System.currentTimeMillis()}" // Уникальное имя
        val productPrice = "9.99"
        val productQuantity = "15"
        Log.d("AppE2ETest_Product", "Step 4: Filling product data (Name: $productName)...")

        onView(withId(R.id.edTextProductName)).perform(typeText(productName), closeSoftKeyboard())
        onView(withId(R.id.edTextProductCost)).perform(typeText(productPrice), closeSoftKeyboard())
        onView(withId(R.id.edTextProductQuantity)).perform(typeText(productQuantity), closeSoftKeyboard())

        Log.d("AppE2ETest_Product", "Step 4: Skipping photo selection.")

        Log.d("AppE2ETest_Product", "Step 5: Clicking Save Product button...")
        onView(withId(R.id.floatingActionButton)).perform(click())
        Log.d("AppE2ETest_Product", "Step 5: Save Product button clicked.")


        Log.d("AppE2ETest_Product", "Step 6: Verifying product name in the list...")

        val recyclerViewMatcher = withId(R.id.rcProducts)


        Log.d("AppE2ETest_Product", "Scrolling RecyclerView to find product: $productName")
        onView(recyclerViewMatcher)
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(

                hasDescendant(withText(productName))
            ))
        Log.d("AppE2ETest_Product", "Scroll finished (or item already visible).")

        onView(withText(productName))
            .check(matches(isDisplayed()))
        Log.d("AppE2ETest_Product", "CHECK PASSED: Found View with product name: $productName")

        Log.d("AppE2ETest_Product", "Test finished successfully.")
    }
}