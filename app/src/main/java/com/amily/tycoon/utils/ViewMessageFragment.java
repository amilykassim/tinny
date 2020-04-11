package com.amily.tycoon.utils;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amily.tycoon.models.Post;
import com.amily.tycoon.models.RealMessages;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.R;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.muddzdev.styleabletoastlibrary.StyleableToast;
import com.rockerhieu.emojicon.EmojiconEditText;
import com.rockerhieu.emojicon.EmojiconsFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewMessageFragment extends Fragment {

    private static final String TAG = "ViewMessageFragment";


    public ViewMessageFragment() {

    }

    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private FirebaseMethods mFirebaseMethods;
    private StorageReference mStorageReference;
    private Context mContext;

    // widgets
    private String mCurrentUserId;
    private CircleImageView circleImageView;
    private TextView mUsernameTextView, mLastSeen;
    private ImageView mChatSendBtn, mBack;
    private CircleImageView mChatAddEmoji, mKeyboard;
    private static EmojiconEditText mChatMessageView;
    private static LinearLayout mLoadingLayout;
    private RecyclerView mMessagesListRecyclerView;

    // vars
    private final List<RealMessages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter mMessageAdapter;
    private Post mPhoto;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view =  inflater.inflate(R.layout.fragment_view_message, container, false);

        // instantiate firebase
        instantiateFirebase();


        mContext = getActivity();
        mCurrentUserId = mAuth.getCurrentUser().getUid();
        mLoadingLayout = view.findViewById(R.id.loadingLayout);

        // instantiate widgets
        mChatAddEmoji = view.findViewById(R.id.chat_add_emoji);
        mKeyboard = view.findViewById(R.id.ic_keyboard);
        mChatSendBtn = view.findViewById(R.id.chat_send);
        mChatMessageView = view.findViewById(R.id.chat_message_view);
        mBack = view.findViewById(R.id.backArrow);
        circleImageView = view.findViewById(R.id.custom_image_bar_layout);
        mUsernameTextView = view.findViewById(R.id.username_textView_chat_activity);
        mLastSeen = view.findViewById(R.id.last_seen_chat_activity);


        try {
            mPhoto = getPhotoFromBundle();

            /* Instantiate the recycler view state*/
            mMessageAdapter = new MessageAdapter(getActivity(), messagesList, getPhotoFromBundle().getUid(),
                    mLoadingLayout);
            mMessagesListRecyclerView = view.findViewById(R.id.messageRecyclerView_ChatActivity);

            linearLayoutManager = new LinearLayoutManager(getActivity());
            linearLayoutManager.setStackFromEnd(true);
            mMessagesListRecyclerView.setHasFixedSize(true);
            mMessagesListRecyclerView.setLayoutManager(linearLayoutManager);
            mMessagesListRecyclerView.setAdapter(mMessageAdapter);


            getChatUserInfo();

        }catch (Exception e) {
            Log.e(TAG, "onCreateView: there is an exception in receiving the photo : " + e.getMessage());
        }

        // send messages
        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: clicked the send button");
                sendMessage(mChatMessageView.getText().toString());
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        // setting up the emoji and the actions which happen when you click on it.
        mChatAddEmoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(TAG, "onClick: clicking the smile icon" );
                RelativeLayout relativeLayout = view.findViewById(R.id.majorLayout);
                LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        40.0f
                );
                relativeLayout.setLayoutParams(parameter);

                FrameLayout emojiconFragment = view.findViewById(R.id.emojicons_frame_layout);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        60.0f
                );
                emojiconFragment.setLayoutParams(param);

                hideKeyboard();
                mKeyboard.setVisibility(View.VISIBLE);
                mChatAddEmoji.setVisibility(View.GONE);

            }
        });

        // settting up the keyboard and the actions which happen when you click on it.
        mKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Log.e(TAG, "onClick: clicking the smile icon" );
                RelativeLayout relativeLayout = view.findViewById(R.id.majorLayout);
                LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0.0f
                );
                relativeLayout.setLayoutParams(parameter);

                FrameLayout emojiconFragment = view.findViewById(R.id.emojicons_frame_layout);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        100.0f
                );
                emojiconFragment.setLayoutParams(param);


                showKeyboard();
                mKeyboard.setVisibility(View.GONE);
                mChatAddEmoji.setVisibility(View.VISIBLE);
            }
        });

        mChatMessageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: clicking the message view icon" );
                RelativeLayout relativeLayout = view.findViewById(R.id.majorLayout);
                LinearLayout.LayoutParams parameter = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0.0f
                );
                relativeLayout.setLayoutParams(parameter);

                FrameLayout emojiconFragment = view.findViewById(R.id.emojicons_frame_layout);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        100.0f
                );
                emojiconFragment.setLayoutParams(param);


