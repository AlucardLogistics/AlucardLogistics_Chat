package com.logistics.alucard.alucardlogistics_chat;

import android.app.Application;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

public class AlucardChat extends Application {

    private static final String TAG = "AlucardChat";

    //class that runs at application level

    private DatabaseReference mUserDatabse;
    private FirebaseAuth mAuth;

    @Override
    public void onCreate() {
        super.onCreate();

        //setup firebase offline feature
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        //picasso offline feature for images
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso build = builder.build();
        build.setIndicatorsEnabled(true);
        build.setLoggingEnabled(true);
        Picasso.setSingletonInstance(build);

        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser() != null) {
            mUserDatabse = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(mAuth.getCurrentUser().getUid());

            mUserDatabse.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    //check if user is online or offline
                    if (dataSnapshot != null) {

                        mUserDatabse.child("online").onDisconnect().setValue(false);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }
}
