package com.amily.tycoon.checkPaymentAndUpdate;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.BuildConfig;
import com.amily.tycoon.R;
import com.amily.tycoon.home.HomeActivity;
import com.amily.tycoon.login.LoginActivity;
import com.amily.tycoon.utils.GetTimeAgo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CheckPaymentAndUpdateActivity extends AppCompatActivity {

    private static final String TAG = "CheckPaymentAndUpdateAc";

    // firebase
    private DatabaseReference mRootRef;

    // firebase remote config
    private final static String NEWEST_VERSION = "newest_version";
    private FirebaseRemoteConfig mFirebaseConfig;

    // widgets
    private Button mCheckAccountBalance, mPaywithMtn,
            m_3_DaysPlan, m_6_DaysPlan, mMonthlyPlan;
    private EditText mMobileMoneyPin;
    private RelativeLayout mPaymentPlanHeader, mMobileMoneyPinHeader;
    private LinearLayout mNewVersionLayout;
    private TextView mPaymentPlan, mInfoTextview, mInternetConnectionTextView;
    private Button mUpdateBtn, mTryAgainBtn;
    private ProgressBar mProgressBar;

    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_payment_and_update);

        mFirebaseConfig = FirebaseRemoteConfig.getInstance();

        // new version layout
        mNewVersionLayout = findViewById(R.id.new_version_layout);
        mUpdateBtn = findViewById(R.id.update_btn);
        mInfoTextview = findViewById(R.id.info_textview);
        mProgressBar = findViewById(R.id.progressbar);
        mInternetConnectionTextView = findViewById(R.id.check_your_internet_textview);
        mTryAgainBtn = findViewById(R.id.try_again_btn);


        mRootRef = FirebaseDatabase.getInstance().getReference();
        settings = this.getSharedPreferences(getString(R.string.shared_preferences), 0);

        Log.e(TAG, "onCreate: the app version is : " + getAppVersion());



        if(checkInternetConnection()) {
            mInternetConnectionTextView.setVisibility(View.GONE);
            mTryAgainBtn.setVisibility(View.GONE);

            // start checking configuration and time stamp
            startCheckingConfigurationsAndTimeStamp();
        }

        mTryAgainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkInternetConnection()) {
                    mInternetConnectionTextView.setVisibility(View.GONE);
                    mTryAgainBtn.setVisibility(View.GONE);

                    // start checking configuration and time stamp
                    startCheckingConfigurationsAndTimeStamp();
                }
            }
        });
    }


    private boolean checkInternetConnection() {

        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
            else {
                StyleableToast.makeText(this, "Check your internet connection", R.style.customToast).show();
                mInternetConnectionTextView.setVisibility(View.VISIBLE);
                mTryAgainBtn.setVisibility(View.VISIBLE);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private void startCheckingConfigurationsAndTimeStamp() {

        String timestamp = settings.getString(getString(R.string.timestamp_key), "");
        Log.e(TAG, "startCheckingConfigurationsAndTimeStamp: the timestamp retrieved is : " + timestamp);

        /// If its the first time a user install the app, then the timestamp is empty
        if(timestamp.equals("")) {

            // Send the user to the login activity, if the user has logged out
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if(currentUser == null) {
                startActivity(new Intent(CheckPaymentAndUpdateActivity.this, LoginActivity.class));
                finish();
            }

            // if the user is logged in, then check the configurations
            else
                checkOfficialConfiguration();
        }

        // if its not the first time a user has opened the app, then check the configurations
        else {
            Log.e(TAG, "onCreate: check the duration since it was checked");
            final long cutoff = new Date().getTime() - TimeUnit.MILLISECONDS.convert(3, TimeUnit.HOURS);

            try {

                // if the timestamp is stored more than 23 hours, then check the configuration and update the timestamp
                if(Long.parseLong(timestamp) < cutoff) {

                    // check if the user is logged in
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if(currentUser == null) {
                        startActivity(new Intent(CheckPaymentAndUpdateActivity.this, LoginActivity.class));
                        finish();
                    }
                    else
                        checkOfficialConfiguration();
                }

                // if the timestamp is stored less than 23 hours, then navigate to home activity
                else {
                    mNewVersionLayout.setVisibility(View.GONE);
                    startActivity(new Intent(this, HomeActivity.class));
                    this.finish();
                }


            }catch (Exception e) {
                Log.e(TAG, "startCheckingConfigurationsAndTimeStamp: THERE IS NO EXCEPTION : " + e.getMessage());
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(getString(R.string.timestamp_key), ""); // the first element is the key, and the second element is the value to be stored
                editor.apply();

                mNewVersionLayout.setVisibility(View.GONE);
                startActivity(new Intent(this, HomeActivity.class));
                this.finish();
            }
        }

    }

    private void updateTimestamp() {
        Map map = new HashMap();
        String timePath = getString(R.string.dbname_configuration_time) + "/" +
                FirebaseAuth.getInstance().getCurrentUser().getUid() + "/" + getString(R.string.field_time_created);

        map.put(timePath, ServerValue.TIMESTAMP);

        mRootRef.updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if(databaseError == null) {
                    Log.e(TAG, "onComplete: sent successfully");
                    retrieveTimestamp();
                }
                else
                    Log.e(TAG, "onComplete: failed to send the data");

            }
        });

    }

    private void retrieveTimestamp() {
        Query query =
                mRootRef.child(getString(R.string.dbname_configuration_time))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(getString(R.string.field_time_created));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                SharedPreferences.Editor editor = settings.edit();
                editor.putString(getString(R.string.timestamp_key), String.valueOf(dataSnapshot.getValue(Long.class))); // the first element is the key, and the second element is the value to be stored
                editor.apply();
                Log.e(TAG, "onDataChange: the time stamp saved is : " + String.valueOf(dataSnapshot.getValue(Long.class)));

                if(dataSnapshot.exists()) {
                    Log.e(TAG, "onDataChange: the data really exists");
                }
                else {
                    Log.e(TAG, "onDataChange: the data does not really exists");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkOfficialConfiguration() {
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseConfig.setConfigSettings(configSettings);
        fetchConfiguration();
    }

    private void fetchConfiguration() {
        long cacheExpiration = 3600;
        if(mFirebaseConfig.getInfo().getConfigSettings().isDeveloperModeEnabled())
            cacheExpiration = 0;
        mFirebaseConfig.fetch(cacheExpiration).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mFirebaseConfig.activateFetched();
                applyConfigurationChanges();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CheckPaymentAndUpdateActivity.this, "Check your internet connection", Toast.LENGTH_SHORT).show();
                applyConfigurationChanges();
            }
        });
    }
    private void applyConfigurationChanges() {
        double new_msg_length_limit = mFirebaseConfig.getDouble(NEWEST_VERSION);
        checkIfVersionsMatch(new_msg_length_limit);
    }

    private void checkIfVersionsMatch(double version) {
        Log.e(TAG, "checkVersionNumber: the msg length converted is : " + version + " /////");

        if(getAppVersion() != 0) {
            if(version == getAppVersion()) {

                // update timestamp
                updateTimestamp();

                mNewVersionLayout.setVisibility(View.GONE);
                startActivity(new Intent(this, HomeActivity.class));
                this.finish();

            }
            else {

                Log.e(TAG, "checkIfVersionsMatch: the version is : " + version + " ///" );
                Log.e(TAG, "checkIfVersionsMatch: and the app version is : " + getAppVersion() + " ///");
                CharSequence[] options = new CharSequence[] {"A new version of Tinny is out now, get it on google play store", "update now"};
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(true);

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 1) {

                            String url = "https://play.google.com/store/apps/details?id=com.amily.tycoon";
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);
                        }
                    }
                });

                // this prevent the crush if a user open and close the app imediately
                try {
                    builder.show();
                }catch (Exception e) {
                    Log.e(TAG, "checkIfVersionsMatch: THERE IS AN ERROR : " + e.getMessage());
                }

                mInfoTextview.setVisibility(View.VISIBLE);
                mUpdateBtn.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);

                mUpdateBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String url = "https://play.google.com/store/apps/details?id=com.amily.tycoon";
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                });
            }
        }
    }

    private double getAppVersion() {
        String version = null;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            return Double.parseDouble(version);
        }catch (Exception e) {
            Log.e(TAG, "onCreate: there is an EXCEPTION : " + e.getMessage());
        }
        return 0;
    }

}
