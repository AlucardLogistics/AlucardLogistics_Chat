package com.logistics.alucard.alucardlogistics_chat;

import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StatusActivity extends AppCompatActivity {

    private static final String TAG = "StatusActivity";

    //firebase
    FirebaseDatabase mFirebaseDatabase;
    DatabaseReference mRef, mUserOnlineRef;
    FirebaseUser mCurrentUser;


    //widgets
    Toolbar mToolbar;
    Button btnSaveChanges;
    TextInputLayout mStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        Log.d(TAG, "onCreate: started");

        mStatus = findViewById(R.id.statusInput);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserOnlineRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mCurrentUser.getUid());

        mToolbar = findViewById(R.id.status_bar_layout);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.change_status);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getStatus();

        btnSaveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setStatus();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mCurrentUser != null ) {
            mUserOnlineRef.child("online").setValue(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mCurrentUser != null ) {
            //mUserOnlineRef.child("online").setValue(false);
        }
    }

    private void getStatus() {
        Log.d(TAG, "getStatus: get status field from settings activity with intent");

        String statusValue = getIntent().getStringExtra(getString(R.string.statusValue));
        mStatus.getEditText().setText(statusValue);

//        mRef = mFirebaseDatabase.getReference().child("users").child(mCurrentUser.getUid());
//        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                String status = dataSnapshot.child("status").getValue().toString();
//                mStatus.getEditText().setText(status);
//            }
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
    }

    private void setStatus() {
        mRef = mFirebaseDatabase.getReference().child("users").child(mCurrentUser.getUid()).child("status");
        final String status = mStatus.getEditText().getText().toString();
        mRef.setValue(status).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    Log.d(TAG, "onComplete: updated status field: " + status);
                    Toast.makeText(StatusActivity.this, "Status Updated Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.d(TAG, "onComplete: there was an error!");
                }
            }
        });
    }
}
