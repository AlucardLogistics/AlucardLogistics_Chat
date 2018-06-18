package com.logistics.alucard.alucardlogistics_chat;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    //Firebase auth
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase mDatabase;
    DatabaseReference myRef;

    //widgets
    private EditText mDisplayName, mEmail, mPassword;
    private Button mCreateAccount;
    private Toolbar mToolbar;

    private ProgressBar mProgressBar;

    //vars
    private String displayName, email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mDisplayName = findViewById(R.id.regDisplayName);
        mEmail = findViewById(R.id.loginEmail);
        mPassword = findViewById(R.id.loginPassword);
        mCreateAccount = findViewById(R.id.regButton);
        mProgressBar = findViewById(R.id.registerProgressBar);
        mProgressBar.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        mToolbar = findViewById(R.id.register_bar_layout);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.register);
        //creates a back arrow that goes back one level
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                displayName = mDisplayName.getText().toString();
                email = mEmail.getText().toString();
                password = mPassword.getText().toString();

                if(TextUtils.isEmpty(displayName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(RegisterActivity.this, "Fields cant be Blank.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    create_account(email, password);
                }
            }
        });
    }

    private void addUserData(String userID) {
        Log.d(TAG, "addUserData: adding the user data to database: ");

        //access the point in database where we want to store the user data
        myRef = mDatabase.getReference().child("users").child(userID);

        //create a tokenID for the users device
        String deviceToken = FirebaseInstanceId.getInstance().getToken();

        //populate database with user data
        HashMap<String, String> userMap = new HashMap<>();
        userMap.put("name", displayName);
        userMap.put("status", "Welcome to Alucard Logistics Chat. Enjoy!");
        userMap.put("image", "default");
        userMap.put("thumb_image", "default");
        userMap.put("device_token", deviceToken);

        myRef.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "onComplete: added user data to firebase SUCCESS!");
                if(task.isSuccessful()) {
                    mProgressBar.setVisibility(View.GONE);
                    Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainIntent);
                    finish();
                } else if(!task.isSuccessful()) {
                    Log.d(TAG, "onComplete: something went wrong" + task.getException());
                }
            }
        });

    }

    private void create_account(String email, String password) {
        Log.d(TAG, "create_account: started: ");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");

                            //retrieve current_user ID
                            currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            String userID = currentUser.getUid();

                            addUserData(userID);


                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            mProgressBar.setVisibility(View.GONE);
                            Toast.makeText(RegisterActivity.this, task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });

    }
}
