package com.example.customview.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;

import com.example.customview.R;
import com.example.customview.utils.MyImageLoader;


/**
 * 自定义View，实现圆角，圆形等效果
 *
 * @author jackxu
 */
public class CustomImageView extends ImageView {

    /**
     * TYPE_CIRCLE / TYPE_ROUND
     */
    private int mType;
    private static final int TYPE_CIRCLE = 0;
    private static final int TYPE_ROUND = 1;
    private static final int TYPE_URL = 2;
    private static final int TYPE_CIRCLE_URL = 3;
    private static final int TYPE_ROUND_URL = 4;

    /**
     * 图片
     */
    private Bitmap mSrc;

    /**
     * 圆角的大小
     */
    private int mRadius;

    /**
     * 控件的宽度
     */
    private int mWidth;
    /**
     * 控件的高度
     */
    private int mHeight;

    private String mUrlSrc;




    public CustomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomImageView(Context context) {
        this(context, null);
    }

    /**
     * 初始化一些自定义的参数
     */
    public CustomImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.CustomImageView, defStyle, 0);

        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.CustomImageView_src:
                    mSrc = BitmapFactory.decodeResource(getResources(), a.getResourceId(attr, 0));
                    break;
                case R.styleable.CustomImageView_type:
                    mType = a.getInt(attr, 0);// 默认为Circle
                    break;
                case R.styleable.CustomImageView_borderRadius:
                    mRadius = a.getDimensionPixelSize(attr,
                            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                    10f,
                                    getResources().getDisplayMetrics()));// 默认为10DP
                    break;
                case R.styleable.CustomImageView_url_src:
                    mType = Integer.parseInt(a.getString(R.styleable.CustomImageView_type));
                    mUrlSrc = a.getString(R.styleable.CustomImageView_url_src);
                    break;
                default:
                    break;
            }
        }
        a.recycle();
    }




    /**
     * 计算控件的高度和宽度
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        /**
         * 设置宽度
         */
        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);

        if (specMode == MeasureSpec.EXACTLY) {// match_parent , accurate
            mWidth = specSize;
        } else {
            // 由图片决定的宽
            int desireByImg = getPaddingLeft() + getPaddingRight() + mSrc.getWidth();
            if (specMode == MeasureSpec.AT_MOST) {// wrap_content
                mWidth = Math.min(desireByImg, specSize);
            } else {
                mWidth = desireByImg;
            }
        }

        //设置高度
        specMode = MeasureSpec.getMode(heightMeasureSpec);
        specSize = MeasureSpec.getSize(heightMeasureSpec);
        if (specMode == MeasureSpec.EXACTLY){   // match_parent , accurate
            mHeight = specSize;
        } else {
            int desire = getPaddingTop() + getPaddingBottom() + mSrc.getHeight();

            if (specMode == MeasureSpec.AT_MOST){// wrap_content
                mHeight = Math.min(desire, specSize);
            } else {
                mHeight = desire;
            }
        }
        setMeasuredDimension(mWidth, mHeight);
    }


    /**
     * 绘制
     */
    @Override
    protected void onDraw(final Canvas canvas) {
        MyImageLoader myImageLoader = null;
        Bitmap bitmap = null;
        int min = 0;
        final ImageView imageView = ((ImageView) this);
        switch (mType) {
            // 如果是TYPE_CIRCLE绘制圆形
            case TYPE_CIRCLE:
                min = Math.min(mWidth, mHeight);
                //长度如果不一致，按小的值进行压缩
                mSrc = Bitmap.createScaledBitmap(mSrc, min, min, false);
                canvas.drawBitmap(createCircleImage(mSrc, min), 0, 0, null);
                break;
            case TYPE_ROUND:
                canvas.drawBitmap(createRoundConnerImage(mSrc), 0, 0, null);
                break;
            case TYPE_URL:
                myImageLoader = MyImageLoader.getInstance();
                myImageLoader.loadImage(mUrlSrc, this, true);
                super.onDraw(canvas);   //必须调用super才会绘制图片
                break;
            case TYPE_CIRCLE_URL:
                myImageLoader = MyImageLoader.getInstance();
                myImageLoader.loadCircleImage(mUrlSrc, this, true,
                        new MyImageLoader.ILoadingComplete() {
                    @Override
                    public void onLoadFinished() {
                        int min = Math.min(mWidth, mHeight);
//                        if(imageView.getDrawable() != null){  // TODO: 2018/8/29 会crash
//                            Bitmap bitmap = getBitmap(imageView.getDrawable());
//                            canvas.drawBitmap(createCircleImage(bitmap, min), 0, 0, null);
//                        }
                    }
                });
                super.onDraw(canvas);
                break;
            case TYPE_ROUND_URL:
                myImageLoader = MyImageLoader.getInstance();
                myImageLoader.loadRoundImage(mUrlSrc, this, true,
                        new MyImageLoader.ILoadingComplete() {
                            @Override
                            public void onLoadFinished() {
//                                if(imageView.getDrawable() != null){  // TODO: 2018/8/29  会crash
//                                    Bitmap bitmap = getBitmap(imageView.getDrawable());
//                                    canvas.drawBitmap(createRoundConnerImage(mSrc), 0, 0, null);
//                                }
                            }
                        });
                super.onDraw(canvas);
                break;
            default:
                break;
        }
    }


    private Bitmap getBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        //canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }




    /**
     * 根据原图和变长绘制圆形图片
     */
    private Bitmap createCircleImage(Bitmap source, int min) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap target = Bitmap.createBitmap(min, min, Config.ARGB_8888);
        /**
         * 产生一个同样大小的画布
         */
        Canvas canvas = new Canvas(target);
        /**
         * 首先绘制圆形
         */
        canvas.drawCircle(min / 2, min / 2, min / 2, paint);
        /**
         * 使用SRC_IN，参考上面的说明
         */
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        /**
         * 绘制图片
         */
        canvas.drawBitmap(source, 0, 0, paint);
        return target;
    }

    /**
     * 根据原图添加圆角
     */
    private Bitmap createRoundConnerImage(Bitmap source) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap target = Bitmap.createBitmap(mWidth, mHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        RectF rect = new RectF(0, 0, source.getWidth(), source.getHeight());
        canvas.drawRoundRect(rect, mRadius, mRadius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, 0, 0, paint);
        return target;
    }



}