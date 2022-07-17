package environzen.dev;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import environzen.dev.Algorithms.Filter.RollingAverage;
import environzen.dev.Algorithms.PocketDetector;
import environzen.dev.Algorithms.PeakDetector;

public class MainActivity extends AppCompatActivity implements Observer, SharedPreferences.OnSharedPreferenceChangeListener {


    StepDetector stepDetector;
    PocketDetector pocketDetector;
    boolean inPocket;
    boolean pocketMode;
    public static Soundscape soundscape;
    int soundscapeIndex = 0;
    boolean enabled = false;
    TextView soundscapeText;
    List<SoundscapeInfo> soundscapes;
    ConstraintLayout constraintLayout;
    ImageView leftArrow;
    ImageView rightArrow;
    boolean storyMode;
    int lastRandomNumber;
    Handler handler;
    Runnable storyRunnable, stepDelayRunnable, pocketDelayRunnable, increaseStepDelayRunnable;
    SeekBar ambienceVolume, stepVolume;
    int stepDelay;
    RollingAverage stepTimeAverage;
    int stepTimeCount = 0;
    float averageStepTime = 0;
    Soundscape.Type previousStory;

    public static SensorLogger sensorLogger;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView buddha2 = (ImageView) findViewById(R.id.buddha2);
        buddha2.setAlpha(0f);

        soundscapeText = findViewById(R.id.soundscape_text);
        leftArrow = findViewById(R.id.arrow_left);
        rightArrow = findViewById(R.id.arrow_right);
        constraintLayout = findViewById(R.id.constraintLayout);

        handler = new Handler(Looper.getMainLooper());
        storyRunnable = new Runnable() {
            @Override
            public void run() {
                startStoryMode();
            }
        };

        stepDelayRunnable = new Runnable() {
            @Override
            public void run() {
                playDelayedSteps();
            }
        };

        increaseStepDelayRunnable = new Runnable() {
            @Override
            public void run() {
                increaseStepDelay();
            }
        };

        pocketDelayRunnable = new Runnable() {
            @Override
            public void run() {
                stepDetector.start();
            }
        };



        stepDetector = new PeakDetector(MainActivity.this);
        stepDetector.addObserver(MainActivity.this);

        soundscapes = new ArrayList<>();
        soundscapes.add(new SoundscapeInfo(Soundscape.Type.REGULAR, "Regular", Color.BLACK));
        soundscapes.add(new SoundscapeInfo(Soundscape.Type.FOREST,"Forest", Color.parseColor("#6fa334")));
        soundscapes.add(new SoundscapeInfo(Soundscape.Type.BEACH,"Beach", Color.parseColor("#faff90")));
        soundscapes.add(new SoundscapeInfo(Soundscape.Type.RAIN,"Rain", Color.parseColor("#90a3ff")));
        soundscapes.add(new SoundscapeInfo(Soundscape.Type.CREEK,"Creek", Color.parseColor("#96f4ff")));
        soundscapes.add(new SoundscapeInfo(Soundscape.Type.SNOW,"Snow", Color.WHITE));
        soundscapes.add(new SoundscapeInfo(Soundscape.Type.KALIMBA,"Musical", Color.parseColor("#ffa559")));
        soundscapes.add(new SoundscapeInfo(Soundscape.Type.MOUNTAINS,"Mountains", Color.parseColor("#d3dae0")));
        soundscapes.add(new SoundscapeInfo(Soundscape.Type.JUNGLE,"Jungle", Color.parseColor("#006400")));
        soundscapes.add(new SoundscapeInfo(Soundscape.Type.STORY,"Story Mode", Color.WHITE));

        soundscape = new Soundscape(MainActivity.this.getApplicationContext());
        selectSoundscape();
        soundscape.initializeNavigationSounds();


        pocketDetector = new PocketDetector(this);


        constraintLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public boolean onSwipeLeft() {
                soundscapeIndex++;
                if(soundscapeIndex > soundscapes.size() - 1) soundscapeIndex = 0;
                selectSoundscape();
                return true;
            }

            @Override
            public boolean onSwipeRight() {
                soundscapeIndex--;
                if(soundscapeIndex < 0) soundscapeIndex = soundscapes.size() - 1;
                selectSoundscape();
                return true;
            }

            @Override
            public boolean onSwipeTop() {

                if(!enabled) {
                    enabled = true;
                    ImageView buddha = (ImageView) findViewById(R.id.buddha);
                    ImageView buddha2 = (ImageView) findViewById(R.id.buddha2);
                    ImageView b1= buddha2;
                    ImageView b2= buddha;
                    b1= buddha;
                    b2= buddha2;
                    fade(b1, b2);
                    if(pocketMode) {
                        pocketDetector.addObserver(MainActivity.this);
                        pocketDetector.start();
                    } else {
                        stepDetector.start();
                    }
                    if(storyMode) {
                        startStoryMode();
                    } else {
                        if(soundscape.type != soundscapes.get(soundscapeIndex).type) {
                            soundscape.setType(soundscapes.get(soundscapeIndex).type);
                        }
                        soundscape.start();
                    }
                    Toast toast = Toast.makeText(MainActivity.this, "Swipe down to stop", Toast.LENGTH_LONG);
                    toast.show();
                }

                return true;
            }

