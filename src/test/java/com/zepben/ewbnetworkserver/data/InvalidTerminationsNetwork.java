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
import com.zepben.evolve.cim.iec61968.common.PositionPoint;
import com.zepben.evolve.cim.iec61970.base.core.PhaseCode;
import com.zepben.evolve.cim.iec61970.base.wires.Junction;
import com.zepben.evolve.services.network.NetworkService;
import com.zepben.ewbnetworkserver.Services;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Collections;

import static com.zepben.ewbnetworkserver.PatchProcessor.unknownPerLengthSequenceImpedance;
import static com.zepben.ewbnetworkserver.PatchProcessor.unknownWireInfo;
import static com.zepben.ewbnetworkserver.TestObjectCreators.*;
import static com.zepben.ewbnetworkserver.data.PatchCreators.*;

@SuppressWarnings("ConstantConditions")
@EverythingIsNonnullByDefault
public class InvalidTerminationsNetwork {

    public static final PositionPoint LNG_LAT_1_1 = new PositionPoint(1, 1);
    public static final PositionPoint LNG_LAT_2_1 = new PositionPoint(2, 1);

    //
    //    j1   j2
    //
    public static Services toPatch() {
        Services services = new Services();
        NetworkService network = services.networkService();

        createJunction(network, "10000001", "j1", 2, PhaseCode.ABC, locationOf(network, "10000001-loc", LNG_LAT_1_1));
        createJunction(network, "10000002", "j2", 2, PhaseCode.ABC, locationOf(network, "10000002-loc", LNG_LAT_2_1));

        return services;
    }

    //
    //    j1   j2
    //
    public static Services patched() {
        Services services = new Services();
        NetworkService network = services.networkService();

        network.add(unknownWireInfo);
        network.add(unknownPerLengthSequenceImpedance);

        Junction j1 = createJunction(network, "10000001", "j1", 2, PhaseCode.ABC, locationOf(network, "10000001-loc", LNG_LAT_1_1));
        Junction j2 = createJunction(network, "10000002", "j2", 2, PhaseCode.ABC, locationOf(network, "10000002-loc", LNG_LAT_2_1));

        connect(network, j1.getTerminal(1), j2.getTerminal(1), "30000001");

        return services;
    }

    //
    // From:
    //
    //    j1   j2
    //
    //
    // To:
    //
    //    j1   j2
    //
    // NOTE: will also attempt to connect c3 to b1, but it will fail.
    //
    public static String patch() {
        JsonObject patch1 = createPatch(1,
            Arrays.asList(
                createPointFeature(LNG_LAT_1_1, createTerminationProperties("30000001", "10000001", "10000002")),
                createPointFeature(LNG_LAT_2_1, createTerminationProperties("30000002", "10000002", "30000001"))
            ),
            Collections.emptyList()
        );

        return createPatchResponse(patch1).encode();
    }

}
