package com.amily.tycoon.utils;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.amily.tycoon.home.HomeActivity;
import com.amily.tycoon.models.Comment;
import com.amily.tycoon.models.Post;
import com.bumptech.glide.Glide;
//import com.google.android.gms.ads.AdListener;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewPostFragment extends Fragment implements TextToSpeech.OnInitListener{


    @Override
    public void onInit(int status) {

        if(status == TextToSpeech.SUCCESS) {
            int result = mTextToSpeech.setLanguage(Locale.US);

            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "onInit: This language is not supported");
            }
        }
        else {
            Log.e(TAG, "onInit: Text to speech : Initialization failed");
        }
    }

    private static final String TAG = "ViewPostFragment";
    private Context mContext;

    // widgets
    private ImageView mBackArrow, mSelectedImage, mOptions;
    private CircleImageView mProfileImage;
    private TextView mUsername, mDisplayName, mTimePosted,
            mDescription, mCoursesTaken, mPostMessage;
    private Button mMessageBtn, mPostsBtn, mMoreInfoBtn, mComment, mPostViews;
    private Dialog mDialog;
    private ProgressBar mProgressBar;
    private LinearLayout mParentLinearLayoutBottom;

    // vars
    private Post mPost;
    private TextToSpeech mTextToSpeech;

    // Ads
//    private InterstitialAd interstitialAd;

    // shared preferences
    private SharedPreferences mSettings;

    public ViewPostFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_post, container, false);


        // Ads
//        interstitialAd = new InterstitialAd(getActivity());
////        interstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712"); // here is your test app id
//        interstitialAd.setAdUnitId("ca-app-pub-6617145085687341/3487530081"); // here is your real interstitial unit id
//        interstitialAd.loadAd(new AdRequest.Builder().build());
////
////
////        interstitialAd.setAdListener(new AdListener()
////                                     {
////                                         @Override
////                                         public void onAdClosed() {
////                                             super.onAdClosed();
////
////                                             // this is for showing the layout once a user clicks on back press
////
////                                             try {
////                                                 getActivity().finish();
////                                             }catch (Exception e) {
////                                                 Log.e(TAG, "onAdClosed: THERE IS AN EXCEPTION : " + e.getMessage());
////                                             }
////                                             interstitialAd.loadAd(new AdRequest.Builder().build());
////                                         }
////                                     }
////        );

//        // Ads shared preferences
//        mSettings = getActivity().getSharedPreferences(getString(R.string.ads_shared_preferences), 0);



        // dialog for displaying the profile image when it is clicked
        mDialog = new Dialog(getActivity());
        mDialog.setContentView(R.layout.layout_view_profile_photo_dialog);
        mSelectedImage = mDialog.findViewById(R.id.selectedImage);


        mContext = getActivity();
        mBackArrow = view.findViewById(R.id.backArrow);
        mProgressBar = view.findViewById(R.id.progressbar);
        mOptions = view.findViewById(R.id.options_iv_view_post_fragment);
        mProfileImage = view.findViewById(R.id.profileImage);
        mUsername = view.findViewById(R.id.username);
        mDescription = view.findViewById(R.id.description);
        mCoursesTaken = view.findViewById(R.id.courses_taken);
