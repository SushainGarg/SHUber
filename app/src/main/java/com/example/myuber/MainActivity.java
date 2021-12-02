package com.example.myuber;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

//intent When Application is initiated on a device
public class MainActivity extends AppCompatActivity {
    private Button nDriver , nCustomer;

    //First method to be called
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initiating Driver and customer buttons
        nDriver = findViewById(R.id.driver);
        nCustomer = findViewById(R.id.customer);

        //Navigating to DriverLoginActivity on clicking Driver button
        startService(new Intent(MainActivity.this , onAppKilled.class));
        nDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this , DriverLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        //Navigating to CustomerLoginActivity on clicking Customer button
        nCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this , CustomerLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

    }
}