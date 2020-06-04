package com.example.whatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class ImageViewActivity extends AppCompatActivity {

    private ImageView mImageView;
    private String imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        mImageView=(ImageView)findViewById(R.id.image_viewer);
        imageUrl=getIntent().getStringExtra("url");

        Picasso.get().load(imageUrl).into(mImageView);
    }
}
