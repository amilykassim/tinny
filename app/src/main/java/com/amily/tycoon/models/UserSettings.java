package com.amily.tycoon.models;

import android.view.View;

public class UserSettings{

    private User user;
    private UserAccountSettings userAccountSettings;

    public UserSettings() {

    }

    public UserSettings(User user, UserAccountSettings userAccountSettings) {
        this.user = user;
        this.userAccountSettings = userAccountSettings;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserAccountSettings getUserAccountSettings() {
        return userAccountSettings;
    }

    public void setUserAccountSettings(UserAccountSettings userAccountSettings) {
        this.userAccountSettings = userAccountSettings;
    }

    @Override
    public String toString() {
        return "UserSettings{" +
                "user=" + user +
                ", userAccountSettings=" + userAccountSettings +
                '}';
    }

}
