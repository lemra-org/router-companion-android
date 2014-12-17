/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.rm3l.ddwrt.feedback;

import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.suredigit.inappfeedback.FeedbackDialog;
import com.suredigit.inappfeedback.FeedbackSettings;
import org.jetbrains.annotations.NotNull;
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

    @NotNull
    private final FeedbackDialog mFeedbackDialog;

    public SendFeedbackDialog(@NotNull final Context context) {

        final Resources resources = context.getResources();
        final FeedbackSettings feedbackSettings = new FeedbackSettings();

        //SUBMIT-CANCEL BUTTONS
        feedbackSettings.setCancelButtonText(resources.getString(R.string.feedback_cancel));
        feedbackSettings.setSendButtonText(resources.getString(R.string.feedback_send));

//        //DIALOG TEXT
        feedbackSettings.setText(resources.getString(R.string.feedback_dialog_text));
        feedbackSettings.setYourComments(resources.getString(R.string.feedback_dialog_comments_text));
        feedbackSettings.setTitle(resources.getString(R.string.feedback));
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

        this.mFeedbackDialog =new FeedbackDialog(context, ANDROID_FEEDBACK_APIKEY);
        this.setSettings(feedbackSettings);
    }

    @NotNull
    public FeedbackDialog getFeedbackDialog() {
        return mFeedbackDialog;
    }

    @NotNull
    private SendFeedbackDialog setSettings(@NotNull final FeedbackSettings feedbackSettings) {
        this.mFeedbackDialog.setSettings(feedbackSettings);
        return this;
    }
}
