package com.dnielfe.manager.ui;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dnielfe.manager.R;
import com.dnielfe.manager.utils.SimpleUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class DirectoryNavigationView {

    public final Set<OnNavigateListener> listeners;
    private final Activity mActivity;
    private final int text_size = 16;
    private final int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;
    private final int MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT;

    public interface OnNavigateListener {
        void onNavigate(String path);
    }

    public DirectoryNavigationView(Activity activity) {
        this.mActivity = activity;
        this.listeners = new HashSet<>();
    }

    public void setDirectoryButtons(String path) {
        File currentDirectory = new File(path);
        String dir = "";

        HorizontalScrollView scrolltext = (HorizontalScrollView) mActivity
                .findViewById(R.id.scroll_text);
        LinearLayout mView = (LinearLayout) mActivity.findViewById(R.id.directory_buttons);
        mView.removeAllViews();

        String[] parts = currentDirectory.getAbsolutePath().split("/");

        // Add home view separately
        TextView t0 = new TextView(mActivity, null, android.R.attr.actionButtonStyle);
        t0.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT,
                Gravity.CENTER_VERTICAL));
        t0.setText("/");
        t0.setTextSize(text_size);
        t0.setTag(dir);
        t0.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                for (final OnNavigateListener listener : listeners) {
                    listener.onNavigate("/");
                }
            }
        });

        mView.addView(t0);

        // Add other buttons
        for (int i = 1; i < parts.length; i++) {
            dir += "/" + parts[i];

            // add a LinearLayout as a divider
            FrameLayout fv1 = new FrameLayout(mActivity);
            LinearLayout divider = (LinearLayout) mActivity.getLayoutInflater()
                    .inflate(R.layout.item_navigation_divider, null);
            fv1.addView(divider);
            fv1.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL));

            // add clickable TextView
            TextView t2 = new TextView(mActivity, null, android.R.attr.actionButtonStyle);
            t2.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT,
                    Gravity.CENTER_VERTICAL));
            t2.setText(parts[i]);
            t2.setTextSize(text_size);
            t2.setTag(dir);
            t2.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    String dir1 = (String) view.getTag();
                    for (final OnNavigateListener listener : listeners) {
                        listener.onNavigate(dir1);
                    }
                }
            });

            t2.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View view) {
                    String dir1 = (String) view.getTag();
                    SimpleUtils.savetoClipBoard(mActivity, dir1);
                    return true;
                }
            });

            mView.addView(fv1);
            mView.addView(t2);
            scrolltext.postDelayed(new Runnable() {
                @Override
                public void run() {
                    HorizontalScrollView hv = (HorizontalScrollView) mActivity
                            .findViewById(R.id.scroll_text);
                    hv.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                }
            }, 100L);
        }
    }

    public void addonNavigateListener(final OnNavigateListener listener) {
        this.listeners.add(listener);
    }

    public void removeOnNavigateListener(final OnNavigateListener listener) {
        this.listeners.remove(listener);
    }
}