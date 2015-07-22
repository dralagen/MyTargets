/*
 * MyTargets Archery
 *
 * Copyright (C) 2015 Florian Dreier
 * All rights reserved
 */

package de.dreier.mytargets.fragments;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import de.dreier.mytargets.R;
import de.dreier.mytargets.activities.SimpleFragmentActivity;
import de.dreier.mytargets.utils.PresentationService;
import de.dreier.mytargets.views.Timer;

/**
 * Shows all passes of one round
 */
public class TimerFragment extends Fragment {
    public static final String CAST_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;//"MY_TARGETS";

    private PowerManager.WakeLock wakeLock;
    private HelloWorldChannel mHelloWorldChannel;
    private boolean mApplicationStarted;
    private Timer mTimer;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mTimer = (Timer)inflater.inflate(R.layout.fragment_timer, container, false);
        return mTimer;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        Toolbar toolbar = (Toolbar) mTimer.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setHasOptionsMenu(true);
        mTimer.changeStatus(Timer.Status.WAIT_FOR_START);
        mMediaRouter = MediaRouter.getInstance(getActivity().getApplicationContext());
    }


    @Override
    public void onResume() {
        super.onResume();
        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "WakeLock");
        wakeLock.acquire();
    }

    @Override
    public void onPause() {
        super.onPause();
        wakeLock.release();
    }

    @Override
    public void onStop() {
        mTimer.cleanup();
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
    }

    MediaRouteSelector mMediaRouteSelector = new MediaRouteSelector.Builder()
            .addControlCategory(CastMediaControlIntent.categoryForCast(CAST_ID))
            .build();

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.timer, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_preferences) {
            startActivity(
                    new Intent(getActivity(), SimpleFragmentActivity.SettingsActivity.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    MyMediaRouterCallback mMediaRouterCallback = new MyMediaRouterCallback();

    private class MyMediaRouterCallback extends MediaRouter.Callback {

        private GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks();
        private GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener = new ConnectionFailedListener();

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            String routeId = info.getId();
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(mSelectedDevice, mCastClientListener);

            mApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mConnectionFailedListener)
                    .build();

            mApiClient.connect();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            CastRemoteDisplayLocalService.stopService();
            mSelectedDevice = null;
        }
    }

    private CastDevice mSelectedDevice;
    private MediaRouter mMediaRouter;
    private GoogleApiClient mApiClient;
    private boolean mWaitingForReconnect = false;

    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;
                //reconnectChannels();
            } else {
                try {
                    Cast.CastApi.launchApplication(mApiClient, CAST_ID, false)
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(Cast.ApplicationConnectionResult result) {
                                            Status status = result.getStatus();
                                            if (status.isSuccess()) {
                                                ApplicationMetadata applicationMetadata =
                                                        result.getApplicationMetadata();
                                                String sessionId = result.getSessionId();
                                                String applicationStatus = result
                                                        .getApplicationStatus();
                                                boolean wasLaunched = result.getWasLaunched();

                                                Intent intent = new Intent(getActivity(),
                                                        SimpleFragmentActivity.TimerActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                                PendingIntent notificationPendingIntent = PendingIntent
                                                        .getActivity(getActivity(), 0, intent, 0);

                                                CastRemoteDisplayLocalService.NotificationSettings settings =
                                                        new CastRemoteDisplayLocalService.NotificationSettings.Builder()
                                                                .setNotificationPendingIntent(notificationPendingIntent).build();

                                                CastRemoteDisplayLocalService.startService(
                                                        getActivity().getApplicationContext(),
                                                        PresentationService.class, CAST_ID,
                                                        mSelectedDevice, settings,
                                                        new CastRemoteDisplayLocalService.Callbacks() {
                                                            @Override
                                                            public void onRemoteDisplaySessionStarted(
                                                                    CastRemoteDisplayLocalService service) {
                                                                // initialize sender UI
                                                            }

                                                            @Override
                                                            public void onRemoteDisplaySessionError(
                                                                    Status errorReason){
                                                                //initError();
                                                            }
                                                        });
                                            } else {
                                                //teardown();
                                            }
                                        }
                                    });

                } catch (Exception e) {
                    Log.e(CAST_ID, "Failed to launch application", e);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            mWaitingForReconnect = true;
        }
    }

    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            //teardown();
        }
    }

    private void sendMessage(String message) {
        if (mApiClient != null && mHelloWorldChannel != null) {
            try {
                Cast.CastApi.sendMessage(mApiClient, mHelloWorldChannel.getNamespace(), message)
                        .setResultCallback(
                                new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(Status result) {
                                        if (!result.isSuccess()) {
                                            Log.e(CAST_ID, "Sending message failed");
                                        }
                                    }
                                });
            } catch (Exception e) {
                Log.e(CAST_ID, "Exception while sending message", e);
            }
        }
    }

    /*@Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mRemoteMediaPlayer != null) {
                        double currentVolume = Cast.CastApi.getVolume(mApiClient);
                        if (currentVolume < 1.0) {
                            try {
                                Cast.CastApi.setVolume(mApiClient,
                                        Math.min(currentVolume + VOLUME_INCREMENT, 1.0));
                            } catch (Exception e) {
                                Log.e(CAST_ID, "unable to set volume", e);
                            }
                        }
                    } else {
                        Log.e(CAST_ID, "dispatchKeyEvent - volume up");
                    }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mRemoteMediaPlayer != null) {
                        double currentVolume = Cast.CastApi.getVolume(mApiClient);
                        if (currentVolume > 0.0) {
                            try {
                                Cast.CastApi.setVolume(mApiClient,
                                        Math.max(currentVolume - VOLUME_INCREMENT, 0.0));
                            } catch (Exception e) {
                                Log.e(CAST_ID, "unable to set volume", e);
                            }
                        }
                    } else {
                        Log.e(CAST_ID, "dispatchKeyEvent - volume down");
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }*/

    Cast.Listener mCastClientListener = new Cast.Listener() {
        @Override
        public void onApplicationStatusChanged() {
            if (mApiClient != null) {
                Log.d(CAST_ID, "onApplicationStatusChanged: "
                        + Cast.CastApi.getApplicationStatus(mApiClient));
            }
        }

        @Override
        public void onVolumeChanged() {
            if (mApiClient != null) {
                Log.d(CAST_ID, "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
            }
        }

        @Override
        public void onApplicationDisconnected(int errorCode) {
            //teardown();
        }
    };

    class HelloWorldChannel implements Cast.MessageReceivedCallback {
        public String getNamespace() {
            return "urn:x-cast:com.example.custom";
        }

        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace,
                String message) {
            Log.d(CAST_ID, "onMessageReceived: " + message);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }
}
