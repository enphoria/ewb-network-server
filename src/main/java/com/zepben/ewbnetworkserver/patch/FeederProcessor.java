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
import com.zepben.cimbend.cim.iec61970.base.core.ConductingEquipment;
import com.zepben.cimbend.cim.iec61970.base.core.Equipment;
import com.zepben.cimbend.cim.iec61970.base.core.Feeder;
import com.zepben.cimbend.cim.iec61970.base.core.PhaseCode;
import com.zepben.cimbend.network.tracing.PhaseStep;
import com.zepben.cimbend.network.tracing.Tracing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

@EverythingIsNonnullByDefault
public class FeederProcessor {

    public void applyDownstream(ConductingEquipment asset) {
        Tracing.normalDownstreamTrace()
            .addStepAction((phaseStep, isStopping) -> addNormalFeeders(phaseStep.conductingEquipment(), asset.getNormalFeeders()))
            .run(PhaseStep.startAt(asset, PhaseCode.ABCN));

        Tracing.currentDownstreamTrace()
            .addStepAction((phaseStep, isStopping) -> addCurrentFeeders(phaseStep.conductingEquipment(), asset.getCurrentFeeders()))
            .run(PhaseStep.startAt(asset, PhaseCode.ABCN));
    }

    public void removeDownstream(ConductingEquipment asset) {
        Tracing.normalDownstreamTrace()
            .addStepAction((a, s) -> removeNormalFeeders(a))
            .run(PhaseStep.startAt(asset, PhaseCode.ABCN));

        Tracing.currentDownstreamTrace()
            .addStepAction((a, s) -> removeCurrentFeeders(a))
            .run(PhaseStep.startAt(asset, PhaseCode.ABCN));
    }

    private void addNormalFeeders(ConductingEquipment asset, Collection<Feeder> normalFeeders) {
        addFeeders(asset, normalFeeders, Equipment::addContainer, Feeder::addEquipment);
    }

    private void addCurrentFeeders(ConductingEquipment asset, Collection<Feeder> currentFeeders) {
        addFeeders(asset, currentFeeders, Equipment::addCurrentFeeder, Feeder::addCurrentEquipment);
    }

    private void addFeeders(ConductingEquipment asset,
                            Collection<Feeder> feeders,
                            BiConsumer<ConductingEquipment, Feeder> assetFeederAdder,
                            BiConsumer<Feeder, ConductingEquipment> feederAssetAdder) {
        feeders.forEach(feeder -> {
            assetFeederAdder.accept(asset, feeder);
            feederAssetAdder.accept(feeder, asset);
        });
    }

    private void removeNormalFeeders(PhaseStep phaseStep) {
        removeFeeders(phaseStep.conductingEquipment(), Equipment::getNormalFeeders, Equipment::removeContainer, Feeder::removeEquipment);
    }

    private void removeCurrentFeeders(PhaseStep phaseStep) {
        removeFeeders(phaseStep.conductingEquipment(), Equipment::getCurrentFeeders, Equipment::removeCurrentFeeder, Feeder::removeCurrentEquipment);
    }

    private void removeFeeders(ConductingEquipment asset,
                               Function<ConductingEquipment, Collection<Feeder>> assetFeederExtractor,
                               BiConsumer<ConductingEquipment, Feeder> assetFeederRemover,
                               BiConsumer<Feeder, ConductingEquipment> feederAssetRemover) {
        new ArrayList<>(assetFeederExtractor.apply(asset)).forEach(feeder -> {
            assetFeederRemover.accept(asset, feeder);
            feederAssetRemover.accept(feeder, asset);
        });
    }

}
