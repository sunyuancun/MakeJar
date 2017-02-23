/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.xs.record;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Piasy{github.com/Piasy} on 16/2/24.
 * <p>
 * <em>NOTE: users should only have one instance active at the same time.</em>
 */
public final class StreamAudioPlayer {
    private static final String TAG = "StreamAudioPlayer";
    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_BUFFER_SIZE = 2048;

    private PLAY_STATUS mPlay_Status;

    public enum PLAY_STATUS {
        INIT, START, STOP
    }

    public AtomicBoolean mIsPlaying;
    private ExecutorService mPlayExecutorService;

    public interface AudioPlayCompeletedCallback {
        @WorkerThread
        void onAudioPlayCompeleted();
    }

    private StreamAudioPlayer() {
        // singleton
        mPlay_Status = PLAY_STATUS.INIT;
        mIsPlaying = new AtomicBoolean(false);
        mPlayExecutorService = Executors.newSingleThreadExecutor();
    }

    private static final class StreamAudioPlayerHolder {
        private static final StreamAudioPlayer INSTANCE = new StreamAudioPlayer();
    }

    public static StreamAudioPlayer getInstance() {
        return StreamAudioPlayerHolder.INSTANCE;
    }


    public int play(String path, AudioPlayCompeletedCallback playCompeletedCallback) {
        mPlay_Status = PLAY_STATUS.START;

        if (path == null || TextUtils.isEmpty(path)) {
            Log.w(TAG, "can't set empty play_path");
            return 3;
        }

        if (playCompeletedCallback == null) {
            Log.w(TAG, "can't set empty play_compelete_callback");
            return 2;
        }

        File file = new File(path);
        if (file.exists()) {
            if (mIsPlaying.compareAndSet(false, true)) {
                mPlayExecutorService.execute(new AudioTrackRunnable(path, playCompeletedCallback));
                return 0;
            }
            return 1;
        }

        return 4;
    }

    public int stopPlay() {
        mPlay_Status = PLAY_STATUS.STOP;
        mIsPlaying.compareAndSet(true, false);
        return 0;
    }

    private class AudioTrackRunnable implements Runnable {
        private AudioTrack mAudioTrack;
        private int minBufferSize;

        private String mPlayPath;
        private byte[] buffer = null;

        private AudioPlayCompeletedCallback mAudioPlayCompeletedCallback;

        AudioTrackRunnable(String path, AudioPlayCompeletedCallback playCompeletedCallback) {
            if (mAudioTrack != null) {
                mAudioTrack.release();
                mAudioTrack = null;
            }

            mAudioPlayCompeletedCallback = playCompeletedCallback;

            mPlayPath = path;
            minBufferSize = AudioTrack.getMinBufferSize(DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minBufferSize, DEFAULT_BUFFER_SIZE), AudioTrack.MODE_STREAM);
            buffer = new byte[Math.max(minBufferSize, DEFAULT_BUFFER_SIZE)];
        }

        @Override
        public void run() {
            if (mAudioTrack != null && mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {

                RandomAccessFile file = null;
                try {
                    file = new RandomAccessFile(mPlayPath, "r");
                } catch (Exception e) {
                    Log.w(TAG, "startplayed fail: " + e.getMessage());
                }

                try {
                    mAudioTrack.play();
                    Log.w(TAG, "startplayed");
                } catch (Exception e) {
                    Log.w(TAG, "startplayed fail: " + e.getMessage());
                }

                try {
                    while (mIsPlaying.get()) {
                        int size = file.read(buffer, 0, buffer.length);
                        if (size == -1) {
                            break;
                        }
                        mAudioTrack.write(buffer, 0, size);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    mAudioTrack.flush();
                    mAudioTrack.stop();
                    mAudioTrack.release();
                    mAudioTrack = null;
                    Log.w(TAG, "release sucess");
                } catch (Exception e) {
                    e.printStackTrace();
                }


                try {
                    if (file != null) {
                        file.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {

                    if (mPlay_Status.equals(PLAY_STATUS.START)) {
                        mAudioPlayCompeletedCallback.onAudioPlayCompeleted();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
