package com.xs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.xs.res.NativeResource;
import com.xs.utils.AiUtil;
import com.xs.utils.NetWorkUtil;
import com.tt.SSound;
import com.xs.record.StreamAudioPlayer;
import com.xs.record.StreamAudioRecorder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wang on 2016/11/29.
 */

public class SingEngine {

    /**
     * 回调接口
     */
    public interface ResultListener {
        /**
         * 录音开始时回调
         */
        void onBegin();

        /**
         * 测评结果返回
         *
         * @param result 测评结果json对象
         */
        void onResult(JSONObject result);

        /**
         * 结束时回调
         *
         * @param Code 错误代码，等于0时为正常结束
         * @param msg  错误消息
         */
        void onEnd(int Code, String msg);

        /**
         * 麦克风音量回调
         *
         * @param volume 音量
         */
        void onUpdateVolume(final int volume);

        /**
         * vad 前置端点超时回调
         */
        void onFrontVadTimeOut();

        /**
         * vad 后置端点超时回调
         */
        void onBackVadTimeOut();

        /**
         * 录音数据回调
         *
         * @param data 录音数据
         */
        void onRecordingBuffer(byte[] data);


        /**
         * 录音长度过长，监听录音长度超时
         */
        void onRecordLengthOut();

        /**
         * 引擎初始化成功
         */
        void onReady();

        /**
         * 录音播放完成监听
         */
        void onPlayCompeleted();

    }

    /**
     * 引擎内核类型
     */
    public enum coreProvideType {
        CLOUD("cloud"), NATIVE("native"), AUTO("auto");
        private String value;

