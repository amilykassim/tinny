package com.amily.tycoon.home;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.amily.tycoon.models.Story;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.share.ShareStoryActivity;
import com.amily.tycoon.utils.FirebaseMethods;
import com.amily.tycoon.utils.GetTimeAgo;
import com.amily.tycoon.utils.PostActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class DiscoverFragment extends Fragment {

    private static final String TAG = "DiscoverFragment";

    // permissions
    String[] PERMISSIONS = new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};


    private Context mContext;

    // firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private FirebaseMethods mFirebaseMethods;
    private StorageReference mStorageReference;
    private FirebaseStorage mFirebaseStorage;

    // swiping widgets
    private DiscoverAdapter mDiscoverAdapter;
    private ViewHolder viewHolder;
    private ArrayList<Story> array;
    private SwipeFlingAdapterView flingContainer; // this is the adapter of holding and swiping cards

    // widgets
    private ProgressBar mProgressBar;
    private Button mLoadMoreItemsBtn;
    private FloatingActionButton mAddStoryBtn;

    // vars
    private ArrayList<Story> mStories;
    private ArrayList<Story> mPaginatedStories;
    private ArrayList<Story> mOnlyStories;
    private ArrayList<String> mAllUsers;
    private int mResultsCount;
    private String mPhotoId;
    private String mPhotoUserId;
    private Boolean mLikedByCurrentUser;
    private StringBuilder mUsers;
    private UserAccountSettings mUserAccountSettings;
    private ArrayList<String> mAllUsersIDs;
    private int GALLERY_PICK = 1;
    private int REQUEST_TAKE_GALLERY_VIDEO = 2;


    private boolean mIsEmpty = false;
    private int mCount = 1;
    private String mTestingCaption;
    private SharedPreferences mSettings;


    public DiscoverFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        mContext = getActivity();
        instantiateFirebase();

        mProgressBar = view.findViewById(R.id.progressbar);
        mLoadMoreItemsBtn = view.findViewById(R.id.load_more_items_btn);
        mAddStoryBtn = view.findViewById(R.id.add_story_fragment_btn);

        mAllUsers = new ArrayList<>();
        mStories = new ArrayList<>();
        mOnlyStories = new ArrayList<>();
        mPaginatedStories = new ArrayList<>();
        array = new ArrayList<>();
        mAllUsersIDs = new ArrayList<>();


        initOnClickListeners();


        flingContainer = (SwipeFlingAdapterView) view.findViewById(R.id.frame);

        // assigning the shared preferences
        mSettings = mContext.getSharedPreferences("PREFS_PHOTO_STAFF", 0);


        mDiscoverAdapter = new DiscoverAdapter(array, getActivity());
        flingContainer.setAdapter(mDiscoverAdapter);
        flingContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {

            }
            @Override
            public void onLeftCardExit(Object dataObject) {
                array.remove(0);
                mDiscoverAdapter.notifyDataSetChanged();
                Log.e(TAG, "onRightCardExit: added a new like to this user : " + mPhotoUserId + "" +
                        " to this photo id : " + mPhotoId + " with this caption : " + mTestingCaption);

                //Do something on the left!
                //You also have access to the original object.
                //If you want to use it just cast it (String) dataObject


                SharedPreferences.Editor editor = mSettings.edit();

                editor.putString(mCount + getString(R.string.swipe_photo_user_id), mPhotoUserId); // the first element is the key, and the second element is the value to be stored
                editor.putString(mCount + getString(R.string.swipe_photo_id), mPhotoId); // same as above
                editor.apply();

                mCount++; // this is to increment the key number in order to avoid similar keys in preferences

                addNewLike();
            }
            @Override
            public void onRightCardExit(Object dataObject) {
                array.remove(0);
                mDiscoverAdapter.notifyDataSetChanged();
                Log.e(TAG, "onRightCardExit: added a new like to this user : " + mPhotoUserId + "" +
                        " to this photo id : " + mPhotoId + " with this caption : " + mTestingCaption);

                /* we save the user id in preferences because at the time that the user swipes, the photo id and
                 * the photo's user id changes and when we try to update the likes, it gets updated at the wrong photo,
                 * therefore we need to keep a reference of the previous ID'S in order to update the like at the
                 * right photo*/

                ///////////// UNCOMMENT THE CODES IN ON STOP METHOD ALSO ///////////

                SharedPreferences.Editor editor = mSettings.edit();

                editor.putString(mCount + getString(R.string.swipe_photo_user_id), mPhotoUserId); // the first element is the key, and the second element is the value to be stored
                editor.putString(mCount + getString(R.string.swipe_photo_id), mPhotoId); // same as above
                editor.apply();

                mCount++; // this is to increment the key number in order to avoid similar keys in preferences

                addNewLike();

            }
            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
                Log.e(TAG, "onAdapterAboutToEmpty: the array size is now : " + array.size());
                /* we use 6 bcz sometimes when the array size becomes empty instead of having a size of 0 as usual,
                 * it indicates a size of 6 then we decided to use that value also in order to detect when
                 * the adapter is about to end*/

                if((array.size() == 6 || array.size() == 1) && mStories.size() > 0) {
                    displayMorePhotos();
                }
                if(mIsEmpty && (array.size() == 0)) { // the mIsEmpty is used to help us to fill the photo array list when the photo are finished

                    mLoadMoreItemsBtn.setVisibility(View.VISIBLE);

                }
            }
            @Override
            public void onScroll(float scrollProgressPercent) {

                View view = flingContainer.getSelectedView();
//                view.findViewById(R.id.background).setAlpha(0);
                view.findViewById(R.id.item_swipe_right_indicator).setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
                view.findViewById(R.id.item_swipe_left_indicator).setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);
            }
        });


        // Optionally add an OnItemClickListener
        flingContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
            @Override
            public void onItemClicked(int itemPosition, Object dataObject) {

                View view = flingContainer.getSelectedView();
//                view.findViewById(R.id.background).setAlpha(0);

                mDiscoverAdapter.notifyDataSetChanged();
            }
        });

        mLoadMoreItemsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnlyStories.clear();
                mPaginatedStories.clear();

                displayPhotos();
                mLoadMoreItemsBtn.setVisibility(View.GONE);
            }
        });


        getAllUsers();

        return view;
    }

    private void addNewLike() {

        final String realPhotoUserId = mSettings.getString((mCount - 1) + getString(R.string.swipe_photo_user_id), "");
        final String realPhotoId = mSettings.getString((mCount - 1) + getString(R.string.swipe_photo_id), "");


        DatabaseReference videoViewsReference = FirebaseDatabase.getInstance().getReference();
        Query videoViewsQuery = videoViewsReference
                .child(mContext.getString(R.string.dbname_user_stories))
                .child(realPhotoUserId)
                .child(realPhotoId)
                .child(mContext.getString(R.string.field_video_views));


        videoViewsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                try {
                    int currentViews = Integer.parseInt(dataSnapshot.getValue(String.class));
                    String currentViewsString = String.valueOf(currentViews + 1);

                    if(!realPhotoUserId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        mRootRef.child(getString(R.string.dbname_user_stories))
                                .child(realPhotoUserId)
                                .child(realPhotoId)
                                .child(getString(R.string.field_video_views))
                                .setValue(currentViewsString);
                    }

                }catch (Exception e) {
                    Log.e(TAG, "onDataChange: There is an Exception : " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void initOnClickListeners() {

        mAddStoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CharSequence[] options = new CharSequence[] {"Photos", "Videos"};
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setCancelable(true);

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if(which == 0) {

                            // choose photos
                            checkPermission(); // check permissions before picking images
                            Intent galleryIntent = new Intent();
                            galleryIntent.setType("image/*");
                            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(galleryIntent, "Select images"), GALLERY_PICK);
                        }
                        else if(which == 1) {

                            // choose videos
                            checkPermission(); // check permissions before picking videos
                            Intent intent = new Intent();
                            intent.setType("video/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);
                        }
                    }
                });
                builder.show();
            }
        });
    }

    private void checkPermission() {

        if (!hasPermissions(getActivity(), PERMISSIONS)) {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, 11);
        }
    }

    private boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Intent intent = new Intent(getActivity(), ShareStoryActivity.class);
            intent.putExtra(getString(R.string.stories_fragment), getString(R.string.stories_fragment));
            intent.putExtra(getString(R.string.selected_image), data.getData().toString());
            startActivity(intent);

        }
        else if(requestCode == REQUEST_TAKE_GALLERY_VIDEO && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Intent intent = new Intent(getActivity(), ShareStoryActivity.class);
            intent.putExtra(getString(R.string.stories_fragment), getString(R.string.stories_fragment));
            intent.putExtra(getString(R.string.selected_video), data.getData().toString());
            startActivity(intent);
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        SharedPreferences settings = mContext.getSharedPreferences("PREFS_PHOTO_STAFF", 0);
        String tempLastCount = settings.getString(getString(R.string.swipe_last_count), 0 + "");
        if(Integer.parseInt(tempLastCount) != 0) {

            mCount = Integer.parseInt(tempLastCount);
            Log.e(TAG, "onStart: the last count before we closed the activity was : " + mCount);
        }else {
            Log.e(TAG, "onStart: it is the first time for the user to enter the app");
            mCount = 1; /* set it to the initial this means that
             it is the first time for the user to enter the application */
            Log.e(TAG, "onStart: so the initial count is : " + mCount);
        }

        getPhotoDetails();
//        getLikeString();

    }

    @Override
    public void onStop() {
        super.onStop();


        ////// DON'T EVER DELETE THE BELOW CODE IT WILL BE USED WHEN WE WILL INTRODUCE THE LIKE SYSTEM //////////

        SharedPreferences settings = mContext.getSharedPreferences("PREFS_PHOTO_STAFF", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(getString(R.string.swipe_last_count), String.valueOf(mCount));
        editor.apply();
        Log.e(TAG, "onStop: the count that is stored when the activity has stopped is : " + mCount);

    }

    private void getAllUsers() {
        Log.e(TAG, "getFollowing: searching for following " );

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_users));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                    mAllUsers.add(singleSnapshot.child(getString(R.string.field_user_id)).getValue().toString());
                }
                // we added our user_id so that we can see our posts also on the mainfeed
