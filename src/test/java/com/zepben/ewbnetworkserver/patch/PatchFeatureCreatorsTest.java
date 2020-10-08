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

import com.zepben.cimbend.cim.iec61970.base.core.PhaseCode;
import com.zepben.cimbend.cim.iec61970.base.wires.PowerTransformer;
import com.zepben.ewbnetworkserver.Services;
import com.zepben.ewbnetworkserver.data.LoadDataJson;
import com.zepben.ewbnetworkserver.data.ReplaceObjectsNetwork;
import com.zepben.ewbnetworkserver.geojson.GeoJson;
import com.zepben.testutils.junit.SystemLogExtension;
import com.zepben.vertxutils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@SuppressWarnings("ConstantConditions")
class PatchFeatureCreatorsTest {

    @RegisterExtension
    SystemLogExtension systemOut = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private final Services services = new Services();
    private final LoadManipulations loadManipulations = mock(LoadManipulations.class);
    private final PatchFeatureCreators patchFeatureCreators = new PatchFeatureCreators(services, loadManipulations);
    private final Map<String, PhaseCode> assetPhases = new HashMap<>();
    private final PatchResult patchResult = new PatchResult(1);

    //
    // NOTE: There are a heap of tests that should be moved here that are still located in the patch processor. Ideally
    //       they would be converted to tests here with check that they are passed correctly from the patch processor.
    //

    @Test
    void extractsLoadDataForTransformers() throws JsonUtils.ParsingException {
        patchFeatureCreators.addTransformer(GeoJson.parse(LoadDataJson.transformer()), assetPhases, patchResult);

        verify(loadManipulations, times(2)).add(any(), any(), any(), anyDouble(), anyDouble());

        verify(loadManipulations, times(1)).add("tx", LoadOperation.Added, LoadType.Load, 1.1, 2.2);
        verify(loadManipulations, times(1)).add("tx", LoadOperation.Removed, LoadType.Load, 3.3, 4.4);
    }

    @Test
    void extractsLoadDataForSupplyPoints() throws JsonUtils.ParsingException {
        patchFeatureCreators.addSupplyPoint(GeoJson.parse(LoadDataJson.supplyPoint()), assetPhases, patchResult);

        verify(loadManipulations, times(2)).add(any(), any(), any(), anyDouble(), anyDouble());

        verify(loadManipulations, times(1)).add("sp", LoadOperation.Added, LoadType.Generation, 5.5, 6.6);
        verify(loadManipulations, times(1)).add("sp", LoadOperation.Removed, LoadType.Generation, 7.7, 8.8);
    }

    @Test
    void replacesExistingObjects() throws JsonUtils.ParsingException {
        ReplaceObjectsNetwork.initialiseToPatch(services);

        patchFeatureCreators.addTransformer(GeoJson.parse(ReplaceObjectsNetwork.replaceTxGeoJson()), assetPhases, patchResult);

        PowerTransformer tx1 = services.networkService().get(PowerTransformer.class, "tx1");
        PowerTransformer tx2 = services.networkService().get(PowerTransformer.class, "tx2");

        assertThat(tx2.getUsagePoints(), equalTo(tx1.getUsagePoints()));
        assertThat(tx2.getContainers(), equalTo(tx1.getContainers()));
        assertThat(tx2.getCurrentFeeders(), equalTo(tx1.getCurrentFeeders()));
        assertThat(tx2.getOperationalRestrictions(), equalTo(tx1.getOperationalRestrictions()));

        assertThat(tx2.numTerminals(), greaterThanOrEqualTo(tx1.numTerminals()));
        for (int i = 0; i < tx1.numTerminals(); ++i)
            assertThat(tx2.getTerminals().get(i).connectivityNodeId(), equalTo(tx1.getTerminals().get(i).connectivityNodeId()));
    }

}
