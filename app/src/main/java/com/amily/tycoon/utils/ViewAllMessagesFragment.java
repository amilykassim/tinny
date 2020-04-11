package com.amily.tycoon.utils;


import android.app.Dialog;
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

import com.amily.tycoon.R;
import com.amily.tycoon.models.TwoPartyMessageIDs;
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

public class ViewAllMessagesFragment extends Fragment {

    private static final String TAG = "ViewAllMessagesFragment";


    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;

    // vars
    private ArrayList<TwoPartyMessageIDs> mAllSendersIds;
    private ArrayList<TwoPartyMessageIDs> mPaginatedSendersIds;
    private AllMessagesAdapter mAdapter;
    private int mResultsCount;

    // widgets
    private ImageView mBackArrow;
    private RecyclerView mRecyclerView;
    private Dialog mDialog;
    private ImageView mSelectedImage;
    private RelativeLayout mNomessageLayout;

    public ViewAllMessagesFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_all_messages, container, false);

        mAllSendersIds = new ArrayList<>();
        mPaginatedSendersIds = new ArrayList<>();
        mRecyclerView = view.findViewById(R.id.recyclerView_all_messages);
        mBackArrow = view.findViewById(R.id.backArrow);
        mNomessageLayout = view.findViewById(R.id.no_message_layout);

        mDialog = new Dialog(getActivity());
        mDialog.setContentView(R.layout.layout_view_profile_photo_dialog);
        mSelectedImage = mDialog.findViewById(R.id.selectedImage);

        instantiateFirebase();
        getAllSendersIds();
        popUpDialog();

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

    private void getAllSendersIds() {
        Log.e(TAG, "getFollowing: searching for senders ids " );

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_sender_messages_receiver))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                /* we clear all the sender ids and paginated ids array list in order to prevent data redundancy,
                 * i.e reduce the repetition of the same data */
                mAllSendersIds.clear();
                mPaginatedSendersIds.clear();

                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                    TwoPartyMessageIDs twoPartyMessageIDs = new TwoPartyMessageIDs();

                    twoPartyMessageIDs.setTm(singleSnapshot.getValue(TwoPartyMessageIDs.class).getTm());
                    twoPartyMessageIDs.setSid(singleSnapshot.getValue(TwoPartyMessageIDs.class).getSid());
                    twoPartyMessageIDs.setRid(singleSnapshot.getValue(TwoPartyMessageIDs.class).getRid());

                    mAllSendersIds.add(twoPartyMessageIDs);

                // Rearrange PERSONS IDS  according to the time they was sent

                try {
                    Collections.sort(mAllSendersIds, new Comparator<TwoPartyMessageIDs>() {
                        @Override
                        public int compare(TwoPartyMessageIDs o1, TwoPartyMessageIDs o2) {
                            return String.valueOf(o2.getTm()).compareTo(String.valueOf(o1.getTm()));
                        }
                    });

                }catch (NullPointerException e) {
                    Log.e(TAG, "displayPhotos: NullPointerException : " + e.getMessage() );

                }catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "displayPhotos: IndexOutOfBoundsException : " + e.getMessage() );
                }


                }

                // get all messages of everyone who sent you a message
                initAllMessageAdapter();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void initAllMessageAdapter() {

        if(mAllSendersIds != null) {
            try {

                int iteration = mAllSendersIds.size();

                if(iteration > 10) {
                    iteration = 10;
                }

                mResultsCount = iteration; // this will hold the number of photos that is added to mPaginated photos
                for(int i = 0; i < iteration; i++) {
                    mPaginatedSendersIds.add(mAllSendersIds.get(i));
                }

                if(mAllSendersIds.size() == 0) {
                    mNomessageLayout.setVisibility(View.VISIBLE);
                }

                mAdapter = new AllMessagesAdapter(getActivity(), R.layout.layout_message_item,
                        mPaginatedSendersIds, mSelectedImage, mDialog);
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));


            }catch (NullPointerException e) {
                Log.e(TAG, "displayPhotos: NullPointerException : " + e.getMessage() );

            }catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "displayPhotos: IndexOutOfBoundsException : " + e.getMessage() );
            }

        }
    }


    public void displayMoreOfAllMessages() {
        Log.e(TAG, "loadMorePhotos: loading more photos" );

        try {

            if(mAllSendersIds.size() > mResultsCount && mAllSendersIds.size() > 0) {
                int iterations;
                if(mAllSendersIds.size() > (mResultsCount + 10)) {
                    Log.e(TAG, "loadMorePhotos: There is more than 10 more messages");
                    iterations = 10;
                }
                else {
                    Log.e(TAG, "loadMorePhotos: There is less than 10 more messages" );
                    iterations = mAllSendersIds.size() - mResultsCount;
                }

                for(int i = mResultsCount; i < mResultsCount + iterations; i++) {
                    mPaginatedSendersIds.add(mAllSendersIds.get(i));
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

    private void instantiateFirebase() {

        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
    }

}
