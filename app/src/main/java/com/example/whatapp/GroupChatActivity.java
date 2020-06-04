package com.example.whatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.icu.text.Edits;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;


public class GroupChatActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ImageButton sendMessageButton;
    private EditText userMessageInput;
    private ScrollView mScrollView;
    private TextView displayTextMessages;
    private DatabaseReference userRef, groupNameRef,groupMessageKeyRef;

    private FirebaseAuth mAuth;

    private  String currentGroupName,currentUserName,currentUserID,saveCurrentDate,saveCurrentTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        currentGroupName=getIntent().getExtras().get("groupName").toString();
        Toast.makeText(this, currentGroupName, Toast.LENGTH_SHORT).show();

        mAuth=FirebaseAuth.getInstance();
        currentUserID=mAuth.getCurrentUser().getUid();

        userRef= FirebaseDatabase.getInstance().getReference().child("Users");
        groupNameRef =FirebaseDatabase.getInstance().getReference().child("Groups").child(currentGroupName);

        InitField();

        getUserInfo();

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMessageInfoToDatabase();
                userMessageInput.setText("");
                //scroll automatically
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        //addChildEventListener is use to communicate direct to the data stored in side a node
        groupNameRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (dataSnapshot.exists()){
                    displayMessages(dataSnapshot);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (dataSnapshot.exists()){
                    displayMessages(dataSnapshot);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void InitField() {
        mToolbar=(Toolbar) findViewById(R.id.group_chat_bar_layout);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(currentGroupName);

        sendMessageButton=(ImageButton)findViewById(R.id.send_message_button);
        userMessageInput=(EditText)findViewById(R.id.input_group_message);
        displayTextMessages=(TextView)findViewById(R.id.group_chat_text_display);
        mScrollView=(ScrollView)findViewById(R.id.my_scroll_view);
    }

    private void getUserInfo() {
        userRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    currentUserName=dataSnapshot.child("name").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveMessageInfoToDatabase() {
        String message=userMessageInput.getText().toString().trim();

        //those message we send with groupNameRef,we get the uniqueKey by .push().getKey(),store it in messageKey
        //Note: each message sent generate a uniqueKey,with the help of push(),no updating of previous message,they are all new once wit different Key
        String messageKey= groupNameRef.push().getKey();

        if (TextUtils.isEmpty(message)){
            Toast.makeText(this, "Please write message first...", Toast.LENGTH_SHORT).show();
        }
        else {

            Calendar calendar = Calendar.getInstance();

            SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
            saveCurrentDate = currentDate.format(calendar.getTime());

            SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm a");
            saveCurrentTime = currentTime.format(calendar.getTime());

            //here we sent our message to database
            HashMap<String,Object> groupMessageKey=new HashMap<>();
            groupNameRef.updateChildren(groupMessageKey);

            //groupMessageKeyRef we use this to store the message data
            groupMessageKeyRef=groupNameRef.child(messageKey);
            HashMap<String,Object> messageInfoMap=new HashMap<>();
                messageInfoMap.put("name",currentUserName);
                messageInfoMap.put("message",message);
                messageInfoMap.put("date",saveCurrentDate);
                messageInfoMap.put("time",saveCurrentTime);
                groupMessageKeyRef.updateChildren(messageInfoMap);


        }
    }

    private void displayMessages(DataSnapshot dataSnapshot) {
        Iterator iterator=dataSnapshot.getChildren().iterator();
         //here we are getting the message line by line
        while (iterator.hasNext()){
            //date,message,name,time.. they are inside the database.. this what ur going to retrieve base on the dataSnapshot that will pass
            String chatDate=(String) ((DataSnapshot)iterator.next()).getValue();
            String chatMessage=(String) ((DataSnapshot)iterator.next()).getValue();
            String chatName=(String) ((DataSnapshot)iterator.next()).getValue();
            String chatTime=(String) ((DataSnapshot)iterator.next()).getValue();

            displayTextMessages.append(chatName+ ":\n" + chatMessage + "\n" + chatTime + "    " + chatDate + "\n\n\n");

            //scroll automatically
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
        }
    }

}
