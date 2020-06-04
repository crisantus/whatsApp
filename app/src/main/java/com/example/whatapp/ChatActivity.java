package com.example.whatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String messageReceivedID,messageReceiverName,messageReceiverImage,messageSenderID;
    private TextView userName,userLastSeen;
    private CircleImageView userImage;
    private Toolbar chatToolBar;
    private ImageButton sendMessageBtn,sendFilesButton;
    private EditText messageInputText;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;
    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayoutManager;
    private MessageAdapter mMessageAdapter;
    private RecyclerView userMessageList;

    private String saveCurrentTime, saveCurrentData;
    private String checker="",myUrl="";
    private Uri fileUri;
    private StorageTask uploadTask;
    private ProgressDialog loadingBar;
    private String mPdf;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageReceivedID=getIntent().getExtras().get("visit_user_id").toString();
        messageReceiverName=getIntent().getExtras().get("visit_userName").toString();
        messageReceiverImage=getIntent().getExtras().get("visit_image").toString();

        mAuth=FirebaseAuth.getInstance();
        messageSenderID=mAuth.getCurrentUser().getUid();
        rootRef= FirebaseDatabase.getInstance().getReference();

        IntializeController();

        userName.setText(messageReceiverName);
        Picasso.get().load(messageReceiverImage).placeholder(R.drawable.profile_image).into(userImage);

        sendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        displayLastSeen();


        sendFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence options[]=new CharSequence[]
                        {
                              "Images",
                              "PDF Files",
                              "Ms Word Files"
                        };

                AlertDialog.Builder builder=new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Select the Files");

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            //images
                            checker = "image";
                            //opens the phone gallery
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent, "select Image"), 438);
                        }

                        if (which == 1) {
                            //pdf
                            checker = "pdf";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf*");
                            startActivityForResult(intent.createChooser(intent, "select PDF File"), 438);

                        }
                        if (which == 2) {
                            //ms Word
                            checker = "docx";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/msword*");
                            startActivityForResult(intent.createChooser(intent, "select Ms Word File"), 438);
                        }

                    }
                });

                builder.show();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode==438 && resultCode==RESULT_OK && data!=null && data.getData()!=null){

            loadingBar.setTitle("Sending File");
            loadingBar.setMessage("Please wait,we are sending that file....");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();


            fileUri=data.getData();//image from gallery to store at fileUri

            if (!checker.equals("image")){

                StorageReference storageReference= FirebaseStorage.getInstance().getReference().child("Document Files");

                //save to the database,sender is the person currently online.., Messages is the parent Node
                final String messageSenderRef="Messages/"+messageSenderID+ "/" + messageReceivedID;
                final String messageRecieverRef="Messages/"+messageReceivedID+ "/" + messageSenderID;

                //create a random key for each user imageMessage so no message will be replace with another one
                DatabaseReference userMessageKeyRef=rootRef.child("Messages").child(messageSenderID).child(messageReceivedID).push();

                final String messagePuchID=userMessageKeyRef.getKey();

                final StorageReference filepath=storageReference.child(messagePuchID+"."+checker);

                filepath.putFile(fileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        loadingBar.dismiss();
                        Toast.makeText(ChatActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();

                        filepath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()){
                                    String filePDF=task.getResult().toString();

                                    HashMap<String,Object> imageTextBody=new HashMap();
                                    imageTextBody.put("message", filePDF);
                                    imageTextBody.put("name",fileUri.getLastPathSegment());
                                    imageTextBody.put("type",checker);
                                    imageTextBody.put("from",messageSenderID);
                                    imageTextBody.put("to",messageReceivedID);
                                    imageTextBody.put("messageID",messagePuchID);
                                    imageTextBody.put("time",saveCurrentTime);
                                    imageTextBody.put("date",saveCurrentData);

                                    Map messageBodyDetails=new HashMap();
                                    messageBodyDetails.put(messageSenderRef + "/" + messagePuchID,imageTextBody);
                                    messageBodyDetails.put(messageRecieverRef + "/" + messagePuchID,imageTextBody);

                                    rootRef.updateChildren(messageBodyDetails);
                                    loadingBar.dismiss();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                loadingBar.dismiss();
                                Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                });


            }
            else if (checker.equals("image")){
              //it a sn image
                StorageReference storageReference= FirebaseStorage.getInstance().getReference().child("Image Files");

                //save to the database,sender is the person currently online.., Messages is the parent Node
                final String messageSenderRef="Messages/"+messageSenderID+ "/" + messageReceivedID;
                final String messageRecieverRef="Messages/"+messageReceivedID+ "/" + messageSenderID;

                //create a random key for each user imageMessage so no message will be replace with another one
                DatabaseReference userMessageKeyRef=rootRef.child("Messages").child(messageSenderID).child(messageReceivedID).push();

                final String messagePuchID=userMessageKeyRef.getKey();

                final StorageReference filepath=storageReference.child(messagePuchID+"_"+"jpg");

                filepath.putFile(fileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        loadingBar.dismiss();
                        Toast.makeText(ChatActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();


                        filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {


                                HashMap<String,Object> imageTextBody=new HashMap();
                                imageTextBody.put("message",String.valueOf(uri));
                                imageTextBody.put("name",fileUri.getLastPathSegment());
                                imageTextBody.put("type",checker);
                                imageTextBody.put("from",messageSenderID);
                                imageTextBody.put("to",messageReceivedID);
                                imageTextBody.put("messageID",messagePuchID);
                                imageTextBody.put("time",saveCurrentTime);
                                imageTextBody.put("date",saveCurrentData);

                                HashMap<String,Object> messageBodyDetails=new HashMap();
                                messageBodyDetails.put(messageSenderRef + "/" + messagePuchID,imageTextBody);
                                messageBodyDetails.put(messageRecieverRef + "/" + messagePuchID,imageTextBody);

                                rootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                    @Override
                                    public void onComplete(@NonNull Task task) {
                                        if (task.isSuccessful()){


                                            loadingBar.dismiss();
                                            Toast.makeText(ChatActivity.this, "Image Sent", Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            loadingBar.dismiss();
                                            Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                        }
                                        messageInputText.setText("");
                                    }
                                });


                            }
                        });
                    }
                });

            }
            else {
                loadingBar.dismiss();
                Toast.makeText(this, "Nothing Selected, Error", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void IntializeController() {

        chatToolBar=(Toolbar)findViewById(R.id.chat_toolBar);
        setSupportActionBar(chatToolBar);

        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater=(LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView= layoutInflater.inflate(R.layout.custom_chat_bar,null);
        actionBar.setCustomView(actionBarView);

        userImage=(CircleImageView)findViewById(R.id.custom_profile_IMAGE);
        userName=(TextView)findViewById(R.id.custom_profile_name);
        userLastSeen=(TextView)findViewById(R.id.custom_user_lastSeen);

        sendMessageBtn=(ImageButton)findViewById(R.id.send_message_btn);
        sendFilesButton=(ImageButton)findViewById(R.id.send_file_btn);
        messageInputText=(EditText)findViewById(R.id.input_message);

        mMessageAdapter=new MessageAdapter(messagesList);
        userMessageList=(RecyclerView) findViewById(R.id.private_messages_list_of_users);
        mLinearLayoutManager=new LinearLayoutManager(this);
        userMessageList.setLayoutManager(mLinearLayoutManager);
        userMessageList.setAdapter(mMessageAdapter);


        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentData = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        loadingBar=new ProgressDialog(this);


    }


    private void displayLastSeen(){
        rootRef.child("Users").child(messageReceivedID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.child("userState").hasChild("state")){

                    String state =dataSnapshot.child("userState").child("state").getValue().toString();
                    String time =dataSnapshot.child("userState").child("time").getValue().toString();
                    String date =dataSnapshot.child("userState").child("date").getValue().toString();

                    if (state.equals("online")){

                       userLastSeen.setText("online");
                    }
                    else if (state.equals("offline")){

                        userLastSeen.setText("Last Seen: "+ date + " "+time);
                    }
                }
                else {
                    userLastSeen.setText("offline");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        rootRef.child("Messages").child(messageSenderID).child(messageReceivedID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Messages messages=dataSnapshot.getValue(Messages.class);

                        messagesList.add(messages);

                        mMessageAdapter.notifyDataSetChanged();

                        userMessageList.smoothScrollToPosition(userMessageList.getAdapter().getItemCount());

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

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

    private void sendMessage(){
        //this is the pattern in which the message is being sent to the database
       String messageText=messageInputText.getText().toString();
       if (TextUtils.isEmpty(messageText)){
           Toast.makeText(this, "First write your message", Toast.LENGTH_SHORT).show();
       }
       else {
           //save to the database,sender is the person currently online.., Messages is the parent Node
           String messageSenderRef="Messages/"+messageSenderID+ "/" + messageReceivedID;
           String messageRecieverRef="Messages/"+messageReceivedID+ "/" + messageSenderID;

            //create a random key for each user message so no message will be replace with another one
           DatabaseReference userMessageKeyRef=rootRef.child("Messages").child(messageSenderID).child(messageReceivedID).push();

           String messagePuchID=userMessageKeyRef.getKey();

           Map messageTextBody=new HashMap();
           messageTextBody.put("message",messageText);
           messageTextBody.put("type","text");
           messageTextBody.put("from",messageSenderID);
           messageTextBody.put("to",messageReceivedID);
           messageTextBody.put("messageID",messagePuchID);
           messageTextBody.put("time",saveCurrentTime);
           messageTextBody.put("date",saveCurrentData);

           Map messageBodyDetails=new HashMap();
           messageBodyDetails.put(messageSenderRef + "/" + messagePuchID,messageTextBody);
           messageBodyDetails.put(messageRecieverRef + "/" + messagePuchID,messageTextBody);

           rootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
               @Override
               public void onComplete(@NonNull Task task) {
                  if (task.isSuccessful()){
                      Toast.makeText(ChatActivity.this, "Message Sent", Toast.LENGTH_SHORT).show();
                  }
                  else {
                      Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                  }
                  messageInputText.setText("");
               }
           });






       }
    }

}
