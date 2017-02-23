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
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Piasy{github.com/Piasy} on 16/2/24.
 * <p>   solve  startRecording() called on an uninitialized AudioRecord. : http://www.cnblogs.com/mythou/p/3241925.html
 * <em>NOTE: users should only have one instance active at the same time.</em>
 */
public final class StreamAudioRecorder {
    private static final String TAG = "StreamAudioRecorder";
    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private Boolean fileHeader = true;
    public AtomicBoolean mIsRecording;
    private ExecutorService mExecutorService;


    private StreamAudioRecorder() {
        // singleton
        mIsRecording = new AtomicBoolean(false);
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    private static final class StreamAudioRecorderHolder {
        private static final StreamAudioRecorder INSTANCE = new StreamAudioRecorder();
    }

    public static StreamAudioRecorder getInstance() {
        return StreamAudioRecorderHolder.INSTANCE;
    }

    /**
     * Although Android frameworks jni implementation are the same for ENCODING_PCM_16BIT and
     * ENCODING_PCM_8BIT, the Java doc declared that the buffer type should be the corresponding
     * type, so we use different ways.
     */
    public interface AudioDataCallback {
        @WorkerThread
        void onAudioData(byte[] data, int size);

        void onError();
    }

    public interface AudioStartCompeletedCallback {
        @WorkerThread
        void onAudioStartCompeleted();

    }


    private class AudioRecordRunnable implements Runnable {

        private final AudioDataCallback mAudioDataCallback;
        private final AudioStartCompeletedCallback mAudioStartCompeletedCallback;


        private final byte[] mByteBuffer;
        private final short[] mShortBuffer;
        private final int mByteBufferSize;
        private final int mShortBufferSize;
        private final int mAudioFormat;
        private RandomAccessFile file = null;

        private int minBufferSize;
        private AudioRecord mAudioRecord;

        AudioRecordRunnable(int audioFormat, @NonNull AudioStartCompeletedCallback audioStartCompeletedCallback, @NonNull AudioDataCallback audioDataCallback, String path) {

            if (mAudioRecord != null) {
                mAudioRecord.release();
                mAudioRecord = null;
            }

            minBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minBufferSize, DEFAULT_BUFFER_SIZE));

            mAudioFormat = audioFormat;
            mByteBufferSize = Math.max(minBufferSize, DEFAULT_BUFFER_SIZE);
            mShortBufferSize = mByteBufferSize / 2;
            mByteBuffer = new byte[mByteBufferSize];
            mShortBuffer = new short[mShortBufferSize];
            mAudioDataCallback = audioDataCallback;
            mAudioStartCompeletedCallback = audioStartCompeletedCallback;

            try {
                file = StreamAudioRecorder.this.fopen(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                try {
                    mAudioRecord.startRecording();
                    Log.w(TAG, "startRecorded");
                    mAudioStartCompeletedCallback.onAudioStartCompeleted();
                } catch (Exception e) {
                    Log.w(TAG, "startRecording fail: " + e.getMessage());
                    mAudioDataCallback.onError();
                }

                try {

                      /*
                     * discard the beginning 100ms for fixing the transient noise bug
                     *
                     */
                    int discardBytes = 1 * 16000 * 16 * 100 / 1000 / 8;
                    while (discardBytes > 0) {
                        int requestBytes = mByteBuffer.length < discardBytes ? mByteBuffer.length : discardBytes;
                        int readBytes = mAudioRecord.read(mByteBuffer, 0, requestBytes);
                        if (readBytes > 0) {
                            discardBytes -= readBytes;
                            Log.d(TAG, "discard: " + readBytes);
                        } else {
                            break;
                        }
                    }

                    while (mIsRecording.get()) {
                        int ret;
                        if (mAudioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                            ret = mAudioRecord.read(mByteBuffer, 0, mByteBufferSize);
                            if (ret > 0) {
                                Log.w(TAG, "Recording ....");
                                mAudioDataCallback.onAudioData(mByteBuffer, ret);

                                if (file != null) {
                                    StreamAudioRecorder.this.fwrite(file, mByteBuffer, 0, ret);
                                }

                            } else {
                                onError(ret);
                                Log.w(TAG, "Recording error");
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;
                    Log.w(TAG, "release sucess");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    if (file != null) {
                        StreamAudioRecorder.this.fclose(file);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }

        private byte[] short2byte(short[] sData, int size, byte[] bData) {
            if (size > sData.length || size * 2 > bData.length) {
                Log.w(TAG, "short2byte: too long short data array");
            }
            for (int i = 0; i < size; i++) {
                bData[i * 2] = (byte) (sData[i] & 0x00FF);
                bData[(i * 2) + 1] = (byte) (sData[i] >> 8);
            }
            return bData;
        }

        private void onError(int errorCode) {
            if (errorCode == AudioRecord.ERROR_INVALID_OPERATION) {
                Log.w(TAG, "record fail: ERROR_INVALID_OPERATION");
                mAudioDataCallback.onError();
            } else if (errorCode == AudioRecord.ERROR_BAD_VALUE) {
                Log.w(TAG, "record fail: ERROR_BAD_VALUE");
                mAudioDataCallback.onError();
            }
        }
    }


    public int start(String path, @NonNull AudioStartCompeletedCallback audioStartCompeletedCallback, @NonNull AudioDataCallback audioDataCallback) {

        if (path == null || TextUtils.isEmpty(path)) {
            Log.w(TAG, "can't set empty  record_path");
            return 1;
        }

        if (mIsRecording.compareAndSet(false, true)) {
            mExecutorService.execute(new AudioRecordRunnable(AudioFormat.ENCODING_PCM_16BIT, audioStartCompeletedCallback, audioDataCallback, path));
            return 0;
        }
        return 2;
    }

    public int stop() {
        mIsRecording.compareAndSet(true, false);
        return 0;
    }

    private RandomAccessFile fopen(String path) throws IOException {

        int CHANNELS = 1;
        int BITS = 16;
        int FREQUENCY = 16000;

        File f = new File(path);
        if (f.exists()) {
            f.delete();
        } else {
            File file = f.getParentFile();
            if (!file.exists()) {
                file.mkdirs();
            }
        }

        RandomAccessFile file1 = new RandomAccessFile(f, "rw");
        if (StreamAudioRecorder.getInstance().fileHeader) {
            file1.writeBytes("RIFF");
            file1.writeInt(0);
            file1.writeBytes("WAVE");
            file1.writeBytes("fmt ");
            file1.writeInt(Integer.reverseBytes(16));
            file1.writeShort(Short.reverseBytes((short) 1));
            file1.writeShort(Short.reverseBytes((short) CHANNELS));
            file1.writeInt(Integer.reverseBytes(FREQUENCY));
            file1.writeInt(Integer.reverseBytes(CHANNELS * FREQUENCY * BITS / 8));
            file1.writeShort(Short.reverseBytes((short) (CHANNELS * BITS / 8)));
            file1.writeShort(Short.reverseBytes((short) (CHANNELS * BITS)));
            file1.writeBytes("data");
            file1.writeInt(0);
        }

        Log.d(TAG, "wav path: " + path);
        return file1;
    }

    private void fwrite(RandomAccessFile file, byte[] data, int offset, int size) throws IOException {
        file.write(data, offset, size);
    }

    private void fclose(RandomAccessFile file) throws IOException {
        try {
            file.seek(4L);
            file.writeInt(Integer.reverseBytes((int) (file.length() - 8L)));
            file.seek(40L);
            file.writeInt(Integer.reverseBytes((int) (file.length() - 44L)));
            Log.d(TAG, "wav size: " + file.length());
        } finally {
            file.close();
        }

    }

}
