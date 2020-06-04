package com.example.whatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;


public class FindFriendsActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView findFriendsRecyclerlist;

    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        findFriendsRecyclerlist=(RecyclerView)findViewById(R.id.find_friends_recycler_list);
        findFriendsRecyclerlist.setLayoutManager(new LinearLayoutManager(this));

        userRef= FirebaseDatabase.getInstance().getReference().child("Users");


        mToolbar=(Toolbar) findViewById(R.id.find_friends_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Find Friends");
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options=new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(userRef, Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts, ContactViewHolder> adapter=new FirebaseRecyclerAdapter<Contacts, ContactViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ContactViewHolder holder, final int position, @NonNull Contacts model) {

                holder.username.setText(model.getName());
                holder.userStatus.setText(model.getStatus());

                Picasso.get().load(model.getImage()).placeholder(R.drawable.profile_image).into(holder.profileImage);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String visit_user_id=getRef(position).getKey();

                        Intent profileIntent=new Intent(FindFriendsActivity.this,ProfileActivity.class);
                        profileIntent.putExtra("visit_user_id",visit_user_id);
                        startActivity(profileIntent);
                    }
                });

            }

            @NonNull
            @Override
            public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout,parent,false);
                ContactViewHolder viewHolder=new ContactViewHolder(view);
                return viewHolder;
            }
        };

        findFriendsRecyclerlist.setAdapter(adapter);
        adapter.startListening();
    }
}
