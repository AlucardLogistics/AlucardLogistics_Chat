package com.logistics.alucard.alucardlogistics_chat;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {

    private static final String TAG = "UsersActivity";

    //widgets
    private Toolbar mToolbar;
    private RecyclerView mUsersList;

    //firebase
    private DatabaseReference mDatabaseReference, mUserOnlineRef;
    FirebaseUser mCurrentUser;

    //adapter
    private FirebaseRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);
        Log.d(TAG, "onCreate: started: ");

        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserOnlineRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mCurrentUser.getUid());

        mToolbar = findViewById(R.id.all_users_appbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("All Users");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mUsersList = findViewById(R.id.recycle_users_list);
        mUsersList.setHasFixedSize(true);
        mUsersList.setLayoutManager(new LinearLayoutManager(this));

        fireBaseRecyclerAdapter();
        mUsersList.setAdapter(adapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: started: ");
        adapter.startListening();
        if(mCurrentUser != null ) {
            mUserOnlineRef.child("online").setValue(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: started: ");
        adapter.stopListening();
    }

    private void fireBaseRecyclerAdapter()  {
        FirebaseRecyclerOptions<UsersModel> options =
                new FirebaseRecyclerOptions.Builder<UsersModel>()
                        .setQuery(mDatabaseReference, UsersModel.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<UsersModel, UsersViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final UsersViewHolder holder, int position, @NonNull UsersModel users) {
                Log.d(TAG, "onBindViewHolder: setting the user data: ");
                //enable prevent double click on items
                //holder.mView.setEnabled(true);
                holder.setName(users.getName());
                holder.setStatus(users.getStatus());
                holder.setImage(users.getThumb_image());
                holder.setOnlineStatus(users.getOnline());



                final String userID = getRef(position).getKey();

                //go to users profile page
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //disable prevent double click on items
                        //holder.mView.setEnabled(false);
                        Intent profileIntent = new Intent(UsersActivity.this, ProfileActivity.class);
                        profileIntent.putExtra(getString(R.string.userID), userID);
                        startActivity(profileIntent);
                    }
                });

                Log.d(TAG, "onBindViewHolder: finished setting data");

            }

            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                Log.d(TAG, "onCreateViewHolder: create users view holder: ");
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_users_model, parent, false);
                return new UsersViewHolder(view);
            }
        };

    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public UsersViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setName(String name) {
            TextView userNameView = mView.findViewById(R.id.tvUserName);
            userNameView.setText(name);
        }

        public void setStatus(String status) {
            TextView userStatus = mView.findViewById(R.id.tvUserStatus);
            userStatus.setText(status);
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
