package com.amily.tycoon.utils;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amily.tycoon.home.ViewStoryActivity;
import com.amily.tycoon.models.Story;
import com.amily.tycoon.R;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class StoryProfileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "StoriesAdapter";
    private Context mContext;
    private int mLayoutResource;
    private ArrayList<Story> mPhotos;
    private FirebaseMethods mFirebaseMethods;
    private FirebaseStorage mFirebaseStorage;

    private static final int PHOTO = 0;
    private static final int VIDEO = 1;

    // permissions
    String[] PERMISSIONS = new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    private ArrayList<Story> mPhotosForParticularUser = new ArrayList<>();


    public StoryProfileAdapter(Context mContext, int mLayoutResource,
                               ArrayList<Story> mPhotos, FirebaseMethods firebaseMethods,
                               FirebaseStorage firebaseStorage) {
        this.mContext = mContext;
        this.mLayoutResource = mLayoutResource;
        this.mPhotos = mPhotos;
        this.mFirebaseMethods = firebaseMethods;
        this.mFirebaseStorage = firebaseStorage;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if(viewType == PHOTO) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_image_item, parent, false);
            return new ImageViewHolder(view);
        }
        else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_video_item, parent, false);
            return new VideoViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

        // it's a photo that we are inflating
        if(holder instanceof ImageViewHolder) {

            ((ImageViewHolder) holder).mUsername.setVisibility(View.GONE);
            ((ImageViewHolder) holder).mDateCreated.setVisibility(View.GONE);

            // if the image path is valid then set it as a background on circle image view
            if(!mPhotos.get(position).getUrl().equals("")) {

                ((ImageViewHolder)holder).mStoryCircleImageView.setImageBitmap(stringToBitMap(mPhotos.
                        get(position).getTh()));

            }
            // if the image path is invalid then set the video thumbnail as the background on circle image view
            else {

                final BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(),
                        stringToBitMap(mPhotos.get(position).getvTh()));

                // converting the bitmap drawable into bitmap in order to set it in the circle image view
                Drawable drawable = bitmapDrawable;
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

                ((ImageViewHolder) holder).mStoryCircleImageView.setImageBitmap(bitmap);
            }


            ((ImageViewHolder) holder).mParentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    mPhotosForParticularUser.clear();
                    mPhotosForParticularUser.add(mPhotos.get(position));

