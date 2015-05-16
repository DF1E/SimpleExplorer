package com.dnielfe.manager;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerMatchers.isClosed;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class PickerActivityFunctionalTest extends AbstractBrowserActivityFunctionalTestCase {
    private static final String MIME_TYPE_FILE = "file/*";
    private PickerActivity mPickerActivity;

    @Rule
    public ActivityTestRule<PickerActivity> mActivityRule =
            new ActivityTestRule<>(PickerActivity.class, false, false);

    @Before
    public void setUp() throws Exception {
        initActivity();
        setAbsBrowserActivity(mPickerActivity);
    }

    @After
    public void tearDown() throws Exception {
        mPickerActivity.finish();
    }

    @Test
    public void testPreconditions() {
        assertNotNull(mPickerActivity);
        assertThat(mPickerActivity.hasWindowFocus(), is(true));
    }

    @Test
    public void testUiState_WithDefaultScreen_CorrectElementsDisplayed() {
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed()))
                .check(matches(hasDescendant(withId(R.id.browser_fragment_container))));
        onView(withId(R.id.toolbar))
                .check(matches(hasDescendant(withText(R.string.picker_choose_one_file))));
        onView(withId(R.id.pick_cancel))
                .check(matches(isDisplayed()));
        onView(withId(R.id.folderinfo))
                .check(doesNotExist());
        onView(withId(R.id.search))
                .check(doesNotExist());
        onView(withId(R.id.directory_buttons))
                .check(matches(isDisplayed()))
                .check(matches(withChild(withText("/"))));
        onView(withId(R.id.browser_fragment_container))
                .check(matches(isDisplayed()))
                .check(matches(hasDescendant(withId(android.R.id.list))));
        onView(withId(android.R.id.list))
                .check(matches(isDisplayed()));
        onView(withId(R.id.pager))
                .check(doesNotExist());
        onView(withId(R.id.fabbutton))
                .check(doesNotExist());
    }

    @Test
    public void testClickCancel_WithDefaultScreen_ClosesActivity() {
        onView(withId(R.id.pick_cancel))
                .perform(click());
        assertThat(mPickerActivity.isFinishing(), is(true));
    }

    // TODO: test tapping a list item

    // Helpers

    private void initActivity() {
        Intent startIntent = new Intent(Intent.ACTION_GET_CONTENT);
        startIntent.setType(MIME_TYPE_FILE);
        // TODO: create another test class for EXTRA_ALLOW_MULTIPLE?
        mPickerActivity = mActivityRule.launchActivity(startIntent);
    }
}
