package com.tt;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogUtil {
    //8 美联   // 盒子鱼9
    private ExecutorService mExecutorService;
    private final String LOG_URL = "http://log.client.ssapi.cn:8080/bus?eid=9&est=9";

    private LogUtil() {
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    private static final class LogUtilHolder {
        private static final LogUtil INSTANCE = new LogUtil();
    }

    public static LogUtil getInstance() {
        return LogUtilHolder.INSTANCE;
    }

    public void resultLogUploadToLogServer(final String appkey, final String uid, final String result) {

        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    String newAppkey = appkey;
                    String newUid = uid;

                    if (newAppkey == null) {
                        newAppkey = "1459219202000001";
                    }

                    if (newUid == null) {
                        newUid = "guest";
                    }

                    // 请求的地址
                    String PostURL = LOG_URL + "&applicationId=" + newAppkey + "&uid=" + newUid;
                    // 根据地址创建URL对象
                    URL url = new URL(PostURL);
                    // 根据URL对象打开链接
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    // 设置请求的方式
                    urlConnection.setRequestMethod("POST");
                    // 设置请求的超时时间
                    urlConnection.setReadTimeout(5000);
                    urlConnection.setConnectTimeout(5000);
                    // 传递的数据
                    String data = "log=" + URLEncoder.encode(result, "UTF-8");
                    // 设置请求的头
                    urlConnection.setRequestProperty("Connection", "keep-alive");
                    // 设置请求的头
                    urlConnection.setRequestProperty("Content-Type",
                            "application/x-www-form-urlencoded");
                    // 设置请求的头
                    urlConnection.setRequestProperty("Content-Length",
                            String.valueOf(data.getBytes().length));
                    // 发送POST请求必须设置允许输出
                    urlConnection.setDoOutput(true);
                    // 发送POST请求必须设置允许输入
                    urlConnection.setDoInput(true);

                    //获取输出流
                    OutputStream os = urlConnection.getOutputStream();
                    os.write(data.getBytes());
                    os.flush();
                    if (urlConnection.getResponseCode() == 200) {
                        Log.d("LogUpLoad:", "suceess");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

}
