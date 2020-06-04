package com.example.whatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.badge.BadgeUtils;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity {

    private Button sendVerificationCodebtn,verifyBtn;
    private EditText inputPhoneNumber, inputVerificationCode;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private ProgressDialog loadingBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        sendVerificationCodebtn=(Button)findViewById(R.id.send_verification_button);
        verifyBtn=(Button)findViewById(R.id.verification_button);
        inputPhoneNumber=(EditText)findViewById(R.id.phone_number_input);
        inputVerificationCode=(EditText)findViewById(R.id.verification_code_input);
        loadingBar=new ProgressDialog(this);


        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        sendVerificationCodebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get the phone number
                String phoneNumber=inputPhoneNumber.getText().toString();

                if (TextUtils.isEmpty(phoneNumber)){
                    inputPhoneNumber.setError("Phone Number is Required");
                }
                else {

                    loadingBar.setTitle("Phone Verification");
                    loadingBar.setMessage("Please wait, while we are authenticating your phone");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                           phoneNumber,         //Phone number to verify
                            60,               //Timeout duration
                            TimeUnit.SECONDS,    //Unit of timeout
                            PhoneLoginActivity.this,  //Activity
                            mCallBacks          //onVerificationStateChangeCallbacks
                    );
                }
            }
        });

        verifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //make sure its invisible
                sendVerificationCodebtn.setVisibility(View.INVISIBLE);
                inputPhoneNumber.setVisibility(View.INVISIBLE);

                String verificationCode=inputVerificationCode.getText().toString();
                if (TextUtils.isEmpty(verificationCode)){
                    Toast.makeText(PhoneLoginActivity.this, "please write the code First", Toast.LENGTH_SHORT).show();
                }
                else {
                    loadingBar.setTitle("Verification Code");
                    loadingBar.setMessage("Please wait, while we are Verifying verification code...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();
                    //checking the code if it correct or legit
                    PhoneAuthCredential credential=PhoneAuthProvider.getCredential(mVerificationId,verificationCode);
                    signInWithPhoneAuthCredential(credential);
                }

            }
        });

        mCallBacks=new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                //called when our verification is successful
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                //called when our verification is Failed
                loadingBar.dismiss();

                //when the code is not Sent to sendVerificationCodebtn and inputPhoneNumber
                sendVerificationCodebtn.setVisibility(View.VISIBLE);
                inputPhoneNumber.setVisibility(View.VISIBLE);

                verifyBtn.setVisibility(View.INVISIBLE);
                inputVerificationCode.setVisibility(View.INVISIBLE);
                Toast.makeText(PhoneLoginActivity.this, "Invalid Phone Number,Please enter correct phone number with your county code...", Toast.LENGTH_SHORT).show();
            }

            public void onCodeSent(String verification,PhoneAuthProvider.ForceResendingToken token){
               //this method is called when our code is sent to the mobile phone
                loadingBar.dismiss();

                //save verification ID and resending token so we can use them later
                mVerificationId=verification;
                mResendToken=token;

                Toast.makeText(PhoneLoginActivity.this, "Code has been sent to your phone", Toast.LENGTH_SHORT).show();

                //when the code is sent disable sendVerificationCodebtn and inputPhoneNumber
                sendVerificationCodebtn.setVisibility(View.INVISIBLE);
                inputPhoneNumber.setVisibility(View.INVISIBLE);

                verifyBtn.setVisibility(View.VISIBLE);
                inputVerificationCode.setVisibility(View.VISIBLE);

            }
        };
    }





    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential){
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            //sign in successfully
                            sendUserToMainActivity();
                             loadingBar.dismiss();
                            Toast.makeText(PhoneLoginActivity.this, "Congratulations, you 're logged in successfully", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            //signIn failed
                            String message=task.getException().toString();
                            Toast.makeText(PhoneLoginActivity.this, "Error :" +message, Toast.LENGTH_SHORT).show();
                  }
                    }
                });
    }

    private void sendUserToMainActivity() {
        Intent mainIntent=new Intent(PhoneLoginActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