//                    Intent intent = new Intent(mContext, ViewPostActivity.class);
//                    intent.putExtra(mContext.getString(R.string.photo), mPhotosForParticularUser);
//                    mContext.startActivity(intent);

                    CharSequence[] options = new CharSequence[]{"View", "Delete"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setCancelable(true);

                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            viewOrDeleteStory(which, position);

                        }
                    });
                    builder.show();
                }
            });

        }


        // it's a video that we are inflating,
        else {

            ((VideoViewHolder) holder).mUsername.setVisibility(View.GONE);
            ((VideoViewHolder) holder).mDateCreated.setVisibility(View.GONE);

            Log.e(TAG, "onBindViewHolder: it's a video");

            // if the video thumbnail is valid then set it as a background in circle image view
            if(!mPhotos.get(position).getvTh().equals("")) {
                final BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(),
                        stringToBitMap(mPhotos.get(position).getvTh()));

                // converting the bitmap drawable into bitmap in order to set it in the circle image view
                Drawable drawable = bitmapDrawable;
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

                ((VideoViewHolder) holder).mStoryCircleImageView.setImageBitmap(bitmap);
            }

            // if the video thumbnail is invalid then set the image path as a background on circle image view
            else {

                // we set the bitmap to circle image view bcz it loads fast
                ((ImageViewHolder)holder).mStoryCircleImageView.setImageBitmap(stringToBitMap(mPhotos.
                        get(position).getTh()));
            }


            // on item clicked
            ((VideoViewHolder) holder).mParentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    CharSequence[] options = new CharSequence[]{"View", "Delete"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setCancelable(true);

                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            viewOrDeleteStory(which, position);

                        }
                    });
                    builder.show();

                }
            });

        }
    }

    private void viewOrDeleteStory(int which, final int position) {

        if (which == 0) {
            mPhotosForParticularUser.clear();

            ArrayList<Story> storyArrayList = new ArrayList<>();
            storyArrayList.add(mPhotos.get(position));

            Intent intent = new Intent(mContext, ViewStoryActivity.class);
            intent.putExtra(mContext.getString(R.string.swipe_photo_id), storyArrayList.get(0).getPid());
            intent.putExtra(mContext.getString(R.string.swipe_photo_user_id), storyArrayList.get(0).getUid());
            mContext.startActivity(intent);

//            // it's a photo that we are passing
//            if(storyArrayList.get(0).getvUrl().equals("")) {
//                Intent intent = new Intent(mContext, ViewStoryActivity.class);
//                intent.putExtra(mContext.getString(R.string.photo), storyArrayList);
//                mContext.startActivity(intent);
//            }
//
//            // it's a video that we are passing
//            else {
//                Intent intent = new Intent(mContext, ViewStoryActivity.class);
//                intent.putExtra(mContext.getString(R.string.swipe_photo_id), storyArrayList.get(0).getPid());
//                intent.putExtra(mContext.getString(R.string.swipe_photo_user_id), storyArrayList.get(0).getUid());
//                mContext.startActivity(intent);
//            }
        }

        if(which == 1) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setCancelable(true);

            builder.setMessage("Are you sure you want to delete this story?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            // deleting the post
                            Log.e(TAG, "onClick: deleting the post");

                            ProgressDialog progressDialog = new ProgressDialog(mContext);
                            progressDialog.setMessage("Deleting...");
                            progressDialog.setCancelable(false);
                            progressDialog.show();

                            if(!mPhotos.get(position).getUrl().equals("")) {
                                mFirebaseMethods.deleteStoryFromStorage(
                                        mFirebaseStorage.getReferenceFromUrl(mPhotos.get(position).getUrl()), progressDialog);

                            }

                            // it's a story video that we are deleting
                            else {
                                mFirebaseMethods.deleteStoryFromStorage(
                                        mFirebaseStorage.getReferenceFromUrl(mPhotos.get(position).getvUrl()), progressDialog);

                            }

                            FirebaseDatabase.getInstance().getReference().child(mContext.getString(R.string.dbname_user_stories))
                                    .child(mPhotos.get(position).getUid())
                                    .child(mPhotos.get(position).getPid())
                                    .removeValue(); // removing the story

                            mPhotos.remove(position);
                            notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // dismiss
                        }
                    });
            builder.show();
        }
    }

    @Override
    public int getItemCount() {

        return mPhotos.size();
    }

    @Override
    public int getItemViewType(int position) {

        // complete here
        if(mPhotos.get(position).getvUrl().equals("")) {
            return PHOTO;
        }
        else
            return VIDEO;
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView mStoryCircleImageView;
        private TextView mUsername, mDateCreated;
        private RelativeLayout mParentLayout;

        public ImageViewHolder(View itemView) {
            super(itemView);

            mStoryCircleImageView = itemView.findViewById(R.id.storyThumbnail_circl_image_view);
            mUsername = itemView.findViewById(R.id.username);
            mDateCreated = itemView.findViewById(R.id.date_created);
            mParentLayout = itemView.findViewById(R.id.parentLayout);
        }
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView mStoryCircleImageView;
        private TextView mUsername, mDateCreated;
        private RelativeLayout mParentLayout;

        public VideoViewHolder(View itemView) {
            super(itemView);

            mStoryCircleImageView = itemView.findViewById(R.id.storyThumbnail_circl_image_view);
            mUsername = itemView.findViewById(R.id.username);
            mDateCreated = itemView.findViewById(R.id.date_created);
            mParentLayout = itemView.findViewById(R.id.parentLayout);
        }
    }

    public Bitmap stringToBitMap(String encodedString){
        try {
            byte [] encodeByte=Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap=BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }
}
