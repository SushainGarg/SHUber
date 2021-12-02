package com.example.myuber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.example.myuber.databinding.ActivityCustomerMapBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, com.google.android.gms.location.LocationListener {

    //Creating object for GoogleMap API(it is deprecated but works fine)
    private GoogleMap mMap;
    private ActivityCustomerMapBinding binding;
    GoogleApiClient mGoogleApiClient;


    //Creating necessary Objects with appropriate identifiers

    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button mLogout , mRequest , mSettings , mHistory;
    Marker pickupMarker;

    private String destination , requestService;

    private SupportMapFragment mapFragment;

    private LatLng pickupLocation , destinationLatLng;

    private Boolean requestBol = false;

    private LinearLayout mDriverInfo;

    private ImageView mDriverProfileImage;

    private TextView mDriverName , mDriverPhone , mDriverCar;

    private RadioGroup mRadioGroup;

    private RatingBar mRatingBar;

    private int radius = 1;

    private boolean driverFound = false;

    private String driverFoundId;

    GeoQuery geoQuery;

    String value = "";

    DatabaseReference driveHasEndedRef;

    private ValueEventListener driveHasEndedRefListener;

    Marker mDriverMarker;

    private DatabaseReference driverRef;

    private ValueEventListener driverRefListner;

    final int LOCATION_REQUEST_CODE = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the SDK
        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));

        PlacesClient placesClient = Places.createClient(this);

        binding = ActivityCustomerMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        //Checking Permission on device for Location Access and Sync map on success
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }else{
            mapFragment.getMapAsync(this);
        }
        mapFragment.getMapAsync(this);

        //initiating Destination Location Coordinates
        destinationLatLng = new LatLng(0.0 , 0.0);

        //Initiating Driver Layout Components
        mDriverInfo = (LinearLayout) findViewById(R.id.driverInfo);

        mDriverProfileImage = (ImageView) findViewById(R.id.driverProfileImage);

        //TextView Components
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverCar = (TextView) findViewById(R.id.driverCar);

        //Button Components
        mLogout = (Button) findViewById(R.id.logout);
        mRequest = (Button)findViewById(R.id.request);
        mSettings = (Button)findViewById(R.id.settings);
        mHistory = (Button)findViewById(R.id.history);

        //RadioGroup and RatingBar Components
        mRadioGroup = findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.UberX);

        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);

        //Method called on Logout Clicked
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Successfully Logs out User from the firebase and thus the application and moves to MainActivity
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this , MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        //Method Called On Clicking Call Driver Button
        mRequest.setOnClickListener(v -> {
            //Method For creating A request and finding , booking driver Location Info , getting Driver Details and Route to customer
           request();
        });

        //Method Called onClicking the Settings Button
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Navigates to the CustomerSettingsActivity Page where one can edit their profile info
                Intent intent = new Intent(CustomerMapActivity.this , CustomerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        //Method Called onClicking the History Button
        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Navigates to the HistoryActivity Page where you can see all the previous rides of the user
                Intent intent = new Intent(CustomerMapActivity.this , HistoryActivity.class);
                intent.putExtra("customerOrDriver" ,"Customers" );
                startActivity(intent);
                return;
            }
        });


        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME , Place.Field.LAT_LNG));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                //Gets Destination Place name and Coordinates
                destination = place.getName().toString();
                if(destinationLatLng!=null){
                    destinationLatLng = place.getLatLng();
                }

            }
            @Override
            public void onError(@NonNull Status status) {

            }
        });
    }


    //Request Method used to create a customer Request for ride
    private void request() {
        //checking if Request exits
        if(requestBol){
            endRide();
        }else{

            //Comparing Uber Service Types(UberX , UberXL etc..)
            int SelectedId = mRadioGroup.getCheckedRadioButtonId();
            final RadioButton radioButton = (RadioButton) findViewById(SelectedId);
            if(radioButton.getText() == null){
                return;
            }
            requestService = radioButton.getText().toString();
            requestBol = true;

            //Getting Current Customer Database Id
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");

            //Adding current user Location Using GeoFire
            GeoFire geoFire = new GeoFire(ref);
            geoFire.setLocation(userId , new GeoLocation(mLastLocation.getLatitude() , mLastLocation.getLongitude()));

            //Adding Request Status with the request
            HashMap map = new HashMap();
            map.put("isTaken","");
            ref.child(userId).updateChildren(map);

            //Adding Marker on Pickup Location(Current Customer Location)
            pickupLocation = new LatLng(mLastLocation.getLatitude() , mLastLocation.getLongitude());
            pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here"));

            mRequest.setText("Getting your driver....");

            //Searching and Getting closest Driver
            getClosestDriver();
        }
    }


    private void getClosestDriver() {
        //Creating DatabaseReference to Driver Available Document
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        //Getting Locations of drivers Stored in Drivers Available
        GeoFire geoFire = new GeoFire(driverLocation);

        //Creating a Query to search for driver nearest based on the current Customer Location and Radius which starts
        //from 1 and increments by 1 at each unsuccessful find
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude , pickupLocation.longitude) , radius);
        geoQuery.removeAllListeners();

        //Listener to check if driver found on current Radius or not(Recursive Function)
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            //Called When There is a change probable if driver is found
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestBol){

                    //Creating DatabaseReference to the Driver whose Id Matches the parameter key of the function
                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    //Adding a value listener for the current Driver Found
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists() && snapshot.getChildrenCount()>0){

                                //Creating a Customer Request in the Driver Id Document with ride Details
                                Map<String , Object> driverMap = (Map<String, Object>) snapshot.getValue();
                                if(driverFound){
                                    return;
                                }

                                if(driverMap.get("service").equals(requestService)){

                                    driverFound = true;
                                    driverFoundId = snapshot.getKey();

                                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
                                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("customerRideId",customerId);
                                    map.put("destination",destination);
                                    if(destinationLatLng!=null){
                                        map.put("destinationLat",destinationLatLng.latitude);
                                        map.put("destinationLng",destinationLatLng.longitude);
                                    }else{
                                        map.put("destinationLat",0);
                                        map.put("destinationLng",0);
                                    }
                                    ref.updateChildren(map);

                                    //Checking for driver consent on ride
                                    acceptanceCheck();

                                    //Checking if ride has ended is called continuously in the background every time listener is
                                    //activated
                                    getHasRideEnded();


                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            //Called When driver is not found in current Radius
            @Override
            public void onGeoQueryReady() {
                if(!driverFound){
                    radius++;
                    getClosestDriver();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    //Acceptance Check to check whether driver has accepted or Declined ride
    private void acceptanceCheck() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest").child(userId);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    //Checking Driver Response using "isTaken" key in document
                    Map<String , Object> map = (Map<String, Object>) snapshot.getValue();
                    if(map.get("isTaken") != null){
                        value = map.get("isTaken").toString();
                        if(value.equals("1")){
                            mRequest.setText("Looking for driver Location....");
                            getDriverLocation();
                            getDriverInfo();
                        }else if(value.equals("0")){
                            map.remove("isTaken");
                            ref.updateChildren(map);
                            driverFound = false;
                            driverFoundId = "";
                            request();
                        }else if(value.equals("")){
                            //Checking till we get a response
                            acceptanceCheck();
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    //Getting ride Driver Information
    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        //Creating Database Reference to Driver Document
        DatabaseReference mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        mDriverDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //Getting Driver Details through Database Reference and displaying them to customer
                if(snapshot.exists() && snapshot.getChildrenCount()>0){
                    if(snapshot.child("name")!= null){
                        mDriverName.setText(snapshot.child("name").getValue().toString());
                    }
                    if(snapshot.child("phone")!= null){
                        mDriverPhone.setText(snapshot.child("phone").getValue().toString());
                    }
                    if(snapshot.child("car")!= null){
                        mDriverCar.setText(snapshot.child("car").getValue().toString());
                    }
                    if(snapshot.child("profileImageUrl")!= null){
                        Glide.with(getApplication()).load(snapshot.child("profileImageUrl").toString()).into(mDriverProfileImage);
                    }

                    //Rating Driver can be done anytime during and after the ride
                    int ratingSum = 0;
                    float ratingTotal = 0;
                    float ratingAvg = 0;
                    for(DataSnapshot child:snapshot.child("rating").getChildren()){
                        ratingSum += Integer.parseInt(child.getValue().toString());
                        ratingTotal++;
                    }
                    if(ratingTotal!= 0){
                        ratingAvg = ratingSum/ratingTotal;
                        mRatingBar.setRating(ratingAvg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    //Checking Ride Status After Ride Start
    private void getHasRideEnded() {
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest").child("customerRideId");
        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){


                }else {
                    //Ends Ride if Snapshot does not exist
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    //Method used to end ride on ride cancel from driver or customer side
    private void endRide(){
        requestBol = false;

        //Removing All Listeners if they exist
        geoQuery.removeAllListeners();
        if(driverRefListner != null){
            driverRef.removeEventListener(driverRefListner);
            driveHasEndedRef.removeEventListener(driveHasEndedRefListener);
        }


        //Destroying CustomerRequest Document in driver document of DriverId
        if(driverFoundId != null){
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
            ref.removeValue();
            driverFoundId = null;
        }

        driverFound = false;
        radius = 1;

        //Destroying Customer Request Document
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        ref.removeValue();
        geoFire.removeLocation(userId);

        //Resetting Default Values
        if(pickupMarker!=null){
            pickupMarker.remove();
        }
        if(mDriverMarker!=null){
            mDriverMarker.remove();
        }
        mRequest.setText("Call Taxi");

        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.mipmap.ic_launcher);
    }

    //Getting Location Of Driver Found
    private void getDriverLocation() {
        driverRef = FirebaseDatabase.getInstance().getReference().child("driverWorking").child(driverFoundId).child("l");
        driverRefListner = driverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot.exists() && requestBol){
                    //Getting Location Coordinates from snapshot
                    List<Object> map = (List<Object>) snapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Driver Found");
                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng DriverlatLng = new LatLng(locationLat , locationLng);
                    if(mDriverMarker != null){
                        mDriverMarker.remove();
                    }

                    //Setting Coordinates for Pickup and Driver Location in Local Identifier
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(DriverlatLng.latitude);
                    loc2.setLongitude(DriverlatLng.longitude);

                    float distance = (loc1.distanceTo(loc2))/1000;

                    if(distance<0.5){
                        mRequest.setText("Driver is Here: ");
                    }
                    //Displaying Distance of driver from Customer in real time
                    else{
                        mRequest.setText("Driver Found: " + String.valueOf(distance));
                    }

                    //Setting Driver marker(updates Realtime)
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(DriverlatLng).title("Your Driver"));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //Setting up and enabling map fragment
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);


    }

    //Building a GoogleApiClient Object and initialising it
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    //Connecting to map and updating location in realtime
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        Boolean con = mGoogleApiClient.isConnected();
        if(con){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //Showing Current User Location On Map
    @Override
    public void onLocationChanged(@NonNull Location location) {
        mLastLocation = location;
        Toast.makeText(CustomerMapActivity.this , "Location accessed"  , Toast.LENGTH_SHORT).show();
        LatLng latLng = new LatLng(location.getLatitude() , location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

    }


    //Checking and Getting Permission to access device location
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case LOCATION_REQUEST_CODE:{
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this);
                }else{
                    Toast.makeText(CustomerMapActivity.this, "Please Provide the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            }

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}