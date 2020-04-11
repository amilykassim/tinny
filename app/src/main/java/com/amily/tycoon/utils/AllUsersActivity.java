package com.amily.tycoon.utils;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.amily.tycoon.models.UserAccountSettings;
import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class AllUsersActivity extends AppCompatActivity {

    private static final String TAG = "AllUsersActivity";

    // firebase
    private DatabaseReference mDatabaseReference;


    // widgets
    private RecyclerView mUsersList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);


        mUsersList = findViewById(R.id.users_list_recyclerView);
        mUsersList.setLayoutManager(new LinearLayoutManager(this));
        mUsersList.setHasFixedSize(true);
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child(getString(R.string.dbname_user_account_settings));

    }


    @Override
    protected void onStart() {
        super.onStart();

        final int[] count = {0};

        FirebaseRecyclerOptions<UserAccountSettings> options = new FirebaseRecyclerOptions.Builder<UserAccountSettings>()
                .setQuery(mDatabaseReference, UserAccountSettings.class)
                .build();

        FirebaseRecyclerAdapter<UserAccountSettings, UsersViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<UserAccountSettings, UsersViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UsersViewHolder holder, int position, @NonNull UserAccountSettings model) {

                holder.setName(model.getUnm());
                holder.setStatus(model.getDnm());
                holder.setImage(model.getPp());

                count[0]++;


                holder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(AllUsersActivity.this, "the number of users are : " + count[0], Toast.LENGTH_SHORT).show();
                    }
                });

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
        TextView username, displayName;
        CircleImageView profileImage;

        public UsersViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            username = itemView.findViewById(R.id.username);
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

            Glide.with(AllUsersActivity.this)
                    .load(image)
                    .into(circleImageView);

        }
    }

}
