/*
 * MyTargets Archery
 *
 * Copyright (C) 2015 Florian Dreier
 * All rights reserved
 */

package de.dreier.mytargets.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bignerdranch.android.recyclerviewchoicemode.SelectableViewHolder;

import java.util.ArrayList;
import java.util.List;

import de.dreier.mytargets.R;
import de.dreier.mytargets.activities.SimpleFragmentActivity;
import de.dreier.mytargets.adapters.NowListAdapter;
import de.dreier.mytargets.managers.dao.StandardRoundDataSource;
import de.dreier.mytargets.shared.models.Dimension;
import de.dreier.mytargets.shared.models.Distance;
import de.dreier.mytargets.shared.models.RoundTemplate;
import de.dreier.mytargets.shared.models.StandardRound;
import de.dreier.mytargets.shared.models.target.Target;

import static de.dreier.mytargets.activities.ItemSelectActivity.ITEM;

public class StandardRoundFragment extends SelectItemFragment<StandardRound> implements View.OnClickListener {

    private static final int NEW_STANDARD_ROUND = 1;
    private DrawerLayout mDrawerLayout;
    private ArrayList<StandardRound> list;
    private final CheckBox[] clubs = new CheckBox[9];
    private RadioGroup location;
    private RadioGroup unit;
    private RadioGroup typ;
    private StandardRound currentSelection;


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDrawerLayout = (DrawerLayout) getView().findViewById(R.id.drawer_layout);
        mSelector.setSelectable(true);
        currentSelection = (StandardRound) getArguments().getSerializable(ITEM);
        list = new StandardRoundDataSource(getContext()).getAll();
        if (!list.contains(currentSelection)) {
            list.add(currentSelection);
        }
        setHasOptionsMenu(true);
        initFilter();
    }

    private void initFilter() {
        location = (RadioGroup) getView().findViewById(R.id.location);
        unit = (RadioGroup) getView().findViewById(R.id.unit);
        typ = (RadioGroup) getView().findViewById(R.id.round_typ);
        getClubs();

        // Set default values
        RoundTemplate firstRound = currentSelection.getRounds().get(0);
        setLocation();
        setMeasurementType(firstRound);
        setRoundType(firstRound);
        setInitialFilterMask();
        updateFilter();

        // Listen for filter setting changes
        for (CheckBox club : clubs) {
            club.setOnCheckedChangeListener((buttonView, isChecked) -> updateFilter());
        }
        location.setOnCheckedChangeListener((group, checkedId) -> updateFilter());
        unit.setOnCheckedChangeListener((group, checkedId) -> updateFilter());
        typ.setOnCheckedChangeListener((group, checkedId) -> updateFilter());
    }

    private void setLocation() {
        RadioButton outdoor = (RadioButton) getView().findViewById(R.id.outdoor);
        RadioButton indoor = (RadioButton) getView().findViewById(R.id.indoor);
        indoor.setChecked(currentSelection.indoor);
        outdoor.setChecked(!currentSelection.indoor);
    }

    private void setMeasurementType(RoundTemplate firstRound) {
        RadioButton metric = (RadioButton) getView().findViewById(R.id.metric);
        RadioButton imperial = (RadioButton) getView().findViewById(R.id.imperial);
        if (firstRound.distance.unit.equals(Dimension.METER)) {
            metric.setChecked(true);
        } else {
            imperial.setChecked(true);
        }
    }

    private void setRoundType(RoundTemplate firstRound) {
        RadioButton target = (RadioButton) getView().findViewById(R.id.target);
        RadioButton field = (RadioButton) getView().findViewById(R.id.field);
        RadioButton threeD = (RadioButton) getView().findViewById(R.id.three_d);
        if (firstRound.target.isFieldTarget()) {
            field.setChecked(true);
        } else if (firstRound.target.is3DTarget()) {
            threeD.setChecked(true);
        } else {
            target.setChecked(true);
        }
    }

    private void setInitialFilterMask() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int filterMask = prefs.getInt("filter_club", 0x1FF);
        filterMask |= currentSelection.club;
        for (int i = 0; i < clubs.length; i++) {
            clubs[i].setChecked((1 << i & filterMask) != 0);
        }
    }

    private void getClubs() {
        clubs[0] = (CheckBox) getView().findViewById(R.id.asa);
        clubs[1] = (CheckBox) getView().findViewById(R.id.aussie);
        clubs[2] = (CheckBox) getView().findViewById(R.id.gnas);
        clubs[3] = (CheckBox) getView().findViewById(R.id.ifaa);
        clubs[4] = (CheckBox) getView().findViewById(R.id.nasp);
        clubs[5] = (CheckBox) getView().findViewById(R.id.nfaa);
        clubs[6] = (CheckBox) getView().findViewById(R.id.nfas);
        clubs[7] = (CheckBox) getView().findViewById(R.id.wa);
        clubs[8] = (CheckBox) getView().findViewById(R.id.custom);
    }

    private void updateFilter() {
        int filter = getFilter();
        List<StandardRound> displayList = getFilteredStandardRound(filter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor e = prefs.edit();
        e.putInt("filter_club", filter);
        e.apply();

        setList(displayList, new StandardRoundAdapter());
        int position = displayList.indexOf(currentSelection);
        if (position > -1) {
            mSelector.setSelected(position, currentSelection.getId(), true);
            mRecyclerView.scrollToPosition(position);
        } else {
            mSelector.clearSelections();
        }
    }

    private List<StandardRound> getFilteredStandardRound(int filter) {
        ArrayList<StandardRound> displayList = new ArrayList<>();
        boolean indoor = location.getCheckedRadioButtonId() == R.id.indoor;
        boolean isMetric = unit.getCheckedRadioButtonId() == R.id.metric;
        String unitDistance = isMetric ? Distance.METER : Distance.YARDS;
        for (StandardRound r : list) {
            if (((r.club & filter) != 0 ||
                    r.name.startsWith("NFAA/IFAA") && (filter & StandardRound.IFAA) != 0) &&
                    r.getRounds().get(0).distance.unit.equals(unitDistance) &&
                    r.indoor == indoor) {
                int checked = typ.getCheckedRadioButtonId();
                Target target = r.getRounds().get(0).target;
                if ((checked != R.id.field || target.isFieldTarget()) &&
                        (checked != R.id.three_d || target.is3DTarget())) {
                    displayList.add(r);
                }
            }
        }
        return displayList;
    }

    private int getFilter() {
        int filter = 0;
        for (int i = 0; i < clubs.length; i++) {
            filter |= (clubs[i].isChecked() ? 1 : 0) << i;
        }
        return filter;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_standard_round_selection;
    }

    @Override
    public void onClick(SelectableViewHolder holder, StandardRound mItem) {
        if (currentSelection.equals(mItem)) {
            onSaveItem();
            return;
        }
        int oldSelectedPosition = mSelector.getSelectedPosition();
        currentSelection = mItem;
        int position = holder.getAdapterPosition();
        mSelector.setSelected(position, currentSelection.getId(), true);
        mAdapter.notifyItemChanged(oldSelectedPosition);
        mAdapter.notifyItemChanged(position);
    }

    @Override
    public void onLongClick(SelectableViewHolder holder) {
        StandardRound item = (StandardRound) holder.getItem();
        if (item.club == StandardRound.CUSTOM) {
            Intent i = new Intent(getActivity(),
                    SimpleFragmentActivity.EditStandardRoundActivity.class);
            i.putExtra(ITEM, item);
            getActivity().startActivityForResult(i, NEW_STANDARD_ROUND);
            getActivity().overridePendingTransition(R.anim.right_in, R.anim.left_out);
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.use_as_template)
                    .setMessage(R.string.create_copy)
                    .setIcon(null)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        Intent i = new Intent(getActivity(),
                                SimpleFragmentActivity.EditStandardRoundActivity.class);
                        i.putExtra(ITEM, item);
                        getActivity().startActivityForResult(i, NEW_STANDARD_ROUND);
                        getActivity().overridePendingTransition(R.anim.right_in, R.anim.left_out);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.filter, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null && item.getItemId() == R.id.action_filter) {
            if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                mDrawerLayout.closeDrawer(Gravity.RIGHT);
            } else {
                mDrawerLayout.openDrawer(Gravity.RIGHT);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        startActivityForResult(
                new Intent(getActivity(), SimpleFragmentActivity.EditStandardRoundActivity.class),
                NEW_STANDARD_ROUND);
        getActivity().overridePendingTransition(R.anim.right_in, R.anim.left_out);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == NEW_STANDARD_ROUND) {
            getActivity().setResult(resultCode, data);
            getActivity().onBackPressed();
        }
    }

    protected class StandardRoundAdapter extends NowListAdapter<StandardRound> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_standard_round, parent, false);
            return new ViewHolder(itemView);
        }
    }

    public class ViewHolder extends SelectableViewHolder<StandardRound> {
        private final TextView mName;
        private final ImageView mImage;
        private final TextView mDetails;

        public ViewHolder(View itemView) {
            super(itemView, mSelector, StandardRoundFragment.this);
            mImage = (ImageView) itemView.findViewById(R.id.image);
            mName = (TextView) itemView.findViewById(android.R.id.text1);
            mDetails = (TextView) itemView.findViewById(android.R.id.text2);
        }

        @Override
        public void bindCursor() {
            mName.setText(mItem.name);

            if (mItem.equals(currentSelection)) {
                mImage.setVisibility(View.VISIBLE);
                mDetails.setVisibility(View.VISIBLE);
                mDetails.setText(mItem.getDescription(getActivity()));
                mImage.setImageDrawable(mItem.getTargetDrawable(getActivity()));
            } else {
                mImage.setVisibility(View.GONE);
                mDetails.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected StandardRound onSave() {
        return currentSelection;
    }
}
