package com.logistics.alucard.alucardlogistics_chat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


public class ChatsFragment extends Fragment {
    private static final String TAG = "ChatsFragment";

    //widgets
    private RecyclerView mConvList;
    private View mMainView;

    //firebase
    private DatabaseReference mConvDatabase;
    private DatabaseReference mMessageDatabase;
    private DatabaseReference mUserDatabase;
    private FirebaseAuth mAuth;

    //adapter
    private FirebaseRecyclerAdapter adapter;

    //vars
    private String mCurrentUserID;
    private String userName;

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: started: ");
        
       mMainView = inflater.inflate(R.layout.fragment_chats, container, false);

       //--------------- widgets
       mConvList = mMainView.findViewById(R.id.conv_list);

       //--------------- firebase
        mAuth = FirebaseAuth.getInstance();
        mCurrentUserID = mAuth.getCurrentUser().getUid();
        mConvDatabase = FirebaseDatabase.getInstance().getReference().child("chat").child(mCurrentUserID);
        mConvDatabase.keepSynced(true); //? not sure if i want this

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        mUserDatabase.keepSynced(true);

        mMessageDatabase = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrentUserID);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mConvList.setHasFixedSize(true); //???
        mConvList.setLayoutManager(linearLayoutManager);

        fireBaseRecyclerAdapter();
        mConvList.setAdapter(adapter);

       return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: started");
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: started ");
        adapter.stopListening();
    }

    private void fireBaseRecyclerAdapter()  {
        Log.d(TAG, "fireBaseRecyclerAdapter: started");

        //enable prevent double click on items
        //holder.mView.setEnabled(true);

        Query conversationQuery = mConvDatabase.orderByChild("time_stamp");

        FirebaseRecyclerOptions<Conversation> options =
                new FirebaseRecyclerOptions.Builder<Conversation>()
                        .setQuery(conversationQuery, Conversation.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Conversation, ConvViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ConvViewHolder holder, int position, @NonNull final Conversation conversation) {
                Log.d(TAG, "onBindViewHolder: setting the user data: ");

                final String userID = getRef(position).getKey();


                Query lastMessageQuery = mMessageDatabase.child(userID).limitToLast(1);
                Log.d(TAG, "onBindViewHolder: *************lastMessageQuery*********** is " + lastMessageQuery.getRef().getKey());

                lastMessageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        String data = dataSnapshot.child("message").getValue().toString();
                        String dataType = dataSnapshot.child("type").getValue().toString();
                        if(dataType.equals("text")) {
                            holder.setMessage(data, conversation.isSeen());
                        } else {
                            holder.setMessage("New photo sent.", false);
                        }
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

                mUserDatabase.child(userID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        userName = dataSnapshot.child("name").getValue().toString();
                        String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();

                        if(dataSnapshot.hasChild("online")) {
                            boolean userOnline = (boolean) dataSnapshot.child("online").getValue();
                            Log.d(TAG, "onDataChange: online status is " + userOnline);
                            holder.setOnlineStatus(userOnline);

                        }

                        holder.setName(userName);
                        holder.setUserImage(thumbImage);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                //go to chat page
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //disable prevent double click on items
                        //holder.mView.setEnabled(false);
                        Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                        chatIntent.putExtra(getString(R.string.userID), userID);
                        chatIntent.putExtra(getString(R.string.profileUserName), userName);
                        startActivity(chatIntent);
                    }
                });

                Log.d(TAG, "onBindViewHolder: finished setting data");

            }

            @NonNull
            @Override
            public ConvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                Log.d(TAG, "onCreateViewHolder: create users view holder: ");
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_users_model, parent, false);
                return new ConvViewHolder(view);
            }
        };

    }

    public static class ConvViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public ConvViewHolder(View itemView) {
            super(itemView);

            mView = itemView;

        }

        public void setMessage(String message, boolean isSeen){

            TextView userStatusView = mView.findViewById(R.id.tvUserStatus);
            userStatusView.setText(message);

            if(!isSeen){
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.BOLD);
            } else {
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.NORMAL);
            }

        }

        public void setName(String name){

            TextView userNameView = mView.findViewById(R.id.tvUserName);
            userNameView.setText(name);

        }

        public void setUserImage(String thumb_image){

            CircleImageView userImageView = mView.findViewById(R.id.circle_profile_image);
            Picasso.get().load(thumb_image).placeholder(R.drawable.defaultphoto).into(userImageView);

        }

        public void setOnlineStatus(Boolean onlineStatus) {
            ImageView userOnline = mView.findViewById(R.id.onlineStatus);
            if(onlineStatus == true) {
                userOnline.setImageResource(R.drawable.green_dot_online);
                userOnline.setVisibility(View.VISIBLE);
            } else if(onlineStatus == false) {
                userOnline.setImageResource(R.drawable.gray_dot_offline);
                userOnline.setVisibility(View.VISIBLE);
            } else {
                userOnline.setVisibility(View.GONE);
            }
        }

    }

}
