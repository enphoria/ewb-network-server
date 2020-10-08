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
import com.zepben.cimbend.cim.iec61970.base.core.*;
import com.zepben.cimbend.cim.iec61970.base.wires.*;
import com.zepben.cimbend.network.NetworkService;
import com.zepben.cimbend.network.tracing.Tracing;
import com.zepben.ewbnetworkserver.Services;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Collections;

import static com.zepben.ewbnetworkserver.PatchProcessor.unknownPerLengthSequenceImpedance;
import static com.zepben.ewbnetworkserver.PatchProcessor.unknownWireInfo;
import static com.zepben.ewbnetworkserver.TestObjectCreators.*;
import static com.zepben.ewbnetworkserver.data.PatchCreators.*;
import static com.zepben.ewbnetworkserver.patch.PatchLayer.*;

@SuppressWarnings({"SameParameterValue", "ConstantConditions"})
@EverythingIsNonnullByDefault
public class LargeNetworkTestData {

    public static final PositionPoint LNG_LAT_1_1 = new PositionPoint(1, 1);
    public static final PositionPoint LNG_LAT_2_1 = new PositionPoint(2, 1);
    public static final PositionPoint LNG_LAT_2_2 = new PositionPoint(2, 2);
    public static final PositionPoint LNG_LAT_3_1 = new PositionPoint(3, 1);
    public static final PositionPoint LNG_LAT_3_2 = new PositionPoint(3, 2);
    public static final PositionPoint LNG_LAT_3_3 = new PositionPoint(3, 3);
    public static final PositionPoint LNG_LAT_35_15 = new PositionPoint(3.5, 1.5);
    public static final PositionPoint LNG_LAT_35_2 = new PositionPoint(3.5, 2);
    public static final PositionPoint LNG_LAT_4_1 = new PositionPoint(4, 1);
    public static final PositionPoint LNG_LAT_5_1 = new PositionPoint(5, 1);

