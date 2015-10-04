/*
 * MyTargets Archery
 *
 * Copyright (C) 2015 Florian Dreier
 * All rights reserved
 */
package de.dreier.mytargets.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.nineoldandroids.view.ViewPropertyAnimator;

import de.dreier.mytargets.R;
import de.dreier.mytargets.activities.MainActivity;
import de.dreier.mytargets.activities.SimpleFragmentActivity;
import de.dreier.mytargets.adapters.MainTabsFragmentPagerAdapter;
import de.dreier.mytargets.shared.models.Arrow;
import de.dreier.mytargets.shared.models.Bow;
import de.dreier.mytargets.shared.models.IIdProvider;

public class MainFragment extends Fragment implements ViewPager.OnPageChangeListener,
        FragmentBase.OnItemSelectedListener, FragmentBase.ContentListener,
        View.OnClickListener {

    private View mNewLayout;
    private TextView mNewText;
    private ViewPager viewPager;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        viewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        MainTabsFragmentPagerAdapter adapter = new MainTabsFragmentPagerAdapter(activity);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(this);

        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.sliding_tabs);
        tabLayout.setTabTextColors(0xCCFFFFFF, Color.WHITE);
        tabLayout.setupWithViewPager(viewPager);

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(this);
        if (savedInstanceState == null) {
            fab.setScaleX(0);
            fab.setScaleY(0);
            ViewPropertyAnimator.animate(fab).setStartDelay(500).scaleX(1).scaleY(1)
                    .setDuration(200).setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
        mNewLayout = rootView.findViewById(R.id.new_layout);
        mNewText = (TextView) rootView.findViewById(R.id.new_text);

        activity.setSupportActionBar((Toolbar) rootView.findViewById(R.id.toolbar));
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.settings, menu);
    }

    @Override
    public void onClick(View v) {
        EditFragmentBase fragment;
        switch (viewPager.getCurrentItem()) {
            case 0:
                fragment = new EditTrainingFragment();
                break;
            case 1:
                fragment = new EditBowFragment();
                break;
            default:
                fragment =  new EditArrowFragment();
                break;
        }
        ((MainActivity)getActivity()).openFragmentFromClickOn(v, fragment);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    private final boolean[] empty = new boolean[3];
    private final int[] stringRes = new int[3];

    {
        stringRes[0] = R.string.new_training;
        stringRes[1] = R.string.new_bow;
        stringRes[2] = R.string.new_arrow;
    }

    @Override
    public void onPageSelected(int position) {
        mNewLayout.setVisibility(empty[position] ? View.VISIBLE : View.GONE);
        mNewText.setText(stringRes[position]);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onContentChanged(boolean empty, int stringRes) {
        for (int i = 0; i < this.stringRes.length; i++) {
            if (stringRes == this.stringRes[i]) {
                this.empty[i] = empty;
            }
        }
        onPageSelected(viewPager.getCurrentItem());
    }

    @Override
    public void onItemSelected(IIdProvider item) {
        Intent i;
        if (item instanceof Arrow) {
            i = new Intent(getContext(), SimpleFragmentActivity.EditArrowActivity.class);
            i.putExtra(EditArrowFragment.ARROW_ID, item.getId());
        } else if (item instanceof Bow) {
            i = new Intent(getContext(), SimpleFragmentActivity.EditBowActivity.class);
            i.putExtra(EditBowFragment.BOW_ID, item.getId());
        } else {
            i = new Intent(getContext(), SimpleFragmentActivity.EditTrainingActivity.class);
            i.putExtra(TrainingsFragment.TRAINING_ID, item.getId());
        }
        startActivity(i);
        getActivity().overridePendingTransition(R.anim.right_in, R.anim.left_out);
    }
}
