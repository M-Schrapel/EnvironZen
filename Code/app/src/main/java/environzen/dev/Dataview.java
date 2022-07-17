package environzen.dev;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.graphics.Color;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.widget.TextView;

/*import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;*/


import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class Dataview extends AppCompatActivity implements SensorEventListener{

    private TextView accX, accY, accZ, gyrX, gyrY,gyrZ,magX,magY,magZ,fAcc,fGyr,fMag;
    private Sensor accSensor,gyrSensor,magSensor;
    private SensorManager SM;
    //private Deque<Integer> accTS,gyrTS,magTS = new ArrayDeque<>(100);
    private Queue<Integer> accTS,gyrTS,magTS = new ArrayBlockingQueue<>(100);

    private int fAcnt,fGcnt,fMcnt=0;
    private long aTS,gTS,mTS=0;
    private int freqAcc,freqGyr,freqMag=0;
    private LineChart accChart,gyrChart,magChart,accChartSum,gyrChartSum,magChartSum;


    /*private LineGraphSeries<DataPoint> seriesAcc;
    private int lastX = 0;*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dataview);

        // Assign TextView
        accX = (TextView)findViewById(R.id.accX);
        accY = (TextView)findViewById(R.id.accY);
        accZ = (TextView)findViewById(R.id.accZ);

        gyrX = (TextView)findViewById(R.id.gyrX);
        gyrY = (TextView)findViewById(R.id.gyrY);
        gyrZ = (TextView)findViewById(R.id.gyrZ);

        magX = (TextView)findViewById(R.id.magX);
        magY = (TextView)findViewById(R.id.magY);
        magZ = (TextView)findViewById(R.id.magZ);

        fAcc = (TextView)findViewById(R.id.fAcc);
        fGyr = (TextView)findViewById(R.id.fGyr);
        fMag = (TextView)findViewById(R.id.fMag);

        SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList  = SM.getSensorList(Sensor.TYPE_ALL);

        accSensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyrSensor = SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magSensor = SM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Register sensor Listeners
        accSensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SM.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        gyrSensor = SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        SM.registerListener(this, gyrSensor, SensorManager.SENSOR_DELAY_FASTEST);
        magSensor = SM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        SM.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_FASTEST);


        accChart = (LineChart) findViewById(R.id.chartAcc);
        // enable description text
        accChart.getDescription().setEnabled(true);
        // enable touch gestures
        accChart.setTouchEnabled(true);
        // enable scaling and dragging
        accChart.setDragEnabled(true);
        accChart.setScaleEnabled(true);
        accChart.setDrawGridBackground(false);
        // if disabled, scaling can be done on x- and y-axis separately
        accChart.setPinchZoom(true);
        // set an alternative background color
        accChart.setBackgroundColor(Color.WHITE);
        accChart.getXAxis().setEnabled(false);
        ////////////////
        gyrChart = (LineChart) findViewById(R.id.chartGyr);
        gyrChart.getDescription().setEnabled(true);
        gyrChart.setTouchEnabled(true);
        gyrChart.setDragEnabled(true);
        gyrChart.setScaleEnabled(true);
        gyrChart.setDrawGridBackground(false);
        gyrChart.setPinchZoom(true);
        gyrChart.setBackgroundColor(Color.WHITE);
        gyrChart.getXAxis().setEnabled(false);
        ////////////////
        magChart = (LineChart) findViewById(R.id.chartMag);
        magChart.getDescription().setEnabled(true);
        magChart.setTouchEnabled(true);
        magChart.setDragEnabled(true);
        magChart.setScaleEnabled(true);
        magChart.setDrawGridBackground(false);
        magChart.setPinchZoom(true);
        magChart.setBackgroundColor(Color.WHITE);
        magChart.getXAxis().setEnabled(false);
        ////////////////
        accChartSum = (LineChart) findViewById(R.id.chartAccSum);
        accChartSum.getDescription().setEnabled(true);
        accChartSum.setTouchEnabled(true);
        accChartSum.setDragEnabled(true);
        accChartSum.setScaleEnabled(true);
        accChartSum.setDrawGridBackground(false);
        accChartSum.setPinchZoom(true);
        accChartSum.setBackgroundColor(Color.WHITE);
        accChartSum.getXAxis().setEnabled(false);
        ////////////////
        gyrChartSum = (LineChart) findViewById(R.id.chartGyrSum);
        gyrChartSum.getDescription().setEnabled(true);
        gyrChartSum.setTouchEnabled(true);
        gyrChartSum.setDragEnabled(true);
        gyrChartSum.setScaleEnabled(true);
        gyrChartSum.setDrawGridBackground(false);
        gyrChartSum.setPinchZoom(true);
        gyrChartSum.setBackgroundColor(Color.WHITE);
        gyrChartSum.getXAxis().setEnabled(false);
        ////////////////
        magChartSum = (LineChart) findViewById(R.id.chartMagSum);
        magChartSum.getDescription().setEnabled(true);
        magChartSum.setTouchEnabled(true);
        magChartSum.setDragEnabled(true);
        magChartSum.setScaleEnabled(true);
        magChartSum.setDrawGridBackground(false);
        magChartSum.setPinchZoom(true);
        magChartSum.setBackgroundColor(Color.WHITE);
        magChartSum.getXAxis().setEnabled(false);


        LineData data0 = new LineData();
        data0.setValueTextColor(Color.WHITE);
        LineData data1 = new LineData();
        data1.setValueTextColor(Color.WHITE);

        // add empty data
        accChart.setData(data0);
        gyrChart.setData(data0);
        magChart.setData(data0);
        //accChart.


        accChartSum.setData(data1);
        gyrChartSum.setData(data1);
        magChartSum.setData(data1);
    }

    private LineDataSet createSumSet(int colorline) {

        LineDataSet set = new LineDataSet(null, "Summarized");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(colorline);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }


    private void addEntry(SensorEvent event) {
        LineChart chart,chartSum;
        LineData data;
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                chart=accChart;
                chartSum=accChartSum;
                break;
            case Sensor.TYPE_GYROSCOPE:
                chart=gyrChart;
                chartSum=gyrChartSum;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                chart=magChart;
                chartSum=magChartSum;
                break;
            default:
                return;
        }
        chartUpdate(chart, chartSum, event);
    }

    private void chartUpdate(LineChart chart, LineChart chartSum, SensorEvent event) {
        LineData data,dataSum;
        data=chart.getData();
        dataSum=chartSum.getData();
        int freq=0;
        String str;
        if (data != null && dataSum != null) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    str= fAcc.getText().toString();
                    freq=freqAcc;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    freq=freqGyr;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    freq=freqMag;
                    break;
                default:
                    return;
            }
            if (freq==0){
                freq=1;
            }
            ILineDataSet set = data.getDataSetByIndex(0);
            ILineDataSet set1 = data.getDataSetByIndex(1);
            ILineDataSet set2 = data.getDataSetByIndex(2);
            ILineDataSet setSum = dataSum.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well



            if(set == null){
                set = createSet();
                set1 = createSet1();
                set2 = createSet2();
                data.addDataSet(set);
                data.addDataSet(set1);
                data.addDataSet(set2);
            }

            if (setSum == null) {
                setSum = createSumSet(Color.MAGENTA);
                dataSum.addDataSet(setSum);
            }
//            data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 80) + 10f), 0);
//            data.addEntry(new Entry(set.getEntryCount(), event.values[0]), 0);
//            data.notifyDataChanged();

            data.addEntry(new Entry(set.getEntryCount(),event.values[0] + 5),0);
            data.addEntry(new Entry(set1.getEntryCount(),event.values[1] + 5),1);
            data.addEntry(new Entry(set2.getEntryCount(),event.values[2] + 5),2);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();
            // limit the number of visible entries
            chart.setVisibleXRangeMaximum(freq*100);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chart.moveViewToX(data.getEntryCount());

            dataSum.addEntry(new Entry(setSum.getEntryCount(), event.values[0] + event.values[1]+event.values[2]), 0);
            dataSum.notifyDataChanged();
            chartSum.notifyDataSetChanged();
            chartSum.setVisibleXRangeMaximum(freq*100);
            chartSum.moveViewToX(dataSum.getEntryCount());
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        LineChart chart,chartSum;
        LineData data;
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                chart=accChart;
                chartSum=accChartSum;
                accX.setText(String.format("%.2f",event.values[0]));
                accY.setText(String.format("%.2f",event.values[1]));
                accZ.setText(String.format("%.2f",event.values[2]));
               // accTS.add((int) 0);
                break;
            case Sensor.TYPE_GYROSCOPE:
                chart=gyrChart;
                chartSum=gyrChartSum;
                gyrX.setText(String.format("%.2f",event.values[0]));
                gyrY.setText(String.format("%.2f",event.values[1]));
                gyrZ.setText(String.format("%.2f",event.values[2]));
                //gyrTS.add(Math.toIntExact(event.timestamp));
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                chart=magChart;
                chartSum=magChartSum;
                magX.setText(String.format("%.2f",event.values[0]));
                magY.setText(String.format("%.2f",event.values[1]));
                magZ.setText(String.format("%.2f",event.values[2]));
                //magTS.add(Math.toIntExact(event.timestamp));
                break;
            default:
                return;
        }
        chartUpdate(chart, chartSum, event);
        setFrequency(event);
        /*
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                accX.setText( String.valueOf(event.values[0]));
                accY.setText(String.valueOf(event.values[1]));
                accZ.setText(String.valueOf(event.values[2]));
                //seriesAcc.appendData(new DataPoint(lastX++, event.values[0]), true, 10);
                addEntry(event.values[1]);
                addEntry(event);
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyrX.setText( String.valueOf(event.values[0]));
                gyrY.setText(String.valueOf(event.values[1]));
                gyrZ.setText(String.valueOf(event.values[2]));
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magX.setText( String.valueOf(event.values[0]));
                magY.setText(String.valueOf(event.values[1]));
                magZ.setText(String.valueOf(event.values[2]));
                break;
            default:
                accX.setText(  event.sensor.getType());
                break;
        }
        /*accX.setText( String.valueOf(event.values[0]));
        accY.setText(String.valueOf(event.values[1]));
        accZ.setText(String.valueOf(event.values[2]));
        magX.setText( String.valueOf(event.values[3]));
        magY.setText(String.valueOf(event.values[4]));
        magZ.setText(String.valueOf(event.values[5]));*/
    }

    private void setFrequency(SensorEvent event){
        long diff,tmp=event.timestamp/1000000L;
        diff = tmp;
        double freq=0;
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                if ((fAcnt++%100)==0){
                    diff -= aTS;
                    aTS=tmp;
                    freq= 1/((double)diff /10000.0);
                    fAcc.setText( String.format("%.1f Hz",freq));
                    freqAcc= (int) freq;
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                if ((fGcnt++%100)==0){
                    diff -= gTS;
                    gTS=tmp;
                    freq= 1/((double)diff /10000.0);
                    fGyr.setText( String.format("%.1f Hz",freq));
                    freqGyr= (int) freq;
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                if ((fMcnt++%100)==0){
                    diff -= mTS;
                    mTS=tmp;
                    freq= 1/((double)diff /10000.0);
                    fMag.setText( String.format("%.1f Hz",freq));
                    freqMag= (int) freq;
                }
                break;
            default:
                return;
        }
    }

    private LineDataSet createSet(){
        LineDataSet set = new LineDataSet(null,"X Axis");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(1f);
        set.setColor(Color.MAGENTA);
        //set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        //set.setCubicIntensity(0.2f);
        return  set;
    }

    private LineDataSet createSet1(){
        LineDataSet set = new LineDataSet(null,"Y Axis");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.WHITE);
        //set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        //set.setCubicIntensity(0.2f);
        return  set;
    }

    private LineDataSet createSet2(){
        LineDataSet set = new LineDataSet(null,"Z Axis");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.RED);
        //set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        //set.setCubicIntensity(0.2f);
        return  set;
    }







    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        SM.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        accSensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SM.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        gyrSensor = SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        SM.registerListener(this, gyrSensor, SensorManager.SENSOR_DELAY_FASTEST);
        magSensor = SM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        SM.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onDestroy() {
        SM.unregisterListener(this);
        //thread.interrupt();
        super.onDestroy();
    }
}