package com.logistics.alucard.alucardlogistics_chat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private static final int GALLERY_PICK = 1;

    //firebase
    private DatabaseReference mRootRef, mUserOnlineRef;
    private FirebaseUser mCurrentUser;
    private FirebaseAuth mAuth;
    private StorageReference mImageStorage;

    //vars
    private String mChatUserID, mChatUserName, mThumbImage,  mCurrentUserID;
    private List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;
    private MessageListAdapter mNewAdapter;
    //pagination vars
    private int mCurrentPage = 1; // how many messages to load per pagination
    private int itemPos = 0;
    private String mLastKey = ""; //takes the last message loaded key
    private String mPervKey = "";
    private String mFirstKey = "";

    //widgets
    private Toolbar mChatToolbar;
    private TextView mTitleView, mLastSeenView;
    private CircleImageView mProfileImage;
    private ImageButton mChatAddBtn, mChatSendBtn;
    private EditText mChatMessageView;
    private RecyclerView mMessageList;
    private SwipeRefreshLayout mRefreshLayout;
    private Context mContext;
    //private MessageListAdapter.OnCustomItemClickListener onCustomItemClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // ------------------- toolbar setup

        mChatUserID = getIntent().getStringExtra(getString(R.string.userID));
        mChatUserName = getIntent().getStringExtra(getString(R.string.profileUserName));
        mThumbImage = getIntent().getStringExtra(getString(R.string.profileThumbImage));

        mChatToolbar = findViewById(R.id.chat_appbar_layout);
        setSupportActionBar(mChatToolbar);
        //getSupportActionBar().setTitle(mChatUser);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = inflater.inflate(R.layout.chat_custom_bar, null);
        actionBar.setCustomView(actionBarView);

        mTitleView = findViewById(R.id.custom_bar_title);
        mLastSeenView = findViewById(R.id.custom_bar_seen);
        mProfileImage = findViewById(R.id.custom_bar_image);

        mTitleView.setText(mChatUserName);
        Picasso.get().load(mThumbImage).into(mProfileImage);

        //--------------------- widgets setup

        mChatAddBtn = findViewById(R.id.btnAddChat);
        mChatSendBtn = findViewById(R.id.btnSendChat);
        mChatMessageView = findViewById(R.id.chat_message_view);

        //mAdapter = new MessageAdapter(messagesList);
        mNewAdapter = new MessageListAdapter(messagesList, ChatActivity.this);

        mMessageList = findViewById(R.id.messages_list);
        mLinearLayout = new LinearLayoutManager(this);

        //mMessageList.setHasFixedSize(true);
        mMessageList.setLayoutManager(mLinearLayout);

        mMessageList.setAdapter(mNewAdapter);


        mRefreshLayout = findViewById(R.id.message_swipe_layout);

        //-------------------- firebase setup

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserOnlineRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mCurrentUser.getUid());

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mCurrentUserID = mAuth.getCurrentUser().getUid();

        //---------------  Image storage
        mImageStorage = FirebaseStorage.getInstance().getReference();

        loadMessages();
        //setting the seen feature to true
        mRootRef.child("chat").child(mCurrentUserID).child(mChatUserID).child("seen").setValue(true);

        //setup last seen feature in chat app bar
        mRootRef.child("users").child(mChatUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String online = dataSnapshot.child("online").getValue().toString();
                Log.d(TAG, "onDataChange: Online status is: " + online);
                String timeStamp = dataSnapshot.child("last_seen").getValue().toString();
                long timeAgo = Long.parseLong(timeStamp);
                if(online.equals("true")) {
                    mLastSeenView.setText("Online");
                } else {
                    mLastSeenView.setText(GetTimeAgo.getTimeAgo(timeAgo, getApplicationContext()));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //create chat node in database
        mRootRef.child("chat").child(mCurrentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: create the chat node");
                if(!dataSnapshot.hasChild(mChatUserID)) {
                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("time_stamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("chat/" + mCurrentUserID + "/" + mChatUserID, chatAddMap);
                    chatUserMap.put("chat/" + mChatUserID + "/" + mCurrentUserID, chatAddMap);

                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            Log.d(TAG, "onComplete: add the data to the chat node");

                            if(databaseError != null) {
                                Log.d(TAG, "onComplete: errors found: " + databaseError.getMessage());
                            }

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //setup send button
        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: sending message");
                sendMessage();
                mUserOnlineRef.child("last_seen").setValue(ServerValue.TIMESTAMP);
            }
        });

        //share image button
        mChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: selecting a picture to send");
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);
            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "onRefresh: started mLastKey is" + mLastKey + "and mPervKey is " + mPervKey);


                if(!mPervKey.equals(mFirstKey)) {

                    mCurrentPage++;

                    itemPos = 0;

                    loadMoreMessages();
                } else {
                    //hide the refresh icon
                    mRefreshLayout.setRefreshing(false);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: started");

        if(requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            Log.d(TAG, "onActivityResult: ready to upload");

            Uri imageUri = data.getData();

            final String currentUserRef = "messages/" + mCurrentUserID + "/" + mChatUserID;
            final String chatUserRef = "messages/" + mChatUserID  + "/" + mCurrentUserID;

            DatabaseReference userMessagePush = mRootRef.child("messages")
                    .child(mCurrentUserID).child(mChatUserID).push();

            final String pushID = userMessagePush.getKey();

            StorageReference filePath = mImageStorage.child("message_images").child(pushID + ".jpg");
            filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    Log.d(TAG, "onComplete: attempt to upload message image to storage");

                    if(task.isSuccessful()) {
                        Log.d(TAG, "onComplete: message image uploaded to storage");
                        String downloadUrl = task.getResult().getDownloadUrl().toString();

                        Map messageMap = new HashMap();
                        messageMap.put("message_id", pushID);
                        messageMap.put("message", downloadUrl);
                        messageMap.put("seen", false);
                        messageMap.put("type", "image");
                        messageMap.put("time", ServerValue.TIMESTAMP);
                        messageMap.put("from", mCurrentUserID);
                        messageMap.put("to", mChatUserID);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(currentUserRef + "/" + pushID, messageMap);
                        messageUserMap.put(chatUserRef + "/" + pushID, messageMap);

                        mChatMessageView.setText("");

                        mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null) {
                                    Log.d(TAG, "onComplete: there was an error: " + databaseError.getMessage());
                                }
                            }
                        });
                    }

                }
            });


        }

    }

    private void loadMoreMessages() {
        Log.d(TAG, "loadMoreMessages: started");

        DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserID).child(mChatUserID);

        //load messages by key by specifying how many to load and provide endAt last message loaded key
        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(7);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Messages message = dataSnapshot.getValue(Messages.class);
                String messageKey = dataSnapshot.getKey();


                if(!mPervKey.equals(messageKey)) {
                    messagesList.add(itemPos++, message);
                } else {
                    mPervKey = mLastKey;
                    mFirstKey = messageKey;

                }

                if(itemPos == 1) {
                    mLastKey = messageKey;
                }

                Log.d("TOTALKEYS", "Last Key : " + mLastKey + " | Prev Key : " + mPervKey + " | Message Key : " + messageKey + " | First Key : " + mFirstKey);

                mNewAdapter.notifyDataSetChanged();

                //start at the bottom of the recycle view when loaded
                //mMessageList.scrollToPosition(messagesList.size() -1);

                //hide the refresh icon
                mRefreshLayout.setRefreshing(false);

                mLinearLayout.scrollToPositionWithOffset(6, 0);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void loadMessages() {
        Log.d(TAG, "loadMessages: started:");

        DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserID).child(mChatUserID);

        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Messages message = dataSnapshot.getValue(Messages.class);

                itemPos++;

                if(itemPos == 1) {
                    String messageKey = dataSnapshot.getKey();
                    Log.d(TAG, "onChildAdded: messageKey" + messageKey);
                    mLastKey = messageKey;
                    mPervKey = messageKey;
                }
                messagesList.add(message);
                mNewAdapter.notifyDataSetChanged();

                //start at the bottom of the recycle view when loaded
                mMessageList.scrollToPosition(messagesList.size() -1);

                //hide the refresh icon
                mRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage() {
        Log.d(TAG, "sendMessage: started");
        String message = mChatMessageView.getText().toString();

        if(!TextUtils.isEmpty(message)) {

            String currentUserRef = "messages/" + mCurrentUserID + "/" + mChatUserID;
            String chatUserRef = "messages/" + mChatUserID + "/" + mCurrentUserID;

            DatabaseReference mUserMessagePush = mRootRef.child("messages")
                    .child(mCurrentUserID).child(mChatUserID).push();

            String pushId = mUserMessagePush.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message_id", pushId);
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserID);
            messageMap.put("to", mChatUserID);

            Map messageUserMap = new HashMap();
            messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
            messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

            mChatMessageView.setText("");

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError != null) {
                        Log.d(TAG, "onComplete: something went wrong " + databaseError.getMessage());
                    }
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: set status online");
        if(mCurrentUser != null ) {
            Log.d(TAG, "onStart: status is online");
            mUserOnlineRef.child("online").setValue(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: set status offline");
        if(mCurrentUser != null ) {
            mUserOnlineRef.child("online").setValue(false);
            mUserOnlineRef.child("last_seen").setValue(ServerValue.TIMESTAMP);
        }
    }
}
