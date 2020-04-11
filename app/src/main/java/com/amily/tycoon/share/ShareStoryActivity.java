package com.amily.tycoon.share;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.VideoView;

import com.amily.tycoon.models.User;
import com.amily.tycoon.R;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.utils.FirebaseMethods;
import com.amily.tycoon.utils.ImageManager;
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
import com.rockerhieu.emojicon.EmojiconsFragment;
import com.rockerhieu.emojicon.emoji.Emojicon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ShareStoryActivity extends AppCompatActivity implements EmojiconGridFragment.OnEmojiconClickedListener,
        EmojiconsFragment.OnEmojiconBackspaceClickedListener {

    private static final String TAG = "SharePostActivity";
    private Context mContext;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        for(int i = 0; i < grantResults.length; i++) {
            if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                Log.e(TAG,"Permission: "+permissions[0]+ "was "+ grantResults[0]);
            }
            else {
                Toast.makeText(mContext, "Not all the permission was granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private FirebaseMethods mFirebaseMethods;

    // widgets
    private EditText mEditTextEmojicon;
    private ImageView mSmileIcon, mKeyboardIcon, mBackArrow;
    private ImageView mSelectedImage;
    private VideoView mVideoView;
    private ImageView mShare;
    private ProgressBar mProgressBar;

    // vars
    private int mImageCount = 0;
    private int mVideoCount = 0;
    public static final int GALLERY_PICK = 2;
    public static final int REQUEST_TAKE_GALLERY_VIDEO = 5;
    private Uri mSelectedImageUri = null, mSelectedVideoUri = null;
    private ProgressDialog mProgressDialog;

    private boolean isImage;

    // permissions
    String[] PERMISSIONS = new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_story);

        instantiateFirebase();

        mSelectedImage = findViewById(R.id.story_image_share_story_activity);
        mShare = findViewById(R.id.checkmark_story);
        mVideoView = findViewById(R.id.videoView_share_story_activity);
        mEditTextEmojicon = findViewById(R.id.edit_txt_emojicon);
        mSmileIcon = findViewById(R.id.smile_icon);
        mKeyboardIcon = findViewById(R.id.keyboard_icon);
        mBackArrow = findViewById(R.id.backArrow);
        mKeyboardIcon.setVisibility(View.GONE);


        final FirebaseMethods mFirebaseMethods = new FirebaseMethods(this);
        setEmojiconFragment(false);

        onActivityResultCustom(Uri.parse(getSelectedData()));

        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(TAG, "onClick: share button was clicked");
                if(mEditTextEmojicon.getText().toString().equals("")) {
                    StyleableToast.makeText(mContext, "Add a caption...", R.style.customToast).show();
                }
                else if(mSelectedImageUri != null) {

                    mProgressDialog = new ProgressDialog(ShareStoryActivity.this);
                    mProgressDialog.setMessage("Processing...");
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.show();

                    final String caption = mEditTextEmojicon.getText().toString();
                    String realPath = ImageManager.getPath(ShareStoryActivity.this, mSelectedImageUri);
                    Bitmap thumb_bitmap = ImageManager.getBitmapAndShrinkBitmap(realPath, 640, 480);
                    final byte[] imageBytes = ImageManager.getBytesFromBitmap(thumb_bitmap, 100);

                    /* This snippet code get's the photo's number on the end of the photo,
                     * which is currently used as the original ID and is passed in order to get the thumbnail of the photo*/

                    Long origId = null;
                    String lastPathSegment = mSelectedImageUri.getLastPathSegment();

                    // if the image is in the recent imagea
                    try {
                        String id = lastPathSegment.split(":")[1];
                        origId = Long.parseLong(id);

                    }
                    // if the image is not in the recent images, means in the gallery
                    catch (Exception e) {
                        Log.e(TAG, "onClick: there is an exception : " + e.getMessage() );

                        origId = Long.parseLong(lastPathSegment);
                    }

                    final Bitmap imageThumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                            getContentResolver(), origId,
                            MediaStore.Images.Thumbnails.MINI_KIND,
                            (BitmapFactory.Options) null );


                    // getting the username and then upload the photo to the database
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                    Query query = reference
                            .child(mContext.getString(R.string.dbname_user_account_settings))
                            .orderByChild(mContext.getString(R.string.field_user_id))
                            .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());

                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            String username = null;
                            String profilePhoto = null;

                            for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()) {

                                username = singleSnapshot.getValue(UserAccountSettings.class).getUnm();
                                profilePhoto = singleSnapshot.getValue(UserAccountSettings.class).getPp();
                            }
                            Log.e(TAG, "onDataChange: the username we are trying to upload is : " + username);
                            Log.e(TAG, "onDataChange: the caption is : " + caption + " ///////////" );

                            mFirebaseMethods.uploadNewPhoto(profilePhoto, username, imageBytes, imageThumbnail, caption,
                                    mImageCount, mProgressDialog, getString(R.string.it_is_a_story)); // this indicates that it is a story that we want to upload
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
                else if(mSelectedVideoUri != null) {

                    final String videoPath = ImageManager.getPath(ShareStoryActivity.this, mSelectedVideoUri);
                    try {
                        if (videoPath != null) {

                            // converting the video size from bytes to kilobytes to megabytes
                            File file = new File(ImageManager.getPath(mContext, mSelectedVideoUri));
                            double size = ((file.length() / 1024) / 1024);
                            Log.e(TAG, "onActivityResult: the size of the video is : " + size);

                            /* If the video file is bigger than 5 MB*/
                            if(size > 5.0){
                                Log.e(TAG, "onClick: the video is large than 5 MB");

                                CharSequence[] options = new CharSequence[] {"Click here to choose another one"};
                                AlertDialog.Builder builder = new AlertDialog.Builder(ShareStoryActivity.this);
                                builder.setTitle("The video has " + size + " MB, Post only videos of 5MB or less");
                                builder.setCancelable(false);

                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        if(which == 0) {
                                            // choose videos
                                            Intent intent = new Intent();
                                            intent.setType("video/*");
                                            intent.setAction(Intent.ACTION_GET_CONTENT);
                                            startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);
                                        }
                                    }
                                });
                                builder.show();
                            }

                            // if the video file is less than 5 MB
                            else{
                                Log.e(TAG, "onClick: the video is less 5 MB");
                                Log.e(TAG, "onActivityResult: the real video path is : " + videoPath );

                                mProgressDialog = new ProgressDialog(ShareStoryActivity.this);
                                mProgressDialog.setMessage("Processing...");
                                mProgressDialog.setCancelable(false);
                                mProgressDialog.show();
                                final String caption = mEditTextEmojicon.getText().toString();

                                final Bitmap videoThumbnail = ThumbnailUtils.createVideoThumbnail(ImageManager.getPath(ShareStoryActivity.this,
                                        mSelectedVideoUri), MediaStore.Images.Thumbnails.MINI_KIND);

                                // getting the username and then upload the video to the database
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                                Query query = reference
                                        .child(mContext.getString(R.string.dbname_user_account_settings))
                                        .orderByChild(mContext.getString(R.string.field_user_id))
                                        .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());

                                query.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        String username = null;
                                        String profilePhoto = null;
                                        for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()) {

                                            username = singleSnapshot.getValue(UserAccountSettings.class).getUnm();
                                            profilePhoto = singleSnapshot.getValue(UserAccountSettings.class).getPp();
                                        }
                                        Log.e(TAG, "onDataChange: the username we are trying to upload is : " + username);
                                        Log.e(TAG, "onDataChange: the video path is : " + videoPath + " ///////////////");
                                        mFirebaseMethods.uploadNewVideo(profilePhoto, username, mSelectedVideoUri, caption, mProgressDialog,
                                                videoThumbnail, mVideoCount, videoPath, getString(R.string.it_is_a_story));

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });


                            }
                        }
                        // the video file is corrupted
                    } catch (Exception e) {
                        Log.e(TAG, "onClick: there is an exception : " + e.getMessage() );
                        CharSequence[] options = new CharSequence[] {"Click here to choose another one"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(ShareStoryActivity.this);
                        builder.setTitle("The video file is corrupted");
                        builder.setCancelable(false);

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if(which == 0) {
                                    // choose videos
                                    Intent intent = new Intent();
                                    intent.setType("video/*");
                                    intent.setAction(Intent.ACTION_GET_CONTENT);
                                    startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);
                                }
                            }
                        });
                        builder.show();
                    }
                }
                else {
                    Toast.makeText(mContext, "Choose a photo or a video first", Toast.LENGTH_SHORT).show();
                }

            }
        });

        // setting up the emoji and the actions which happen when you click on it.
        mSmileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(TAG, "onClick: clicking the smile icon" );
                RelativeLayout relativeLayout = findViewById(R.id.majorLayout);
                LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        40.0f
                );
                relativeLayout.setLayoutParams(parameter);

                FrameLayout emojiconFragment = findViewById(R.id.emojicons_frame_layout);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        60.0f
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


                Log.e(TAG, "onClick: clicking the smile icon" );
                RelativeLayout relativeLayout = findViewById(R.id.majorLayout);
                LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0.0f
                );
                relativeLayout.setLayoutParams(parameter);

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

                    Log.e(TAG, "onClick: clicking the smile icon" );
                    RelativeLayout relativeLayout = findViewById(R.id.majorLayout);
                    LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            0.0f
                    );
                    relativeLayout.setLayoutParams(parameter);

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
            }
        });

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: back arrow is clicked" );

                CharSequence[] options = new CharSequence[] {"Photos", "Videos", "Return home"};
                AlertDialog.Builder builder = new AlertDialog.Builder(ShareStoryActivity.this);
                builder.setCancelable(true);

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if(which == 0) {
                            // choose photos
                            checkPermission(); // check permissions before picking images
                            Intent galleryIntent = new Intent();
                            galleryIntent.setType("image/*");
                            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(galleryIntent, "Select images"), GALLERY_PICK);
                        }
                        else if(which == 1) {
                            // choose videos
                            checkPermission();
                            Intent intent = new Intent();
                            intent.setType("video/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);
                        }
                        else if(which == 2) {
                            ShareStoryActivity.this.finish();
                        }
                    }
                });
                builder.show();
            }
        });

    }

    private String getSelectedData() {

        String result = getIntent().getStringExtra(getString(R.string.selected_image));
        isImage = true;

        if(result == null) {

            isImage = false;
            result = getIntent().getStringExtra(getString(R.string.selected_video));
        }
        return result;
    }

    @Override
    public void onBackPressed() {

        if(mKeyboardIcon.getVisibility() == View.VISIBLE) {

            Log.e(TAG, "onClick: clicking the smile icon" );
            RelativeLayout relativeLayout = findViewById(R.id.majorLayout);
            LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0.0f
            );
            relativeLayout.setLayoutParams(parameter);

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
        else {
            CharSequence[] options = new CharSequence[] {"Photos", "Videos", "Return home"};
            AlertDialog.Builder builder = new AlertDialog.Builder(ShareStoryActivity.this);
            builder.setCancelable(true);

            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    if(which == 0) {
                        // choose photos
                        checkPermission(); // check permissions before picking images
                        Intent galleryIntent = new Intent();
                        galleryIntent.setType("image/*");
                        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(galleryIntent, "Select images"), GALLERY_PICK);
                    }
                    else if(which == 1) {
                        // choose videos
                        checkPermission();
                        Intent intent = new Intent();
                        intent.setType("video/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);
                    }
                    else if(which == 2) {
                        ShareStoryActivity.this.finish();
                    }
                }
            });
            builder.show();
        }
    }

    private void onActivityResultCustom(Uri selectedDataUri) {

        if(isImage) {
            mSelectedImage.setVisibility(View.VISIBLE);
            mVideoView.setVisibility(View.GONE);

            mSelectedImageUri = selectedDataUri;
            mSelectedImage.setImageURI(mSelectedImageUri);
        }
        else {

            mSelectedImage.setVisibility(View.GONE);
            mVideoView.setVisibility(View.VISIBLE);

            mSelectedVideoUri = selectedDataUri;

            mVideoView.setVideoURI(mSelectedVideoUri);
            mVideoView.requestFocus();
            mVideoView.setMediaController(new MediaController(this));
            mVideoView.start();

            // checking if the video is not bigger than 5 MB or if it is not corrupted
            String videoPath = ImageManager.getPath(ShareStoryActivity.this, mSelectedVideoUri);
            Log.e(TAG, "onActivityResult: the video path is : " + videoPath );
            try {
                if (videoPath != null) {

//                    MediaPlayer mp = MediaPlayer.create(SharePostActivity.this, Uri.parse(videoPath));
//                    int duration = mp.getDuration();
//                    mp.release();

                    // converting the video size from bytes to kilobytes to megabytes
                    File file = new File(ImageManager.getPath(mContext, mSelectedVideoUri));
                    double size = ((file.length() / 1024) / 1024);
                    Log.e(TAG, "onActivityResult: the size of the video is : " + size);

                    if(size > 5.0){
                        Log.e(TAG, "onClick: the video is bigger than 5 MB ");

                        CharSequence[] options = new CharSequence[] {"Click here to choose another one"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(ShareStoryActivity.this);
                        builder.setTitle("The video has " + size + " MB, Post only videos of 5MB or less");
                        builder.setCancelable(true);

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if(which == 0) {
                                    // choose videos
                                    Intent intent = new Intent();
                                    intent.setType("video/*");
                                    intent.setAction(Intent.ACTION_GET_CONTENT);
                                    startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);
                                }
                            }
                        });
                        builder.show();
                    }
                }
                // if the video file is corrupted
            } catch (Exception e) {
                Log.e(TAG, "onClick: there is an exception : " + e.getMessage() );
                CharSequence[] options = new CharSequence[] {"Click here to choose another one"};
                AlertDialog.Builder builder = new AlertDialog.Builder(ShareStoryActivity.this);
                builder.setTitle("The video file is corrupted");
                builder.setCancelable(false);

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if(which == 0) {
                            // choose videos
                            Intent intent = new Intent();
                            intent.setType("video/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);
                        }
                    }
                });
                builder.show();
            }

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mSelectedImageUri = null;
        mSelectedVideoUri = null;

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {

            mSelectedImage.setVisibility(View.VISIBLE);
            mVideoView.setVisibility(View.GONE);

            mSelectedImageUri = data.getData();
            mSelectedImage.setImageURI(mSelectedImageUri);
        }
        else if(requestCode == REQUEST_TAKE_GALLERY_VIDEO && resultCode == RESULT_OK && data != null && data.getData() != null) {

            mSelectedImage.setVisibility(View.GONE);
            mVideoView.setVisibility(View.VISIBLE);

            mSelectedVideoUri = data.getData();

            mVideoView.setVideoURI(mSelectedVideoUri);
            mVideoView.requestFocus();
            mVideoView.setMediaController(new MediaController(this));
            mVideoView.start();

            // checking if the video is not bigger than 5 MB or if it is not corrupted
            String videoPath = ImageManager.getPath(ShareStoryActivity.this, mSelectedVideoUri);
            Log.e(TAG, "onActivityResult: the video path is : " + videoPath );
            try {
                if (videoPath != null) {

//                    MediaPlayer mp = MediaPlayer.create(SharePostActivity.this, Uri.parse(videoPath));
//                    int duration = mp.getDuration();
//                    mp.release();

                    // converting the video size from bytes to kilobytes to megabytes
                    File file = new File(ImageManager.getPath(mContext, mSelectedVideoUri));
                    double size = ((file.length() / 1024) / 1024);
                    Log.e(TAG, "onActivityResult: the size of the video is : " + size);

                    if(size > 5.0){
                        Log.e(TAG, "onClick: the video is bigger than 5 MB ");

                        CharSequence[] options = new CharSequence[] {"Click here to choose another one"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(ShareStoryActivity.this);
                        builder.setTitle("The video has " + size + " MB, Post only videos of 5MB or less");
                        builder.setCancelable(true);

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if(which == 0) {
                                    // choose videos
                                    Intent intent = new Intent();
                                    intent.setType("video/*");
                                    intent.setAction(Intent.ACTION_GET_CONTENT);
                                    startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);
                                }
                            }
                        });
                        builder.show();
                    }
                }
                // if the video file is corrupted
            } catch (Exception e) {
                Log.e(TAG, "onClick: there is an exception : " + e.getMessage() );
                CharSequence[] options = new CharSequence[] {"Click here to choose another one"};
                AlertDialog.Builder builder = new AlertDialog.Builder(ShareStoryActivity.this);
                builder.setTitle("The video file is corrupted");
                builder.setCancelable(false);

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if(which == 0) {
                            // choose videos
                            Intent intent = new Intent();
                            intent.setType("video/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);
                        }
                    }
                });
                builder.show();
            }

        }
    }

    private void checkPermission() {

        if (!hasPermissions(mContext, PERMISSIONS)) {
            ActivityCompat.requestPermissions((Activity) mContext, PERMISSIONS, 11);
            return;
        }
    }

    private boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    public String bitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp=Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    public Bitmap stringToBitMap(String encodedString){
        try {
            byte [] encodeByte=Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap=BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }

    public byte[] convertFileToBytes(String path) throws IOException {

        FileInputStream fis = new FileInputStream(path);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[1024];

        for (int readNum; (readNum = fis.read(b)) != -1;) {
            bos.write(b, 0, readNum);
        }

        byte[] bytes = bos.toByteArray();

        return bytes;
    }

    private void instantiateFirebase() {

        mContext = ShareStoryActivity.this;
        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseMethods = new FirebaseMethods(mContext);

        mRootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                mImageCount = mFirebaseMethods.getPhotoStoriesCount(dataSnapshot);
                Log.e(TAG, "onDataChange: the number of images is : " + mImageCount );

                mVideoCount = mFirebaseMethods.getVideoStoriesCount(dataSnapshot);
                Log.e(TAG, "onDataChange: the number of videos is : " + mVideoCount );

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
