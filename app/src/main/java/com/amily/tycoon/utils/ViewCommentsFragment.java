package com.amily.tycoon.utils;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amily.tycoon.home.HomeActivity;
import com.amily.tycoon.models.Comment;
//import com.amily.tycoon.models.Notification;
import com.amily.tycoon.models.Notification;
import com.amily.tycoon.models.Post;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.R;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewCommentsFragment extends Fragment {

    private static final String TAG = "ViewCommentsFragment";

    private Context mContext;

    // firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private DatabaseReference mRootRef;

    // vars
    private Post mPhoto;
    private ArrayList<Comment> mComments;
    private UserAccountSettings mUserAccountSettings;

    // widgets
    private ImageView mBackArrow, mCheckMark, mCommentProfileImage;
    private EditText mComment;
    private RecyclerView mRecyclerView;
    private LinearLayout mLoadingLayout;

    public ViewCommentsFragment() {
        super();
        setArguments(new Bundle());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_view_comments, container, false);

        mContext = getActivity();
        mBackArrow = view.findViewById(R.id.backArrow);
        mCheckMark = view.findViewById(R.id.checkmark_comment);
        mCommentProfileImage = view.findViewById(R.id.comment_profileImage);
        mComment = view.findViewById(R.id.comment_et);
        mRecyclerView = view.findViewById(R.id.recyclerView_comment);
        mLoadingLayout = view.findViewById(R.id.loadingLayout);

        mComments = new ArrayList<>();
        mRootRef = FirebaseDatabase.getInstance().getReference();

        try {

            Log.e(TAG, "onCreateView: entered in the comments Fragment ");
            mPhoto = getPostFromBundle();
            Log.e(TAG, "onCreateView: the photo user id is : " + mPhoto.getUid());
            Log.e(TAG, "onCreateView: the photo id is ; " + mPhoto.getPid());

            retrieveComments();
            setupFirebaseAuth();
            getPhotoDetails();

        }catch (NullPointerException e) {
            Log.e(TAG, "onCreateView: THERE IS AN EXCEPTION : NullPointerException : " + e.getMessage());

        }

        return view;
    }


    private void retrieveComments() {

        Log.e(TAG, "retrieveComments: retrieving comments");
        Query query = mRootRef
                .child(getString(R.string.dbname_user_posts))
                .child(mPhoto.getUid())
                .child(mPhoto.getPid())
                .child(getString(R.string.field_comments));


        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()) {
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                        Log.e(TAG, "onDataChange: entered the loop of comments");
                        Comment comment = new Comment();
                        comment.setUid(singleSnapshot.getValue(Comment.class).getUid());
                        comment.setC(singleSnapshot.getValue(Comment.class).getC());
                        comment.setTm(singleSnapshot.getValue(Comment.class).getTm());
                        mComments.add(comment);
                        Log.e(TAG, "onDataChange: The loop of comments is working : "
                                + comment.getC());
                    }

                    setupWidgets();
                }
                else {
                    setupWidgets();
                    mLoadingLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setupWidgets() {

        Log.e(TAG, "setupWidgets: the array of comments we got : " + mComments );
        CommentListAdapter commentListAdapter = new CommentListAdapter(mContext, R.layout.layout_comment_item,
                mComments,mPhoto, mLoadingLayout);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(commentListAdapter);

        mCheckMark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mComment.getText().toString().equals("")) {

                    Log.e(TAG, "onClick: posting a comment" );
                    String comment = mComment.getText().toString();
                    addNewComment(comment);
                    mComment.setText("");
                    closeKeyBoard();
                    StyleableToast.makeText(mContext, "Posted!", R.style.customToast).show();
                }
                else {
                    Toast.makeText(getActivity(), "The comment field is empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: navigating back to view post fragment" );

                try {
                    getActivity().finish();

                }catch (NullPointerException e) {
                    Log.e(TAG, "onClick: NullPointerException : " + e.getMessage());
                    getActivity().finish();
                }

            }
        });
    }

    // this is the method to close the keyboard, this is used only for fragments, and then for the activity look into into the search activity
    private void closeKeyBoard() {
        View view = getActivity().getCurrentFocus();
        if(view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void addNewComment(String newComment) {
        Log.e(TAG, "addNewComment: adding a new comment");

        String commentID = mRootRef.push().getKey();
        Comment comment = new Comment();
        comment.setCid(commentID);
        comment.setC(newComment);
        comment.setUid(FirebaseAuth.getInstance().getCurrentUser().getUid());

        // insert into photos node
//        mRootRef.child(getString(R.string.dbname_posts))
//                .child(mPhoto.getPid())
//                .child(getString(R.string.field_comments))
//                .child(commentID)
//                .setValue(comment);

        // insert into user photos node
        mRootRef.child(getString(R.string.dbname_user_posts))
                .child(mPhoto.getUid()) // should be mphoto.getUserID ( the id of owner of the photo) and that's what i wrote there
                .child(mPhoto.getPid())
                .child(getString(R.string.field_comments))
                .child(commentID)
                .setValue(comment);

        // adding date to database as a Timestamp
//        String datePath_photos = mContext.getString(R.string.dbname_posts)
//                + "/" + mPhoto.getPid()
//                + "/" + mContext.getString(R.string.field_comments)
//                + "/" + commentID
//                + "/" + mContext.getString(R.string.field_time_created);

        String datePath_userPhotos = mContext.getString(R.string.dbname_user_posts)
                + "/" + mPhoto.getUid()
                + "/" + mPhoto.getPid()
                + "/" + mContext.getString(R.string.field_comments)
                + "/" + commentID
                + "/" + mContext.getString(R.string.field_time_created);

        Map date = new HashMap();
//        date.put(datePath_photos, ServerValue.TIMESTAMP);
        date.put(datePath_userPhotos, ServerValue.TIMESTAMP);

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

        // this is the code to arrange the comments in ascending order in terms of time posted
        if(mComments != null) {
            try {
                Collections.sort(mComments, new Comparator<Comment>() {
                    @Override
                    public int compare(Comment o1, Comment o2) {
                        return String.valueOf(o2.getTm()).compareTo(String.valueOf(o1.getTm()));
                    }
                });

            } catch (NullPointerException e) {
                Log.e(TAG, "displayPhotos: NullPointerException : " + e.getMessage());

            }
        }

        // adding comment notification to database
        addCommentNotificationToDatabase(mPhoto);

    }

    private void addCommentNotificationToDatabase(Post photo) {

        Log.e(TAG, "addNotificationToDatabase: sending notification to database ////////////");
        Notification notification = new Notification();
        String new_notificationKey = mRootRef.child(mContext.getString(R.string.dbname_comment_notifications)).push().getKey();

        notification.setF(FirebaseAuth.getInstance().getCurrentUser().getUid());
        notification.setNid(new_notificationKey);
        notification.setT(mContext.getString(R.string.it_is_a_comment));
        notification.setUid(photo.getUid());

        mRootRef.child(mContext.getString(R.string.dbname_comment_notifications))
                .child(photo.getUid())
                .child(new_notificationKey)
                .setValue(notification);

    }

    /*Get post from the incoming bundle*/
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

    private void getPhotoDetails() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_user_account_settings))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());


        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()) {
                    mUserAccountSettings = singleSnapshot.getValue(UserAccountSettings.class);
                }

                try {
                    Glide.with(mContext)
                            .load(mUserAccountSettings.getPp())
                            .into(mCommentProfileImage);
                }catch (Exception e) {
                    Log.e(TAG, "onDataChange: THERE IS AN EXCEPTION : " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: started gracefully");

        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();

        // listening to the data changes.
        mRootRef.child(mContext.getString(R.string.dbname_user_posts))
                .child(mPhoto.getUid())
                .child(mPhoto.getPid())
                .child(mContext.getString(R.string.field_comments))
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        // getting the comments from a particular photo

                        Query query = mRootRef
                                .child(mContext.getString(R.string.dbname_user_posts))
                                .child(mPhoto.getUid())
                                .orderByChild(mContext.getString(R.string.field_photo_id))
                                .equalTo(mPhoto.getPid());

                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()) {
                                    Log.e(TAG, "onDataChange: entered the loop i just wrote right now" );
                                    Post photo = new Post();
                                    // we casted the singlesnapshot in the hashmap
                                    Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();
                                    photo.setD(objectMap.get(mContext.getString(R.string.field_description)).toString());
                                    photo.setTm(Long.parseLong(objectMap.get(mContext.getString(R.string.field_time_created)).toString()));
                                    photo.setPid(objectMap.get(mContext.getString(R.string.field_photo_id)).toString());
                                    photo.setUid(objectMap.get(mContext.getString(R.string.field_user_id)).toString());

                                    mComments.clear(); // to get a fresh list everytime

                                    for(DataSnapshot commentSnapshot : singleSnapshot.child(mContext.getString(R.string.field_comments)).getChildren()) {
                                        Comment comment = new Comment();
                                        comment.setUid(commentSnapshot.getValue(Comment.class).getUid());
                                        comment.setCid(commentSnapshot.getValue(Comment.class).getCid());
                                        comment.setC(commentSnapshot.getValue(Comment.class).getC());
                                        comment.setTm(commentSnapshot.getValue(Comment.class).getTm());
                                        mComments.add(comment);
                                        Log.e(TAG, "onDataChange: The loop of comments is working : "
                                                + comment.getC());
                                    }

                                    // this is the code to arrange the comments in ascending order in terms of time posted
                                    if(mComments != null) {
                                        try {
                                            Collections.sort(mComments, new Comparator<Comment>() {
                                                @Override
                                                public int compare(Comment o1, Comment o2) {
                                                    return String.valueOf(o2.getTm()).compareTo(String.valueOf(o1.getTm()));
                                                }
                                            });

                                        } catch (NullPointerException e) {
                                            Log.e(TAG, "displayPhotos: NullPointerException : " + e.getMessage());

                                        }
                                    }

                                    photo.setC(mComments);
                                    mPhoto = photo;
                                    setupWidgets();

                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }


    // ********************************** END OF FIREBASE *******************************************************//

}
