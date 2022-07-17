package environzen.dev.Algorithms;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Observable;

import environzen.dev.StepDetector;

public class NativeStepDetector extends Observable implements StepDetector, SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;

    public NativeStepDetector(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        setChanged();
        notifyObservers();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void start() {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void stop() {
        sensorManager.unregisterListener(this);
    }
}
