package com.itamecodes.thewatch;

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

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by vivek on 4/13/15.
 */
public class TheWatchFaceService extends CanvasWatchFaceService {
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);


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
            Resources resources = TheWatchFaceService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.background, null);
            Drawable secondDrawable = resources.getDrawable(R.drawable.secondhand, null);
            mBackGroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            mSecondHandBitmap = ((BitmapDrawable) secondDrawable).getBitmap();
            setWatchFaceStyle(new WatchFaceStyle.Builder(TheWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(true)
                    .build());
            mTime = new Time();
            paintBitmap = new Paint();
            paintBitmap.setAntiAlias(true);
            paintBitmap.setFilterBitmap(true);
            mCenterPaint = new Paint();
            mCenterPaint.setAntiAlias(true);
            mCenterPaint.setFilterBitmap(true);
            mCenterPaint.setStyle(Paint.Style.FILL);
            mCenterPaint.setColor(Color.BLUE);
            mCenterPaint.setStrokeWidth(30);

            mPaint = new Paint();
            mPaint.setColor(Color.BLUE);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            mTime.setToNow();
            int width = bounds.width();
            int height = bounds.height();
            if (mBackGroundScaledBitmap == null || mBackGroundScaledBitmap.getWidth() != width || mBackGroundScaledBitmap.getHeight() != height) {
                mBackGroundScaledBitmap = Bitmap.createScaledBitmap(mBackGroundBitmap, width, height, true);
                mSecondHandScaledBitmap = Bitmap.createScaledBitmap(mSecondHandBitmap, mSecondHandBitmap.getWidth(), 120, true);
                mHourScaledBitmap = Bitmap.createScaledBitmap(mSecondHandBitmap, mSecondHandBitmap.getWidth(), 80, true);
                mMinuteScaledBitmap = Bitmap.createScaledBitmap(mSecondHandBitmap, mSecondHandBitmap.getWidth(), 100, true);
            }
            canvas.drawBitmap(mBackGroundScaledBitmap, 0, 0, null);
            float secRot = mTime.second;
            if (secRot == 0.0) {
                if (shouldRevCircle) {
                    Log.v("vivekrot", "reverse true");
                    shouldRevCircle = false;
                } else {
                    Log.v("vivekrot", "reverse false");
                    shouldRevCircle = true;
                }
            }
            //float secRot=10;
            // if (!isInAmbientMode()) {
            RectF oval = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
            if (shouldRevCircle) {
                Log.v("vivekrot", "sec=" + secRot + "---" + secRot * 6);
                canvas.drawArc(oval, 270, secRot * 6, true, mPaint);
            } else {
                Log.v("vivekrot", "sec=" + secRot + "---" + secRot * 6);
                float startdeg = (secRot * 6) - 90;
                canvas.drawArc(oval, startdeg, (180 - startdeg) + 90, true, mPaint);
            }
               /* Matrix rotator = new Matrix();
                rotator.postRotate(secRot * 6, 0, mSecondHandScaledBitmap.getHeight() - 30);
                rotator.postTranslate(canvas.getWidth() / 2, ((canvas.getHeight() / 2) - mSecondHandScaledBitmap.getHeight()) + 30);
                canvas.drawBitmap(mSecondHandScaledBitmap, rotator, paintBitmap);*/
            //   }

            Matrix rotatorminute = new Matrix();
            rotatorminute.postRotate((float) ((mTime.minute * 6) + (secRot * 0.1)), 0, mMinuteScaledBitmap.getHeight());
            rotatorminute.postTranslate(canvas.getWidth() / 2, ((canvas.getHeight() / 2) - mMinuteScaledBitmap.getHeight()));
            canvas.drawBitmap(mMinuteScaledBitmap, rotatorminute, paintBitmap);

            Matrix rotatorhour = new Matrix();
            rotatorhour.postRotate((float) ((mTime.hour * 30) + (mTime.minute * 0.5) + (mTime.second * (30 / 3600))), 0, mHourScaledBitmap.getHeight());
            rotatorhour.postTranslate(canvas.getWidth() / 2, ((canvas.getHeight() / 2) - mHourScaledBitmap.getHeight()));
            canvas.drawBitmap(mHourScaledBitmap, rotatorhour, paintBitmap);
            // canvas.drawCircle();

            canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, 5, mCenterPaint);

        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            // invalidate();

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

            //return isVisible() && !isInAmbientMode();
            return isVisible();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            TheWatchFaceService.this.registerReceiver(mTimeZoneReciever, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            TheWatchFaceService.this.unregisterReceiver(mTimeZoneReciever);
        }
    }
}
