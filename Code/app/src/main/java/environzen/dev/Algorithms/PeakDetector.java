package environzen.dev.Algorithms;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Observable;

import environzen.dev.Algorithms.Filter.RollingAverage;
import environzen.dev.StepDetector;

public class PeakDetector extends Observable implements StepDetector, SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accSensor;

    RollingAverage rollingAverage, averageWindow, intensityAverage, timeAverage;
    double freq;
    long firstTime;
    int fCnt = 0;
    boolean frequencySet = false;
    boolean nextStep = true;
    long lastStepTime;
    boolean firstStepTaken = false;
    double timeSinceLastStep = 0.6;


    double filterSize = 0.3;
    double minTimeBetween = 0.5;

    //Parameters
    double thresholdWalking = 0.71481386;
    double thresholdStep = 1.01742984;
    double averageWindowSize = 0.62887104;
    double intensityWindowSize = 1.36496344;
    double joggingIntensity = 3.89286316;


    public PeakDetector(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void start() {
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void stop() {
        sensorManager.unregisterListener(this);
        fCnt = 0;
        nextStep = true;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (!frequencySet) {
            if(fCnt == 0) {
                firstTime = sensorEvent.timestamp;
            } else if (fCnt == 100) {
                long diff = sensorEvent.timestamp - firstTime;
                freq = 1 / ((double) diff / 100000000000L);
                rollingAverage = new RollingAverage((int) (freq * filterSize));
                averageWindow = new RollingAverage((int) (freq * averageWindowSize));
                intensityAverage = new RollingAverage((int) (freq * intensityWindowSize));
                frequencySet = true;
                fCnt = 0;
            }

        } else {
            double magnitude = Math.sqrt(Math.pow(sensorEvent.values[0], 2) + Math.pow(sensorEvent.values[1], 2) + Math.pow(sensorEvent.values[2], 2)) - 9.8;
            rollingAverage.add(magnitude);
            intensityAverage.add(rollingAverage.getAverage());

            if(firstStepTaken) {
                timeSinceLastStep = (double)(sensorEvent.timestamp - lastStepTime)/1000000000L;
            }

            if(fCnt >= freq * filterSize && fCnt >= freq * averageWindowSize) {
                if(timeSinceLastStep > minTimeBetween) {
                    nextStep = true;
                }

                if(intensityAverage.getAverage() > joggingIntensity) {
                    minTimeBetween = 0.33;
                } else {
                    minTimeBetween = 0.5;
                }

                if (rollingAverage.getAverage() > averageWindow.getAverage() * thresholdStep && nextStep && rollingAverage.getAverage() > thresholdWalking) {
                    setChanged();
                    notifyObservers((float)timeSinceLastStep);
                    lastStepTime = sensorEvent.timestamp;
                    nextStep = false;
                    firstStepTaken = true;

                }
                if(timeSinceLastStep > 2.5) {
                    firstStepTaken = false;
                    timeSinceLastStep = 0.6;

                    //Notify that user stopped walking
                    setChanged();
                    notifyObservers(0f);
                }
            }
            averageWindow.add(rollingAverage.getAverage());
        }
        if(!frequencySet || fCnt < freq * filterSize || fCnt < freq * averageWindowSize) {
            fCnt++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
