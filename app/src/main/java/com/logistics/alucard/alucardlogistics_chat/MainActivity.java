package com.logistics.alucard.alucardlogistics_chat;

import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference mUserRef;


    private Toolbar mTollbar;

    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private TabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);





        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() != null) {
            mUserRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(mAuth.getCurrentUser().getUid());
        }


        //toolbar
        mTollbar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mTollbar);
        getSupportActionBar().setTitle(R.string.alucard_chat);

        //viewPager Tabs
        mViewPager = findViewById(R.id.main_page_viewPager);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);

        //connecting the tabLayout to the ViewPager
        mTabLayout = findViewById(R.id.main_tabLayout);
        mTabLayout.setTabTextColors(getResources().getColor(R.color.colorWhite), getResources().getColor(R.color.colorPressed));
        mTabLayout.setupWithViewPager(mViewPager);

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
        if(currentUser != null ) {
            mUserRef.child("online").setValue(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        FirebaseUser mCurrentUser = mAuth.getCurrentUser();
        if(mCurrentUser != null ) {
            Log.d(TAG, "onPause: status offline");
            mUserRef.child("online").setValue(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser mCurrentUser = mAuth.getCurrentUser();
        if(mCurrentUser != null ) {
            Log.d(TAG, "onStop: set TimeStamp");
            mUserRef.child("last_seen").setValue(ServerValue.TIMESTAMP);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.mainLogOut:
                FirebaseAuth.getInstance().signOut();
                updateUI(currentUser);
                mUserRef.child("online").setValue(false);
                Toast.makeText(this, "You have been Signed Out.", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.mainAccountSettings:
                Intent accountSettingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(accountSettingsIntent);
                Toast.makeText(this, "Account Settings", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.mainAllUsers:
                Intent usersList = new Intent(MainActivity.this, UsersActivity.class);
                startActivity(usersList);
                Toast.makeText(this, "All Users", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * check for current user and will send to main page if not
     * user will be sent to account creation
     * @param currentUser
     */
    private void updateUI(FirebaseUser currentUser) {
        if(currentUser == null) {
            Log.d(TAG, "updateUI: user is null");
            Intent startIntent = new Intent(MainActivity.this, StartActivity.class);
            startActivity(startIntent);
            finish();
        } else {
            Log.d(TAG, "updateUI: current user: " + currentUser.getUid());
        }
    }
}
