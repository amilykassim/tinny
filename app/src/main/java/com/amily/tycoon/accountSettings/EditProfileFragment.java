package com.amily.tycoon.accountSettings;


import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
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
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.amily.tycoon.models.Story;
import com.amily.tycoon.models.User;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.models.UserSettings;
import com.amily.tycoon.share.SharePostActivity;
import com.amily.tycoon.utils.FirebaseMethods;
import com.amily.tycoon.utils.ImageManager;
import com.amily.tycoon.utils.PostActivity;
import com.amily.tycoon.utils.StoryProfileAdapter;
import com.amily.tycoon.utils.StringManipulation;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.CONNECTIVITY_SERVICE;

public class EditProfileFragment extends Fragment {


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

    // permissions
    private String[] PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

    private static final String TAG = "EditProfileFragment";
    private Context mContext;

    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private FirebaseMethods mFirebaseMethods;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;
    private String userID;

    // edit profile fragment widgets
    private EditText mDisplayName, mUsername, mDescription, mCoursesTaken;
    private Button mChangeProfilePhoto;
    private ProgressBar mProgressBar;
    private CircleImageView profileImage;
    private ImageView mSelectedImage, mBackArrow, mSelectedProfileImageDialog;
    private Dialog mDialog, mDialogProfileImage;
    private Button mSave, myPosts;
    private RecyclerView mStoryRecyclerView;
    private ArrayList<Story> mStories;

    // variable sections
    private UserSettings mUserSettings;
    private static final int GALLERY_PICK = 1;

    private Uri imageUri;