        coreProvideType(String value) {
            this.setValue(value);
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * 评测题型类型
     */
    public enum coreType {
        enWord("en.word.score"),
        enSent("en.sent.score");

        private String value;

        coreType(String value) {
            this.setValue(value);
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private Context ct;
    private long engine = 0;
    private ResultListener caller;

    private StreamAudioRecorder mStreamAudioRecorder = null;
    private StreamAudioPlayer mStreamAudioPlayer = null;

    private JSONObject newCfg = null;
    private JSONObject startCfg = null;
    private String local = null;
    private String avdLocalPath = null;
    private boolean useVad = false;
    private boolean needCheckResource = false;
    private String wavPath, mLastRecordPath;
    private coreProvideType cpt = coreProvideType.CLOUD;

    private long frontVadTime = 2000;
    private long backVadTime = 2000;
    private long logEnable = 1;  //开启log   0为关闭;1为开启
    private long defaultServerTimeout = 60; //引擎超时时间  单位秒

    private String mAudioType = "wav";
    private long mSampleRate = 16000;


    // 用来规避 vadTimeOut多次回调 和 60011 Interface calls in the wrong order
    private boolean mNeedFeedData = true;

    // 用于离线结果上传
    private String mResultTag;

    //用于美联录音超时自动stop处理
    private long mStartRecordTimeStamp, mStopRecordTimeStamp;

    private SingEngine(Context context) {
        ct = context.getApplicationContext();
    }

    /**
     * 实例化引擎，添加评测监听
     */
    public static SingEngine newInstance(Context context) {
        SingEngine singEngine = new SingEngine(context);
        singEngine.setWavPath(AiUtil.getFilesDir(
                context.getApplicationContext()).getPath()
                + "/record/");
        return singEngine;
    }

    /**
     * 设置测评结果监听器
     */
    public void setListener(ResultListener listenner) {
        caller = listenner;
    }

    /**
     * 设置引擎类型
     */
    public void setServerType(String type) {
        if (type.equals(coreProvideType.AUTO.getValue())) {
            cpt = coreProvideType.AUTO;
        } else if (type.equals(coreProvideType.NATIVE.getValue())) {
            cpt = coreProvideType.NATIVE;
        } else {
            cpt = coreProvideType.CLOUD;
        }
    }

    /**
     * vad功能开关
     */
    public void setOpenVad(boolean b, String vadResourcename) {
        this.useVad = b;
        if (b) {
            NativeResource.vadResourceName = vadResourcename;
        }
    }

    /**
     * vad前置端点时间 ，超时后自动取消评测
     */
    public void setFrontVadTime(long frontVadTime) {
        this.frontVadTime = frontVadTime;
    }

    /**
     * vad后置端点时间 ，超时后自动返回评测结果
     */
    public void setBackVadTime(long backVadTime) {
        this.backVadTime = backVadTime;
    }

    /**
     * 设置离线资源包名称
     */
    public void setNativeZip(String resourceName) {
        NativeResource.zipResourceName = resourceName;
    }

    /**
     * 初始化时,资源检查功能
     */
    public void setOpenCheckResource(boolean checkResource) {
        this.needCheckResource = checkResource;
    }

    /**
     * 设置log上传日志
     */
    public void setLogEnable(long logEnable) {
        this.logEnable = logEnable;
    }

    /**
     * 设置引擎超时时间
     */
    public void setServerTimeout(long serverTimeout) {
        this.defaultServerTimeout = serverTimeout;
    }

    /**
     * 设置录音路径（可为 全路径+文件名 或者 文件目录）
     *
     * @param wavPath
     */
    public void setWavPath(String wavPath) {
        this.wavPath = wavPath;
    }

    /**
     * 获取音频地址
     */
    public String getWavPath() {
        return wavPath;
    }

    /**
     * 获取sdk版本号
     */
    public String getVersion() {
        return "1.4.0";
    }

//-----------------初始化---------------------------------------------------------------------------------

    /**
     * 构建引擎初始化参数
     */
    public JSONObject buildInitJson(String appKey, String secretKey) throws JSONException {

        if (appKey == null || secretKey == null) {
            caller.onEnd(60000, "please check your appKey,secretKey");
        }
        JSONObject cfg = new JSONObject();
        cfg.put("appKey", appKey).put("secretKey", secretKey).put("logEnable", logEnable);
        return cfg;
    }

    /**
     * 设置引擎初始化参数
     */
    public void setNewCfg(JSONObject cfg) {
        newCfg = cfg;
    }

    /**
     * 引擎初始化
     */
    public void newEngine() throws JSONException {
        buildEngineJson();
        buildAvdInitJson();

        log("NewCfg" + newCfg.toString());
        engine = SSound.ssound_new(newCfg.toString(), ct);
        if (engine == 0) {
            caller.onEnd(60001, " init fail, please check param");
            return;
        }

        mStreamAudioRecorder = StreamAudioRecorder.getInstance();
        mStreamAudioPlayer = StreamAudioPlayer.getInstance();
        caller.onReady();

    }

    /**
     * 构建评测参数
     */
    public JSONObject buildStartJson(String UserId, JSONObject request) throws JSONException {

        if (UserId == null) {
            UserId = "guest";
        }

        JSONObject cfg = new JSONObject();
        JSONObject audio = new JSONObject();
        String UID = "{\"userId\":" + "\"" + UserId.trim() + "\"}";

        audio.put("audioType", mAudioType)
                .put("sampleRate", mSampleRate)
                .put("sampleBytes", 2)
                .put("channel", 1);

        cfg.put("coreProvideType", cpt.getValue())
                .put("soundIntensityEnable", 1)
                .put("app", new JSONObject(UID))
                .put("audio", audio)
                .put("request", request);

        return cfg;
    }

    /**
     * 设置评测参数
     */
    public void setStartCfg(JSONObject cfg) throws JSONException {
        startCfg = cfg;
    }


    /**
     * 开始评测
     */
    public void start() {
        try {

            int r;
            String recordPath;
            mNeedFeedData = true;

            buildAvdStartJson();
            selectServerTypeWhenAuto();
            log("StartCfg" + startCfg.toString());

            if (this.wavPath.contains(".pcm") || this.wavPath.contains(".wav")) {
                recordPath = this.wavPath;
            } else {
                recordPath = this.getRecordFilePath((String.valueOf(System.currentTimeMillis())).trim());
            }
            mLastRecordPath = recordPath;

            r = mStreamAudioRecorder.start(recordPath, new StreamAudioRecorder.AudioStartCompeletedCallback() {
                @Override
                public void onAudioStartCompeleted() {
                    mStartRecordTimeStamp = System.currentTimeMillis();
                    byte[] rid = new byte[64];
                    SingEngine.this.SSoundStart(rid);
                }
            }, new StreamAudioRecorder.AudioDataCallback() {
                @Override
                public void onAudioData(byte[] data, int size) {
                    // 处理cancel 或 stop 后多feed一次，导致60011
                    if (!mNeedFeedData) return;
                    if (size <= 0) return;

                    if (mResultTag != null && mResultTag.equals(coreProvideType.CLOUD.getValue())) {
                        String coreTypeString = startCfg.optJSONObject("request").optString("coreType");
                        mStopRecordTimeStamp = System.currentTimeMillis();
                        long recordTime = mStopRecordTimeStamp - mStartRecordTimeStamp;
                        //word
                        if (coreTypeString != null && (coreTypeString.equals(coreType.enWord.getValue()))) {

                            if (recordTime >= 18000) {
                                log("record timeout : word");
                                caller.onRecordLengthOut();
                            }

                            SsoundFeed(data, size);

                        }
                        //sent
                        else if (coreTypeString != null && (coreTypeString.equals(coreType.enSent.getValue()))) {

                            if (recordTime >= 38000) {
                                log("record timeout : sent");
                                caller.onRecordLengthOut();
                            }

                            SsoundFeed(data, size);

                        } else {
                            SsoundFeed(data, size);
                        }

                    } else {
                        SsoundFeed(data, size);
                    }
                }

                @Override
                public void onError() {
                    stop();
                }
            });
            if (r != 0) {
                caller.onEnd(70004, "StreamAudioRecorder start error");
            }

            caller.onBegin();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 结束测评
     */
    public void stop() {
        int r;
        r = mStreamAudioRecorder.stop();
        if (r != 0) {
            caller.onEnd(70005, "StreamAudioRecorder stop error");
        }

        r = SSound.ssound_stop(engine);
        if (r != 0) {
            caller.onEnd(70002, "engine stop error");
        }
        mNeedFeedData = false;
    }

    /**
     * 取消评测
     */
    public void cancel() {
        int r;
        r = mStreamAudioRecorder.stop();
        if (r != 0) {
            caller.onEnd(70005, "StreamAudioRecorder stop error");
        }

        r = SSound.ssound_cancel(engine);

        if (r != 0) {
            caller.onEnd(70003, "cancel error");
        }
        mNeedFeedData = false;
    }

    /**
     * 销毁引擎
     */
    public void delete() {
        int r;
        r = SSound.ssound_delete(engine);
        if (r != 0) caller.onEnd(70010, "delete error");
    }

    /**
     * 根据文件path回放录音
     */
    public void playback(String recordPath) {
        mStreamAudioPlayer.play(recordPath, new StreamAudioPlayer.AudioPlayCompeletedCallback() {
            @Override
            public void onAudioPlayCompeleted() {
                caller.onPlayCompeleted();
            }
        });
    }

    /**
     * 回放最近文件（用户传的是完整路径）
     */
    public void playback() {
        if (mLastRecordPath != null && (mLastRecordPath.contains(".pcm") || mLastRecordPath.contains(".wav"))) {
            mStreamAudioPlayer.play(mLastRecordPath, new StreamAudioPlayer.AudioPlayCompeletedCallback() {
                @Override
                public void onAudioPlayCompeleted() {
                    caller.onPlayCompeleted();
                }
            });
        }
    }

    public void stopPlayBack() {
        mStreamAudioPlayer.stopPlay();
    }

    /**
     * 调用ssound_feed，并更新测评音频数据
     */
    private void SsoundFeed(byte[] data, int size) {
        if (!mNeedFeedData) return;
        caller.onRecordingBuffer(data);
        int rr = SSound.ssound_feed(engine, data, size);
        if (rr != 0) {
            stop();
        }
    }

    /**
     * 调用ssound_start，并返回测评结果
     */
    private int SSoundStart(byte[] rid) {
        try {
            int r = SSound.ssound_start(engine, startCfg.toString(), rid, new SSound.ssound_callback() {
                @Override
                public int run(byte[] id, final int type, final byte[] data, final int size) {
                    //  run on CallBack Thread，can‘t do blocking work
                    if (type == SSound.SSOUND_MESSAGE_TYPE_JSON) {

                        final String result = new String(data, 0, size).trim();

                        if (result.isEmpty()) {
                            SSound.ssound_log(engine, " empty result：" + result);
                        }


                        try {
                            final JSONObject json = new JSONObject(result);
                            if (json.has("errId")) {
                                //返回error message
                                caller.onEnd(json.getInt("errId"), json.getString("error"));
                                if (mNeedFeedData && mStreamAudioRecorder.mIsRecording.get()) {
                                    stop();
                                }
                            } else if (json.has("vad_status") || json.has("sound_intensity")) {
                                if (mNeedFeedData) {
                                    int vadCode = json.optInt("vad_status");
                                    int volume = json.optInt("sound_intensity");
                                    caller.onUpdateVolume(volume);

                                    if (vadCode == 2 && mStreamAudioRecorder.mIsRecording.get()) {
                                        stop(); //自动stop
                                        caller.onBackVadTimeOut();    //后vad时间超时
                                    }

                                    if (vadCode == 3 && mStreamAudioRecorder.mIsRecording.get()) {
                                        caller.onFrontVadTimeOut();    //前vad时间超时

                                    }
                                }
                            } else {
                                //返回测评结果json
                                caller.onEnd(0, "success");
                                caller.onResult(json);

                                if (mResultTag != null && mResultTag.equals(coreProvideType.NATIVE.getValue())) {
                                    //UPLOAD NATIVE RESULT
                                    SSound.ssound_log(engine, json.toString());
                                }

                            }


                        } catch (JSONException e) {
                            caller.onEnd(70001, "server result string error");
                            SSound.ssound_log(engine, "Error resault can't covert to json: " + result.toString());
                            e.printStackTrace();
                        }
                    }
                    return 0;
                }
            }, ct);

            if (r != 0) {
                stop();
            }

            return r;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


//------BUILD-----------JSON--------------------------------

    private void buildEngineJson() throws JSONException {

        if (cpt == coreProvideType.AUTO) {
            addCloudInitJson();
            addNativeInitJson();
            return;
        }

        if (cpt == coreProvideType.CLOUD) {
            addCloudInitJson();
            return;
        }

        if (cpt == coreProvideType.NATIVE) {
            addNativeInitJson();
        }

    }

    private void addNativeInitJson() throws JSONException {
        newCfg.put("native", buildNativePath());
    }

    private void addCloudInitJson() throws JSONException {
        JSONObject cloud = new JSONObject();
        String serverAPI = "ws://api.cloud.ssapi.cn:8080";
        String testAPI = "ws://139.196.138.232:8090";
        cloud.put("enable", 1)
                .put("server", serverAPI)
                .put("connectTimeout", 20)
                .put("serverTimeout", defaultServerTimeout);
        newCfg.put("cloud", cloud);
    }

    private JSONObject buildNativePath() throws JSONException {

        if (needCheckResource) {
            local = AiUtil.unzipFile(ct, NativeResource.zipResourceName).toString();
        } else {
            if (local == null) {
                local = AiUtil.unzipFile(ct, NativeResource.zipResourceName).toString();
            }
        }
        String res_path = String.format(NativeResource.native_zip_res_path, local, local);
        return new JSONObject(res_path);
    }

    private void buildAvdInitJson() throws JSONException {
        if (useVad) {
            JSONObject vad = new JSONObject();
            vad.put("enable", 1);
            vad.put("res", buildAvdPath());
            vad.put("maxBeginSil", frontVadTime / 30);
            vad.put("rightMargin", backVadTime / 200);
            newCfg.put("vad", vad);
        }
    }

    private String buildAvdPath() throws JSONException {

        if (needCheckResource) {
            avdLocalPath = AiUtil.getFilePathFromAssets(ct, NativeResource.vadResourceName);
        } else {
            if (avdLocalPath == null) {
                avdLocalPath = AiUtil.getFilePathFromAssets(ct, NativeResource.vadResourceName);
            }
        }

        return avdLocalPath;
    }

    private void buildAvdStartJson() {
        try {
            // vad仅支持单词句子
            if (useVad) {
                String coreTypeString = startCfg.optJSONObject("request").optString("coreType");
                if (coreTypeString != null && (coreTypeString.equals(coreType.enWord.getValue())
                        || coreTypeString.equals(coreType.enSent.getValue()))) {
                    JSONObject vad = new JSONObject();
                    vad.put("vadEnable", 1);
                    startCfg.put("vad", vad);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置Auto模式时动态切换引擎Type ， 有网cloud ，没网native
     */
    private void selectServerTypeWhenAuto() {
        try {

            if (cpt == coreProvideType.CLOUD) {
                mResultTag = coreProvideType.CLOUD.getValue();
                return;
            }

            if (cpt == coreProvideType.NATIVE) {
                mResultTag = coreProvideType.NATIVE.getValue();
                return;
            }

            if (!NetWorkUtil.getInstance().isConnected(ct)) {
                startCfg.put("coreProvideType", coreProvideType.NATIVE.getValue());
                mResultTag = coreProvideType.NATIVE.getValue();

            } else {
                startCfg.put("coreProvideType", coreProvideType.CLOUD.getValue());
                mResultTag = coreProvideType.CLOUD.getValue();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 组拼文件路径名
     */
    @NonNull
    private String getRecordFilePath(String filename) {
        return wavPath + filename + ".pcm";
    }


//    /**
//     * 离线WAV 文件评测
//     */
//    public void startWithPCM(String wavName) {
//        byte[] rid = new byte[64];
//        if (SSoundStart(rid) != 0) return;
//        int bytes, rv;
//        byte[] buf = new byte[1024];
//
//        InputStream fis;
//        try {
//            fis = ct.getAssets().open(wavName);
//            while ((bytes = fis.read(buf, 0, 1024)) > 0) {
//                if ((rv = SSound.ssound_feed(engine, buf, bytes)) != 0) {
//                    break;
//                }
//            }
//
//            fis.close();
//        } catch (IOException e) {
//            caller.onEnd(70011, "feed audio data fail");
//        } finally {
//        }
//
//        stop();
//
//    }

    /**
     * 离线WAV 文件评测
     */
    public void startWithPCM(String filePath) {
        try {
            if (selectAudioTypeConfig(filePath)) return;

            byte[] rid = new byte[64];
            if (SSoundStart(rid) != 0) return;
            int bytes, rv;
            byte[] buf = new byte[1024];

            InputStream fis;
            try {
                fis = new FileInputStream(filePath);
                while ((bytes = fis.read(buf, 0, 1024)) > 0) {
                    if ((rv = SSound.ssound_feed(engine, buf, bytes)) != 0) {
                        break;
                    }
                }

                fis.close();
            } catch (IOException e) {
                caller.onEnd(70011, "feed audio data fail");
            } finally {
            }

            stop();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean selectAudioTypeConfig(String filePath) throws JSONException {
        if (TextUtils.isEmpty(filePath)) return true;


        if (filePath.endsWith("WAV") || filePath.endsWith("wav")) {
            mAudioType = "wav";
            mSampleRate = 16000;
        }

        if (filePath.endsWith("MP3") || filePath.endsWith("mp3")) {
            mAudioType = "mp3";
            mSampleRate = 44100;
        }

        JSONObject audio = new JSONObject();
        audio.put("audioType", mAudioType)
                .put("sampleRate", mSampleRate)
                .put("sampleBytes", 2)
                .put("channel", 1);
        startCfg.put("audio", audio);
        return false;
    }


//    /**
//     * 离线 MP3 文件评测
//     */
//    public void startWithLocalMP3(String MP3Name) {
//        byte[] rid = new byte[64];
//        if (SSoundStart(rid) != 0) return;
//
//        InputStream fis;
//        try {
//            fis = new FileInputStream(MP3Name);
//            int length = fis.available();
//            byte[] buf_mp3 = new byte[length];
//            fis.read(buf_mp3, 0, length);
//            SSound.ssound_offline(engine, buf_mp3, length);
//            fis.close();
//        } catch (IOException e) {
//            caller.onEnd(70011, "offine mp3 audio data fail");
//        } finally {
//        }
//
//        stop();
//
//    }

    private void log(String s) {
        Log.d("SingEngine", s);
    }

}
