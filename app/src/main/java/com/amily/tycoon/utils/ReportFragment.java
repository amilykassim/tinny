package com.amily.tycoon.utils;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class ReportFragment extends Fragment {

    private static final String TAG = "ReportFragment";

    private Button mSubmitBtn;
    private ImageView mBack;
    private EditText mReportField;
    private TextView mTitle;
    private ProgressBar mProgressBar;

    public ReportFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_report, container, false);

        mBack = view.findViewById(R.id.backArrow);
        mTitle = view.findViewById(R.id.report_title_toolbar);
        mProgressBar = view.findViewById(R.id.progressbar);
        mSubmitBtn = view.findViewById(R.id.submit_report_fragment);
        mReportField = view.findViewById(R.id.report_problem_edit_text);


        try {
            // if the user has clicked feedback (i.e wants to send a feedback)
            if (getStringExtra().equals(getString(R.string.feedback))) {
                mTitle.setText("Feedback");
                mReportField.setHint("Your feedback....");
            }
        }catch (Exception e) {
            Log.e(TAG, "onCreateView: THERE IS AN EXCEPTION : " + e.getMessage());
        }

        mSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(checkInternetConnection()) {
                    if (!mReportField.getText().toString().equals("")) {

                        mProgressBar.setVisibility(View.VISIBLE);
                        mReportField.setEnabled(false);
                        mSubmitBtn.setEnabled(false);
                        sendTheReportToDatabase();

                    } else {
                        StyleableToast.makeText(getActivity(), "Report field cannot be empty", R.style.customToast).show();

                    }
                }
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        return view;
    }

    private boolean checkInternetConnection() {

        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
            else {
                StyleableToast.makeText(getActivity(), "Check your internet connection", R.style.customToast).show();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void sendTheReportToDatabase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();

        String reportID = ref.push().getKey();
        ref.child(getString(R.string.dbname_reports))
                .child(reportID)
                .child(getString(R.string.field_problem))
                .setValue(mReportField.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            mProgressBar.setVisibility(View.GONE);
                            mReportField.setEnabled(true);
                            mSubmitBtn.setEnabled(true);
                            displayThankingMessage();
                            mReportField.setText("");
                        }
                        else {
                            StyleableToast.makeText(getActivity(), "Failed to submit the report, try again!", R.style.customToast).show();
                            mProgressBar.setVisibility(View.GONE);
                            mReportField.setEnabled(true);
                            mSubmitBtn.setEnabled(true);
                            mReportField.setText("");
                        }
                    }
                });

    }

    private void displayThankingMessage() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true);


        try {
            if (getStringExtra().equals(getString(R.string.feedback))) {
                builder.setMessage("We thank you a lot to contribute and playing a big part " +
                        "to improve our services, your feedback means a lot to us.")
                        .setCancelable(false)
                        .setPositiveButton("Thanks", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                builder.show();
            }
        }catch (Exception e) {
            builder.setMessage("We thank you a lot to contribute and playing a big part " +
                    "to improve our services, we'll analyze the problem and we'll solve it as " +
                    "soon as we can, kind regards.")
                    .setCancelable(false)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            builder.show();
        }
    }


    // Getting the string extra and check if the user want to send a feedback or to report something
    private String getStringExtra() {

        try {
            Bundle bundle = this.getArguments();
            if (bundle != null) {
                return bundle.getString(getString(R.string.feedback));
            }
        }catch (Exception e) {
            Log.e(TAG, "getCallingActivityFromBundle: THERE IS AN EXCEPTION : " + e.getMessage());
        }
        return null;
    }

}
