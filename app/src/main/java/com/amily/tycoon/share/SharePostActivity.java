package com.amily.tycoon.share;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.amily.tycoon.R;
import com.amily.tycoon.home.HomeActivity;
import com.amily.tycoon.utils.FirebaseMethods;
import com.amily.tycoon.utils.StringManipulation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.muddzdev.styleabletoastlibrary.StyleableToast;
import com.rockerhieu.emojicon.EmojiconEditText;
import com.rockerhieu.emojicon.EmojiconGridFragment;
import com.rockerhieu.emojicon.EmojiconTextView;
import com.rockerhieu.emojicon.EmojiconsFragment;
import com.rockerhieu.emojicon.emoji.Emojicon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SharePostActivity extends AppCompatActivity implements EmojiconGridFragment.OnEmojiconClickedListener,
        EmojiconsFragment.OnEmojiconBackspaceClickedListener{


    private static final String TAG = "SharePostActivity";
    private Context mContext;

    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private FirebaseMethods mFirebaseMethods;

    // widgets
    private EditText mEditTextEmojicon;
    private ImageView mSmileIcon, mKeyboardIcon, mBackArrow;
    private TextView mShare;
    private ProgressBar mProgressBar;
    private EditText mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_post);

        instantiateFirebase();

        mShare = findViewById(R.id.share);
        mTitle = findViewById(R.id.title_of_description);
        mProgressBar = findViewById(R.id.progressbar);
        mEditTextEmojicon = findViewById(R.id.edit_txt_emojicon);
        mSmileIcon = findViewById(R.id.smile_icon);
        mKeyboardIcon = findViewById(R.id.keyboard_icon);
        mBackArrow = findViewById(R.id.backArrow);
        mKeyboardIcon.setVisibility(View.GONE);

        setEmojiconFragment(false);

        // limiting the title to 20 characters only
        Long new_msg_length_limit = Long.parseLong("20");
        mTitle.setFilters(new InputFilter[]{new InputFilter.LengthFilter(new_msg_length_limit.intValue())});

        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(TAG, "onClick: share button was clicked");
                if(!mEditTextEmojicon.getText().toString().equals("")) {

                    if(!mTitle.getText().toString().equals("")) {
                        if(checkInternetConnection())
                            sendPost();
                    }
                    else
                        StyleableToast.makeText(mContext, "Add a topic or a title", R.style.customToast).show();
                }
                else {
                    StyleableToast.makeText(mContext, "Write something...", R.style.customToast).show();
                }

            }
        });

        // setting up the smile face and the actions which happen when you click on it.
        mSmileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(TAG, "onClick: clicking the smile icon" );
                ScrollView scrollView = findViewById(R.id.scrollview_layout);
                LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        50.0f
                );
                scrollView.setLayoutParams(parameter);

                FrameLayout emojiconFragment = findViewById(R.id.emojicons_frame_layout);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        50.0f
                );
                emojiconFragment.setLayoutParams(param);

                hideKeyboard();
                mKeyboardIcon.setVisibility(View.VISIBLE);
                mSmileIcon.setVisibility(View.GONE);

            }
        });

        // settting up the keyboard and the actions which happen when you click on it.
        mKeyboardIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(TAG, "onClick: clicking the keyboard icon" );
                // setting up the layout weight
                ScrollView scrollView = findViewById(R.id.scrollview_layout);
                LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0.0f
                );
                scrollView.setLayoutParams(parameter);

                // setting up the layout weight
                FrameLayout emojiconFragment = findViewById(R.id.emojicons_frame_layout);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        100.0f
                );
                emojiconFragment.setLayoutParams(param);

                showKeyboard();
                mKeyboardIcon.setVisibility(View.GONE);
                mSmileIcon.setVisibility(View.VISIBLE);
            }
        });

        mEditTextEmojicon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(TAG, "onClick: clicked on edit text field" );
                if(mKeyboardIcon.getVisibility() == View.VISIBLE) {

                    ScrollView scrollView = findViewById(R.id.scrollview_layout);
                    LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            0.0f
                    );
                    scrollView.setLayoutParams(parameter);

                    // setting up the layout weight
                    FrameLayout emojiconFragment = findViewById(R.id.emojicons_frame_layout);
                    LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            100.0f
                    );
                    emojiconFragment.setLayoutParams(param);

