package environzen.dev;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.WindowManager;

import java.util.Observable;
import java.util.Observer;

import environzen.dev.Algorithms.PocketDetector;

public class WakeUpActivity extends AppCompatActivity implements Observer {

    PocketDetector pocketDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        pocketDetector = new PocketDetector(this);
        pocketDetector.addObserver(this);
        pocketDetector.start();
    }

    @Override
    public void update(Observable observable, Object o) {
        boolean inPocket = (boolean) o;
        if(!inPocket) {
            pocketDetector.stop();
            finish();
        }
    }
}