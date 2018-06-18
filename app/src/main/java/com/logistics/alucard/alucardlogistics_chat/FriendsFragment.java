package com.logistics.alucard.alucardlogistics_chat;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {
    private static final String TAG = "FriendsFragment";

    private RecyclerView mFriendsList;

    private DatabaseReference mFriendsDatabase, mUsersDatabase;
    private FirebaseAuth mAuth;

    private String mCurrentUserID;

    private View mMainView;

    //adapter
    private FirebaseRecyclerAdapter adapter;

    public FriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: friends fragment started");
        // Inflate the layout for this fragment
        mMainView = inflater.inflate(R.layout.fragment_friends, container, false);


        mAuth = FirebaseAuth.getInstance();

        mCurrentUserID = mAuth.getCurrentUser().getUid();

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        //firebase offline feature - load once keep in memory
        mUsersDatabase.keepSynced(true);
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("friendships").child(mCurrentUserID);
        //firebase offline feature - load once keep in memory
        mFriendsDatabase.keepSynced(true);

        mFriendsList = mMainView.findViewById(R.id.friends_list);
        mFriendsList.setHasFixedSize(true);
        mFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));

        fireBaseRecyclerAdapter();
        mFriendsList.setAdapter(adapter);

        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: adapter is listening");
        adapter.startListening();

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: adapter stopped listening");
        adapter.stopListening();
    }

    private void fireBaseRecyclerAdapter()  {
        FirebaseRecyclerOptions<Friendships> options =
                new FirebaseRecyclerOptions.Builder<Friendships>()
                        .setQuery(mFriendsDatabase, Friendships.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Friendships, FriendshipsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendshipsViewHolder holder, int position, @NonNull Friendships friendships) {
                Log.d(TAG, "onBindViewHolder: setting the user data: ");
                //enable prevent double click on items
                holder.mView.setEnabled(true);
                holder.setDate(friendships.getDate());

                final String listUserID = getRef(position).getKey();
                mUsersDatabase.child(listUserID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "onDataChange: getting user fields values");

                        final String displayName = dataSnapshot.child("name").getValue().toString();
                        final String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();

                        holder.setName(displayName);
                        holder.setImage(thumbImage);

                        if(dataSnapshot.hasChild("online")) {
                            Boolean userOnline = (boolean) dataSnapshot.child("online").getValue();
                            holder.setOnlineStatus(userOnline);
                        }
                        Log.d(TAG, "onDataChange: finished loading user fields");

                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //options menu
                                CharSequence options[] = new CharSequence[]{"Open Profile", "Send Message"};
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setTitle("Select Options");
                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int position) {
                                        switch (position) {
                                            case 0: //option 1 - send to profile of the user clicked
                                                Intent profielIntent = new Intent(getContext(), ProfileActivity.class);
                                                profielIntent.putExtra(getString(R.string.userID), listUserID);
                                                startActivity(profielIntent);
                                                break;
                                            case 1: //option 2 - send to chat channel of current user and user clicked
                                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                                chatIntent.putExtra(getString(R.string.userID), listUserID);
                                                chatIntent.putExtra(getString(R.string.profileUserName), displayName);
                                                chatIntent.putExtra(getString(R.string.profileThumbImage), thumbImage);
                                                startActivity(chatIntent);
                                                break;
                                        }

                                    }
                                });

                                builder.show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public FriendshipsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                Log.d(TAG, "onCreateViewHolder: create users view holder: ");
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_friends_model, parent, false);
                return new FriendshipsViewHolder(view);
            }
        };

    }

    public static class FriendshipsViewHolder extends RecyclerView.ViewHolder {
        View mView;

        public FriendshipsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setDate(String date) {
            TextView userNameView = mView.findViewById(R.id.tvUserStatus);
            userNameView.setText(date);
        }

        public void setName(String name) {
            TextView displayName = mView.findViewById(R.id.tvUserName);
            displayName.setText(name);
        }

        public void setImage(String image) {
            CircleImageView userImage = mView.findViewById(R.id.circle_profile_image);
            Picasso.get().load(image).placeholder(R.drawable.defaultphoto).into(userImage);
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
