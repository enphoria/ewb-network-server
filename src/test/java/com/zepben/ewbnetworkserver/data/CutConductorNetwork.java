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
import com.zepben.evolve.cim.iec61970.base.core.*;
import com.zepben.evolve.cim.iec61970.base.wires.AcLineSegment;
import com.zepben.evolve.cim.iec61970.base.wires.EnergySource;
import com.zepben.evolve.cim.iec61970.base.wires.Junction;
import com.zepben.evolve.services.network.NetworkService;
import com.zepben.evolve.services.network.tracing.Tracing;
import com.zepben.ewbnetworkserver.Services;
import com.zepben.ewbnetworkserver.patch.PatchLayer;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Collections;

import static com.zepben.ewbnetworkserver.PatchProcessor.unknownPerLengthSequenceImpedance;
import static com.zepben.ewbnetworkserver.PatchProcessor.unknownWireInfo;
import static com.zepben.ewbnetworkserver.TestObjectCreators.*;
import static com.zepben.ewbnetworkserver.data.PatchCreators.*;

@SuppressWarnings({"SameParameterValue", "ConstantConditions"})
@EverythingIsNonnullByDefault
public class CutConductorNetwork {

    public static final PositionPoint LNG_LAT_1_1 = new PositionPoint(1, 1);
    public static final PositionPoint LNG_LAT_2_1 = new PositionPoint(2, 1);
    public static final PositionPoint LNG_LAT_2_2 = new PositionPoint(2, 2);
    public static final PositionPoint LNG_LAT_3_1 = new PositionPoint(3, 1);
    public static final PositionPoint LNG_LAT_3_2 = new PositionPoint(3, 2);
    public static final PositionPoint LNG_LAT_35_15 = new PositionPoint(3.5, 1.5);
    public static final PositionPoint LNG_LAT_4_1 = new PositionPoint(4, 1);

    public static final PositionPoint LNG_LAT_15_05 = new PositionPoint(1.5, 0.5);
    public static final PositionPoint LNG_LAT_24_1 = new PositionPoint(2.4, 1);
    public static final PositionPoint LNG_LAT_26_1 = new PositionPoint(2.6, 1);

    //
    //            c1
    //  n1-----+-----+-----n2
    //         |     |
    //      c2 |     | c3
    //         |     |
    //         |     |
    //         n3    n4
    //
    public static Services toPatch() {
        Services services = new Services();
        NetworkService network = services.networkService();

        network.add(unknownWireInfo);

        GeographicalRegion business = createGeographicalRegion(network, "b1", "b1");
        SubGeographicalRegion region = createSubGeographicalRegion(network, business, "r1", "r1");
        Substation zone = createSubstation(network, region, "z1", "z1");

        EnergySource s0 = createEnergySource(network, "s0", PhaseCode.BC, null);

        Junction n1 = createJunction(network, "10000001", "n1", 2, PhaseCode.BC, locationOf(network, "10000001-loc", LNG_LAT_1_1));
        Junction n2 = createJunction(network, "10000002", "n2", 1, PhaseCode.BC, locationOf(network, "10000002-loc", LNG_LAT_4_1));
        Junction n3 = createJunction(network, "10000003", "n3", 1, PhaseCode.BC, locationOf(network, "10000003-loc", LNG_LAT_2_2));
        Junction n4 = createJunction(network, "10000004", "n4", 1, PhaseCode.BC, locationOf(network, "10000004-loc", LNG_LAT_3_2));

        AcLineSegment c1 = createAcLineSegment(network, "20000001", "c1", PhaseCode.BC, unknownWireInfo, locationOf(network, "20000001-loc", LNG_LAT_1_1, LNG_LAT_2_1, LNG_LAT_3_1, LNG_LAT_4_1));
        AcLineSegment c2 = createAcLineSegment(network, "20000002", "c2", PhaseCode.BC, unknownWireInfo, locationOf(network, "20000002-loc", LNG_LAT_2_1, LNG_LAT_2_2));
        AcLineSegment c3 = createAcLineSegment(network, "20000003", "c3", PhaseCode.BC, unknownWireInfo, locationOf(network, "20000003-loc", LNG_LAT_3_1, LNG_LAT_3_2));

        createTerminals(network, c1, 2, PhaseCode.BC);

        createFeeder(network, zone, "f001", "f001", n1.getTerminal(2));

        connect(network, s0.getTerminal(1), n1.getTerminal(1), "cn1");
        connect(network, n1.getTerminal(2), c1.getTerminal(1), "cn2");
        connect(network, c2.getTerminal(1), c1.getTerminal(2), "cn3");
        connect(network, c3.getTerminal(1), c1.getTerminal(3), "cn4");
        connect(network, n2.getTerminal(1), c1.getTerminal(4), "cn5");
        connect(network, n3.getTerminal(1), c2.getTerminal(2), "cn6");
        connect(network, n4.getTerminal(1), c3.getTerminal(2), "cn7");

        Tracing.setPhases().run(network);
        Tracing.assignEquipmentContainersToFeeders().run(network);

        return services;
    }

