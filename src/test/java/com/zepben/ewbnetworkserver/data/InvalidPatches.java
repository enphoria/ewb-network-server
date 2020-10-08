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
import static com.zepben.ewbnetworkserver.data.PatchCreators.*;
import static com.zepben.ewbnetworkserver.patch.PatchProperties.*;

@EverythingIsNonnullByDefault
public class InvalidPatches {

    public static String unknownValuesPatch() {
        int actionId = 0;

        JsonObject patch = createPatch(1,
            Arrays.asList(
                createPointFeature(LNG_LAT_1_1,
                    defaultSwitchProperties("id1", "name1", PatchLayer.acr, ++actionId)
                        .put(NORMAL_STATE, "unknown1")
                        .put(OPERATING_VOLTAGE, "unknown2")),
                createPointFeature(LNG_LAT_1_1,
                    defaultTransformerProperties("id2", "name2", PatchLayer.transformers, ++actionId)
                        .put(PRIMARY_VOLTAGE, "unknown3")
                        .put(SECONDARY_VOLTAGE, "unknown4")),
                createLineFeature(Arrays.asList(LNG_LAT_1_1, LNG_LAT_1_1),
                    defaultLineProperties("id3", PatchLayer.hvBusBars, ++actionId)
                        .put(OPERATING_VOLTAGE, "unknown5"))
            ),
            Collections.emptyList()
        );

        return createPatchResponse(patch).encode();
    }

    public static String unknownLayersPatch() {
        int actionId = 0;

        JsonObject patch = createPatch(1,
            Arrays.asList(
                createPointFeature(LNG_LAT_1_1,
                    defaultSwitchProperties("id1", "name1", PatchLayer.acr, ++actionId)
                        .put(LAYER_ID, "SOME_LAYER_I_DONT_KNOW")),
                createLineFeature(Arrays.asList(LNG_LAT_1_1, LNG_LAT_1_1),
                    defaultLineProperties("id2", PatchLayer.hvBusBars, ++actionId)
                        .put(LAYER_ID, "SOME_OTHER_LAYER"))
            ),
            Collections.emptyList()
        );

        return createPatchResponse(patch).encode();
    }

}
