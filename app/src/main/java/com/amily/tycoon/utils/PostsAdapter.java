package com.amily.tycoon.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.amily.tycoon.home.HomeActivity;
import com.amily.tycoon.models.Post;
import com.amily.tycoon.models.User;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.models.Views;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {

    public interface OnLoadMoreItemsListener {
        void onLoadMoreItems();
    }
    OnLoadMoreItemsListener mOnLoadMoreItemsListener;

    private static final String TAG = "ProfileRecyclerViewAdap";

    private Context mContext;
    private int layoutResource;
    private ArrayList<Post> mPostArrayList = new ArrayList<>();
    private String mCalledFragment;
    private ImageView mSelectedImage;
    private Dialog mDialog;

    // firebase
    private DatabaseReference mRootRef;

    private SharedPreferences mSettings;

    public PostsAdapter(Context mContext, int layoutResource,
                        ArrayList<Post> posts, String calledFragment,
                        ImageView selectedImage, Dialog dialog) {
        this.mContext = mContext;
        this.layoutResource = layoutResource;
        this.mPostArrayList = posts;
        this.mCalledFragment = calledFragment;
        this.mSelectedImage = selectedImage;
        this.mDialog = dialog;
        instantiateFirebase();

        // Ads shared preferences
        mSettings = mContext.getSharedPreferences(mContext.getString(R.string.ads_shared_preferences), 0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResource, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        Log.e(TAG, "onBindViewHolder:  started");

        // getting the profile photo and the username in the home page
        getProfilePhoto(holder, position);


        /* THIS POSTS ADAPTER IS USED BY HOME FRAGMENT AND POST ACTIVITY*/
        // Here the called fragment is home fragment that needs to go view post fragment
        if(mCalledFragment.equals(mContext.getString(R.string.home_fragment))) {
            holder.mRelativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // add a person who viewed the post, and if it is your own post then don't add a view
                    if(!mPostArrayList.get(position).getUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {

                        checkIfHeHasViewedThePost(holder, mPostArrayList.get(position));
                        onPostClicked(holder, mPostArrayList.get(position));
                    }

                    Intent intent = new Intent(mContext, PostActivity.class);
                    intent.putExtra(mContext.getString(R.string.post), mPostArrayList.get(position));
                    intent.putExtra(mContext.getString(R.string.home_fragment), mContext.getString(R.string.home_fragment));
                    mContext.startActivity(intent);

                    String count = mSettings.getString(mContext.getString(R.string.ads_counting_key), "0");
                    int realCount = Integer.parseInt(count);

                    if(realCount < 3) {
                        realCount++;

                        SharedPreferences.Editor editor = mSettings.edit();
                        editor.putString(mContext.getString(R.string.ads_counting_key), String.valueOf(realCount)); // the first element is the key, and the second element is the value to be stored
                        editor.apply();
                    }

                }
            });
        }

        // and here the called fragment is view all personal posts fragment  that needs to go to view post fragment
        else if(mCalledFragment.equals(mContext.getString(R.string.view_all_personal_posts_fragment))) {
            holder.mRelativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ((PostActivity) mContext).onPostSelected(mPostArrayList.get(position),
                            mContext.getString(R.string.post_activity));

                }
            });
        }



        // if the user has reached the end of the list then load more data
        if(reachedEndOfList(position)) {
            loadMoreData();
        }

    }


    private void onPostClicked(final ViewHolder holder, final Post post) {
        Log.e(TAG, "onLikeButtonClicked: " );

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_posts))
                .child(post.getUid())
                .child(post.getPid())
                .child(mContext.getString(R.string.field_views));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // we only get into this loop if the photo has likes anyway we can't get into this loop
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                    // case 2: the user has not liked the photo
                    if(!holder.mViewedByCurrentUser) {
                        // add a new like
                        addNewView(holder, post);
                        break;
                    }

                }
                if(!dataSnapshot.exists()) {
                    // add new like
                    addNewView(holder, post);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void addNewView(ViewHolder holder, Post post) {
        Log.e(TAG, "addNewLike: adding a new like" );
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        String newLikeID = reference.push().getKey();
        Views like = new Views();
        like.setUid(FirebaseAuth.getInstance().getCurrentUser().getUid());
//        like.setType(mContext.getString(R.string.it_is_a_like));
//        reference.child(mContext.getString(R.string.dbname_posts))
//                .child(post.getPid())
//                .child(mContext.getString(R.string.field_views))
//                .child(newLikeID)
//                .setValue(like);

        reference.child(mContext.getString(R.string.dbname_user_posts))
                .child(post.getUid()) // the owner of the photo
                .child(post.getPid())
                .child(mContext.getString(R.string.field_views))
                .child(newLikeID)
                .setValue(like);

        // sdding date to database as a Timestamp
//        String datePath_photos = mContext.getString(R.string.dbname_posts)
//                + "/" + post.getPid()
//                + "/" + mContext.getString(R.string.field_views)
//                + "/" + newLikeID
//                + "/" + mContext.getString(R.string.field_time_created);

        String datePath_userPhotos = mContext.getString(R.string.dbname_user_posts)
                + "/" + post.getUid()
                + "/" + post.getPid()
                + "/" + mContext.getString(R.string.field_views)
                + "/" + newLikeID
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

        // add notification to database if the photo was not liked by it's owner
        if(!post.getUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            Log.e(TAG, "addNewLike: the photo was not liked by it's owner");
//            addNotificationToDatabase(photo);
        }

    }

    private void checkIfHeHasViewedThePost(final ViewHolder holder, Post photo) {
        Log.e(TAG, "getLikeString: started" );

        try {

            // checking if there is likes in the photos node
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            Query query = reference
                    .child(mContext.getString(R.string.dbname_user_posts))
                    .child(photo.getUid())
                    .child(photo.getPid())
                    .child(mContext.getString(R.string.field_views));

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    holder.mUsersSB = new StringBuilder();
                    ArrayList<String> usersWhoLiked = new ArrayList<>();

                    for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                        Log.e(TAG, "onDataChange: the singlesnapshot of like is : " + singleSnapshot.getValue(Views.class).getUid());

                        usersWhoLiked.add(singleSnapshot.getValue(Views.class).getUid());

                    }

                    if(dataSnapshot.exists()) {
                        Log.e(TAG, "onDataChange: checking if the current user has liked the photo");
                        for(int i = 0; i < usersWhoLiked.size(); i++) {
                            if(usersWhoLiked.contains(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                Log.e(TAG, "onDataChange: The current user liked the photo");
                                holder.mViewedByCurrentUser = true;
                            }
                            else {
                                Log.e(TAG, "onDataChange: the current user has not liked the photo");
                                holder.mViewedByCurrentUser = false;
                            }
                        }

                    }

                    if(!dataSnapshot.exists()) {
                        Log.e(TAG, "onDataChange: The datasnapshot doesn't exist means that there is no likes");
                        holder.mViewedByCurrentUser = false;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }catch (NullPointerException e) {
            Log.e(TAG, "getLikeString: NullPointerException " );
            holder.mViewedByCurrentUser = false;
        }
    }


    // reached the end of the list
    private boolean reachedEndOfList(int position) {
        return position == mPostArrayList.size() - 1;

    }

    // load more data into the main feed
    private void loadMoreData() {
        try {
            mOnLoadMoreItemsListener = (OnLoadMoreItemsListener) mContext;
        }catch (ClassCastException e) {
            Log.e(TAG, "onLoadMoreData: ClassCastException : " + e.getMessage() );
        }

        try {
            mOnLoadMoreItemsListener.onLoadMoreItems();
        }catch (NullPointerException e) {
            Log.e(TAG, "onLoadMoreData: NullPointerException : " + e.getMessage() );
        }
    }


    private void getUsername(final ViewHolder holder, final int position) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .child(mPostArrayList.get(position).getUid())
                .child(mContext.getString(R.string.field_displayName));

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(mPostArrayList.get(position).getD().length() >= 50) {
                    holder.postMessage.setText(mPostArrayList.get(position).getD().substring(0, 50) + "....");
                    holder.readMore.setVisibility(View.VISIBLE);
                }
                else {
                    holder.postMessage.setText(mPostArrayList.get(position).getD());
                    holder.readMore.setVisibility(View.GONE);
                }

                holder.topic.setText(mPostArrayList.get(position).getT());
                holder.username.setText(dataSnapshot.getValue(String.class) +
                        ", " + getLastTimeAgo(mPostArrayList.get(position)));

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getProfilePhoto(final ViewHolder holder, final int position) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .child(mPostArrayList.get(position).getUid())
                .child(mContext.getString(R.string.field_profile_photo));

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                try {
                    Glide.with(mContext).load(dataSnapshot.getValue(String.class)).into((holder).profileImage);

                }catch (Exception e) {
                    Log.e(TAG, "onDataChange: THERE IS AN EXCEPTION IN LOADING THE IMAGE" + e.getMessage());
                }

                // displaying the profile image in th dialog box when clicked on the profile image
                holder.profileImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mSelectedImage != null) {

                            try {
                                Glide.with(mContext).load(dataSnapshot.getValue(String.class)).into(mSelectedImage);
                            }catch (Exception e) {
                                Log.e(TAG, "onClick: THERE IS AN EXCEPTION : " + e.getMessage());
                            }
                            mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                            mDialog.show();
                        }
                    }
                });

                getUsername(holder, position);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void instantiateFirebase() {

        mRootRef = FirebaseDatabase.getInstance().getReference();
    }

    private String getLastTimeAgo(Post post) {
        String retrievedTimestamp = String.valueOf(post.getTm());
        String timeAgo = GetTimeAgo.getTimeAgoShort(Long.parseLong(retrievedTimestamp), mContext);
        if(timeAgo == null) {
            return "Just now";
        }
        return timeAgo;
    }

    @Override
    public int getItemCount() {
        return mPostArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView username, postMessage, readMore;
        Button topic;
        RelativeLayout mRelativeLayout;
        boolean mViewedByCurrentUser;
        StringBuilder mUsersSB;

        public ViewHolder (View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            username = itemView.findViewById(R.id.username);
            postMessage = itemView.findViewById(R.id.post_message);
            topic = itemView.findViewById(R.id.topic_btn);
            readMore = itemView.findViewById(R.id.read_more_text_view);
            mRelativeLayout = itemView.findViewById(R.id.parent_post_item);
        }
    }

}