    //
    //      c1-1      c1-2
    //      _
    //  n1-/ \-+-- --+-\ /-n2
    //         |     |  -
    //      c2 |     | c3
    //         |     |
    //         |     |
    //         n3    n4
    //
    public static Services patched() {
        Services services = new Services();
        NetworkService network = services.networkService();

        BaseVoltage bv6600 = baseVoltageOf(network, 6600);

        network.add(unknownWireInfo);
        network.add(unknownPerLengthSequenceImpedance);

        GeographicalRegion business = createGeographicalRegion(network, "b1", "b1");
        SubGeographicalRegion region = createSubGeographicalRegion(network, business, "r1", "r1");
        Substation zone = createSubstation(network, region, "z1", "z1");

        EnergySource s0 = createEnergySource(network, "s0", PhaseCode.BC, null);

        Junction n1 = createJunction(network, "10000001", "n1", 2, PhaseCode.BC, locationOf(network, "10000001-loc", LNG_LAT_1_1));
        Junction n2 = createJunction(network, "10000002", "n2", 1, PhaseCode.BC, locationOf(network, "10000002-loc", LNG_LAT_4_1));
        Junction n3 = createJunction(network, "10000003", "n3", 1, PhaseCode.BC, locationOf(network, "10000003-loc", LNG_LAT_2_2));
        Junction n4 = createJunction(network, "10000004", "n4", 1, PhaseCode.BC, locationOf(network, "10000004-loc", LNG_LAT_3_2));

        AcLineSegment c11 = createAcLineSegment(network, "20000001-1", "20000001-1", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000001-1-loc", LNG_LAT_1_1, LNG_LAT_15_05, LNG_LAT_2_1, LNG_LAT_24_1));
        AcLineSegment c12 = createAcLineSegment(network, "20000001-2", "20000001-2", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000001-2-loc", LNG_LAT_4_1, LNG_LAT_35_15, LNG_LAT_3_1, LNG_LAT_26_1));
        AcLineSegment c2 = createAcLineSegment(network, "20000002", "c2", PhaseCode.BC, unknownWireInfo, locationOf(network, "20000002-loc", LNG_LAT_2_1, LNG_LAT_2_2));
        AcLineSegment c3 = createAcLineSegment(network, "20000003", "c3", PhaseCode.BC, unknownWireInfo, locationOf(network, "20000003-loc", LNG_LAT_3_1, LNG_LAT_3_2));

        // Patch changes
        c11.setBaseVoltage(bv6600);
        createTerminals(network, c11, 1, PhaseCode.ABC);
        c11.setPerLengthSequenceImpedance(unknownPerLengthSequenceImpedance);
        c12.setBaseVoltage(bv6600);
        createTerminals(network, c12, 1, PhaseCode.ABC);
        c12.setPerLengthSequenceImpedance(unknownPerLengthSequenceImpedance);

        createFeeder(network, zone, "f001", "f001", n1.getTerminal(2));

        connect(network, s0.getTerminal(1), n1.getTerminal(1), "cn1");
        connect(network, n1.getTerminal(2), c11.getTerminal(1), "cn2");
        connect(network, c2.getTerminal(1), c11.getTerminal(2), "cn3");
        connect(network, c3.getTerminal(1), c12.getTerminal(2), "cn4");
        connect(network, n2.getTerminal(1), c12.getTerminal(1), "cn5");
        connect(network, n3.getTerminal(1), c2.getTerminal(2), "cn6");
        connect(network, n4.getTerminal(1), c3.getTerminal(2), "cn7");

        Tracing.setPhases().run(network);
        Tracing.assignEquipmentContainersToFeeders().run(network);

        return services;
    }

    //
    // From:
    //
    //            c1
    //  n1-----+-----+-----n2
    //         |     |
    //      c2 |     | c3
    //         |     |
    //         |     |
    //         n3    n4
    //
    // To:
    //
    //      c1-1      c1-2
    //      _
    //  n1-/ \-+-- --+-\ /-n2
    //         |     |  -
    //      c2 |     | c3
    //         |     |
    //         |     |
    //         n3    n4
    //
    public static String patch() {
        int actionId = 1;

        JsonObject patch1 = createPatch(1,
            Arrays.asList(
                createLineFeature(Arrays.asList(LNG_LAT_1_1, LNG_LAT_15_05, LNG_LAT_2_1, LNG_LAT_24_1), defaultLineProperties("20000001-1", PatchLayer.hvLines, actionId)),
                createLineFeature(Arrays.asList(LNG_LAT_26_1, LNG_LAT_3_1, LNG_LAT_35_15, LNG_LAT_4_1), defaultLineProperties("20000001-2", PatchLayer.hvLines, actionId))
            ),
            Collections.singletonList(
                createLineFeature(Collections.emptyList(), defaultLineProperties("20000001", PatchLayer.hvLines, actionId))
            )
        );

        return createPatchResponse(patch1).encode();
    }

}
