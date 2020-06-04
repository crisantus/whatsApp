package com.example.whatapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Messages> usersMessagesList;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    public MessageAdapter(List<Messages> usersMessagesList){
        this.usersMessagesList=usersMessagesList;
    }


    public class MessageViewHolder extends RecyclerView.ViewHolder{

        public TextView senderMessageText, receiverMessageText;
        public CircleImageView receiverProfileImage;
        public ImageView messageSenderPicture,messageReceiverPicture;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            senderMessageText=(TextView)itemView.findViewById(R.id.sender_message_text);
            receiverMessageText=(TextView)itemView.findViewById(R.id.receiver_message_text);
            receiverProfileImage=(CircleImageView) itemView.findViewById(R.id.message_profile_image);

            messageReceiverPicture=(ImageView)itemView.findViewById(R.id.message_receiver_view);
            messageSenderPicture=(ImageView)itemView.findViewById(R.id.message_sender_view);
        }
    }



    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
       View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_message_layout,parent,false);

       mAuth=FirebaseAuth.getInstance();

       return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, final int position) {

        String messageSenderId=mAuth.getCurrentUser().getUid();
        Messages messages=usersMessagesList.get(position);

        String fromUserID=messages.getFrom();//from the sender
        String fromMessagetype=messages.getType();

        userRef= FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserID);

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild("image")){
                    String receiverImage=dataSnapshot.child("image").getValue().toString();

                    Picasso.get().load(receiverImage).placeholder(R.drawable.profile_image).into(holder.receiverProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        holder.receiverMessageText.setVisibility(View.GONE);
        holder.receiverProfileImage.setVisibility(View.GONE);
        holder.senderMessageText.setVisibility(View.GONE);
        holder.messageSenderPicture.setVisibility(View.GONE);
        holder.messageReceiverPicture.setVisibility(View.GONE);


        if (fromMessagetype.equals("text")) {


            // this is the sender
            if (fromUserID.equals(messageSenderId)) {

                holder.senderMessageText.setVisibility(View.VISIBLE);

                holder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
                holder.senderMessageText.setTextColor(Color.BLACK);
                holder.senderMessageText.setText(messages.getMessage() + "\n\n" + messages.getTime()+ " - "+ messages.getDate());

            } else {
                //receiver of the message
                ///holder.senderMessageText.setVisibility(View.INVISIBLE);

                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.receiverMessageText.setVisibility(View.VISIBLE);

                holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                holder.receiverMessageText.setTextColor(Color.BLACK);
                holder.receiverMessageText.setText(messages.getMessage() + "\n\n" + messages.getTime()+ " - "+ messages.getDate());
            }

        }
        else if(fromMessagetype.equals("image")){

            // this is the sender
            if (fromUserID.equals(messageSenderId)){

                holder.messageSenderPicture.setVisibility(View.VISIBLE);
                Picasso.get().load(messages.getMessage()).into(holder.messageSenderPicture);
            }
            else {
                //receiver of the message
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);
                Picasso.get().load(messages.getMessage()).into(holder.messageReceiverPicture);
            }
        }
        else if(fromMessagetype.equals("pdf") || fromMessagetype.equals("docx")) {
            // this is the sender
            if (fromUserID.equals(messageSenderId)){
                holder.messageSenderPicture.setVisibility(View.VISIBLE);

                holder.messageSenderPicture.setBackgroundResource(R.drawable.file);

//                //when u have to click the file u sent
//                holder.itemView.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(usersMessagesList.get(position).getMessage()));
//                        holder.itemView.getContext().startActivity(intent);
//
//                    }
//                });
            }
            else {
                // this is the receiver
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);

                holder.messageReceiverPicture.setBackgroundResource(R.drawable.file);

            }
        }

        if (fromUserID.equals(messageSenderId)){
            //for the sender
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (usersMessagesList.get(position).getType().equals("pdf")|| usersMessagesList.get(position).getType().equals("docx")){

                        CharSequence options[]=new CharSequence[]{

                                "Delete For Me",
                                "Download and View This Document",
                                "Cancel",
                                "Delete for Everyone"

                        };

                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which==0){
                                   deleteSentMessages(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else  if (which==1){
                                    //when u have to click the file u sent
                                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(usersMessagesList.get(position).getMessage()));
                                            holder.itemView.getContext().startActivity(intent);

                                        }
                                    });
                                }
                                else  if (which==3){
                                    deleteMessagesForEveryOne(position,holder);
                                }
                            }
                        });

                        builder.show();
                    }

                    else if (usersMessagesList.get(position).getType().equals("text") ){

                        CharSequence options[]=new CharSequence[]{

                                "Delete For Me",
                                "Cancel",
                                "Delete for Everyone"

                        };

                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which==0){
                                    deleteSentMessages(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }

                                else  if (which==2){
                                    deleteMessagesForEveryOne(position,holder);
                                }

                            }
                        });

                        builder.show();
                    }

                    else if (usersMessagesList.get(position).getType().equals("image") ){

                        CharSequence options[]=new CharSequence[]{

                                "Delete For Me",
                                "View this Image",
                                "Cancel",
                                "Delete for Everyone"

                        };

                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which==0){
                                  deleteSentMessages(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }

                                if (which==1){

                                    Intent intent=new Intent(holder.itemView.getContext(),ImageViewActivity.class);
                                    intent.putExtra("url",usersMessagesList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);

                                }

                                else  if (which==3){
                                    deleteMessagesForEveryOne(position,holder);
                                }

                            }
                        });

                        builder.show();
                    }

                }
            });
        }
        else {
            //on the receiver side
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (usersMessagesList.get(position).getType().equals("pdf")|| usersMessagesList.get(position).getType().equals("docx")){

                        CharSequence options[]=new CharSequence[]{

                                "Delete For Me",
                                "Download and View This Document",
                                "Cancel"

                        };

                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which==0){
                                    deleteReceiveMessages(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);

                                }
                                else  if (which==1){
                                    //when u have to click the file u sent
                                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(usersMessagesList.get(position).getMessage()));
                                            holder.itemView.getContext().startActivity(intent);

                                        }
                                    });

                                }
                            }
                        });

                        builder.show();
                    }

                    else if (usersMessagesList.get(position).getType().equals("text") ){

                        CharSequence options[]=new CharSequence[]{

                                "Delete For Me",
                                "Cancel",

                        };

                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which==0){
                                    deleteReceiveMessages(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });

                        builder.show();
                    }

                    else if (usersMessagesList.get(position).getType().equals("image") ){

                        CharSequence options[]=new CharSequence[]{

                                "Delete For Me",
                                "View this Image",
                                "Cancel",


                        };

                        AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which==0){
                                    deleteReceiveMessages(position,holder);
                                    Intent intent=new Intent(holder.itemView.getContext(),MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }

                               else if (which==1){

                                    Intent intent=new Intent(holder.itemView.getContext(),ImageViewActivity.class);
                                    intent.putExtra("url",usersMessagesList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);

                                }

                            }
                        });

                        builder.show();
                    }

                }
            });
        }

    }


    @Override
    public int getItemCount() {
        return usersMessagesList.size();
    }


    private void deleteSentMessages(final int position, final  MessageViewHolder holder){

        DatabaseReference rootRef=FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages")
                .child(usersMessagesList.get(position).getFrom())
                .child(usersMessagesList.get(position).getTo())
                .child(usersMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(holder.itemView.getContext(), "Deleted Successfully.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(holder.itemView.getContext(), "Error Occurred.", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void deleteReceiveMessages(final int position, final  MessageViewHolder holder){

        DatabaseReference rootRef=FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages")
                .child(usersMessagesList.get(position).getTo())
                .child(usersMessagesList.get(position).getFrom())
                .child(usersMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(holder.itemView.getContext(), "Deleted Successfully.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(holder.itemView.getContext(), "Error Occurred.", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void deleteMessagesForEveryOne(final int position, final  MessageViewHolder holder){

        final DatabaseReference rootRef=FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages")
                .child(usersMessagesList.get(position).getTo())
                .child(usersMessagesList.get(position).getFrom())
                .child(usersMessagesList.get(position).getMessage())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    rootRef.child("Messages")
                            .child(usersMessagesList.get(position).getFrom())
                            .child(usersMessagesList.get(position).getTo())
                            .child(usersMessagesList.get(position).getMessageID())
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(holder.itemView.getContext(), "Deleted Successfully.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else {
                    Toast.makeText(holder.itemView.getContext(), "Error Occurred.", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }



}
