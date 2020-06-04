package com.example.whatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserID,current_state, senderUserID;
    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button sendMessageRequestBtn,declineMessageRequestBtn;
    private DatabaseReference userRef,chatRequestRef,contactRef,notificationRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        receiverUserID=getIntent().getExtras().get("visit_user_id").toString();

        mAuth=FirebaseAuth.getInstance();
        senderUserID =mAuth.getCurrentUser().getUid();

        userProfileImage=(CircleImageView)findViewById(R.id.visit_profile_image);
        userProfileName=(TextView)findViewById(R.id.visit_user_name);
        userProfileStatus=(TextView)findViewById(R.id.visit_profile_status);
        sendMessageRequestBtn=(Button)findViewById(R.id.send_message_request_button);
        declineMessageRequestBtn=(Button)findViewById(R.id.decline_message_request_btn);
        current_state="new";

        userRef= FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestRef=FirebaseDatabase.getInstance().getReference().child("Chat Request");
        contactRef= FirebaseDatabase.getInstance().getReference().child("Contacts");
        notificationRef= FirebaseDatabase.getInstance().getReference().child("Notifications");


        RetrieveUserInfo();
    }



    private void RetrieveUserInfo() {
        //receiverUserID
        userRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("image"))){

                    String userImage=dataSnapshot.child("image").getValue().toString();
                    String userName=dataSnapshot.child("name").getValue().toString();
                    String userStatus=dataSnapshot.child("status").getValue().toString();

                    Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);

                    ManageChatRequest();
                }
                else {
                    //if the user have no profile picture
                    String userName=dataSnapshot.child("name").getValue().toString();
                    String userStatus=dataSnapshot.child("status").getValue().toString();

                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);

                    ManageChatRequest();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void ManageChatRequest() {
        //maintain the cancelRequest button to still appear
        chatRequestRef.child(senderUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(receiverUserID)){
                            String request_type=dataSnapshot.child(receiverUserID).child("request_type").getValue().toString();

                            if (request_type.equals("sent")){
                                current_state="request_sent";//at this point current_sate is request_sent
                                sendMessageRequestBtn.setText("Cancel Chat Request");
                            }
                            //from database request_type == received
                            else if(request_type.equals("received")){
                                current_state="request_received";
                                sendMessageRequestBtn.setText("Accept ChatRequest");

                                //this button will only be visible to the receiver of the ChatRequest
                                declineMessageRequestBtn.setVisibility(View.VISIBLE);
                                declineMessageRequestBtn.setEnabled(true);

                                declineMessageRequestBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        cancelChatRequest();
                                    }
                                });

                            }
                        }
                        else {
                            contactRef.child(senderUserID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.hasChild(receiverUserID)){
                                                current_state="friends";
                                                sendMessageRequestBtn.setText("Remove this Contact");
                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        //check if sender=currentUser
        if(!senderUserID.equals(receiverUserID)){
            //clicking button control the actions of the request_state
            sendMessageRequestBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (current_state.equals("new")){
                        sendChatRequest();
                    }
                    if (current_state.equals("request_sent")){
                        //when i send you a friend Request then i can cancelChatRequest
                        cancelChatRequest();
                    }
                    if (current_state.equals("request_received")){
                        //when you receive my friendRequest then you can acceptChatRequest
                        acceptChatRequest();
                    }
                    if (current_state.equals("friends")){
                        //when we are friends then you can removeFriendContact
                        removeSpecificContact();
                    }

                }
            });

        }
        else {
            //if its true sendMessage button setInvisible
           sendMessageRequestBtn.setVisibility(View.INVISIBLE);
        }
    }

    public void sendChatRequest(){
        //sender node
        chatRequestRef.child(senderUserID).child(receiverUserID)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            //receiver node
                            chatRequestRef.child(receiverUserID).child(senderUserID)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                  //NOTIFICATION LOGIC
                                                HashMap<String,Object> chatNotificationMap=new HashMap<>();
                                                chatNotificationMap.put("from",senderUserID);
                                                chatNotificationMap.put("type","request");

                                                //randomKay for each notification
                                                notificationRef.child(receiverUserID).push()
                                                        .setValue(chatNotificationMap)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()){

                                                                    sendMessageRequestBtn.setEnabled(true);
                                                                    current_state="request_sent";
                                                                    sendMessageRequestBtn.setText("Cancel Chat Request");
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

    public void cancelChatRequest(){

        chatRequestRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            chatRequestRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                sendMessageRequestBtn.setEnabled(true);
                                                current_state="new";
                                                sendMessageRequestBtn.setText("Send Message");

                                                //set the button invisible when the user cancel the chat request
                                                declineMessageRequestBtn.setVisibility(View.INVISIBLE);
                                                declineMessageRequestBtn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });

    }

    public void acceptChatRequest(){
        //create another contact node,let it appear to both the sender and receiver
        contactRef.child(senderUserID).child(receiverUserID)
                .child("Contacts").setValue("saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            //receiver node
                            contactRef.child(receiverUserID).child(senderUserID)
                                    .child("Contacts").setValue("saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            //once the request is accepted removed it from the ChatRequest Node
                                            if (task.isSuccessful()){
                                               //Remove from Sender
                                               chatRequestRef.child(senderUserID).child(receiverUserID)
                                                       .removeValue()
                                                       .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                           @Override
                                                           public void onComplete(@NonNull Task<Void> task) {
                                                               if (task.isSuccessful()){
                                                                   //Remove from Receiver
                                                                   chatRequestRef.child(receiverUserID).child(senderUserID)
                                                                           .removeValue()
                                                                           .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                               @Override
                                                                               public void onComplete(@NonNull Task<Void> task) {
                                                                                   if (task.isSuccessful()){
                                                                                       sendMessageRequestBtn.setEnabled(true);
                                                                                       current_state="friends";
                                                                                       sendMessageRequestBtn.setText("Remove this Contact");

                                                                                       declineMessageRequestBtn.setVisibility(View.INVISIBLE);
                                                                                       declineMessageRequestBtn.setEnabled(false);

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

    public void removeSpecificContact(){

        contactRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            contactRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                sendMessageRequestBtn.setEnabled(true);
                                                current_state="new";
                                                sendMessageRequestBtn.setText("Send Message");

                                                declineMessageRequestBtn.setVisibility(View.INVISIBLE);
                                                declineMessageRequestBtn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });

    }
}
