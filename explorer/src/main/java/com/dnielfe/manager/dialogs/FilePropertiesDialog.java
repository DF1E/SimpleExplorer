package com.dnielfe.manager.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dnielfe.manager.R;
import com.dnielfe.manager.settings.Settings;
import com.dnielfe.manager.ui.PageIndicator;
import com.dnielfe.manager.utils.Permissions;
import com.dnielfe.manager.utils.RootCommands;
import com.dnielfe.manager.utils.SimpleUtils;

import java.io.File;
import java.text.DateFormat;

public final class FilePropertiesDialog extends DialogFragment {

    private Activity activity;
    private static File mFile;
    private PropertiesAdapter mAdapter;

    public static DialogFragment instantiate(File file) {
        mFile = file;
        return new FilePropertiesDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle state) {
        activity = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        mAdapter = new PropertiesAdapter(activity, mFile);
        builder.setTitle(mFile.getName());
        builder.setNeutralButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final FilePermissionsPagerItem fragment = (FilePermissionsPagerItem) mAdapter
                                .getItem(1);
                        fragment.applyPermissions(activity);
                    }
                });
        final View content = activity.getLayoutInflater().inflate(
                R.layout.dialog_properties_container, null);
        this.initView(content);
        builder.setView(content);
        return builder.create();
    }

    private void initView(final View view) {
        final ViewPager pager = (ViewPager) view.findViewById(R.id.tabsContainer);
        pager.setAdapter(mAdapter);

        PageIndicator mIndicator = (PageIndicator) view.findViewById(R.id.tab_indicator);
        mIndicator.setViewPager(pager);
        mIndicator.setFades(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdapter.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.onStop();
    }

    private interface PagerItem {

        void onStart();

        void onStop();

        View onCreateView(LayoutInflater inflater);
    }

    private final class PropertiesAdapter extends PagerAdapter {

        private final LayoutInflater mLayoutInflater;
        private final File mFile;
        private final PagerItem[] mItems;

        private PropertiesAdapter(final Activity context, final File file) {
            mLayoutInflater = context.getLayoutInflater();
            mFile = file;
            mItems = new PagerItem[]{new FilePropertiesPagerItem(mFile),
                    new FilePermissionsPagerItem(mFile)};
        }

        private void onStart() {
            for (final PagerItem item : mItems) {
                item.onStart();
            }
        }

        private void onStop() {
            for (final PagerItem item : mItems) {
                item.onStop();
            }
        }

        private PagerItem getItem(final int position) {
            return mItems[position];
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {
            final PagerItem item = mItems[position];
            final View view = item.onCreateView(mLayoutInflater);
            container.addView(view);
            item.onStart();
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return mItems.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    private static final class FilePropertiesPagerItem implements PagerItem {
        private final File mFile;
        private TextView mPathLabel, mTimeLabel, mSizeLabel, mMD5Label, mSHA1Label;
        private View mView;
        private LoadFsTask mTask;

        private FilePropertiesPagerItem(File file) {
            mFile = file;
        }

        @Override
        public View onCreateView(final LayoutInflater inflater) {
            mView = inflater.inflate(R.layout.dialog_properties, null);
            initView(mView);
            return mView;
        }

        @Override
        public void onStart() {
            if (mView != null) {
                if (mTask == null) {
                    mTask = new LoadFsTask();
                    mTask.execute(mFile);
                }
            }
        }

        @Override
        public void onStop() {
            if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
                mTask.cancel(true);
            }
        }

        private void initView(View table) {
            this.mPathLabel = (TextView) table.findViewById(R.id.path_label);
            this.mTimeLabel = (TextView) table.findViewById(R.id.time_stamp);
            this.mSizeLabel = (TextView) table.findViewById(R.id.total_size);
            this.mMD5Label = (TextView) table.findViewById(R.id.md5_summary);
            this.mSHA1Label = (TextView) table.findViewById(R.id.sha1_summary);
        }

        private final class LoadFsTask extends AsyncTask<File, Void, String[]> {

            @Override
            protected void onPreExecute() {
                mPathLabel.setText(mFile.getAbsolutePath());
                mTimeLabel.setText("...");
                mSizeLabel.setText("...");
                mMD5Label.setText("...");
                mSHA1Label.setText("...");
            }

            @Override
            protected String[] doInBackground(final File... params) {
                DateFormat df = DateFormat.getDateTimeInstance();

                if (!params[0].canRead()) {
                    return null;
                }

                String time = df.format(params[0].lastModified());
                String md5, sha1, size;

                if (params[0].isFile()) {
                    size = SimpleUtils.formatCalculatedSize(params[0].length());

                    md5 = SimpleUtils.getChecksum(params[0], "MD5");
                    sha1 = SimpleUtils.getChecksum(params[0], "SHA1");
                } else {
                    size = SimpleUtils.formatCalculatedSize(SimpleUtils.getDirectorySize(params[0]));
                    md5 = "-";
                    sha1 = "-";
                }

                return new String[]{size, time, md5, sha1};
            }

            @Override
            protected void onPostExecute(final String[] result) {
                if (result != null) {
                    mSizeLabel.setText(result[0]);
                    mTimeLabel.setText(result[1]);
                    mMD5Label.setText(result[2]);
                    mSHA1Label.setText(result[3]);
                } else {
                    mSizeLabel.setText("-");
                    mTimeLabel.setText("-");
                    mMD5Label.setText("-");
                    mSHA1Label.setText("-");
                }
            }
        }
    }

    private final class FilePermissionsPagerItem implements PagerItem,
            CompoundButton.OnCheckedChangeListener {

        // Permissions that file had when FilePermissionsController was created
        private Permissions mInputPermissions = null;

        // Currently modified permissions
        private Permissions mModifiedPermissions = null;

        // User: read, write, execute
        private CompoundButton ur, uw, ux = null;

        // Group: read, write, execute
        private CompoundButton gr, gw, gx = null;

        // Others: read, write, execute
        private CompoundButton or, ow, ox = null;

        private final File mFile;
        private View mView;
        private TextView mGroup, mOwner;
        private Permissions mPermission;

        private FilePermissionsPagerItem(final File file) {
            mFile = file;
        }

        @Override
        public View onCreateView(final LayoutInflater inflater) {
            mView = inflater.inflate(R.layout.dialog_permissions, null);
            initView();
            return mView;
        }

        @Override
        public void onStart() {
        }

        @Override
        public void onStop() {
        }

        private void initView() {
            this.mOwner = (TextView) mView.findViewById(R.id.owner);
            this.mGroup = (TextView) mView.findViewById(R.id.group);
            this.ur = (CompoundButton) mView.findViewById(R.id.uread);
            this.uw = (CompoundButton) mView.findViewById(R.id.uwrite);
            this.ux = (CompoundButton) mView.findViewById(R.id.uexecute);
            this.gr = (CompoundButton) mView.findViewById(R.id.gread);
            this.gw = (CompoundButton) mView.findViewById(R.id.gwrite);
            this.gx = (CompoundButton) mView.findViewById(R.id.gexecute);
            this.or = (CompoundButton) mView.findViewById(R.id.oread);
            this.ow = (CompoundButton) mView.findViewById(R.id.owrite);
            this.ox = (CompoundButton) mView.findViewById(R.id.oexecute);

            getPermissions(mFile);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            this.mModifiedPermissions = new Permissions(this.ur.isChecked(),
                    this.uw.isChecked(), this.ux.isChecked(),
                    this.gr.isChecked(), this.gw.isChecked(),
                    this.gx.isChecked(), this.or.isChecked(),
                    this.ow.isChecked(), this.ox.isChecked());
        }

        public void applyPermissions(final Context context) {
            if (mInputPermissions != null
                    && !mInputPermissions.equals(mModifiedPermissions)) {
                final ApplyTask task = new ApplyTask(context,
                        mModifiedPermissions);
                task.execute(this.mFile);
            }
        }

        private void disableBoxes() {
            this.ur.setEnabled(false);
            this.uw.setEnabled(false);
            this.ux.setEnabled(false);
            this.gr.setEnabled(false);
            this.gw.setEnabled(false);
            this.gx.setEnabled(false);
            this.or.setEnabled(false);
            this.ow.setEnabled(false);
            this.ox.setEnabled(false);
        }

        private class ApplyTask extends AsyncTask<File, Void, Boolean> {
            private final Context context;
            private final Permissions target;

            private ApplyTask(Context context, Permissions target) {
                this.context = context;
                this.target = target;
            }

            @Override
            protected Boolean doInBackground(final File... params) {
                return RootCommands.applyPermissions(params[0], this.target);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result)
                    Toast.makeText(context, context.getString(R.string.permissionschanged),
                            Toast.LENGTH_SHORT).show();
            }
        }

        private void getPermissions(File file) {
            String[] mFileInfo = null;

            if (Settings.rootAccess())
                mFileInfo = RootCommands.getFileProperties(file);

            if (mFileInfo != null) {
                mOwner.setText(mFileInfo[1]);
                mGroup.setText(mFileInfo[2]);
                mPermission = new Permissions(mFileInfo[0]);
            } else {
                disableBoxes();
                return;
            }

            this.ur.setChecked(mPermission.ur);
            this.uw.setChecked(mPermission.uw);
            this.ux.setChecked(mPermission.ux);
            this.gr.setChecked(mPermission.gr);
            this.gw.setChecked(mPermission.gw);
            this.gx.setChecked(mPermission.gx);
            this.or.setChecked(mPermission.or);
            this.ow.setChecked(mPermission.ow);
            this.ox.setChecked(mPermission.ox);

            this.ur.setOnCheckedChangeListener(this);
            this.uw.setOnCheckedChangeListener(this);
            this.ux.setOnCheckedChangeListener(this);
            this.gr.setOnCheckedChangeListener(this);
            this.gw.setOnCheckedChangeListener(this);
            this.gx.setOnCheckedChangeListener(this);
            this.or.setOnCheckedChangeListener(this);
            this.ow.setOnCheckedChangeListener(this);
            this.ox.setOnCheckedChangeListener(this);

            this.mInputPermissions = mPermission;
            this.mModifiedPermissions = mPermission;
        }
    }
}