//                mFollowing.add(FirebaseAuth.getInstance().getCurrentUser().getUid());
                // get Photos
                getPhotos();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getPhotoDetails() {

//        SharedPreferences settings = mContext.getSharedPreferences("PREFS_PHOTO_STAFF", 0);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_user_account_settings))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(mSettings.getString((mCount - 1) + getString(R.string.swipe_photo_user_id), ""));


        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    mUserAccountSettings = singleSnapshot.getValue(UserAccountSettings.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // retrieving photos posts
    private void getPhotos() {
        Log.e(TAG, "getPhotos: entered in get photos method");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        for(int i = 0; i < mAllUsers.size(); i++) {
            final int count = i;

            final long cutoff = new Date().getTime() - TimeUnit.MILLISECONDS.convert(8760, TimeUnit.HOURS);

            Query query = reference
                    .child(getString(R.string.dbname_user_stories))
                    .child(mAllUsers.get(i))
                    .orderByChild(getString(R.string.field_user_id))
                    .equalTo(mAllUsers.get(i));


            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                        Log.e(TAG, "onDataChange: found user : " +
                                singleSnapshot.child(getString(R.string.field_user_id)).getValue());

                        try {
                            Story story = new Story();
                            // we casted the singlesnapshot in the hashmap
                            Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                            story.setC(objectMap.get(getString(R.string.field_caption)).toString());
                            story.setUnm(objectMap.get(getString(R.string.field_username)).toString());
                            story.setPp(objectMap.get(getString(R.string.field_profile_photo)).toString());
                            story.setTm(Long.parseLong(objectMap.get(getString(R.string.field_time_created)).toString()));
                            story.setPid(objectMap.get(getString(R.string.field_photo_id)).toString());
                            story.setUid(objectMap.get(getString(R.string.field_user_id)).toString());
                            story.setUrl(objectMap.get(getString(R.string.field_image_path)).toString());
                            story.setTh(objectMap.get(getString(R.string.field_image_thumbnail)).toString());
                            story.setvUrl(objectMap.get(getString(R.string.field_video_path)).toString());
                            story.setvTh(objectMap.get(getString(R.string.field_video_thumbnail)).toString());
                            story.setVs(objectMap.get(getString(R.string.field_video_views)).toString());

                            // the stories's date created is greater than 23 hours ago
                            if(story.getTm() < cutoff) {

                                // removing the data from firebase storage
                                // it's a story photo that we are deleting
                                if(!story.getUrl().equals("")) {
                                    mFirebaseMethods.deleteStoryFromStorage(
                                            mFirebaseStorage.getReferenceFromUrl(story.getUrl()), null);

                                }

                                // it's a story video that we are deleting
                                else {
                                    mFirebaseMethods.deleteStoryFromStorage(
                                            mFirebaseStorage.getReferenceFromUrl(story.getvUrl()), null);

                                }

                                mRootRef.child(getString(R.string.dbname_user_stories))
                                        .child(story.getUid())
                                        .child(story.getPid())
                                        .removeValue(); // removing the story


                            }

                            // the stories's date created is less than 23 hours ago
                            else {
                                mStories.add(story);
                            }


                        }catch (Exception e) {
                            Log.e(TAG, "onDataChange: The Exception was caught");
                        }

                    }

                    if(count >= mAllUsers.size() - 1) {
                        // display our photos
                        displayPhotos();

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void displayPhotos() {

        Collections.shuffle(mStories); // shuffle all the photos before putting them in the mOnlyStories array list
        if(mStories != null) {
            try {

                int iteration = mStories.size();

                if(iteration > 10) {
                    iteration = 10;
                }

                mResultsCount = iteration; // this will hold the number of photos that is added to mPaginated photos
                for(int i = 0; i < iteration; i++) {
                    mPaginatedStories.add(mStories.get(i));
                }

                for(int i = 0; i < mPaginatedStories.size(); i++) {
                    array.add(new Story(
                            mPaginatedStories.get(i).getC(),
                            mPaginatedStories.get(i).getUnm(),
                            mPaginatedStories.get(i).getPp(),
                            mPaginatedStories.get(i).getTm(),
                            mPaginatedStories.get(i).getUrl(),
                            mPaginatedStories.get(i).getTh(),
                            mPaginatedStories.get(i).getPid(),
                            mPaginatedStories.get(i).getUid(),
                            mPaginatedStories.get(i).getvUrl(),
                            mPaginatedStories.get(i).getvTh(),
                            mPaginatedStories.get(i).getVs()


                    ));
                }

                mDiscoverAdapter.notifyDataSetChanged();

                mIsEmpty = true; /* this is used for filling the photo array list when all photos have been viewed, so
                 that we keep showing something to the user*/

            }catch (NullPointerException e) {
                Log.e(TAG, "displayPhotos: NullPointerException : " + e.getMessage() );

            }catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "displayPhotos: IndexOutOfBoundsException : " + e.getMessage() );
            }

        }
    }

    public void displayMorePhotos() {
        Log.e(TAG, "loadMorePhotos: loading more photos" );

        try {
            if(mStories.size() > mResultsCount && mStories.size() > 0) {
                int iterations;
                if(mStories.size() > (mResultsCount + 10)) {
                    Log.e(TAG, "loadMorePhotos: There is more than 10 photos");
                    iterations = 10;
                }
                else {
                    Log.e(TAG, "loadMorePhotos: There is less than 10 photos" );
                    iterations = mStories.size() - mResultsCount;
                }

                mPaginatedStories.clear(); // clear the photos seen by the user and add new photos
                for(int i = mResultsCount; i < mResultsCount + iterations; i++) {
                    mPaginatedStories.add(mStories.get(i));
                }
                mResultsCount = mResultsCount + iterations; // update the mResultsCount

                Collections.shuffle(mPaginatedStories); // shuffle the additional photos before adding them to the array list
                Log.e(TAG, "displayMorePhotos: the number of photos that we are going to add again is : " + mPaginatedStories.size() );
                // adding to the array list new items
                for(int i = 0; i < mPaginatedStories.size(); i++) {
                    array.add(new Story(
                            mPaginatedStories.get(i).getC(),
                            mPaginatedStories.get(i).getUnm(),
                            mPaginatedStories.get(i).getPp(),
                            mPaginatedStories.get(i).getTm(),
                            mPaginatedStories.get(i).getUrl(),
                            mPaginatedStories.get(i).getTh(),
                            mPaginatedStories.get(i).getPid(),
                            mPaginatedStories.get(i).getUid(),
                            mPaginatedStories.get(i).getvUrl(),
                            mPaginatedStories.get(i).getvTh(),
                            mPaginatedStories.get(i).getVs()
                    ));
                }
                mDiscoverAdapter.notifyDataSetChanged();

            }
        }catch (NullPointerException e) {
            Log.e(TAG, "displayPhotos: NullPointerException : " + e.getMessage() );

        }catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "displayPhotos: IndexOutOfBoundsException : " + e.getMessage() );
        }
    }

    /*------------------------------------ START OF DISCOVER ADAPTER ----------------------------------------------------------
     * ----------------------------------------------------------------------------------------------------------------*/
    public class DiscoverAdapter extends BaseAdapter {

        public Context context;
        public List<Story> parkingList;

        private DiscoverAdapter (List<Story> apps, Context context) {
            this.parkingList = apps;
            this.context = context;
        }
        @Override
        public int getCount() {
            return parkingList.size();
        }
        @Override
        public Object getItem(int position) {
            mPhotoId = parkingList.get(position).getPid(); // this is for storing those ID'S in the preferences
            mPhotoUserId = parkingList.get(position).getUid();
            mTestingCaption = parkingList.get(position).getC();
            return position;
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {

            View rowView = convertView;

            if (rowView == null) {

                LayoutInflater inflater = getLayoutInflater();
                rowView = inflater.inflate(R.layout.swipe_item, parent, false);
                // configure view holder
                viewHolder = new ViewHolder();
                viewHolder.caption = rowView.findViewById(R.id.caption);
                viewHolder.imageTimePosted = rowView.findViewById(R.id.image_time_posted);
                viewHolder.profileImage = rowView.findViewById(R.id.profileImage);
                viewHolder.postImage = rowView.findViewById(R.id.postImage);
                viewHolder.ellipsis = rowView.findViewById(R.id.ellipsis);
                viewHolder.play = rowView.findViewById(R.id.play_icon);
                viewHolder.mLoadingProgressBar = rowView.findViewById(R.id.progressbar);
                viewHolder.username = rowView.findViewById(R.id.username);
                rowView.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }


            viewHolder.username.setText(parkingList.get(position).getUnm());
            viewHolder.imageTimePosted.setText(getLastTimeAgo(parkingList.get(position)));
            Glide.with(getActivity())
                    .load(parkingList.get(position).getPp())
                    .into(viewHolder.profileImage);



            if(parkingList.get(position).getC().length() >= 50) {
                viewHolder.caption.setText(parkingList.get(position).getC().substring(0, 50) + "....");
            }
            else {
                viewHolder.caption.setText(parkingList.get(position).getC());
            }


            // if it's a video
            if(parkingList.get(position).getUrl().equals("")) {

                viewHolder.play.setVisibility(View.VISIBLE);
                viewHolder.mLoadingProgressBar.setVisibility(View.GONE);

                final BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(),
                        stringToBitMap(parkingList.get(position).getvTh()));
                viewHolder.postImage.setBackground(bitmapDrawable);
            }
            else {

                // bitmap drawable to hold while loading the photo
                final BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(),
                        stringToBitMap(parkingList.get(position).getTh()));

                viewHolder.play.setVisibility(View.GONE);

                Glide.with(mContext)
                        .load(parkingList.get(position).getUrl())
                        .apply(new RequestOptions().placeholder(bitmapDrawable))
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {

                                Toast.makeText(mContext, "failed to load the photo", Toast.LENGTH_SHORT).show();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {

                                mProgressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(viewHolder.postImage);
            }

            viewHolder.play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    checkPermission();
                    ArrayList<Story> storyArrayList = new ArrayList<>();
                    storyArrayList.add(parkingList.get(position));
                    Intent intent = new Intent(mContext, ViewStoryActivity.class);
                    intent.putExtra(mContext.getString(R.string.swipe_photo_id), storyArrayList.get(0).getPid());
                    intent.putExtra(mContext.getString(R.string.swipe_photo_user_id), storyArrayList.get(0).getUid());
                    mContext.startActivity(intent);
                }
            });

            viewHolder.caption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    checkPermission();
                    ArrayList<Story> storyArrayList = new ArrayList<>();
                    storyArrayList.add(parkingList.get(position));

                    Intent intent = new Intent(mContext, ViewStoryActivity.class);
                    intent.putExtra(mContext.getString(R.string.swipe_photo_id), storyArrayList.get(0).getPid());
                    intent.putExtra(mContext.getString(R.string.swipe_photo_user_id), storyArrayList.get(0).getUid());
                    mContext.startActivity(intent);