            @Override
            public boolean onSwipeBottom() {
                if(enabled) {
                    enabled = false;
                    ImageView buddha = (ImageView) findViewById(R.id.buddha);
                    ImageView buddha2 = (ImageView) findViewById(R.id.buddha2);
                    ImageView b1= buddha2;
                    ImageView b2= buddha;
                    fade(b1, b2);
                    if(pocketMode) {
                        pocketDetector.stop();
                    } else {
                        stepDetector.stop();
                    }
                    soundscape.stop();
                    if(storyMode) {
                        handler.removeCallbacks(storyRunnable);
                        previousStory = null;
                        soundscape.setStoryMode(false);
                        backgroundAnimation(soundscapeIndex);
                    }
                }

                return true;
            }
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        pocketMode = !sharedPreferences.getBoolean("pocket_mode_preference", false);
        stepDelay = 0;
        stepTimeAverage = new RollingAverage(5);

        Toast toast = Toast.makeText(this, "Swipe up to start", Toast.LENGTH_LONG);
        toast.show();

        sensorLogger = new SensorLogger(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_example, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            /*case R.id.mainactivity:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;*/
            case R.id.settings:
                Intent intent2 = new Intent(this, Settings.class);
                startActivity(intent2);
                return true;
            case R.id.about:
                Intent intent3 = new Intent(this, About.class);
                startActivity(intent3);
                return true;
            case R.id.sensor_logger:
                Intent intent4 = new Intent(this, SensorLoggerActivity.class);
                startActivity(intent4);
                return true;
            case R.id.bluetooth:
                Intent intent5 = new Intent(this, BluetoothActivity.class);
                startActivity(intent5);
                return true;
            case R.id.navigation:
                Intent intent6 = new Intent(this, NavigationActivity.class);
                startActivity(intent6);
                return true;
            case R.id.volume:
                onButtonShowPopupWindowClick();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void fade(ImageView v1, ImageView v2) {
        v1.animate().alpha(0f).setDuration(2000);
        v2.animate().alpha(1f).setDuration(2000);
        v2.bringToFront();
    }

    @Override
    public void update(Observable observable, Object o) {
        if(observable instanceof PocketDetector) {
            boolean newInPocket = (Boolean) o;
            if(newInPocket && !inPocket) {
                inPocket = true;
                Intent intent = new Intent(this, WakeUpActivity.class);
                startActivity(intent);
                handler.postDelayed(pocketDelayRunnable, 1000);
            } else if(!newInPocket && inPocket){
                handler.removeCallbacks(pocketDelayRunnable);
                inPocket = false;
                if(pocketMode) {
                    stepDetector.stop();
                }
            }
        } else if(observable instanceof StepDetector) {
            float stepTime = (float) o;
            if(sensorLogger != null && sensorLogger.isRunning && stepTime != 0.6f) {
                sensorLogger.logStepTime(stepTime);
            }
            if(stepTime == 0 || (stepDelay == 0 && averageStepTime != 0)) {
                handler.removeCallbacks(stepDelayRunnable);
                handler.removeCallbacks(increaseStepDelayRunnable);
                stepTimeCount = 0;
                averageStepTime = 0;
                stepTimeAverage = new RollingAverage(5);
            } else {
                if(stepTime == 0.6) {
                    soundscape.playStep(stepTime);
                } else {
                    if(stepTimeCount >= 5 && averageStepTime == 0 && stepDelay != 0) {
                        averageStepTime = (float) stepTimeAverage.getAverage();
                        playDelayedSteps();
                        handler.postDelayed(increaseStepDelayRunnable, 2000);
                    } else if (stepTimeCount < 5 || stepDelay == 0){
                        soundscape.playStep(stepTime);
                        stepTimeAverage.add(stepTime);
                        if(stepTimeCount < 5) stepTimeCount++;
                    }
                }
            }
        }
    }

    private void playDelayedSteps() {
        soundscape.playStep(averageStepTime + (float)stepDelay/1000);
        if(sensorLogger.isRunning) sensorLogger.setCondition(stepDelay);
        handler.postDelayed(stepDelayRunnable, (long)(averageStepTime*1000) + stepDelay);
    }

    private void increaseStepDelay() {
        if(stepDelay < 0) stepDelay -= 5;
        if(stepDelay > 0) stepDelay += 5;
        if(sensorLogger.isRunning) sensorLogger.setCondition(stepDelay);
        if(!(stepDelay >= 150 || stepDelay <= -150)) {
            handler.postDelayed(increaseStepDelayRunnable, 2000);
        } else {
            if(BluetoothActivity.myThreadConnected != null) {
                BluetoothActivity.myThreadConnected.write(new Message(Message.Type.DELAY, 0));
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch(s) {
            case "pocket_mode_preference":
                pocketMode = !sharedPreferences.getBoolean(s, false);
                if(!pocketMode && enabled) {
                    stepDetector.start();
                } else if(pocketMode) {
                    stepDetector.stop();
                    pocketDetector.addObserver(this);
                    pocketDetector.start();
                }
                break;
            case "delay_preference":
                stepDelay = Integer.parseInt(sharedPreferences.getString("delay_preference", "0"));
                if(stepDelay != 0 && stepTimeAverage == null) {
                    stepTimeAverage = new RollingAverage(5);
                } else if (stepDelay == 0) {
                    handler.removeCallbacks(increaseStepDelayRunnable);
                }
        }
    }

    private void selectSoundscape() {
        if(soundscape != null) {
            soundscape.stop();
        }
        if(soundscapes.get(soundscapeIndex).type != Soundscape.Type.STORY) {
            storyMode = false;
            handler.removeCallbacks(storyRunnable);
            if(enabled) {
                soundscape.setType(soundscapes.get(soundscapeIndex).type);
                soundscape.start();
            }
            backgroundAnimation(soundscapeIndex);
        } else {
            storyMode = true;
            if(enabled) {
                startStoryMode();
            } else {
                backgroundAnimation(soundscapeIndex);
            }
        }
        soundscapeText.setText(soundscapes.get(soundscapeIndex).name);
    }

    private void backgroundAnimation(int index) {
        int colorFrom = Color.TRANSPARENT;

        Drawable background = (ColorDrawable) constraintLayout.getBackground();
        if (background instanceof ColorDrawable)
            colorFrom = ((ColorDrawable) background).getColor();


        int colorTo = soundscapes.get(index).color;
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(500);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                constraintLayout.setBackgroundColor((int) animator.getAnimatedValue());
            }

        });
        colorAnimation.start();

        int color = soundscapes.get(index).color;

        if(Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114 > 140) {
            soundscapeText.setTextColor(Color.BLACK);
            rightArrow.setColorFilter(Color.argb(255, 0, 0, 0));
            leftArrow.setColorFilter(Color.argb(255, 0, 0, 0));
        } else {
            soundscapeText.setTextColor(Color.WHITE);
            rightArrow.setColorFilter(Color.argb(255, 255, 255, 255));
            leftArrow.setColorFilter(Color.argb(255, 255, 255, 255));
        }
    }

