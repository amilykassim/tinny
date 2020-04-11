package com.amily.tycoon.home;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amily.tycoon.R;
import com.amily.tycoon.models.Comment;
import com.amily.tycoon.models.Post;
import com.amily.tycoon.share.SharePostActivity;
import com.amily.tycoon.utils.FirebaseMethods;
import com.amily.tycoon.utils.PostsAdapter;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;
//import com.google.android.gms.ads.InterstitialAd;
//import com.google.android.gms.ads.MobileAds;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private FirebaseMethods mFirebaseMethods;

    // widgets
    private ProgressBar mProgressBar;
    private FloatingActionButton mShare;
    private Dialog mDialog;
    private ImageView mSelectedImage;
    private TextView mInternetConnectionTextView, mRefreshTextview;

    // vars
    public static final int GALLERY_PICK = 2;
    private ArrayList<Post> mPosts;
    private ArrayList<Post> mPaginatedPosts;
    private ArrayList<String> mAllUsers;
    private PostsAdapter mAdapter;
    private int mResultsCount;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;

    // mobile ads
//    private AdView mAdView;

    public HomeFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);


        // MOBILE BANNER ADS ///////
//        MobileAds.initialize(getActivity(), "ca-app-pub-6617145085687341~4894997018"); // here is your real app id
//        mAdView = view.findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);

//        MobileAds.initialize(getActivity(), "ca-app-pub-3940256099942544~3347511713"); // here is your test app id
//        mAdView = view.findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);






        mDialog = new Dialog(getActivity());
        mDialog.setContentView(R.layout.layout_view_profile_photo_dialog);
        mSelectedImage = mDialog.findViewById(R.id.selectedImage);


        mShare = view.findViewById(R.id.share_home_btn);
        mInternetConnectionTextView = view.findViewById(R.id.check_your_internet_textview);
        mRefreshTextview = view.findViewById(R.id.refresh_textview);
        final CoordinatorLayout coordinatorLayout = view.findViewById(R.id.coordinatorLayout);
        mAuth = FirebaseAuth.getInstance();

        mRecyclerView = view.findViewById(R.id.home_fragment_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAllUsers = new ArrayList<>();
        mPosts = new ArrayList<>();

        mRefreshTextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTheFragment();
            }
        });

        // start the fragment and load the data if there is connection
        startTheFragment();

        return view;
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
                mInternetConnectionTextView.setVisibility(View.VISIBLE);
                mRefreshTextview.setVisibility(View.VISIBLE);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private void startTheFragment() {

        if(checkInternetConnection()) {
            mInternetConnectionTextView.setVisibility(View.GONE);
            mRefreshTextview.setVisibility(View.GONE);
            instantiateFirebase();
            initOnClickListeners();
            getAllUsers();
            popUpDialog(); // pop up dialog when viewing profile photo
        }
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
    }

    private void initOnClickListeners() {

        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SharePostActivity.class));
                getActivity().finish();
            }
        });
    }
    private void getAllUsers() {
        Log.e(TAG, "getFollowing: searching for following " );

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_users));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                    mAllUsers.add(singleSnapshot.child(getString(R.string.field_user_id)).getValue().toString());
                }

                // get Photos
                getPosts();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    private void getPosts() {
        Log.e(TAG, "getPhotos: entered in get photos method");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        for(int i = 0; i < mAllUsers.size(); i++) {
            final int count = i;

            Query query = reference
                    .child(getString(R.string.dbname_user_posts))
                    .child(mAllUsers.get(i))
                    .orderByChild(getString(R.string.field_user_id))
                    .equalTo(mAllUsers.get(i));


            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                        Log.e(TAG, "onDataChange: found user : " +
                                singleSnapshot.child(getString(R.string.field_user_id)).getValue());


                        Post post = new Post();
                        // we casted the singlesnapshot in the hashmap
                        Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                        post.setT(objectMap.get(getString(R.string.field_title)).toString());
                        post.setD(objectMap.get(getString(R.string.field_description)).toString());
                        post.setTm(Long.parseLong(objectMap.get(getString(R.string.field_time_created)).toString()));
                        post.setPid(objectMap.get(getString(R.string.field_photo_id)).toString());
                        post.setUid(objectMap.get(getString(R.string.field_user_id)).toString());
                        post.setV(objectMap.get(getString(R.string.field_post_views)).toString());

                        // setting up the comments
                        List<Comment> mComments = new ArrayList<>();
                        for(DataSnapshot commentSnapshot : singleSnapshot.child(getString(R.string.field_comments)).getChildren()) {
                            Comment comment = new Comment();
                            comment.setUid(commentSnapshot.getValue(Comment.class).getUid());
                            comment.setC(commentSnapshot.getValue(Comment.class).getC());
                            comment.setTm(commentSnapshot.getValue(Comment.class).getTm());
                            mComments.add(comment);
                            Log.e(TAG, "onDataChange: The loop of comments is working : "
                                    + comment.getC());
                        }
                        post.setC(mComments);

                        // We don't set the likes here bcz in the adapter we have method that set the likes
                        mPosts.add(post);

                    }

                    if(count >= mAllUsers.size() - 1) {
                        // display our photos
                        displayPosts();

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void displayPosts() {
        mPaginatedPosts = new ArrayList<>();

        if(mPosts != null) {
            try {
                Collections.sort(mPosts, new Comparator<Post>() {
                    @Override
                    public int compare(Post o1, Post o2) {
                        return String.valueOf(o2.getTm()).compareTo(String.valueOf(o1.getTm()));
                    }
                });

                int iteration = mPosts.size();

                if(iteration > 10) {
                    iteration = 10;
                }

                mResultsCount = iteration; // this will hold the number of photos that is added to mPaginated photos
                for(int i = 0; i < iteration; i++) {
                    mPaginatedPosts.add(mPosts.get(i));
                }

                mAdapter = new PostsAdapter(getActivity(), R.layout.layout_post_item,
                        mPaginatedPosts, getString(R.string.home_fragment), mSelectedImage, mDialog);
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            }catch (NullPointerException e) {
                Log.e(TAG, "displayPhotos: NullPointerException : " + e.getMessage() );

            }catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "displayPhotos: IndexOutOfBoundsException : " + e.getMessage() );
            }

        }
    }

    public void displayMorePosts() {
        Log.e(TAG, "loadMorePhotos: loadind more photos" );

        try {

            if(mPosts.size() > mResultsCount && mPosts.size() > 0) {
                int iterations;
                if(mPosts.size() > (mResultsCount + 10)) {
                    Log.e(TAG, "loadMorePhotos: There is more than 10 more photos");
                    iterations = 10;
                }
                else {
                    Log.e(TAG, "loadMorePhotos: There is less than 10 more photos" );
                    iterations = mPosts.size() - mResultsCount;
                }

                for(int i = mResultsCount; i < mResultsCount + iterations; i++) {
                    mPaginatedPosts.add(mPosts.get(i));
                }
                mResultsCount = mResultsCount + iterations; // update the mResultsCount
                mRecyclerView.setAdapter(mAdapter);
//                mAdapter.notifyDataSetChanged();
            }

        }catch (NullPointerException e) {
            Log.e(TAG, "displayPhotos: NullPointerException : " + e.getMessage() );

        }catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "displayPhotos: IndexOutOfBoundsException : " + e.getMessage() );
        }
    }

    private void instantiateFirebase() {

        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseMethods = new FirebaseMethods(getActivity());
    }

}
