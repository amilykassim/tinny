package com.amily.tycoon.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.amily.tycoon.accountSettings.AccountSettingsActivity;
import com.amily.tycoon.home.HomeActivity;
import com.amily.tycoon.models.Notification;
import com.amily.tycoon.models.Post;
import com.amily.tycoon.models.RealMessages;
import com.amily.tycoon.models.Story;
import com.amily.tycoon.models.TwoPartyMessageIDs;
import com.amily.tycoon.models.User;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.models.UserSettings;
import com.amily.tycoon.share.SharePostActivity;
import com.amily.tycoon.share.ShareStoryActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.muddzdev.styleabletoastlibrary.StyleableToast;
import com.rockerhieu.emojicon.EmojiconEditText;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class FirebaseMethods {
    private static final String TAG = "FirebaseMethods";

    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private StorageReference mStorageReference;
    private String userID;

    // vars
    private Context mContext;
    private double mPhotoUploadProgress = 0;

    public FirebaseMethods(Context context) {
        Log.e(TAG, "FirebaseMethods: Started gracefully" );
        this.mContext = context;
        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mStorageReference = FirebaseStorage.getInstance().getReference();

        if(mAuth.getCurrentUser() != null) {
            Log.e(TAG, "FirebaseMethods: The current user ID is :  " + mAuth.getCurrentUser().getUid());
            userID = mAuth.getCurrentUser().getUid();
        }
        else {
            Log.e(TAG, "FirebaseMethods: The current user is null");
        }
    }

    public void addNewUser(String username, String email, String userID,
                           String password, String description, String profile_photo, String coursesTaken) {

        // adding new user to the database
        User user = new User(
                StringManipulation.condenseUsername(username),
                email,
                userID,
                password
        );
        mRootRef.child(mContext.getString(R.string.dbname_users))
                .child(userID)
                .setValue(user);


        // adding userAccountSettings to the database
        UserAccountSettings settings = new UserAccountSettings(
                description,
                username,
                StringManipulation.condenseUsername(username),
                profile_photo,
                userID,
                password,
                coursesTaken

        );
        mRootRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(userID)
                .setValue(settings);


        Log.e(TAG, "addNewUser: added the data successully" );
    }

    public UserSettings getUserSettings(DataSnapshot dataSnapshot) {
        Log.e(TAG, "getUserAccountSettings: retrieving the user account settings from the database" );
        UserAccountSettings settings = new UserAccountSettings();
        User user = new User();

        for(DataSnapshot ds: dataSnapshot.getChildren()) {

            // checking for the user account settings
            if(ds.getKey().equals(mContext.getString(R.string.dbname_user_account_settings))) {
//                Log.e(TAG, "getUserAccountSettings: dataSnapshot : " + ds);

                try {

                    settings.setDnm(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getDnm()
                    );
                    settings.setUnm(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getUnm()

                    );
                    settings.setD(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getD()
                    );
                    settings.setPp(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getPp()
                    );
                    settings.setPsw(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getPsw()
                    );
                    settings.setUid(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getUid()
                    );
                    settings.setCt(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getCt()
                    );

//                    Log.e(TAG, "getUserAccountSettings: retrieved user account settings information : " + settings.toString() );

                }catch (NullPointerException e) {
                    Log.e(TAG, "getUserAccountSettings: " + e.getMessage() );
                }

            }

            // checking for user node
            if(ds.getKey().equals(mContext.getString(R.string.dbname_users))) {
//                Log.e(TAG, "getUser settings: dataSnapshot : " + ds);


                try {

                    user.setUnm(
                            ds.child(userID)
                                    .getValue(User.class)
                                    .getUnm()
                    );
                    user.setE(
                            ds.child(userID)
                                    .getValue(User.class)
                                    .getE()
                    );
                    user.setUid(
                            ds.child(userID)
                                    .getValue(User.class)
                                    .getUid()
                    );
                    user.setPsw(
                            ds.child(userID)
                                    .getValue(User.class)
                                    .getPsw()
                    );

//                Log.e(TAG, "getUserAccountSettings: retrieved user account settings information : " + settings.toString() );

                }catch (Exception e) {
                    Log.e(TAG, "getUserSettings: there is an EXCEPTION : " + e.getMessage() );
                }
            }
        }
        return new UserSettings(user, settings);
    }

    public UserAccountSettings getUserAccountSettings(DataSnapshot dataSnapshot) {
        Log.e(TAG, "getUserAccountSettings: retrieving the user account settings from the database" );
        UserAccountSettings settings = new UserAccountSettings();

        for(DataSnapshot ds: dataSnapshot.getChildren()) {

            // checking for the user account settings
            if(ds.getKey().equals(mContext.getString(R.string.dbname_user_account_settings))) {
//                Log.e(TAG, "getUserAccountSettings: dataSnapshot : " + ds);

                try {

                    settings.setDnm(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getDnm()
                    );
                    settings.setUnm(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getUnm()

                    );
                    settings.setD(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getD()
                    );
                    settings.setPp(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getPp()
                    );
                    settings.setPsw(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getPsw()
                    );
                    settings.setUid(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getUid()
                    );
                    settings.setCt(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getCt()
                    );

//                    Log.e(TAG, "getUserAccountSettings: retrieved user account settings information : " + settings.toString() );

                }catch (NullPointerException e) {
                    Log.e(TAG, "getUserAccountSettings: " + e.getMessage() );
                }

            }

        }
        return settings;
    }

    public void updateUserAccountSettings(String displayName, String description, String coursesTaken) {
        Log.e(TAG, "updateUserAccountSettings: updating the user account settings node" );

        if(displayName != null) {
            mRootRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_displayName))
                    .setValue(displayName);
        }
        if(description != null) {
            mRootRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_description))
                    .setValue(description);
        }
        if(coursesTaken != null) {
            mRootRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_courses_taken))
                    .setValue(coursesTaken);
        }
        Toast.makeText(mContext, "Saved", Toast.LENGTH_SHORT).show();
    }

    public void updateUsername(String username) {
        Log.e(TAG, "updateUsername: updating the username to " + username );
        mRootRef.child(mContext.getString(R.string.dbname_users))
                .child(userID)
                .child(mContext.getString(R.string.field_username))
                .setValue(StringManipulation.condenseUsername(username));

        mRootRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(userID)
                .child(mContext.getString(R.string.field_username))
                .setValue(StringManipulation.condenseUsername(username));
    }

    public void addPostToDatabase(String postMessage, String title, final EditText editText,
                                  final EditText titleEditext, final ImageView smileIcon,
                                  final  ImageView keyboardIcon, final TextView sharePost, final ProgressBar progressBar) {

        final String newPhotoKey = mRootRef.child(mContext.getString(R.string.dbname_user_posts)).push().getKey();

        Post post = new Post();

        // it's a photo that we are uploading
        post.setT(title);
        post.setD(postMessage);
        post.setUid(mAuth.getCurrentUser().getUid());
        post.setPid(newPhotoKey);
        post.setV("0");

        // insert the photo into the database
        mRootRef.child(mContext.getString(R.string.dbname_user_posts))
                .child(mAuth.getCurrentUser().getUid())
                .child(newPhotoKey)
                .setValue(post);

//        mRootRef.child(mContext.getString(R.string.dbname_posts))
//                .child(newPhotoKey)
//                .setValue(post);

        // sdding date to database as a Timestamp
//        String datePath_photos = mContext.getString(R.string.dbname_posts) + "/" + newPhotoKey + "/tm"; // tm is time created
        String datePath_userPhotos = mContext.getString(R.string.dbname_user_posts) + "/" + mAuth.getCurrentUser().getUid() + "/" + newPhotoKey + "/tm"; // tm is time created

        Map date = new HashMap();

//        date.put(datePath_photos, ServerValue.TIMESTAMP);
        date.put(datePath_userPhotos, ServerValue.TIMESTAMP);


        mRootRef.updateChildren(date, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError == null) {
                    Log.e(TAG, "onComplete: great upload");

                    StyleableToast.makeText(mContext, "Posted!", R.style.customToast).show();
                    progressBar.setVisibility(View.GONE);
                    mContext.startActivity(new Intent(mContext, HomeActivity.class));
                    ((SharePostActivity)mContext).finish(); // finish the activity after uploading

                } else {
                    progressBar.setVisibility(View.GONE);
                    StyleableToast.makeText(mContext, "Failed to post, try again!", R.style.customToast).show();
                    editText.setEnabled(true);
                    titleEditext.setEnabled(true);
                    smileIcon.setEnabled(true);
                    keyboardIcon.setEnabled(true);
                    sharePost.setEnabled(true);
                }
            }
        });
    }

    public void uploadProfilePhoto(byte[] imageBytes, final ProgressBar progressBar, final Button changeProfilePhoto,
                                   final CircleImageView profileImage, final EditText username,
                                   final EditText displayName, final EditText description, final EditText coursesTaken,
                                   final String editProfile) {

        String user_id = mAuth.getCurrentUser().getUid();
        Log.e(TAG, "uploadProfilePhoto: uploading a PROFILE PHOTO" );

        final StorageReference storageReference = mStorageReference.child("photos/users/" + user_id + "/profile_photo");

        byte[] bytes = imageBytes;

        UploadTask uploadTask = (UploadTask) storageReference.putBytes(bytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.e(TAG, "onSuccess: uploaded the photo successfull" );

                storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if(task.isSuccessful()) {
                            Log.e(TAG, "onComplete: the image url is successfully donwloaded" );
                            String downloadedImageUrl = task.getResult().toString();
                            // set a new profile photo
                            setProfilePhoto(downloadedImageUrl, progressBar,
                                    changeProfilePhoto, profileImage, username, displayName,
                                    description, coursesTaken, editProfile);

                        }
                        else {
                            Log.e(TAG, "onComplete: failed to download the image url " );
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(mContext, "unable to update your profile", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: failed to upload the image" );
                Toast.makeText(mContext, "unable to update your profile, try again", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                if(progress - 15 > mPhotoUploadProgress) {
                    Log.e(TAG, "onProgress: photo upload progress " + String.format("%.0f", progress) + "%" );
                    StyleableToast.makeText(mContext, "photo upload progress : " + String.format("%.0f", progress), R.style.customToast).show();
                    mPhotoUploadProgress = progress;
                }
            }
        });
    }

    public void setProfilePhoto(String imageUrl, ProgressBar progressBar, Button changeProfilePhoto,
                                CircleImageView profileImage, EditText username, EditText displayName,
                                EditText description, EditText coursesTaken, String editProfile) {
        Log.e(TAG, "setProfilePhoto: uploading a new PROFILE PHOTO " + Uri.parse(imageUrl) );

        // sending profile photo
        mRootRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(mAuth.getCurrentUser().getUid())
                .child(mContext.getString(R.string.field_profile_photo))
                .setValue(imageUrl);

        progressBar.setVisibility(View.GONE);

        if(changeProfilePhoto != null && profileImage != null) {
            changeProfilePhoto.setEnabled(true);
            profileImage.setEnabled(true);
            username.setEnabled(true);
            displayName.setEnabled(true);
            description.setEnabled(true);
            coursesTaken.setEnabled(true);
        }

        // this means that we come from register activity, so we need start an intent to home activity and clear the activity task
        if(editProfile == null) {
            Intent intent = new Intent(mContext, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mContext.startActivity(intent);
            StyleableToast.makeText(mContext, "Welcome", R.style.customToast).show();
        }
    }

    public String bitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos = new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b = baos.toByteArray();
        String temp=Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    // THE TIMESTAMP IS REMAINING
    public void setupTwoPartyMessageIDs(final String message, final String chatUserId) {


        if(!TextUtils.isEmpty(message)) {

            //////////////// This is for the person who sent the message ////////////
            TwoPartyMessageIDs twoPartyMessageIDsSender = new TwoPartyMessageIDs();
            twoPartyMessageIDsSender.setSid(mAuth.getCurrentUser().getUid());
            twoPartyMessageIDsSender.setRid(chatUserId);


            //////////////// This is for the person who receive the message ///////////////
            /* I reversed, and put CHAT USER ID in the place of the SENDER ID, in order to enable the person
             who receives the message to fetch the data of the person who sent the message, otherwise he will fetch
              the data as it was him/her who sent the message, that's why exchanged the data*/

            TwoPartyMessageIDs twoPartyMessageIDsReceiver = new TwoPartyMessageIDs();
            twoPartyMessageIDsReceiver.setSid(chatUserId);
            twoPartyMessageIDsReceiver.setRid(mAuth.getCurrentUser().getUid());




            /// the person who sent the message (the current user), the value that was setted is TWO PARTY MESSAGE IDS SENDER
            mRootRef.child(mContext.getString(R.string.dbname_sender_messages_receiver))
                    .child(mAuth.getCurrentUser().getUid())
                    .child(chatUserId)
                    .setValue(twoPartyMessageIDsSender);


            /// the person to receive the message (the receiver), the value that was setted is TWO PARTY MESSAGE IDS RECEIVER
            mRootRef.child(mContext.getString(R.string.dbname_sender_messages_receiver))
                    .child(chatUserId)
                    .child(mAuth.getCurrentUser().getUid())
                    .setValue(twoPartyMessageIDsReceiver);


            String senderTimePath = mContext.getString(R.string.dbname_sender_messages_receiver)
                    + "/" + mAuth.getCurrentUser().getUid()
                    + "/" + chatUserId
                    + "/" + mContext.getString(R.string.field_time_created);

            String receiverTimePath = mContext.getString(R.string.dbname_sender_messages_receiver)
                    + "/" + chatUserId
                    + "/" + mAuth.getCurrentUser().getUid()
                    + "/" + mContext.getString(R.string.field_time_created);

            Map date = new HashMap();
            date.put(senderTimePath, ServerValue.TIMESTAMP);
            date.put(receiverTimePath, ServerValue.TIMESTAMP);

            mRootRef.updateChildren(date, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if (databaseError == null) {
                        Log.e(TAG, "onComplete: great upload");


                    } else {
                        Log.e(TAG, "onComplete: failed to upload the date");
                    }
                }
            });

            sendMessage(message, chatUserId);

        }
    }

    public void sendMessage(String message, String chatUserId) {

        String messageID = mRootRef.push().getKey();

        RealMessages realMessages = new RealMessages();

        realMessages.setM(message);
        realMessages.setF(mAuth.getCurrentUser().getUid());
        realMessages.setT(chatUserId);
        realMessages.setMid(messageID);
        realMessages.setSn("false");

        mRootRef.child(mContext.getString(R.string.dbname_real_messages))
                .child(mAuth.getCurrentUser().getUid())
                .child(chatUserId)
                .child(messageID)
                .setValue(realMessages);

        mRootRef.child(mContext.getString(R.string.dbname_real_messages))
                .child(chatUserId)
                .child(mAuth.getCurrentUser().getUid())
                .child(messageID)
                .setValue(realMessages);


        String senderTimePath = mContext.getString(R.string.dbname_real_messages)
                + "/" + mAuth.getCurrentUser().getUid()
                + "/" + chatUserId
                + "/" + messageID
                + "/" + mContext.getString(R.string.field_time_created);

        String receiverTimePath = mContext.getString(R.string.dbname_real_messages)
                + "/" + chatUserId
                + "/" + mAuth.getCurrentUser().getUid()
                + "/" + messageID
                + "/" + mContext.getString(R.string.field_time_created);

        Map date = new HashMap();
        date.put(senderTimePath, ServerValue.TIMESTAMP);
        date.put(receiverTimePath, ServerValue.TIMESTAMP);

        mRootRef.updateChildren(date, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if(databaseError == null) {
                    Log.e(TAG, "onComplete: great upload" );
                }
                else {
                    Log.e(TAG, "onComplete: failed to upload the date" );
                }
            }
        });

        // sending message notification to database
        sendNewMessageNotification(chatUserId, message);

    }

    public void sendNewMessageNotification(String chatUserId, String message) {

        Log.e(TAG, "addNotificationToDatabase: sending new message notification to database ////////////");
        Notification notification = new Notification();
        String new_notificationKey = mRootRef.child(mContext.getString(R.string.dbname_new_messages_notifications))
                .push().getKey();

        notification.setF(FirebaseAuth.getInstance().getCurrentUser().getUid());
        notification.setNid(new_notificationKey);
        notification.setT(message);
        notification.setUid(chatUserId);

        mRootRef.child(mContext.getString(R.string.dbname_new_messages_notifications))
                .child(chatUserId)
                .child(new_notificationKey)
                .setValue(notification);
    }


    public void uploadNewPhoto(final String profilePhoto, final String username, byte[] imageBytes, final Bitmap imageThumbnail,
                               final String caption, int count, final ProgressDialog progressDialog, final String storyOrPost) {
        Log.e(TAG, "uploadNewPhoto: attempting to upload a photo");

        String user_id = mAuth.getCurrentUser().getUid();

        StorageReference storageReference = null;

        if(storyOrPost.equals(mContext.getString(R.string.it_is_a_post))) {
            Log.e(TAG, "uploadNewPhoto: uploading a NEW PHOTO");
            storageReference = mStorageReference.child("photos/users/" + user_id + "/photo" + (count + 1));
        }
        else {
            storageReference = mStorageReference.child("stories/users/" + user_id + "/" + StringManipulation.condenseUsername(getTimestamp().replace(',', ' ')));
        }

        byte[] bytes = imageBytes;

        final StorageReference finalStorageReference = storageReference;
        UploadTask uploadTask = (UploadTask) storageReference.putBytes(bytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.e(TAG, "onSuccess: uploaded the photo successfull" );

                finalStorageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if(task.isSuccessful()) {
                            Log.e(TAG, "onComplete: the image url is successfully donwloaded" );
                            String downloadedImageUrl = task.getResult().toString();
                            // add photo to the database

                            // it is a story
                            if(storyOrPost.equals(mContext.getString(R.string.it_is_a_story))) {

                                addStoryToDatabase(profilePhoto, username, caption, imageThumbnail, downloadedImageUrl, "",
                                        null, progressDialog, null);
                            }
                        }
                        else {
                            Log.e(TAG, "onComplete: failed to download the image url " );
                            Toast.makeText(mContext, "unable to post the photo, try again", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: failed to upload the image" );
                Toast.makeText(mContext, "unable to post the photo, try again", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                if(progress - 15 > mPhotoUploadProgress) {
                    Log.e(TAG, "onProgress: photo upload progress " + String.format("%.0f", progress) + "%" );
                    StyleableToast.makeText(mContext, "photo upload progress : " + String.format("%.0f", progress), R.style.customToast).show();
                    mPhotoUploadProgress = progress;
                }
            }
        });

    }

    private void addStoryToDatabase(String profilePhoto, String username, String caption, Bitmap imageThumbnail, String imageUrl,
                                    String videoUrl, Bitmap videoThumbnail, ProgressDialog progressDialog, String videoPath) {

        Log.e(TAG, "addStoryToDatabase: adding a story to database");

        String newStoryKey = mRootRef.child(mContext.getString(R.string.dbname_user_stories)).push().getKey();
        Story story = new Story();

        // it's a photo that we are uploading
        if(videoThumbnail == null) {
            story.setUnm(username);
            story.setPp(profilePhoto);
            story.setC(caption);
            story.setUrl(imageUrl);
            story.setTh(bitMapToString(imageThumbnail));
            story.setUid(mAuth.getCurrentUser().getUid());
            story.setPid(newStoryKey);
            story.setvUrl(videoUrl);
            story.setvTh("");
            story.setVs("0");
        }
        // it's a video that we are uploading
        else if(videoThumbnail != null) {
            story.setUnm(username);
            story.setPp(profilePhoto);
            story.setC(caption);
            story.setUrl(imageUrl);
            story.setTh("");
            story.setUid(mAuth.getCurrentUser().getUid());
            story.setPid(newStoryKey);
            story.setvUrl(videoUrl);
            story.setvTh(bitMapToString(videoThumbnail));
            story.setVs("0");

            /* if it is a video that we are uploading we keep it's direct path so that we won't
             * download it again while we have it in our local files*/

            Log.e(TAG, "addPhotoToDatabase: the photo id is : " + newStoryKey + " ////////////////////////////////////");
            SharedPreferences sharedPreferences =
                    mContext.getSharedPreferences(mContext.getString(R.string.PREFS_DOWNLOADING_SYSTEMS), 0);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(newStoryKey, videoPath);
            editor.commit();
        }

        // insert the photo into the database
        mRootRef.child(mContext.getString(R.string.dbname_user_stories))
                .child(mAuth.getCurrentUser().getUid())
                .child(newStoryKey)
                .setValue(story);

        // sdding date to database as a Timestamp
        String datePath_userStories = mContext.getString(R.string.dbname_user_stories) + "/" + mAuth.getCurrentUser().getUid() + "/" + newStoryKey + "/tm";

        Map date = new HashMap();
        date.put(datePath_userStories, ServerValue.TIMESTAMP);


        mRootRef.updateChildren(date, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if(databaseError == null) {
                    Log.e(TAG, "onComplete: great upload" );
                }
                else {
                    Log.e(TAG, "onComplete: failed to upload the date" );
                }
            }
        });
        StyleableToast.makeText(mContext, "Finished", R.style.customToast).show();
        progressDialog.dismiss();

        Intent intent = new Intent(mContext, AccountSettingsActivity.class);
        intent.putExtra(mContext.getString(R.string.edit_profile_header), mContext.getString(R.string.edit_profile_header));
        mContext.startActivity(intent);
        ((ShareStoryActivity)mContext).finish(); // finish the activity after uploading
    }


    public void uploadNewVideo (final String profilePhoto, final String username, Uri fileUri, final String caption, final ProgressDialog progressDialog,
                                final Bitmap thumbnail, int videoCount, final String videoPath, final String storyOrPost) {
        Log.e(TAG, "uploadNewVideo: attempting to upload a new video" );

        String user_id = mAuth.getCurrentUser().getUid();

        StorageReference storageReference = null;
        if(storyOrPost.equals(mContext.getString(R.string.it_is_a_post))) {
            storageReference = mStorageReference.child("videos/users/" + user_id + "/video" + (videoCount + 1));
        }
        else {
            storageReference = mStorageReference.child("videos/users/" + user_id + "/" + StringManipulation.condenseUsername(getTimestamp().replace(',', ' ')));
        }

        final StorageReference finalStorageReference = storageReference;
        storageReference.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()) {
                    Log.e(TAG, "onComplete:  the task is successfull" );
                    finalStorageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if(task.isSuccessful()) {
                                Log.e(TAG, "onComplete: the video url is successfully donwloaded" );
                                String downloadedVideoUrl = task.getResult().toString();
                                // add video to the database


                                // it 's a story
                                if(storyOrPost.equals(mContext.getString(R.string.it_is_a_story))) {

                                    addStoryToDatabase(profilePhoto, username, caption, null,"",
                                            downloadedVideoUrl, thumbnail, progressDialog, videoPath);

                                }
                            }
                            else {
                                Log.e(TAG, "onComplete: failed to download the video url " );
                                Toast.makeText(mContext, "unable to post the video, try again", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }
                    });
                }
                else {
                    Log.e(TAG, "onComplete: the task has failed" );
                }
            }
        });

    }

    private String getTimestamp() {
        return DateFormat.getDateTimeInstance().format(new Date());
    }


    // it is a story photo post
    public int getPhotoStoriesCount(DataSnapshot dataSnapshot) {
        int count = 0;
        Log.e(TAG, "getImageCount: the datasnapshot exists" );
        for(DataSnapshot ds: dataSnapshot.child(mContext.getString(R.string.dbname_user_stories))
                .child(mAuth.getCurrentUser().getUid()).getChildren()) {
            count++;
        }

        return count;
    }

    // it is a story video post
    public int getVideoStoriesCount(DataSnapshot dataSnapshot) {
        int count = 0;
        Log.e(TAG, "getImageCount: the datasnapshot exists" );
        for(DataSnapshot ds: dataSnapshot.child(mContext.getString(R.string.dbname_user_stories))
                .child(mAuth.getCurrentUser().getUid()).getChildren()) {
            count++;
        }

        return count;
    }

    public void deleteStoryFromStorage(StorageReference photoRef, final ProgressDialog progressDialog) {

        Log.e(TAG, "deleteStoryFromDatabase: entered in the delete story method in firebase method");

        photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // File deleted successfully
                Log.e(TAG, "onSuccess: deleted file //////////");

                if(progressDialog != null) {
                    progressDialog.dismiss();
                    StyleableToast.makeText(mContext, "Deleted", R.style.customToast).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
                Log.e(TAG, "onFailure: did not delete file ///////////////");
                if(progressDialog != null) {
                    progressDialog.dismiss();
                    StyleableToast.makeText(mContext, "Oops, something went wrong", R.style.customToast).show();
                }
            }
        });
    }

    public void addViewToDatabase(Story photo, String views) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();

        ref.child(mContext.getString(R.string.dbname_user_stories))
                .child(photo.getUid())
                .child(photo.getPid())
                .child(mContext.getString(R.string.field_video_views))
                .setValue(views);
    }
}

