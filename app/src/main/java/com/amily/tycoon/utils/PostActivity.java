package com.amily.tycoon.utils;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.amily.tycoon.models.Post;
import com.amily.tycoon.R;
import com.google.firebase.auth.FirebaseAuth;
import com.rockerhieu.emojicon.EmojiconGridFragment;
import com.rockerhieu.emojicon.EmojiconsFragment;
import com.rockerhieu.emojicon.emoji.Emojicon;

public class PostActivity extends AppCompatActivity implements
        PostsAdapter.OnLoadMoreItemsListener,
        AllMessagesAdapter.OnLoadMoreOfAllMessagesListener,
        EmojiconGridFragment.OnEmojiconClickedListener,
        EmojiconsFragment.OnEmojiconBackspaceClickedListener{

    @Override
    public void onLoadMoreItems() {
        ViewAllPersonalPostsFragment personalPostsFragment = (ViewAllPersonalPostsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.post_activity_container);

        if(personalPostsFragment != null) {
            personalPostsFragment.displayMorePosts();
        }
    }

    @Override
    public void onLoadMoreOfAllMessages() {

        ViewAllMessagesFragment allMessagesFragment = (ViewAllMessagesFragment) getSupportFragmentManager()
                .findFragmentById(R.id.post_activity_container);

        if(allMessagesFragment != null) {
            allMessagesFragment.displayMoreOfAllMessages();
        }
    }


    private static final String TAG = "PostActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        getIncomingIntent();
    }

    private void getIncomingIntent() {

        Intent intent = getIntent();

        // Navigating to comment fragment
        if(intent.hasExtra(getString(R.string.field_comments))) {

            Post post = intent.getParcelableExtra(getString(R.string.post));
            Log.e(TAG, "onCommentThreadSelected: entered in the method to naviagete to comments");
            ViewCommentsFragment commentsFragment = new ViewCommentsFragment();

            Bundle args = new Bundle();
            args.putParcelable(getString(R.string.post), post);
            commentsFragment.setArguments(args);

            Log.e(TAG, "onCommentThreadSelected: finished setting bundles, and then trying to navigate to comments");
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.post_activity_container, commentsFragment);
            transaction.commit();
        }

        // Navigating to ViewAllMessages Fragment to view all messages
        else if(intent.hasExtra(getString(R.string.view_all_messages))) {

            ViewAllMessagesFragment allMessagesFragment = new ViewAllMessagesFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.post_activity_container, allMessagesFragment);
            transaction.commit();
        }

        // Navigating to View message fragment to chat with someone inbox
        else if(intent.hasExtra(getString(R.string.field_message))) {

            Post post = intent.getParcelableExtra(getString(R.string.post));
            ViewMessageFragment messageFragment = new ViewMessageFragment();
            Bundle args = new Bundle();
            args.putParcelable(getString(R.string.photo), post);
            messageFragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.post_activity_container, messageFragment);
            transaction.commit();
        }



        /* Navigating to ALL our personal posts or ALL other's personal posts,
        * for ours we use our USER ID AS THE KEY, AND FOR OTHERS WE USE (view_personal_posts) AS THE KEY*/
        else if(intent.hasExtra(getString(R.string.view_personal_posts))
                || intent.hasExtra(getString(R.string.my_user_id))) {

            // navigating to see other's all personal posts
            if(intent.hasExtra(getString(R.string.view_personal_posts))) {
                Post post = intent.getParcelableExtra(getString(R.string.post));
                ViewAllPersonalPostsFragment personalPosts = new ViewAllPersonalPostsFragment();
                Bundle args = new Bundle();
                args.putParcelable(getString(R.string.post), post);
                personalPosts.setArguments(args);


                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.post_activity_container, personalPosts);
                transaction.commit();
            }

            // Navigating to see our all personal posts
            else if(intent.hasExtra(getString(R.string.my_user_id))) {
                String userId = intent.getStringExtra(getString(R.string.my_user_id));
                ViewAllPersonalPostsFragment personalPosts = new ViewAllPersonalPostsFragment();
                Bundle args = new Bundle();
                args.putString(getString(R.string.my_user_id), userId);
                personalPosts.setArguments(args);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.post_activity_container, personalPosts);
                transaction.commit();
            }
        }
        // Navigating to view PERSONAL PROFILE
        else if(intent.hasExtra(getString(R.string.view_other_profile_fragment))) {

            Post post = intent.getParcelableExtra(getString(R.string.post));
            ViewPersonalProfileFragment personalProfileFragment = new ViewPersonalProfileFragment();
            Bundle args = new Bundle();
            args.putParcelable(getString(R.string.post), post);
            personalProfileFragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.post_activity_container, personalProfileFragment);
            transaction.commit();
        }

        // Navigating to report fragment and check if it is a feedback or a report
        else if(intent.hasExtra(getString(R.string.report_fragment))) {

            String feedback = intent.getStringExtra(getString(R.string.feedback));
            ReportFragment reportFragment = new ReportFragment();

            Bundle args = new Bundle();
            args.putString(getString(R.string.feedback), feedback);
            reportFragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.post_activity_container, reportFragment);
            transaction.commit();
        }

        // Navigating to all post views fragment
        else if(intent.hasExtra(getString(R.string.field_views))) {

            Log.e(TAG, "getIncomingIntent: navigate to see all post views in view post fragment");

            Post post = intent.getParcelableExtra(getString(R.string.post));
            ViewAllPostViewsFragment allPostViewsFragment = new ViewAllPostViewsFragment();

            Bundle args = new Bundle();
            args.putParcelable(getString(R.string.post), post);
            allPostViewsFragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.post_activity_container, allPostViewsFragment);
            transaction.commit();
        }

        // Navigating to search fragment
        else if(intent.hasExtra(getString(R.string.search_fragment))) {
            SearchFragment searchFragment = new SearchFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.post_activity_container, searchFragment);
            transaction.commit();
        }

        /* Navigating to view post fragment but acting like it comes from home activity,
         * we did this in order to reduce to reduce the chances of admob account to being banned,
         * that's why we use the HOME ACTIVITY as the calling activity in passing the bundles */
        else if(intent.hasExtra(getString(R.string.home_fragment))) {

            Post post = intent.getParcelableExtra(getString(R.string.post));

            ViewPostFragment viewPostFragment = new ViewPostFragment();
            Bundle args = new Bundle();
            args.putParcelable(getString(R.string.post), post);
            args.putString(getString(R.string.home_activity), getString(R.string.home_activity));
            viewPostFragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.post_activity_container, viewPostFragment);
            transaction.commit();
        }

        /* When nothing is choosed then navigate to View All Messages fragment,
        this usually happens when you click on the message notification */
        else {
            ViewAllMessagesFragment searchFragment = new ViewAllMessagesFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.post_activity_container, searchFragment);
            transaction.commit();
        }
    }


    // this is called from the post adapter to show the post of a given person
    public void onPostSelected(Post post, String callingActivity) {

        ViewPostFragment viewPostFragment = new ViewPostFragment();
        Bundle args = new Bundle();
        args.putParcelable(getString(R.string.post), post);
        args.putString(callingActivity, callingActivity);
        viewPostFragment.setArguments(args);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.post_activity_container, viewPostFragment);
        transaction.addToBackStack(getString(R.string.view_post_fragment));
        transaction.commit();
    }

    @Override
    public void onEmojiconClicked(Emojicon emojicon) {
        EmojiconsFragment.input(ViewMessageFragment.getMessageField(), emojicon);
    }

    @Override
    public void onEmojiconBackspaceClicked(View v) {
        EmojiconsFragment.backspace(ViewMessageFragment.getMessageField());
    }
}
