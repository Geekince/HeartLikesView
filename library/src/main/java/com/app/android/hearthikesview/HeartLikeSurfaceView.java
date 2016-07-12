package com.app.android.hearthikesview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by kince on 7/12/16.
 * 直播点赞动画
 * 使用SurfaceView实现
 */
public class HeartLikeSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private SurfaceHolder mHolder;
    private Thread mThread;// 当前线程
    private Canvas mCanvas;// 当前画布

    private Paint mPaint;

    private Bitmap mBitmap;// 当前显示的图像
    private ArrayList<Bitmap> totalBitmaps;// 一共有几种图像

    private List<PathObj> list = new ArrayList<PathObj>();// 路径

    private Canvas pagerCanvas;
    private Bitmap pagerBitmap;// 每次使用这个bitmap刷新

    private Random mRandom = new Random();

    public static int screenW, screenH;
    private boolean mFlag;//
    private long start_time = 0;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(android.os.Message msg) {
            int what = msg.what;
            switch (what) {
                case 0:
                    list.add(new PathObj(getWidth(), getHeight(), mBitmap));
                    break;

                default:
                    break;
            }
        }
    };

    public HeartLikeSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public HeartLikeSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HeartLikeSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HeartLikeSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFlag = false;
        mThread = null;
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        if (pagerBitmap != null && !pagerBitmap.isRecycled()) {
            pagerBitmap.recycle();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (pagerBitmap == null) {
            startThread();
            screenW = this.getWidth();
            screenH = this.getHeight();
            pagerBitmap = Bitmap.createBitmap(screenW, screenH, Bitmap.Config.ARGB_8888);
            pagerCanvas = new Canvas(pagerBitmap);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mFlag = false;
        list.clear();
    }

    @Override
    public void run() {
        while (mFlag) {
            try {
                synchronized (mHolder) {
                    drawView();
                }
            } catch (Exception e) {
            } finally {
            }
        }
    }

    private void init(Context context) {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setFormat(PixelFormat.TRANSLUCENT);

        mPaint = new Paint();
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        setFocusable(true);

        mBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.heart);
        totalBitmaps = new ArrayList<Bitmap>();
        totalBitmaps.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.heart));
        totalBitmaps.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.heart));

        setZOrderOnTop(true);
    }

    /**
     * 绘图图像
     */
    public void drawView() {
        try {
            mCanvas = mHolder.lockCanvas();
            if (mCanvas != null) {
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                pagerCanvas.drawPaint(mPaint);
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                pagerCanvas.save(Canvas.ALL_SAVE_FLAG);// 保存
                drawQpath(pagerCanvas);
                pagerCanvas.restore();// 存储
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                mCanvas.drawPaint(mPaint);
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                mCanvas.drawBitmap(pagerBitmap, 0, 0, null);
            }
        } catch (Exception e) {
        } finally {
            try {
                if (mCanvas != null)
                    mHolder.unlockCanvasAndPost(mCanvas);
            } catch (Exception e2) {
            }
        }
    }

    /**
     * 绘制贝赛尔曲线
     *
     * @param canvas 主画布
     */
    public void drawQpath(Canvas canvas) {
        if (list.size() <= 0) {
            mFlag = false;
        }
        for (int i = 0; i < list.size(); i++) {
            try {
                PathObj obj = list.get(i);
                if (obj.getAlpha() <= 0) {
                    list.remove(i);
                    i--;
                    continue;
                }
                Rect src = obj.getSrcRect();
                Rect dst = obj.getDstRect();
                if (dst == null) {
                    list.remove(i);
                    i--;
                    continue;
                }
                canvas.drawBitmap(obj.getBitmap(), src, dst, obj.getPaint());
            } catch (Exception e) {
                list.remove(i);
                i--;
            }
        }
    }

    /**
     * 点赞
     */
    public void click() {
        if (System.currentTimeMillis() - start_time > 50) {
            // 禁止同时显示多个赞
            start_time = System.currentTimeMillis();
            Bitmap bitmap = totalBitmaps.get(mRandom.nextInt(totalBitmaps.size()));
            list.add(new PathObj(getWidth(), getHeight(), bitmap));
            startThread();
        }
    }

    public void add(int count) {
        for (int i = 0; i < count; i++) {
            Message msg = Message.obtain();
            msg.what = 0;
            mHandler.sendMessageDelayed(msg, 80 * i);
        }
    }

    // 启动回话线程
    private void startThread() {
        if (!mFlag || mThread == null || !mThread.isAlive()) {
            mFlag = true;
            mThread = new Thread(this);
            mThread.setPriority(Thread.MAX_PRIORITY);
            mThread.start();
        }
    }

    /**
     * 随机路径
     */
    public class PathObj {
        private Paint paint;

        public Path path;
        public PathMeasure pathMeasure;

        public float speed;// 速度
        private final float acceleratedSpeed = 0.05f;// 加速度，目前恒定
        private final float speedMax = 6;

        public int curPos;
        public int length;// 路径总长度
        private int startX = 300, startY = 0, controlX, controlY, controlX2, controlY2, endX, endY;
        private float[] p = new float[2];// 坐标点，很重要

        private int time;// 执行的次数
        private int scaleTime = 20;// 放大

        private Rect src;
        private Rect dst;

        private Bitmap bitmap;

        private int bitmapWidth;// 图片宽度
        private int bitmapHeight;// 图片高度

        private int bitmapWidthDst;// 最终放大宽度
        private int bitmapHeightDst;// 最终放大高度

        private int alpha = 255;// 透明度范围 0-255
        private final int alphaOffset = 5;// 透明度偏移量

        /**
         * 初始化路径
         *
         * @param width  控件宽度
         * @param height 控件高度
         * @param bitmap 绘制的图片
         */
        public PathObj(int width, int height, Bitmap bitmap) {
            this.bitmap = bitmap;
            this.bitmapWidth = bitmap.getWidth();
            this.bitmapHeight = bitmap.getHeight();

            bitmapWidthDst = bitmapWidth;// + bitmapWidth/4;
            bitmapHeightDst = bitmapHeight;// + bitmapHeight/4;

            startX = width - bitmapWidthDst / 2;
            startY = height;
            endY = 0;
            curPos = 0;
            endX = mRandom.nextInt(width / 2) + width / 4;
            controlX = mRandom.nextInt(width - bitmapWidthDst) + bitmapWidthDst / 2;
            controlY = mRandom.nextInt(height / 2) + height / 2;
            controlX2 = mRandom.nextInt(width - bitmapWidthDst) + bitmapWidthDst / 2;
            controlY2 = mRandom.nextInt(height / 2) + height / 4;

            src = new Rect(0, 0, bitmapWidth, bitmapHeight);
            dst = new Rect(0, 0, bitmapWidthDst / 2, bitmapHeightDst / 2);

            paint = new Paint();
            paint.setAntiAlias(true);

            path = new Path();
            pathMeasure = new PathMeasure();
            path.moveTo(startX, startY);
            path.cubicTo(controlX, controlY, controlX2, controlY2, endX, endY);
            pathMeasure.setPath(path, false);
            length = (int) pathMeasure.getLength();
            speed = mRandom.nextInt(1) + 1f;
        }

        /**
         * 获取bitmap，用来canvas
         *
         * @return
         */
        public Bitmap getBitmap() {
            return bitmap;
        }

        /**
         * 画笔
         *
         * @return
         */
        public Paint getPaint() {
            return paint;
        }

        /**
         * 获取起始Rect
         *
         * @return
         */
        public Rect getSrcRect() {
            return src;
        }

        /**
         * 暂时不用了
         *
         * @return
         */
        @Deprecated
        public float[] getPos() {
            curPos += speed;
            speed += acceleratedSpeed;
            if (curPos > length) {
                curPos = length;
                return null;
            }
            pathMeasure.getPosTan(curPos, p, null);
            time++;
            alpha();
            return p;
        }

        /**
         * 获取目标Rect，根据当前点坐标计算Rect，已经加入放大/淡出动画
         *
         * @return
         */
        public Rect getDstRect() {
            curPos += speed;
            if (time < scaleTime) {
                speed = 3;
            } else {
                if (speed <= speedMax) {
                    speed += acceleratedSpeed;
                }
            }

            if (curPos > length) {
                curPos = length;
                return null;
            }

            pathMeasure.getPosTan(curPos, p, null);

            if (time < scaleTime) {
                // 放大动画
                float s = (float) time / scaleTime;
                dst.left = (int) (p[0] - bitmapWidthDst / 4 * s);
                dst.right = (int) (p[0] + bitmapWidthDst / 4 * s);
                dst.top = (int) ((p[1] - bitmapHeightDst / 2 * s));
                dst.bottom = (int) (p[1]);
            } else {
                dst.left = (int) (p[0] - bitmapWidthDst / 4);
                dst.right = (int) (p[0] + bitmapWidthDst / 4);
                dst.top = (int) (p[1] - bitmapHeightDst / 2);
                dst.bottom = (int) (p[1]);
            }
            time++;
            alpha();
            return dst;
        }

        private int alpha() {
            int offset = length - curPos;
            if (offset < length / 1.5) {
                alpha -= alphaOffset;
                if (alpha < 0) {
                    alpha = 0;
                }
                paint.setAlpha(alpha);
            } else if (offset <= 10) {
                alpha = 0;
                paint.setAlpha(alpha);
            }
            return 0;
        }

        public int getAlpha() {
            return alpha;
        }

        /**
         * 获取当前执行次数
         *
         * @return
         */
        public int getTime() {
            return time;
        }
    }

}
