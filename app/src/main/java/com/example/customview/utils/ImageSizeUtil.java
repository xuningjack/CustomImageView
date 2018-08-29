package com.example.customview.utils;

import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;


/**
 * date: 2018/8/28
 * time: 下午4:07
 * user: jackxu
 */
public class ImageSizeUtil {

    public static class ImageSize {
        int width;
        int height;
    }

    /**
     * 获得imageview想要显示的大小
     *
     * 可以看到，我们拿到imageview以后：
     * 首先企图通过getWidth获取显示的宽；有些时候，这个getWidth返回的是0；
     * 那么我们再去看看它有没有在布局文件中书写宽；
     * 如果布局文件中也没有精确值，那么我们再去看看它有没有设置最大值；
     * 如果最大值也没设置，那么我们只有拿出我们的终极方案，使用我们的屏幕宽度；
     * 总之，不能让它任性，我们一定要拿到一个合适的显示值。
     * 可以看到这里或者最大宽度，我们用的反射，而不是getMaxWidth()；
     * 维萨呢，因为getMaxWidth竟然要API 16，我也是醉了；为了兼容性，我们采用反射的方案。反射的代码就不贴了。
     * @param imageView
     * @return imageView的大小
     */
    public static ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();

        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        int width = imageView.getWidth();
        if(width <= 0) width = layoutParams.width;
        // if(width <= 0) width = imageView.getMaxWidth();
        if(width <= 0) width = displayMetrics.widthPixels;

        int height = imageView.getHeight();
        if(height <= 0) height = layoutParams.height;
        // if(height <= 0) height = imageView.getMaxHeight();
        if(height <= 0) height = displayMetrics.heightPixels;

        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    /**
     * 计算BitmapFactory.Options options 中的 inSampleSize, 加载图片的大小的重要参数
     * 根据需求的宽和高以及图片实际的宽和高计算inSampleSize
     *     1. 如果 inSampleSize > 1,返回一个较小的图像保存在内存中.
     *           例如，insamplesize = = 4返回一个图像是1 / 4的宽度/高度的图像
     *     2. 如果 inSampleSize <= 1, 返回原始图像
     * @param options 保存着实际图片的大小
     * @param reqWidth 压缩后图片的 width
     * @param reqHeight 压缩后图片的 height
     * @return options里面存了实际的宽和高；reqWidth和reqHeight就是我们之前得到的想要显示的大小；
     * 经过比较，得到一个合适的inSampleSize;
     */
    public static int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        int width = options.outWidth, height=options.outHeight;

        if(width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round(width/reqWidth);
            int heightRadio = Math.round(height/reqHeight);
            inSampleSize = Math.max(widthRadio, heightRadio);
        }

        return inSampleSize;
    }

}