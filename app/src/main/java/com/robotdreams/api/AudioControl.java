package com.robotdreams.api;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import com.robotdreams.R;

/**
 * Created by itaimendelsohn on 10/14/15.
 */
public class AudioControl {

    //
    private MediaPlayer audioPlayer;

    //
    private Context context;

    //
    private RefreshHandler audioHandler;

    //
    private boolean isPaused;

    public AudioControl(Context context)
    {
        audioPlayer = null;
        this.context = context;
        isPaused = false;
        this.audioHandler = new RefreshHandler();
    }

    public void playAudio() {

        try {
            Thread.sleep(500);                 // delay by 500 milliseconds (0.5 seconds)
        } catch(Exception e) {
        }

        Uri uri = Uri.parse("android.resource://" + this.context.getPackageName() + "/" + R.raw.booksample);
        audioPlayer = audioPlayer.create(this.context,R.raw.booksample1);
        audioHandler.startAudioDelay(4000); // 4 sec delay
        //audioPlayer.start();
    }

    public void stopAudio()
    {
        if ((audioPlayer != null)&&(audioPlayer.isPlaying()))
        {
            audioPlayer.stop();
        }
    }
    public void pauseAudio()
    {
        if ((audioPlayer != null)&&(audioPlayer.isPlaying()))
        {
            audioPlayer.pause();
            isPaused = true;
        }
    }

    public void resumeAudio()
    {
        if ((audioPlayer != null)&&(isPaused))
        {
            audioHandler.startAudioDelay(4000); // 4 sec delay
            //audioPlayer.start();
            isPaused = false;
        }
    }

    public boolean isPaused() { return isPaused;}

    class RefreshHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) // start audio
                audioPlayer.start();
        }

        public void startAudioDelay(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }

    };

}
