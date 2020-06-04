package com.example.whatapp;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;


/**
 * A simple {@link Fragment} subclass.
 */
public class ContactFragment extends Fragment {

    private View contactView;
    private RecyclerView myContactList;
    private DatabaseReference contactRef,userRef;
    private FirebaseAuth mAuth;
    private String currentUserID;


    public ContactFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        contactView= inflater.inflate(R.layout.fragment_contact, container, false);

        myContactList=(RecyclerView)contactView.findViewById(R.id.contactslist);
        myContactList.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth=FirebaseAuth.getInstance();
        currentUserID=mAuth.getCurrentUser().getUid();

        contactRef= FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        userRef= FirebaseDatabase.getInstance().getReference().child("Users");




        return contactView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions options=new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(contactRef,Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts,ContactViewHolder> adapter=new FirebaseRecyclerAdapter<Contacts, ContactViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ContactViewHolder holder, int position, @NonNull Contacts model) {

                String userID=getRef(position).getKey();

                userRef.child(userID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                       if (dataSnapshot.exists()){


                           if (dataSnapshot.child("userState").hasChild("state")){

                               String state =dataSnapshot.child("userState").child("state").getValue().toString();
                               String time =dataSnapshot.child("userState").child("time").getValue().toString();
                               String date =dataSnapshot.child("userState").child("date").getValue().toString();

                               if (state.equals("online")){

                                   holder.onlineIcon.setVisibility(View.VISIBLE);
                               }
                               else if (state.equals("offline")){

                                   holder.onlineIcon.setVisibility(View.INVISIBLE);
                               }
                           }
                           else {
                               holder.onlineIcon.setVisibility(View.INVISIBLE);
                           }


                           //check if there is image since its optional
                           if(dataSnapshot.hasChild("image")){

                               String userImage=dataSnapshot.child("image").getValue().toString();
                               String profileName=dataSnapshot.child("name").getValue().toString();
                               String profileStatus=dataSnapshot.child("status").getValue().toString();

                               holder.username.setText(profileName);
                               holder.userStatus.setText(profileStatus);
                               Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(holder.profileImage);
                           }

                           else {

                               String profileName=dataSnapshot.child("name").getValue().toString();
                               String profileStatus=dataSnapshot.child("status").getValue().toString();

                               holder.username.setText(profileName);
                               holder.userStatus.setText(profileStatus);

                           }
                       }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
               //format of display
                View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout,parent,false);
                ContactViewHolder viewHolder=new ContactViewHolder(view);
                return viewHolder;
            }
        };

        myContactList.setAdapter(adapter);
        adapter.startListening();
    }
}
