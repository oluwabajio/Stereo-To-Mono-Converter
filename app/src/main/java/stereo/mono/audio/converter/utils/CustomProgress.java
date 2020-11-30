package stereo.mono.audio.converter.utils;


import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import stereo.mono.audio.converter.R;

public class CustomProgress {

    public static CustomProgress customProgress = null;
    private Dialog mDialog;

    public static CustomProgress getInstance() {
        if (customProgress == null) {
            customProgress = new CustomProgress();
        }
        return customProgress;
    }

    public void showProgress(Context context, String message, boolean cancelable) {
        mDialog = new Dialog(context);
        // no tile for the dialog
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.prograss_bar_dialog);
        ProgressBar mProgressBar = (ProgressBar) mDialog.findViewById(R.id.progress_bar);
        //  mProgressBar.getIndeterminateDrawable().setColorFilter(context.getResources()
        // .getColor(R.color.material_blue_gray_500), PorterDuff.Mode.SRC_IN);
        TextView progressText = (TextView) mDialog.findViewById(R.id.progress_text);
        progressText.setText("" + message);
        progressText.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        // you can change or add this line according to your need
        mProgressBar.setIndeterminate(true);
        mDialog.setCancelable(cancelable);
        mDialog.setCanceledOnTouchOutside(cancelable);
        mDialog.show();
    }

    public void hideProgress() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }
}
