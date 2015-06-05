/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */
package org.rm3l.ddwrt.feedback;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.suredigit.inappfeedback.FeedbackDialog;
import com.suredigit.inappfeedback.FeedbackSettings;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

/**
 * Feedback dialog and settings wrapper
 */
public class SendFeedbackDialog {

    /**
     * API Key for this app package
     */
    private static final String ANDROID_FEEDBACK_APIKEY = \"fake-api-key\";

    @NonNull
    private final FeedbackDialog mFeedbackDialog;

    public SendFeedbackDialog(@NonNull final Context context) {

        final Resources resources = context.getResources();
        final FeedbackSettings feedbackSettings = new FeedbackSettings();

        //SUBMIT-CANCEL BUTTONS
        feedbackSettings.setCancelButtonText(resources.getString(R.string.feedback_cancel));
        feedbackSettings.setSendButtonText(resources.getString(R.string.feedback_send));

//        //DIALOG TEXT
        feedbackSettings.setText(resources.getString(R.string.feedback_dialog_text));
        feedbackSettings.setYourComments(resources.getString(R.string.feedback_dialog_comments_text));
        feedbackSettings.setTitle(resources.getString(R.string.send_feedback));
//
//        //TOAST MESSAGE
        feedbackSettings.setToast(resources.getString(R.string.feedback_toast_msg));
        feedbackSettings.setToastDuration(Toast.LENGTH_LONG);
//
//        //RADIO BUTTONS
//        feedbackSettings.setRadioButtons(false); // Disables radio buttons
        feedbackSettings.setBugLabel(resources.getString(R.string.feedback_bug));
        feedbackSettings.setIdeaLabel(resources.getString(R.string.feedback_idea));
        feedbackSettings.setQuestionLabel(resources.getString(R.string.feedback_question));

//        //RADIO BUTTONS ORIENTATION AND GRAVITY
//        feedbackSettings.setOrientation(LinearLayout.HORIZONTAL); // Default
//        feedbackSettings.setOrientation(LinearLayout.VERTICAL);
//        feedbackSettings.setGravity(Gravity.END); // Default
//        feedbackSettings.setGravity(Gravity.START);
//        feedbackSettings.setGravity(Gravity.CENTER);
//
//        //SET DIALOG MODAL
        feedbackSettings.setModal(true); //Default is false
//
//        //DEVELOPER REPLIES
        feedbackSettings.setReplyTitle(resources.getString(R.string.feedback_reply_title));
        feedbackSettings.setReplyCloseButtonText(resources.getString(R.string.feedback_reply_close));
        feedbackSettings.setReplyRateButtonText(resources.getString(R.string.feedback_reply_rate));
//
//        //DEVELOPER CUSTOM MESSAGE (NOT SEEN BY THE END USER)
        feedbackSettings.setDeveloperMessage(DDWRTCompanionConstants.PUBKEY);

        this.mFeedbackDialog = new FeedbackDialog(context, ANDROID_FEEDBACK_APIKEY);
        this.setSettings(feedbackSettings);
    }

    @NonNull
    public FeedbackDialog getFeedbackDialog() {
        return mFeedbackDialog;
    }

    @NonNull
    private SendFeedbackDialog setSettings(@NonNull final FeedbackSettings feedbackSettings) {
        this.mFeedbackDialog.setSettings(feedbackSettings);
        return this;
    }
}
