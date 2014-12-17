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
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.suredigit.inappfeedback.FeedbackDialog;
import com.suredigit.inappfeedback.FeedbackSettings;
import org.jetbrains.annotations.NotNull;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

public class SendFeedbackDialog {

    /**
     * API Key for this app package
     */
    private static final String ANDROID_FEEDBACK_APIKEY = \"fake-api-key\";

    @NotNull
    private final FeedbackDialog mFeedbackDialog;

    public SendFeedbackDialog(@NotNull final Context context) {
//
        final FeedbackSettings feedbackSettings = new FeedbackSettings();

        //SUBMIT-CANCEL BUTTONS
        feedbackSettings.setCancelButtonText("No");
        feedbackSettings.setSendButtonText("Send");

//        //DIALOG TEXT
//        feedbackSettings.setText("Hey, would you like to give us some feedback so that we can improve your experience?");
//        feedbackSettings.setYourComments("Type your question here...");
//        feedbackSettings.setTitle("Feedback Dialog Title");
//
//        //TOAST MESSAGE
//        feedbackSettings.setToast("Thank you so much!");
//        feedbackSettings.setToastDuration(Toast.LENGTH_SHORT);  // Default
        feedbackSettings.setToastDuration(Toast.LENGTH_LONG);
//
//        //RADIO BUTTONS
//        feedbackSettings.setRadioButtons(false); // Disables radio buttons
        feedbackSettings.setBugLabel("Bug");
        feedbackSettings.setIdeaLabel("Idea");
        feedbackSettings.setQuestionLabel("Question");

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
//        feedbackSettings.setReplyTitle("Message from the Developer");
//        feedbackSettings.setReplyCloseButtonText("Close");
//        feedbackSettings.setReplyRateButtonText("RATE!");
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
