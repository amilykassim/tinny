package com.amily.tycoon.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.models.Comment;
import com.amily.tycoon.models.Post;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.R;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentListAdapter extends RecyclerView.Adapter<CommentListAdapter.ViewHolder> {

    private static final String TAG = "CommentListAdapter";

    private Context mContext;
    private int mLayoutResource;
    private ArrayList<Comment> mComments = new ArrayList<>();
    private Post mPost = new Post();
    private LinearLayout mLoadingLayout;

    public CommentListAdapter(Context context, int mLayoutResource, ArrayList<Comment> mComments,
                              Post photo, LinearLayout loadingLayout) {
        Log.e(TAG, "CommentListAdapter: instantiating the context, layout resource and the comments array list" );
        this.mContext = context;
        this.mLayoutResource = mLayoutResource;
        this.mComments = mComments;
        this.mPost = photo;
        this.mLoadingLayout = loadingLayout;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.e(TAG, "onCreateViewHolder: entered the on create view holder of commentListAdapter" );
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutResource, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        Log.e(TAG, "onBindViewHolder: entered the the bind view holder of the commentListAdapter" );
        // set the comment
        holder.comment.setText(mComments.get(position).getC());
        holder.mRelLayout1.setBackgroundResource(R.drawable.message_receiver_background);

        // set the time ago
        String timeAgo = getLastTimeAgo(mComments.get(position));
        holder.timestamp.setText(timeAgo);

        // set the username and profile image;
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(mComments.get(position).getUid()); // that get(position) gets the comment object that you have passed to you adapter


        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()) {
                    mLoadingLayout.setVisibility(View.VISIBLE);
                    holder.username.setText(singleSnapshot.getValue(UserAccountSettings.class).getDnm());

                    try {
                        Glide.with(mContext)
                                .load(singleSnapshot.getValue(UserAccountSettings.class).getPp())
                                .into(holder.profileImage);
                    }catch (Exception e) {
                        Log.e(TAG, "onDataChange: THERE IS AN EXCEPTION : " + e.getMessage());
                    }

                }
                mLoadingLayout.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mLoadingLayout.setVisibility(View.GONE);
            }
        });

        holder.mRelLayoutParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(FirebaseAuth.getInstance().getCurrentUser().getUid().equals(mComments.get(position).getUid())) {

                    CharSequence[] options = new CharSequence[]{"delete", "cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setCancelable(true);

                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (which == 0) {
                                // delete
                                holder.mProgressBar.setVisibility(View.VISIBLE);
                                holder.mRelLayoutParent.setEnabled(false);

                                deleteComment(position, holder);
                            }
                            if (which == 1) {
                                // cancel
                            }

                        }
                    });
                    builder.show();
                }
            }
        });

    }

    private void deleteComment(final int position, final ViewHolder holder) {
        Log.e(TAG, "deleteComment: deleting comment...");

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

//        rootRef.child(mContext.getString(R.string.dbname_posts))
//                .child(mPost.getPid())
//                .child(mContext.getString(R.string.field_comments))
//                .child(mComments.get(position).getCid())
//                .removeValue();

        rootRef.child(mContext.getString(R.string.dbname_user_posts))
                .child(mPost.getUid())
                .child(mPost.getPid())
                .child(mContext.getString(R.string.field_comments))
                .child(mComments.get(position).getCid())
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            holder.mProgressBar.setVisibility(View.GONE);
                            holder.mRelLayoutParent.setEnabled(true);
                            mComments.remove(position);
                            notifyDataSetChanged(); // notify that the values in the adapter has changed
                        }
                        else {
                            holder.mProgressBar.setVisibility(View.GONE);
                            holder.mRelLayoutParent.setEnabled(true);
                        }
                    }
                });

    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

    private String getLastTimeAgo(Comment comment) {
        String retrievedTimestamp = String.valueOf(comment.getTm());
        String timeAgo = GetTimeAgo.getTimeAgo(Long.parseLong(retrievedTimestamp), this);
        return timeAgo;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView comment, username, timestamp;
        CircleImageView profileImage;
        RelativeLayout mRelLayoutParent, mRelLayout1;
        ProgressBar mProgressBar;

        public ViewHolder(View itemView) {
            super(itemView);

            comment = itemView.findViewById(R.id.comment);
            username = itemView.findViewById(R.id.comment_username);
            timestamp = itemView.findViewById(R.id.comment_time_posted);
            profileImage = itemView.findViewById(R.id.comment_profileImage);
            mRelLayout1 = itemView.findViewById(R.id.relLayout1);
            mRelLayoutParent = itemView.findViewById(R.id.relLayoutParent_comment);
            mProgressBar = itemView.findViewById(R.id.progressbar);

        }
    }
}
