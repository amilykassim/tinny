package com.amily.tycoon.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.amily.tycoon.models.Story;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import jp.shts.android.storiesprogressview.StoriesProgressView;

public class ViewPostActivity extends AppCompatActivity {

    private static final String TAG = "ViewPostActivity";
    private Context mContext;

    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private FirebaseMethods mFirebaseMethods;

    // widgets
    private ImageView mStoryImage, mBackArrow;
    private CircleImageView mProfileImage;
    private VideoView mVideoView;
    private TextView mUsername, mDateCreated, mCaption, mVideoViews;
    private StoriesProgressView mStoriesProgressView;
    private ProgressBar mLoadingProgressBar;
    private FrameLayout mLeft, mRight;
    private RelativeLayout mParentLayout;

    // vars
    private int counter = 0;
    private ArrayList<Story> mPhotos;
    private String mFileName;
    private SharedPreferences mSettings;
    private UserAccountSettings mUserAccountSettings;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_post);

        Log.e(TAG, "onCreate: entered in the view post activity ///////////////////////////");

        instantiateFirebase();

        mStoryImage = findViewById(R.id.story_image);
        mVideoView = findViewById(R.id.story_video);
        mProfileImage = findViewById(R.id.profileImage);
        mUsername = findViewById(R.id.username);
        mDateCreated = findViewById(R.id.date_created);
        mVideoViews = findViewById(R.id.video_views);
        mCaption = findViewById(R.id.caption);
        mStoriesProgressView = findViewById(R.id.stories);
        mLoadingProgressBar = findViewById(R.id.loadingProgressBar);
        mLeft = findViewById(R.id.pressedLeft);
        mRight = findViewById(R.id.pressedRight);
        mBackArrow = findViewById(R.id.backArrow);
        mParentLayout = findViewById(R.id.parentLayout);

        mSettings = ViewPostActivity.this.getSharedPreferences(getString(R.string.PREFS_DOWNLOADING_SYSTEMS), 0);

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewPostActivity.this.finish();
            }
        });

        try {
            Log.e(TAG, "onCreate: before retrieving the photo from bundles /////////");
            mPhotos = getPhotosFromIntentExtras();
            Log.e(TAG, "onCreate: after retrieving the photo from bundles ///////////////");

        }catch (Exception e) {
            Log.e(TAG, "onCreateView: There is an EXCEPTION : " + e.getMessage());
            e.printStackTrace();
        }

        // setting up the latest username that you uploaded with the photo but it will get updated in the getPhotoDetails method
        mUsername.setText(mPhotos.get(counter).getUnm());

        // setting up the date created
        String retrievedTimestamp = String.valueOf(mPhotos.get(counter).getTm());
        String timeAgo = GetTimeAgo.getTimeAgo(Long.parseLong(retrievedTimestamp), this);
        mDateCreated.setText(timeAgo);


        // setting up the stories
        mStoriesProgressView.setStoriesCount(mPhotos.size());
        //   mStoriesProgressView.startStories();


        // setting up the profile photo and the username
        getPhotoDetails(mPhotos.get(counter));


        // it's a photo on a first click
        if(!mPhotos.get(counter).getUrl().equals("")) {

            mStoriesProgressView.setStoryDuration(4000L);

            Log.e(TAG, "onCreateView: it's a photo the counter is : " + counter);
            mVideoView.setVisibility(View.GONE);
            mStoryImage.setVisibility(View.VISIBLE);


            DatabaseReference videoViewsReference = FirebaseDatabase.getInstance().getReference();
            Query videoViewsQuery = videoViewsReference
                    .child(mContext.getString(R.string.dbname_user_stories))
                    .child(mPhotos.get(counter).getUid())
                    .child(mPhotos.get(counter).getPid())
                    .child(mContext.getString(R.string.field_video_views));


            videoViewsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    try {
                        int currentViews = Integer.parseInt(dataSnapshot.getValue(String.class));
                        String views = (currentViews + 1) + " views";

                        if(mPhotos.get(counter).getUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                            mVideoViews.setText(views);
                        }

                    }catch (Exception e) {
                        Log.e(TAG, "onDataChange: There is an Exception");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            Glide.with(this)
                    .load(mPhotos.get(counter).getUrl())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            mLoadingProgressBar.setVisibility(View.VISIBLE);
//                            mStoriesProgressView.pause();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            mLoadingProgressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(mStoryImage);

            mCaption.setText(mPhotos.get(counter).getC());
        }
        // it's a video on a first click
        else {
            Log.e(TAG, "onNext: it's a video on the counter : " + counter );

            // retrieving the video path that was saved during uploading the video, if there is any
            String videoPath = mSettings.getString(mPhotos.get(counter).getPid(), "");
            Log.e(TAG, "onCreate: the video path retrieved in view story activity is : " + videoPath );

            mStoriesProgressView.setStoryDuration(4000L);
            if(videoPath.equals("")) {
                startVideoRelatedExternal();
            }
            else {
                playVideoOffline(videoPath);
            }

            mCaption.setText(mPhotos.get(counter).getC());

        }

        mStoriesProgressView.setStoriesListener(new StoriesProgressView.StoriesListener() {

            // On Next
            @Override
            public void onNext() {

                mStoriesProgressView.setStoryDuration(4000L);
                ++counter; // incrementing the counter


                // it's a photo
                if(!mPhotos.get(counter).getUrl().equals("")) {
                    Log.e(TAG, "onNext: it's a photo on the count " + counter);

                    mVideoView.setVisibility(View.GONE);
                    mStoryImage.setVisibility(View.VISIBLE);

                    Glide.with(ViewPostActivity.this)
                            .load(mPhotos.get(counter).getUrl())
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    mLoadingProgressBar.setVisibility(View.VISIBLE);
//                                    mStoriesProgressView.pause();
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    mLoadingProgressBar.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .into(mStoryImage);

                }
                // it's a video ON NEXT
                else {

                    String videoPath = mSettings.getString(mPhotos.get(counter).getPid(), "");

                    if(videoPath.equals("")) {
                        startVideoRelatedExternal();
                    }
                    else {
                        playVideoOffline(videoPath);
                    }

                }

                mCaption.setText(mPhotos.get(counter).getC()); // putting the caption on the photo

                // setting the time date created
                String retrievedTimestamp = String.valueOf(mPhotos.get(counter).getTm());
                String timeAgo = GetTimeAgo.getTimeAgo(Long.parseLong(retrievedTimestamp), ViewPostActivity.this);
                mDateCreated.setText(timeAgo);
            }

//            // on previous ON PREVIOUS
//            @Override
//            public void onPrev() {
//                // this condition is to avoid the index out of bounds exception
//                if(counter > 0) {
//
//                    mStoriesProgressView.setStoryDuration(4000L);
//                    --counter; // decrementing the counter
//
//                    // it's a photo
//                    if(!mPhotos.get(counter).getUrl().equals("")) {
//
//                        Log.e(TAG, "onPrev: it'a photo on the counter : " + counter);
//                        mVideoView.setVisibility(View.GONE);
//                        mStoryImage.setVisibility(View.VISIBLE);
//
//                        Glide.with(ViewPostActivity.this)
//                                .load(mPhotos.get(counter).getUrl())
//                                .listener(new RequestListener<Drawable>() {
//                                    @Override
//                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
//                                        mLoadingProgressBar.setVisibility(View.VISIBLE);
//                                        return false;
//                                    }
//
//                                    @Override
//                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
//                                        mLoadingProgressBar.setVisibility(View.GONE);
//                                        return false;
//                                    }
//                                })
//                                .into(mStoryImage);
//                    }
//                    // it's a video ON PREVIOUS
//                    else {
//
//                        String videoPath = mSettings.getString(mPhotos.get(counter).getPid(), "");
//
//                        if(videoPath.equals("")) {
//                            startVideoRelatedExternal();
//                        }
//                        else {
//                            playVideoOffline(videoPath);
//                        }
//
//                    }
//
//                    mCaption.setText(mPhotos.get(counter).getC());
//
//                    // setting the time date created
//                    String retrievedTimestamp = String.valueOf(mPhotos.get(counter).getTm());
//                    String timeAgo = GetTimeAgo.getTimeAgo(Long.parseLong(retrievedTimestamp), ViewPostActivity.this);
//                    mDateCreated.setText(timeAgo);
//                }
//            }

            @Override
            public void onComplete() {
                try {

//                    ViewPostActivity.this.finish();
                }catch (NullPointerException e) {
                    Log.e(TAG, "onClick: NullPointerException : " + e.getMessage());
                }
            }
        });

        // skip the stories
        mRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStoriesProgressView.skip();
            }
        });

        // reverse the stories
        mLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mStoriesProgressView.reverse();
            }
        });

        mParentLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    if(mStoryImage.getVisibility() == View.VISIBLE) {
//                        mStoriesProgressView.pause();
                        return true;
                    }
                }
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(mStoryImage.getVisibility() == View.VISIBLE) {
//                        mStoriesProgressView.resume();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void getPhotoDetails(Story photo) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_user_account_settings))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(photo.getUid());


        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()) {
                    mUserAccountSettings = singleSnapshot.getValue(UserAccountSettings.class);
                }

                // setting up the profile photo
                Glide.with(ViewPostActivity.this)
                        .load(mUserAccountSettings.getPp())
                        .into(mProfileImage);
                mUsername.setText(mUserAccountSettings.getUnm()); // setting the username
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        SharedPreferences.Editor editor = mSettings.edit();

        try {
            for (int i = 0; i < mPhotos.size(); i++) {
                editor.putString("" + i, "");
                editor.apply();
            }
            Log.e(TAG, "onStop: the on start is called");
        }catch (Exception e) {
            Log.e(TAG, "onStart: there is an exception in On Start method : " + e.getMessage());
        }
    }

    private void startVideoRelatedExternal() {

        mVideoView.setVisibility(View.VISIBLE);
        mStoryImage.setVisibility(View.GONE);

        mLoadingProgressBar.setVisibility(View.VISIBLE);

        String keyOfVideoPath = "" + mPhotos.get(counter).getTm();
        SharedPreferences settings = this.getSharedPreferences("PREFS_EXTERNAL_STORAGE", 0);

        String cachedUrl_pref = settings.getString(keyOfVideoPath, "");
        Log.e(TAG, "startVideoRelatedExternal: the retrieved video's path is : " + cachedUrl_pref );

        // checking for the internet connection
        try {
            Log.e(TAG, "startVideoRelatedExternal: entered in the try catch block");
            ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            Log.e(TAG, "startVideoRelatedExternal: finished getting the network information");

            if(networkInfo != null && networkInfo.isConnected()) {

                Log.e(TAG, "startVideoRelatedExternal: The network is available");
                if(cachedUrl_pref.equals("")) {
                    Log.e(TAG, "onClick: the cached url is empty");
                    mLoadingProgressBar.setVisibility(View.VISIBLE);
                    mStoriesProgressView.setStoryDuration(100000000000L);
                    mStoriesProgressView.startStories();

                    String isDownloading = mSettings.getString("" + counter, "");
                    if(isDownloading.equals("")) {

                        Log.e(TAG, "onError: the video of this counter " + counter + " is not being downloaded, so let's download it");
                        SharedPreferences.Editor editor = mSettings.edit();
                        editor.putString("" + counter, "downloading");
                        editor.commit();
                        new DownloadVideo().execute(counter);
                    }
                    else {
                        Log.e(TAG, "onError: the video of this couner " + counter + "is being downloaded");
                        mLoadingProgressBar.setVisibility(View.VISIBLE);
                    }
                }
                else {
                    playVideoOffline(cachedUrl_pref);
                }
            }
            else {
                playVideoOffline(cachedUrl_pref);
            }
        } catch (Exception e) {
            Log.e(TAG, "startVideo: there is an error in playing the video, maybe the video is miessed");
            e.printStackTrace();
//            mStoriesProgressView.pause();
            mLoadingProgressBar.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Check your internet connection", Toast.LENGTH_SHORT).show();
        }

    }


    private void playVideoOffline(final String cachedUrl_pref) {

        Log.e(TAG, "startVideoRelatedExternal: the network is not available");
        Log.e(TAG, "onDataChange: the video path from preferences is : " + cachedUrl_pref );

        mLoadingProgressBar.setVisibility(View.INVISIBLE);
        mVideoView.setVideoPath(cachedUrl_pref);

        Log.e(TAG, "onNext: it's a video on the counter : " + counter );
        mVideoView.setVisibility(View.VISIBLE);
        mStoryImage.setVisibility(View.GONE);

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final MediaPlayer mp) {

                Log.e(TAG, "onPrepared: the video duration below is : " + mVideoView.getDuration());
                mp.setLooping(true); // loop the video once it finishes
                mVideoView.requestFocus();
                mVideoView.start();
                mStoriesProgressView.setStoryDuration(mVideoView.getDuration());
                mStoriesProgressView.startStories();

                // add a view if it's not the owner of the video
                DatabaseReference videoViewsReference = FirebaseDatabase.getInstance().getReference();
                Query videoViewsQuery = videoViewsReference
                        .child(mContext.getString(R.string.dbname_user_stories))
                        .child(mPhotos.get(counter).getUid())
                        .child(mPhotos.get(counter).getPid())
                        .child(mContext.getString(R.string.field_video_views));


                videoViewsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        try {
                            int currentViews = Integer.parseInt(dataSnapshot.getValue(String.class));
                            String views = (currentViews + 1) + " views";

                            if(!mPhotos.get(counter).getUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                mFirebaseMethods.addViewToDatabase(mPhotos.get(counter), "" + (currentViews + 1));
                            }
                            else {
                                mVideoViews.setText(views);
                            }
                        }catch (Exception e) {
                            Log.e(TAG, "onDataChange: There is an Exception : " + e.getMessage());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        });

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {

                Log.e(TAG, "onError: the given path is not being found, so entered in on error listener");
                Log.e(TAG, "onError: entered in the on error listener");
                mStoriesProgressView.setStoryDuration(100000000000L);
                mStoriesProgressView.startStories();
                String isDownloading = mSettings.getString("" + counter, "");

                // removing the video path that was saved bcz the user has deleted it
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putString(mPhotos.get(counter).getPid(), "");
                editor.apply();


                /* Check if the current video is being downloaded or not, if not then download it*/
                if(isDownloading.equals("")) {

                    mLoadingProgressBar.setVisibility(View.VISIBLE);
//                    SharedPreferences.Editor editor = mSettings.edit();
                    Log.e(TAG, "onError: the video of this counter " + counter + " is not being downloaded, so let's download it");
                    editor.putString("" + counter, "downloading");
                    editor.commit();
                    new DownloadVideo().execute(counter);
                }
                else {
                    Log.e(TAG, "onError: the video of this couner " + counter + "is being downloaded");
                    mLoadingProgressBar.setVisibility(View.VISIBLE);
                }

                // returning true indicates that the error happened
                return true;
            }
        });
    }


    // download the video
    public class DownloadVideo extends AsyncTask<Integer, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mLoadingProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Integer... integers) {

            // the integer passed is the position of a video file
            // check if the video path is valid and then download it
            if(!mPhotos.get(integers[0]).getvUrl().equals("")) {
                Log.e(TAG, "doInBackground: the video path is valid");
                try {
                    Log.e(TAG, "doInBackground: downloading the video");
                    URL url = new URL(mPhotos.get(integers[0]).getvUrl());//Create Download URl
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();//Open Url Connection
                    c.setRequestMethod("GET");//Set Request Method to "GET" since we are grtting data
                    c.connect();//connect the URL Connection

                    //If Connection response is not OK then show Logs
                    if (c.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        Log.e(TAG, "Server returned HTTP " + c.getResponseCode()
                                + " " + c.getResponseMessage());
                    }
                    // creating directory and file name
                    Log.e(TAG, "saveVideoToExternalStorage: attempting to store the video on the external storage");
                    String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
                    File myDir = new File(root + "/Tinny Videos");
                    myDir.mkdirs();
                    Random generator = new Random();
                    int n = 10000;
                    n = generator.nextInt(n);
                    String fname = "Video-" + n + ".mp4";
                    mFileName = fname;
                    File file = new File(myDir, fname);
                    if (file.exists())
                        file.delete();

                    Log.e(TAG, "doInBackground: creating files and directory has no issues");

                    FileOutputStream fos = new FileOutputStream(file);//Get OutputStream for NewFile Location
                    InputStream is = c.getInputStream();//Get InputStream for connection

                    byte[] buffer = new byte[1024];//Set buffer type
                    int len1 = 0;//init length
                    while ((len1 = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len1);//Write new file
                    }
                    Log.e(TAG, "doInBackground: downloadig has no issue");

                    //Close all connection after doing task
                    fos.close();
                    is.close();
                    Log.e(TAG, "doInBackground: closed the streams");

                    // store the downloaded file in the preferences
                    if(!mPhotos.get(integers[0]).getvUrl().equals("")) {

                        SharedPreferences settings = ViewPostActivity.this.getSharedPreferences("PREFS_EXTERNAL_STORAGE", 0);
                        SharedPreferences.Editor editor = settings.edit();

                        String videoFilePath = "/storage/emulated/0/Movies/Tinny Videos/" + mFileName;
                        editor.putString("" + mPhotos.get(integers[0]).getTm(), videoFilePath);
                        editor.commit();
                        Log.e(TAG, "doInBackground: finished downloading the video, " +
                                "and the video file path is : " + videoFilePath);
                        Log.e(TAG, "doInBackground: downloaded the video path of this counter : " + counter);
                    }

                    // Tell the media scanner about the new file so that it is
                    // immediately available to the user.

                    // the video file was downloaded

                    MediaScannerConnection.scanFile(ViewPostActivity.this, new String[] { file.toString() }, null, new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.e("ExternalStorage", "Scanned " + path + ":");
                            Log.e("ExternalStorage", "-> uri=" + uri);
                        }
                    });

                    return true;

                } catch (Exception e) {
                    Log.e(TAG, "doInBackground: there is an exception in downloading the video " +
                            "for saving, : " + e.getMessage());

                }
                return false;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            try {
                if(aBoolean) {
                    SharedPreferences settings = ViewPostActivity.this.getSharedPreferences("PREFS_EXTERNAL_STORAGE", 0);
                    String cachedUrl_pref = settings.getString("" + mPhotos.get(counter).getTm(), "");
                    playVideoOffline(cachedUrl_pref);
                }
                else {
                    Toast.makeText(ViewPostActivity.this, "There is a problem in loading the video", Toast.LENGTH_SHORT).show();
                }
            }catch (Exception e) {
                Log.e(TAG, "onPostExecute: Exception caught" + e.getMessage());
            }
        }
    }


    private void instantiateFirebase() {
        Log.e(TAG, "instantiateFirebase: entered in the instantiateFirebase method");

        mContext = ViewPostActivity.this;
        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseMethods = new FirebaseMethods(ViewPostActivity.this);
    }

    @Override
    public void onDestroy() {
        mStoriesProgressView.destroy();
        super.onDestroy();
    }

    private ArrayList<Story> getPhotosFromIntentExtras() {

        return getIntent().getParcelableArrayListExtra(getString(R.string.photo));
    }

}
