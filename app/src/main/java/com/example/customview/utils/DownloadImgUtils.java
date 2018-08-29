package com.example.customview.utils;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * date: 2018/8/28
 * time: 下午4:08
 * user: jackxu
 */
public class DownloadImgUtils {

    public static Bitmap downloadImageByUrl(String imgUrl, ImageView imageView) {

        if (null == imgUrl) return null;
        try {
            URL url = new URL(imgUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            InputStream is = new BufferedInputStream(httpConn.getInputStream());
            is.mark(is.available()); // 在InputStream中设置一个标记位置.
            // 参数readlimit 表示多少字节可以读取.
            // 调用reset()将重新流回到标记的位置
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            // 获取imageview想要显示的宽和高
            ImageSizeUtil.ImageSize imageSize = ImageSizeUtil.getImageViewSize(imageView);
            options.inSampleSize = ImageSizeUtil.caculateInSampleSize(options, imageSize.width,
                    imageSize.height);
            options.inJustDecodeBounds = false;
            is.reset();  // Resets this stream to the last marked location.
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            httpConn.disconnect();
            is.close();
            return bitmap;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 根据url下载图片在指定的文件
     */
    public static boolean downloadImageByUrl(String urlStr, File file) {
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            is = conn.getInputStream();
            fos = new FileOutputStream(file);
            byte[] buf = new byte[512];
            int len = 0;
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            fos.flush();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
            }

            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


}
