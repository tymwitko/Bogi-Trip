package com.tymwitko.bogitrip

import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    private lateinit var mDevice: UiDevice
    private lateinit var navController: TestNavHostController
    private val timeout = 5

    @Before
    fun setup_nav(){
        navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )

        val mapScenario = launchFragmentInContainer<MapFragment>(themeResId =
        R.style.Theme_BogiTrip)

        mapScenario.onFragment { fragment ->

            navController.setGraph(R.navigation.nav_graph)

            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun map_loaded(){
        mDevice.wait(Until.hasObject(By.desc("MAP LOADED")), timeout.toLong())
    }

    @Test
    fun center_map() {

//        onView(withId(R.id.btnLocation))
//            .perform(click())

//        assertEquals()

//        assertEquals(navController.currentDestination?.id, R.id.)
    }

    @Test
    fun orient_map(){
        onView(withId(R.id.btnOrient)).perform(click())
//        assertEquals()
        //TODO: How do I interact with map in tests
    }
}