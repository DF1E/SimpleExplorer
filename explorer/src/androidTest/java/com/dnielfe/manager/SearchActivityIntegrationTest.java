package com.dnielfe.manager;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class SearchActivityIntegrationTest
        extends ActivityInstrumentationTestCase2<BrowserActivity> {
    private Instrumentation mInstrumentation;
    private BrowserActivity mBrowserActivity;
    private SearchActivity mSearchActivity;

    public SearchActivityIntegrationTest() {
        super(BrowserActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        setActivityInitialTouchMode(false);
        mInstrumentation = getInstrumentation();
        mBrowserActivity = this.getActivity();
        mSearchActivity = getSearchActivityByMenuClick();
    }

    @After
    public void tearDown() throws Exception {
        mSearchActivity.finish();
        mBrowserActivity.finish();
        super.tearDown();
    }

    @Test
    public void testPreconditions() {
        assertTrue(mSearchActivity.hasWindowFocus());
        assertFalse(mBrowserActivity.hasWindowFocus());
    }

    @Test
    public void testNavBack_ByBackButton_DisplaysBrowserActivity() {
        this.sendKeys(KeyEvent.KEYCODE_BACK);
        mInstrumentation.waitForIdleSync();
        assertFalse(mSearchActivity.hasWindowFocus());
        assertTrue(mBrowserActivity.hasWindowFocus());
    }

    private SearchActivity getSearchActivityByMenuClick() {
        Instrumentation.ActivityMonitor activityMonitor =
                mInstrumentation.addMonitor(SearchActivity.class.getName(), null, false);
        onView(withId(R.id.search))
                .perform(click());
        SearchActivity searchActivity =
                (SearchActivity) activityMonitor.waitForActivityWithTimeout(2000);
        mInstrumentation.removeMonitor(activityMonitor);
        mInstrumentation.waitForIdleSync();
        return searchActivity;
    }
}
