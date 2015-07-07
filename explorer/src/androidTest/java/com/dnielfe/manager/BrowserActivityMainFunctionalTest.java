package com.dnielfe.manager;

import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.dnielfe.manager.adapters.BrowserListAdapter;
import com.dnielfe.manager.adapters.BrowserTabsAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerMatchers.isClosed;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@RunWith(AndroidJUnit4.class)
public class BrowserActivityMainFunctionalTest
        extends ActivityInstrumentationTestCase2<BrowserActivity> {
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String NULL_ADAPTER = "NULL_ADAPTER";
    //private Instrumentation mInstrumentation;
    private BrowserActivity mBrowserActivity;

    public BrowserActivityMainFunctionalTest() {
        super(BrowserActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        setActivityInitialTouchMode(false);
        //mInstrumentation = getInstrumentation();
        mBrowserActivity = this.getActivity();
    }

    @After
    public void tearDown() throws Exception {
        mBrowserActivity.finish();
        super.tearDown();
    }

    @Test
    public void testPreconditions() {
        assertThat(mBrowserActivity.hasWindowFocus(), is(true));
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed()));
        onView(withId(R.id.folderinfo))
                .check(matches(isDisplayed()));
        onView(withId(R.id.search))
                .check(matches(isDisplayed()));
        onView(withId(R.id.directory_buttons))
                .check(matches(isDisplayed()))
                .check(matches(withChild(withText("/"))));
        onView(withId(R.id.indicator))
                .check(matches(isDisplayed()));
        onView(withId(R.id.pager))
                .check(matches(isDisplayed()))
                .check(matches(hasDescendant(withId(android.R.id.list))))
                .check(matches(hasDescendant(withId(R.id.fabbutton))));
        // TODO: check that list and fabbutton are actually displayed when fragments are tagged
    }

    @Test
    public void testBrowserViews_DefaultPath_DirButtonsAndBrowserListConsistent() {
        checkViewAndFileSystemConsistency();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDirectoryButtons_NavByButtonClick_DirButtonsAndBrowserListConsistent() {
        onView(allOf(withParent(withId(R.id.directory_buttons)), withText("/")))
                .perform(click());
        assertThat(getDirButtonCount(), equalTo(1));
        onView(allOf(withId(android.R.id.list), isDisplayed()))
                .check(matches(hasDescendant(withText("cache"))))
                .check(matches(hasDescendant(withText("data"))))
                .check(matches(hasDescendant(withText("etc"))));
        checkViewAndFileSystemConsistency();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBrowserList_NavByListItemClick_DirButtonsAndBrowserListConsistent() {
        final int clickPos = 0;
        assertThat(isBrowserListItemADirectory(clickPos), is(true));
        final int initDirButtonCount = getDirButtonCount();
        final String clickPath = getBrowserListItemPath(clickPos);
        onData(is(instanceOf(String.class)))
                .inAdapterView(allOf(withId(android.R.id.list), isDisplayed()))
                .atPosition(clickPos)
                .perform(click());
        assertThat(getDirButtonCount(), equalTo(initDirButtonCount + 1));
        assertThat(getBrowserListPath(), equalTo(clickPath));
        checkViewAndFileSystemConsistency();
    }

    @Test
    public void testBrowserList_NavByBackButton_DirButtonsAndBrowserListConsistent() {
        final int preNavDirButtonCount = getDirButtonCount();
        pressBack();
        assertThat(getDirButtonCount(), equalTo(preNavDirButtonCount - 1));
        checkViewAndFileSystemConsistency();
    }

    // TODO: test folder info
    // TODO: test viewpager nav
    // TODO: test browser list item details
    // TODO: test browser list item context actions
    // TODO: test add file
    // TODO: test add folder

    // Assert Helpers

    private void checkViewAndFileSystemConsistency() {
        checkDirButtonsMatchBrowserListPath();
        checkBrowserListMatchesFileSystem();
    }

    private void checkDirButtonsMatchBrowserListPath() {
        final String[] listPathElements = getBrowserListPathElements();
        final ViewGroup dirButtons =
                (ViewGroup) mBrowserActivity.findViewById(R.id.directory_buttons);
        final int viewCount = dirButtons.getChildCount();
        int curButtonPos = -1;
        String curListPathPart = "";

        for (int i = 0; i < viewCount; i++) {
            final View child = dirButtons.getChildAt(i);
            if (child instanceof TextView) {
                curButtonPos++;
                final String curButtonPath = (String) child.getTag();

                if (!"".equals(listPathElements[curButtonPos])) curListPathPart += "/";
                curListPathPart += listPathElements[curButtonPos];

                assertThat(curButtonPath, equalTo(curListPathPart));
            }
        }
    }

    private void checkBrowserListMatchesFileSystem() {
        final File fsDir = new File(getBrowserListPath());
        final File[] fsList = fsDir.listFiles();

        if (fsList.length == 0) {
            assertThat(getBrowserListCount(), equalTo(0));
        } else {
            for (File fsFile : fsList) {
                final String pathname = fsFile.getPath();
                assertThat(findInBrowserList(pathname), equalTo(pathname));
            }
        }
    }

    // Misc helpers

    private boolean isBrowserListItemADirectory(int listViewIndex) {
        final ListView browserListView = getBrowserListView();
        if (browserListView == null) return false;
        final View blItemView = browserListView.getChildAt(listViewIndex);
        final String blItemBottomViewText =
                ((TextView) blItemView.findViewById(R.id.bottom_view)).getText().toString();
        return blItemBottomViewText.contains(mBrowserActivity.getText(R.string.files));
    }

    private String findInBrowserList(String pathname) {
        final BrowserListAdapter blAdapter = getBrowserListAdapter();
        if (blAdapter == null) return NULL_ADAPTER;
        final int blCount = blAdapter.getCount();

        for (int i = 0; i < blCount; i++) {
            final String blPathname = blAdapter.getItem(i).replaceFirst("//", "/");
            if (blPathname.equals(pathname)) return blPathname;
        }
        return NOT_FOUND;
    }

    private BrowserListAdapter getBrowserListAdapter() {
        final ListView browserListView = getBrowserListView();
        if (browserListView == null) return null;
        return (BrowserListAdapter) getBrowserListView().getAdapter();
    }

    private ListView getBrowserListView() {
        final View browserFragmentView = BrowserTabsAdapter.getCurrentBrowserFragment().getView();
        if (browserFragmentView == null) return null;
        return (ListView) browserFragmentView.findViewById(android.R.id.list);
    }

    private int getBrowserListCount() {
        final BrowserListAdapter blAdapter = getBrowserListAdapter();
        if (blAdapter == null) return 0;
        return blAdapter.getCount();
    }

    private int getDirButtonCount() {
        final ViewGroup dirButtons =
                (ViewGroup) mBrowserActivity.findViewById(R.id.directory_buttons);
        final int viewCount = dirButtons.getChildCount();
        int dirButtonCount = 0;

        for (int i = 0; i < viewCount; i++) {
            final View child = dirButtons.getChildAt(i);
            if (child instanceof TextView) dirButtonCount++;
        }
        return dirButtonCount;
    }

    private String getBrowserListPath() {
        return BrowserTabsAdapter.getCurrentBrowserFragment().mCurrentPath;
    }

    private String[] getBrowserListPathElements() {
        final String browserListPath = getBrowserListPath();
        return "/".equals(browserListPath)
                ? new String[] { "" }
                : getBrowserListPath().split("/");
    }

    private String getBrowserListItemPath(int pos) {
        final BrowserListAdapter blAdapter = getBrowserListAdapter();
        if (blAdapter == null) return null;
        return blAdapter.getItem(pos);
    }
}
