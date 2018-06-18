package com.logistics.alucard.alucardlogistics_chat;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class FullScreenImage extends AppCompatActivity {

    private ImageView ivFullScreen;
    private Button btnClose;

    private String imgUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ivFullScreen = findViewById(R.id.imgDisplay);
        btnClose = findViewById(R.id.btnClose);

        imgUrl = getIntent().getStringExtra("imgUrl");



        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FullScreenImage.this.finish();
            }
        });

        Picasso.get().load(imgUrl).into(ivFullScreen);



    }
}
