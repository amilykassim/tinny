package com.amily.tycoon.utils;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amily.tycoon.R;
import com.amily.tycoon.models.Post;
import com.amily.tycoon.models.UserAccountSettings;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class SearchFragment extends Fragment {

    private static final String TAG = "AllUsersFragment";

    public SearchFragment() {
    }

    private EditText mSearchField;
    private RecyclerView mResultListRecyclerview;
    private ImageView mSearch, mBackArrow;
    private static TextView mNoResults;
    private static ProgressBar mProgressBar;
    private DatabaseReference mUserDatabaseReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_search, container, false);

        mSearchField = view.findViewById(R.id.search_field);
        mSearch = view.findViewById(R.id.ic_search);
        mBackArrow = view.findViewById(R.id.backArrow);
        mProgressBar = view.findViewById(R.id.progressbar);
        mNoResults = view.findViewById(R.id.no_result_found_textview);
        mResultListRecyclerview = view.findViewById(R.id.search_recyclerview);
        mResultListRecyclerview.setHasFixedSize(true);
        mResultListRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));
        mUserDatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.dbname_user_account_settings));


        initOnClickListeners();

        return view;
    }

    private void initOnClickListeners() {

        mSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchText = mSearchField.getText().toString().toLowerCase();
                mNoResults.setVisibility(View.GONE);

                // checking for the internet connection
                try {
                    ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    if(networkInfo != null && networkInfo.isConnected()) {
                        if(!searchText.equals("")) {
                            mProgressBar.setVisibility(View.VISIBLE);
                            closeKeyBoard();
                            firebaseUserSearch(searchText);
                        }
                        else
                            StyleableToast.makeText(getActivity(), "Type something...", R.style.customToast).show();
                    }
                    else {
                        StyleableToast.makeText(getActivity(), "Check your internet connection", R.style.customToast).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(s.toString().equals("")) {
                    mProgressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }


    ///////////////////////////// SEARCHING IN THE DISPLAY NAMES OF THE USERS //////////////////////////////////
    private void firebaseUserSearch(String searchText) {

        Log.e(TAG, "firebaseUserSearch: started firebase search");

        Query firebaseSearchQuery = mUserDatabaseReference
                .orderByChild(getString(R.string.field_displayName))
                .startAt(searchText)
                .endAt(searchText + "\uf8ff"); // without this unicode the firebase search won't work

        firebaseSearchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() == null) {
                    mProgressBar.setVisibility(View.GONE);
                    mNoResults.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        FirebaseRecyclerOptions<UserAccountSettings> options = new FirebaseRecyclerOptions.Builder<UserAccountSettings>()
                .setQuery(firebaseSearchQuery, UserAccountSettings.class)
                .build();

        FirebaseRecyclerAdapter<UserAccountSettings, UsersViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<UserAccountSettings, UsersViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UsersViewHolder holder, int position, @NonNull UserAccountSettings model) {

                try {
                    if (isAdded()) {
                        // setting the user information
                        holder.setDetails(getActivity(), model);
                    }
                }catch (Exception e) {
                    Log.e(TAG, "onBindViewHolder: there is an Exception : " + e.getMessage());
                }
            }

            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_search_user_item, parent, false);

                return new UsersViewHolder(view);
            }
        };

        firebaseRecyclerAdapter.startListening();
        mResultListRecyclerview.setAdapter(firebaseRecyclerAdapter);
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public UsersViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setDetails(final Context context, final UserAccountSettings userAccountSettings){

            // these are the views
            RelativeLayout notification_relative_layout = mView.findViewById(R.id.notification_relative_layout);
            final CircleImageView profile_image_view = mView.findViewById(R.id.profileImage);
            TextView user_name_view = mView.findViewById(R.id.username);
            TextView display_name_view = mView.findViewById(R.id.display_name);


            if(!userAccountSettings.getPp().equals("")) {
                // if there is no photo
                try {
                    Glide.with(context)
                            .load(userAccountSettings.getPp())
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    return false;
                                }
                            })
                            .into(profile_image_view);
                } catch (Exception e) {
                    Log.e(TAG, "setupProfileWidgets: there is an EXCEPTION : " + e.getMessage());
                }

            }

            user_name_view.setText(userAccountSettings.getUnm());
            display_name_view.setText(userAccountSettings.getDnm());
            mProgressBar.setVisibility(View.GONE);
            mNoResults.setVisibility(View.GONE);


            notification_relative_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Post post = new Post();
                    post.setUid(userAccountSettings.getUid());
                    final Intent intent = new Intent(context, PostActivity.class);
                    intent.putExtra(context.getString(R.string.post), post);

                    // setting up the options
                    CharSequence[] options = new CharSequence[]{"Profile", "Posts", "Message"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setCancelable(true);

                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (which == 0) {
                                intent.putExtra(context.getString(R.string.view_other_profile_fragment),
                                        context.getString(R.string.view_other_profile_fragment));
                                context.startActivity(intent);
                            }
                            if (which == 1) {
                                intent.putExtra(context.getString(R.string.view_personal_posts),
                                        context.getString(R.string.view_personal_posts));
                                context.startActivity(intent);
                            }
                            if(which == 2) {

                                if(!userAccountSettings.getUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                    intent.putExtra(context.getString(R.string.field_message),
                                            context.getString(R.string.field_message));
                                    context.startActivity(intent);
                                }
                                else
                                    StyleableToast.makeText(context, "You can't send a message to yourself", R.style.customToast).show();
                            }

                        }
                    });
                    builder.show();
                }
            });
        }
    }

    // this is the method to close the keyboard, this is used only for fragments, and then for the activity look into into the search activity
    private void closeKeyBoard() {
        View view = getActivity().getCurrentFocus();
        if(view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
