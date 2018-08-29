package com.example.customview.utils;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import com.example.customview.view.CustomImageView;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;





/**
 * date: 2018/8/28
 * time: 下午4:08
 * user: jackxu
 */
public class MyImageLoader {

    public static final String TAG = "MyImageLoader";

    private static MyImageLoader mInstance;
    private LruCache<String, Bitmap> mLruCache;  // 图片缓存的核心对象
    private ExecutorService mThreadPool;  // 线程池
    private static final int DEFAULT_THREAD_COUNT = 1;

    private LinkedList<Runnable> mTaskQueue;  // 任务队列

    private Thread mBackstageThread;  // 后台轮询线程
    private Handler mBackstageThreadHandler;

    // Semaphore, 它负责协调各个线程, 以保证它们能够正确、合理的使用公共资源。
    // 也是操作系统中用于控制进程同步互斥的量。
    // Semaphore分为单值和多值两种，前者只能被一个线程获得，后者可以被若干个线程获得。
    // 停车场系统中，车位是公共资源，每辆车好比一个线程，看门人起的就是信号量的作用
    private Semaphore mBackstageThreadSemaphore;
    private Semaphore mBackstageThreadHandlerSemaphore = new Semaphore(0);

    private static final Object syncObject = new Object();  // 单例模式 && synchronized

    public enum QueueType {FIFO, LIFO}

    private QueueType mType = QueueType.LIFO;

    private boolean isDiskCacheEnable = true;  // 硬盘缓存可用

    // UI Thread
    private Handler mUIHandler;


    public static MyImageLoader getInstance() {
        if (mInstance == null) {
            mInstance = new MyImageLoader(DEFAULT_THREAD_COUNT, QueueType.LIFO);
        }
        return mInstance;
    }

    private MyImageLoader(int threadCount, QueueType type) {
        init(threadCount, type);
    }

    private void init(int threadCount, QueueType type) {
        initBackThread();

        // get the max available memory
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;

        // 继承LruCache时，必须要复写sizeof方法，用于计算每个条目的大小
        // the size of bitmap can not over cacheMemory
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };

