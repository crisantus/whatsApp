package com.example.whatapp;


import android.content.Intent;
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
public class ChatFragment extends Fragment {

    private View privateChatView;
    private RecyclerView chatList;
    private DatabaseReference chatsReference,userRef;
    private FirebaseAuth mAuth;
    private String currentUserID;



    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
      privateChatView =inflater.inflate(R.layout.fragment_chat, container, false);

      chatList=(RecyclerView)privateChatView.findViewById(R.id.chats_list);
      chatList.setLayoutManager(new LinearLayoutManager(getContext()));

      mAuth=FirebaseAuth.getInstance();
      currentUserID=mAuth.getCurrentUser().getUid();

      chatsReference= FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        userRef= FirebaseDatabase.getInstance().getReference().child("Users");


      return privateChatView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options =new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(chatsReference,Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts,ChatViewHolder> adapter=new FirebaseRecyclerAdapter<Contacts, ChatViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatViewHolder holder, int position, @NonNull Contacts model) {

                final String user_id=getRef(position).getKey();
                final String[] retImage = {"default image"};

                userRef.child(user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()){
                            if (dataSnapshot.hasChild("image")){
                                  retImage[0] =dataSnapshot.child("image").getValue().toString();
                                Picasso.get().load(retImage[0]).into(holder.profileImage);
                            }

                            final String retName=dataSnapshot.child("name").getValue().toString();
                            final String retStatus=dataSnapshot.child("status").getValue().toString();

                            holder.username.setText(retName);
                            holder.userStatus.setText("Last Seen: "+ "\n"+ "Date " + " Time");

                            if (dataSnapshot.child("userState").hasChild("state")){

                                String state =dataSnapshot.child("userState").child("state").getValue().toString();
                                String time =dataSnapshot.child("userState").child("time").getValue().toString();
                                String date =dataSnapshot.child("userState").child("date").getValue().toString();

                                if (state.equals("online")){

                                    holder.userStatus.setText("online");
                                }
                                else if (state.equals("offline")){

                                    holder.userStatus.setText("Last Seen: "+ date + " "+time);
                                }
                            }
                            else {
                                holder.userStatus.setText("offline");
                            }

                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent chatIntent=new Intent(getContext(),ChatActivity.class);
                                    chatIntent.putExtra("visit_user_id",user_id);
                                    chatIntent.putExtra("visit_userName",retName);
                                    chatIntent.putExtra("visit_image", retImage[0]);
                                    startActivity(chatIntent);
                                }
                            });

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout,parent,false);
                ChatViewHolder viewHolder=new ChatViewHolder(view);
                return viewHolder;

            }
        };

        chatList.setAdapter(adapter);
        adapter.startListening();


    }


}
