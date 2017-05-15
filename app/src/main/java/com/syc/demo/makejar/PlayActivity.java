package com.syc.demo.makejar;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.xs.SingEngine;
import com.xs.utils.AiUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class PlayActivity extends AppCompatActivity implements View.OnClickListener {

    EditText edit;
    Button bt, button_play;
    TextView tv;

    Boolean running = false;
    Boolean playing = false;

    SingEngine engine;

    ProgressDialog mProgressDialog;

    String mCurrentTokenId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        edit = (EditText) findViewById(R.id.et);
        bt = (Button) findViewById(R.id.button);
        tv = (TextView) findViewById(R.id.tv);
        button_play = (Button) findViewById(R.id.button_play);

        tv.setMovementMethod(ScrollingMovementMethod.getInstance());


        bt.setText("开始录音");
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("正在加载...");


        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/lingoes.ttf");
        tv.setTypeface(face);
        //兼容6.0权限管理
        if (ContextCompat.checkSelfPermission(PlayActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(PlayActivity.this, new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        } else {
            initSingEnge();
        }

        bt.setOnClickListener(this);
        button_play.setOnClickListener(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        doNext(requestCode, grantResults);
    }

    private void doNext(int requestCode, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                initSingEnge();
            } else {
                // Permission Denied
            }
        }
    }

    private void initSingEnge() {
        mProgressDialog.show();
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    //  获取引擎实例,设置测评监听对象
                    engine = SingEngine.newInstance(PlayActivity.this);
                    engine.setListener(mResultListener);
                    //  设置引擎类型
                    engine.setServerType("auto");
                    //  设置是否开启VAD功能
//                    engine.setOpenVad(false, null);
////                    engine.setOpenVad(true, "vad.0.1.bin");
////                    engine.setFrontVadTime(10000);
////                    engine.setBackVadTime(200000);
//                    //设置离线资源
                    engine.setNativeZip("resources.0.6.zip");
//                    engine.setLogPath(Environment.getExternalStorageDirectory() + "/aaa");
                    //   构建引擎初始化参数
                    JSONObject cfg_init = engine.buildInitJson("1459219202000002", "3bc23f814868ebd5b61a71acde532abb");
                    //   设置引擎初始化参数
                    engine.setNewCfg(cfg_init);
                    //   引擎初始化
                    engine.newEngine();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();


    }

    private void start() {
        bt.setText("停止录音");
        String retext = edit.getText().toString().trim();


        engine.setWavPath(AiUtil.getFilesDir(
                this).getPath()
                + "/record2");

        if (engine != null) {
            try {
                JSONObject request = new JSONObject();

                if (retext.contains(" ")) {
                    request.put("coreType", "en.sent.score");
                } else {
                    request.put("coreType", "en.word.score");
                }
                request.put("attachAudioUrl", 1);
                request.put("refText", retext);
                request.put("rank", 100);
                //构建评测请求参数
                JSONObject startCfg = engine.buildStartJson("guest", request);
                //设置评测请求参数
                engine.setStartCfg(startCfg);
//                //path
//                engine.setWavPath(AiUtil.getFilesDir(
//                        this.getApplicationContext()).getPath()
//                        + "/record/haha.wav");
                //开始测评
                engine.start();
                running = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void stop() {
        bt.setText("开始录音");
        if (engine != null) {
            engine.stop();
            running = false;
        }
    }

    private void cancel() {
        if (engine != null) {
            engine.cancel();
            running = false;
        }
    }


//    private void playBack() {
//        if (engine != null) {
//            engine.playback();
//        }
//        playing = true;
//    }


    private void playBackByTokenid(String tokenid) {
        if (engine != null) {
            engine.playback(tokenid);
        }
        playing = true;
    }

    private void stopPlayBack() {
        if (engine != null) {
            engine.stopPlayBack();
        }
        playing = false;

    }

    SingEngine.ResultListener mResultListener = new SingEngine.ResultListener() {
        @Override
        public void onBegin() {
        }

        @Override
        public void onResult(final JSONObject result) {
            Log.w("Main--->", "-----onResult()-----" + result.toString());


            try {
                mCurrentTokenId = result.getString("tokenId");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    tv.setText(result.toString());
                }
            });
        }

        @Override
        public void onEnd(int Code, String msg) {
            Log.w("Main--->", "-----onEnd()-----" + Code);
        }

        @Override
        public void onUpdateVolume(int volume) {
            Log.e("--onUpdateVolume----", volume + "");
        }

        @Override
        public void onFrontVadTimeOut() {
            Log.e("--onFrontVadTimeOut----", "前置超时");

            running = false;
        }

        @Override
        public void onBackVadTimeOut() {
            Log.e("--onBackVadTimeOut----", "后置超时");

            running = false;
        }

        @Override
        public void onRecordingBuffer(byte[] data) {
        }

        @Override
        public void onRecordLengthOut() {
            Log.e("--onRecordLengthOut----", "录音超时");
            stop();
        }

        @Override
        public void onReady() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.dismiss();
                }
            });
            Log.e("------onReady-------", "onReady()");
        }

        @Override
        public void onPlayCompeleted() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    playing = false;
                    Toast.makeText(PlayActivity.this, "compelete", Toast.LENGTH_SHORT).show();
                }
            });

        }
    };

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.button:
                if (!running) {
                    start();
                } else {
                    stop();
                }
                break;

            case R.id.button_play:
                if (!playing) {
                    playBackByTokenid(mCurrentTokenId);
                    Log.e("------", "playback");

                } else {
                    stopPlayBack();
                    Log.e("--------", "stopPlayBack");
                }
                break;
        }

    }

}