package journal.gratitude.com.gratitudejournal.ui

import android.app.Activity
import android.net.Uri
import android.view.KeyEvent
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import journal.gratitude.com.gratitudejournal.R
import journal.gratitude.com.gratitudejournal.di.DaggerTestApplicationRule
import journal.gratitude.com.gratitudejournal.model.Entry
import journal.gratitude.com.gratitudejournal.repository.EntryRepository
import journal.gratitude.com.gratitudejournal.testUtils.getText
import journal.gratitude.com.gratitudejournal.testUtils.isEditTextValueEqualTo
import journal.gratitude.com.gratitudejournal.testUtils.saveEntryBlocking
import journal.gratitude.com.gratitudejournal.testUtils.waitFor
import journal.gratitude.com.gratitudejournal.ui.entry.EntryFragment
import journal.gratitude.com.gratitudejournal.ui.entry.EntryFragmentArgs
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.LocalDate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RunWith(AndroidJUnit4::class)
class EntryFragmentInstrumentedTest {

    private lateinit var repository: EntryRepository

    @get:Rule
    val rule = DaggerTestApplicationRule()

    @Before
    fun setupDaggerComponent() {
        repository = rule.component.entryRepository
    }

    @Test
    fun todaysEntry_showsTodayDateStrings() {
        val date = LocalDate.now()

        val mockEntry = Entry(date, "test content")
        repository.saveEntryBlocking(mockEntry)

        val bundle = EntryFragmentArgs(
            date.toString(),
            true,
            0)

        launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = bundle.toBundle()
        )

