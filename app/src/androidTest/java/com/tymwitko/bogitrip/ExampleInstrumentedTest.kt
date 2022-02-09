package com.tymwitko.bogitrip

//import android.app.PendingIntent.getActivity
import android.content.Context
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.internal.ContextUtils.getActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.app.bogitrip", appContext.packageName)
    }
    @Test
    fun overEdgeTest(){
//        onView(withText(R.string.TOAST_EDGE)).inRoot(
//            withDecorView(
//                not(
//                    `is`(
//                        getActivity().getWindow().getDecorView()
//                    )
//                )
//            )
//        ).check(
//            matches(
//                isDisplayed()
//            )
//        )
    }
}