package com.dnielfe.manager.application;

import android.app.Application;

import com.dnielfe.manager.R;
import com.dnielfe.manager.settings.Settings;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(
        formUri = "https://mandrillapp.com/api/1.0/messages/send.json",
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text,
        resDialogIcon = R.drawable.holo_light_action_info,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogText = R.string.crash_dialog_text,
        resDialogOkToast = R.string.crash_dialog_ok_toast
)

public class SimpleExplorerApplication extends Application {

    private ReportsCrashes mReportsCrashes;

    @Override
    public void onCreate() {
        super.onCreate();
        // get default preferences at start - we need this for setting the theme
        Settings.updatePreferences(this);

        if (Settings.getErrorReports()) {
            ACRA.init(this);

            mReportsCrashes = this.getClass().getAnnotation(ReportsCrashes.class);
            JsonSender jsonSender = new JsonSender(mReportsCrashes.formUri(), null);
            ACRA.getErrorReporter().setReportSender(jsonSender);
        }
    }
}