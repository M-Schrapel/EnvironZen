package environzen.dev;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static android.content.Context.SENSOR_SERVICE;

public class SensorLogger implements SensorEventListener, LocationListener {

    Context context;
    private Sensor accSensor;
    private SensorManager SM;
    boolean isRunning;
    FileWriter writer;
    LocationManager locationManager;
    long startTime;
    IntentFilter messageFilter;
    Receiver messageReceiver;
    String task;
    int condition;
    int participant;

    public SensorLogger(Context context) {
        this.context = context;

        isRunning = false;

        SM = (SensorManager) context.getSystemService(SENSOR_SERVICE);

        accSensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);

        messageFilter = new IntentFilter(Intent.ACTION_SEND);
        messageReceiver = new Receiver();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(isRunning) {
            try {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    writer.write(String.format("%d;%s;%d;%s;ACC;%f;%f;%f\n", participant, task, SystemClock.elapsedRealtimeNanos() - startTime, condition, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        try {
            writer.write(String.format("%d;%s;%d;%s;GPS;%f;%f;%f\n", participant, task, SystemClock.elapsedRealtimeNanos() - startTime, condition, location.getLatitude(), location.getLongitude(), location.getSpeed()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logStepTime(float time) {
        try {
            writer.write(String.format("%d;%s;%d;%s;STEP;%f;%f;%f\n", participant, task, SystemClock.elapsedRealtimeNanos() - startTime, condition, time, 0f, 0f));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logWarning() {
        if(isRunning) {
            try {
                writer.write(String.format("%d;%s;%d;%s;WARN;%f;%f;%f\n", participant, task, SystemClock.elapsedRealtimeNanos() - startTime, condition, 0f, 0f, 0f));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logVolumeLevels() {
        float ambienceVolume = MainActivity.soundscape.getAmbienceSliderVolume();
        float stepVolume = MainActivity.soundscape.getStepSliderVolume();
        try {
            writer.write(String.format("%d;%s;%d;%s;VOLUME;%f;%f;%f\n", participant, task, SystemClock.elapsedRealtimeNanos() - startTime, condition, ambienceVolume, stepVolume, 0f));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            float heartRate = Float.parseFloat(intent.getExtras().getString("message"));
            try {
                writer.write(String.format("%d;%s;%d;%s;HEART;%f;%f;%f\n", participant, task, SystemClock.elapsedRealtimeNanos() - startTime, condition, heartRate, 0f, 0f));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getStorageDir() {
        return context.getExternalFilesDir(null).getAbsolutePath();
    }

    public void startLogging() {
        Log.d("SensorLog", "Writing to " + getStorageDir());
        String fileName = "Participant" + participant + "_Task" + task + "_" + System.currentTimeMillis() + ".csv";
        try {
            writer = new FileWriter(new File(getStorageDir(), fileName));
            writer.write("Participant;Task;Time;Condition;Sensor;x;y;z\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        startTime = SystemClock.elapsedRealtimeNanos();

        if (!(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 2000, 5, this);
        }

        SM.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);

        LocalBroadcastManager.getInstance(context).registerReceiver(messageReceiver, messageFilter);

        isRunning = true;

        logVolumeLevels();
    }

    public void stopLogging() {
        locationManager.removeUpdates(this);
        SM.flush(this);
        SM.unregisterListener(this);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageReceiver);
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isRunning = false;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public void setCondition(int condition) {
        this.condition = condition;
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
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void setParticipant(int participant) {
        this.participant = participant;
    }
}
