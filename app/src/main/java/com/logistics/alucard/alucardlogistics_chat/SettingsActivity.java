package com.logistics.alucard.alucardlogistics_chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final int GALLERY_PICK = 1;

    //firebase
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mRef, mUserOnlineRef;
    private FirebaseUser mCurrentUser;
    private StorageReference mImageStorage;

    //widgets
    private CircleImageView civThumb_image;
    private TextView tvDisplayName, tvStatus;
    private Button btnChangaImage, btnChangeStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        civThumb_image = findViewById(R.id.settings_image);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvStatus = findViewById(R.id.tvStatus);
        btnChangaImage = findViewById(R.id.btnChangeImage);
        btnChangeStatus = findViewById(R.id.btnChangeStatus);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mImageStorage = FirebaseStorage.getInstance().getReference();
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserOnlineRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mCurrentUser.getUid());

        Log.d(TAG, "onCreate: setting user data from database");
        setUserData();

        btnChangeStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String statusValue = tvStatus.getText().toString();

                Intent statusIntent = new Intent(SettingsActivity.this, StatusActivity.class);
                statusIntent.putExtra(getString(R.string.statusValue), statusValue);
                startActivity(statusIntent);
            }
        });

        btnChangaImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //intent to pick a photo from phone gallery
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);



                //start picker to get image for cropping and then use the image in cropping activity
//                CropImage.activity()
//                        .setGuidelines(CropImageView.Guidelines.ON)
//                        .start(SettingsActivity.this);


            }
        });


    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: started");
        super.onStart();
        setUserData();
        if(mCurrentUser != null ) {
            Log.d(TAG, "onStart: set online status to true");
            mUserOnlineRef.child("online").setValue(true);
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: started");
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onStop: set online status to false");
        if(mCurrentUser != null ) {
            mUserOnlineRef.child("online").setValue(false);
            mUserOnlineRef.child("last_seen").setValue(ServerValue.TIMESTAMP);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: Photo crop success: ");
        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            Uri imgURI = data.getData();
            // start cropping activity for pre-acquired image saved on the device
            CropImage.activity(imgURI)
                    .setAspectRatio(1, 1)
                    .setMinCropWindowSize(500, 500)
                    .start(this);

        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();

                File thumb_filepath = new File(resultUri.getPath());

                final String currentUserID = mCurrentUser.getUid();

                try {
                    //get the picture as bitmap from database
                    Bitmap thumbBitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(200)
                            .setQuality(50)
                            .compressToBitmap(thumb_filepath);

                    //convert/compress into an array to upload
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    final byte[] thumbByte = baos.toByteArray();


                    final StorageReference thumbBytePath = mImageStorage.child("profile_images").child("thumbs").child(currentUserID + ".jpg");

                    // current user id as name of profile img
                    StorageReference profileImagesPath = mImageStorage.child("profile_images").child(currentUserID + ".jpg");

                    profileImagesPath.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                            final String downloadImageUrl = taskSnapshot.getDownloadUrl().toString();
                            Log.d(TAG, "onSuccess: url to uploaded pic: " + downloadImageUrl);

                            //upload photo as thumb in storage under profile_images/thumbs/ directory
                            UploadTask uploadThumbFile = thumbBytePath.putBytes(thumbByte);
                            uploadThumbFile.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumbTask) {

                                    String thumbUrl = thumbTask.getResult().getDownloadUrl().toString();
                                    Log.d(TAG, "onComplete: thumb URL: " + thumbUrl);

                                    if(thumbTask.isSuccessful()) {
                                        Log.d(TAG, "onComplete: uploaded image file as photo and thumb file: " + taskSnapshot.getDownloadUrl().toString());

                                        //Map is used for update database where
                                        // HashMap will replace the node just with present data
                                        Map updatePhotoAndThumbUrl = new HashMap<>();
                                        updatePhotoAndThumbUrl.put("image", downloadImageUrl);
                                        updatePhotoAndThumbUrl.put("thumb_image", thumbUrl);

                                        //set image field in users node in the database
                                        mRef = mFirebaseDatabase.getReference().child("users").child(currentUserID);
                                        mRef.updateChildren(updatePhotoAndThumbUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()) {
                                                    Log.d(TAG, "onComplete: profile image link added to the photo and thumb_image field in database: ");
                                                }
                                            }
                                        });

                                    } else {
                                        Log.d(TAG, "onComplete: could not upload thumb file: ");
                                    }
                                }
                            });

                            Toast.makeText(SettingsActivity.this, "Profile image uploaded.", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "onFailure: Could not get the img URL: ");
                            Toast.makeText(SettingsActivity.this, "Profile image upload FAILED.", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (IOException e) {
                    Log.w(TAG, "onActivityResult: could not upload photo to storage: " + e.getMessage() );
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.d(TAG, "onActivityResult: Error uploading the file: " + error.getMessage());
            }


        }


    }

    private void setUserData() {
        Log.d(TAG, "setUserData: started: ");
        // Read from the database
        String userID = mCurrentUser.getUid();
        mRef = mFirebaseDatabase.getReference().child("users").child(userID);
        //mRef.keepSynced(true);
        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Log.d(TAG, "Value is: " + dataSnapshot.toString());

                if(dataSnapshot != null) {

                    String image = dataSnapshot.child("image").getValue().toString();
                    final String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();
                    String displayName = dataSnapshot.child("name").getValue().toString();
                    String status = dataSnapshot.child("status").getValue().toString();


                    tvDisplayName.setText(displayName);
                    tvStatus.setText(status);

                    if (!image.equals("default")) {
                        //Picasso.get().load(thumb_image).placeholder(R.drawable.defaultphoto).into(civThumb_image);
                        //Picasso get image and store it for offline feature
                        Picasso.get().load(thumb_image)
                                .networkPolicy(NetworkPolicy.OFFLINE)
                                .placeholder(R.drawable.defaultphoto)
                                .into(civThumb_image, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "onSuccess: load profile pic offline: ");

                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Log.d(TAG, "onError: load profile picture from fire base: ");
                                        // if there is no image offline onSuccess(), it will download it from database onError
                                        Picasso.get().load(thumb_image).placeholder(R.drawable.defaultphoto).into(civThumb_image);
                                    }
                                });
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
}
