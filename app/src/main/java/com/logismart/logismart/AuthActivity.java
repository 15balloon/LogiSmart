package com.logismart.logismart;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends Activity {

    private static final String TAG = "AuthActivity";

    private FirebaseAuth mAuth;

    private RetrofitService retrofit;

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

    private TextView personalText;
    private TextView nameInfo;
    private EditText nameText;
    private TextView birthInfo;
    private EditText birthText;
    private Button completeBtn;

    private ProgressBar pb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_auth);

        backBtn = findViewById(R.id.back_btn);
        authCallBtn = findViewById(R.id.authcall_btn);
        reAuthBtn = findViewById(R.id.reauth_btn);
        authBtn = findViewById(R.id.auth_btn);
        phoneText = (EditText) findViewById(R.id.phone_input);
        codeText = (EditText) findViewById(R.id.authnum_input);

        personalText = findViewById(R.id.personal);
        nameInfo = findViewById(R.id.name_text);
        nameText = findViewById(R.id.name_input);
        birthInfo = findViewById(R.id.birth_text);
        birthText = findViewById(R.id.birth_input);
        completeBtn = findViewById(R.id.complete_btn);

        pb = (ProgressBar) findViewById(R.id.loading);

        mAuth = FirebaseAuth.getInstance();

        retrofit = RetrofitBuilder.getRetrofit().create(RetrofitService.class);

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

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        authCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = phoneText.getText().toString().replace(" ", "");
                if (phone.isEmpty() || !phone.startsWith("01") || phone.length() != 10 || !Pattern.matches("^[0-9]*$", phone)) {
                    Toast.makeText(AuthActivity.this, "올바른 번호를 써주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                authCallBtn.setClickable(false);
                reAuthBtn.setClickable(true);
                updateUI(0);
                String phonenumber = "+82" + phone.substring(1);
                startPhoneNumberVerification(phonenumber); // Korea
            }
        });

        reAuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = phoneText.getText().toString().replace(" ", "");
                if (phone.isEmpty() || !phone.startsWith("01") || phone.length() != 10 || !Pattern.matches("^[0-9]*$", phone)) {
                    Toast.makeText(AuthActivity.this, "올바른 번호를 써주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateUI(0);
                String phonenumber = "+82" + phone.substring(1);
                resendVerificationCode(phonenumber, mResendToken);
            }
        });

        authBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUI(0);
                verifyPhoneNumberWithCode(mVerificationId, codeText.getText().toString());
            }
        });

        completeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUI(3);
                // app -> SQL
                savetoSQL();
            }
        });

        reAuthBtn.setClickable(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
//        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "onStart: mAuth exist");
            String data = mPreferences.getString("name", "nothing");
            if (data != "nothing")
//            if (true)
                moveActivity();
            else {
                updateUI(0);
                updateUI(2);
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
                            updateUI(2);
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

    private void savetoSQL() {
        // app -> SQL
        // GSON parsing confirm
        String name = nameText.getText().toString();
        String birth = birthText.getText().toString();
        String phone = mPreferences.getString("phone", "null");
        retrofit.save_info(name, birth, phone)
                            .enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                                    if (response.isSuccessful()) {
                                        Log.d(TAG, "onResponse: SUCCESS");
                                        Toast.makeText(AuthActivity.this, "전송 성공", Toast.LENGTH_SHORT).show();
                                        savetoSharedPrefName(name);

                                    }
                                    else {
                                        Log.d(TAG, "onResponse: FAIL");
                                        updateUI(4);
                                        Toast.makeText(AuthActivity.this, "전송 실패", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<JsonObject> call, Throwable t) {
                                    Log.d(TAG, "onFailure");
                                    t.printStackTrace();
                                    updateUI(4);
                                    Toast.makeText(AuthActivity.this, "전송 실패", Toast.LENGTH_SHORT).show();
                                }
                            });
    }

    private void savetoSharedPrefPhone(String phone) {
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();

        // data
        preferencesEditor.putString("phone", phone);

        preferencesEditor.apply();
    }

    private void savetoSharedPrefName(String name) {
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();

        // data
        preferencesEditor.putString("name", name);

        preferencesEditor.apply();
    }

    private void updateUI(int check) {
        if (check == 0) { // click auth
            authCallBtn.setClickable(false);
            reAuthBtn.setClickable(false);
            authBtn.setClickable(false);
            phoneText.setInputType(InputType.TYPE_NULL);
            codeText.setInputType(InputType.TYPE_NULL);
            pb.setVisibility(View.VISIBLE);
        }
        else if (check == 1) { // fail auth
            reAuthBtn.setClickable(true);
            authBtn.setClickable(true);
            phoneText.setClickable(true);
            codeText.setClickable(true);
            phoneText.setInputType(InputType.TYPE_CLASS_PHONE);
            codeText.setInputType(InputType.TYPE_CLASS_PHONE);
            pb.setVisibility(View.INVISIBLE);
        }
        else if (check == 2) { // success auth
            pb.setVisibility(View.INVISIBLE);
            personalText.setVisibility(View.VISIBLE);
            nameInfo.setVisibility(View.VISIBLE);
            nameText.setVisibility(View.VISIBLE);
            birthInfo.setVisibility(View.VISIBLE);
            birthText.setVisibility(View.VISIBLE);
            completeBtn.setVisibility(View.VISIBLE);
        }
        else if (check == 3) { // complete write personal info
            nameText.setInputType(InputType.TYPE_NULL);
            birthText.setInputType(InputType.TYPE_NULL);
            completeBtn.setClickable(false);
            pb.setVisibility(View.VISIBLE);
        }

        else if (check == 4) { // fail to response personal info
            nameText.setInputType(InputType.TYPE_CLASS_TEXT);
            birthText.setInputType(InputType.TYPE_CLASS_PHONE);
            completeBtn.setClickable(true);
            pb.setVisibility(View.INVISIBLE);
        }
    }

    private void moveActivity() { // app -> SQL
        Intent intent = new Intent(AuthActivity.this, WaitActivity.class);
        // From SQL
//        intent.putExtra("ble", "LogiSmart"); // ble name
//        intent.putExtra("name", ); // user name
//        intent.putExtra("from", ); // starting point
//        intent.putExtra("to", ); // destination
        startActivity(intent);
    }

    // implementation 'com.google.code:gson:gson:2.8.5'
}