    class SoundscapeInfo{
        Soundscape.Type type;
        String name;
        int color;

        public SoundscapeInfo(Soundscape.Type type, String name, int color) {
            this.type = type;
            this.name = name;
            this.color = color;
        }
    }

    public void onLeft(View view) {
        soundscapeIndex--;
        if(soundscapeIndex < 0) soundscapeIndex = soundscapes.size() - 1;
        selectSoundscape();
    }

    public void onRight(View view) {
        soundscapeIndex++;
        if(soundscapeIndex > soundscapes.size() - 1) soundscapeIndex = 0;
        selectSoundscape();
    }

    public void startStoryMode() {
        if(enabled && storyMode) {
            int random = 0;
            if(soundscapes.size() > 1) {
                while(random == 0 || random == 9 || random == 6) {
                    int rotate = 1 + new Random().nextInt(soundscapes.size() - 1);
                    random = (lastRandomNumber + rotate) % soundscapes.size();
                }
            }
            lastRandomNumber = random;
            if(previousStory != null) {
                soundscape.fadeOut(previousStory);
            }
            soundscape.setType(soundscapes.get(random).type);
            previousStory = soundscapes.get(random).type;
            soundscape.start();
            soundscape.setStoryMode(true);
            backgroundAnimation(random);
            handler.postDelayed(storyRunnable, 45000);
        }

    }

    public void onButtonShowPopupWindowClick() {

        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.volume_popup, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        Rect rectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
        int contentViewTop =
                window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleBarHeight= contentViewTop - statusBarHeight;

        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }
        popupWindow.showAtLocation(popupView, Gravity.TOP | Gravity.RIGHT, 16, actionBarHeight + titleBarHeight);

        ambienceVolume = popupView.findViewById(R.id.ambience_volume);
        ambienceVolume.setProgress(soundscape.getAmbienceSliderVolume());
        ambienceVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                soundscape.setAmbienceVolume(progress);
            }
        });

        stepVolume = popupView.findViewById(R.id.step_volume);
        stepVolume.setProgress(soundscape.getStepSliderVolume());
        stepVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                soundscape.setStepVolume(progress);
            }
        });
    }

}