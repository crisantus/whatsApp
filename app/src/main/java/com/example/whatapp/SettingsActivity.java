package com.example.whatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button updateAccountSettings;
    private EditText username,useerStatus;
    private CircleImageView userProfileImage;
    private DatabaseReference rootRef;
    private static final int GallaryPick=1;
    private StorageTask uploadtask;
    private String myUri;
    private StorageReference userProfileImageRef;
    private String currentUserID;
    private FirebaseAuth mAuth;
    private Uri imageUri;
    private ProgressDialog loadingBar;
    private Toolbar settingToolBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        InitializeField();

        mAuth=FirebaseAuth.getInstance();
        currentUserID=mAuth.getCurrentUser().getUid();
        rootRef= FirebaseDatabase.getInstance().getReference();

        userProfileImageRef= FirebaseStorage.getInstance().getReference().child("Profile Images");

        updateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upDateSettings();
            }
        });

        RetrieveUserInformation();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CropImage.activity(imageUri)
                        .setAspectRatio(1,1)
                        .start(SettingsActivity.this);
//               Intent gallaryIntent= new Intent();
//               gallaryIntent.setAction(Intent.ACTION_GET_CONTENT);
//               gallaryIntent.setType("image/*");
//               startActivityForResult(gallaryIntent,GallaryPick);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode==GallaryPick && resultCode==RESULT_OK && data !=null){
//            Uri imageUr=data.getData();
//
//            CropImage.activity()
//                    .setGuidelines(CropImageView.Guidelines.ON)
//                    .setAspectRatio(1,1)
//                    .start(this);
//        }
        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){

            CropImage.ActivityResult result=CropImage.getActivityResult(data);

            if (resultCode==RESULT_OK){

                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Profile image is updating...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                imageUri=result.getUri();

                final StorageReference filePath=userProfileImageRef.child(currentUserID + ".jpg");
                filePath.putFile(imageUri);

                uploadtask=filePath.putFile(imageUri);
                //get the result of the upload task
                Task<Uri> uriTaskImageLink = uploadtask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        myUri = filePath.getDownloadUrl().toString();
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()){
                            myUri = task.getResult().toString();
                            //store it to the database
                            rootRef.child("Users").child(currentUserID).child("image")
                                    .setValue(myUri)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                loadingBar.dismiss();
                                                Toast.makeText(SettingsActivity.this, "Profile image uploaded Successfully", Toast.LENGTH_SHORT).show();
                                            }
                                            else {
                                                loadingBar.dismiss();
                                                String message=task.getException().toString();
                                                Toast.makeText(SettingsActivity.this, "Error: "+message, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });

                        }
                        else {
                            loadingBar.dismiss();
                            String message=task.getException().toString();
                            Toast.makeText(SettingsActivity.this, "Error: "+message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    private void InitializeField() {
        updateAccountSettings=(Button)findViewById(R.id.update_setting_btn);
        username=(EditText)findViewById(R.id.set_user_name);
        useerStatus=(EditText)findViewById(R.id.set_profile_status);
        userProfileImage=(CircleImageView)findViewById(R.id.set_profile_image);
        loadingBar=new ProgressDialog(this);

        settingToolBar =(Toolbar)findViewById(R.id.settings_toolbar);
        setSupportActionBar(settingToolBar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");
    }

    private void upDateSettings() {
        String setUserName=username.getText().toString().trim();
        String setUserStatus=useerStatus.getText().toString().trim();

        if (TextUtils.isEmpty(setUserName)){
            username.setError("Field Required");
            return;
        }
        else if (TextUtils.isEmpty(setUserStatus)){
            useerStatus.setError("Field Required");
            return;
        }
        else {
            HashMap<String,Object> profileMap=new HashMap<>();
            profileMap.put("uid",currentUserID);
            profileMap.put("name",setUserName);
            profileMap.put("status",setUserStatus);

            rootRef.child("Users").child(currentUserID).updateChildren(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                sendUserToMainActivity();
                                Toast.makeText(SettingsActivity.this, "Profile Updated Successfully...", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                String message=task.getException().toString();
                                Toast.makeText(SettingsActivity.this, "Error :"+message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }


    private void RetrieveUserInformation() {
        rootRef.child("Users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if ((dataSnapshot.exists()) &&  (dataSnapshot.hasChild("name") && (dataSnapshot.hasChild("image")))){
                            String retrieveUserName=dataSnapshot.child("name").getValue().toString();
                            String retrieveStatus=dataSnapshot.child("status").getValue().toString();
                            String retrieveProfileImage=dataSnapshot.child("image").getValue().toString();

                            username.setText(retrieveUserName);
                            useerStatus.setText(retrieveStatus);
                           //use picasso to load image to user
                            Picasso.get().load(retrieveProfileImage).into(userProfileImage);
                        }
                        else if ((dataSnapshot.exists()) &&  (dataSnapshot.hasChild("name"))){
                            String retrieveUserName=dataSnapshot.child("name").getValue().toString();
                            String retrieveStatus=dataSnapshot.child("status").getValue().toString();

                            username.setText(retrieveUserName);
                            useerStatus.setText(retrieveStatus);
                        }
                        else {
                            Toast.makeText(SettingsActivity.this, "Please set & update your profile Information", Toast.LENGTH_SHORT).show();
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void sendUserToMainActivity() {
        Intent mainIntent=new Intent(SettingsActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

}
