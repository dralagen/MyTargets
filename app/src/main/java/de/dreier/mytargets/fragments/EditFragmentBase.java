/*
 * MyTargets Archery
 *
 * Copyright (C) 2015 Florian Dreier
 * All rights reserved
 */
package de.dreier.mytargets.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.Arrays;
import java.util.Collections;

import de.dreier.mytargets.R;
import de.dreier.mytargets.utils.MyBackupAgent;
import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

public abstract class EditFragmentBase extends Fragment {
    AppCompatActivity activity;
    SharedPreferences prefs;
    private OnFragmentTouched listener;

    protected AppCompatActivity setUpToolbar(View rootView) {
        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        assert activity.getSupportActionBar() != null;
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        setHasOptionsMenu(true);

        prefs = activity.getSharedPreferences(MyBackupAgent.PREFS, 0);
        return activity;
    }

    public void setTitle(@StringRes int title) {
        assert activity.getSupportActionBar() != null;
        activity.getSupportActionBar().setTitle(title);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.save, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            getActivity().setResult(Activity.RESULT_OK);
            onSave();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected abstract void onSave();


    public void setUpReveal(View rootView, Bundle savedInstanceState) {
        if (savedInstanceState != null || !getArguments().containsKey("cx")) {
            return;
        }

        // To run the animation as soon as the view is layout in the view hierarchy we add this
        // listener and remove it
        // as soon as it runs to prevent multiple animations if the view changes bounds
        rootView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                    int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                int cx = getArguments().getInt("cx");
                int cy = getArguments().getInt("cy");

                // get the hypothenuse so the radius is from one corner to the other
                int radius = (int) Math.hypot(right, bottom);

                SupportAnimator reveal = ViewAnimationUtils
                        .createCircularReveal(v, cx, cy, 0, radius);
                //reveal.setInterpolator(new DecelerateInterpolator(2f));
                reveal.start();
            }
        });

        // attach a touch listener
        rootView.setOnTouchListener((v, event) -> {
            listener.onFragmentTouched(this, event.getX(), event.getY());
            return true;
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentTouched) {
            listener = (OnFragmentTouched) context;
        }
    }

    /**
     * Get the animator to unreveal the circle
     *
     * @param cx center x of the circle (or where the view was touched)
     * @param cy center y of the circle (or where the view was touched)
     * @return Animator object that will be used for the animation
     */
    public SupportAnimator prepareUnrevealAnimator(float cx, float cy) {
        int radius = getEnclosingCircleRadius(getView(), (int) cx, (int) cy);
        SupportAnimator anim = ViewAnimationUtils
                .createCircularReveal(getView(), (int) cx, (int) cy, radius, 0);
        //anim.setInterpolator(new AccelerateInterpolator(2f));
        //anim.setDuration(1000);
        return anim;
    }

    /**
     * To be really accurate we have to start the circle on the furthest corner of the view
     *
     * @param v  the view to unreveal
     * @param cx center x of the circle
     * @param cy center y of the circle
     * @return the maximum radius
     */
    private int getEnclosingCircleRadius(View v, int cx, int cy) {
        int realCenterX = cx + v.getLeft();
        int realCenterY = cy + v.getTop();
        int distanceTopLeft = (int) Math.hypot(realCenterX - v.getLeft(), realCenterY - v.getTop());
        int distanceTopRight = (int) Math
                .hypot(v.getRight() - realCenterX, realCenterY - v.getTop());
        int distanceBottomLeft = (int) Math
                .hypot(realCenterX - v.getLeft(), v.getBottom() - realCenterY);
        int distanceBottomRight = (int) Math
                .hypot(v.getRight() - realCenterX, v.getBottom() - realCenterY);

        Integer[] distances = new Integer[]{distanceTopLeft, distanceTopRight, distanceBottomLeft,
                distanceBottomRight};

        return Collections.max(Arrays.asList(distances));
    }

    public interface OnFragmentTouched {
        void onFragmentTouched(Fragment fragment, float x, float y);
    }
}