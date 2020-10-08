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
import com.zepben.cimbend.cim.iec61968.assetinfo.CableInfo;
import com.zepben.cimbend.cim.iec61968.assetinfo.OverheadWireInfo;
import com.zepben.cimbend.cim.iec61968.common.PositionPoint;
import com.zepben.cimbend.cim.iec61970.base.core.GeographicalRegion;
import com.zepben.cimbend.cim.iec61970.base.core.PhaseCode;
import com.zepben.cimbend.cim.iec61970.base.core.SubGeographicalRegion;
import com.zepben.cimbend.cim.iec61970.base.core.Substation;
import com.zepben.cimbend.cim.iec61970.base.wires.AcLineSegment;
import com.zepben.cimbend.cim.iec61970.base.wires.EnergySource;
import com.zepben.cimbend.cim.iec61970.base.wires.Junction;
import com.zepben.cimbend.network.NetworkService;
import com.zepben.cimbend.network.tracing.Tracing;
import com.zepben.ewbnetworkserver.Services;
import com.zepben.ewbnetworkserver.patch.PatchLayer;
import io.vertx.core.json.JsonObject;

import java.util.Collections;

import static com.zepben.ewbnetworkserver.TestObjectCreators.*;
import static com.zepben.ewbnetworkserver.data.PatchCreators.*;

@SuppressWarnings({"SameParameterValue", "ConstantConditions"})
@EverythingIsNonnullByDefault
public class LoopsNetwork {

    public static final PositionPoint LNG_LAT_1_1 = new PositionPoint(1, 1);
    public static final PositionPoint LNG_LAT_2_1 = new PositionPoint(2, 1);
    public static final PositionPoint LNG_LAT_2_2 = new PositionPoint(2, 2);
    public static final PositionPoint LNG_LAT_3_1 = new PositionPoint(3, 1);
    public static final PositionPoint LNG_LAT_4_1 = new PositionPoint(4, 1);

    //
    //        c1         c2
    //  n1-----------n2-----n3
    //   \          /
    //    \c3      /c4
    //     ---n4---
    //
    public static Services toPatch() {
        Services services = new Services();
        NetworkService network = services.networkService();

        GeographicalRegion business = createGeographicalRegion(network, "b1", "b1");
        SubGeographicalRegion region = createSubGeographicalRegion(network, business, "r1", "r1");
        Substation zone = createSubstation(network, region, "z1", "z1");

        EnergySource s0 = createEnergySource(network, "s0", PhaseCode.A, null);

        Junction n1 = createJunction(network, "10000001", "n1", 3, PhaseCode.A, locationOf(network, "10000001-loc", LNG_LAT_1_1));
        Junction n2 = createJunction(network, "10000002", "n2", 3, PhaseCode.A, locationOf(network, "10000002-loc", LNG_LAT_3_1));
        Junction n3 = createJunction(network, "10000003", "n3", 1, PhaseCode.A, locationOf(network, "10000003-loc", LNG_LAT_4_1));
        Junction n4 = createJunction(network, "10000004", "n4", 2, PhaseCode.A, locationOf(network, "10000004-loc", LNG_LAT_2_2));

        OverheadWireInfo overheadWireInfo = new OverheadWireInfo("oh");
        CableInfo cableInfo = new CableInfo("ug");

        AcLineSegment c1 = createAcLineSegment(network, "20000001", "c1", PhaseCode.A, overheadWireInfo, locationOf(network, "20000001-loc", LNG_LAT_1_1, LNG_LAT_3_1));
        AcLineSegment c2 = createAcLineSegment(network, "20000002", "c2", PhaseCode.A, overheadWireInfo, locationOf(network, "20000002-loc", LNG_LAT_3_1, LNG_LAT_4_1));
        AcLineSegment c3 = createAcLineSegment(network, "20000003", "c3", PhaseCode.A, cableInfo, locationOf(network, "20000003-loc", LNG_LAT_1_1, LNG_LAT_2_2));
        AcLineSegment c4 = createAcLineSegment(network, "20000004", "c4", PhaseCode.A, cableInfo, locationOf(network, "20000004-loc", LNG_LAT_2_2, LNG_LAT_3_1));

        createFeeder(network, zone, "f001", "f001", n1);

        network.add(overheadWireInfo);
        network.add(cableInfo);

        network.connect(s0.getTerminal(1), n1.getTerminal(1));
        network.connect(n1.getTerminal(2), c1.getTerminal(1));
        network.connect(n2.getTerminal(1), c1.getTerminal(2));
        network.connect(n2.getTerminal(2), c2.getTerminal(1));
        network.connect(n3.getTerminal(1), c2.getTerminal(2));
        network.connect(n1.getTerminal(3), c3.getTerminal(1));
        network.connect(n4.getTerminal(1), c3.getTerminal(2));
        network.connect(n4.getTerminal(2), c4.getTerminal(1));
        network.connect(n2.getTerminal(3), c4.getTerminal(2));

        Tracing.setPhases().run(network);
        Tracing.assignEquipmentContainersToFeeders().run(network);

        return services;
    }