        onView(withId(R.id.date)).check(matches(withText("Today")))
        onView(withId(R.id.thankful_for)).check(matches(withText("I am grateful for")))
    }

    @Test
    fun yesterdaysEntry_showsYesterdayDateStrings() {
        val date = LocalDate.now().minusDays(1)

        val mockEntry = Entry(date, "test content")
        repository.saveEntryBlocking(mockEntry)

        val bundle = EntryFragmentArgs(
            date.toString(),
            true,
            0)

        launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = bundle.toBundle()
        )

        onView(withId(R.id.date)).check(matches(withText("Yesterday")))
        onView(withId(R.id.thankful_for)).check(matches(withText("I was grateful for")))
    }

    @Test
    fun writtenEntry_showsShareButton() {
        val date = LocalDate.of(2019, 3, 22)
        val mockEntry = Entry(date, "test content")
        repository.saveEntryBlocking(mockEntry)

        val bundle = EntryFragmentArgs(
            date.toString(),
            true,
            0)

        launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = bundle.toBundle()
        )

        onView(withId(R.id.share_button))
            .check(matches(isDisplayed()))
        onView(withId(R.id.prompt_button))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun noEntry_showsPromptButton() {
        val date = LocalDate.of(2019, 3, 23)

        val args = EntryFragmentArgs(
            date.toString(),
            true,
            4)

        launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = args.toBundle()
        )

        onView(withId(R.id.share_button)).check(matches(not(isDisplayed())))
        onView(withId(R.id.prompt_button)).check(matches(isDisplayed()))
    }

    @Test
    fun promptButton_changesHintText() {
        val date = LocalDate.of(2019, 3, 23)

        val args = EntryFragmentArgs(
            date.toString(),
            true,
            4)

        launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = args.toBundle()
        )


        onView(withId(R.id.entry_text)).check(matches(withHint("What were you grateful for?")))
        onView(withId(R.id.prompt_button)).perform(click())
        onView(withId(R.id.entry_text)).check(matches(not(withHint("What were you grateful for?"))))
    }

    @Test
    fun saveButton_navigatesBack() {
        val mockNavController = mock<NavController>()
        val date = LocalDate.of(2019, 3, 22)

        val args = EntryFragmentArgs(
            date.toString(),
            true,
            4)
        val scenario = launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = args.toBundle()
        )
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }

        onView(withId(R.id.entry_text)).perform(
            typeText("Test string!"),
            closeSoftKeyboard()
        )

        onView(withId(R.id.save_button)).perform(click())

        verify(mockNavController).navigateUp()
    }

    @Test
    fun saveButton_onMilestone_showsMilestoneDialog() {
        val mockNavController = mock<NavController>()
        val date = LocalDate.of(2019, 3, 22)

        val bundle = EntryFragmentArgs(
            date.toString(),
            true,
            4)

        val scenario = launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = bundle.toBundle()
        )
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }

        onView(withId(R.id.entry_text)).perform(
            typeText("Test string!"),
            closeSoftKeyboard()
        )

        onView(withId(R.id.save_button)).perform(click())

        onView(withText("Share your achievement")).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    fun entryFragment_longPressQuote_copiesToClipboard() {
        val date = LocalDate.of(2019, 3, 23)

        val args = EntryFragmentArgs(
            date.toString(),
            true,
            4)

        launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = args.toBundle()
        )

        val quote =
            getText(withId(R.id.inspiration))

        onView(withId(R.id.inspiration)).perform(longClick())
        onView(withId(R.id.entry_text)).perform(click())
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_MASK)


        onView(withId(R.id.entry_text)).check(matches(
            isEditTextValueEqualTo(
                quote
            )
        ))
    }

    @Test
    fun entryFragment_longPressQuote_showsToast() {
        val date = LocalDate.of(2019, 3, 22)

        val args = EntryFragmentArgs(
            date.toString(),
            true,
            4)

        val scenario = launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = args.toBundle()
        )

        var activity: Activity? = null
        scenario.onFragment { fragment ->
            activity = fragment.activity
        }

        onView(withId(R.id.inspiration)).perform(longClick())

        onView(withText(R.string.copied)).inRoot(withDecorView(not((activity?.window?.decorView))))
            .check(matches(isDisplayed()))
    }

    //these tests are all together to save testing debounce time
    @Test
    fun entryFragment_makeEdit_navigatesBack() {
        val mockNavController = mock<NavController>()
        val date = LocalDate.of(2019, 3, 22)

        val args = EntryFragmentArgs(
            date.toString(),
            true,
            4)

        val scenario = launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = args.toBundle()
        )
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }
        //Simulate user typing
        onView(withId(R.id.entry_text)).perform(
            typeText("Test string!")
        )
        onView(isRoot()).perform(waitFor(550))
        onView(withId(R.id.entry_text)).perform(
            typeText("Yeehaw!"),
            closeSoftKeyboard()
        )

        //wait for debounce to detect changes
        onView(isRoot()).perform(waitFor(550))

        //back is pressed
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mDevice.pressBack()

        //dialog is displayed
        onView(withText(R.string.are_you_sure)).check(matches(isDisplayed()))

        //cancel is pressed
        onView(withId(android.R.id.button2)).perform(click())
        onView(withText(R.string.are_you_sure)).check(ViewAssertions.doesNotExist())

        //back pressed again
        mDevice.pressBack()

        //continue clicked
        onView(withId(android.R.id.button1)).perform(click())

        //back navigate
        verify(mockNavController).navigateUp()
    }

    @Test
    fun entryFragment_noEdit_navigatesBack_noDialog() {
        val mockNavController = mock<NavController>()
        val date = LocalDate.of(2019, 3, 22)

        val bundle = EntryFragmentArgs(
            date.toString(),
            true,
            4)

        val scenario = launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = bundle.toBundle()
        )
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }

        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mDevice.pressBack()

        //back navigate
        verify(mockNavController).navigateUp()

        onView(withText(R.string.are_you_sure)).check(ViewAssertions.doesNotExist())
    }

    @Test
    fun entryFragment_clicksShare_opensShareActivity() {
        val mockNavController = mock<NavController>()
        val date = LocalDate.of(2019, 3, 22)
        val expectedDateString = URLEncoder.encode("March 22, 2019", StandardCharsets.UTF_8.toString())
        val expectedContentString = URLEncoder.encode("Test string!", StandardCharsets.UTF_8.toString())

        val bundle = EntryFragmentArgs(
            date.toString(),
            true,
            0)

        val scenario = launchFragmentInContainer<EntryFragment>(
            themeResId = R.style.Base_AppTheme,
            fragmentArgs = bundle.toBundle()
        )
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }

        onView(withId(R.id.entry_text)).perform(
            typeText("Test string!"),
            closeSoftKeyboard()
        )

        onView(withId(R.id.share_button)).perform(click())

        verify(mockNavController).navigate(Uri.parse("presently://sharing/$expectedDateString/$expectedContentString"))
    }

}