//                    // it's a photo that we are passing
//                    if(storyArrayList.get(0).getvUrl().equals("")) {
//                        Intent intent = new Intent(mContext, ViewStoryActivity.class);
//                        intent.putExtra(mContext.getString(R.string.photo), storyArrayList);
//                        mContext.startActivity(intent);
//                    }
//
//                    // it's a video that we are passing
//                    else {
//                        Intent intent = new Intent(mContext, ViewStoryActivity.class);
//                        intent.putExtra(mContext.getString(R.string.swipe_photo_id), storyArrayList.get(0).getPid());
//                        intent.putExtra(mContext.getString(R.string.swipe_photo_user_id), storyArrayList.get(0).getUid());
//                        mContext.startActivity(intent);
//                    }
                }
            });

            viewHolder.ellipsis.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    CharSequence[] options = new CharSequence[]{"View full", "Report", "Cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setCancelable(true);

                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (which == 0) {

                                checkPermission();
                                ArrayList<Story> storyArrayList = new ArrayList<>();
                                storyArrayList.add(parkingList.get(position));

                                Intent intent = new Intent(mContext, ViewStoryActivity.class);
                                intent.putExtra(mContext.getString(R.string.swipe_photo_id), storyArrayList.get(0).getPid());
                                intent.putExtra(mContext.getString(R.string.swipe_photo_user_id), storyArrayList.get(0).getUid());
                                mContext.startActivity(intent);

//                                // it's a photo that we are passing
//                                if(storyArrayList.get(0).getvUrl().equals("")) {
//                                    Intent intent = new Intent(mContext, ViewStoryActivity.class);
//                                    intent.putExtra(mContext.getString(R.string.photo), storyArrayList);
//                                    mContext.startActivity(intent);
//                                }
//
//                                // it's a video that we are passing
//                                else {
//                                    Intent intent = new Intent(mContext, ViewStoryActivity.class);
//                                    intent.putExtra(mContext.getString(R.string.swipe_photo_id), storyArrayList.get(0).getPid());
//                                    intent.putExtra(mContext.getString(R.string.swipe_photo_user_id), storyArrayList.get(0).getUid());
//                                    mContext.startActivity(intent);
//                                }
                            }
                            if(which == 1) {
                                // report the user or the content
                                Intent myPostIntent = new Intent(getActivity(), PostActivity.class);
                                myPostIntent.putExtra(getString(R.string.report_fragment), getString(R.string.report_fragment));
                                startActivity(myPostIntent);
                            }
                            if (which == 2) {
                                // cancel
                            }

                        }
                    });
                    builder.show();
                }
            });

            mProgressBar.setVisibility(View.GONE);


            return rowView;
        }
    }

    public Bitmap stringToBitMap(String encodedString){
        try {
            byte [] encodeByte= Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }

    private String getLastTimeAgo(Story story) {
        String retrievedTimestamp = String.valueOf(story.getTm());
        String timeAgo = GetTimeAgo.getTimeAgoShort(Long.parseLong(retrievedTimestamp), mContext);
        if(timeAgo == null) {
            return "Just now";
        }
        return timeAgo;
    }


    public static class ViewHolder {
        public static FrameLayout background;
        public TextView caption;
        public ImageView postImage, ellipsis;
        public TextView username, imageTimePosted;
        public CircleImageView profileImage;
        public ImageView play;
        public ProgressBar mLoadingProgressBar;

    }

    /*------------------------------------- END OF DISCOVER ADAPTER ----------------------------------------------------------
     * ----------------------------------------------------------------------------------------------------------------*/


    private void instantiateFirebase() {

        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mStorageReference = FirebaseStorage.getInstance().getReference();
        mFirebaseMethods = new FirebaseMethods(mContext);
        mFirebaseStorage = FirebaseStorage.getInstance();
    }


}
