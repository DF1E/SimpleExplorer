package com.dnielfe.manager;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;

public class SearchActivityIntegrationTest
        extends ActivityInstrumentationTestCase2<BrowserActivity> {
    private Instrumentation mInstrumentation;
    private BrowserActivity mBrowserActivity;
    private SearchActivity mSearchActivity;

    public SearchActivityIntegrationTest() {
        super(BrowserActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        mInstrumentation = getInstrumentation();
        mBrowserActivity = getActivity();
        mSearchActivity = getSearchActivityByMenuClick();
    }

    @Override
    protected void tearDown() throws Exception {
        mSearchActivity.finish();
        mBrowserActivity.finish();
    }

    public void testPreconditions() {
        assertTrue(mSearchActivity.hasWindowFocus());
        assertFalse(mBrowserActivity.hasWindowFocus());
    }

    public void testNavBack_ByBackButton_DisplaysBrowserActivity() throws Exception {
        this.sendKeys(KeyEvent.KEYCODE_BACK);
        mInstrumentation.waitForIdleSync();
        assertFalse(mSearchActivity.hasWindowFocus());
        assertTrue(mBrowserActivity.hasWindowFocus());
    }

    private SearchActivity getSearchActivityByMenuClick() {
        Instrumentation.ActivityMonitor activityMonitor =
                mInstrumentation.addMonitor(SearchActivity.class.getName(), null, false);
        mBrowserActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View searchMenuItem = mBrowserActivity.findViewById(R.id.search);
                assertNotNull(searchMenuItem);
                searchMenuItem.performClick();
            }
        });
        SearchActivity searchActivity =
                (SearchActivity) activityMonitor.waitForActivityWithTimeout(2000);
        mInstrumentation.removeMonitor(activityMonitor);
        mInstrumentation.waitForIdleSync();
        return searchActivity;
    }
}
