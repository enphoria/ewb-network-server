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

package com.zepben.ewbnetworkserver.data;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.ewbnetworkserver.patch.PatchLayer;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Collections;

import static com.zepben.ewbnetworkserver.data.LargeNetworkTestData.LNG_LAT_1_1;
import static com.zepben.ewbnetworkserver.data.LargeNetworkTestData.LNG_LAT_2_1;
import static com.zepben.ewbnetworkserver.data.PatchCreators.*;
import static com.zepben.ewbnetworkserver.patch.PatchProperties.*;

@EverythingIsNonnullByDefault
public class AddAllTypesPatch {

    public static String patch() {
        int actionId = 0;

        JsonObject patch = createPatch(1,
            Arrays.asList(
                createPointFeature(LNG_LAT_1_1, defaultSwitchProperties("id1", "name1", PatchLayer.acr, ++actionId).put(OPERATING_VOLTAGE, "LV")),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id2", PatchLayer.annotationTextBuilder)),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id3", PatchLayer.builderConduit)),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id4", PatchLayer.capacitor)),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id5", PatchLayer.faultIndicators)),
                createLineFeature(Arrays.asList(LNG_LAT_1_1, LNG_LAT_2_1), defaultLineProperties("id6", PatchLayer.hvBusBars, ++actionId).put(OPERATING_VOLTAGE, "12700")),
                createLineFeature(Arrays.asList(LNG_LAT_1_1, LNG_LAT_2_1), defaultLineProperties("id7", PatchLayer.hvCables, ++actionId)),
                createPointFeature(LNG_LAT_1_1, defaultSwitchProperties("id8", "name8", PatchLayer.hvCircuitBreakers, ++actionId)),
                createLineFeature(Arrays.asList(LNG_LAT_1_1, LNG_LAT_2_1), defaultLineProperties("id9", PatchLayer.hvLines, ++actionId)),
                createPointFeature(LNG_LAT_1_1, defaultSwitchProperties("id10", "name10", PatchLayer.hvSwitches, ++actionId).put(NORMAL_STATE, "Open")),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id11", PatchLayer.hvSwitchBoard)),
                createLineFeature(Arrays.asList(LNG_LAT_1_1, LNG_LAT_2_1), defaultLineProperties("id12", PatchLayer.lvBusBars, ++actionId)),
                createLineFeature(Arrays.asList(LNG_LAT_1_1, LNG_LAT_2_1), defaultLineProperties("id13", PatchLayer.lvCables, ++actionId)),
                createLineFeature(Arrays.asList(LNG_LAT_1_1, LNG_LAT_2_1), defaultLineProperties("id14", PatchLayer.lvCablesService, ++actionId)),
                createPointFeature(LNG_LAT_1_1, defaultSwitchProperties("id15", "name15", PatchLayer.lvCircuitBreakers, ++actionId)),
                createLineFeature(Arrays.asList(LNG_LAT_1_1, LNG_LAT_2_1), defaultLineProperties("id16", PatchLayer.lvLines, ++actionId)),
                createPointFeature(LNG_LAT_1_1, defaultSwitchProperties("id17", "name17", PatchLayer.lvSwitches, ++actionId)),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id18", PatchLayer.lvSwitchBoard)),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id19", PatchLayer.poles)),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id20", PatchLayer.publicLights)),
                createPointFeature(LNG_LAT_1_1, defaultTransformerProperties("id21", "name21", PatchLayer.regulator, ++actionId).put(SECONDARY_VOLTAGE, "11kV")),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id22", PatchLayer.servicePits)),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id23", PatchLayer.substation)),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id24", PatchLayer.substationCoverage)),
                createPointFeature(LNG_LAT_1_1, createBaseProperties("id25", PatchLayer.supplyPoints)),
                createPointFeature(LNG_LAT_1_1, createTerminationProperties("id26", "id7", "id8")),
                createPointFeature(LNG_LAT_1_1, createTerminationProperties("id27", "id5", "id6")),
                createPointFeature(LNG_LAT_1_1, defaultTransformerProperties("id28", "name28", PatchLayer.transformers, ++actionId)),
                createPointFeature(LNG_LAT_1_1, defaultTransformerProperties("id29", "name29", PatchLayer.transformers, ++actionId).put(SECONDARY_VOLTAGE, "12700")),
                createPointFeature(LNG_LAT_1_1, defaultTransformerProperties("id30", "name30", PatchLayer.transformers, ++actionId).put(SECONDARY_VOLTAGE, "11kV")),
                createPointFeature(LNG_LAT_1_1, defaultSwitchProperties("id31", "name31", PatchLayer.txProt, ++actionId))
            ),
            Collections.emptyList()
        );

        return createPatchResponse(patch).encode();
    }

}
