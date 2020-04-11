package com.amily.tycoon.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amily.tycoon.R;
import com.amily.tycoon.models.Post;
import com.amily.tycoon.models.RealMessages;
import com.amily.tycoon.models.TwoPartyMessageIDs;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.support.constraint.Constraints.TAG;

public class AllMessagesAdapter extends RecyclerView.Adapter<AllMessagesAdapter.ViewHolder> {

    public interface OnLoadMoreOfAllMessagesListener {
        void onLoadMoreOfAllMessages();
    }
    OnLoadMoreOfAllMessagesListener mOnLoadMoreOfAllMessagesListener;

    private Context mContext;
    private int mLayoutResource;
    private ArrayList<TwoPartyMessageIDs> mAllSendersIds = new ArrayList<>();
    private ArrayList<RealMessages> mMessages = new ArrayList<>();
    private LinearLayout mLoadingLayout;
    private ImageView mSelectedImage;
    private Dialog mDialog;

    public AllMessagesAdapter(Context mContext, int mLayoutResource, ArrayList<TwoPartyMessageIDs> mSendersIds,
                              ImageView selectedImage, Dialog dialog) {
        this.mContext = mContext;
        this.mLayoutResource = mLayoutResource;
        this.mAllSendersIds = mSendersIds;
        this.mSelectedImage = selectedImage;
        this.mDialog = dialog;
//        this.mLoadingLayout = mLoadingLayout;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutResource, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        getProfilePhoto(holder, position);

        // if the user has reached the end of the list then load more data
        if(reachedEndOfList(position)) {
            loadMoreData();
        }
    }

    // reached the end of the list
    private boolean reachedEndOfList(int position) {
        return position == mAllSendersIds.size() - 1;

    }

    // load more data into the main feed
    private void loadMoreData() {
        try {
            mOnLoadMoreOfAllMessagesListener = (OnLoadMoreOfAllMessagesListener) mContext;
        }catch (ClassCastException e) {
            Log.e(TAG, "onLoadMoreData: ClassCastException : " + e.getMessage() );
        }

        try {
            mOnLoadMoreOfAllMessagesListener.onLoadMoreOfAllMessages();
        }catch (NullPointerException e) {
            Log.e(TAG, "onLoadMoreData: NullPointerException : " + e.getMessage() );
        }
    }


    private void getProfilePhoto(final ViewHolder holder, final int position) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .child(mAllSendersIds.get(position).getRid())
                .child(mContext.getString(R.string.field_profile_photo));

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                try {

                    Log.e(TAG, "onDataChange: entered is setting up the profile image /////////");
                    Log.e(TAG, "onDataChange: the profile image url is : " + dataSnapshot.getValue(String.class));
                    Glide.with(mContext)
                            .load(dataSnapshot.getValue(String.class))
                            .into(holder.profileImage);

                }catch (Exception e) {
                    Log.e(TAG, "onDataChange: THERE IS AN EXCEPTION IN LOADING THE IMAGE" + e.getMessage());
                }

                // displaying the profile image in th dialog box when clicked on the profile image
                (holder).profileImage.setOnClickListener(new View.OnClickListener() {
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

    private void getUsername(final ViewHolder holder, final int position) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .child(mAllSendersIds.get(position).getRid())
                .child(mContext.getString(R.string.field_displayName));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                try {
                    holder.username.setText(dataSnapshot.getValue(String.class));

                    getMessages(holder, position);

                }catch (Exception e) {
                    Log.e(TAG, "onDataChange: THERE IS AN EXCEPTION IN LOADING THE IMAGE" + e.getMessage());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getMessages(final ViewHolder holder, final int position) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_real_messages))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(mAllSendersIds.get(position).getRid());


        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                /* clear all the messages in order to add new messages
                 of a new person who is loaded in the message list */
                mMessages.clear();

                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    RealMessages messages = new RealMessages();
                    messages.setM(singleSnapshot.getValue(RealMessages.class).getM());
                    messages.setF(singleSnapshot.getValue(RealMessages.class).getF());
                    messages.setTm(singleSnapshot.getValue(RealMessages.class).getTm());
                    messages.setSn(singleSnapshot.getValue(RealMessages.class).getSn());
                    mMessages.add(messages);

                }

                // Rearrange messages according to the time they was sent
                try {
                    Collections.sort(mMessages, new Comparator<RealMessages>() {
                        @Override
                        public int compare(RealMessages o1, RealMessages o2) {
                            return String.valueOf(o2.getTm()).compareTo(String.valueOf(o1.getTm()));
                        }
                    });

                }catch (NullPointerException e) {
                    Log.e(TAG, "displayPhotos: NullPointerException : " + e.getMessage() );

                }catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "displayPhotos: IndexOutOfBoundsException : " + e.getMessage() );
                }


                // set the message and the time sent ( the message we set is the last message that's why */
                holder.message.setText(mMessages.get(0).getM());
                if(mMessages.get(0).getM().length() > 30) {
                    holder.message.setText(mMessages.get(0).getM().substring(0, 30) + "....");
                }

                String timeAgo = getLastTimeAgo(mMessages.get(0));
                holder.timeSent.setText(timeAgo);


                if(mMessages.get(0).getF().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    holder.mGreyCheckMarks.setVisibility(View.VISIBLE);
                    holder.mBlueCheckMarks.setVisibility(View.GONE);
                    holder.mNewMessageIcon.setVisibility(View.GONE);
                }
                else {
                    holder.mGreyCheckMarks.setVisibility(View.GONE);
                    holder.mBlueCheckMarks.setVisibility(View.GONE);
                    holder.mNewMessageIcon.setVisibility(View.VISIBLE);
                }


                // Navigate to View message fragment when you click on the message IN THE MESSAGES LIST
                holder.mParentLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Post post = new Post();
                        post.setUid(mAllSendersIds.get(position).getRid());

                        Intent intent = new Intent(mContext, PostActivity.class);
                        intent.putExtra(mContext.getString(R.string.post), post);
                        intent.putExtra(mContext.getString(R.string.field_message), mContext.getString(R.string.field_message));
                        mContext.startActivity(intent);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private String getLastTimeAgo(RealMessages messages) {
        String retrievedTimestamp = String.valueOf(messages.getTm());
        String timeAgo = GetTimeAgo.getTimeAgo(Long.parseLong(retrievedTimestamp), this);
        if(timeAgo == null) {
            return "now";
        }
        return timeAgo;
    }


    @Override
    public int getItemCount() {
        return mAllSendersIds.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView username, message, timeSent;
        ImageView mNewMessageIcon;
        CircleImageView profileImage;
        RelativeLayout mParentLayout, mGreyCheckMarks, mBlueCheckMarks;

        public ViewHolder(View itemView) {
            super(itemView);

            username = itemView.findViewById(R.id.username);
            message = itemView.findViewById(R.id.message_v_all_messages);
            timeSent = itemView.findViewById(R.id.message_time_v_all_messages);
            profileImage = itemView.findViewById(R.id.profileImage);
            mParentLayout = itemView.findViewById(R.id.relLayoutParent);
            mGreyCheckMarks = itemView.findViewById(R.id.relLayout_grey_check_marks);
            mBlueCheckMarks = itemView.findViewById(R.id.relLayout_blue_check_marks);
            mNewMessageIcon = itemView.findViewById(R.id.ic_new_message);
        }
    }
}