        // create thread pool that
        // reuses a fixed number of threads operating off a shared unbounded queue.
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<>();
        mType = type;
        mBackstageThreadSemaphore = new Semaphore(threadCount);
    }

    /**
     * 初始化后台轮询线程
     */
    private void initBackThread() {
        mBackstageThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mBackstageThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        mThreadPool.execute(getTask());  // 线程池去取出一个任务进行执行
                        try {
                            mBackstageThreadSemaphore.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                mBackstageThreadHandlerSemaphore.release();  // 释放一个信号量
                Looper.loop();
            }
        };
        mBackstageThread.start();
    }


    public interface ILoadingComplete{

        void onLoadFinished();
    }


    /**
     * 从网络加载图片
     * @param path
     * @param imageView
     * @param isFromNet
     */
    public void loadImage(String path, final CustomImageView imageView, boolean isFromNet) {
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    CustomImageView iv = holder.imageView;
                    String path = holder.path;
                    if (iv.getTag().toString().equals(path)) {
                        iv.setImageBitmap(bm);
                    }
                }
            };
        }

        Bitmap bitmap = getBitmapFromLruCache(path);  // 根据path在缓存中获取bitmap
        if (bitmap != null) {
            refreshBitmap(path, imageView, bitmap);
        } else {
            addTask(buildTask(path, imageView, isFromNet));
        }
    }


    /**
     * 加载圆角图片
     * @param path
     * @param imageView
     * @param isFromNet
     */
    public void loadCircleImage(String path, final CustomImageView imageView,
            boolean isFromNet, final ILoadingComplete iLoadingComplete) {
        imageView.setTag(path);
        mUIHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                Bitmap bm = holder.bitmap;
                CustomImageView iv = holder.imageView;
                String path = holder.path;
                if (iv.getTag().toString().equals(path)) {
                    iv.setImageBitmap(bm);
                }
                if(iLoadingComplete != null){
                    iLoadingComplete.onLoadFinished();
                }
            }
        };

        Bitmap bitmap = getBitmapFromLruCache(path);  // 根据path在缓存中获取bitmap
        if (bitmap != null) {
            refreshBitmap(path, imageView, bitmap);
        } else {
            addTask(buildTask(path, imageView, isFromNet));
        }
    }



    /**
     * 加载圆角图片
     * @param path
     * @param imageView
     * @param isFromNet
     */
    public void loadRoundImage(String path, final CustomImageView imageView,
            boolean isFromNet, final ILoadingComplete iLoadingComplete) {
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    CustomImageView iv = holder.imageView;
                    String path = holder.path;
                    if (iv.getTag().toString().equals(path)) {
                        iv.setImageBitmap(bm);
                    }
                    if(iLoadingComplete != null){
                        iLoadingComplete.onLoadFinished();
                    }
                }
            };
        }

        Bitmap bitmap = getBitmapFromLruCache(path);  // 根据path在缓存中获取bitmap
        if (bitmap != null) {
            refreshBitmap(path, imageView, bitmap);
        } else {
            addTask(buildTask(path, imageView, isFromNet));
        }
    }



    private void refreshBitmap(String path, final CustomImageView imageView, Bitmap bitmap) {
        Message msg = Message.obtain();
        ImageBeanHolder holder = new ImageBeanHolder();
        holder.imageView = imageView;
        holder.bitmap = bitmap;
        holder.path = path;
        msg.obj = holder;
        mUIHandler.sendMessage(msg);
    }

    /**
     * 就是runnable加入TaskQueue，与此同时使用mBackstageThreadHandler（这个handler还记得么，
     * 用于和我们后台线程交互。）去发送一个消息给后台线程，叫它去取出一个任务执行
     *
     * @param runnable
     */
    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mBackstageThreadHandler == null)
                mBackstageThreadHandlerSemaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBackstageThreadHandler.sendEmptyMessage(0x110); //
    }

    /**
     * 就是根据Type从任务队列头或者尾进行取任务
     *
     * @return
     */
    private Runnable getTask() {
        if (mType == QueueType.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == QueueType.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    /**
     * 我们新建任务，说明在内存中没有找到缓存的bitmap；我们的任务就是去根据path加载压缩后的bitmap返回即可，然后加入LruCache，设置回调显示。
     * 首先我们判断是否是网络任务？
     * 如果是，首先去硬盘缓存中找一下，（硬盘中文件名为：根据path生成的md5为名称）。
     * 如果硬盘缓存中没有，那么去判断是否开启了硬盘缓存：
     * 开启了的话：下载图片，使用loadImageFromLocal本地加载图片的方式进行加载（压缩的代码前面已经详细说过）；
     * 如果没有开启：则直接从网络获取（压缩获取的代码，前面详细说过）；
     * 如果不是网络图片：直接loadImageFromLocal本地加载图片的方式进行加载
     * 经过上面，就获得了bitmap；然后加入addBitmapToLruCache，refreashBitmap回调显示图片
     *
     * @param path
     * @param imageView
     * @param isFromNet
     * @return
     */
    private Runnable buildTask(final String path, final CustomImageView imageView, final boolean isFromNet) {
        return new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                if (isFromNet) {
                    File file = getDiskCacheDir(imageView.getContext(), md5(path));
                    if (file.exists()) { // 如果本地已经缓存了该文件
                        bitmap = loadImageFromLocal(file.getAbsolutePath(), imageView);
                        if (bitmap == null)
                            Log.d(TAG, "load image failed from local: " + path);
                    } else {  // 需要从网络下载
                        if (isDiskCacheEnable) { // 检测是否开启硬盘缓存
                            boolean downloadState = DownloadImgUtils.downloadImageByUrl(path, file);
                            if (downloadState) {
                                bitmap = loadImageFromLocal(file.getAbsolutePath(), imageView);
                            }
                            if (bitmap == null)
                                Log.d(TAG, "download image failed to diskcache(" + path + ")");
                        } else {  // 直接从网络加载到imageView
                            bitmap = DownloadImgUtils.downloadImageByUrl(path, imageView);
                            if (bitmap == null)
                                Log.d(TAG, "download image failed to memory(" + path + ")");
                        }
                    }
                } else {
                    bitmap = loadImageFromLocal(path, imageView);
                }
                addBitmapToLruCache(path, bitmap);
                refreshBitmap(path, imageView, bitmap);
                mBackstageThreadSemaphore.release();
            }
        };
    }

    /**
     * 使用loadImageFromLocal本地加载图片的方式进行加载
     *
     * @param path
     * @param imageView
     * @return
     */
    private Bitmap loadImageFromLocal(final String path, final CustomImageView imageView) {
        Bitmap bitmap = null;
        // 1、获得图片需要显示的大小
        ImageSizeUtil.ImageSize imageSize = ImageSizeUtil.getImageViewSize(imageView);
        // 2、压缩图片
        bitmap = decodeSampledBitmapFromPath(path, imageSize.width, imageSize.height);
        return bitmap;
    }

    /**
     * 将图片加入LruCache
     *
     * @param path
     * @param bitmap
     */
    protected void addBitmapToLruCache(String path, Bitmap bitmap) {
        if (getBitmapFromLruCache(path) == null) {
            if (bitmap != null)
                mLruCache.put(path, bitmap);
        }
    }

    /**
     * 根据图片需要显示的宽和高对图片进行压缩
     *
     * @param path
     * @param width
     * @param height
     * @return
     */
    protected Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        // 获得图片的宽和高，并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = ImageSizeUtil.caculateInSampleSize(options, width, height);
        // 使用获得到的InSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);

        if (null == bitmap)
            Log.d(TAG, "options.inSampleSize = " + options.inSampleSize + ", " + path);
        return bitmap;
    }

    /**
     * 获得缓存图片的地址
     *
     * @param context
     * @param uniqueName
     * @return
     */
    public File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * 根据path在缓存中获取bitmap
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    /**
     * 利用签名辅助类，将字符串字节数组
     *
     * @param str
     * @return
     */
    public String md5(String str) {
        byte[] digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            digest = md.digest(str.getBytes());
            return bytes2hex02(digest);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 方式二
     *
     * @param bytes
     * @return
     */
    public String bytes2hex02(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        String tmp = null;
        for (byte b : bytes) {
            // 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
            tmp = Integer.toHexString(0xFF & b);
            if (tmp.length() == 1){// 每个字节8为，转为16进制标志，2个16进制位
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }

        return sb.toString();

    }

    private class ImageBeanHolder {
        Bitmap bitmap;
        CustomImageView imageView;
        String path;
    }
}
