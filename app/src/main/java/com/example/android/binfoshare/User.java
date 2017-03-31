package com.example.android.binfoshare;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by S on 01/03/2017.
 */

@IgnoreExtraProperties
public class User {
    public String name;
    public String email;

    // Default constructor required for calls to
    // DataSnapshot.getValue(User.class)
    public User() {
    }

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
