package tony.waveprogressview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * 默认以５个周期显示
 * Created by tony on 7/14/16.
 */
public class WaveProgressView extends ImageView {
    private Bitmap mBitmap;

    private float mWaveHeight = 16f;
    private int mWaveNum = 1;
    private float mWaveWidth = 0f;
    private int mWaveColor = Color.RED;
    private float mWaveSpeed = 312f;

    private Path mPath;
    private Paint mPaint;

    private Paint mDstPaint;

    private int mWidth, mHeight;

    private int maxProgress = 100;
    private int currentProgress = 0;
    private float mCurrentY;

    private float distance = 0f;

    //确保在60fps以上
    private static final int INVALIDATE = 0x0001;
    private static final int INVALIDATE_TIME = 16;
    private static final int INVALIDATE_TIMES = 1000 / INVALIDATE_TIME;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case INVALIDATE:
                    invalidate();
                    sendEmptyMessageDelayed(INVALIDATE, INVALIDATE_TIME);
                    break;
            }
        }
    };

    public WaveProgressView(Context context) {
        this(context, null);
    }

    public WaveProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        mHeight = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        mCurrentY = mHeight;
        mWaveWidth = mWidth / mWaveNum;
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        if (null == getDrawable()) {
            throw new IllegalArgumentException(String.format("background is null."));
        } else {
            mBitmap = getBitmapFromDrawable(getDrawable());
        }

        //波浪
        mPath = new Path();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);


        //上层图形
        mDstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDstPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));

        //第一次可见之后开始刷新界面
        post(new Runnable() {
            @Override
            public void run() {
                handler.sendEmptyMessage(INVALIDATE);
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            canvas.drawBitmap(createBitmap(), 0, 0, null);
        }
    }

    private Bitmap createBitmap() {
        mPaint.setColor(mWaveColor);

        //CurMidY 是目标的Y轴坐标，每次波浪上升的时候为了不产生卡顿效果，把这1/100的上升分为10次来绘制。
        float CurMidY = mHeight * (maxProgress - currentProgress) / maxProgress;
        //CurY是当前需要绘制的y坐标
        if (mCurrentY > CurMidY) {
            mCurrentY = mCurrentY - (mCurrentY - CurMidY) / 10;
        } else {
            mCurrentY = mCurrentY + (CurMidY - mCurrentY) / 10;
        }

        //==========================================================================================
        mPath.reset();
        //distance是ｘ轴的偏移量，为了使水波动起来，每次绘制时都要向左进行一段偏移
        mPath.moveTo(0 - distance, mCurrentY);
        int multiplier = 0;
        float waveQuarterWidth = mWaveWidth / 4;
        for (int i = 0; i < mWaveNum * 2; i++) {
            mPath.quadTo(waveQuarterWidth * (multiplier + 1) - distance, mCurrentY - mWaveHeight, waveQuarterWidth * (multiplier + 2) - distance, mCurrentY);
            mPath.quadTo(waveQuarterWidth * (multiplier + 3) - distance, mCurrentY + mWaveHeight, waveQuarterWidth * (multiplier + 4) - distance, mCurrentY);
            multiplier += 4;
        }
        //每次偏移距离为　波速/每秒更新的次数
        distance += mWaveSpeed / INVALIDATE_TIMES;
        if (distance >= mWaveWidth * mWaveNum) {
            distance = 0;
        }
        mPath.lineTo(mWidth, mHeight);
        mPath.lineTo(0, mHeight);
        mPath.close();
        //创建一个要显示的画布
        Bitmap dst = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dst);
        canvas.drawPath(mPath, mPaint);
        //==========================================================================================

        canvas.drawBitmap(mBitmap, 0, 0, mDstPaint);
        return dst;
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        try {
            Bitmap bitmap;
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    public void setCurrent(int currentProgress) {
        this.currentProgress = currentProgress;
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
    }

    public void setWave(float mWaveHeight, float mWaveWidth) {
        this.mWaveHeight = mWaveHeight;
        this.mWaveWidth = mWaveWidth;
    }


    public void setWaveColor(int mWaveColor) {
        this.mWaveColor = mWaveColor;
    }

    /**
     * @param waveSpeed px/s
     */
    public void setWaveSpeed(int waveSpeed) {
        this.mWaveSpeed = waveSpeed;
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }
}
