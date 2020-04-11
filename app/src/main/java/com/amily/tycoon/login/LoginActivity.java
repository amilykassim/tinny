package com.amily.tycoon.login;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.amily.tycoon.home.HomeActivity;
import com.amily.tycoon.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private Context mContext = LoginActivity.this;

    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;

    // widgets
    private Button mLoginBtn, mSignupBtn;
    private EditText mEmail, mPassword;
    private TextView mForgotPassword;
    private ProgressBar mProgressBar;
    private AlertDialog alertDialog;

    private Dialog mDialog;
    private EditText mEnteredEmailDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.e(TAG, "onCreate: Login activity started gracefully" );

        mContext = LoginActivity.this;
        mDialog = new Dialog(this);
        mDialog.setContentView(R.layout.layout_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();

        mLoginBtn = findViewById(R.id.login_btn);
        mSignupBtn = findViewById(R.id.sign_up_btn);
        mEmail = findViewById(R.id.email_login_activity);
        mPassword = findViewById(R.id.password_login_activity);
        mForgotPassword = findViewById(R.id.forgotPassword_tv);
        mProgressBar = findViewById(R.id.progressbar);
        mProgressBar.setVisibility(View.GONE);


        alertDialog = new AlertDialog.Builder(LoginActivity.this).create();

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: attempting to login" );
                if(checkInputs(mEmail.getText().toString(), mPassword.getText().toString())) {
                    // checking internet connection
                    try {
                        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                        if(networkInfo != null && networkInfo.isConnected()) {
                            mProgressBar.setVisibility(View.VISIBLE);
                            mLoginBtn.setEnabled(false);
                            mSignupBtn.setEnabled(false);
                            mEmail.setEnabled(false);
                            mPassword.setEnabled(false);
                            loginTheUser(mEmail.getText().toString().toLowerCase(), mPassword.getText().toString());

                        }
                        else {
                            mProgressBar.setVisibility(View.GONE);
                            Toast.makeText(mContext, "Check your internet connection", Toast.LENGTH_SHORT).show();
                            mProgressBar.setVisibility(View.GONE);
                            mLoginBtn.setEnabled(true);
                            mSignupBtn.setEnabled(true);
                            mEmail.setEnabled(true);
                            mPassword.setEnabled(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else
                    Toast.makeText(mContext, "All the text fields should not be empty", Toast.LENGTH_SHORT).show();

            }
        });

        mSignupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: navigating to sign up activity" );
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        mForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popUpDialog();
            }
        });
    }

    private void popUpDialog() {

        TextView txtclose;
        Button cancelBtn, submitBtn;
        final ProgressBar progressBarDialog;


        txtclose = mDialog.findViewById(R.id.txtclose);
        cancelBtn = mDialog.findViewById(R.id.cancel_btn_dialog);
        mEnteredEmailDialog = mDialog.findViewById(R.id.send_offer_btn_dialog_et);
        submitBtn = mDialog.findViewById(R.id.send_offer_btn_dialog);
        progressBarDialog = mDialog.findViewById(R.id.progressbarDialog);
        progressBarDialog.setVisibility(View.GONE);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });
        txtclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });


        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!mEnteredEmailDialog.getText().toString().equals("")) {

                    progressBarDialog.setVisibility(View.VISIBLE);
                    getEmail(mEnteredEmailDialog.getText().toString(), progressBarDialog);
                }
                else {
                    StyleableToast.makeText(mContext, "Enter your email please", R.style.customToast).show();
                }
            }
        });


        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.show();
    }

    private void getEmail(final String email, final ProgressBar progressBarDialog) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_users))
                .orderByChild(getString(R.string.field_email))
                .equalTo(email.toLowerCase());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                
                if(dataSnapshot.exists()) {

                    try {
                        progressBarDialog.setVisibility(View.GONE);

                        String string = dataSnapshot.getValue().toString();
                        String stringWithPassword = string.substring(string.lastIndexOf("psw=") + 4); // we added 4 to skip 4 characters which are (psw=)
                        int iend = stringWithPassword.indexOf(",");


                        String password = null;
                        if (iend != -1) {
                            password = stringWithPassword.substring(0, iend); //this will give abc
                        }

                        alertDialog.setTitle("Tinny");
                        alertDialog.setMessage("The password hint is : " + password.substring(0, password.length() - 2) + "**");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                        mDialog.dismiss();
                    }catch (Exception e) {
                        Log.e(TAG, "onDataChange: THERE IS AN EXCEPTION : " + e.getMessage());
                    }

                }
                else {
                    alertDialog.setTitle("Tinny");
                    alertDialog.setMessage("Invalid email, check your email and try again.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                    progressBarDialog.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private boolean checkInputs(String email, String password) {
        if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password)  ) {
            Toast.makeText(mContext, "All the text fields should not be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void loginTheUser(String email, String password) {

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {


                    String devicetoken = FirebaseInstanceId.getInstance().getToken();

                    mRootRef.child(getString(R.string.dbname_users))
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child(getString(R.string.field_device_token))
                            .setValue(devicetoken).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful()) {

                                try {
                                    Log.e(TAG, "onComplete: The login the user" );
                                    Intent loginIntent = new Intent(mContext, HomeActivity.class);
                                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(loginIntent);
                                    finish();
                                }
                                catch (Exception e) {
                                    Log.e(TAG, "onComplete: " + e.getMessage() );
                                }
                            }
                            else {
                                Toast.makeText(LoginActivity.this, "There is a problem in regarding log in", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

                }
                else if(task.getException() instanceof FirebaseAuthInvalidUserException){ // If a user is already registered
                    alertDialog.setTitle("Tinny");
                    alertDialog.setMessage("Invalid email, check your email and try again.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();

                }else if(task.getException() instanceof FirebaseAuthInvalidCredentialsException){ // If the password is weak
                    alertDialog.setTitle("Tinny");
                    alertDialog.setMessage("Invalid password, check your password and try again.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
                mProgressBar.setVisibility(View.GONE);
                mLoginBtn.setEnabled(true);
                mSignupBtn.setEnabled(true);
                mEmail.setEnabled(true);
                mPassword.setEnabled(true);
            }
        });
    }
}
