package com.logismart.logismart;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class AuthActivity extends Activity {

    private static final String TAG = "AuthActivity";

    private FirebaseAuth mAuth;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private Button authCallBtn;
    private Button reAuthBtn;
    private Button authBtn;
    private EditText phoneText;
    private EditText codeText;
    private ProgressBar pb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_auth);

        authCallBtn = findViewById(R.id.authcall_btn);
        reAuthBtn = findViewById(R.id.reauth_btn);
        authBtn = findViewById(R.id.auth_btn);
        phoneText = (EditText) findViewById(R.id.phone_input);
        codeText = (EditText) findViewById(R.id.authnum_input);
        pb = (ProgressBar) findViewById(R.id.loading);

        mAuth = FirebaseAuth.getInstance();

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // 1 - Instant verification
                // 2 - Auto-retrieval
                Log.d(TAG, "onVerificationCompleted:" + credential);

                pb.setVisibility(View.INVISIBLE);

                Toast.makeText(AuthActivity.this, "인증 성공", Toast.LENGTH_SHORT).show();

                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e);
                pb.setVisibility(View.INVISIBLE);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }

                // Show a message and update the UI
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                Toast.makeText(AuthActivity.this, "인증번호 전송", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(@NonNull String verificationId) {
                super.onCodeAutoRetrievalTimeOut(verificationId);
                Log.d(TAG, "onCodeAutoRetrievalTimeOut: TimeOut");
                pb.setVisibility(View.INVISIBLE);

                mVerificationId = verificationId;
                Toast.makeText(AuthActivity.this, "시간초과", Toast.LENGTH_SHORT).show();

            }
        };

        authCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phonenumber = "+82" + phoneText.getText().toString().substring(1);
                startPhoneNumberVerification(phonenumber); // Korea
            }
        });

        reAuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phonenumber = "+82" + phoneText.getText().toString().substring(1);
                resendVerificationCode(phonenumber, mResendToken);
            }
        });

        authBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pb.setVisibility(View.VISIBLE);
                verifyPhoneNumberWithCode(mVerificationId, codeText.getText().toString());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)                  // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                           // Activity (for callback binding)
                        .setCallbacks(mCallbacks)                   // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
    }

    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)                 // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                          // Activity (for callback binding)
                        .setCallbacks(mCallbacks)                   // OnVerificationStateChangedCallbacks
                        .setForceResendingToken(token)              // ForceResendingToken from callbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            // Update UI

                            Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                            intent.putExtra("type", "driver");
                            intent.putExtra("ble", "BLE's name~~~~~"); // 블루투스 이름
//                            intent.putExtra("name", user); // user
                            startActivity(intent);
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }

    private void updateUI(FirebaseUser user) {

    }
}