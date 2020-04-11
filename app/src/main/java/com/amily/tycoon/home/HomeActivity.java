package com.amily.tycoon.home;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amily.tycoon.R;
import com.amily.tycoon.accountSettings.AccountSettingsActivity;
import com.amily.tycoon.accountSettings.EditProfileFragment;
import com.amily.tycoon.login.LoginActivity;
import com.amily.tycoon.models.Post;
import com.amily.tycoon.models.UserAccountSettings;
import com.amily.tycoon.models.UserSettings;
import com.amily.tycoon.utils.AllUsersActivity;
import com.amily.tycoon.utils.FirebaseMethods;
import com.amily.tycoon.utils.PostActivity;
import com.amily.tycoon.utils.PostsAdapter;
import com.amily.tycoon.utils.SectionsPagerAdapter;
import com.amily.tycoon.utils.ViewPostFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
//import com.google.android.gms.ads.AdListener;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;
//import com.google.android.gms.ads.InterstitialAd;
//import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity implements
        PostsAdapter.OnLoadMoreItemsListener,
        NavigationView.OnNavigationItemSelectedListener {

    @Override
    public void onLoadMoreItems() {
        HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.viewpager_container + ":" +  mViewPager.getCurrentItem());

        if(homeFragment != null) {
            homeFragment.displayMorePosts();
        }
    }

    private static final String TAG = "HomeActivity";
    private Context mContext;

    // firebase
    private DatabaseReference mRootRef;
    private FirebaseMethods mFirebaseMethods;

    // widgets
    private ViewPager mViewPager;
    private TextView mSearchField, mHeaderUsername, mHeaderDescriptions;
    private CircleImageView mHeaderProfileImage;
    private ImageView mProfileImage;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;
    private Toolbar mToolbar;
    private View mHeaderView;
    private static LinearLayout mLoadingLayout;
    private Dialog mDialog;
    private ImageView mSelectedImage;
    private String mOnlineState = "false";

    // vars
    private static final int REQUEST_CALL = 1;
    private static int mMtnOrTigo = 0;
    
    // send money dialog
    private Dialog mDialogSendMoney;
    private EditText mEnteredNumberDialog, mEnteredMoneyDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mContext = HomeActivity.this;
        mToolbar = findViewById(R.id.profileToolBar);

        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mDrawerLayout = findViewById(R.id.drawer);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open, R.string.close);
        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        mHeaderView = navigationView.getHeaderView(0);


        // dialog to show the profile image
        mDialog = new Dialog(this);
        mDialog.setContentView(R.layout.layout_view_profile_photo_dialog);
        mSelectedImage = mDialog.findViewById(R.id.selectedImage);

        
        // dialog to send money
        mContext = HomeActivity.this;
        mDialogSendMoney = new Dialog(this);
        mDialogSendMoney.setContentView(R.layout.layout_send_money_dialog);


        mViewPager = findViewById(R.id.viewpager_container);
        mSearchField = findViewById(R.id.search_textview);
        mProfileImage = findViewById(R.id.profileImage);
        mLoadingLayout = findViewById(R.id.loadingLayout);
        mHeaderUsername = mHeaderView.findViewById(R.id.username);
        mHeaderDescriptions = mHeaderView.findViewById(R.id.description);
        mHeaderProfileImage = mHeaderView.findViewById(R.id.profileImage);

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseMethods = new FirebaseMethods(mContext);


        deleteAllNotificationFromDatabase();
        setupViewPager();
        retrieveUserInformation();

        mSearchField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(checkInternetConnection()) {
                    Intent intent = new Intent(HomeActivity.this, PostActivity.class);
                    intent.putExtra(getString(R.string.search_fragment), getString(R.string.search_fragment));
                    mContext.startActivity(intent);
                }

            }
        });


        // instantiating the button to close the popping dialog which show the image
        TextView txtclose;
        txtclose = mDialog.findViewById(R.id.txtclose);
        txtclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });

    }

    private boolean checkInternetConnection() {

        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
            else {
                StyleableToast.makeText(this, "Check your internet connection", R.style.customToast).show();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Log.e(TAG, "onStart: on start method has started now /////////////////");
        mOnlineState = getString(R.string.its_false);

        if (currentUser == null) {
            Log.e(TAG, "onStart: the current user is : " + currentUser);
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        }
        else {
            int currentItem = mViewPager.getCurrentItem(); // getting the current item positon
            mViewPager.setCurrentItem(currentItem);
        }

    }

    private void deleteAllNotificationFromDatabase() {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        // deleting the messages notifications
        rootRef.child(getString(R.string.dbname_new_messages_notifications))
                .removeValue();

        // deleting the comments notifications
        rootRef.child(getString(R.string.dbname_comment_notifications))
                .removeValue();
    }

    private void retrieveUserInformation() {

        mRootRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e(TAG, "onDataChange: retrieve user information from the database" );

                Log.e(TAG, "onDataChange: datasnapshot exists" );
                setupProfileWidgets(mFirebaseMethods.getUserAccountSettings(dataSnapshot));

                /* delay the process so that it finishes to update the user's information,
                 * there is an inner class below all other codes that executes the
                 * method down below*/
                MyRunnable mRunnable = new MyRunnable(HomeActivity.this);
                mHandler.postDelayed(mRunnable, 1700);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setupProfileWidgets(final UserAccountSettings settings) {

        try {
            if (!settings.getPp().equals("")) {
                Glide.with(mContext)
                        .load(settings.getPp())
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
                        .into(mProfileImage);
            }

            // the header profile image
            Glide.with(mContext)
                    .load(settings.getPp())
                    .into(mHeaderProfileImage);


            // the profile image in the corner
            Glide.with(mContext)
                    .load(settings.getPp())
                    .into(mProfileImage);

            mProfileImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // the header profile image
//                    Glide.with(mContext)
//                            .load(settings.getPp())
//                            .into(mSelectedImage);
//
//                    mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//                    mDialog.show();

                    if(checkInternetConnection()) {
                        Intent myPostIntent = new Intent(HomeActivity.this, AccountSettingsActivity.class);
                        myPostIntent.putExtra(getString(R.string.edit_profile_header), getString(R.string.edit_profile_header));
                        startActivity(myPostIntent);
                    }
                }
            });

            mHeaderUsername.setText(settings.getDnm());
            mHeaderDescriptions.setText(settings.getD());

        }catch (Exception e) {
            Log.e(TAG, "setupProfileWidgets: THERE IS AN EXCEPTION : " + e.getMessage());
        }
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

    private void setupViewPager() {
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new HomeFragment(), "Feed"); // index 1
//        adapter.addFragment(new EditProfileFragment(), "Profile");
        adapter.addFragment(new DiscoverFragment(), "Discover");

        mViewPager.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.setSelectedTabIndicatorColor(Color.parseColor("#e90000"));
        tabLayout.setSelectedTabIndicatorHeight((int) (3 * getResources().getDisplayMetrics().density));
        tabLayout.setTabTextColors(Color.parseColor("#03A9F4"), Color.parseColor("#e90000"));

        // this is the previous color FF505050
    }

    // this is used to enable the nav drawer to come from the right side
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item != null && item.getItemId() == android.R.id.home) {
            if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                mDrawerLayout.closeDrawer(Gravity.RIGHT);
            }
            else {
                mDrawerLayout.openDrawer(Gravity.RIGHT);
            }
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

            case R.id.refresh_header :

                if(checkInternetConnection()) {
                    setupViewPager();
                }
