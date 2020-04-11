package com.amily.tycoon.accountSettings;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.amily.tycoon.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

public class SignOutFragment extends Fragment {

    private static final String TAG = "SignOutFragment";
    private Button mSignOutBtn, mCancelBtn;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseAuth mAuth;

    public SignOutFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_sign_out, container, false);

        mSignOutBtn = view.findViewById(R.id.sign_out_btn);
        mCancelBtn = view.findViewById(R.id.cancel_btn);


        // Setup Firebase Authentication
        setupFirebaseAuth();

        mSignOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClicck : signing out" );
                mAuth.signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });


        return view;
    }

    // ********************************** START OF FIREBASE *******************************************************//

    private void setupFirebaseAuth() {
        Log.e(TAG, "setupFirebaseAuth: started gracefully");

        mAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();

                if(currentUser != null) {
                    Log.e(TAG, "onAuthStateChanged: The user is signed in" + currentUser.getUid());
                }
                else {
                    Log.e(TAG, "onAuthStateChanged: The user is signed out");
                    Log.e(TAG, "onAuthStateChanged: navigating Login activity" );

                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mAuthStateListener != null) {
            mAuth.removeAuthStateListener(mAuthStateListener);
        }
    }
    // ********************************** END OF FIREBASE *******************************************************//


}
