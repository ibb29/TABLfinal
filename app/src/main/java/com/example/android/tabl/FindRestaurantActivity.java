package com.example.android.tabl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.example.android.tabl.utils.RecyclerItemClickListener;
import com.example.android.tabl.restaurant_recyclerview.Restaurant;
import com.example.android.tabl.restaurant_recyclerview.RestaurantsAdapter;
import com.example.android.tabl.utils.TablUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * First main screen of TABL. Allows user to select the restaurant they intend to order from using
 * either a map or search function.
 *
 * @WRFitch
 */

/**
 * TODO: implement snapToLocation() on floatingActionButton
 * TODO: implement/update map utilities.
 * TODO: implement additional search method in appbar.
 * TODO: implement passing restaurant data to MenuActivity
 * TODO: clean up this class
 * TODO: fix getLocation
 * TODO:
 */

public class FindRestaurantActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private List<Restaurant> restaurantList= new ArrayList<>();
    private RecyclerView recyclerView;
    private RestaurantsAdapter rAdapter;
    private FloatingActionButton fab;

    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private Location mCurrentLocation;
    private final static String KEY_LOCATION = "location";
    private final float DEFAULT_ZOOM = 16f;

    /*
     * Define a request code to send to Google Play services This code is
     * returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_restaurant);

        if (savedInstanceState != null && savedInstanceState.keySet().contains(KEY_LOCATION)) {
            mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        }

        mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override // see below - dupe method
            public void onMapReady(GoogleMap map) {
                loadMap(map);
            }
        });

        fab = findViewById(R.id.snapToLocationButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snapToCurrentLocation(view);
            }
        });

        recyclerView = findViewById(R.id.find_restaurant_recyView);
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                        restaurantList.get(position).getMenuTitles();
                        //pass menuTitles to menuactivity
                        //preload favourites menu
                        callMenuActivity(getApplicationContext());
                    }

                    @Override public void onLongItemClick(View view, int position) {
                        //perhaps use this to display restaurant info/add to favourites?
                        TablUtils.functionNotImplemented(view, "maybe add to favourites?");
                    }
                })
        );

        rAdapter = new RestaurantsAdapter(restaurantList);
        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(
                getApplicationContext());
        recyclerView.setLayoutManager(rLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(rAdapter);
        prepRestaurantData();
        showRestaurantsOnMap();
    }

    //check if the user has location services on when returning to the application
    @Override
    protected void onStart() {
        super.onStart();
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    }

    protected void loadMap(GoogleMap googleMap){
        mMap = googleMap;
        if(!checkLocationPermission()){
            //find a good way to handle errors!
            TablUtils.errorMsg(fab, "failed to get permissions!");
            finish();//or call search function?
        }
        getLocation();
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setMyLocationEnabled(true);

        LatLng userLocation = new LatLng(-34, 151);//getUserLocation();
        mMap.addMarker(new MarkerOptions().position(userLocation).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));

    }

    //how is this different from above?
    @Override
    public void onMapReady(GoogleMap googleMap) {
        loadMap(googleMap);
    }

    @SuppressLint("MissingPermission")
    public void getLocation() {
        // get location using both network and gps providers, no need for permission check as that is done before the method is called
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1000, this);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1000, this);
    }

    @SuppressLint("MissingPermission")
    public void getUserLocation(){
        //edit this to prefer gps then use network if insufficient
        getLocation();
        Location currentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM));
    }

    //returns true if we have location permission. would be more robust if returned false.
    private Boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]
                            { Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION },
                    1);
        } else {
            return false;
        }
        return true;
    }

    public void updateMarker(Location currentLocation) {
        LatLng currentLatlng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatlng, DEFAULT_ZOOM));
    }

    public void onLocationChanged(Location location) {
        updateMarker(location);
    }

    public void onStatusChanged(String provider, int status, Bundle extras){}

    public void onProviderEnabled(String provider) {
        if(checkLocationPermission())
            getLocation();
    }

    public void onProviderDisabled(String provider) {
        checkLocationPermission();
    }

    ///////////////////////////////////////////////////////////////////////////

    private void snapToCurrentLocation(View v){
        TablUtils.errorMsg(v, "half-implemented");
        getUserLocation();
    }

    private void showRestaurantsOnMap(){
        for(Restaurant r: restaurantList){
            //mMap.addMarker(new MarkerOptions().position(r.getLocation()).title(r.getName()));
        }
    }

    private void prepRestaurantData(){
        //current implementation uses test data! something like i->getCachedRestaurants
        for(int i=0; i<5; i++){
            restaurantList.add(new Restaurant(FindRestaurantActivity.this));
        }
        rAdapter.notifyDataSetChanged();
    }

    //call next activity. Make sure to pass parcelable restaurant data.
    private void callMenuActivity(Context c) {
        Intent intent = new Intent(c, MenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}