//                    showKeyboard();
                    mKeyboardIcon.setVisibility(View.GONE);
                    mSmileIcon.setVisibility(View.VISIBLE);
                }
            }
        });

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: back arrow is clicked" );

                if(mKeyboardIcon.getVisibility() == View.VISIBLE) {
                    Log.e(TAG, "onBackPressed: keyboard icon is visible" );
                    ScrollView scrollView = findViewById(R.id.scrollview_layout);
                    LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            0.0f
                    );
                    scrollView.setLayoutParams(parameter);

                    // setting up the layout weight
                    FrameLayout emojiconFragment = findViewById(R.id.emojicons_frame_layout);
                    LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            100.0f
                    );
                    emojiconFragment.setLayoutParams(param);

                    mKeyboardIcon.setVisibility(View.GONE);
                    mSmileIcon.setVisibility(View.VISIBLE);
                }
                else {

                    AlertDialog.Builder builder = new AlertDialog.Builder(SharePostActivity.this);
                    builder.setCancelable(true);

                    if(!mEditTextEmojicon.getText().toString().equals("")) {
                        showAlertDialog(builder, "Your post will be discarded !", "filled");
                    }
                    else
                        showAlertDialog(builder, "You can even post a quote of the day", "empty");
                }
            }
        });
    }

    @Override
    public void onBackPressed() {

        if(mKeyboardIcon.getVisibility() == View.VISIBLE) {

            ScrollView scrollView = findViewById(R.id.scrollview_layout);
            LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0.0f
            );
            scrollView.setLayoutParams(parameter);

            // setting up the layout weight
            FrameLayout emojiconFragment = findViewById(R.id.emojicons_frame_layout);
            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    100.0f
            );
            emojiconFragment.setLayoutParams(param);

            mKeyboardIcon.setVisibility(View.GONE);
            mSmileIcon.setVisibility(View.VISIBLE);
        }
        else {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);

            if(!mEditTextEmojicon.getText().toString().equals("")) {
                showAlertDialog(builder, "Your post will be discarded !", "filled");
            }
            else
                showAlertDialog(builder, "You can even post a quote of the day", "empty");
        }
    }

    private void showAlertDialog(AlertDialog.Builder builder, String message, String type) {

        if(type.equals("empty")) {
            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("No, thanks", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(SharePostActivity.this, HomeActivity.class));
                            finish();
                        }
                    })
                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
        }
        else {
            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(SharePostActivity.this, HomeActivity.class));
                            finish();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
        }
        builder.show();

    }

    private void sendPost() {
        mProgressBar.setVisibility(View.VISIBLE);
        mEditTextEmojicon.setEnabled(false);
        mTitle.setEnabled(false);
        mSmileIcon.setEnabled(false);
        mKeyboardIcon.setEnabled(false);
        mShare.setEnabled(false);
        mFirebaseMethods.addPostToDatabase(mEditTextEmojicon.getText().toString(),
                mTitle.getText().toString(), mEditTextEmojicon, mTitle,
                mSmileIcon, mKeyboardIcon, mShare, mProgressBar);
    }

    private boolean checkInternetConnection() {

        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
            else {
                StyleableToast.makeText(this, "Check your internet connection", R.style.customToast).show();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void instantiateFirebase() {

        mContext = SharePostActivity.this;
        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseMethods = new FirebaseMethods(mContext);
    }

    public void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setEmojiconFragment(boolean useSystemDefault) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.emojicons_frame_layout, EmojiconsFragment.newInstance(useSystemDefault))
                .commit();
    }

    @Override
    public void onEmojiconClicked(Emojicon emojicon) {
        EmojiconsFragment.input(mEditTextEmojicon, emojicon);
    }

    @Override
    public void onEmojiconBackspaceClicked(View v) {
        EmojiconsFragment.backspace(mEditTextEmojicon);
    }
}
