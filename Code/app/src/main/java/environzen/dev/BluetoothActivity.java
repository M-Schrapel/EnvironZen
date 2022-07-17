package environzen.dev;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class BluetoothActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    BluetoothAdapter bluetoothAdapter;

    TextView textInfo, textStatus;
    private UUID myUUID;

    Button btnReceiver, btnDisconnect;

    static ThreadConnected myThreadConnected;
    ThreadBeConnected myThreadBeConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        textInfo = (TextView)findViewById(R.id.info);
        textStatus = (TextView)findViewById(R.id.status);


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this,
                    "FEATURE_BLUETOOTH not support",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //has to match the UUID on the another device of the BT connection
        myUUID = UUID.fromString("74c0c1d0-ad84-11eb-9d0d-0800200c9a66");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not supported on this device",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String stInfo = bluetoothAdapter.getName();
        textInfo.setText(stInfo);

        btnReceiver = (Button)findViewById(R.id.setup2);
        btnReceiver.setOnClickListener(v -> setup());

        btnDisconnect = (Button)findViewById(R.id.disconnect);
        btnDisconnect.setOnClickListener(v -> {
            if(myThreadBeConnected!=null){
                myThreadBeConnected.cancel();
            }
            if(myThreadConnected!=null){
                myThreadConnected.cancel();
            }
            btnReceiver.setVisibility(View.VISIBLE);
            btnDisconnect.setVisibility(View.GONE);
        });

        if(myThreadConnected != null && !myThreadConnected.cancelled) {
            btnReceiver.setVisibility(View.GONE);
            btnDisconnect.setVisibility(View.VISIBLE);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
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


    @Override
    protected void onStart() {
        super.onStart();

        //Turn ON BlueTooth if it is OFF
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    private void setup() {
        textStatus.setText("setup()");
        myThreadBeConnected = new BluetoothActivity.ThreadBeConnected();
        myThreadBeConnected.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(myThreadBeConnected!=null){
            myThreadBeConnected.cancel();
        }
    }


    private class ThreadBeConnected extends Thread {

        private BluetoothServerSocket bluetoothServerSocket = null;

        public ThreadBeConnected() {
            try {
                bluetoothServerSocket =
                        bluetoothAdapter.listenUsingRfcommWithServiceRecord("", myUUID);

                textStatus.setText("Waiting for device to connect...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            BluetoothSocket bluetoothSocket;

            if(bluetoothServerSocket!=null){
                try {
                    bluetoothSocket = bluetoothServerSocket.accept();

                    BluetoothDevice remoteDevice = bluetoothSocket.getRemoteDevice();

                    final String strConnected = "Connected:\n" +
                            remoteDevice.getName();

                    //connected
                    runOnUiThread(() -> {
                        textStatus.setText(strConnected);
                        btnReceiver.setVisibility(View.GONE);
                        btnDisconnect.setVisibility(View.VISIBLE);
                    });

                    startThreadConnected(bluetoothSocket);

                } catch (IOException e) {
                    e.printStackTrace();

                    final String eMessage = e.getMessage();
                    runOnUiThread(() -> textStatus.setText("Something went wrong: \n" + eMessage));
                    cancel();
                }
            }else{
                runOnUiThread(() -> textStatus.setText("bluetoothServerSocket == null"));
            }
        }

        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    public class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final ObjectInputStream connectedInputStream;
        private final ObjectOutputStream connectedOutputStream;
        public boolean cancelled = false;
        Handler handler;
        SharedPreferences sharedPreferences;
        SharedPreferences.Editor editor;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            ObjectInputStream in = null;
            ObjectOutputStream out = null;

            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
            handler = new Handler(Looper.getMainLooper());
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(BluetoothActivity.this);
            editor = sharedPreferences.edit();
        }

        @Override
        public void run() {
            Message message;
            while (true) {
                try {
                    message = (Message) connectedInputStream.readObject();
                    int m = message.getInfo();

                    switch (message.getType()) {
                        case INITIATE:
                            MainActivity.sensorLogger.setParticipant(message.getInfo());
                            MainActivity.soundscape.setMuteSteps(true);
                            break;
                        case WARNING:
                            runOnUiThread(() -> {
                                MainActivity.soundscape.playWarningSound(m);
                            });
                            MainActivity.sensorLogger.logWarning();
                            myThreadConnected.write(new Message(Message.Type.RESPONSE, 0));
                            break;
                        case NAVIGATION:
                            MainActivity.soundscape.wrongPath(message.getInfo() == -1);
                            MainActivity.sensorLogger.setCondition(message.getInfo());
                            myThreadConnected.write(new Message(Message.Type.RESPONSE, 0));
                            break;
                        case ADD_STEPS:
                            MainActivity.soundscape.setMuteSteps(false);
                            MainActivity.sensorLogger.setCondition(message.getInfo());
                            myThreadConnected.write(new Message(Message.Type.RESPONSE, 0));
                            break;
                        case START_LOGGING:
                            if(!MainActivity.sensorLogger.isRunning) {
                                switch (message.getInfo()) {
                                    case 1:
                                        String task = "";
                                        Soundscape.Type type = Soundscape.Type.REGULAR;
                                        switch (message.getSubTask()) {
                                            case 0:
                                                task = "1.1";
                                                break;
                                            case 1:
                                                task = "1.2";
                                                type = Soundscape.Type.FOREST;
                                                break;
                                            case 2:
                                                task = "1.3";
                                                type = Soundscape.Type.BEACH;
                                                break;
                                            case 3:
                                                task = "1.4";
                                                type = Soundscape.Type.MOUNTAINS;
                                                break;
                                        }
                                        final Soundscape.Type type2 = type;
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                MainActivity.soundscape.stop();
                                                MainActivity.soundscape.setType(type2);
                                                MainActivity.soundscape.setMuteSteps(true);
                                                MainActivity.soundscape.start();
                                            }
                                        });
                                        MainActivity.sensorLogger.setTask(task);
                                        MainActivity.sensorLogger.setCondition(0);
                                        break;
                                    case 2:
                                        MainActivity.sensorLogger.setTask("2");
                                        MainActivity.sensorLogger.setCondition(0);
                                        MainActivity.soundscape.setMuteSteps(false);
                                        MainActivity.soundscape.initStudyStoryMode();
                                        break;
                                    case 3:
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                MainActivity.soundscape.stop();
                                                MainActivity.soundscape.setType(Soundscape.Type.FOREST);
                                                MainActivity.soundscape.start();
                                                MainActivity.soundscape.setMuteSteps(false);
                                            }
                                        });
                                        MainActivity.sensorLogger.setTask("3");
                                        MainActivity.sensorLogger.setCondition(1);
                                        break;
                                    case 4:
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                MainActivity.soundscape.stop();
                                                MainActivity.soundscape.setType(Soundscape.Type.FOREST);
                                                MainActivity.soundscape.start();
                                                MainActivity.soundscape.setMuteSteps(false);
                                            }
                                        });
                                        String task2 = "";
                                        switch (message.getSubTask()) {
                                            case 0:
                                                task2 = "4.1";
                                                editor.putString("delay_preference", String.valueOf(-5));
                                                break;
                                            case 1:
                                                task2 = "4.2";
                                                editor.putString("delay_preference", String.valueOf(5));
                                                break;
                                        }
                                        MainActivity.sensorLogger.setTask(task2);
                                        MainActivity.sensorLogger.setCondition(0);
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                editor.apply();
                                            }
                                        }, 5000);
                                        break;
                                }
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        MainActivity.sensorLogger.startLogging();
                                    }
                                }, 2000);
                                myThreadConnected.write(new Message(Message.Type.START_LOGGING, 1));
                            } else {
                                MainActivity.sensorLogger.stopLogging();
                                switch (message.getInfo()) {
                                    case 2:
                                        MainActivity.soundscape.stopStudyStoryMode();
                                        break;
                                    case 3:
                                        MainActivity.soundscape.playArrivalSound();
                                        MainActivity.soundscape.wrongPath(false);
                                        break;
                                    case 4:
                                        editor.putString("delay_preference", "0");
                                        editor.apply();
                                        break;
                                }

                                MainActivity.soundscape.stop();
                                MainActivity.soundscape.setMuteSteps(true);

                                myThreadConnected.write(new Message(Message.Type.START_LOGGING, 0));
                            }

                            break;
                    }



                } catch (IOException e) {
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost.";
                    runOnUiThread(() -> {
                        textStatus.setText(msgConnectionLost);
                        btnReceiver.setVisibility(View.VISIBLE);
                        btnDisconnect.setVisibility(View.GONE);
                    });
                    if(MainActivity.sensorLogger != null && MainActivity.sensorLogger.isRunning) {
                        MainActivity.sensorLogger.stopLogging();
                    }
                    cancel();
                    cancelled = true;
                    if(myThreadBeConnected != null) {
                        myThreadBeConnected.cancel();
                    }
                    return;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(Message message) {
            try {
                connectedOutputStream.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}