    //
    //        c1         c2
    //  n1-----------n2-----n3
    //              /
    //             /c4
    //        n4---
    //
    public static Services patched() {
        Services services = new Services();
        NetworkService network = services.networkService();

        GeographicalRegion business = createGeographicalRegion(network, "b1", "b1");
        SubGeographicalRegion region = createSubGeographicalRegion(network, business, "r1", "r1");
        Substation zone = createSubstation(network, region, "z1", "z1");

        EnergySource s0 = createEnergySource(network, "s0", PhaseCode.A, null);

        Junction n1 = createJunction(network, "10000001", "n1", 3, PhaseCode.A, locationOf(network, "10000001-loc", LNG_LAT_1_1));
        Junction n2 = createJunction(network, "10000002", "n2", 3, PhaseCode.A, locationOf(network, "10000002-loc", LNG_LAT_2_1));
        Junction n3 = createJunction(network, "10000003", "n3", 1, PhaseCode.A, locationOf(network, "10000003-loc", LNG_LAT_3_1));
        Junction n4 = createJunction(network, "10000004", "n4", 2, PhaseCode.A, locationOf(network, "10000004-loc", LNG_LAT_4_1));

        OverheadWireInfo overheadWireInfo = new OverheadWireInfo("oh");
        CableInfo cableInfo = new CableInfo("ug");

        AcLineSegment c1 = createAcLineSegment(network, "20000001", "c1", PhaseCode.A, overheadWireInfo, locationOf(network, "20000001-loc", LNG_LAT_1_1, LNG_LAT_3_1));
        AcLineSegment c2 = createAcLineSegment(network, "20000002", "c2", PhaseCode.A, overheadWireInfo, locationOf(network, "20000002-loc", LNG_LAT_3_1, LNG_LAT_4_1));
        AcLineSegment c4 = createAcLineSegment(network, "20000004", "c4", PhaseCode.A, cableInfo, locationOf(network, "20000004-loc", LNG_LAT_2_2, LNG_LAT_3_1));

        createFeeder(network, zone, "f001", "f001", n1);

        network.add(overheadWireInfo);
        network.add(cableInfo);

        network.connect(s0.getTerminal(1), n1.getTerminal(1));
        network.connect(n1.getTerminal(2), c1.getTerminal(1));
        network.connect(n2.getTerminal(1), c1.getTerminal(2));
        network.connect(n2.getTerminal(2), c2.getTerminal(1));
        network.connect(n3.getTerminal(1), c2.getTerminal(2));
        network.connect(n4.getTerminal(2), c4.getTerminal(1));
        network.connect(n2.getTerminal(3), c4.getTerminal(2));

        Tracing.setPhases().run(network);
        Tracing.assignEquipmentContainersToFeeders().run(network);

        return services;
    }

    //
    // From:
    //
    //        c1         c2
    //  n1-----------n2-----n3
    //   \          /
    //    \c3      /c4
    //     ---n4---
    //
    // To:
    //
    //        c1         c2
    //  n1-----------n2-----n3
    //              /
    //             /c4
    //        n4---
    //
    public static String patch() {
        int actionId = 1;

        JsonObject patch1 = createPatch(1,
            Collections.emptyList(),
            Collections.singletonList(
                createLineFeature(Collections.emptyList(), defaultLineProperties("20000003", PatchLayer.hvLines, actionId))
            )
        );

        return createPatchResponse(patch1).encode();
    }

}
