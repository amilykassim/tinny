package com.amily.tycoon.accountSettings;

import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.amily.tycoon.R;
import com.amily.tycoon.utils.SectionsStatePagerAdapter;

public class AccountSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AccountSettingsActivity";
    private static final int ACTIVITY_NUM = 3;

    private SectionsStatePagerAdapter mPagerAdapter;
    private ViewPager viewPager;
    private RelativeLayout mRelativelayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        viewPager = findViewById(R.id.viewpager_container);

        setupFragment();

        getIncomingIntents();
    }

    private void getIncomingIntents() {
        Intent intent = getIntent();

        if(intent.hasExtra(getString(R.string.edit_profile_header))) {
            setupViewPager(mPagerAdapter.getFragmentNumber(getString(R.string.edit_profile_fragment)));
        }
        else if(intent.hasExtra(getString(R.string.log_out_header))) {
            setupViewPager(mPagerAdapter.getFragmentNumber(getString(R.string.log_out_fragment)));
        }
    }

    private void setupFragment() {
        mPagerAdapter = new SectionsStatePagerAdapter(getSupportFragmentManager());
        mPagerAdapter.addFragment(new EditProfileFragment(), getString(R.string.edit_profile_fragment)); // fragment 1
        mPagerAdapter.addFragment(new SignOutFragment(), getString(R.string.log_out_fragment));// fragment 2

    }
    private void setupViewPager(int fragmentNumber) {
        viewPager.setAdapter(mPagerAdapter);
        viewPager.setCurrentItem(fragmentNumber);
    }
}
