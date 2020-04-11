package com.amily.tycoon.utils;


import android.app.Dialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class ViewAllPersonalPostsFragment extends Fragment {

    private static final String TAG = "ViewAllPersonalPosts";

    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private FirebaseMethods mFirebaseMethods;

    // widgets
    private ProgressBar mProgressBar;
    private TextView mResultsNotFound;
    private ImageView mBackArrow;
    private Dialog mDialog;
    private ImageView mSelectedImage;


    // vars
    public static final int GALLERY_PICK = 2;
    private ArrayList<Post> mPosts;
    private ArrayList<Post> mPaginatedPosts;
    private ArrayList<String> mAllUsers;
    private PostsAdapter mAdapter;
    private int mResultsCount;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;

    public ViewAllPersonalPostsFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_all_personal_posts, container, false);

        mDialog = new Dialog(getActivity());
        mDialog.setContentView(R.layout.layout_view_profile_photo_dialog);
        mSelectedImage = mDialog.findViewById(R.id.selectedImage);

        mAuth = FirebaseAuth.getInstance();
        mBackArrow = view.findViewById(R.id.backArrow);
        mProgressBar = view.findViewById(R.id.progressbar);
        mResultsNotFound = view.findViewById(R.id.no_result_found_textview);
        mRecyclerView = view.findViewById(R.id.personal_posts_recyclerview);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAllUsers = new ArrayList<>();
        mPosts = new ArrayList<>();

        // checking for the internet connection
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo != null && networkInfo.isConnected()) {
            }
            else {
                StyleableToast.makeText(getActivity(), "Check your internet connection", R.style.customToast).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        instantiateFirebase();
        getPosts();
        popUpDialog();

        // on back pressed
        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        return view;
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

    private void getPosts() {
        Log.e(TAG, "getPhotos: entered in get photos method");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = null;

        // display your own posts
        if(getUserIdFromBundle() != null) {
            query = reference
                    .child(getString(R.string.dbname_user_posts))
                    .child(getUserIdFromBundle());
        }

        // display the post of the person you're stalking
        else if(getPostFromBundle() != null) {
            query = reference
                    .child(getString(R.string.dbname_user_posts))
                    .child(getPostFromBundle().getUid());
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                    // if there is a user id means that the post is valid, this prevents crashes of the app
                    if(singleSnapshot.child(getString(R.string.field_user_id)).getValue() != null) {

                        Post post = new Post();
                        // we casted the singlesnapshot in the hashmap
                        Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                        post.setT(objectMap.get(getString(R.string.field_title)).toString());
                        post.setD(objectMap.get(getString(R.string.field_description)).toString());
                        post.setTm(Long.parseLong(objectMap.get(getString(R.string.field_time_created)).toString()));
                        post.setPid(objectMap.get(getString(R.string.field_photo_id)).toString());
                        post.setUid(objectMap.get(getString(R.string.field_user_id)).toString());

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
                    else {

                        Log.e(TAG, "onDataChange: the key is : " + singleSnapshot.getKey() + " ////");

                        // display your own posts
                        if(getUserIdFromBundle() != null) {
                            mRootRef.child(getString(R.string.dbname_user_posts))
                                    .child(getUserIdFromBundle())
                                    .child(singleSnapshot.getKey())
                                    .removeValue();
                        }

                        // display the post of the person you're stalking
                        else if(getPostFromBundle() != null) {
                            mRootRef.child(getString(R.string.dbname_user_posts))
                                    .child(getPostFromBundle().getUid())
                                    .child(singleSnapshot.getKey())
                                    .removeValue();
                        }

                    }

                }

                // if there is data to show up, then hide the results not found text view, else show it
                if(!dataSnapshot.exists())
                    mResultsNotFound.setVisibility(View.VISIBLE);
                else
                    mResultsNotFound.setVisibility(View.GONE);

                // display posts
                displayPosts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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

                mAdapter = new PostsAdapter(getActivity(), R.layout.layout_post_item, mPaginatedPosts,
                        getString(R.string.view_all_personal_posts_fragment), mSelectedImage, mDialog);
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
        Log.e(TAG, "loadMorePhotos: loading more photos" );

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

    /*Get post from the incoming bundle from post activity*/
    private Post getPostFromBundle() {
        Log.e(TAG, "getPostFromBundle: ");
        Bundle bundle = this.getArguments();
        if(bundle != null) {
            return bundle.getParcelable(getString(R.string.post));
        }
        else {
            return null;
        }
    }

    /*Get user id from the incoming bundle from post activity*/
    private String getUserIdFromBundle() {
        Log.e(TAG, "getPostFromBundle: ");
        Bundle bundle = this.getArguments();
        if(bundle != null) {
            return bundle.getString(getString(R.string.my_user_id));
        }
        else {
            return null;
        }
    }

    private void instantiateFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseMethods = new FirebaseMethods(getActivity());
    }
}
