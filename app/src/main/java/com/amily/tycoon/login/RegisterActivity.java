package com.amily.tycoon.login;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.amily.tycoon.home.HomeActivity;
import com.amily.tycoon.models.User;
import com.amily.tycoon.utils.FirebaseMethods;
import com.amily.tycoon.utils.ImageManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.io.File;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class RegisterActivity extends AppCompatActivity {


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

    private static final String TAG = "RegisterActivity";
    private Context mContext = RegisterActivity.this;

    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private FirebaseMethods mFirebaseMethods;

    // widgets
    private String append = "";
    private Button mCreateAccountBtn;
    private CircleImageView mProfileImage;
    private EditText mUsername, mEmail, mPassword, mConfirmPassword;
    private ProgressBar mProgressBar;
    private AlertDialog alertDialog;
    private ImageView mSelectedImage;
    private Dialog mDialog;

    // vars
    private Uri mImageUri;
    private static final int GALLERY_PICK = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Log.e(TAG, "onCreate: Register Activity started gracefully" );

        mDialog = new Dialog(RegisterActivity.this);
        mDialog.setContentView(R.layout.layout_upload_profile_photo_dialog);
        mSelectedImage = mDialog.findViewById(R.id.selectedImage);


        instantiateFirebase();
        mCreateAccountBtn = findViewById(R.id.createAccount_btn);
        mProfileImage = findViewById(R.id.profileImage);
        mUsername = findViewById(R.id.username);
        mEmail = findViewById(R.id.email_register_activity);
        mPassword = findViewById(R.id.password_register_activity);
        mConfirmPassword = findViewById(R.id.confirm_password_register_activity);
        mProgressBar = findViewById(R.id.progressbar);
        mProgressBar.setVisibility(View.GONE);

        alertDialog = new AlertDialog.Builder(RegisterActivity.this).create();

        createAccount();

    }

    private void createAccount() {

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "Select images"), GALLERY_PICK);
            }
        });

        mCreateAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: attempting to signing up" );

                if(checkInputs(mEmail.getText().toString(), mUsername.getText().toString().toLowerCase(), mPassword.getText().toString(),
                        mConfirmPassword.getText().toString())) {

                    if(mImageUri != null) {

                        if(mConfirmPassword.getText().toString().equals(mPassword.getText().toString())) {
                            StyleableToast.makeText(mContext, "Please wait...", R.style.customToast).show();
                            mProfileImage.setEnabled(false);
                            mCreateAccountBtn.setEnabled(false);
                            mUsername.setEnabled(false);
                            mEmail.setEnabled(false);
                            mPassword.setEnabled(false);
                            mConfirmPassword.setEnabled(false);
                            mProgressBar.setVisibility(View.VISIBLE);
                            mProgressBar.setIndeterminate(true);

                            // checking internet connection
                            try {
                                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                                if(networkInfo != null && networkInfo.isConnected()) {
                                    checkIfUsernameExists(mUsername.getText().toString().toLowerCase());
                                }
                                else {
                                    mProgressBar.setVisibility(View.GONE);
                                    StyleableToast.makeText(mContext, "Check your internet connection", R.style.customToast).show();
                                    mProfileImage.setEnabled(true);
                                    mCreateAccountBtn.setEnabled(true);
                                    mUsername.setEnabled(true);
                                    mEmail.setEnabled(true);
                                    mPassword.setEnabled(true);
                                    mConfirmPassword.setEnabled(true);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                        else {
                            StyleableToast.makeText(mContext, "Password does not match", R.style.customToast).show();
                        }
                    }else {
                        StyleableToast.makeText(mContext, "Add your profile photo", R.style.customToast).show();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            mImageUri = data.getData();
            Log.e(TAG, "onActivityResult: the image uri retrieved is : " + mImageUri + " /////////////");
            mSelectedImage.setImageURI(mImageUri);
            mProfileImage.setImageURI(mImageUri);
            popUpDialog();

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
                mProfileImage.setImageURI(mImageUri);
                mDialog.dismiss();
            }
        });

        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.show();
    }


    private boolean checkInputs(String email, String username, String password, String confirmPassword) {

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(mContext, "All the text fields should not be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void checkIfUsernameExists(final String username) {
        Log.e(TAG, "checkIfUsernameExists: checking if " + username + " already exists" );

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_user_account_settings))
                .orderByChild(getString(R.string.field_displayName))
                .equalTo(username);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()) {
                    if(singleSnapshot.exists()) {
                        Log.e(TAG, "onDataChange: FOUND A MATCH, The username already exists " + singleSnapshot.getValue(User.class).getUnm());

                        append = mRootRef.push().getKey().substring(3, 10);
                        Log.e(TAG, "onDataChange: in Setup firebase method, The random generated String is " + append);
                    }
                }

                Log.e(TAG, "onDataChange: THERE IS NO MATCH" );
                String muUsernameAppended = "";
                muUsernameAppended = username + append;

                registerNewUser(muUsernameAppended,
                        mEmail.getText().toString().toLowerCase(), mPassword.getText().toString(), "", "");

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public void registerNewUser(final String username, final String email,
                                final String user_password, final String description,
                                final String profile_photo) {

        mAuth.createUserWithEmailAndPassword(email, user_password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Log.e(TAG, "onComplete: entered the register new user method" );
                if(task.isSuccessful()) {
                    Log.e(TAG, "onComplete: signed up successfully" );
                    mFirebaseMethods.addNewUser(username, email, mAuth.getCurrentUser().getUid(), user_password,
                            description, profile_photo, "");


                    // getting the device token
                    String deviceToken = FirebaseInstanceId.getInstance().getToken();

                    mRootRef.child(getString(R.string.dbname_users))
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child(getString(R.string.field_device_token))
                            .setValue(deviceToken).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful()) {

                                try {
                                    sendProfilePhoto();
                                }
                                catch (Exception e) {
                                    Log.e(TAG, "onComplete: " + e.getMessage() );
                                }
                            }
                            else {
                                Toast.makeText(RegisterActivity.this, "There is a problem in regarding log in", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

                }
                else if(task.getException() instanceof FirebaseAuthUserCollisionException){ // If a user is already registered
                    alertDialog.setTitle("Tinny");
                    alertDialog.setMessage("That email is already registered.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();

                }else if(task.getException() instanceof FirebaseAuthWeakPasswordException){ // If the password is weak
                    alertDialog.setTitle("Tinny");
                    alertDialog.setMessage("The password is weak, please consider using a strong password.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }

//                mProgressBar.setVisibility(View.GONE);
//                mCreateAccountBtn.setEnabled(true);
//                mProfileImage.setEnabled(true);
//                mUsername.setEnabled(true);
//                mEmail.setEnabled(true);
//                mPassword.setEnabled(true);
//                mConfirmPassword.setEnabled(true);
            }
        });
    }

    private void sendProfilePhoto() {

        String realPath = ImageManager.getPath(this, mImageUri);
        Bitmap thumb_bitmap = ImageManager.getBitmapAndShrinkBitmap(realPath, 640, 480);
        final byte[] imageBytes = ImageManager.getBytesFromBitmap(thumb_bitmap, 100);

        mProgressBar.setVisibility(View.VISIBLE);
        mFirebaseMethods.uploadProfilePhoto(imageBytes, mProgressBar, null,
                null, null, null,
                null, null,null);
    }


    private void instantiateFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseMethods = new FirebaseMethods(mContext);
    }
}