//                showKeyboard(); // no need to show the keyboard cause it appears automatically
                mKeyboard.setVisibility(View.GONE);
                mChatAddEmoji.setVisibility(View.VISIBLE);
            }
        });

        setEmojiconFragment(false);



        ////////////////////////////////////// LOADING MESSAGES ///////////////////////////////////////////
        try {

            loadMessages();
        }catch (Exception e) {
            Log.e(TAG, "onCreateView: THERE IS AN EXCEPTION : " + e.getMessage() );
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        ref.child(getString(R.string.dbname_user_account_settings))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(getString(R.string.field_online))
                .setValue("t");
    }

    @Override
    public void onStop() {
        super.onStop();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null) {
            // TIMESTAMP indicates the last seen of a user.
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
            ref.child(getString(R.string.dbname_user_account_settings))
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child(getString(R.string.field_online))
                    .setValue(ServerValue.TIMESTAMP);

        }
    }

    private void getChatUserInfo() {
        // get the profile image and the username
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .child(mPhoto.getUid())
                .child(getString(R.string.field_displayName));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                mUsernameTextView.setText(dataSnapshot.getValue(String.class));
                getProfilePhoto();

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getProfilePhoto() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .child(mPhoto.getUid())
                .child(getString(R.string.field_profile_photo));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                try {
                    Glide.with(mContext)
                            .load(dataSnapshot.getValue(String.class))
                            .into(circleImageView);

                    // get the last seen
                    getLastSeen();
                }catch (Exception e) {
                    Log.e(TAG, "onDataChange: THERE IS AN EXCEPTION : " + e.getMessage());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getLastSeen() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        Query query = reference
                .child(mContext.getString(R.string.dbname_user_account_settings))
                .child(mPhoto.getUid())
                .child(getString(R.string.field_online));

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                try {
                    String online = dataSnapshot.getValue().toString();
                    if (online.equals("t")) {
                        mLastSeen.setText("Online");
                    } else {
                        long lastTime = Long.parseLong(online);
                        String lastSeenTime = GetTimeAgo.getTimeAgo(lastTime, mContext);
                        if (lastSeenTime == null)
                            mLastSeen.setText("Just now");
                        else
                            mLastSeen.setText(lastSeenTime);
                    }
                }catch (Exception e) {
                    Log.e(TAG, "onDataChange: THERE IS AN EXCEPTIONS : " + e.getMessage());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    private void sendMessage(String message) {

        if(message.equals("")) {
            StyleableToast.makeText(getActivity(), "Type a message...", R.style.customToast).show();
        }
        else {
            mChatMessageView.setText("");
            mFirebaseMethods.setupTwoPartyMessageIDs(message, mPhoto.getUid());
        }
    }

    private void loadMessages() {

        mRootRef.child(getString(R.string.dbname_real_messages))
                .child(mCurrentUserId)
                .child(mPhoto.getUid())
                .addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                try {
                    RealMessages messages = dataSnapshot.getValue(RealMessages.class);

                    messagesList.add(messages);
                    mMessageAdapter.notifyDataSetChanged();

//                 this code scrolls down even when you finish typing your text
                    mMessagesListRecyclerView.scrollToPosition(messagesList.size() - 1);
                }catch (Exception e) {
                    Log.e(TAG, "onChildAdded: THERE IS AN EXCEPTION : " + e.getMessage());
                }
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

        // when there is no messages just show the loading layout for about 1.5 seconds
        MyRunnable mRunnable = new MyRunnable(getActivity());
        mHandler.postDelayed(mRunnable, 1000);
    }

    private void hideKeyboard() {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    public void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
    }

    private void setEmojiconFragment(boolean useSystemDefault) {
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.emojicons_frame_layout, EmojiconsFragment.newInstance(useSystemDefault))
                .commit();
    }

    public static EmojiconEditText getMessageField() {
        return mChatMessageView;
    }

    /*Get photo from the incoming bundle from home activity*/
    private Post getPhotoFromBundle() {
        Bundle bundle = this.getArguments();
        if(bundle != null) {
            return bundle.getParcelable(getString(R.string.photo));
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

    // this class is used to execute the method of delaying the process
    private static class MyHandler extends Handler {}
    private final MyHandler mHandler = new MyHandler();

    public static class MyRunnable implements Runnable {
        private final WeakReference<Activity> mActivity;

        public MyRunnable(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            Activity activity = mActivity.get();
            if (activity != null) {
                mLoadingLayout.setVisibility(View.GONE);
            }
        }
    }
}
