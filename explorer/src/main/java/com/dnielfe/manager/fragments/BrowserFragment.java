package com.dnielfe.manager.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

import com.dnielfe.manager.AbstractBrowserActivity;
import com.dnielfe.manager.R;
import com.dnielfe.manager.SearchActivity;
import com.dnielfe.manager.adapters.BrowserListAdapter;
import com.dnielfe.manager.adapters.BrowserTabsAdapter;
import com.dnielfe.manager.controller.ActionModeController;
import com.dnielfe.manager.dialogs.CreateFileDialog;
import com.dnielfe.manager.dialogs.CreateFolderDialog;
import com.dnielfe.manager.dialogs.DirectoryInfoDialog;
import com.dnielfe.manager.dialogs.UnpackDialog;
import com.dnielfe.manager.fileobserver.FileObserverCache;
import com.dnielfe.manager.fileobserver.MultiFileObserver;
import com.dnielfe.manager.fileobserver.MultiFileObserver.OnEventListener;
import com.dnielfe.manager.settings.Settings;
import com.dnielfe.manager.tasks.PasteTaskExecutor;
import com.dnielfe.manager.utils.ClipBoard;
import com.dnielfe.manager.utils.SimpleUtils;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;

public final class BrowserFragment extends UserVisibleHintFragment implements
        OnEventListener, OnMenuItemClickListener {

    public static final String KEY_IS_GET_CONTENT = "is_get_content";
    public static final String TAG_PRIMARY_BROWSER_LIST_FRAGMENT = "primary_browser_list_fragment";
    private Activity mActivity;
    private FragmentManager fm;

    private MultiFileObserver mObserver;
    private FileObserverCache mObserverCache;
    private Runnable mLastRunnable;
    private static Handler sHandler;

    private onUpdatePathListener mUpdatePathListener;
    private ActionModeController mActionController;
    private BrowserListAdapter mListAdapter;
    public String mCurrentPath;
    private AbsListView mListView;

    private boolean mUseBackKey = true;
    private boolean mIsGetContent;

    public interface onUpdatePathListener {
        void onUpdatePath(String path);
    }

    @Override
    public void onCreate(Bundle state) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        // TODO: GET_CONTENT: get boolean
        final Bundle args = getArguments();
        if (args != null) mIsGetContent = args.getBoolean(KEY_IS_GET_CONTENT);
        super.onCreate(state);
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        mObserverCache = FileObserverCache.getInstance();
        mActionController = new ActionModeController(activity);

        if (sHandler == null) {
            sHandler = new Handler(activity.getMainLooper());
        }

        try {
            mUpdatePathListener = (onUpdatePathListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement mUpdatePathListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);
        mActivity = getActivity();
        Intent intent = mActivity.getIntent();
        fm = getFragmentManager();
        mActionController.setListView(mListView);

        initDirectory(state, intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browser, container, false);

        initList(inflater, rootView);
        return rootView;
    }

    @Override
    protected void onVisible() {
        final AbstractBrowserActivity activity = (AbstractBrowserActivity) getActivity();
        // check for root
        Settings.rootAccess();

        navigateTo(mCurrentPath);

        // this is only needed if you select "move/copy files" in SearchActivity and come back
        if (!ClipBoard.isEmpty())
            activity.supportInvalidateOptionsMenu();
    }

    @Override
    protected void onInvisible() {
        mObserver.stopWatching();

        if (mActionController != null) {
            mActionController.finishActionMode();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("location", mCurrentPath);
    }

    private void initList(LayoutInflater inflater, View rootView) {
        final AbstractBrowserActivity context = (AbstractBrowserActivity) getActivity();
        mListAdapter = new BrowserListAdapter(context, inflater);

        mListView = (ListView) rootView.findViewById(android.R.id.list);
        mListView.setEmptyView(rootView.findViewById(android.R.id.empty));
        mListView.setAdapter(mListAdapter);
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                final File file = new File((mListView.getAdapter()
                        .getItem(position)).toString());

                if (file.isDirectory()) {
                    navigateTo(file.getAbsolutePath());

                    // go to the top of the ListView
                    mListView.setSelection(0);
                } else {
                    listItemAction(file);
                }
            }
        });

        // TODO: GET_CONTENT: remove FAB
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fabbutton);
        if (mIsGetContent) {
            fab.setVisibility(View.GONE);
        } else {
            fab.attachToListView(mListView);
            fab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMenu(view);
                }
            });
        }
    }

    private void showMenu(View v) {
        PopupMenu popup = new PopupMenu(mActivity, v);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.fab_menu);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.createfile:
                final DialogFragment dialog1 = new CreateFileDialog();
                dialog1.show(fm, AbstractBrowserActivity.TAG_DIALOG);
                return true;
            case R.id.createfolder:
                final DialogFragment dialog2 = new CreateFolderDialog();
                dialog2.show(fm, AbstractBrowserActivity.TAG_DIALOG);
                return true;
            default:
                return false;
        }
    }

    public void navigateTo(String path) {
        mCurrentPath = path;

        if (!mUseBackKey)
            mUseBackKey = true;

        if (mObserver != null) {
            mObserver.stopWatching();
            mObserver.removeOnEventListener(this);
        }

        mListAdapter.addFiles(path);

        mObserver = mObserverCache.getOrCreate(path);

        // add listener for FileObserver and start watching
        if (mObserver.listeners.isEmpty())
            mObserver.addOnEventListener(this);
        mObserver.startWatching();

        mUpdatePathListener.onUpdatePath(path);
    }

    private void listItemAction(File file) {
        // TODO: GET_CONTENT: return the Uri
        if (mIsGetContent) {
            final Uri pickedUri = Uri.fromFile(file);
            final Intent result = new Intent();
            result.setData(pickedUri);
            final Activity activity = getActivity();
            activity.setResult(Activity.RESULT_OK, result);
            activity.finish();
        } else if (SimpleUtils.isSupportedArchive(file)) {
            final DialogFragment dialog = UnpackDialog.instantiate(file);
            dialog.show(fm, AbstractBrowserActivity.TAG_DIALOG);
        } else {
            SimpleUtils.openFile(mActivity, file);
        }
    }

    @Override
    public void onEvent(int event, String path) {
        // this will automatically update the directory when an action like this
        // will be performed
        switch (event & FileObserver.ALL_EVENTS) {
            case FileObserver.CREATE:
            case FileObserver.CLOSE_WRITE:
            case FileObserver.MOVE_SELF:
            case FileObserver.MOVED_TO:
            case FileObserver.MOVED_FROM:
            case FileObserver.ATTRIB:
            case FileObserver.DELETE:
            case FileObserver.DELETE_SELF:
                sHandler.removeCallbacks(mLastRunnable);
                sHandler.post(mLastRunnable = new NavigateRunnable(path));
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO: GET_CONTENT: change menu
        if (mIsGetContent) {
            inflater.inflate(R.menu.picker_menu, menu);
        } else {
            inflater.inflate(R.menu.main, menu);

            if (AbstractBrowserActivity.isDrawerOpen()) {
                menu.findItem(R.id.paste).setVisible(false);
                menu.findItem(R.id.folderinfo).setVisible(false);
                menu.findItem(R.id.search).setVisible(false);
            } else {
                menu.findItem(R.id.paste).setVisible(!ClipBoard.isEmpty());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.folderinfo:
                final DialogFragment dirInfo = new DirectoryInfoDialog();
                dirInfo.show(fm, AbstractBrowserActivity.TAG_DIALOG);
                return true;
            case R.id.search:
                Intent sintent = new Intent(mActivity, SearchActivity.class);
                startActivity(sintent);
                return true;
            case R.id.paste:
                final PasteTaskExecutor ptc = new PasteTaskExecutor(mActivity,
                        mCurrentPath);
                ptc.start();
                return true;
            case R.id.pick_cancel:
                // TODO: GET_CONTENT: close activity, return nothing
                getActivity().finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onNavigate(String path) {
        // navigate to path when Navigation button is clicked
        navigateTo(path);
        // go to the top of the ListView
        mListView.setSelection(0);
    }

    private void initDirectory(Bundle savedInstanceState, Intent intent) {
        String defaultdir;

        if (savedInstanceState != null) {
            // get directory when you rotate your phone
            defaultdir = savedInstanceState.getString("location");
        } else {
            try {
                File dir = new File(intent.getStringExtra(AbstractBrowserActivity.EXTRA_SHORTCUT));

                if (dir.exists() && dir.isDirectory()) {
                    defaultdir = dir.getAbsolutePath();
                } else {
                    if (dir.exists() && dir.isFile())
                        listItemAction(dir);
                    // you need to call it when shortcut-dir not exists
                    defaultdir = Settings.getDefaultDir();
                }
            } catch (Exception e) {
                defaultdir = Settings.getDefaultDir();
            }
        }

        File dir = new File(defaultdir);

        if (dir.exists() && dir.isDirectory())
            navigateTo(dir.getAbsolutePath());
    }

    private static final class NavigateRunnable implements Runnable {
        private final String target;

        NavigateRunnable(final String path) {
            this.target = path;
        }

        @Override
        public void run() {
            // TODO: Need different methods of getting current BrowserFragment
            BrowserTabsAdapter.getCurrentBrowserFragment().navigateTo(target);
        }
    }

    public void onBookmarkClick(File file) {
        if (!file.exists()) {
            Toast.makeText(mActivity, getString(R.string.cantopenfile),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (file.isDirectory()) {
            navigateTo(file.getAbsolutePath());

            // go to the top of the ListView
            mListView.setSelection(0);
        } else {
            listItemAction(file);
        }
    }

    public boolean onBackPressed() {
        if (mUseBackKey && mActionController.isActionMode()) {
            mActionController.finishActionMode();
            return true;
        } else if (mUseBackKey && !mCurrentPath.equals("/")) {
            File file = new File(mCurrentPath);
            navigateTo(file.getParent());

            // get position of the previous folder in ListView
            mListView.setSelection(mListAdapter.getPosition(file.getPath()));
            return true;
        } else if (mUseBackKey && mCurrentPath.equals("/")) {
            Toast.makeText(mActivity, getString(R.string.pressbackagaintoquit),
                    Toast.LENGTH_SHORT).show();

            mUseBackKey = false;
            return false;
        } else if (!mUseBackKey && mCurrentPath.equals("/")) {
            mActivity.finish();
            return false;
        }

        return true;
    }
}
