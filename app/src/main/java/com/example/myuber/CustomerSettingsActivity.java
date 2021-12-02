package com.example.myuber;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//Setting Page for editing Customer Profile
public class CustomerSettingsActivity extends AppCompatActivity {

    //Declaration for Layout Components and necessary variables
    private EditText mNameField , mPhoneField;

    private Button mConfirm , mBack;

    private ImageView mProfileImage;


    private FirebaseAuth mAuth;

    private DatabaseReference mCustomerDatabase;


    private String UserId , mName , mPhone , mProfileImageUrl;

    private Uri resultUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);

        //Initialising Layout Components
         mNameField = (EditText) findViewById(R.id.name);
         mPhoneField = (EditText) findViewById(R.id.phone);

         mProfileImage = (ImageView) findViewById(R.id.profileImage);

         mConfirm = (Button) findViewById(R.id.confirm);
         mBack = (Button) findViewById(R.id.back);

         mAuth = FirebaseAuth.getInstance();
         UserId = mAuth.getCurrentUser().getUid();
         mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(UserId);

         //Gets Current User Info
         getUserInfo();

         //On clicking Profile Image
         mProfileImage.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 //Picking an image from already existing images on the device
                 Intent intent = new Intent(Intent.ACTION_PICK);
                 intent.setType("image/*");
                 startActivityForResult(intent , 1);
             }
         });

         //Saving changed data on clicking
         mConfirm.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 saveUserInformation();
             }
         });

         //Going back into map Activity
         mBack.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 finish();
                 return;
             }
         });
    }



    //For Getting User Information from Firebase
    private void getUserInfo(){
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Getting Data on Database Reference to user Data Document
                if(snapshot.exists() && snapshot.getChildrenCount()>0){
                    Map<String , Object> map = (Map<String, Object>) snapshot.getValue();
                    if(map.get("name") != null){
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                    }
                    if(map.get("phone") != null){
                        mPhone = map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    }
                    if(map.get("profileImageUrl") != null){
                        mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImageUrl).into(mProfileImage);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //Saving Changed Information
    private void saveUserInformation() {

        mName = mNameField.getText().toString();
        mPhone = mPhoneField.getText().toString();
        Map userInfo = new HashMap();
        userInfo.put("name" , mName);
        userInfo.put("phone" , mPhone);
        mCustomerDatabase.updateChildren(userInfo);

        //Storing Image URI in Firebase and image by serialisation and deserialization
        if(resultUri != null){

            StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profileImages").child(UserId);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver() , resultUri);
            }catch (IOException e){

            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG , 20 , baos);
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);
            filePath.getDownloadUrl().addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });
            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Uri downloadUrl = uri;
                    Toast.makeText(CustomerSettingsActivity.this , "Image Success! URL - "+ downloadUrl.toString() ,Toast.LENGTH_SHORT ).show();
                    Map newImage = new HashMap();
                    newImage.put("profileImageUrl" , downloadUrl.toString());
                    mCustomerDatabase.updateChildren(newImage);

                    finish();
                    return;
                }
            });
        }else{
            finish();
        }


    }

    //Method Called when Image is aselected from phone to get image data
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImage.setImageURI(resultUri);
        }
    }
}