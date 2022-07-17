package environzen.dev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback {

    static List<LatLng> coordinates, lastSegment;
    static boolean navigating = false;
    static double distance = Double.MAX_VALUE;
    EditText addressView;
    ImageButton findRoute;
    Button startNavigation;

    static Location nextStop;
    static Location currentLocation;
    static private GoogleMap map;
    private static final float DEFAULT_ZOOM = 15;
    static boolean currentLocationSet;
    static boolean mapInitialized;
    static ArrayList<Polyline> path;
    static Marker destinationMarker;
    static float tolerance;
    static boolean activityVisible;
    static long lastTurnTime;
    static long timeOnWrongPath;
    static long lastDirectionSoundTime;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    LocationRequest locationRequest;

    TextView log1, log2;
    Polyline las1;
    Marker las2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        addressView = findViewById(R.id.address);
        findRoute = findViewById(R.id.find_route);
        findRoute.setEnabled(false);
        startNavigation = findViewById(R.id.start_navigation);
        mapInitialized = false;
        currentLocationSet = false;
        timeOnWrongPath = 0;

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if(navigating) {
            startNavigation.setText("Stop Navigation");
            startNavigation.setEnabled(true);
            findRoute.setEnabled(false);
            addressView.setEnabled(false);
        }

        log1 = findViewById(R.id.log1);
        log2 = findViewById(R.id.log2);
    }

    public void onTaskCompleted(ArrayList<String> c) {
        if(path != null) {
            for(Polyline p : path) {
                p.remove();
            }
        }
        path = new ArrayList<>();
        if(destinationMarker != null) {
            destinationMarker.remove();
        }
        coordinates = new ArrayList<>();
        for (String coordinate : c) {
            coordinate = coordinate.replace("[", "");
            coordinate = coordinate.replace("]", "");
            String[] latlng = coordinate.split(",");
            coordinates.add(new LatLng(Double.parseDouble(latlng[1]), Double.parseDouble(latlng[0])));
        }

        for(int i = 0; i < coordinates.size() - 1; i++) {
            PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
            options.add(coordinates.get(i));
            options.add(coordinates.get(i + 1));
            path.add(map.addPolyline(options));
        }
        LatLng destination = coordinates.get(coordinates.size() - 1);
        destinationMarker = map.addMarker(new MarkerOptions()
                .position(new LatLng(destination.latitude, destination.longitude)));
        startNavigation.setEnabled(true);
    }

    public void onLocationChanged(Location location) {
        currentLocation = location;
        tolerance = 5 + location.getAccuracy();

        if(!currentLocationSet && !navigating && activityVisible) {
            findRoute.setEnabled(true);
            currentLocationSet = true;
        }
        if(!mapInitialized) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        }
        if (navigating) {
            double newDistance = location.distanceTo(nextStop);
            if(newDistance <= distance && PolyUtil.isLocationOnPath(new LatLng(location.getLatitude(), location.getLongitude()), path.get(0).getPoints(), false, tolerance * 2)) {
                MainActivity.soundscape.wrongPath(false);
                timeOnWrongPath = 0;
                lastDirectionSoundTime = 0;
            } else {
                if(timeOnWrongPath == 0) {
                    timeOnWrongPath = System.currentTimeMillis();
                }
                if((System.currentTimeMillis() - timeOnWrongPath) / 1000 >= 3) {
                    MainActivity.soundscape.wrongPath(true);
                    if((System.currentTimeMillis() - timeOnWrongPath) / 1000 >= 5 && lastDirectionSoundTime == 0) {
                        playDirectionSound(true);
                    } else if((System.currentTimeMillis() - lastDirectionSoundTime) / 1000 >= 10) {
                        playDirectionSound(true);
                        lastDirectionSoundTime = System.currentTimeMillis();
                    }
                }
            }
            if(path.size() <= 1) {
                if(location.distanceTo(nextStop) < tolerance) {
                    path.get(0).remove();
                    path.remove(0);
                    navigating = false;
                    startNavigation.setText("Start Navigation");
                    startNavigation.setEnabled(false);
                    addressView.setEnabled(true);
                    findRoute.setEnabled(true);
                    timeOnWrongPath = 0;
                    lastDirectionSoundTime = 0;
                    lastSegment = null;
                    MainActivity.soundscape.wrongPath(false);
                    MainActivity.soundscape.playArrivalSound();
                    if(!activityVisible) {
                        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                        fusedLocationProviderClient = null;
                    }
                }
            } else {
                boolean nextSegment = false;
                boolean lastSegmentSet = false;
                for(int i = 1; i < path.size(); i++) {
                    if(PolyUtil.isLocationOnPath(new LatLng(location.getLatitude(), location.getLongitude()), path.get(i).getPoints(), false, tolerance)) {
                        Location l1 = new Location("");
                        Location l2 = new Location("");
                        l1.setLatitude(path.get(i).getPoints().get(0).latitude);
                        l1.setLongitude(path.get(i).getPoints().get(0).longitude);
                        l2.setLatitude(path.get(i).getPoints().get(1).latitude);
                        l2.setLongitude(path.get(i).getPoints().get(1).longitude);
                        float segmentDistance = l1.distanceTo(l2);

                        if(l1.distanceTo(l2) > 10) {
                            nextSegment = true;
                        } else {
                            /*
                            List<LatLng> latlng = new ArrayList<>();
                            latlng.add(path.get(0).getPoints().get(0));
                            latlng.add(path.get(i).getPoints().get(1));
                            path.get(i-1).setPoints(latlng);
                             */
                            if(path.size()-1 > i) {
                                List<LatLng> latlng = new ArrayList<>();
                                latlng.add(path.get(i).getPoints().get(0));
                                latlng.add(path.get(i+1).getPoints().get(1));
                                path.get(i+1).setPoints(latlng);
                            }
                        }


                        nextStop.setLatitude(path.get(i).getPoints().get(1).latitude);
                        nextStop.setLongitude(path.get(i).getPoints().get(1).longitude);
                        distance = location.distanceTo(nextStop);
                        MainActivity.soundscape.wrongPath(false);
                        log1.setText("Segment length: " + segmentDistance);
                        Log.d("distance", String.valueOf(segmentDistance));
                        if(i == 1 && !lastSegmentSet) {
                            lastSegment = path.get(0).getPoints();
                            lastSegmentSet = true;
                        }
                        for(int j = 0; j < i; j++) {
                            path.get(j).remove();
                            path.remove(j);
                            i--;
                        }
                    }
                }
                if(nextSegment) {
                    /*
                    if((System.currentTimeMillis() - lastTurnTime) / 1000 > 10) {
                        playDirectionSound(false);
                    } else {
                        playDirectionSound(true);
                    }
                    */
                    /*
                    if(las1 != null) las1.remove();
                    PolylineOptions options = new PolylineOptions().width(6).color(Color.GREEN).geodesic(true);
                    options.add(lastSegment.get(0));
                    options.add(lastSegment.get(1));
                    las1 = map.addPolyline(options);

                    if(las2 != null) las2.remove();
                    las2 = destinationMarker = map.addMarker(new MarkerOptions()
                            .position(new LatLng(path.get(0).getPoints().get(1).latitude, path.get(0).getPoints().get(1).longitude)));
                    */
                    playDirectionSound(false);
                }
            }
            distance = newDistance;
        }
    }

    // if ownBearing is true uses the users direction of travel, else uses angle between previous and current segment of the path
    private void playDirectionSound(boolean ownBearing) {
        float bearing1;
        float bearing2;
        float direction;
        if(lastSegment == null) {
            ownBearing = true;
        }

        if(ownBearing) {
            bearing1 = currentLocation.getBearing();
            if(bearing1 == 0) {
                return;
            }
            bearing2 = normalizeDegree(currentLocation.bearingTo(nextStop));
        } else {
            Location l1 = new Location("");
            Location l2 = new Location("");
            Location l3 = new Location("");

            l1.setLatitude(lastSegment.get(0).latitude);
            l1.setLongitude(lastSegment.get(0).longitude);
            l2.setLatitude(lastSegment.get(1).latitude);
            l2.setLongitude(lastSegment.get(1).longitude);
            l3.setLatitude(path.get(0).getPoints().get(1).latitude);
            l3.setLongitude(path.get(0).getPoints().get(1).longitude);

            bearing1 = normalizeDegree(l1.bearingTo(l2));
            bearing2 = normalizeDegree(l2.bearingTo(l3));
        }

        direction = normalizeDegree(bearing2 - bearing1);
        int d = 0;
        if(direction < 337.5 && direction > 22.5) {
            d = (int) ((direction + 22.5) / 45);
        }
        if(!ownBearing && (d == 2 || d == 6)) {
            MainActivity.soundscape.playDirectionSound(d);
            lastTurnTime = System.currentTimeMillis();
        } else if(ownBearing && d != 0) {
            MainActivity.soundscape.playDirectionSound(d);
        }
        log2.setText("Direction: " + d);
        Log.d("direction", String.valueOf(direction) + " " + String.valueOf(d));
    }

    private float normalizeDegree(float value){
        if(value < 0){
            return value + 360;
        }else{
            return value;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                } else {
                    finish();
                }
            }
        }
    }

    public void findRoute(View view) {
        boolean addressFound = true;
        String addressString = addressView.getText().toString();
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses = new ArrayList<>();
        Address address = null;
        try {
            addresses = geocoder.getFromLocationName(addressString, 1);
        } catch (IOException e) {
            addressFound = false;
        }
        if (addresses == null || addresses.size() == 0) {
            addressFound = false;
        } else {
            address = addresses.get(0);
        }

        if (!addressFound) {
            Toast toast = Toast.makeText(this, "The address was not found", Toast.LENGTH_LONG);
            toast.show();
        } else {
            String str = "https://api.openrouteservice.org/v2/directions/foot-walking?api_key=" + getResources().getString(R.string.openroute_api_key) + "&start=" + currentLocation.getLongitude() + "," + currentLocation.getLatitude() + "&end=" + address.getLongitude() + "," + address.getLatitude();
            new NavigationTask(this).execute(str);
        }
    }

    public void startNavigation(View view) {
        if(!navigating) {
            nextStop = new Location("");
            nextStop.setLatitude(path.get(0).getPoints().get(1).latitude);
            nextStop.setLongitude(path.get(0).getPoints().get(1).longitude);
            startNavigation.setText("Stop Navigation");
            findRoute.setEnabled(false);
            addressView.setEnabled(false);
        } else {
            startNavigation.setText("Start Navigation");
            findRoute.setEnabled(true);
            addressView.setEnabled(true);
            timeOnWrongPath = 0;
            lastDirectionSoundTime = 0;
            lastSegment = null;
            MainActivity.soundscape.wrongPath(false);
        }
        navigating = !navigating;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            map.setMyLocationEnabled(true);
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM));
        if(navigating) {
            for(int i = 0; i < path.size(); i++) {
                PolylineOptions options = new PolylineOptions().width(6).color(Color.BLUE).geodesic(true);
                options.add(path.get(i).getPoints().get(0));
                options.add(path.get(i).getPoints().get(1));
                path.set(i, map.addPolyline(options));
            }
            LatLng destination = coordinates.get(coordinates.size() - 1);
            destinationMarker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(destination.latitude, destination.longitude)));
        }
        mapInitialized = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityVisible = true;
        if(fusedLocationProviderClient == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    onLocationChanged(locationResult.getLastLocation());
                }
            };
            locationRequest = new LocationRequest();
            locationRequest.setInterval(500)
                    .setFastestInterval(0)
                    .setMaxWaitTime(0)
                    .setSmallestDisplacement(1)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        0);
            } else {
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!navigating) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            fusedLocationProviderClient = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}