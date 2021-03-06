package com.example.myuber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.myuber.databinding.ActivityDriverMapBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, com.google.android.gms.location.LocationListener , RoutingListener {

    //Declaring Identifiers for objects from classes used and Layout components
    private static final String STOP = "onStop";

    private GoogleMap mMap;

    private ActivityDriverMapBinding binding;

    GoogleApiClient mGoogleApiClient;

    Location mLastLocation;

    LocationRequest mLocationRequest;

    private Button mLogout,mSettings , mRideStatus , mHistory , mAccept , mCancel;

    private int status = 0;

    String userId;

    private float rideDistance;

    private SupportMapFragment mapFragment;

    private String customerId = "", destination , tempId = "";

    private LatLng destinationLatLng , PickuplatLng;

    private Boolean isLoggingOut;

    String Acceptance = "";

    private LinearLayout mCustomerInfo;

    private ImageView mCustomerProfileImage;

    private TextView mCustomerName , mCustomerPhone , mCustomerDestination;

    private SwitchMaterial mWorkingSwitch;

    Marker pickupMarker;

    private DatabaseReference assignedCustomerPickupLocationRef;

    private ValueEventListener assignedCustomerPickupLocationRefListener;

    final int LOCATION_REQUEST_CODE = 1;

    private List<Polyline> polylines;

    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};


    //First Method Called on Intent Creation
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDriverMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //Initialising polylines Arraylist for Storing Route Data Points to create route Lines
        polylines = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        //Checking Device Location Access Permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapActivity.this , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }else{
            mapFragment.getMapAsync(this);
        }

        mapFragment.getMapAsync(this);

        //Initialising Layout Components
        mCustomerInfo = (LinearLayout) findViewById(R.id.CustomerInfo);

        mCustomerProfileImage = (ImageView) findViewById(R.id.customerProfileImage);

        mCustomerPhone = (TextView) findViewById(R.id.customerPhone);
        mCustomerName = (TextView) findViewById(R.id.customerName);
        mCustomerDestination = (TextView) findViewById(R.id.customerDestination);
        mSettings = findViewById(R.id.settings);
        mLogout = findViewById(R.id.logout);
        mHistory = findViewById(R.id.history);
        mAccept = (Button) findViewById(R.id.accept);
        mCancel = (Button) findViewById(R.id.cancel);
        mWorkingSwitch = (SwitchMaterial) findViewById(R.id.workingSwitch);

        //Toggle Between Working and Not Working
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    connectDriver();
                }else {
                    disconnectDriver();
                }
            }
        });

        userId =FirebaseAuth.getInstance().getCurrentUser().getUid();
        mRideStatus  =(Button) findViewById(R.id.rideStatus);

        //Declares Current Ride Status(Customer Picked? , Ride Completed?)
        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status){
                    case 1:
                        //On Reaching Customer Pickup Point
                        status = 2;
                        erasePolylines();
                        if(destinationLatLng.latitude!=0.0 && destinationLatLng.longitude!=0.0){
                            getRouteToMarker(destinationLatLng);
                        }
                        mRideStatus.setText("Drive Completed");
                        break;
                    case 2:
                        //On Ride Completion
                        recordRide();
                        endRide();
                        break;
                }
            }
        });

        //For logging Out
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut = true;

                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapActivity.this , MainActivity.class);
                startActivity(intent);
                return;
            }
        });

        //To view Profile settings
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapActivity.this , DriverSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        //Tao View User History
        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapActivity.this , HistoryActivity.class);
                intent.putExtra("customerOrDriver" ,"Drivers" );
                startActivity(intent);
                return;
            }
        });

        //To Accept Ride on Invitation
        mAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Acceptance = "1";
                String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest").child(customerId);
                HashMap map = new HashMap();
                map.put("isTaken",Acceptance);

                ref.updateChildren(map);
                mRideStatus.setEnabled(true);
            }
        });
        //To Decline ride on Invitation
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               endRide();
            }
        });
        getAssignedCustomer();
    }

    //Gets Assigned Customer from Ride on Accepting Details
    private void getAssignedCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    //Ride Status Updated
                    status = 1;
                    customerId = snapshot.getValue().toString();
                    //Getting Customer Pickup Location  , Destination , Info
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();

                }else{
                    //Ending Ride on Completion or any exception
                   endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //Gets Customer Destination Information
    private void getAssignedCustomerDestination() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    //Customer Destination location name and Location Coordinates by default are set to 0 if
                    //customer has not chosen destination
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if(map.get("destination")!=null){
                        destination = map.get("destination").toString();
                        mCustomerDestination.setText("Destination: "+ destination);
                    }
                    else{
                        mCustomerDestination.setText("Destination: --");
                    }

                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if(map.get("destinationLat")!=null){
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if(map.get("destinationLng")!=null){
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatLng = new LatLng(destinationLat , destinationLng);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //Gets Customer Information
    private void getAssignedCustomerInfo() {
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Gets Customer Details and contact information thorough mCustomerDatabase DatabaseReference
                Map<String , Object> map = (Map<String, Object>) snapshot.getValue();
                if(snapshot.exists() && snapshot.getChildrenCount()>0){
                    if(map.get("name")!= null){
                        mCustomerName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!= null){
                        mCustomerName.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl")!= null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

  //Gets Customer Pickup Location
    private void getAssignedCustomerPickupLocation() {
        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l");
        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //gets Data through Database Reference assignedCustomerPickupLocationRef from CustomerId document in Customer Request
                //Also sets Route polylines to guide driver to pickup location
                if(snapshot.exists() && !customerId.equals("")){
                    List<Object> map = (List<Object>) snapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    PickuplatLng = new LatLng(locationLat , locationLng);
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(PickuplatLng).title("Pickup Location"));
                    //Draws Route Polylines to pickup Location
                    getRouteToMarker(PickuplatLng);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //Uses Open Source Code Directory to draw rout from 1 location coordinate to another
    private void getRouteToMarker(LatLng latLng) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude() , mLastLocation.getLongitude()), latLng)
                .key(getResources().getString(R.string.google_maps_key))
                .build();
        routing.execute();
    }

    //Method Called On ride End or ride Cancelation from either driver or customer
    private void endRide(){
        mRideStatus.setText("Picked Customer");
        //Removing Route Lines from Map
        erasePolylines();

        //Removing Listeners and Destroying Customer Request Document
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
        driverRef.removeValue();


        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerId);

        //Assigning Default Values
        customerId ="";
        Acceptance = "";
        rideDistance = 0;
        if(pickupMarker!=null){
            pickupMarker.remove();
        }
        if(assignedCustomerPickupLocationRefListener != null){
            assignedCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationRefListener);
        }
        mCustomerInfo.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerPhone.setText("");
        mCustomerDestination.setText("Destination: --");
        mCustomerProfileImage.setImageResource(R.mipmap.ic_launcher);
        mRideStatus.setEnabled(false);

    }

    //Recording Ride Details to display in History
    private void recordRide(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");
        String requestId = historyRef.push().getKey();
        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);
        //Putting Details in Key Value Pairs using Hashmap
        HashMap map = new HashMap();
        map.put("driver" , userId);
        map.put("customer" , customerId);
        map.put("rating" , 0);
        map.put("timestamp" , getCurrentTimeStamp());
        map.put("destination" , destination);
        map.put("location/from/lat" , PickuplatLng.latitude);
        map.put("location/from/lng" , PickuplatLng.longitude);
        map.put("location/to/lat" , destinationLatLng.latitude);
        map.put("location/to/lng" , destinationLatLng.longitude);
        map.put("distance" , rideDistance);
        historyRef.child(requestId).updateChildren(map);

    }

    //Getting Current Date and Time
    private Long getCurrentTimeStamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }

    //Initialising MAP Fragment
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapActivity.this , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    //Initialising GoogleApiClient and Connecting to maps
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    //Connecting and Checking Permissions also Updating location Realtime in specified Intervals
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if(mWorkingSwitch.isChecked()){
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(DriverMapActivity.this , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
            Boolean con = mGoogleApiClient.isConnected();
            if(con){
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //Connecting Driver so as to mark as working
    private void connectDriver(){

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapActivity.this , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        Boolean con = mGoogleApiClient.isConnected();
        if(con){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    //Updating data on Location Changed
    @Override
    public void onLocationChanged(@NonNull Location location) {
        if(getApplicationContext()!= null){

            if(!customerId.equals("")){
                rideDistance += mLastLocation.distanceTo(location)/1000;
            }

            mLastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude() , location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

            try {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid().toString();
                DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
                DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driverWorking");
                GeoFire geoFireAvailable = new GeoFire(refAvailable);
                GeoFire geoFireWorking= new GeoFire(refWorking);

                if(customerId == null || customerId.equals("")){
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailable.setLocation(userId , new GeoLocation(mLastLocation.getLatitude() , mLastLocation.getLongitude()));
                    Toast.makeText(DriverMapActivity.this, "Driver Availiable Accessed", Toast.LENGTH_SHORT).show();
                }else{
                    geoFireAvailable.removeLocation(userId);
                    geoFireWorking.setLocation(userId , new GeoLocation(mLastLocation.getLatitude() , mLastLocation.getLongitude()));
                    Toast.makeText(DriverMapActivity.this, "Driver Working Accessed", Toast.LENGTH_SHORT).show();
                }
            }catch (NullPointerException n){
                Toast.makeText(DriverMapActivity.this, "Null UserId Error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Disconnecting Driver from Database on stause NOT WORKING
    private void disconnectDriver(){
        Log.d(STOP , "Stop Called Successfully");
        Toast.makeText(DriverMapActivity.this, "OnStop Called", Toast.LENGTH_SHORT).show();
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient , this);
        try {
            String UserId = new String(FirebaseAuth.getInstance().getCurrentUser().getUid());
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");
            GeoFire geoFire = new GeoFire(ref);
            geoFire.removeLocation(UserId);
        }catch (NullPointerException n){
            Toast.makeText(DriverMapActivity.this, "Null UserId Exception", Toast.LENGTH_SHORT).show();
            String UserId = new String(FirebaseAuth.getInstance().getCurrentUser().getUid());
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");
            GeoFire geoFire = new GeoFire(ref);
            geoFire.removeLocation(UserId);
        }
    }

    //Requesting and Checking for Location Access Permissions from Device
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case LOCATION_REQUEST_CODE:{
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this);
                }else{
                    Toast.makeText(DriverMapActivity.this, "Please Provide the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            }

        }
    }


    //Drawing Route on MAP

    //On Any Exception this method is called
    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error:  " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    //Method for Drawing Polylines from given Location To Destination
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {
    }

    //Erasing Polylines on the event of Ride Ending
    private void erasePolylines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
}