/*
 * MyTargets Archery
 *
 * Copyright (C) 2015 Florian Dreier
 * All rights reserved
 */

package de.dreier.mytargets.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import de.dreier.mytargets.R;
import de.dreier.mytargets.fragments.EditFragmentBase;
import de.dreier.mytargets.fragments.FragmentBase;
import de.dreier.mytargets.shared.models.IIdProvider;
import io.codetail.animation.SupportAnimator;

/**
 * Shows an overview over all trying days
 */
public class MainActivity extends AppCompatActivity implements EditFragmentBase.OnFragmentTouched,
        FragmentBase.OnItemSelectedListener, FragmentBase.ContentListener {

    private static final String FRAGMENT = "fragment";
    private static final String REVEAL_ANIMATION = "reveal_animation";
    private static boolean shownThisTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        askForHelpTranslating();
    }

    private void askForHelpTranslating() {
        ArrayList<String> supportedLanguages = new ArrayList<>();
        Collections.addAll(supportedLanguages, "de", "en", "fr", "es", "ru", "nl", "it", "sl", "ca",
                "zh", "tr", "hu", "sl");
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(MainActivity.this);
        boolean shown = prefs.getBoolean("translation_dialog_shown", false);
        String longLang = Locale.getDefault().getDisplayLanguage();
        String shortLocale = Locale.getDefault().getLanguage();
        if (!supportedLanguages.contains(shortLocale) && !shown && !shownThisTime) {
            // Link the e-mail address in the message
            final SpannableString s = new SpannableString(Html.fromHtml("If you would like " +
                    "to help make MyTargets even better by translating the app to " +
                    longLang +
                    " visit <a href=\"https://crowdin.com/project/mytargets\">crowdin</a>!<br /><br />" +
                    "Thanks in advance :)"));
            Linkify.addLinks(s, Linkify.EMAIL_ADDRESSES);
            AlertDialog d = new AlertDialog.Builder(this)
                    .setTitle("App translation")
                    .setMessage(s)
                    .setPositiveButton("OK", (dialog, which) -> {
                        prefs.edit().putBoolean("translation_dialog_shown", true).apply();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Remind me later", (dialog, which) -> {
                        shownThisTime = true;
                        dialog.dismiss();
                    }).create();
            d.show();
            ((TextView) d.findViewById(android.R.id.message))
                    .setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_preferences:
                startActivity(new Intent(this, SimpleFragmentActivity.SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void openFragmentFromClickOn(View v, EditFragmentBase childFragment) {
        Bundle bundle = new Bundle();
        bundle.putInt("cx", (int) (v.getX() + v.getWidth() / 2));
        bundle.putInt("cy", (int) (v.getY() + v.getWidth() / 2));
        childFragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(REVEAL_ANIMATION)
                .replace(R.id.content, childFragment, "fragment")
                .commit();
    }

    @Override
    public void onItemSelected(IIdProvider item) {
        Fragment fragment = getCurrentFragment();
        if (fragment != null && fragment instanceof FragmentBase.OnItemSelectedListener) {
            ((FragmentBase.OnItemSelectedListener) fragment).onItemSelected(item);
        }
    }

    @Override
    public void onContentChanged(boolean empty, int stringRes) {
        Fragment fragment = getCurrentFragment();
        if (fragment != null && fragment instanceof FragmentBase.ContentListener) {
            ((FragmentBase.ContentListener) fragment).onContentChanged(empty, stringRes);
        }
    }

    protected Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentByTag(FRAGMENT);
    }

    @Override
    public void onBackPressed() {
        int entryCount = getSupportFragmentManager().getBackStackEntryCount();
        if (entryCount == 0) {
            super.onBackPressed();
        }
        FragmentManager.BackStackEntry entry = getSupportFragmentManager()
                .getBackStackEntryAt(entryCount - 1);
        if (REVEAL_ANIMATION.equals(entry.getName())) {
            Fragment fragment = getCurrentFragment();
            onFragmentTouched(fragment, fragment.getArguments().getInt("cx"),
                    fragment.getArguments().getInt("cy"));
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onFragmentTouched(Fragment fragment, float x, float y) {
        if (fragment instanceof EditFragmentBase) {
            final EditFragmentBase theFragment = (EditFragmentBase) fragment;
            SupportAnimator unreveal = theFragment.prepareUnrevealAnimator(x, y);
            unreveal.addListener(new SupportAnimator.SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd() {
                    // remove the fragment only when the animation finishes
                    getSupportFragmentManager().beginTransaction().remove(theFragment).commit();
                    //to prevent flashing the fragment before removing it, execute pending transactions inmediately
                    getSupportFragmentManager().executePendingTransactions();
                }
            });
            unreveal.start();
        }
    }
}
