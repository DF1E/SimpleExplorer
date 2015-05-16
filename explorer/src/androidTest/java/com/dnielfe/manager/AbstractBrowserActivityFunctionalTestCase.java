package com.dnielfe.manager;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.dnielfe.manager.adapters.BrowserListAdapter;

import org.junit.Test;

import java.io.File;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Base class for functional tests of {@link AbstractBrowserActivity} subclasses. Includes
 * common tests and helper methods.
 *
 * Subclasses must call {@link #setAbsBrowserActivity(AbstractBrowserActivity)} in their @Before
 * method to ensure common tests have an {@code AbstractBrowserActivity} reference.
 */
public abstract class AbstractBrowserActivityFunctionalTestCase {
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String NULL_ADAPTER = "NULL_ADAPTER";
    private AbstractBrowserActivity mAbsBrowserActivity;

    public final void setAbsBrowserActivity(AbstractBrowserActivity absBrowserActivity) {
        mAbsBrowserActivity = absBrowserActivity;
    }

    @Test
    public void testCommonBrowserViews_DefaultPath_DirButtonsAndBrowserListConsistent() {
        checkViewAndFileSystemConsistency();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCommonDirectoryButtons_NavByButtonClick_DirButtonsAndBrowserListConsistent() {
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
    public void testCommonBrowserList_NavByListItemClick_DirButtonsAndBrowserListConsistent() {
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
    public void testCommonBrowserList_NavByBackButton_DirButtonsAndBrowserListConsistent() {
        final int preNavDirButtonCount = getDirButtonCount();
        pressBack();
        assertThat(getDirButtonCount(), equalTo(preNavDirButtonCount - 1));
        checkViewAndFileSystemConsistency();
    }

    // TODO: test browser list item details

    // Assert Helpers

    public void checkViewAndFileSystemConsistency() {
        checkDirButtonsMatchBrowserListPath();
        checkBrowserListMatchesFileSystem();
    }

    public void checkDirButtonsMatchBrowserListPath() {
        final String[] listPathElements = getBrowserListPathElements();
        final ViewGroup dirButtons =
                (ViewGroup) mAbsBrowserActivity.findViewById(R.id.directory_buttons);
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

    public void checkBrowserListMatchesFileSystem() {
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

    public boolean isBrowserListItemADirectory(int listViewIndex) {
        final ListView browserListView = getBrowserListView();
        if (browserListView == null) return false;
        final View blItemView = browserListView.getChildAt(listViewIndex);
        final String blItemBottomViewText =
                ((TextView) blItemView.findViewById(R.id.bottom_view)).getText().toString();
        return blItemBottomViewText.contains(mAbsBrowserActivity.getText(R.string.files));
    }

    public String findInBrowserList(String pathname) {
        final BrowserListAdapter blAdapter = getBrowserListAdapter();
        if (blAdapter == null) return NULL_ADAPTER;
        final int blCount = blAdapter.getCount();

        for (int i = 0; i < blCount; i++) {
            final String blPathname = blAdapter.getItem(i).replaceFirst("//", "/");
            if (blPathname.equals(pathname)) return blPathname;
        }
        return NOT_FOUND;
    }

    public BrowserListAdapter getBrowserListAdapter() {
        final ListView browserListView = getBrowserListView();
        if (browserListView == null) return null;
        return (BrowserListAdapter) getBrowserListView().getAdapter();
    }

    public ListView getBrowserListView() {
        final View browserFragmentView = mAbsBrowserActivity.getCurrentBrowserFragment().getView();
        if (browserFragmentView == null) return null;
        return (ListView) browserFragmentView.findViewById(android.R.id.list);
    }

    public int getBrowserListCount() {
        final BrowserListAdapter blAdapter = getBrowserListAdapter();
        if (blAdapter == null) return 0;
        return blAdapter.getCount();
    }

    public int getDirButtonCount() {
        final ViewGroup dirButtons =
                (ViewGroup) mAbsBrowserActivity.findViewById(R.id.directory_buttons);
        final int viewCount = dirButtons.getChildCount();
        int dirButtonCount = 0;

        for (int i = 0; i < viewCount; i++) {
            final View child = dirButtons.getChildAt(i);
            if (child instanceof TextView) dirButtonCount++;
        }
        return dirButtonCount;
    }

    public String getBrowserListPath() {
        return mAbsBrowserActivity.getCurrentBrowserFragment().mCurrentPath;
    }

    public String[] getBrowserListPathElements() {
        final String browserListPath = getBrowserListPath();
        return "/".equals(browserListPath)
                ? new String[] { "" }
                : getBrowserListPath().split("/");
    }

    public String getBrowserListItemPath(int pos) {
        final BrowserListAdapter blAdapter = getBrowserListAdapter();
        if (blAdapter == null) return null;
        return blAdapter.getItem(pos);
    }
}
