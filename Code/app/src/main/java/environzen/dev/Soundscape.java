package environzen.dev;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Soundscape {

    public enum Type {
        REGULAR,
        CREEK,
        KALIMBA,
        BEACH,
        FOREST,
        RAIN,
        SNOW,
        MOUNTAINS,
        JUNGLE,
        STORY
    }

    ArrayList<MediaPlayer> stepSounds, stepSoundsFade;
    LoopMediaPlayer ambientSounds;
    MediaPlayer[] warningSound;
    MediaPlayer[] warningSound2;
    boolean firstWarning = true;
    MediaPlayer[] directionSound;
    MediaPlayer errorSound;
    MediaPlayer arrivalSound;
    float ambienceVolume = 0.2f;
    float stepVolume = 0.6f;
    int ambienceSliderVolume = 60;
    int stepSliderVolume = 60;
    Type type;
    WeakReference<Context> context;
    int lastRandomNumber;
    boolean wrongPath = false;
    boolean muteSteps = false;

    //for study only
    List<Type> soundscapes;
    int index = 0;
    Handler handler;
    Runnable runnable;
    float fadeVolume = 0;
    boolean storyMode = false;

    public Soundscape(Context context) {
        this.context = new WeakReference<>(context);
    }

    public void setType(Type type) {
        if(stepSounds != null) {
            for(MediaPlayer mp : stepSounds) {
                mp.release();
            }
        }
        stepSounds = new ArrayList<>();

        if(firstWarning) {
            if(warningSound != null) {
                for(MediaPlayer mp : warningSound) {
                    mp.release();
                }
            }
        } else {
            if(warningSound2 != null) {
                for(MediaPlayer mp : warningSound2) {
                    mp.release();
                }
            }
        }
        switch (type) {
            case REGULAR:
                stepSounds.add(MediaPlayer.create(context.get(), R.raw.regular));
                break;
            case RAIN:
                initializeStepSounds("rain", 10);
                break;
            case CREEK:
                initializeStepSounds("water", 10);
                break;
            case KALIMBA:
                initializeStepSounds("kalimba", 9);
                break;
            case JUNGLE:
            case FOREST:
                initializeStepSounds("forest", 10);
                break;
            case BEACH:
                initializeStepSounds("beach", 10);
                break;
            case SNOW:
                initializeStepSounds("snow", 10);
                break;
            case MOUNTAINS:
                initializeStepSounds("mountain", 10);
                break;
        }
        if(stepSounds != null && stepSounds.size() > 0) {
            lastRandomNumber = new Random().nextInt(stepSounds.size());
        }
        this.type = type;
        initializeWarningSounds();
    }

    public void initializeNavigationSounds() {
        if(errorSound == null) {
            errorSound = MediaPlayer.create(context.get(), R.raw.error);
        }
        if(arrivalSound == null) {
            arrivalSound = MediaPlayer.create(context.get(), R.raw.gong);
        }
        if(directionSound == null) {
            directionSound = new MediaPlayer[8];
            String fileName = "chimes";
            directionSound[0] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_n", "raw", context.get().getPackageName()));
            directionSound[1] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_ne", "raw", context.get().getPackageName()));
            directionSound[2] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_e", "raw", context.get().getPackageName()));
            directionSound[3] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_se", "raw", context.get().getPackageName()));
            directionSound[4] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_s", "raw", context.get().getPackageName()));
            directionSound[5] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_sw", "raw", context.get().getPackageName()));
            directionSound[6] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_w", "raw", context.get().getPackageName()));
            directionSound[7] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_nw", "raw", context.get().getPackageName()));

        }
    }

    private void initializeStepSounds(String fileName, int sampleNumber) {
        for(int i = 1; i <= sampleNumber; i++) {
            stepSounds.add(MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + i, "raw", context.get().getPackageName())));
        }
    }

    private void initializeWarningSounds() {
        String fileName = "";
        switch (type) {
            case KALIMBA:
                fileName = "eguitar";
                break;
            case JUNGLE:
                fileName = "tiger";
                break;
            case FOREST:
                fileName = "woodpecker";
                break;
            case BEACH:
                fileName = "ship";
                break;
            case SNOW:
                fileName = "sleighbell";
                break;
            default:
                fileName = "bicycle";
                break;
        }
        if(firstWarning) {
            warningSound = new MediaPlayer[8];
            warningSound[0] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_n", "raw", context.get().getPackageName()));
            warningSound[1] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_ne", "raw", context.get().getPackageName()));
            warningSound[2] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_e", "raw", context.get().getPackageName()));
            warningSound[3] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_se", "raw", context.get().getPackageName()));
            warningSound[4] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_s", "raw", context.get().getPackageName()));
            warningSound[5] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_sw", "raw", context.get().getPackageName()));
            warningSound[6] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_w", "raw", context.get().getPackageName()));
            warningSound[7] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_nw", "raw", context.get().getPackageName()));
        } else {
            warningSound2 = new MediaPlayer[8];
            warningSound2[0] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_n", "raw", context.get().getPackageName()));
            warningSound2[1] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_ne", "raw", context.get().getPackageName()));
            warningSound2[2] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_e", "raw", context.get().getPackageName()));
            warningSound2[3] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_se", "raw", context.get().getPackageName()));
            warningSound2[4] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_s", "raw", context.get().getPackageName()));
            warningSound2[5] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_sw", "raw", context.get().getPackageName()));
            warningSound2[6] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_w", "raw", context.get().getPackageName()));
            warningSound2[7] = MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + "_nw", "raw", context.get().getPackageName()));
        }
        firstWarning = !firstWarning;
    }

    public void playStep(float speed) {
        if(muteSteps) return;
        if(wrongPath) {
            errorSound.start();
            return;
        }
        speed = (1 - speed) + 0.4f;
        if(speed < 0.15) {
            speed = 0.15f;
        } else if(speed > 1) {
            speed = 1;
        }
        int random = 0;
        if(stepSounds.size() > 1) {
            int rotate = 1 + new Random().nextInt(stepSounds.size() - 1);
            random = (lastRandomNumber + rotate) % stepSounds.size();
        }
        float volume = (float)(1 - (Math.log(100 - speed * 100) / Math.log(100))) * stepVolume;
        if(volume > 1) volume = 1;
        float fade = 1;
        if(fadeVolume != 0) fade = fadeVolume;
        stepSounds.get(random).setVolume(fade * volume, fade * volume);
        stepSounds.get(random).setPlaybackParams(stepSounds.get(random).getPlaybackParams().setSpeed(0.85f + speed));
        stepSounds.get(random).start();
        if(stepSoundsFade != null && stepSoundsFade.size() > 0) {
            stepSoundsFade.get(random).setVolume((1 - fade) * volume, (1 - fade) * volume);
            stepSoundsFade.get(random).setPlaybackParams(stepSounds.get(random).getPlaybackParams().setSpeed(0.85f + speed));
            stepSoundsFade.get(random).start();
        }
        lastRandomNumber = random;
    }

    public void start() {
        float volume = ambienceVolume;
        if(storyMode) volume = 0;

        switch (type) {
            case CREEK:
                ambientSounds = LoopMediaPlayer.create(context.get(), R.raw.creek_ambience, volume);
                break;
            case RAIN:
                ambientSounds = LoopMediaPlayer.create(context.get(), R.raw.rain_ambience, volume);
                break;
            case FOREST:
                ambientSounds = LoopMediaPlayer.create(context.get(), R.raw.forest_ambience, volume);
                break;
            case BEACH:
                ambientSounds = LoopMediaPlayer.create(context.get(), R.raw.beach_ambience, volume);
                break;
            case SNOW:
                ambientSounds = LoopMediaPlayer.create(context.get(), R.raw.wind_ambience, volume);
                break;
            case MOUNTAINS:
                ambientSounds = LoopMediaPlayer.create(context.get(), R.raw.mountain_ambience, volume);
                break;
            case JUNGLE:
                ambientSounds = LoopMediaPlayer.create(context.get(), R.raw.jungle_ambience, volume);
                break;
            case KALIMBA:
                ambientSounds = LoopMediaPlayer.create(context.get(), R.raw.drum_loop, volume);
                break;
            default:
                if(ambientSounds != null) {
                    ambientSounds.stop();
                }
                break;
        }
    }

    public void stop() {
        if(ambientSounds != null) {
            ambientSounds.stop();
        }
    }

    public void fadeOut(Type soundscape) {
        stepSoundsFade = new ArrayList<>();
        switch (soundscape) {
            case FOREST:
            case JUNGLE:
                initializeStepSoundsFade("forest", 10);
                break;
            case BEACH:
                initializeStepSoundsFade("beach", 10);
                break;
            case MOUNTAINS:
                initializeStepSoundsFade("mountain", 10);
                break;
            case CREEK:
                initializeStepSoundsFade("water", 10);
                break;
            case SNOW:
                initializeStepSoundsFade("snow", 10);
                break;
            case RAIN:
                initializeStepSoundsFade("rain", 10);
                break;
        }
        if(ambientSounds != null) {
            LoopMediaPlayer copy = ambientSounds;

            final int FADE_DURATION = 10000;
            final int FADE_INTERVAL = 250;
            final int MAX_VOLUME = 1;
            int numberOfSteps = FADE_DURATION/FADE_INTERVAL;
            final float deltaVolume = MAX_VOLUME / (float)numberOfSteps;
            fadeVolume = 0;

            final Timer timer = new Timer(true);
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    ambientSounds.setVolume(ambienceVolume * fadeVolume);
                    copy.setVolume(ambienceVolume - ambienceVolume * fadeVolume);
                    fadeVolume += deltaVolume;
                    if(fadeVolume>=MAX_VOLUME){
                        copy.stop();
                        timer.cancel();
                        timer.purge();
                        if(stepSoundsFade != null) {
                            for(MediaPlayer mp : stepSoundsFade) {
                                mp.release();
                            }
                        }
                        stepSoundsFade = null;
                    }
                }
            };

            timer.schedule(timerTask,FADE_INTERVAL,FADE_INTERVAL);
        }
    }

    private void initializeStepSoundsFade(String fileName, int sampleNumber) {
        for(int i = 1; i <= sampleNumber; i++) {
            stepSoundsFade.add(MediaPlayer.create(context.get(), context.get().getResources().getIdentifier(fileName + i, "raw", context.get().getPackageName())));
        }
    }

    public void setAmbienceVolume(int sliderVolume) {
        this.ambienceSliderVolume = sliderVolume;
        float volume = (float)sliderVolume / 100;
        volume = (float)(1 - (Math.log(100 - volume * 100) / Math.log(100)));
        if(volume > 1) volume = 1;
        if(ambientSounds != null) {
            ambientSounds.setVolume(volume);
        }
        ambienceVolume = volume;
    }

    public void setStepVolume(int sliderVolume) {
        this.stepSliderVolume = sliderVolume;
        float volume = (float)sliderVolume / 100;
        stepVolume = volume;
        if(stepSounds != null) {
            stepSounds.get(0).setVolume(volume * 0.2f, volume * 0.2f);
            stepSounds.get(0).start();
        }

    }

    public int getAmbienceSliderVolume() {
        return ambienceSliderVolume;
    }

    public int getStepSliderVolume() {
        return stepSliderVolume;
    }

    public void playWarningSound(int direction) {
        if(warningSound == null && firstWarning) {
            initializeWarningSounds();
        }
        if(!firstWarning) {
            if(type == Type.BEACH || type == Type.MOUNTAINS) {
                warningSound[direction].setVolume(0.5f, 0.5f);
            } else {
                warningSound[direction].setVolume(1f, 1f);
            }
            warningSound[direction].start();
        } else {
            if(type == Type.BEACH || type == Type.MOUNTAINS) {
                warningSound2[direction].setVolume(0.5f, 0.5f);
            } else {
                warningSound2[direction].setVolume(1f, 1f);
            }
            warningSound2[direction].start();
        }
    }

    public void wrongPath(boolean wrong) {
        wrongPath = wrong;
    }

    public void playDirectionSound(int direction) {
        directionSound[direction].start();
    }


    public void playArrivalSound() {
        arrivalSound.start();
    }

    public void initStudyStoryMode() {
        soundscapes = new ArrayList<>();
        soundscapes.add(Type.BEACH);
        soundscapes.add(Type.FOREST);
        soundscapes.add(Type.MOUNTAINS);

        handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                stop();
                setType(soundscapes.get(0));
                start();
                storyMode = true;
            }
        });
        index = 1;
        runnable = new Runnable() {
            @Override
            public void run() {
                studyStoryMode();
            }
        };

        handler.postDelayed(runnable, 120000);
    }

    public void studyStoryMode() {
        if(index < soundscapes.size()) {
            fadeOut(soundscapes.get(index-1));
            setType(soundscapes.get(index));
            start();
            MainActivity.sensorLogger.setCondition(index);
            index++;
            handler.postDelayed(runnable, 120000);
        } else {
            stop();
            storyMode = false;
            MainActivity.sensorLogger.stopLogging();
            setMuteSteps(true);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    stop();
                }
            });
            if(BluetoothActivity.myThreadConnected != null) {
                BluetoothActivity.myThreadConnected.write(new Message(Message.Type.START_LOGGING, 0));
            }
        }
    }

    public void stopStudyStoryMode() {
        handler.removeCallbacks(runnable);
        storyMode = false;
    }

    public void setMuteSteps(boolean muteSteps) {
        this.muteSteps = muteSteps;
    }

    public Type getType() {
        return type;
    }

    public void setStoryMode(boolean storyMode) {
        this.storyMode = storyMode;
    }
}
