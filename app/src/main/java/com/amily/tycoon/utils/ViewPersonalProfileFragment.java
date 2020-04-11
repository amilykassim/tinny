package com.amily.tycoon.utils;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amily.tycoon.R;
import com.amily.tycoon.models.Post;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.models.UserSettings;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewPersonalProfileFragment extends Fragment {

    private static final String TAG = "ViewOtherProfileFragmen";
    private Context mContext;

    // firebase
    private DatabaseReference mRootRef;

    // edit profile fragment widgets
    private TextView mDisplayName, mUsername, mDescription, mCoursesTaken;
    private ProgressBar mProgressBar;
    private CircleImageView mProfileImage;
    private ImageView mBackArrow, mSelectedImage;
    private Dialog mDialog;

    public ViewPersonalProfileFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_other_profile, container, false);
        mContext = getActivity();

        // dialog for displaying the profile image when it is clicked
        mDialog = new Dialog(getActivity());
        mDialog.setContentView(R.layout.layout_view_profile_photo_dialog);
        mSelectedImage = mDialog.findViewById(R.id.selectedImage);


        mUsername = view.findViewById(R.id.username_edit_profile);
        mDisplayName = view.findViewById(R.id.display_name_edit_profile);
        mDescription = view.findViewById(R.id.description_edit_profile);
        mCoursesTaken = view.findViewById(R.id.courses_taken);
        mProfileImage = view.findViewById(R.id.profileImage);
        mProgressBar = view.findViewById(R.id.progressbar);
        mBackArrow = view.findViewById(R.id.backArrow);

        instantiateFirebase();
        retrieveUserInformation();

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        return view;
    }

    private void retrieveUserInformation() {

        mProgressBar.setVisibility(View.VISIBLE);
        Query query = mRootRef.child(getString(R.string.dbname_user_account_settings))
                .child(getPostFromBundle().getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                Log.e(TAG, "onDataChange: retrieve user information from the database" );
                try {
                    // set the profile photo if the image path is valid
                    if(!dataSnapshot.getValue(UserAccountSettings.class).getPp().equals("")) {
                        Glide.with(mContext)
                                .load(dataSnapshot.getValue(UserAccountSettings.class).getPp())
                                .into(mProfileImage);

                        mProfileImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Glide.with(mContext)
                                        .load(dataSnapshot.getValue(UserAccountSettings.class)
                                        .getPp()).into(mSelectedImage);
                                popUpDialog();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "setupProfileWidgets: there is an EXCEPTION : " + e.getMessage());
                }

                // populating the data in the widgets
                mUsername.setText(dataSnapshot.getValue(UserAccountSettings.class).getUnm());
                mDisplayName.setText(dataSnapshot.getValue(UserAccountSettings.class).getDnm());
                mDescription.setText(dataSnapshot.getValue(UserAccountSettings.class).getD());
                mCoursesTaken.setText(dataSnapshot.getValue(UserAccountSettings.class).getCt());

                if(dataSnapshot.getValue(UserAccountSettings.class).getD().equals("")) {
                    mDescription.setText("No interests yet!");
                    mDescription.setTextSize(16);
                }
                if(dataSnapshot.getValue(UserAccountSettings.class).getCt().equals("")) {
                    mCoursesTaken.setText("No courses yet!");
                    mCoursesTaken.setTextSize(16);
                }

                mProgressBar.setVisibility(View.GONE);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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

    /*Get post from the incoming bundle from Post activity*/
    private Post getPostFromBundle() {
        Log.e(TAG, "getPhotoFromBundle: " );
        Bundle bundle = this.getArguments();
        if(bundle != null) {
            return bundle.getParcelable(getString(R.string.post));
        }
        else {
            return null;
        }
    }

    private void instantiateFirebase() {

        mRootRef = FirebaseDatabase.getInstance().getReference();
    }

}
