package com.amily.tycoon.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amily.tycoon.R;
import com.amily.tycoon.models.RealMessages;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MessageAdapter";

    private static final int ITEM_LEFT = 1;
    private static final int ITEM_RIGHT = 0;
    private List<RealMessages> messagesList = new ArrayList<>();
    private Context mContext;
    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private LinearLayout mLoadingLayout;
    private String mUserId;

    public MessageAdapter(Context context, List<RealMessages> messagesList,
                          String userId, LinearLayout loadingLayout) {
        this.messagesList = messagesList;
        this.mContext = context;
        this.mUserId = userId;
        this.mLoadingLayout = loadingLayout;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == ITEM_RIGHT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_right_message, parent, false);
            return new RightViewHolder(view);
        }
        else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_left_message, parent, false);
            return new LeftViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

        if(holder instanceof RightViewHolder) {
            // i inversed the background of messages :)
            ((RightViewHolder) holder).rightMessageTv.setText(messagesList.get(position).getM());
            ((RightViewHolder) holder).rightMessageTv.setBackgroundResource(R.drawable.message_sender_background);


            // current user id
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference imageProfileReference = FirebaseDatabase.getInstance().getReference();
            imageProfileReference.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(currentUserId)
                    .child(mContext.getString(R.string.field_profile_photo))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            try {
                                final String image = dataSnapshot.getValue().toString();

                                Glide.with(mContext)
                                        .load(image)
                                        .listener(new RequestListener<Drawable>() {
                                            @Override
                                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                mLoadingLayout.setVisibility(View.GONE);
                                                return false;
                                            }

                                            @Override
                                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                mLoadingLayout.setVisibility(View.GONE);
                                                return false;
                                            }
                                        })
                                        .into(((RightViewHolder)holder).profileImageView);

                            }catch (Exception e) {
                                Log.e(TAG, "onDataChange: there is an EXCEPTION : " + e.getMessage());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            mLoadingLayout.setVisibility(View.GONE);
                        }
                    });

        }
        else {
            ((LeftViewHolder) holder).leftMessageTv.setText(messagesList.get(position).getM());
            ((LeftViewHolder) holder).leftMessageTv.setBackgroundResource(R.drawable.message_receiver_background);

            DatabaseReference imageProfileReference = FirebaseDatabase.getInstance().getReference();
            String retrievedUserId = messagesList.get(position).getF();
            imageProfileReference
                    .child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(retrievedUserId)
                    .child(mContext.getString(R.string.field_profile_photo)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    try {
                        final String image = dataSnapshot.getValue().toString();

                        Glide.with(mContext)
                                .load(image)
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                        mLoadingLayout.setVisibility(View.GONE);
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                        mLoadingLayout.setVisibility(View.GONE);
                                        return false;
                                    }
                                })
                                .into(((LeftViewHolder)holder).profileImageView);

                    }catch (Exception e) {
                        Log.e(TAG, "onDataChange: there is an EXCEPTION : " + e.getMessage());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    mLoadingLayout.setVisibility(View.GONE);
                }
            });

        }

    }


    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(messagesList.get(position).getF().equals(currentUserId))
            return ITEM_RIGHT;
        else
            return ITEM_LEFT;
    }

    public class RightViewHolder extends RecyclerView.ViewHolder {
        TextView rightMessageTv;
        RelativeLayout mGreyCheckMarks, mBlueCheckMarks;
        CircleImageView profileImageView;

        public RightViewHolder(View itemView) {
            super(itemView);
            mGreyCheckMarks = itemView.findViewById(R.id.relLayout_grey_check_marks);
            mBlueCheckMarks = itemView.findViewById(R.id.relLayout_blue_check_marks);
            rightMessageTv = itemView.findViewById(R.id.right_message_textview);
            profileImageView = itemView.findViewById(R.id.profile_image_right_layout);
        }
    }
    public class LeftViewHolder extends RecyclerView.ViewHolder {
        TextView leftMessageTv;
        CircleImageView profileImageView;

        public LeftViewHolder(View itemView) {
            super(itemView);
            leftMessageTv = itemView.findViewById(R.id.left_message_layout);
            profileImageView = itemView.findViewById(R.id.profile_image_left_layout);
        }
    }
}

