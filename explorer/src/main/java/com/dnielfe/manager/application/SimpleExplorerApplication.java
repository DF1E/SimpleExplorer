package com.dnielfe.manager.application;

import android.app.Application;

import com.dnielfe.manager.R;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(
        formUri = "http://mandrillapp.com/api/1.0/messages/send.json",
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text,
        resDialogText = R.string.crash_dialog_text,
        // resDialogIcon = R.drawable.ic_action_report_problem,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogOkToast = R.string.crash_dialog_ok_toast
)

public class SimpleExplorerApplication extends Application {

    private ReportsCrashes mReportsCrashes;

    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);

        mReportsCrashes = this.getClass().getAnnotation(ReportsCrashes.class);
        JsonSender jsonSender = new JsonSender(mReportsCrashes.formUri(), null);
        ACRA.getErrorReporter().setReportSender(jsonSender);
    }
}