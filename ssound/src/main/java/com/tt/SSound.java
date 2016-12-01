package com.tt;

public final class SSound {

    static {
        System.loadLibrary("ssound");
    }

    /**
     * 引擎回调接口
     */
    public interface ssound_callback {
        /**
         * 抽象函数，调用时需重写函数实体
         * @param id 返回id
         * @param type 返回类型
         * @param data 返回数据
         * @param size 数据大小
         * @return 返回值
         */
        public abstract int run(byte[] id, int type, byte[] data, int size);
    }

    /** 回调返回消息类型 - json */
    public static int SSOUND_MESSAGE_TYPE_JSON = 1;
    /** 回调返回消息类型 - bin */
    public static int SSOUND_MESSAGE_TYPE_BIN = 2;

    public static int SSOUND_OPT_GET_VERSION = 1;
    public static int SSOUND_OPT_GET_MODULES = 2;
    public static int SSOUND_OPT_GET_TRAFFIC = 3;
    public static int SSOUND_OPT_SET_WIFI_STATUS = 4;
    public static int SSOUND_OPT_GET_PROVISION = 5;
    public static int SSOUND_OPT_GET_SERIAL_NUMBER = 6;


    /**
     * 创建引擎
     * @param cfg 配置信息
     * @param androidContext Context对象
     * @return 0：失败
     *    引擎对象值：成功
     */

    public static native long ssound_new(String cfg, Object androidContext);
    /**
     * 移除引擎
     * @param engine 引擎对象值
     * @return 返回值
     */
    public static native int ssound_delete(long engine);

    /**
     * 启动引擎
     * @param engine 引擎对象值
     * @param param 参数配置字符串
     * @param id 存储引擎生成的id字符
     * @param callback ssound_callback对象
     * @param androidContext Context对象
     * @return 返回值
     */
    public static native int ssound_start(long engine, String param, byte[] id, ssound_callback callback, Object context);

    /**
     * 向引擎缓冲区填入语音数据
     * @param engine 引擎对象值
     * @param data 数据
     * @param size 数据大小
     * @return 返回值
     */
    public static native int ssound_feed(long engine, byte[] data, int size);


    /**
     * 离线评测一个音频（目前仅支持16k 16bit单通道mp3文件）
     * @param engine 引擎对象值
     * @param data 数据
     * @param size 数据大小(音频总尺寸)
     * @return 返回值
     */
    public static native int ssound_offline(long engine, byte[] data, int size);

    /**
     * 停止引擎
     * @param engine 引擎对象值
     * @return 返回值
     */
    public static native int ssound_stop(long engine);

    /**
     * 取消引擎, 用于出现异常情况下，用户可以取消引擎
     * @param engine 引擎对象值
     * @return 返回值
     */
    public static native int ssound_cancel(long engine);
    public static native int ssound_log(long engine, String log);
    public static native int ssound_opt(long engine, int opt, byte[] data, int size);

    /**
     * 获取设备ID
     * @param device_id 存储id的缓冲byte[]空间
     * @param androidContext Context对象
     * @return 返回值
     */
    public static native int ssound_get_device_id(byte[] device_id, Object androidContext);
}
