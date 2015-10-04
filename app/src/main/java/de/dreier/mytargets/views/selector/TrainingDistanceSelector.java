/*
 * MyTargets Archery
 *
 * Copyright (C) 2015 Florian Dreier
 * All rights reserved
 */

package de.dreier.mytargets.views.selector;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import de.dreier.mytargets.R;
import de.dreier.mytargets.shared.models.Distance;

public class TrainingDistanceSelector extends DistanceSelectorBase {

    public TrainingDistanceSelector(Context context) {
        this(context, null);
    }

    public TrainingDistanceSelector(Context context, AttributeSet attrs) {
        super(context, attrs, R.layout.item_training_distance);
    }
}
