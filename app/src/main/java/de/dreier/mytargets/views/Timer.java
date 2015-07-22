/*
 * MyTargets Archery
 *
 * Copyright (C) 2015 Florian Dreier
 * All rights reserved
 */
package de.dreier.mytargets.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import de.dreier.mytargets.R;

public class Timer extends FrameLayout implements View.OnClickListener {
    public static class Status {
        public static final int WAIT_FOR_START = 0;
        private static final int PREPARATION = 1;
        private static final int SHOOTING = 2;
        private static final int FINISHED = 3;
    }

    private int mCurStatus;
    private CountDownTimer countdown;

    private int mWaitingTime;
    private int mShootingTime;
    private int mWarnTime;
    private TextView mStatusField, mTimeField;
    private MediaPlayer horn;
    private boolean mSound, mVibrate;

    public Timer(Context context) {
        super(context);
        init();
    }

    public Timer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Timer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnClickListener(this);
        loadPreferenceValues();
        horn = MediaPlayer.create(getContext(), R.raw.horn);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return super.onSaveInstanceState();
    }

    public void changeStatus(int status) {
        mTimeField = (TextView) findViewById(R.id.timer_time);
        mStatusField = (TextView) findViewById(R.id.timer_status);
        if (countdown != null) {
            countdown.cancel();
        }
        mCurStatus = status;
        switch (status) {
            case Status.WAIT_FOR_START:
                setBackgroundResource(R.color.timer_red);
                mStatusField.setText(R.string.touch_to_start);
                mTimeField.setText("");
                break;
            case Status.PREPARATION:
                playSignal(2);
                mStatusField.setText(R.string.preparation);
                countdown = new CountDownTimer(mWaitingTime * 1000, 100) {
                    public void onTick(long millisUntilFinished) {
                        mTimeField.setText("" + millisUntilFinished / 1000);
                    }

                    public void onFinish() {
                        changeStatus(Status.SHOOTING);
                    }
                }.start();
                break;
            case Status.SHOOTING:
                playSignal(1);
                setBackgroundResource(R.color.timer_green);
                mStatusField.setText(R.string.shooting);
                countdown = new CountDownTimer(mShootingTime * 1000, 100) {
                    public void onTick(long millisUntilFinished) {
                        mTimeField.setText("" + millisUntilFinished / 1000);
                        if (millisUntilFinished <= mWarnTime * 1000) {
                            setBackgroundResource(R.color.timer_orange);
                        }
                    }

                    public void onFinish() {
                        changeStatus(Status.FINISHED);
                    }
                }.start();
                break;
            case Status.FINISHED:
                playSignal(3);
                setBackgroundResource(R.color.timer_red);
                mTimeField.setText(R.string.stop);
                countdown = new CountDownTimer(6000, 100) {
                    public void onTick(long millisUntilFinished) {
                        mStatusField.setText("" + millisUntilFinished / 1000);
                    }

                    public void onFinish() {
                        //getActivity().finish(); //TODO
                        //getActivity().overridePendingTransition(R.anim.left_in, R.anim.right_out);
                    }
                }.start();
                break;
        }
    }

    @Override
    public void onClick(View v) {
        changeStatus(mCurStatus + 1);
    }

    public void cleanup() {

        horn.release();
        horn = null;
        if (countdown != null) {
            countdown.cancel();
        }
    }

    private void loadPreferenceValues() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        mWaitingTime = getPrefTime(prefs, "timer_wait_time", "20");
        mShootingTime = getPrefTime(prefs, "timer_shoot_time", "120");
        mWarnTime = getPrefTime(prefs, "timer_warn_time", "30");
        mSound = prefs.getBoolean("timer_sound", true);
        mVibrate = prefs.getBoolean("timer_vibrate", false);
    }

    private int getPrefTime(SharedPreferences prefs, String key, String def) {
        try {
            return Integer.parseInt(prefs.getString(key, def));
        } catch (NumberFormatException e) {
            return Integer.parseInt(def);
        }
    }

    private void playSignal(final int n) {
        if (mSound) {
            playHorn(n);
        }
        if (mVibrate) {
            long[] pattern = new long[1 + n * 2];
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            pattern[0] = 150;
            for (int i = 0; i < n; i++) {
                pattern[i * 2 + 1] = 550;
                pattern[i * 2 + 2] = 800;
            }
            v.vibrate(pattern, -1);
        }
    }

    private void playHorn(final int n) {
        horn.start();
        horn.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                if (n > 1) {
                    playHorn(n - 1);
                }
            }

        });
    }
}
