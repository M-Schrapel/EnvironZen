package environzen.dev;

import android.content.Context;
import android.media.MediaPlayer;

public class LoopMediaPlayer {

    public static final String TAG = LoopMediaPlayer.class.getSimpleName();

    private Context mContext = null;
    private int mResId = 0;
    private float volume;

    private MediaPlayer mCurrentPlayer = null;
    private MediaPlayer mNextPlayer = null;

    public static LoopMediaPlayer create(Context context, int resId, float volume) {
        return new LoopMediaPlayer(context, resId, volume);
    }

    private LoopMediaPlayer(Context context, int resId, float volume) {
        mContext = context;
        mResId = resId;
        this.volume = volume;

        mCurrentPlayer = MediaPlayer.create(mContext, mResId);
        mCurrentPlayer.setVolume(volume, volume);
        mCurrentPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mCurrentPlayer.start();
            }
        });

        createNextMediaPlayer();
    }

    private void createNextMediaPlayer() {
        mNextPlayer = MediaPlayer.create(mContext, mResId);
        mNextPlayer.setVolume(volume, volume);
        mCurrentPlayer.setNextMediaPlayer(mNextPlayer);
        mCurrentPlayer.setOnCompletionListener(onCompletionListener);
    }

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.release();
            mCurrentPlayer = mNextPlayer;

            createNextMediaPlayer();
        }
    };

    public void stop() {
        mCurrentPlayer.stop();
    }

    public void setVolume(float volume) {
        mCurrentPlayer.setVolume(volume, volume);
        mNextPlayer.setVolume(volume, volume);
        this.volume = volume;
    }
}