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
import com.zepben.cimbend.cim.iec61970.base.core.PhaseCode;
import com.zepben.cimbend.cim.iec61970.base.wires.AcLineSegment;
import com.zepben.cimbend.cim.iec61970.base.wires.Breaker;
import com.zepben.cimbend.network.NetworkService;
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
public class ExtraTerminationNetwork {

    public static final PositionPoint LNG_LAT_1_1 = new PositionPoint(1, 1);
    public static final PositionPoint LNG_LAT_2_1 = new PositionPoint(2, 1);
    public static final PositionPoint LNG_LAT_2_11 = new PositionPoint(2, 1.1);
    public static final PositionPoint LNG_LAT_2_12 = new PositionPoint(2, 1.2);
    public static final PositionPoint LNG_LAT_2_13 = new PositionPoint(2, 1.3);
    public static final PositionPoint LNG_LAT_2_2 = new PositionPoint(2, 2);
    public static final PositionPoint LNG_LAT_3_1 = new PositionPoint(3, 1);

    //
    //    c1           c2
    //  -----   b1   -----
    //
    //          |
    //          | c3
    //          |
    //
    public static Services toPatch() {
        Services services = new Services();
        NetworkService network = services.networkService();

        network.add(unknownWireInfo);

        createBreaker(network, "10000001", "b1", 2, PhaseCode.ABC, locationOf(network, "10000001-loc", LNG_LAT_2_1), null);

        createAcLineSegment(network, "20000001", "c1", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000001-loc", LNG_LAT_1_1, LNG_LAT_2_11));
        createAcLineSegment(network, "20000002", "c2", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000002-loc", LNG_LAT_2_12, LNG_LAT_3_1));
        createAcLineSegment(network, "20000003", "c3", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000003-loc", LNG_LAT_2_13, LNG_LAT_2_2));

        return services;
    }

    //
    //    c1           c2
    //  --------b1--------
    //
    //          |
    //          | c3
    //          |
    //
    public static Services patched() {
        Services services = new Services();
        NetworkService network = services.networkService();

        network.add(unknownWireInfo);
        network.add(unknownPerLengthSequenceImpedance);

        Breaker b1 = createBreaker(network, "10000001", "b1", 2, PhaseCode.ABC, locationOf(network, "10000001-loc", LNG_LAT_2_1), null);

        AcLineSegment c1 = createAcLineSegment(network, "20000001", "c1", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000001-loc", LNG_LAT_1_1, LNG_LAT_2_11));
        AcLineSegment c2 = createAcLineSegment(network, "20000002", "c2", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000002-loc", LNG_LAT_2_12, LNG_LAT_3_1));
        AcLineSegment c3 = createAcLineSegment(network, "20000003", "c3", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000003-loc", LNG_LAT_2_13, LNG_LAT_2_2));

        connect(network, b1.getTerminal(1), c1.getTerminal(2), "30000001");
        connect(network, b1.getTerminal(2), c2.getTerminal(1), "30000002");
        network.connect(c3.getTerminal(1), "30000003");

        return services;
    }

    //
    // From:
    //
    //    c1           c2
    //  -----   b1   -----
    //
    //          |
    //          | c3
    //          |
    //
    // To:
    //
    //    c1           c2
    //  --------b1--------
    //
    //          |
    //          | c3
    //          |
    //
    // NOTE: will also attempt to connect c3 to b1, but it will fail.
    //
    public static String patch() {
        JsonObject patch1 = createPatch(1,
            Arrays.asList(
                createPointFeature(LNG_LAT_2_11, createTerminationProperties("30000001", "10000001", "20000001")),
                createPointFeature(LNG_LAT_2_12, createTerminationProperties("30000002", "10000001", "20000002")),
                createPointFeature(LNG_LAT_2_13, createTerminationProperties("30000003", "10000001", "20000003"))
            ),
            Collections.emptyList()
        );

        return createPatchResponse(patch1).encode();
    }

}
