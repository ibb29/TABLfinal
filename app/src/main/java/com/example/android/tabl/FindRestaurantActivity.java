package com.example.android.tabl;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.android.tabl.utils.FirebaseUtils;
import com.example.android.tabl.utils.RecyclerItemClickListener;
import com.example.android.tabl.restaurant_recyclerview.Restaurant;
import com.example.android.tabl.restaurant_recyclerview.RestaurantsAdapter;
import com.example.android.tabl.utils.TablUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * First main screen of TABL. Allows user to select the restaurant they intend to order from using
 * either a map or search function.
 *
 * @WRFitch
 */

/**
 * TODO: implement additional search method in appbar.
 * TODO: implement passing restaurant data to MenuActivity
 * TODO: implement swiping restaurantList up & down
 * TODO: Implement restaurant get radius
 * TODO: check getRestaurants() works with android's weird asynchronous stuff
 * TODO: how to refresh getRestaurants()?
 */

public class FindRestaurantActivity extends AppCompatActivity
        implements OnMapReadyCallback, LocationListener, SwipeRefreshLayout.OnRefreshListener {

    //activity stuff
    private List<Restaurant> restaurantList = new ArrayList<>();
    SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView recyclerView;
    private RestaurantsAdapter rAdapter;
    private FloatingActionButton fab;

    //map stuff
    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    //instantiating currentLocation keeps the app from crashing without a previous location
    private static Location currentLocation = new Location("dummylocation");
    private final static String KEY_LOCATION = "location";
    private final float DEFAULT_ZOOM = 16f;
    private boolean gotLocPerms = false;
    private boolean locDialogOpen = false;
    private double mapRadius = 100; //this is in DEGREES, NOT KM

    //firebase stuff
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    DocumentReference mRestaurantRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_restaurant);
        //set up map things
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        db = FirebaseFirestore.getInstance();

        if(!TablUtils.isNetworkAvailable(this))
            Toast.makeText(this, R.string.connection_failure, Toast.LENGTH_SHORT);
        if (savedInstanceState != null && savedInstanceState.keySet().contains(KEY_LOCATION)) {
            currentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        }

        mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                loadMap(map);
            }
        });

        //set up database things
        fab = findViewById(R.id.snapToLocationButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateCameraWithAnimation();
            }
        });

        recyclerView = findViewById(R.id.find_restaurant_recyView);
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, recyclerView,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                restaurantList.get(position).getMenuTitles();
                                //pass menuTitles to menuactivity
                                //preload favourites menu
                                callMenuActivity(getApplicationContext());
                            }
                            @Override
                            public void onLongItemClick(View view, int position) {
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
        testDB();
        updateRestaurantData();

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        showRestaurantsOnMap();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.find_restaurant_activity_menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.fra_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    //check if the user has location services on when returning to the application
    @Override
    @SuppressLint("MissingPermission")
    protected void onStart() {
        super.onStart();
        checkGPSTurnedOn();
        TablUtils.getLocationPerms(this, this);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onPause() {
        super.onPause();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                loadMap(map);
            }
        });
        super.onResume();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        loadMap(googleMap);
    }

    @SuppressLint("MissingPermission")
    protected void loadMap(GoogleMap googleMap) {
        //TablUtils.getLocationPerms(this, this);
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        updateLocation();
        currentLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(currentLocation!=null)
            updateCameraNoAnimation(currentLocation);

    }

    @SuppressLint("MissingPermission")
    public void updateLocation() {
        if(!gotLocPerms && !locDialogOpen) {
            TablUtils.getLocationPerms(this, this);
            checkGPSTurnedOn();
        }
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 100, this);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 20, this);
    }

    @SuppressLint("MissingPermission")
    public void getUserLocationNoAnimation() {
        updateLocation();
        currentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        currentLocation = mMap.getMyLocation();
        updateCameraNoAnimation(currentLocation);
    }

    public void updateCameraNoAnimation(Location currentLocation) {
        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM));
    }

    @SuppressLint("MissingPermission")
    public void updateCameraWithAnimation() {
        updateLocation();
        currentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        currentLocation = mMap.getMyLocation();
        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM));
    }

    public void onLocationChanged(Location location) {
        updateLocation();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        updateLocation();
    }

    public void onProviderEnabled(String provider) {
        //TablUtils.getLocationPerms(this, this);
        updateLocation();
        updateCameraNoAnimation(currentLocation);
    }

    public void onProviderDisabled(String provider) {
        TablUtils.getLocationPerms(this, this);
    }

    private void checkGPSTurnedOn(){
        if( !mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            !mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            locDialogOpen = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(FindRestaurantActivity.this);
            builder.setCancelable(false);
            builder.setMessage(R.string.gps_dialog_info_text)
                    .setTitle(R.string.gps_dialog_title);
            builder.setPositiveButton(R.string.turn_on_gps, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    requestTurnOnGPS();
                    getParent().recreate();
                    gotLocPerms = true;
                }
            });
            builder.setNegativeButton(R.string.use_search_not_gps, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    callSearchRestaurantActivity(getApplicationContext());
                    //callSearchRestaurantActivity(FindRestaurantActivity.this);
                }
            });
            builder.show();
            AlertDialog dialog = builder.create();
        }
    }

    private void requestTurnOnGPS(){
        Intent gpsOptionsIntent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(gpsOptionsIntent);
    }

    private void showRestaurantsOnMap() {
        for (Restaurant r : restaurantList) {
            //currently crashes app - "null object reference"
            //mMap.addMarker(new MarkerOptions().position(r.updateLocation()).title(r.getName()));
        }
    }

    private void updateRestaurantData() {
        restaurantList.clear();
        if(!TablUtils.isNetworkAvailable(this)) {
            TablUtils.errorMsg(fab, "No Internet Connection!");
            return;
        }
        getRestaurantsInRadius();
        showRestaurantsOnMap();
    }

    private void getRestaurantsInRadius(){
        db = FirebaseFirestore.getInstance();
        CollectionReference restaurantsRef = db.collection("Restaurants");
        Query query1 = restaurantsRef
                .whereLessThanOrEqualTo("Longitude", currentLocation.getLongitude()+mapRadius)
                .whereGreaterThanOrEqualTo("Longitude", currentLocation.getLongitude()-mapRadius);
        Query query2 = restaurantsRef
                .whereLessThanOrEqualTo("Latitude", currentLocation.getLatitude()+mapRadius)
                .whereGreaterThanOrEqualTo("Latitude", currentLocation.getLatitude()-mapRadius);

        // Create a reference to the cities collection
        CollectionReference citiesRef = db.collection("cities");
        // Create a query against the collection.
        Query query = citiesRef.whereEqualTo("state", "CA");
        query.get().addOnCompleteListener( new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        restaurantList.add(new Restaurant());
                        rAdapter.notifyDataSetChanged();
                    }
                }else{
                    TablUtils.errorMsg(fab, "Data not received from Firebase");
                }
            }
        });
    }

    //call next activity. Make sure to pass parcelable restaurant data.
    private void callMenuActivity(Context c) {
        Intent intent = new Intent(c, MenuActivity.class);
        startActivity(intent);
    }

    private void callSearchRestaurantActivity(Context c){
        Intent intent = new Intent(c, SearchRestaurantActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.fra_search:
                return true;

            default:
                //TablUtils.errorMsg(fab, "action not recognised!");
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onRefresh(){
        updateRestaurantData();
        //testDB();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void testDB() {
        CollectionReference cities = db.collection("cities");

        Map<String, Object> data1 = new HashMap<>();
        data1.put("name", "San Francisco");
        data1.put("state", "CA");
        data1.put("country", "USA");
        data1.put("capital", false);
        data1.put("population", 860000);
        data1.put("regions", Arrays.asList("west_coast", "norcal"));
        cities.document("SF").set(data1);

        Map<String, Object> data2 = new HashMap<>();
        data2.put("name", "Los Angeles");
        data2.put("state", "CA");
        data2.put("country", "USA");
        data2.put("capital", false);
        data2.put("population", 3900000);
        data2.put("regions", Arrays.asList("west_coast", "socal"));
        cities.document("LA").set(data2);

        Map<String, Object> data3 = new HashMap<>();
        data3.put("name", "Washington D.C.");
        data3.put("state", null);
        data3.put("country", "USA");
        data3.put("capital", true);
        data3.put("population", 680000);
        data3.put("regions", Arrays.asList("east_coast"));
        cities.document("DC").set(data3);

        Map<String, Object> data4 = new HashMap<>();
        data4.put("name", "Tokyo");
        data4.put("state", null);
        data4.put("country", "Japan");
        data4.put("capital", true);
        data4.put("population", 9000000);
        data4.put("regions", Arrays.asList("kanto", "honshu"));
        cities.document("TOK").set(data4);

        Map<String, Object> data5 = new HashMap<>();
        data5.put("name", "Beijing");
        data5.put("state", null);
        data5.put("country", "China");
        data5.put("capital", true);
        data5.put("population", 21500000);
        data5.put("regions", Arrays.asList("jingjinji", "hebei"));
        cities.document("BJ").set(data5);
        //Toast.makeText(fab.getContext(), "added objects!", Toast.LENGTH_LONG).show();
    }
}
