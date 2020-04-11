package com.amily.tycoon.utils;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SectionsStatePagerAdapter extends FragmentStatePagerAdapter {

  private static final String TAG = "SectionsStatePagerAdapt";

  private List<Fragment> mFragmentList = new ArrayList<>();
  private HashMap<Fragment, Integer> mFragments = new HashMap<>();
  private HashMap<Integer, String> mFragmentNames = new HashMap<>();
  private HashMap<String, Integer> mFragmentNumbers = new HashMap<>();

  public SectionsStatePagerAdapter(FragmentManager fm) {
    super(fm);
  }

  @Override
  public Fragment getItem(int position) {
    return mFragmentList.get(position);
  }

  @Override
  public int getCount() {
    return mFragmentList.size();
  }

  public void addFragment(Fragment fragment, String fragmentName) {
    mFragmentList.add(fragment);
    mFragments.put(fragment, mFragmentList.size() - 1);
    mFragmentNames.put(mFragmentList.size() - 1, fragmentName);
    mFragmentNumbers.put(fragmentName, mFragmentList.size() - 1);
  }

  /**
   * This method return the fragment number with the name @param
   * @param fragmentName
   * @return
   */
  public Integer getFragmentNumber(String fragmentName) {
    if(mFragmentNumbers.containsKey(fragmentName)) {
      return mFragmentNumbers.get(fragmentName);
    }
    else {
      Log.e(TAG, "getFragmentNumber: That fragment doesn't exist ");
      return null;
    }
  }
  /**
   * This method return the fragment number with the number @param
   * @param fragmentNumber
   * @return
   */

  public String getFragmentName(int fragmentNumber) {
    if(mFragmentNames.containsKey(fragmentNumber)) {
      return mFragmentNames.get(fragmentNumber);
    }
    else {
      Log.e(TAG, "getFragmentName: That fragment doesn't exist ");
      return null;
    }
  }
  /**
   * This method return the fragment number with the fragment @param
   * @param fragment
   * @return
   */

  public Integer getFragmentNumber(Fragment fragment) {
    if(mFragments.containsKey(fragment)) {
      return mFragments.get(fragment);
    }
    else {
      Log.e(TAG, "getFragmentNumber by using a fragment parameter: That fragment doesn't exist ");
      return null;
    }
  }
}