    public EditProfileFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_edit_profile, container, false);

        mContext = getActivity();

        mDialog = new Dialog(getActivity());
        mDialog.setContentView(R.layout.layout_upload_profile_photo_dialog);
        mSelectedImage = mDialog.findViewById(R.id.selectedImage);


        // dialog to show the profile image
        mDialogProfileImage = new Dialog(getActivity());
        mDialogProfileImage.setContentView(R.layout.layout_view_profile_photo_dialog);
        mSelectedProfileImageDialog = mDialogProfileImage.findViewById(R.id.selectedImage);


        // open the keyboard as the fragment start, in order to show the stories
        InputMethodManager imm = (InputMethodManager)   getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);


        mUsername = view.findViewById(R.id.username_edit_profile);
        mDisplayName = view.findViewById(R.id.display_name_edit_profile);
        mDescription = view.findViewById(R.id.description_edit_profile);
        mCoursesTaken = view.findViewById(R.id.courses_taken);
        mBackArrow = view.findViewById(R.id.backArrow);
        mSave = view.findViewById(R.id.save_btn);
        myPosts = view.findViewById(R.id.my_posts_btn);
        profileImage = view.findViewById(R.id.profileImage);
        mProgressBar = view.findViewById(R.id.progressbar);
        mChangeProfilePhoto = view.findViewById(R.id.changeProfilePhoto_editProfile);

        mStoryRecyclerView = view.findViewById(R.id.recyclerView_stories);
        mStoryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayout.HORIZONTAL, false));


        mStories = new ArrayList<>();
        instantiateFirebase();
        retrieveUserInformation();

        initOnClickListeners();
        initStoryRecyclerView();



        return view;
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

    private void initStoryRecyclerView() {

        // fetching stories
//        final ArrayList<Story> stories = new ArrayList<>();
        Query queryStories = mRootRef
                .child(getString(R.string.dbname_user_stories))
                .child(mAuth.getCurrentUser().getUid());

        // getting cutoff in order to check for daily stories
        final long cutoff = new Date().getTime() - TimeUnit.MILLISECONDS.convert(8760, TimeUnit.HOURS);

        queryStories.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                    Story story = new Story();
                    // we casted the singlesnapshot in the hashmap
                    Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                    try {
                        story.setUnm(objectMap.get(getString(R.string.field_username)).toString());
                        story.setC(objectMap.get(getString(R.string.field_caption)).toString());
                        story.setTm(Long.parseLong(objectMap.get(getString(R.string.field_time_created)).toString()));
                        story.setPid(objectMap.get(getString(R.string.field_photo_id)).toString());
                        story.setUid(objectMap.get(getString(R.string.field_user_id)).toString());
                        story.setUrl(objectMap.get(getString(R.string.field_image_path)).toString());
                        story.setTh(objectMap.get(getString(R.string.field_image_thumbnail)).toString());
                        story.setvUrl(objectMap.get(getString(R.string.field_video_path)).toString());
                        story.setvTh(objectMap.get(getString(R.string.field_video_thumbnail)).toString());
                        story.setVs(objectMap.get(getString(R.string.field_video_views)).toString());


                        // the stories's date created is greater than 23 hours ago
                        if(story.getTm() < cutoff) {

                            // removing the data from firebase storage
                            // it's a story photo that we are deleting
                            if(!story.getUrl().equals("")) {
                                mFirebaseMethods.deleteStoryFromStorage(mFirebaseStorage.getReferenceFromUrl(story.getUrl()), null);

                            }

                            // it's a story video that we are deleting
                            else {
                                mFirebaseMethods.deleteStoryFromStorage(mFirebaseStorage.getReferenceFromUrl(story.getvUrl()), null);

                            }

                            mRootRef.child(getString(R.string.dbname_user_stories))
                                    .child(story.getUid())
                                    .child(story.getPid())
                                    .removeValue(); // removing the story

                        }

                        // the stories's date created is less than 23 hours ago
                        else {
                            mStories.add(story);

                        }

                    }catch (NullPointerException e) {
                        Log.e(TAG, "onDataChange: NullPointerException : " + e.getMessage());
                    }
                }

                Log.e(TAG, "onDataChange: the size of stories is : " + mStories.size() + " ///////////////////");

                try {
                    Collections.sort(mStories, new Comparator<Story>() {
                        @Override
                        public int compare(Story o1, Story o2) {
                            return String.valueOf(o2.getTm()).compareTo(String.valueOf(o1.getTm()));
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "onDataChange: There is an exception " + e.getMessage());
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



        try {

            StoryProfileAdapter adapter = new StoryProfileAdapter(getActivity(), R.layout.story_image_item,
                    mStories, mFirebaseMethods, mFirebaseStorage);
            mStoryRecyclerView.setAdapter(adapter);

        }catch (Exception e) {
            Log.e(TAG, "onDataChange: the exception in pprfile activity" +
                    "on the big problem is : " + e.getMessage());
        }
    }


    private void initOnClickListeners() {

        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileSettings();
            }
        });

        myPosts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(checkInternetConnection()) {
                    Intent myPostIntent = new Intent(getActivity(), PostActivity.class);
                    myPostIntent.putExtra(getString(R.string.my_user_id), FirebaseAuth.getInstance().getCurrentUser().getUid());
                    startActivity(myPostIntent);
                }
            }
        });

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });


        mChangeProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(checkInternetConnection()) {
                    checkPermission();
                    Intent galleryIntent = new Intent();
                    galleryIntent.setType("image/*");
                    galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(galleryIntent, "Select images"), GALLERY_PICK);
                }

            }
        });
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView cancel;
                cancel = mDialogProfileImage.findViewById(R.id.txtclose);

                if(checkInternetConnection()) {
                    Glide.with(mContext)
                            .load(mUserSettings.getUserAccountSettings().getPp())
                            .into(mSelectedProfileImageDialog);

                    mDialogProfileImage.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    mDialogProfileImage.show();

                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.e(TAG, "onClick: clicked on cancel to cancel the operation of uploading a new profile photot");
                            mDialogProfileImage.dismiss();
                        }
                    });
                }
            }
        });
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


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Log.e(TAG, "onActivityResult: the image uri retrieved is : " + imageUri + " /////////////");
            mSelectedImage.setImageURI(imageUri);
            popUpDialog();

        }
    }

    private void popUpDialog() {
        Log.e(TAG, "popUpDialog: entered in the pop up dialog");
        TextView txtclose;
        Button cancelBtn, sendBtn;

        txtclose = mDialog.findViewById(R.id.txtclose);
        cancelBtn = mDialog.findViewById(R.id.cancel_btn_dialog);
        sendBtn = mDialog.findViewById(R.id.send_offer_btn_dialog);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: clicked on cancel to cancel the operation of uploading a new profile photot");
                mDialog.dismiss();
            }
        });
        txtclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: clicked on close, to choose another photo");
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "Select images"), GALLERY_PICK);
                mDialog.dismiss();
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: clicked on send button, to upload the profile image");
                sendPhoto();
                mChangeProfilePhoto.setEnabled(false);
                profileImage.setEnabled(false);
                mUsername.setEnabled(false);
                mDisplayName.setEnabled(false);
                mDescription.setEnabled(false);
                mCoursesTaken.setEnabled(false);
                mDialog.dismiss();
            }
        });

        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.show();
    }

    private void sendPhoto() {

        String realPath = ImageManager.getPath(getActivity(), imageUri);
        Bitmap thumb_bitmap = ImageManager.getBitmapAndShrinkBitmap(realPath, 640, 480);
        final byte[] imageBytes = ImageManager.getBytesFromBitmap(thumb_bitmap, 100);

        mProgressBar.setVisibility(View.VISIBLE);
        mFirebaseMethods.uploadProfilePhoto(imageBytes, mProgressBar, mChangeProfilePhoto,
                profileImage, mUsername, mDisplayName, mDescription, mCoursesTaken, getString(R.string.edit_profile_fragment));
    }

    private void setupProfileWidgets(final UserSettings settings) {

        mUserSettings = settings;

        try {
            // set the profile photo if the image path is valid
            if(!settings.getUserAccountSettings().getPp().equals("")) {
                Glide.with(mContext)
                        .load(settings.getUserAccountSettings().getPp())
                        .into(profileImage);
            }
        } catch (Exception e) {
            Log.e(TAG, "setupProfileWidgets: there is an EXCEPTION : " + e.getMessage());
        }

        // populating the data in the widgets
        mUsername.setText(settings.getUserAccountSettings().getUnm());
        mDisplayName.setText(settings.getUserAccountSettings().getDnm());
        mDescription.setText(settings.getUserAccountSettings().getD());
        mCoursesTaken.setText(settings.getUserAccountSettings().getCt());

        mProgressBar.setVisibility(View.GONE);

    }

    private void saveProfileSettings() {
        final String username = mUsername.getText().toString().toLowerCase();
        final String displayName = mDisplayName.getText().toString().toLowerCase();
        final String description = mDescription.getText().toString();
        final String coursesTaken = mCoursesTaken.getText().toString();


        try {

            // CASE 1: The user changed their username therefore we need to check for uniqueness
            if(!mUserSettings.getUser().getUnm().equals(username)) {
                checkIfUsernameExists(username);
            }
            /**
             * Changing the rest of the settings that do not require uniqueness
             */
            // update display name
            if(!mUserSettings.getUserAccountSettings().getDnm().equals(displayName)) {
                mFirebaseMethods.updateUserAccountSettings(displayName, null, null);
            }
            // update description
            if(!mUserSettings.getUserAccountSettings().getD().equals(description)) {
                mFirebaseMethods.updateUserAccountSettings(null, description, null);
            }
            // update courses taken
            if(!mUserSettings.getUserAccountSettings().getCt().equals(coursesTaken)) {
                mFirebaseMethods.updateUserAccountSettings(null, null, coursesTaken);
            }


        }catch (Exception e) {
            Log.e(TAG, "saveProfileSettings: THERE IS AN EXCEPTION : " + e.getMessage());
        }

    }


    private void checkIfUsernameExists(final String username) {
        Log.e(TAG, "checkIfUsernameExists: checking if " + username + " already exists" );

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_users))
                .orderByChild(getString(R.string.field_username))
                .equalTo(StringManipulation.condenseUsername(username));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    // add the username
                    mFirebaseMethods.updateUsername(username);
                    Toast.makeText(getActivity(), "Saved username", Toast.LENGTH_SHORT).show();
                }
                for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()) {
                    if(singleSnapshot.exists()) {
                        Log.e(TAG, "onDataChange: FOUND A MATCH " + singleSnapshot.getValue(User.class).getUnm());
                        Toast.makeText(getActivity(), "That usename already exists", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void retrieveUserInformation() {

        mProgressBar.setVisibility(View.VISIBLE);
        mRootRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e(TAG, "onDataChange: retrieve user information from the database" );

                Log.e(TAG, "onDataChange: datasnapshot exists" );
                setupProfileWidgets(mFirebaseMethods.getUserSettings(dataSnapshot));

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void instantiateFirebase() {

        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mStorageReference = FirebaseStorage.getInstance().getReference();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseMethods = new FirebaseMethods(mContext);
    }
}
