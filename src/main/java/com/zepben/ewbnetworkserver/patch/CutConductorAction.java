/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 * This file is part of ewb-network-server.
 *
 * ewb-network-server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ewb-network-server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with ewb-network-server.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zepben.ewbnetworkserver.patch;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@EverythingIsNonnullByDefault
public class CutConductorAction {

    private final String actionGroup;
    private final List<PatchFeature> addFeatures = new ArrayList<>();
    private final List<PatchFeature> removeFeatures = new ArrayList<>();

    @Nullable private PatchFeature addLineFeature1;
    @Nullable private PatchFeature addLineFeature2;
    @Nullable private PatchFeature removeLineFeature;

    private boolean hasExcessFeatures = false;

    public CutConductorAction(String actionGroup) {
        this.actionGroup = actionGroup;
    }

    public boolean isValid() {
        return !hasExcessFeatures
            && (addLineFeature2 != null)
            && (removeLineFeature != null);
    }

    public String actionGroup() {
        return actionGroup;
    }

    public PatchFeature addLineFeature1() {
        return Objects.requireNonNull(addLineFeature1);
    }

    public PatchFeature addLineFeature2() {
        return Objects.requireNonNull(addLineFeature2);
    }

    public PatchFeature removeLineFeature() {
        return Objects.requireNonNull(removeLineFeature);
    }

    public List<PatchFeature> addFeatures() {
        return addFeatures;
    }

    public List<PatchFeature> removeFeatures() {
        return removeFeatures;
    }

    public void includeAddFeature(PatchFeature feature) {
        addFeatures.add(feature);

        switch (feature.geoJson().patchLayer()) {
            case hvBusBars:
            case hvCables:
            case hvLines:
                if (addLineFeature1 == null)
                    addLineFeature1 = feature;
                else if (addLineFeature2 == null)
                    addLineFeature2 = feature;
                else
                    hasExcessFeatures = true;
                break;
            case terminations:
                break;
            default:
                hasExcessFeatures = true;
        }
    }

    public void includeRemoveFeature(PatchFeature feature) {
        removeFeatures().add(feature);

        switch (feature.geoJson().patchLayer()) {
            case hvBusBars:
            case hvCables:
            case hvLines:
                if (removeLineFeature == null)
                    removeLineFeature = feature;
                else
                    hasExcessFeatures = true;
                break;
            case terminations:
                break;
            default:
                hasExcessFeatures = true;
        }
    }

}