//                 startActivity(new Intent(this, AllUsersActivity.class));
                break;
            case R.id.my_messages :

                if(checkInternetConnection()) {
                    Intent myPostIntent = new Intent(this, PostActivity.class);
                    myPostIntent.putExtra(getString(R.string.view_all_messages), getString(R.string.view_all_messages));
                    startActivity(myPostIntent);
                }
                break;
            case R.id.edit_profile_header :

                if(checkInternetConnection()) {
                    Intent myPostIntent = new Intent(this, AccountSettingsActivity.class);
                    myPostIntent.putExtra(getString(R.string.edit_profile_header), getString(R.string.edit_profile_header));
                    startActivity(myPostIntent);
                }
                break;
            case R.id.my_post_header :

                if(checkInternetConnection()) {
                    Intent myPostIntent = new Intent(this, PostActivity.class);
                    myPostIntent.putExtra(getString(R.string.my_user_id), FirebaseAuth.getInstance().getCurrentUser().getUid());
                    startActivity(myPostIntent);
                }
                break;
            case R.id.search_header :

                if(checkInternetConnection()) {
                    Intent intent = new Intent(HomeActivity.this, PostActivity.class);
                    intent.putExtra(getString(R.string.search_fragment), getString(R.string.search_fragment));
                    mContext.startActivity(intent);
                }
                break;

            case R.id.send_money_easy_way_header :

                CharSequence[] options = new CharSequence[]{"MTN", "TIGO"};
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setCancelable(true);

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // if the simcard is MTN
                        if (which == 0) {

                            mMtnOrTigo = 0; // 0 indicates that you choosed MTN

                            CharSequence[] options = new CharSequence[]{"Mobile Money Balance",
                                    "Send Money Easy and Fast", "Cancel"};
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            builder.setCancelable(true);

                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    if (which == 0) {
                                        if (ContextCompat.checkSelfPermission(HomeActivity.this,
                                                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(HomeActivity.this,
                                                    new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
                                        } else {
                                            String suffix = Uri.encode("#");
                                            String checkBalance = "*182*6*1";
                                            String dial = "tel:" + checkBalance + suffix;
                                            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                                        }
                                    }
                                    else if(which == 1) {
                                        // send money
                                        popUpDialog();
                                    }
                                }
                            });
                            builder.show();
                        }

                        // if the simcard is TIGO
                        if(which == 1) {

                            mMtnOrTigo = 1; // 1 indicates that you choosed TIGO

                            CharSequence[] options = new CharSequence[]{"Mobile Money Balance",
                                    "Send Money Easy and Fast", "Cancel"};
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            builder.setCancelable(true);

                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    if (which == 0) {
                                        if (ContextCompat.checkSelfPermission(HomeActivity.this,
                                                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(HomeActivity.this,
                                                    new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
                                        } else {
                                            String suffix = Uri.encode("#");
                                            String checkBalance = "*182*5*1*1";
                                            String dial = "tel:" + checkBalance + suffix;
                                            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                                        }
                                    }
                                    if(which == 1) {
                                        // send money
                                        popUpDialog();
                                    }
                                }
                            });
                            builder.show();
                        }
                    }
                });
                builder.show();
                break;

            case R.id.share_with_header :
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareSub = "Finally join your friends and their friends, see what's they're up to, " +
                        "Inspiring stories, post great photos and videos, send money easy " +
                        "and fast, etc..., what are you waiting for?, just " +
                        "click the link below and get Tinny Snap, Happy Tinning , https://play.google.com/store/apps/details?id=com.amily.tycoon";
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareSub);
                startActivity(Intent.createChooser(shareIntent, "Share using"));
                break;
            case R.id.feedback_header :

                if(checkInternetConnection()) {
                    Intent myPostIntent = new Intent(this, PostActivity.class);
                    myPostIntent.putExtra(getString(R.string.feedback), getString(R.string.feedback));
                    myPostIntent.putExtra(getString(R.string.report_fragment), getString(R.string.report_fragment));
                    startActivity(myPostIntent);
                }
                break;
            case R.id.report_a_problem_header :

                if(checkInternetConnection()) {
                    Intent myPostIntent = new Intent(this, PostActivity.class);
                    myPostIntent.putExtra(getString(R.string.report_fragment), getString(R.string.report_fragment));
                    startActivity(myPostIntent);
                }
                break;
            case R.id.logout_header :

                if(checkInternetConnection()) {
                    Intent logOutIntent = new Intent(this, AccountSettingsActivity.class);
                    logOutIntent.putExtra(getString(R.string.log_out_header), getString(R.string.log_out_fragment));
                    startActivity(logOutIntent);
                }
                break;

            default:
                return false;
        }
        //close navigation drawer
        mDrawerLayout.closeDrawer(Gravity.RIGHT);
        return true;
    }

    private void popUpDialog() {

        TextView txtclose;
        Button cancelBtn, submitBtn;

        txtclose = mDialogSendMoney.findViewById(R.id.txtclose);
        cancelBtn = mDialogSendMoney.findViewById(R.id.cancel_money_btn_dialog);
        mEnteredNumberDialog = mDialogSendMoney.findViewById(R.id.phone_number_dialog_et);
        mEnteredMoneyDialog = mDialogSendMoney.findViewById(R.id.amount_number_dialog_et);
        submitBtn = mDialogSendMoney.findViewById(R.id.send_money_btn_dialog);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialogSendMoney.dismiss();
            }
        });
        txtclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialogSendMoney.dismiss();
            }
        });


        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!mEnteredNumberDialog.getText().toString().equals("") &&
                        !mEnteredMoneyDialog.getText().toString().equals("")) {

                    // send money
                    sendMoney();
                }
                else {
                    StyleableToast.makeText(mContext, "Enter your email please", R.style.customToast).show();
                }
            }
        });


        mDialogSendMoney.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialogSendMoney.show();
    }

    public void sendMoney() {
        String amount = mEnteredMoneyDialog.getText().toString();
        String recipientNumber = mEnteredNumberDialog.getText().toString();
        if (ContextCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(HomeActivity.this,
                    new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
        } else {

            // this is when you choosed MTN as your simcard
            if(mMtnOrTigo == 0) {
                String suffix = Uri.encode("#");
                String checkBalance = "*182*1*1*1*" + recipientNumber + "*" + amount;
                String dial = "tel:" + checkBalance + suffix;
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
            }
            // this is when you choosed TIGO as your simcard
            else if(mMtnOrTigo == 1) {
                String suffix = Uri.encode("#");
                String checkBalance = "*182*1*1*" + recipientNumber + "*" + amount;
                String dial = "tel:" + checkBalance + suffix;
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
            }
        }
    }
}
