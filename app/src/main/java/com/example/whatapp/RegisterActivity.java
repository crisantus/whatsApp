package com.example.whatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class RegisterActivity extends AppCompatActivity {

    private Button createAccountButton;
    private EditText userEmail,userPassword;
    private TextView alreadyHaveAccountLink;

    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;

    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        InitializationFields();

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        rootRef= FirebaseDatabase.getInstance().getReference();

        alreadyHaveAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToLoginActivity();
            }
        });

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewAccount();
            }
        });
    }

    private void createNewAccount() {

        String email=userEmail.getText().toString().trim();
        String password=userPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)){
            userEmail.setError("Field Required");
            return;
        }
        else if (TextUtils.isEmpty(password)){
            userPassword.setError("Field Required");
            return;
        }
        else {
            loadingBar.setTitle("Creating New Account");
            loadingBar.setMessage("Please wait, while we are creating new account for you...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information

                                String deviceToken= FirebaseInstanceId.getInstance().getToken();

                                String currentUserID=mAuth.getCurrentUser().getUid();
                                rootRef.child("Users").child(currentUserID).setValue("");

                                rootRef.child("Users").child(currentUserID).child("device_token")
                                        .setValue(deviceToken);

                                loadingBar.dismiss();
                                sendUserToMainActivity();
                                Toast.makeText(RegisterActivity.this, "Account Created", Toast.LENGTH_SHORT).show();
                            } else {
                                // If sign in fails, display a message to the user.
                                loadingBar.dismiss();
                               String message=task.getException().toString();
                                Toast.makeText(RegisterActivity.this, "Error :"+message, Toast.LENGTH_SHORT).show();
                            }

                            // ...
                        }
                    });
        }
    }



    private void InitializationFields() {
        createAccountButton=(Button)findViewById(R.id.register_btn);
        userEmail=(EditText)findViewById(R.id.register_email);
        userPassword=(EditText)findViewById(R.id.register_password);
        alreadyHaveAccountLink=(TextView)findViewById(R.id.already_have_an_account_link);

        loadingBar =new ProgressDialog(this);
    }

    private void sendUserToLoginActivity() {
        Intent loginIntent=new Intent(RegisterActivity.this,LoginActivity.class);
        startActivity(loginIntent);
    }

    private void sendUserToMainActivity() {
        Intent mainIntent=new Intent(RegisterActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