//        mDisplayName = view.findViewById(R.id.display_name);

        mParentLinearLayoutBottom = view.findViewById(R.id.linearLayoutParent);
        mMessageBtn = view.findViewById(R.id.message_btn);
        mPostsBtn = view.findViewById(R.id.posts_btn);
        mMoreInfoBtn = view.findViewById(R.id.more_info_btn);
        mTimePosted = view.findViewById(R.id.image_time_posted);
        mPostMessage = view.findViewById(R.id.post_message);
        mComment = view.findViewById(R.id.post_comments);
        mPostViews = view.findViewById(R.id.post_views);


        // text to speech variables
        mTextToSpeech = new TextToSpeech(getActivity(), this);


        // if you are looking other person's post or it's your own post, the message and the post button are dismissed
        if(getCallingActivityFromBundle().equals(getString(R.string.post_activity))
                || getPostFromBundle().getUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            mMessageBtn.setVisibility(View.GONE);
            mPostsBtn.setVisibility(View.GONE);
        }
        else {
            mMessageBtn.setVisibility(View.VISIBLE);
            mPostsBtn.setVisibility(View.VISIBLE);
        }

        return view;
    }

    // this checks if the fragment is added to the activity and then resume it
    @Override
    public void onResume() {
        super.onResume();

        if(isAdded()) {

            try {

                Log.e(TAG, "onCreateView: entered in the view posts Fragment ");
                mPost = getPostFromBundle();
                initOnClickListener();

                // if it is not your post then hide the views
                if(!getPostFromBundle().getUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    mPostViews.setVisibility(View.GONE);
                    LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            100.0f
                    );
                    mComment.setLayoutParams(parameter);
                }

                // if it is your own post then show the views
                else {
                    mPostViews.setVisibility(View.VISIBLE);
                    LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            50.0f
                    );
                    mComment.setLayoutParams(parameter);
                }

                // get the views
                getViews();

            }catch (NullPointerException e) {
                Log.e(TAG, "onCreateView: THERE IS AN EXCEPTION : NullPointerException : " + e.getMessage());

            }

        }
    }

    @Override
    public void onStop() {
        super.onStop();

//        String count = mSettings.getString(getString(R.string.ads_counting_key), "0");
//        int realCount = Integer.parseInt(count);
//
//        Log.e(TAG, "onStop: the count number for ads is : " + realCount + " //////////");
//        // We increment the real count in the post adapter when a user click on a certain posts
//        // The ad is loaded if the user has seen 3 posts and the add is loaded
//        if(interstitialAd.isLoaded() && realCount >= 3) {
//            interstitialAd.show();
//            SharedPreferences.Editor editor = mSettings.edit();
//            editor.putString(mContext.getString(R.string.ads_counting_key), "0"); // the first element is the key, and the second element is the value to be stored
//            editor.apply();
//        }
    }

    private void getViews() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();

        Query query = ref
                .child(getString(R.string.dbname_user_posts))
                .child(getPostFromBundle().getUid())
                .child(getPostFromBundle().getPid())
                .child(getString(R.string.field_views));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                int count = 0;
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    count++;
                }


                if(count == 0)
                    mPostViews.setText("No views yet");
                else
                    mPostViews.setText(count + " views");

                setupWidgets();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setupWidgets() {
        mPostMessage.setText(mPost.getD());
        mTimePosted.setText(getLastTimeAgo());

        if(getPostFromBundle().getC().size() == 0) {
            mComment.setText("Add a comment");
        }
        else if(getPostFromBundle().getC().size() == 1) {
            mComment.setText("View " + getPostFromBundle().getC().size() + " comment");
        }
        else if(getPostFromBundle().getC().size() > 1) {
            mComment.setText("View " + getPostFromBundle().getC().size() + " comments");
        }

        // get the profile photo
        getProfilePhoto();

    }

    private void getProfilePhoto() {


        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .child(getPostFromBundle().getUid())
                .child(mContext.getString(R.string.field_profile_photo));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                try {
                    Glide.with(mContext).load(dataSnapshot.getValue(String.class)).into(mProfileImage);
                }catch (Exception e) {
                    Log.e(TAG, "onDataChange: THERE IS AN EXCEPTION : " + e.getMessage());
                }

                mProfileImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        try {
                            Glide.with(mContext).load(dataSnapshot.getValue(String.class)).into(mSelectedImage);
                        }catch (Exception e) {
                            Log.e(TAG, "onClick: THERE IS AN EXCEPTION : " + e.getMessage());
                        }
                        popUpDialog();
                    }
                });


                getUsername();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getUsername() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .child(getPostFromBundle().getUid())
                .child(mContext.getString(R.string.field_displayName));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                mUsername.setText(dataSnapshot.getValue(String.class));
                getDescription();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getDescription() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .child(getPostFromBundle().getUid())
                .child(mContext.getString(R.string.field_description));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                mDescription.setText(dataSnapshot.getValue(String.class));

                if(dataSnapshot.getValue(String.class).equals("")) {
                    mDescription.setText("No interests yet.");
                }
                if(dataSnapshot.getValue(String.class).length() >= 40) {
                    mDescription.setText(dataSnapshot.getValue(String.class).substring(0, 40) + "....");
                }

                getCousesTaken();

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getCousesTaken() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .child(getPostFromBundle().getUid())
                .child(mContext.getString(R.string.field_courses_taken));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                mCoursesTaken.setText("Courses : " + dataSnapshot.getValue(String.class));

                if(dataSnapshot.getValue(String.class).equals("")) {
                    mCoursesTaken.setText("No courses yet.");
                }
                if(dataSnapshot.getValue(String.class).length() >= 15) {
                    mCoursesTaken.setText("Courses : " + dataSnapshot.getValue(String.class).substring(0, 15) + "....");
                }


                // get the comments size
                getCommentsSize();

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getCommentsSize() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_posts))
                .child(getPostFromBundle().getUid())
                .child(getPostFromBundle().getPid())
                .child(mContext.getString(R.string.field_comments));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                int count = 0;
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    count++;
                }

                if(count == 0) {
                    mComment.setText("Add a comment");
                }
                else if(count == 1) {
                    mComment.setText("View " + count + " comment");
                }
                else if(count > 1) {
                    mComment.setText("View " + count + " comments");
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void initOnClickListener() {

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: navigating back to view post fragment" );

                try {
                    if(getCallingActivityFromBundle().equals(getString(R.string.home_activity))) {
                        getActivity().finish();
                    }
                    else if(getCallingActivityFromBundle().equals(getString(R.string.post_activity))) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                    else {
                        getActivity().getSupportFragmentManager().popBackStack();

                    }
                }catch (NullPointerException e) {
                    Log.e(TAG, "onClick: NullPointerException : " + e.getMessage());
                    getActivity().getSupportFragmentManager().popBackStack();
                }

            }
        });

        mComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: clicked on the comment");
                Intent intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra(getString(R.string.post), mPost);
                intent.putExtra(getString(R.string.field_comments), getString(R.string.field_comments));
                startActivity(intent);

            }
        });

        mPostViews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: clicked on post views");
                Intent intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra(getString(R.string.post), mPost);
                intent.putExtra(getString(R.string.field_views), getString(R.string.field_views));
                startActivity(intent);
            }
        });

        mMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: clicked on the message fragment");

                Intent intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra(getString(R.string.post), mPost);
                intent.putExtra(getString(R.string.field_message), getString(R.string.field_message));
                startActivity(intent);
            }
        });

        mPostsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra(getString(R.string.post), mPost);
                intent.putExtra(getString(R.string.view_personal_posts), getString(R.string.view_personal_posts));
                startActivity(intent);
            }
        });

        mMoreInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra(getString(R.string.post), mPost);
                intent.putExtra(getString(R.string.view_other_profile_fragment), getString(R.string.view_other_profile_fragment));
                startActivity(intent);
            }
        });

        mOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(FirebaseAuth.getInstance().getCurrentUser().getUid().equals(getPostFromBundle().getUid())) {

                    CharSequence[] options = new CharSequence[]{"Delete", "Cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setCancelable(true);

                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if(which == 0) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setCancelable(true);

                                builder.setMessage("Are you sure you want to delete this post?")
                                        .setCancelable(false)
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                                // deleting the post
                                                Log.e(TAG, "onClick: deleting the post");
                                                mProgressBar.setVisibility(View.VISIBLE);
                                                mOptions.setEnabled(false);

                                                deletePost();
                                            }
                                        })
                                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                // dismiss
                                            }
                                        });
                                builder.show();
                            }
                            if (which == 1) {
                                // cancel
                            }

                        }
                    });
                    builder.show();
                }

                // report if you found inappropriate content
                else {
                    CharSequence[] options = new CharSequence[]{"Listen as Audio", "Stop audio", "Report", "Cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setCancelable(true);

                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (which == 0) {
                                // listen the text as an audio
                                speakOut();
                            }
                            if(which == 1) {
                                // stop the audio
                                if(mTextToSpeech != null) {
                                    mTextToSpeech.stop();
                                }
                            }
                            if (which == 2) {
                                // report the user or the content
                                Intent myPostIntent = new Intent(getActivity(), PostActivity.class);
                                myPostIntent.putExtra(getString(R.string.report_fragment), getString(R.string.report_fragment));
                                startActivity(myPostIntent);
                            }
                            if (which == 3) {
                                // cancel
                            }

                        }
                    });
                    builder.show();
                }
            }
        });
    }

    private void speakOut() {
        String text = mPostMessage.getText().toString();
        mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void onDestroy() {
        if(mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private void deletePost() {
        Log.e(TAG, "deleteComment: deleting comment...");

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

//        rootRef.child(mContext.getString(R.string.dbname_posts))
//                .child(mPost.getPid())
//                .removeValue();

        rootRef.child(mContext.getString(R.string.dbname_user_posts))
                .child(mPost.getUid())
                .child(mPost.getPid())
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            mProgressBar.setVisibility(View.GONE);
                            mOptions.setEnabled(true);
                            startActivity(new Intent(getActivity(), HomeActivity.class));
                            getActivity().finish();
                        }
                        else {
                            mProgressBar.setVisibility(View.GONE);
                            mOptions.setEnabled(true);
                        }
                    }
                });
    }

    private void popUpDialog() {

        TextView txtclose;
        txtclose = mDialog.findViewById(R.id.txtclose);
        txtclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });

        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.show();
    }

    private String getLastTimeAgo() {
        String retrievedTimestamp = String.valueOf(mPost.getTm());
        String timeAgo = GetTimeAgo.getTimeAgo(Long.parseLong(retrievedTimestamp), mContext);
        if(timeAgo == null) {
            return "Just now";
        }
        return timeAgo;
    }

    /*Get post from the incoming bundle*/
    private Post getPostFromBundle() {
        Log.e(TAG, "getPhotoFromBundle: " );

        try {
            Bundle bundle = this.getArguments();
            if (bundle != null) {
                return bundle.getParcelable(getString(R.string.post));
            }
            else {
                return null;
            }
        }catch (Exception e) {
            Log.e(TAG, "getPostFromBundle: THERE IS AN EXCEPTION : " + e.getMessage());
        }
        return null;
    }

    /*Get the calling activity from the incoming bundle from home activity*/
    private String getCallingActivityFromBundle() {
        Log.e(TAG, "getCallingActivityFromBundle: ");

        try {
            Bundle bundle = this.getArguments();
            if (bundle != null) {

                if (bundle.getString(getString(R.string.home_activity)) != null) {
                    return bundle.getString(getString(R.string.home_activity));
                }
                if (bundle.getString(getString(R.string.post_activity)) != null) {
                    return bundle.getString(getString(R.string.post_activity));
                }
            }
            else {
                return null;
            }
        }catch (Exception e) {
            Log.e(TAG, "getCallingActivityFromBundle: THERE IS AN EXCEPTION : " + e.getMessage());
        }
        return null;
    }
}