    //
    //  fcb1======n2------n3------|cb4|
    //        c1   |  c2   |  c3
    //           c4|     c5|
    //             |       |
    //           |cb5|    f6
    //                     |
    //                   c6|
    //                     |
    //                    tx7
    //
    public static Services networkToPatch() {
        Services services = new Services();
        NetworkService network = services.networkService();

        network.add(unknownWireInfo);

        GeographicalRegion business = createGeographicalRegion(network, "b1", "b1");
        SubGeographicalRegion region = createSubGeographicalRegion(network, business, "r1", "r1");
        Substation zone = createSubstation(network, region, "z1", "z1");

        EnergySource s0 = createEnergySource(network, "s0", PhaseCode.ABC, null);

        Breaker fcb1 = createBreaker(network, "10000001", "fcb1", 2, PhaseCode.ABC, locationOf(network, "10000001-loc", LNG_LAT_1_1), zone);
        Junction n2 = createJunction(network, "10000002", "n2", 3, PhaseCode.ABC, locationOf(network, "10000002-loc", LNG_LAT_2_1));
        Junction n3 = createJunction(network, "10000003", "n3", 3, PhaseCode.ABC, locationOf(network, "10000003-loc", LNG_LAT_3_1));
        Breaker cb4 = createBreaker(network, "10000004", "cb4", 1, PhaseCode.ABC, locationOf(network, "10000004-loc", LNG_LAT_4_1), null);
        Breaker cb5 = createBreaker(network, "10000005", "cb5", 1, PhaseCode.ABC, true, true, locationOf(network, "10000005-loc", LNG_LAT_2_2), null);
        Fuse f6 = createFuse(network, "10000006", "f6", 2, PhaseCode.ABC, locationOf(network, "10000006-loc", LNG_LAT_3_2));
        PowerTransformer tx7 = createPowerTransformer(network, "10000007", "tx7", 1, PhaseCode.ABC, locationOf(network, "10000007-loc", LNG_LAT_3_3));

        AcLineSegment c1 = createAcLineSegment(network, "20000001", "c1", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000001-loc", LNG_LAT_1_1, LNG_LAT_2_1));
        AcLineSegment c2 = createAcLineSegment(network, "20000002", "c2", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000002-loc", LNG_LAT_2_1, LNG_LAT_3_1));
        AcLineSegment c3 = createAcLineSegment(network, "20000003", "c3", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000003-loc", LNG_LAT_3_1, LNG_LAT_4_1));
        AcLineSegment c4 = createAcLineSegment(network, "20000004", "c4", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000004-loc", LNG_LAT_2_1, LNG_LAT_2_2));
        AcLineSegment c5 = createAcLineSegment(network, "20000005", "c5", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000005-loc", LNG_LAT_3_1, LNG_LAT_3_2));
        AcLineSegment c6 = createAcLineSegment(network, "20000006", "c6", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000006-loc", LNG_LAT_3_2, LNG_LAT_3_3));

        createFeeder(network, zone, "f001", "f001", c1);

        connect(network, s0.getTerminal(1), fcb1.getTerminal(2), "cn1");
        connect(network, fcb1.getTerminal(1), c1.getTerminal(1), "cn2");
        connect(network, n2.getTerminal(1), c1.getTerminal(2), "cn3");
        connect(network, n2.getTerminal(2), c2.getTerminal(1), "cn4");
        connect(network, n3.getTerminal(1), c2.getTerminal(2), "cn5");
        connect(network, n3.getTerminal(2), c3.getTerminal(1), "cn6");
        connect(network, cb4.getTerminal(1), c3.getTerminal(2), "cn7");
        connect(network, n2.getTerminal(3), c4.getTerminal(1), "cn8");
        connect(network, cb5.getTerminal(1), c4.getTerminal(2), "cn9");
        connect(network, n3.getTerminal(3), c5.getTerminal(1), "cn10");
        connect(network, f6.getTerminal(1), c5.getTerminal(2), "cn11");
        connect(network, f6.getTerminal(2), c6.getTerminal(1), "cn12");
        connect(network, tx7.getTerminal(1), c6.getTerminal(2), "cn13");

        Tracing.setPhases().run(network);
        Tracing.assignEquipmentContainersToFeeders().run(network);

        return services;
    }

    //
    //  fcb1======n2------n3======cb4======cb8
    //        c1      c2   |  c3    \  c7
    //                   c5|         |c8
    //                     |         |
    //                   |f6|       tx9
    //                     |
    //                   c6|
    //                     |
    //                    tx7
    //
    public static Services patchedNetwork() {
        Services services = new Services();
        NetworkService network = services.networkService();

        BaseVoltage bv11000 = baseVoltageOf(network, 11000);
        BaseVoltage bv6600 = baseVoltageOf(network, 6600);
        BaseVoltage bv415 = baseVoltageOf(network, 415);

        network.add(unknownWireInfo);
        network.add(unknownPerLengthSequenceImpedance);

        GeographicalRegion business = createGeographicalRegion(network, "b1", "b1");
        SubGeographicalRegion region = createSubGeographicalRegion(network, business, "r1", "r1");
        Substation zone = createSubstation(network, region, "z1", "z1");

        EnergySource s0 = createEnergySource(network, "s0", PhaseCode.ABC, null);

        Breaker fcb1 = createBreaker(network, "10000001", "fcb1", 2, PhaseCode.ABC, locationOf(network, "10000001-loc", LNG_LAT_1_1), zone);
        Junction n2 = createJunction(network, "10000002", "n2", 3, PhaseCode.ABC, locationOf(network, "10000002-loc", LNG_LAT_2_1));
        Junction n3 = createJunction(network, "10000003", "n3", 3, PhaseCode.ABC, locationOf(network, "10000003-loc", LNG_LAT_3_1));
        Breaker cb4 = createBreaker(network, "10000004", "cb4", 2, PhaseCode.ABC, locationOf(network, "10000004-loc", LNG_LAT_4_1), null);
        Fuse f6 = createFuse(network, "10000006", "f6", 2, PhaseCode.ABC, locationOf(network, "10000006-loc", LNG_LAT_3_2));
        PowerTransformer tx7 = createPowerTransformer(network, "10000007", "tx7", 1, PhaseCode.ABC, locationOf(network, "10000007-loc", LNG_LAT_3_3));
        Disconnector cb8 = createDisconnector(network, "10000008", "cb8", 2, PhaseCode.ABC, locationOf(network, "10000008-loc", LNG_LAT_5_1), null);
        PowerTransformer tx9 = createPowerTransformer(network, "10000009", "tx9", 2, PhaseCode.ABC, locationOf(network, "10000009-loc", LNG_LAT_35_2));

        // Patch changes
        cb8.setBaseVoltage(bv6600);
        createPowerTransformerEnd(network, tx9, bv11000, 1);
        createPowerTransformerEnd(network, tx9, bv415, 2);

        AcLineSegment c1 = createAcLineSegment(network, "20000001", "c1", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000001-loc", LNG_LAT_1_1, LNG_LAT_2_1));
        AcLineSegment c2 = createAcLineSegment(network, "20000002", "c2", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000002-loc", LNG_LAT_2_1, LNG_LAT_3_1));
        AcLineSegment c3 = createAcLineSegment(network, "20000003", "c3", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000003-loc", LNG_LAT_3_1, LNG_LAT_4_1));
        AcLineSegment c5 = createAcLineSegment(network, "20000005", "c5", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000005-loc", LNG_LAT_3_1, LNG_LAT_3_2));
        AcLineSegment c6 = createAcLineSegment(network, "20000006", "c6", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000006-loc", LNG_LAT_3_2, LNG_LAT_3_3));
        AcLineSegment c7 = createAcLineSegment(network, "20000007", "20000007", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000007-loc", LNG_LAT_4_1, LNG_LAT_5_1));
        AcLineSegment c8 = createAcLineSegment(network, "20000008", "20000008", PhaseCode.ABC, unknownWireInfo, locationOf(network, "20000008-loc", LNG_LAT_4_1, LNG_LAT_35_15, LNG_LAT_35_2));

        // Patch changes
        c7.setBaseVoltage(bv6600);
        c7.setPerLengthSequenceImpedance(unknownPerLengthSequenceImpedance);
        c8.setBaseVoltage(bv6600);
        c8.setPerLengthSequenceImpedance(unknownPerLengthSequenceImpedance);

        createFeeder(network, zone, "f001", "f001", c1);

        connect(network, s0.getTerminal(1), fcb1.getTerminal(2), "cn1");
        connect(network, fcb1.getTerminal(1), c1.getTerminal(1), "cn2");
        connect(network, n2.getTerminal(1), c1.getTerminal(2), "cn3");
        connect(network, n2.getTerminal(2), c2.getTerminal(1), "cn4");
        connect(network, n3.getTerminal(1), c2.getTerminal(2), "cn5");
        connect(network, n3.getTerminal(2), c3.getTerminal(1), "cn6");
        connect(network, cb4.getTerminal(1), c3.getTerminal(2), "cn7");
        network.connect(n2.getTerminal(3), "cn8");
        connect(network, n3.getTerminal(3), c5.getTerminal(1), "cn10");
        connect(network, f6.getTerminal(1), c5.getTerminal(2), "cn11");
        connect(network, f6.getTerminal(2), c6.getTerminal(1), "cn12");
        connect(network, tx7.getTerminal(1), c6.getTerminal(2), "cn13");
        connect(network, cb4.getTerminal(2), c7.getTerminal(1), "t1");
        connect(network, cb8.getTerminal(1), c7.getTerminal(2), "t2");
        connect(network, cb4.getTerminal(2), c8.getTerminal(1), "t1"); // created from t3 but will reuse t1.
        connect(network, tx9.getTerminal(1), c8.getTerminal(2), "t4");

        Tracing.setPhases().run(network);
        Tracing.assignEquipmentContainersToFeeders().run(network);

        return services;
    }

    //
    // From:
    //
    //  fcb1======n2------n3------|cb4|
    //        c1   |  c2   |  c3
    //           c4|     c5|
    //             |       |
    //           |cb5|    f6
    //                     |
    //                   c6|
    //                     |
    //                    tx7
    //
    // To:
    //
    //  fcb1======n2------n3======cb4======cb8
    //        c1      c2   |  c3    \  c7
    //                   c5|         |c8
    //                     |         |
    //                   |f6|       tx9
    //                     |
    //                   c6|
    //                     |
    //                    tx7
    //
    public static String allOperationsPatch() {
        int actionId = 0;
        PositionPoint positionPoint = new PositionPoint(1, 2);

        JsonObject patch1 = createPatch(1,
            Collections.emptyList(),
            Arrays.asList(
                createPointFeature(LNG_LAT_2_2, defaultSwitchProperties("10000005", "", hvCircuitBreakers, ++actionId)),
                createPointFeature(positionPoint, defaultSwitchProperties("30000000", "", hvSwitches, ++actionId))
            )
        );

        JsonObject patch2 = createPatch(2,
            Collections.emptyList(),
            Collections.singletonList(
                createLineFeature(Collections.emptyList(), defaultLineProperties("20000004", hvLines, ++actionId))
            )
        );

        JsonObject patch3 = createPatch(3,
            Arrays.asList(
                createPointFeature(LNG_LAT_5_1, defaultSwitchProperties("10000008", "cb8", hvSwitches, ++actionId)),
                createLineFeature(Arrays.asList(LNG_LAT_4_1, LNG_LAT_5_1), defaultLineProperties("20000007", hvCables, ++actionId)),
                createPointFeature(LNG_LAT_4_1, createTerminationProperties("t1", "20000007", "10000004")),
                createPointFeature(LNG_LAT_5_1, createTerminationProperties("t2", "20000007", "10000008"))
            ),
            Collections.emptyList()
        );

        JsonObject patch4 = createPatch(4,
            Arrays.asList(
                createPointFeature(LNG_LAT_35_2, defaultTransformerProperties("10000009", "tx9", transformers, ++actionId)),
                createLineFeature(Arrays.asList(LNG_LAT_4_1, LNG_LAT_35_15, LNG_LAT_35_2), defaultLineProperties("20000008", hvLines, ++actionId)),
                createPointFeature(LNG_LAT_4_1, createTerminationProperties("t3", "20000008", "10000004")),
                createPointFeature(LNG_LAT_35_2, createTerminationProperties("t4", "20000008", "10000009")),
                createPointFeature(positionPoint, defaultSwitchProperties("30000000", "unordered test", hvSwitches, ++actionId))
            ),
            Collections.emptyList()
        );

        return createPatchResponse(patch3, patch2, patch1, patch4)
            .toString();
    }

    //
    // From:
    //
    //  fcb1======n2------n3======cb4======cb8
    //        c1      c2   |  c3 /     c7
    //                   c5|     |c8
    //                     |     |
    //                   |f6|   tx9
    //                     |
    //                   c6|
    //                     |
    //                    tx7
    //
    // To:
    //
    //  fcb1======n2------n3======cb4 ==== cb8
    //        c1      c2   |  c3       c7
    //                   c5|         |c8
    //                     |
    //                   |f6|       tx9
    //                     |
    //                   c6|
    //                     |
    //                    tx7
    //

    public static String removeTerminationsPatch() {
        JsonObject patch = createPatch(1,
            Collections.emptyList(),
            Arrays.asList(
                createPointFeature(LNG_LAT_4_1, createTerminationProperties("t1", "20000007", "10000004")),
                createPointFeature(LNG_LAT_5_1, createTerminationProperties("t2", "20000007", "10000008")),
                createPointFeature(LNG_LAT_4_1, createTerminationProperties("t3", "20000008", "10000004")),
                createPointFeature(LNG_LAT_35_2, createTerminationProperties("t4", "20000008", "10000009"))
            )
        );

        return createPatchResponse(patch).encode();
    }

}
