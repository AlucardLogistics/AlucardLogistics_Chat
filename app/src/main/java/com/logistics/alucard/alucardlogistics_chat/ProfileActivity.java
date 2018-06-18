package com.logistics.alucard.alucardlogistics_chat;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    //firebase
    private FirebaseDatabase mDatabase;
    private DatabaseReference myRef, mFriendRequestDatabase,
            mFriendshipDatabase, mNotificationDatabase, mRootRef, mUserOnlineRef;
    private FirebaseUser mCurrentUser;

    //widgets
    private ImageView mProfileImage;
    private TextView mProfileName;
    private TextView mProfileStatus;
    private TextView mProfileFriends;
    private Button btnProfileSendRequest, btnDeclineRequest;
    private ProgressBar mProgressBar;

    //vars
    private String mCurrentState;
    private String profileUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mProfileName = findViewById(R.id.tvProfileDisplayName);
        mProfileStatus = findViewById(R.id.tvProfileStatus);
        mProfileFriends = findViewById(R.id.tvProfileTotalFriends);
        mProfileImage = findViewById(R.id.ivProfilePicture);
        btnProfileSendRequest = findViewById(R.id.btnProfileSendRequest);
        btnDeclineRequest = findViewById(R.id.btnDeclineRequest);
        mProgressBar = findViewById(R.id.profileProgressBar);
        mProgressBar.setVisibility(View.VISIBLE);

        profileUserID = getProfileUserID(profileUserID);
        Log.d(TAG, "onCreate: profileUserID: " + profileUserID);

        mCurrentState = "notFriends";
        mFriendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("friend_request");
        mFriendshipDatabase = FirebaseDatabase.getInstance().getReference().child("friendships");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserOnlineRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mCurrentUser.getUid());

        getUserData(profileUserID);

        setupWidgets();

        btnProfileSendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                switch (mCurrentState) {
                    case "notFriends":
                        notFriendState();
                        break;
                    case "requestSent":
                        cancelRequestFriendState();
                        break;
                    case "requestReceived":
                        btnDeclineRequest.setVisibility(View.VISIBLE);
                        requestReceivedState();
                        break;
                    case "friends":
                        removeFriendState();
                        break;
                }
            }
        });

        btnDeclineRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                declineFriendRequest();
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mCurrentUser != null ) {
            Log.d(TAG, "onStart: status is online");
            mUserOnlineRef.child("online").setValue(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: started");

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: status is offline");
        if(mCurrentUser != null ) {
            mUserOnlineRef.child("online").setValue(false);
            mUserOnlineRef.child("last_seen").setValue(ServerValue.TIMESTAMP);
        }
    }

    /**
     * //getting the user id intent from UsersActivity if userID
     * or from FirebaseMessagingService class if fromUserID
     * @param profileUserID
     * @return
     */
    private String getProfileUserID(String profileUserID) {

        profileUserID = getIntent().getStringExtra(getString(R.string.userID));
        if(profileUserID == null) {
            profileUserID = getIntent().getStringExtra(getString(R.string.fromUserID));
        } else {
            profileUserID = getIntent().getStringExtra(getString(R.string.userID));
        }

        return profileUserID;
    }

    private void setupWidgets() {
        //prevent sending friend request to own profile
        if(profileUserID.equals(mCurrentUser.getUid())) {
            btnProfileSendRequest.setVisibility(View.GONE);
        }

        btnDeclineRequest.setVisibility(View.GONE);
    }

    private void declineFriendRequest() {
        Log.d(TAG, "declineFriendRequest: started: ");
            btnProfileSendRequest.setEnabled(false);
            btnDeclineRequest.setEnabled(false);

            Map declineMap = new HashMap();
            declineMap.put("friend_request/" + mCurrentUser.getUid() + "/" + profileUserID, null);
            declineMap.put("friend_request/" + profileUserID + "/" + mCurrentUser.getUid(), null);

            mRootRef.updateChildren(declineMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    Log.d(TAG, "onComplete: attempt to decline friend request");
                    if (databaseError == null) {
                        Log.d(TAG, "onComplete: decline friend request success");

                        mCurrentState = "notFriends";
                        btnProfileSendRequest.setText(R.string.sendFriendRequest);
                        btnDeclineRequest.setVisibility(View.GONE);
                    } else {
                        String error = databaseError.getMessage();
                        Log.d(TAG, "onComplete: something went wrong: " + error);
                    }

                    btnProfileSendRequest.setEnabled(true);
                    btnDeclineRequest.setEnabled(true);
                }
            });

    }

    private void removeFriendState() {

        //------------------------- UNFRIEND PERSON -------------------
        //if(mCurrentState.equals("friends")) {
            Log.d(TAG, "removeFriendState: started: ");

            //disable sent request button
            btnProfileSendRequest.setEnabled(false);

        //remove friendship status from database
        Map unfriendMap = new HashMap();
        unfriendMap.put("friendships/" + mCurrentUser.getUid() + "/" + profileUserID, null);
        unfriendMap.put("friendships/" + profileUserID + "/" + mCurrentUser.getUid(), null);

        mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                Log.d(TAG, "onComplete: trying to remove friendship from database");
                if(databaseError == null) {
                    Log.d(TAG, "onComplete: friendship removed: ");


                    mCurrentState = "notFriends";
                    btnProfileSendRequest.setText(R.string.sendFriendRequest);

                    Toast.makeText(ProfileActivity.this, mProfileName.getText().toString() + " removed from friends.", Toast.LENGTH_SHORT).show();

                } else {
                    Log.d(TAG, "onComplete: could not remove friendship" + databaseError.getMessage());
                    Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }

                //enable sent request button
                btnProfileSendRequest.setEnabled(true);
            }
        });
        //}
    }

    private void requestReceivedState() {
        //---------------------REQUEST RECEIVED STATE ----------------------------
        //if(mCurrentState.equals("requestReceived")) {
        Log.d(TAG, "requestReceivedState: started: ");

        //set friendship and remove the requests from database
        final String currentDate = DateFormat.getDateInstance().format(new Date());

        Map friendsMap = new HashMap();
        friendsMap.put("friendships/" + mCurrentUser.getUid() + "/" + profileUserID + "/date", currentDate);
        friendsMap.put("friendships/" + profileUserID + "/" + mCurrentUser.getUid() + "/date", currentDate);
        //remove friend request fields
        friendsMap.put("friend_request/" + mCurrentUser.getUid() + "/" + profileUserID, null);
        friendsMap.put("friend_request/" + profileUserID + "/" + mCurrentUser.getUid(), null);

        mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                Log.d(TAG, "onComplete: trying to add data to friends table and remove the request from database");

                if (databaseError == null)  {
                    Log.d(TAG, "onComplete: success adding friendship");

                    String currentProfileName = mProfileName.getText().toString();

                    mCurrentState = "friends";
                    btnProfileSendRequest.setText("Unfriend " + currentProfileName);
                    btnProfileSendRequest.setBackgroundColor(ContextCompat.getColor(ProfileActivity.this, R.color.colorPrimary));

                    btnDeclineRequest.setVisibility(View.GONE);

                    Toast.makeText(ProfileActivity.this, currentProfileName  + " is now your friend.", Toast.LENGTH_SHORT).show();

                } else {
                    String error = databaseError.getMessage();
                    Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                }

                //enable sent request button
                btnProfileSendRequest.setEnabled(true);
            }
        });

        //}
    }

    /**
     * delete the request sent to a user
     */
    private void cancelRequestFriendState() {
        //-------------------------------  CANCEL REQUEST FRIENDS STATE ----------------------------------------------------
        //if(mCurrentState.equals("requestSent")) {
        Log.d(TAG, "cancelRequestFriendState: started");

            //disable sent request button
            btnProfileSendRequest.setEnabled(false);


            //remove the sent request from database
            mFriendRequestDatabase.child(mCurrentUser.getUid())
                    .child(profileUserID)
                    .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "onSuccess: sent request deleted from database for " + profileUserID);

                    //removing the received from the profile user also
                    mFriendRequestDatabase.child(profileUserID)
                            .child(mCurrentUser.getUid())
                            .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "onSuccess: received request deleted from database for " + profileUserID);

                            //enable sent request button
                            btnProfileSendRequest.setEnabled(true);
                            mCurrentState = "notFriends";
                            btnProfileSendRequest.setText("Send Friend Request");
                            btnProfileSendRequest.setBackgroundColor(ContextCompat.getColor(ProfileActivity.this, R.color.colorPrimary));

                            btnDeclineRequest.setVisibility(View.GONE);

                            Toast.makeText(ProfileActivity.this, "Request friend for " + mProfileName.getText().toString()  + " canceled", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        //}
    }

    /**
     * sent friend request to an user that is not a friend
     */
    private void notFriendState() {
        //--------------------------- NOT FRIENDS STATE -------------------------------------
        Log.d(TAG, "notFriendState: started : ");

        //if(mCurrentState.equals("notFriends")) {

        //disable sent request button
        btnProfileSendRequest.setEnabled(false);
        btnProfileSendRequest.setBackgroundColor(ContextCompat.getColor(ProfileActivity.this, R.color.colorPrimaryDark));

        //push will create a random id for the notification
        DatabaseReference newNotificationRef = mRootRef.child("notifications")
                .child(profileUserID).push();
        //get the notification ID (key)
        String newNotificationId = newNotificationRef.getKey();

        HashMap<String, String> notificationData = new HashMap<>();
        notificationData.put("from", mCurrentUser.getUid());
        notificationData.put("type", "request");

        Map requestMap = new HashMap();
        requestMap.put("friend_request/" + mCurrentUser.getUid() + "/" + profileUserID + "/request_type", "sent");
        requestMap.put("friend_request/" + profileUserID + "/" + mCurrentUser.getUid() + "/request_type", "received");
        requestMap.put("notifications/" + profileUserID + "/" + newNotificationId, notificationData);

        mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                Log.d(TAG, "onComplete: trying to add friend request and notifications to database: ");

                if(databaseError != null) {
                    Log.d(TAG, "onComplete: something went wrong: " + databaseError.getMessage());
                    Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }

                //enable sent request button
                btnProfileSendRequest.setEnabled(true);
                mCurrentState = "requestSent";
                btnProfileSendRequest.setText(R.string.cancelFriendRequest);

                Toast.makeText(ProfileActivity.this, "Friend Request sent to " + mProfileName.getText().toString(), Toast.LENGTH_SHORT).show();
            }
        });
        //}
    }

    private void friendRequestState() {
        Log.d(TAG, "friendRequestState: started: ");

        //------------------ FRIENDS LIST / REQUEST FEATURE -----------------------
        mFriendRequestDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: check request type received or sent and set button text: ");

                if(dataSnapshot.hasChild(profileUserID)) {
                    String requestType = dataSnapshot.child(profileUserID)
                            .child("request_type")
                            .getValue()
                            .toString();
                    if(requestType.equals("received")) {

                        mCurrentState = "requestReceived";
                        btnProfileSendRequest.setText(R.string.accepteFriendRequest);

                        btnDeclineRequest.setVisibility(View.VISIBLE);

                    } else if(requestType.equals("sent")) {
                        mCurrentState = "requestSent";
                        btnProfileSendRequest.setText(R.string.cancelFriendRequest);

                        btnDeclineRequest.setVisibility(View.GONE);
                    }

                    mProgressBar.setVisibility(View.GONE);

                } else {

                    mFriendshipDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Log.d(TAG, "onDataChange: check friendship and set button text to unfriend " + mProfileName.getText().toString());
                            if(dataSnapshot.hasChild(profileUserID)) {
                                mCurrentState = "friends";
                                btnProfileSendRequest.setText("Unfriend " + mProfileName.getText().toString());

                                btnDeclineRequest.setVisibility(View.GONE);
                            }

                            mProgressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d(TAG, "onCancelled: ERROR check friendship and set button text to unfriend: ");
                            mProgressBar.setVisibility(View.GONE);

                        }
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: ERROR checking request type received or sent and set button text:");
                mProgressBar.setVisibility(View.GONE);

            }
        });
    }

    private void getUserData(final String profileUserID) {
        Log.d(TAG, "getUserData: started: ");

        mDatabase = FirebaseDatabase.getInstance();
        myRef = mDatabase.getReference().child("users").child(profileUserID);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: getting user data from database: " + dataSnapshot.getValue().toString());
                String name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                mProfileName.setText(name);
                mProfileStatus.setText(status);
                if(!image.equals("default")) {
                    Picasso.get().load(image)
                            .placeholder(R.drawable.defaultphoto)
                            .into(mProfileImage);
                }

                setupWidgets();
                friendRequestState();
                Log.d(TAG, "getUserData: mCurrentState value: " + mCurrentState);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
