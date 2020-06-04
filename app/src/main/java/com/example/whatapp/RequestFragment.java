package com.example.whatapp;


import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
public class RequestFragment extends Fragment {

    private View requestFragmentView;
    private RecyclerView myRequestList;
    private DatabaseReference chatRequestRef,userRef,contactRef;
    private FirebaseAuth mAuth;
    private String currentUserID;


    public RequestFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        requestFragmentView= inflater.inflate(R.layout.fragment_request, container, false);

        mAuth=FirebaseAuth.getInstance();
        currentUserID=mAuth.getCurrentUser().getUid();

        myRequestList=(RecyclerView)requestFragmentView.findViewById(R.id.chat_request_list);
        myRequestList.setLayoutManager(new LinearLayoutManager(getContext()));

        chatRequestRef= FirebaseDatabase.getInstance().getReference().child("Chat Request");
        userRef= FirebaseDatabase.getInstance().getReference().child("Users");
        contactRef= FirebaseDatabase.getInstance().getReference().child("Contacts");


        return requestFragmentView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions options=new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(chatRequestRef.child(currentUserID),Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts,RequestViewHolder> adapter=new FirebaseRecyclerAdapter<Contacts, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestViewHolder holder, int position, @NonNull Contacts model) {

                holder.itemView.findViewById(R.id.request_accept_btn).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.VISIBLE);

                  //id of each user from the database LineByLine that have sent there friend request
                final String list_user_id=getRef(position).getKey();

                DatabaseReference getTypeRef=getRef(position).child("request_type").getRef(); //check whats inside the userID

                    //request_type is either received or sent.. we want only the "receive" request_type
                getTypeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            String type=dataSnapshot.getValue().toString();//type=request_type.getValue inside

                            if (type.equals("received")){

                                userRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.hasChild("image")){

                                            final String requestProfileImage=dataSnapshot.child("image").getValue().toString();

                                            Picasso.get().load(requestProfileImage).placeholder(R.drawable.profile_image).into(holder.profileImage);

                                        }
                                            final String requestUserNmae=dataSnapshot.child("name").getValue().toString();
                                            final String requestUserStatus=dataSnapshot.child("status").getValue().toString();

                                            holder.username.setText(requestUserNmae);
                                            holder.userStatus.setText("wants to connect with you");


                                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[]= new CharSequence[]{
                                                        "Accept",
                                                        "Cancel"
                                                };

                                                AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
                                                builder.setTitle(requestUserNmae + " Chat Request");
                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {

                                                        if (which==0){
                                                            //remove from the RequestList and add to contact list
                                                            contactRef.child(currentUserID).child(list_user_id).child("Contact")
                                                                    .setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()){
                                                                        contactRef.child(list_user_id).child(currentUserID).child("Contact")
                                                                                .setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if (task.isSuccessful()){
                                                                                    //remove from the  chatRequest
                                                                                    chatRequestRef.child(currentUserID).child(list_user_id)
                                                                                            .removeValue()
                                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                @Override
                                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                                    if (task.isSuccessful()){
                                                                                                        chatRequestRef.child(list_user_id).child(currentUserID)
                                                                                                                .removeValue()
                                                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                    @Override
                                                                                                                    public void onComplete(@NonNull Task<Void> task) {

                                                                                                                        if (task.isSuccessful()){
                                                                                                                            Toast.makeText(getContext(), "New Contact Saved", Toast.LENGTH_SHORT).show();
                                                                                                                        }
                                                                                                                    }
                                                                                                                });
                                                                                                    }
                                                                                                }
                                                                                            });

                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        if (which==1){
                                                               //just remove it from the chatRequest
                                                            chatRequestRef.child(currentUserID).child(list_user_id)
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()){
                                                                                chatRequestRef.child(list_user_id).child(currentUserID)
                                                                                        .removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                                                if (task.isSuccessful()){
                                                                                                    Toast.makeText(getContext(), "Contact Deleted", Toast.LENGTH_SHORT).show();
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        }
                                                                    });


                                                        }
                                                    }
                                                });
                                                builder.show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                            else if (type.equals("sent")){
                                Button request_sent_btn=holder.itemView.findViewById(R.id.request_accept_btn);
                                request_sent_btn.setText("Request Sent");

                                holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.INVISIBLE);


                                userRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.hasChild("image")){

                                            final String requestProfileImage=dataSnapshot.child("image").getValue().toString();

                                            Picasso.get().load(requestProfileImage).placeholder(R.drawable.profile_image).into(holder.profileImage);

                                        }
                                        final String requestUserNmae=dataSnapshot.child("name").getValue().toString();
                                        final String requestUserStatus=dataSnapshot.child("status").getValue().toString();

                                        holder.username.setText(requestUserNmae);
                                        holder.userStatus.setText("You have sent a request to "+ requestUserNmae);


                                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[]= new CharSequence[]{

                                                        "Cancel Chat Request"
                                                };

                                                AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
                                                builder.setTitle("Already sent request");
                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {

                                                        if (which==0){
                                                            //just remove it from the chatRequest
                                                            chatRequestRef.child(currentUserID).child(list_user_id)
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()){
                                                                                chatRequestRef.child(list_user_id).child(currentUserID)
                                                                                        .removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                                                if (task.isSuccessful()){
                                                                                                    Toast.makeText(getContext(), "You have cancel the chat request.", Toast.LENGTH_SHORT).show();
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        }
                                                                    });


                                                        }
                                                    }
                                                });
                                                builder.show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });


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
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout,parent,false);
                RequestViewHolder viewHolder=new RequestViewHolder(view);
                return viewHolder;

            }
        };

        myRequestList.setAdapter(adapter);
        adapter.startListening();
    }
}
