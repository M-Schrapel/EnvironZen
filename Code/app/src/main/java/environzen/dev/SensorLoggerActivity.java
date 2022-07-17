package environzen.dev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class SensorLoggerActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private Sensor accSensor, gyrSensor, proSensor, ligSensor;
    private SensorManager SM;
    Button buttonStart;
    Button buttonStop;
    boolean isRunning;
    FileWriter writer;
    LocationManager locationManager;
    long startTime;
    boolean isGPSEnabled = false;
    EditText fileNameField;
    String fileName;
    ProgressBar progressBar;
    ArrayList<File> files;
    ListView listView;
    CustomAdapter adapter;
    File directory;
    IntentFilter messageFilter;
    Receiver messageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_logger);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        isRunning = false;

        fileNameField = findViewById(R.id.file_name);

        buttonStart = findViewById(R.id.button_start);
        buttonStop = findViewById(R.id.button_stop);
        buttonStart.setEnabled(true);
        buttonStop.setEnabled(false);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        SM = (SensorManager) getSystemService(SENSOR_SERVICE);

        accSensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyrSensor = SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        proSensor = SM.getDefaultSensor(Sensor.TYPE_LIGHT);
        ligSensor = SM.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }

        messageFilter = new IntentFilter(Intent.ACTION_SEND);
        messageReceiver = new Receiver();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        directory = new File(getStorageDir());
        files = new ArrayList<>(Arrays.asList(directory.listFiles()));

        listView = findViewById(R.id.files);
        adapter = new CustomAdapter(this, files);
        listView.setAdapter(adapter);

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
                    isGPSEnabled = locationManager
                            .isProviderEnabled(LocationManager.GPS_PROVIDER);
                }
            }
        }
    }

    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
    }

    public void onStart(View view) {
        Log.d("SensorLog", "Writing to " + getStorageDir());
        fileName = "SensorLog_" + fileNameField.getText().toString() + "_" + System.currentTimeMillis() + ".csv";
        try {
            writer = new FileWriter(new File(getStorageDir(), fileName));
            writer.write("Time;Sensor;x;y;z\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        startTime = SystemClock.elapsedRealtimeNanos();

        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 5000, 10, this);
        }

        SM.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        SM.registerListener(this, gyrSensor, SensorManager.SENSOR_DELAY_FASTEST);
        SM.registerListener(this, proSensor, SensorManager.SENSOR_DELAY_NORMAL);
        SM.registerListener(this, ligSensor, SensorManager.SENSOR_DELAY_NORMAL);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        isRunning = true;
        buttonStart.setEnabled(false);
        buttonStop.setEnabled(true);
        fileNameField.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        listView.setEnabled(false);
        adapter.notifyDataSetChanged();
    }

    public void onStop(View view) {
        buttonStart.setEnabled(true);
        buttonStop.setEnabled(false);
        fileNameField.setEnabled(true);
        progressBar.setVisibility(View.INVISIBLE);
        isRunning = false;
        stopLogging();

        File file = new File(getStorageDir() + "/" + fileName);
        shareFile(file);

        files.add(file);
        adapter.notifyDataSetChanged();
    }

    private void shareFile(File file) {
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        if(file.exists()) {
            intentShareFile.setType("application/pdf");
            intentShareFile.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file));
            startActivity(Intent.createChooser(intentShareFile, "Share File"));
        }
    }

    private void stopLogging() {
        locationManager.removeUpdates(this);
        SM.flush(this);
        SM.unregisterListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(isRunning) {
            try {
                switch(sensorEvent.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        writer.write(String.format("%d;ACC;%f;%f;%f\n", SystemClock.elapsedRealtimeNanos() - startTime, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        writer.write(String.format("%d;GYRO;%f;%f;%f\n", SystemClock.elapsedRealtimeNanos() - startTime, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
                        break;
                    case Sensor.TYPE_PROXIMITY:
                        writer.write(String.format("%d;PRO;%f;%f;%f\n", SystemClock.elapsedRealtimeNanos() - startTime, sensorEvent.values[0], 0f, 0f));
                        break;
                    case Sensor.TYPE_LIGHT:
                        writer.write(String.format("%d;LIG;%f;%f;%f\n", SystemClock.elapsedRealtimeNanos() - startTime, sensorEvent.values[0], 0f, 0f));
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        try {
            writer.write(String.format("%d;GPS;%f;%f;%f\n", SystemClock.elapsedRealtimeNanos() - startTime, location.getLatitude(), location.getLongitude(), location.getSpeed()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if(isRunning) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);        builder.setTitle("Stop Sensor Logger");
            builder.setMessage("You are currently logging sensor data. By going back the logger will be stopped. The file will still be stored on your device.");
            builder.setPositiveButton("Go Back", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    stopLogging();
                    finish();
                }
            });
            builder.setNegativeButton("Keep logging", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });
            builder.show();
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class CustomAdapter extends BaseAdapter {
        Context context;
        ArrayList<File> files;
        LayoutInflater inflater;


        public CustomAdapter(Context applicationContext, ArrayList<File> files) {
            this.context = applicationContext;
            this.files = files;
            inflater = (LayoutInflater.from(applicationContext));
        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public Object getItem(int i) {
            return files.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            File file = files.get(i);
            view = inflater.inflate(R.layout.file_list_item, null);

            TextView title = view.findViewById(R.id.title);
            title.setText(file.getName());
            TextView date = view.findViewById(R.id.date);
            SimpleDateFormat simpleDate =  new SimpleDateFormat("dd.MM.yyyy HH:mm");
            date.setText(simpleDate.format(new Date(file.lastModified())));

            ImageView share = view.findViewById(R.id.share);
            share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareFile(file);
                }
            });

            ImageView delete = view.findViewById(R.id.delete);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);        builder.setTitle("Delete Log File");
                    builder.setMessage("Are you sure you want to delete this file?");
                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            files.remove(file);
                            boolean deleted = file.delete();
                            notifyDataSetChanged();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    builder.show();
                }
            });

            if(isRunning) {
                share.setEnabled(false);
                delete.setEnabled(false);
            } else {
                share.setEnabled(true);
                delete.setEnabled(true);
            }
            return view;
        }
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            float heartRate = Float.parseFloat(intent.getExtras().getString("message"));
            String message = "Heart rate: " + heartRate;
            try {
                writer.write(String.format("%d;HEA;%f;%f;%f\n", SystemClock.elapsedRealtimeNanos() - startTime, heartRate, 0f, 0f));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}