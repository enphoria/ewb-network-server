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
import com.zepben.cimbend.cim.iec61968.common.PositionPoint;
import com.zepben.ewbnetworkserver.patch.LoadOperation;
import com.zepben.ewbnetworkserver.patch.LoadType;
import com.zepben.ewbnetworkserver.patch.PatchLayer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static com.zepben.ewbnetworkserver.data.PatchCreators.*;
import static com.zepben.ewbnetworkserver.patch.PatchLayer.transformers;
import static com.zepben.ewbnetworkserver.patch.PatchProperties.*;

@SuppressWarnings({"SameParameterValue"})
@EverythingIsNonnullByDefault
public class LoadDataJson {

    public static final PositionPoint LNG_LAT_1_1 = new PositionPoint(1, 1);

    //
    // tx - load: add 250kW, add 300kW, remove 15kW
    //      generation: add 10kW
    //
    //
    public static JsonObject transformer() {
        return createPointFeature(LNG_LAT_1_1,
            defaultTransformerProperties("tx", "tx", transformers, 1)
                .put(LOAD_DATA, new JsonArray()
                    .add(createLoadData(LoadOperation.Added, LoadType.Load, "1.1", 2.2))
                    .add(createLoadData(LoadOperation.Removed, LoadType.Load, "1.1 kVA(N) + 2.2kVA(E) = 3.3kVA", 4.4))
                )
        );
    }

    //
    // sp - load: remove 10kW load
    //      generation: add 7.5kW, remove 2kW
    //
    public static JsonObject supplyPoint() {
        return createPointFeature(LNG_LAT_1_1,
            createBaseProperties("sp", PatchLayer.supplyPoints)
                .put(LOAD_DATA, new JsonArray()
                    .add(createLoadData(LoadOperation.Added, LoadType.Generation, "1.1+4.4=5.5", 6.6))
                    .add(createLoadData(LoadOperation.Removed, LoadType.Generation, "7.7 kVA", 8.8))
                )
        );
    }

    private static JsonObject createLoadData(LoadOperation loadOperation, LoadType loadType, String quantity, double diversificationFactor) {
        return new JsonObject()
            .put(LOAD_OPERATION, loadOperation)
            .put(LOAD_TYPE, loadType)
            .put(LOAD_QUANTITY, quantity)
            .put(LOAD_DIVERSIFICATION_FACTOR, diversificationFactor);
    }

}
