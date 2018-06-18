package com.logistics.alucard.alucardlogistics_chat;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private static final String TAG = "MessageAdapter";

    //vars
    private List<Messages> mMessageList;

    //firebase
    DatabaseReference mUserDatabase;

    public MessageAdapter(List<Messages> mMessageList) {
        Log.d(TAG, "MessageAdapter: created");
        this.mMessageList = mMessageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: inflating the viewHolder");

        View mView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_single_layout, parent, false);

        return new MessageViewHolder(mView);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: attaching data to viewHolder");

        String mCurrentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Messages message = mMessageList.get(position);

        String fromUser = message.getFrom();
        String messageType = message.getType();

        //if the user who sent the message is the current user
        if(fromUser.equals(mCurrentUserID)) {
            holder.messageText.setBackgroundColor(Color.LTGRAY);
            holder.messageText.setTextColor(Color.BLACK);
        } else {
            holder.messageText.setBackgroundResource(R.drawable.message_text_background);
            holder.messageText.setTextColor(Color.WHITE);
        }

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(fromUser);

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: getting user data");
                String name = dataSnapshot.child("name").getValue().toString();
                String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();

                holder.displayName.setText(name);
                Picasso.get().load(thumbImage).placeholder(R.drawable.defaultphoto).into(holder.profileImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: error adding data " + databaseError.getMessage());

            }
        });

        //determine the type of the message
        if(messageType.equals("text")) {
            holder.messageText.setText(message.getMessage());
            holder.messageImage.setVisibility(View.INVISIBLE);
        } else if(messageType.equals("image")) {
            holder.messageText.setVisibility(View.INVISIBLE);
            Picasso.get().load(message.getMessage()).placeholder(R.drawable.defaultphoto).into(holder.messageImage);
            Log.d(TAG, "onBindViewHolder: Imageurl: " + message.getMessage());
        }



        //holder.timeText.setText(message.getTime());


    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }


    public class MessageViewHolder extends RecyclerView.ViewHolder {

        public TextView messageText, timeText, displayName;
        public CircleImageView profileImage;
        public ImageView messageImage;

        public MessageViewHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.message_text_layout);
            //timeText = itemView.findViewById(R.id.time_text_layout);
            profileImage = itemView.findViewById(R.id.message_profile_layout);
            messageImage = itemView.findViewById(R.id.message_image_layout);
            displayName = itemView.findViewById(R.id.name_text_layout);
        }

    }
}
