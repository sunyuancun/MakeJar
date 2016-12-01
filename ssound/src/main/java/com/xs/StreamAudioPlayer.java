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

package com.xs;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Piasy{github.com/Piasy} on 16/2/24.
 * <p>
 * <em>NOTE: users should only have one instance active at the same time.</em>
 */
public final class StreamAudioPlayer {
    private static final String TAG = "StreamAudioPlayer";
    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_BUFFER_SIZE = 2048;

    private ExecutorService mPlayExecutorService;

    private StreamAudioPlayer() {
        // singleton
        mPlayExecutorService = Executors.newSingleThreadExecutor();
    }

    private static final class StreamAudioPlayerHolder {
        private static final StreamAudioPlayer INSTANCE = new StreamAudioPlayer();
    }

    public static StreamAudioPlayer getInstance() {
        return StreamAudioPlayerHolder.INSTANCE;
    }


    public boolean play(String path) {

        if (path == null || TextUtils.isEmpty(path)) {
            Log.w(TAG, "can't set empty play_path");
            return false;
        }

        File file = new File(path);
        if (file.exists()) {
            mPlayExecutorService.execute(new AudioTrackRunnable(path));
            return true;
        }
        return false;
    }


    private class AudioTrackRunnable implements Runnable {
        private AudioTrack mAudioTrack;
        private int minBufferSize;

        private String mPlayPath;
        private byte[] buffer = null;

        AudioTrackRunnable(String path) {
            if (mAudioTrack != null) {
                mAudioTrack.release();
                mAudioTrack = null;
            }

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
                    while (true) {
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
            }
        }
    }

}
