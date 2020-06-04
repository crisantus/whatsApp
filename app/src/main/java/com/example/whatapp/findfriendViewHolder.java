package com.example.whatapp;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import de.hdodenhof.circleimageview.CircleImageView;

public class findfriendViewHolder extends RecyclerView.ViewHolder implements View .OnClickListener {

    TextView username,userStatus;
    CircleImageView profileImage;
    private ItemClickListner itemClickListner;

    public findfriendViewHolder(@NonNull View itemView) {

        super(itemView);

        username=itemView.findViewById(R.id.user_profile_name);
        userStatus=itemView.findViewById(R.id.user_status);
        profileImage=itemView.findViewById(R.id.users_profile_image);
    }

    @Override
    public void onClick(View v) {
        itemClickListner.onClick(v,getAdapterPosition(),false);
    }

    public void setItemClickListner(ItemClickListner itemClickListner){
        this.itemClickListner=itemClickListner;
    }
}
