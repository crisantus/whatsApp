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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button loginBtn,phoneLoginBtn;
    private EditText userEmail,userPassword;
    private TextView needNewAccountLink, forgetPasswordLink;
    private ProgressDialog loadingBar;
    private DatabaseReference userRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        InitializationFields();

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        userRef=FirebaseDatabase.getInstance().getReference().child("Users");

        needNewAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToRegisterActivity();
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 allowUserToLogin();
            }
        });

        phoneLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent phoneLoginInent=new Intent(LoginActivity.this,PhoneLoginActivity.class);
                startActivity(phoneLoginInent);
            }
        });
    }


    private void allowUserToLogin() {
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
        else{

            loadingBar.setTitle("Sign In");
            loadingBar.setMessage("Please wait...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                String currentUserId=mAuth.getCurrentUser().getUid();
                                String deviceToken= FirebaseInstanceId.getInstance().getToken();

                                userRef.child(currentUserId).child("device_token")
                                        .setValue(deviceToken)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()){
                                                    // Sign in success
                                                    loadingBar.dismiss();
                                                    sendUserToMainActivity();
                                                    Toast.makeText(LoginActivity.this, "Logged in successfully..", Toast.LENGTH_SHORT).show();

                                                }
                                            }
                                        });



                            } else {
                                // If sign in fails, display a message to the user.
                                loadingBar.dismiss();
                               String messsge=task.getException().toString();
                                Toast.makeText(LoginActivity.this, "Error :"+messsge, Toast.LENGTH_SHORT).show();
                            }

                            // ...
                        }
                    });
        }

    }


    private void InitializationFields() {
        loginBtn=(Button)findViewById(R.id.login_btn);
        phoneLoginBtn=(Button)findViewById(R.id.phone_login_btn);
        userEmail=(EditText)findViewById(R.id.login_email);
        userPassword=(EditText)findViewById(R.id.login_password);
        needNewAccountLink=(TextView)findViewById(R.id.need_new_Account);
        forgetPasswordLink=(TextView)findViewById(R.id.forget_password_link);

        loadingBar =new ProgressDialog(this);
    }


    private void sendUserToMainActivity() {
        Intent mainIntent=new Intent(LoginActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void sendUserToRegisterActivity() {
        Intent registerIntent=new Intent(LoginActivity.this,RegisterActivity.class);
        startActivity(registerIntent);
    }

}
