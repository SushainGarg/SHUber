package com.example.myuber;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity {
    private EditText mEmail , mPassword;
    private Button mLogin , mRegisteration;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListner;

    //first Method Called on intent creation
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mAuth = FirebaseAuth.getInstance();

        //initiating AuthStateListener
        firebaseAuthListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //getting current user from firebase
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    //navigating to next Activity(DriverMapActivity) in case os successful signup or login
                    Intent intent = new Intent(DriverLoginActivity.this , DriverMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        //initiating Email and password editText
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);

        //Initiating Login and Registration Button
        mLogin = findViewById(R.id.login);
        mRegisteration = findViewById(R.id.registeration);

        //Initiating method on OnClick action of user on Registration Button
        mRegisteration.setOnClickListener(v -> {
            final String email = mEmail.getText().toString();
            final String password = mPassword.getText().toString();

            //Creating new User with given Details
            mAuth.createUserWithEmailAndPassword(email , password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()){
                        Toast.makeText(DriverLoginActivity.this , "Sign Up Error" , Toast.LENGTH_SHORT).show();
                    }else{
                        String user_id = mAuth.getCurrentUser().getUid();
                        DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(user_id).child("name");
                        current_user_db.setValue(email);
                        Toast.makeText(DriverLoginActivity.this, "Successful sign up", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        //Initiating method on OnClick action of user on Login Button
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();

                //Authenticating and logging in of user on correct data entered
                mAuth.signInWithEmailAndPassword(email , password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()){
                            Toast.makeText(DriverLoginActivity.this , "Login Error" , Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListner);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListner);
    }
}