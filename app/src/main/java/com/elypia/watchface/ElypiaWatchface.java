package com.elypia.watchface;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.SystemProviders;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.*;
import android.util.SparseArray;
import android.view.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ElypiaWatchface extends CanvasWatchFaceService {

    private static final long INTERACTIVE_UPDATE_RATE_MS = 60000;
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    public class Engine extends CanvasWatchFaceService.Engine {

        ///
        private SparseArray<ComplicationData> activeComplications;
        private SparseArray<ComplicationDrawable> complicationDrawables;

        public final int[] COMPLICATIONS = ComplicationLocation.getComplicationIds();
        public final int[][] SUPPORTED_TYPES = ComplicationLocation.getSupportedTypes();

        private void initializeComplications() {
            int length = COMPLICATIONS.length;
            activeComplications = new SparseArray<>(length);
            complicationDrawables = new SparseArray<>(length);

            for (int i : COMPLICATIONS) {
                Drawable drawable = getDrawable(R.drawable.complication_style);
                ComplicationDrawable complicationDrawable = (ComplicationDrawable)drawable;

                complicationDrawable.setContext(getApplicationContext());
                complicationDrawables.put(i, complicationDrawable);
            }

            setActiveComplications(COMPLICATIONS);
        }

        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            activeComplications.put(complicationId, complicationData);
            complicationDrawables.get(complicationId).setComplicationData(complicationData);
            invalidate();
        }
        ///

        private final Locale locale;

        /**
         * The format to display the full date including day, month and year.
         */
        private final SimpleDateFormat dateFormat;

        /**
         * The format to display the time of the day.
         */
        private final SimpleDateFormat timeFormat;

        /**
         * The calendar used througout this application, this is tracking the
         * time and how we know what to display on the watchface.
         */
        private final Calendar calendar;

        /**
         * Used to paint text to our canvas.
         */
        private final Paint textPaint;

        /**
         * Used to paint images to our canvas.
         */
        private final Paint bitmapPaint;

        private final Handler handler;
        private final BroadcastReceiver receiver;

        /**
         * The background to apply when in interactive mode
         * as opposed to ambient mode. (When the watch is in active use.)
         */
        private final Bitmap background;

        private boolean registeredTimeZoneReceiver;
        private Date now;
        private int dateCenterX;
        private String dateText;
        private String timeText;
        int complicationOneX;

        private Engine() {
            locale = Locale.getDefault();
            dateFormat = new SimpleDateFormat("EEE dd MMM yyyy", locale);
            timeFormat = new SimpleDateFormat("HH:mm", locale);
            calendar = Calendar.getInstance();
            textPaint = new Paint();
            bitmapPaint = new Paint();

            handler = new EngineHandler(this, MSG_UPDATE_TIME);
            receiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    calendar.setTimeZone(TimeZone.getDefault());
                    invalidate();
                }
            };

            background = BitmapFactory.decodeResource(getResources(), R.drawable.background);

            registeredTimeZoneReceiver = false;
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            WatchFaceStyle.Builder builder = new WatchFaceStyle.Builder(ElypiaWatchface.this);
            setWatchFaceStyle(builder.setAcceptsTapEvents(true).build());

            textPaint.setTypeface(getResources().getFont(R.font.agency));
            textPaint.setTextSize(42);
            textPaint.setAntiAlias(true);
            textPaint.setColor(Color.WHITE);

            setDefaultSystemComplicationProvider(ComplicationLocation.HEAD_RIGHT.getId(), SystemProviders.STEP_COUNT, ComplicationData.TYPE_ICON);
            initializeComplications();
        }

        /**
         * We use this method to calculate where we want to position
         * all of our drawable entities. This allows us to only redetermine
         * where we draw things and the math associated with it when the surface changes
         * rather than doing it everytime we draw.
         *
         * @param holder
         * @param format
         * @param width
         * @param height
         */
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            now = calendar.getTime();
            dateText = dateFormat.format(now);
            timeText = timeFormat.format(now);

            int dateWidth = (int)textPaint.measureText(dateText);

            dateCenterX = (width - dateWidth) / 2;
            complicationOneX = (dateCenterX + dateWidth);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            calendar.setTimeInMillis(System.currentTimeMillis());

            if (isInAmbientMode())
                canvas.drawColor(Color.BLACK);
            else
                canvas.drawBitmap(background, 0, 0, bitmapPaint);

            now = calendar.getTime();

            dateText = dateFormat.format(now);
            timeText = timeFormat.format(now);

            canvas.drawText(dateText, dateCenterX, 86, textPaint);
            canvas.drawText(timeText, dateCenterX, 86 + textPaint.getTextSize(), textPaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            updateTimer();
        }

        private void registerReceiver() {
            if (registeredTimeZoneReceiver)
                return;

            registeredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            ElypiaWatchface.this.registerReceiver(receiver, filter);
        }

        private void unregisterReceiver() {
            if (!registeredTimeZoneReceiver)
                return;

            registeredTimeZoneReceiver = false;
            ElypiaWatchface.this.unregisterReceiver(receiver);
        }

        @Override
        public void onAmbientModeChanged(boolean ambient) {
            super.onAmbientModeChanged(ambient);
            textPaint.setAntiAlias(!ambient);

            for (int i : COMPLICATIONS)
                complicationDrawables.get(i).setInAmbientMode(ambient);

            updateTimer();
        }

        @Override
        public void onDestroy() {
            handler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        /**
         * Starts the {@link #handler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            handler.removeMessages(MSG_UPDATE_TIME);

            if (shouldTimerBeRunning())
                handler.sendEmptyMessage(MSG_UPDATE_TIME);
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        public void handleUpdateTimeMessage() {
            invalidate();

            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);

                handler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        /**
         * Returns whether the {@link #handler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}
