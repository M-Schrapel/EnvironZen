package environzen.dev.Algorithms;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Observable;


public class PocketDetector extends Observable implements SensorEventListener {

    private SensorManager sensorManager;
    Sensor proximitySensor, lightSensor;
    float rp = -1;
    float rl = -1;
    boolean inPocket = false;

    public PocketDetector(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        //lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        /*
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            rp = sensorEvent.values[0];
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
            rl = sensorEvent.values[0];
        }
        if ((rp != -1) && (rl != -1)) {
            inPocket = (rp < 1) && (rl < 40);
            setChanged();
            notifyObservers(inPocket);
        }
        */
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            rp = sensorEvent.values[0];
        }
        if (rp != -1) {
            inPocket = rp <= 1;
            setChanged();
            notifyObservers(inPocket);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void start() {
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        //sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }
}
