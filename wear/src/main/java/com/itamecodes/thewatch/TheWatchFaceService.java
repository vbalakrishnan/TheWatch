package com.itamecodes.thewatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
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

    private class Engine extends CanvasWatchFaceService.Engine{
        static final int MSG_UPDATE_TIME = 0;
        Paint paintBitmap;
        Time mTime;
        boolean mLowBitAmbient,mBurnInProtection;
        Bitmap mBackGroundBitmap,mBackGroundScaledBitmap,mSecondHandBitmap,mSecondHandScaledBitmap;
        Paint mHourPaint;
        Paint mMinutePaint;
        private boolean mRegisteredTimeZoneReceiver=false;

        final Handler mUpdateTimeHandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what){
                    case MSG_UPDATE_TIME:{
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

        final BroadcastReceiver mTimeZoneReciever=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Resources resources=TheWatchFaceService.this.getResources();
            Drawable backgroundDrawable=resources.getDrawable(R.drawable.background,null);
            Drawable secondDrawable=resources.getDrawable(R.drawable.secondhand,null);
            mBackGroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            mSecondHandBitmap=((BitmapDrawable)secondDrawable).getBitmap();
            setWatchFaceStyle(new WatchFaceStyle.Builder(TheWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(true)
                    .build());
            mTime=new Time();
             paintBitmap = new Paint();
            paintBitmap.setAntiAlias(true);
            paintBitmap.setFilterBitmap(true);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            mTime.setToNow();
            int width=bounds.width();
            int height=bounds.height();
            if(mBackGroundScaledBitmap==null||mBackGroundScaledBitmap.getWidth()!=width||mBackGroundScaledBitmap.getHeight()!=height){
                mBackGroundScaledBitmap=Bitmap.createScaledBitmap(mBackGroundBitmap,width,height,true);
                mSecondHandScaledBitmap=Bitmap.createScaledBitmap(mSecondHandBitmap,mSecondHandBitmap.getWidth(),400,true);
            }
            canvas.drawBitmap(mBackGroundScaledBitmap,0,0,null);
            float secRot=mTime.second;
            Log.v("vivekrot", secRot + "--");
            Matrix rotator = new Matrix();
            rotator.postRotate(secRot*6,0,mSecondHandScaledBitmap.getHeight());
            rotator.postTranslate(canvas.getWidth()/2,((canvas.getHeight()/2)-mSecondHandScaledBitmap.getHeight()));

            canvas.drawBitmap(mSecondHandScaledBitmap,rotator,paintBitmap);

        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();

        }



        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient=properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT,false);
            mBurnInProtection=properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,false);
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
            if(visible){
                registerReceiver();
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            }else{
                unregisterReceiver();
            }
            updateTimer();
        }

        private void updateTimer(){
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if(shouldTimerBeRunning()){
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }

        }

        private boolean shouldTimerBeRunning(){

            //return isVisible() && !isInAmbientMode();
            return isVisible();
        }

        private void registerReceiver(){
            if(mRegisteredTimeZoneReceiver){
                return;
            }
            mRegisteredTimeZoneReceiver=true;
            IntentFilter filter=new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            TheWatchFaceService.this.registerReceiver(mTimeZoneReciever,filter);
        }

        private void unregisterReceiver(){
            if(!mRegisteredTimeZoneReceiver){
                return;
            }
            mRegisteredTimeZoneReceiver=false;
            TheWatchFaceService.this.unregisterReceiver(mTimeZoneReciever);
        }
    }
}
