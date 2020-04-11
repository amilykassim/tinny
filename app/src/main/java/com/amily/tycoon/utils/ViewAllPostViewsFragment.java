package com.amily.tycoon.utils;


import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.amily.tycoon.models.Post;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.models.Views;
import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewAllPostViewsFragment extends Fragment {

    private static final String TAG = "ViewAllPostViewsFragm";

    // firebase
    private DatabaseReference mDatabaseReference;

    // widgets
    private RecyclerView mUsersList;
    private Dialog mDialog;
    private ImageView mSelectedImage, mBackArrow;
    private RelativeLayout mProgressBarLayout;

    public ViewAllPostViewsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_post_views, container, false);

        mDialog = new Dialog(getActivity());
        mDialog.setContentView(R.layout.layout_view_profile_photo_dialog);
        mSelectedImage = mDialog.findViewById(R.id.selectedImage);

        mUsersList = view.findViewById(R.id.post_views_list_recyclerView);
        mBackArrow = view.findViewById(R.id.backArrow);
        mProgressBarLayout = view.findViewById(R.id.progressbarLayout);
        mUsersList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mUsersList.setHasFixedSize(true);
        mDatabaseReference = FirebaseDatabase.getInstance().getReference()
                .child(getString(R.string.dbname_user_posts))
                .child(getPostFromBundle().getUid())
                .child(getPostFromBundle().getPid())
                .child(getString(R.string.field_views));


        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Views> options = new FirebaseRecyclerOptions.Builder<Views>()
                .setQuery(mDatabaseReference, Views.class)
                .build();

        FirebaseRecyclerAdapter<Views, UsersViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Views, UsersViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UsersViewHolder holder, int position, @NonNull Views model) {

                getDisplayName(holder, model.getUid());
                holder.timeCreated.setText(getLastTimeAgo(model.getTm()));
            }

            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_search_user_item, parent, false);
                return new UsersViewHolder(view);
            }
        };

        firebaseRecyclerAdapter.startListening();
        mUsersList.setAdapter(firebaseRecyclerAdapter);
    }

    public class UsersViewHolder extends RecyclerView.ViewHolder {
        View view;
        TextView timeCreated, displayName;
        CircleImageView profileImage;

        public UsersViewHolder(View itemView) {
            super(itemView);
            view = itemView;

            // I used the layout of search user item, cause it is the time only the content changes
            timeCreated = itemView.findViewById(R.id.username);
            displayName = itemView.findViewById(R.id.display_name);
            profileImage = itemView.findViewById(R.id.profileImage);
        }
        public void setName(String name) {
            TextView username = view.findViewById(R.id.username);
            username.setText(name);
        }
        public void setStatus(String status) {
            TextView statusTextView = view.findViewById(R.id.display_name);
            statusTextView.setText(status);
        }
        public void setImage(final String image) {
            final CircleImageView circleImageView = view.findViewById(R.id.profileImage);

            Glide.with(getActivity())
                    .load(image)
                    .into(circleImageView);

        }
    }

    public void getProfilePhoto(final UsersViewHolder holder, final String userId) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_user_account_settings))
                .child(userId)
                .child(getString(R.string.field_profile_photo));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                try {
                    Glide.with(getActivity()).load(dataSnapshot.getValue(String.class)).into(holder.profileImage);
                    mProgressBarLayout.setVisibility(View.GONE);
                }catch (Exception e) {
                    Log.e(TAG, "onDataChange: there is an exception : " + e.getMessage());
                }

                holder.profileImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Glide.with(getActivity()).load(dataSnapshot.getValue(String.class)).into(mSelectedImage);
                        popUpDialog();
                    }
                });

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getDisplayName(final UsersViewHolder holder, final String userId) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_user_account_settings))
                .child(userId)
                .child(getString(R.string.field_displayName));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                holder.displayName.setText(dataSnapshot.getValue(String.class));

                getProfilePhoto(holder, userId);
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

    private String getLastTimeAgo(long timeCreated) {
        String retrievedTimestamp = String.valueOf(timeCreated);
        String timeAgo = GetTimeAgo.getTimeAgo(Long.parseLong(retrievedTimestamp), getActivity());
        if(timeAgo == null) {
            return "Just now";
        }
        return timeAgo;
    }


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

}
