package com.yashraj.skillerpartnerapp.RegistrationActivities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.chaos.view.PinView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.yashraj.skillerpartnerapp.DashBoardActivity;
import com.yashraj.skillerpartnerapp.Model.Vendor;
import com.yashraj.skillerpartnerapp.R;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class PhoneAuthenticationActivity extends AppCompatActivity {
    TextView phoneNumber;
    Button verifyPhone;
    PinView pin;
    String codeBySystem;
    String userName, userEmail, userPassword, userGender, userAge, userOccupation, userPhone;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            codeBySystem = s;

        }

        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            String code = phoneAuthCredential.getSmsCode();
            if (code != null) {
                pin.setText(code);
                verifyCode(code);
            }

        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            Toast.makeText(PhoneAuthenticationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_authentication);
        //hooks
        phoneNumber = findViewById(R.id.phoneNumber);
        verifyPhone = findViewById(R.id.verify_button);
        pin = findViewById(R.id.pin);
        mAuth = FirebaseAuth.getInstance();
        //Getting data from the previous activities
        userName = getIntent().getStringExtra("name");
        userEmail = getIntent().getStringExtra("email");
        userPassword = getIntent().getStringExtra("password");
        userGender = getIntent().getStringExtra("gender");
        userAge = getIntent().getStringExtra("age");
        userOccupation = getIntent().getStringExtra("occupation");
        userPhone = getIntent().getStringExtra("phone");
        phoneNumber.setText(userPhone);
        sendVerificationCodeToUser(userPhone);
        verifyPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = pin.getText().toString();
                if (!code.isEmpty()) {
                    verifyCode(code);
                }

            }
        });


    }

    private void sendVerificationCodeToUser(String userPhone) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                userPhone,        //phoneNumber to verify
                60,            //Timeout Duration
                TimeUnit.SECONDS,  //Unit of timeout
                TaskExecutors.MAIN_THREAD,      //Activity(for callback binding)
                mCallbacks);      //OnVerificationStateChangedCallbacks

    }

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(codeBySystem, code);
        signInUsingCredential(credential);


    }

    private void signInUsingCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    storeNewUserData();    // Method to store the data into the firebase

                } else {
                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(PhoneAuthenticationActivity.this, "Verification Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });


    }

    private void storeNewUserData() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference reference = database.getReference("Vendors");
        Vendor addNewUser = new Vendor(userName, userEmail, userPassword, userGender, userAge, userOccupation, userPhone);
        reference.child(mAuth.getCurrentUser().getUid()).setValue(addNewUser).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(PhoneAuthenticationActivity.this, "Verification successful", Toast.LENGTH_SHORT).show();
                    sendUserToMainActivity();

                } else {
                    Toast.makeText(PhoneAuthenticationActivity.this, "Some error occurred, please try again later", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // This part to be deleted later.
        HashMap<String, Object> map = new HashMap<>();
        map.put("description", "You have got a new work");
        map.put("location", "Agra");
        map.put("phoneNo", "9917056564");
        map.put("charges", "500Rs/day");
        map.put("duration", "7days");

        DatabaseReference newTask = database.getReference("NewTask");
        newTask.child(mAuth.getCurrentUser().getUid()).setValue(map);


    }

    // Method to send user to Dashboard acivity after OTP verification
    private void sendUserToMainActivity() {
        Intent intent = new Intent(PhoneAuthenticationActivity.this, DashBoardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}