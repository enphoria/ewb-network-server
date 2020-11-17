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

import com.google.errorprone.annotations.FormatMethod;
import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.cimbend.cim.iec61970.base.core.ConductingEquipment;
import com.zepben.cimbend.cim.iec61970.base.core.IdentifiedObject;

import java.util.*;

@EverythingIsNonnullByDefault
public
class PatchResult {

    private final int patchId;
    private final Set<String> affectedNormalFeeders = new HashSet<>();
    private final Set<String> affectedNormalFeedersView = Collections.unmodifiableSet(affectedNormalFeeders);

    private final Set<String> affectedCurrentFeeders = new HashSet<>();
    private final Set<String> affectedCurrentFeedersView = Collections.unmodifiableSet(affectedCurrentFeeders);

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    private final List<String> errorsView = Collections.unmodifiableList(errors);
    private final List<String> warningsView = Collections.unmodifiableList(warnings);

    public PatchResult(int patchId) {
        this.patchId = patchId;
    }

    public int patchId() {
        return patchId;
    }

    public Set<String> affectedNormalFeeders() {
        return affectedNormalFeedersView;
    }

    public Set<String> affectedCurrentFeeders() {
        return affectedCurrentFeedersView;
    }

    public List<String> errors() {
        return errorsView;
    }

    public List<String> warnings() {
        return warningsView;
    }

    public PatchResult addAffectedFeedersFromAsset(ConductingEquipment asset) {
        asset.getNormalFeeders().stream().map(IdentifiedObject::getMRID).forEach(affectedNormalFeeders::add);
        asset.getCurrentFeeders().stream().map(IdentifiedObject::getMRID).forEach(affectedCurrentFeeders::add);
        return this;
    }

    @FormatMethod
    public PatchResult addError(String error, Object... args) {
        errors.add(String.format(error, args));
        return this;
    }

    @FormatMethod
    public PatchResult addWarning(String warning, Object... args) {
        warnings.add(String.format(warning, args));
        return this;
    }

}
