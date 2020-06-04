package com.example.whatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ViewPager myViewPager;
    private TabLayout myTabLayout;
    private TabsAccessorAdapter myTabAccessorAdapter;


    private FirebaseAuth mAuth;
    private DatabaseReference rooofRef;

    //Update input field
    private EditText createGroupName;
    private Button btnCancel;
    private Button btnCreate;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /***********************
         * mainActivity is the activity that contains all the fragment
         */

        mAuth=FirebaseAuth.getInstance();


        rooofRef= FirebaseDatabase.getInstance().getReference();

        mToolbar=(Toolbar)findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("WhatsApp");

        myViewPager=(ViewPager)findViewById(R.id.main_tabs_pagers);
        myTabAccessorAdapter=new TabsAccessorAdapter(getSupportFragmentManager());
        myViewPager.setAdapter(myTabAccessorAdapter);

        myTabLayout=(TabLayout)findViewById(R.id.main_tabs);
        myTabLayout.setupWithViewPager(myViewPager);
    }


    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser=mAuth.getCurrentUser();
        //if user is not logged in send to logging in page
        if(currentUser==null){
            SendUserToLoginActivity();
        }
        else {
            updateUserStatus("online");
            verifyUserExistence();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();

        FirebaseUser currentUser=mAuth.getCurrentUser();

        if(currentUser!=null){

            updateUserStatus("offline");
        }

    }

    @Override
    protected void onDestroy() {

        FirebaseUser currentUser=mAuth.getCurrentUser();

        super.onDestroy();

        if(currentUser!=null){

            updateUserStatus("offline");
        }
    }

    private void verifyUserExistence() {

        String currentUserID=mAuth.getCurrentUser().getUid();
        rooofRef.child("Users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child("name").exists()){
                            //when the user is not a new user and have set it name,say welcome
                            //Toast.makeText(MainActivity.this, "Welcome", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            //if the user is a new user sent the user to settingActivity.
                            SendUserToSettingsActivity();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.options_menu,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

         super.onOptionsItemSelected(item);

         if (item.getItemId() == R.id.main_logout_option){

             updateUserStatus("offline");
             mAuth.signOut();
             SendUserToLoginActivity();
         }
        if (item.getItemId() == R.id.main_settings_option){
            SendUserToSettingsActivity();
        }
        if (item.getItemId() == R.id.main_find_friend_option){
           SendUserToFindfriendActivity();
        }
        if (item.getItemId() == R.id.main_create_group_option){
            requestNewGroup();
        }
        return true;
    }

    private void requestNewGroup() {

        AlertDialog.Builder myDialog=new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater=LayoutInflater.from(MainActivity.this);

        View myView=inflater.inflate(R.layout.create_group_layout,null);
        myDialog.setView(myView);
        final AlertDialog dialog=myDialog.create();

        createGroupName=myView.findViewById(R.id.groupname);

        btnCancel=(Button) myView.findViewById(R.id.btn_cancel);
        btnCreate=(Button) myView.findViewById(R.id.btn_create);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String groupName=createGroupName.getText().toString().trim();
                if (TextUtils.isEmpty(groupName)){
                    createGroupName.setError("Field Required");
                    return;
                }
                else {
                    createNewGroup(groupName);
                    dialog.dismiss();
                }

            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    private void createNewGroup(final String groupName) {

        rooofRef.child("Groups").child(groupName).setValue("")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(MainActivity.this, groupName+" group is Created Successfully...", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void SendUserToLoginActivity() {

        Intent loginIntent=new Intent(MainActivity.this,LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    private void SendUserToSettingsActivity() {
        Intent settingIntent=new Intent(MainActivity.this,SettingsActivity.class);
        startActivity(settingIntent);
    }

    private void SendUserToFindfriendActivity() {

        Intent findfriendsIntent=new Intent(MainActivity.this,FindFriendsActivity.class);
        startActivity(findfriendsIntent);
    }

    private void updateUserStatus(String state){

        String saveCurrentTime, saveCurrentData;

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentData = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        HashMap<String,Object> onLineStateMap=new HashMap<>();
        onLineStateMap.put("time",saveCurrentTime);
        onLineStateMap.put("date",saveCurrentData);
        onLineStateMap.put("state",state);

        currentUserID=mAuth.getCurrentUser().getUid();

        rooofRef.child("Users").child(currentUserID).child("userState")
                .updateChildren(onLineStateMap);




    }
}
