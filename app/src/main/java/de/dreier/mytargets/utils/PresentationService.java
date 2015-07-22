/*
 * MyTargets Archery
 *
 * Copyright (C) 2015 Florian Dreier
 * All rights reserved
 */
package de.dreier.mytargets.utils;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.gms.cast.CastPresentation;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;

import de.dreier.mytargets.R;

public class PresentationService extends CastRemoteDisplayLocalService {
    private FirstScreenPresentation mPresentation;

    @Override
    public void onCreatePresentation(Display display) {
        createPresentation(display);
    }

    @Override
    public void onDismissPresentation() {
        dismissPresentation();
    }

    private final static class FirstScreenPresentation extends CastPresentation {


        public FirstScreenPresentation(Context context,
                Display display) {
            super(context, display);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.cast_timer);

        }
    }

    private void dismissPresentation() {
        if (mPresentation != null) {
            mPresentation.dismiss();
            mPresentation = null;
        }
    }

    private void createPresentation(Display display) {
        dismissPresentation();
        mPresentation = new FirstScreenPresentation(this, display);
        try {
            mPresentation.show();
        } catch (WindowManager.InvalidDisplayException ex) {
            Log.e("PresentationService", "Unable to show presentation, display was " +
                    "removed.", ex);
            dismissPresentation();
        }
    }
}
