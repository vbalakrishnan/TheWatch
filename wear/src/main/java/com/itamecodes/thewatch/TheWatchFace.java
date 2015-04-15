package com.itamecodes.thewatch;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.animation.LinearInterpolator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by vivek on 4/15/15.
 */
public class TheWatchFace extends CanvasWatchFaceService {
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);


    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;
        boolean shouldRevCircle = false;
        Paint paintBitmap;
        Time mTime;
        boolean mLowBitAmbient, mBurnInProtection;
        Bitmap mBackGroundBitmap, mBackGroundScaledBitmap, mSecondHandBitmap, mSecondHandScaledBitmap, mHourScaledBitmap, mMinuteScaledBitmap;
        Paint mCenterPaint;
        Paint mMinutePaint;
        Paint mPaint;
        private boolean mRegisteredTimeZoneReceiver = false;

        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME: {
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                    }
                }
            }
        };

        final BroadcastReceiver mTimeZoneReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Resources resources = TheWatchFace.this.getResources();
            setWatchFaceStyle(new WatchFaceStyle.Builder(TheWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.thewatch_back, null);
            mBackGroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            mTime = new Time();
            mPaint = new Paint();
            mPaint.setColor(Color.WHITE);
            mPaint.setAntiAlias(true);
            mPaint.setTextSize(30);
            mPaint.setTypeface(BOLD_TYPEFACE);

        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();
            int width = bounds.width();
            int height = bounds.height();
            if (mBackGroundScaledBitmap == null || mBackGroundScaledBitmap.getWidth() != width || mBackGroundScaledBitmap.getHeight() != height) {
                mBackGroundScaledBitmap = Bitmap.createScaledBitmap(mBackGroundBitmap, width, height, true);
            }
          //  if (!isInAmbientMode()) {
                canvas.drawBitmap(mBackGroundScaledBitmap, 0, 0, null);

            Calendar calendar= Calendar.getInstance();
            SimpleDateFormat dateFormat=new SimpleDateFormat("hh:mm:ss");
            String timeNow=dateFormat.format(calendar.getTime());
            float x=mPaint.measureText(timeNow);
            float xOffset=(width-x)/2;
            canvas.drawText(timeNow,xOffset,height/2,mPaint);


        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();

        }


        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            invalidate();
            updateTimer();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }
            updateTimer();
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }

        }

        private boolean shouldTimerBeRunning() {

          //  return isVisible() && !isInAmbientMode();
            return isVisible();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            TheWatchFace.this.registerReceiver(mTimeZoneReciever, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            TheWatchFace.this.unregisterReceiver(mTimeZoneReciever);
        }

    }
}
