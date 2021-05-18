package com.logismart.logismart;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AuthActivity extends Activity {

    private static final String TAG = "AuthActivity";

    private FirebaseAuth mAuth;

    private SharedPreferences mPreferences;
    private String SharedPrefFile = "com.logismart.android.SharedPreferences";

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private Button backBtn;
    private Button authCallBtn;
    private Button reAuthBtn;
    private Button authBtn;
    private EditText phoneText;
    private EditText codeText;

//    private ProgressBar pb;
    private ProgressDialog customProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_auth);

        BtnOnClick btnOnClick = new BtnOnClick();

        backBtn = findViewById(R.id.back_btn);
        backBtn.setOnClickListener(btnOnClick);
        authCallBtn = findViewById(R.id.authcall_btn);
        authCallBtn.setOnClickListener(btnOnClick);
        reAuthBtn = findViewById(R.id.reauth_btn);
        reAuthBtn.setOnClickListener(btnOnClick);
        authBtn = findViewById(R.id.auth_btn);
        authBtn.setOnClickListener(btnOnClick);

        phoneText = (EditText) findViewById(R.id.phone_input);
        codeText = (EditText) findViewById(R.id.authnum_input);

//        pb = (ProgressBar) findViewById(R.id.loading);

        customProgressDialog = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();
        mVerificationId = "";

        mPreferences = getSharedPreferences(SharedPrefFile, MODE_PRIVATE);

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // 1 - Instant verification
                // 2 - Auto-retrieval
                Log.d(TAG, "onVerificationCompleted:" + credential);

                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e);

                updateUI(1);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Toast.makeText(AuthActivity.this, "인증 실패", Toast.LENGTH_SHORT).show();
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
                updateUI(1);
                Toast.makeText(AuthActivity.this, "인증번호 전송", Toast.LENGTH_SHORT).show();
                mVerificationId = verificationId;
                mResendToken = token;
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(@NonNull String verificationId) {
                super.onCodeAutoRetrievalTimeOut(verificationId);
                if (mAuth.getCurrentUser() == null) {
                    Log.d(TAG, "onCodeAutoRetrievalTimeOut: TimeOut");
                    updateUI(1);
                    mVerificationId = verificationId;
                    Toast.makeText(AuthActivity.this, "시간초과", Toast.LENGTH_SHORT).show();
                }
            }
        };

        reAuthBtn.setClickable(false);

//        customProgressDialog.setContentView(R.layout.dialog_progress);
//        customProgressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
//        customProgressDialog.setCancelable(false);
//        customProgressDialog.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
//        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.d(TAG, "onStart called");
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "onStart: mAuth exist");
            moveActivity();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    class BtnOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String phone = phoneText.getText().toString().replace(" ", "");
            String phonenumber;
            switch (v.getId()) {
                case R.id.back_btn:
                    finish();
                    break;
                case R.id.authcall_btn:
                    Log.d(TAG, "onClick: " + !phone.startsWith("01") + (phone.length() < 10) + !Pattern.matches("^[0-9]*$", phone));
                    if (!phone.startsWith("01") || phone.length() < 10 || !Pattern.matches("^[0-9]*$", phone)) {
                        Toast.makeText(AuthActivity.this, "올바른 번호를 써주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    authCallBtn.setClickable(false);
                    reAuthBtn.setClickable(true);
                    updateUI(0);
                    phonenumber = "+82" + phone.substring(1);
                    startPhoneNumberVerification(phonenumber); // Korea
                    break;
                case R.id.reauth_btn:
                    if (!phone.startsWith("01") || phone.length() < 10 || !Pattern.matches("^[0-9]*$", phone)) {
                        Toast.makeText(AuthActivity.this, "올바른 번호를 써주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateUI(0);
                    phonenumber = "+82" + phone.substring(1);
                    resendVerificationCode(phonenumber, mResendToken);
                    break;
                case R.id.auth_btn:
                    if (phone.isEmpty() || mVerificationId.isEmpty()) {
                        Toast.makeText(AuthActivity.this, "먼저 인증 요청을 해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String code = codeText.getText().toString();
                    if (code.isEmpty()) {
                        Toast.makeText(AuthActivity.this, "인증 번호를 써주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateUI(0);
                    verifyPhoneNumberWithCode(mVerificationId, code);
                    break;
            }
        }
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
        signInWithPhoneAuthCredential(credential);
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
                            Toast.makeText(AuthActivity.this, "인증 성공", Toast.LENGTH_SHORT).show();
//                            FirebaseUser user = task.getResult().getUser();
                            savetoSharedPrefPhone(phoneText.getText().toString());
                            // Update UI
                            moveActivity();
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            updateUI(1);
                            Toast.makeText(AuthActivity.this, "인증 실패", Toast.LENGTH_SHORT).show();
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }

    private void savetoSharedPrefPhone(String phone) {
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();

        // data
        preferencesEditor.putString("phone", phone);

        preferencesEditor.apply();
    }

    private void updateUI(int check) {
        if (check == 0) { // click auth
            authCallBtn.setClickable(false);
            reAuthBtn.setClickable(false);
            authBtn.setClickable(false);
            phoneText.setInputType(InputType.TYPE_NULL);
            codeText.setInputType(InputType.TYPE_NULL);
//            pb.setVisibility(View.VISIBLE);
//            customProgressDialog.show();
        }
        else if (check == 1) { // fail auth
            reAuthBtn.setClickable(true);
            authBtn.setClickable(true);
            phoneText.setClickable(true);
            codeText.setClickable(true);
            phoneText.setInputType(InputType.TYPE_CLASS_PHONE);
            codeText.setInputType(InputType.TYPE_CLASS_PHONE);
//            pb.setVisibility(View.INVISIBLE);
//            customProgressDialog.dismiss();
        }
        else if (check == 2) { // success auth
//            pb.setVisibility(View.INVISIBLE);
//            customProgressDialog.dismiss();

            authBtn.setBackgroundColor(Color.GRAY);
            authBtn.setText("인증완료");
        }
    }

    private void moveActivity() { // app -> SQL
        Intent intent = new Intent(AuthActivity.this, WriteInfoActivity.class);
        startActivity(intent);
    }
}