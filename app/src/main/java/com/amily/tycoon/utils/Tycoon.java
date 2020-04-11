package com.amily.tycoon.utils;

import android.app.Application;
import android.support.multidex.MultiDexApplication;

import com.google.firebase.database.FirebaseDatabase;

// extending MultiDexApplication
public class Tycoon extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        // add this class into the manifest or it won't work !!!!!!!!!!!
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

    }
}
