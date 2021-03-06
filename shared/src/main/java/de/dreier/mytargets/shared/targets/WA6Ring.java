/*
 * MyTargets Archery
 *
 * Copyright (C) 2015 Florian Dreier
 * All rights reserved
 */
package de.dreier.mytargets.shared.targets;

import de.dreier.mytargets.shared.R;
import de.dreier.mytargets.shared.models.Dimension;

import static de.dreier.mytargets.shared.models.Dimension.Unit.CENTIMETER;
import static de.dreier.mytargets.shared.utils.Color.CERULEAN_BLUE;
import static de.dreier.mytargets.shared.utils.Color.DARK_GRAY;
import static de.dreier.mytargets.shared.utils.Color.FLAMINGO_RED;
import static de.dreier.mytargets.shared.utils.Color.LEMON_YELLOW;

public class WA6Ring extends TargetModelBase {

    public static final int ID = 1;

    public WA6Ring() {
        super(ID, R.string.wa_6_ring);
        zones = new Zone[]{
                new Zone(42, LEMON_YELLOW, DARK_GRAY, 3),
                new Zone(83, LEMON_YELLOW, DARK_GRAY, 3),
                new Zone(167, LEMON_YELLOW, DARK_GRAY, 3),
                new Zone(250, FLAMINGO_RED, DARK_GRAY, 3),
                new Zone(333, FLAMINGO_RED, DARK_GRAY, 3),
                new Zone(417, CERULEAN_BLUE, DARK_GRAY, 3),
                new Zone(500, CERULEAN_BLUE, DARK_GRAY, 3)
        };
        scoringStyles = new ScoringStyle[]{
                new ScoringStyle(true, 10, 10, 9, 8, 7, 6, 5),
                new ScoringStyle(false, 10, 9, 9, 8, 7, 6, 5),
                new ScoringStyle(false, 11, 10, 9, 8, 7, 6, 5),
                new ScoringStyle(true, 5, 5, 5, 4, 4, 3, 3),
                new ScoringStyle(false, 9, 9, 9, 7, 7, 5, 5)
        };
        diameters = new Dimension[]{
                new Dimension(40, CENTIMETER),
                new Dimension(60, CENTIMETER),
                new Dimension(80, CENTIMETER),
                new Dimension(92, CENTIMETER),
                new Dimension(122, CENTIMETER)
        };
        centerMark = new CenterMark(DARK_GRAY, 8.333f, 4, false);
    }
}
