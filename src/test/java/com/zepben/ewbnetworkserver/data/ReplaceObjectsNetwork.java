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
import com.zepben.cimbend.cim.iec61970.base.wires.EnergySource;
import com.zepben.cimbend.cim.iec61970.base.wires.PowerTransformer;
import com.zepben.cimbend.network.NetworkService;
import com.zepben.cimbend.network.tracing.Tracing;
import com.zepben.ewbnetworkserver.Services;
import com.zepben.ewbnetworkserver.patch.PatchLayer;
import io.vertx.core.json.JsonObject;

import static com.zepben.ewbnetworkserver.PatchProcessor.unknownWireInfo;
import static com.zepben.ewbnetworkserver.TestObjectCreators.*;
import static com.zepben.ewbnetworkserver.data.PatchCreators.createPointFeature;
import static com.zepben.ewbnetworkserver.data.PatchCreators.defaultTransformerProperties;
import static com.zepben.ewbnetworkserver.patch.PatchProperties.REPLACE_ASSET_GIS;

@SuppressWarnings("ConstantConditions")
@EverythingIsNonnullByDefault
public class ReplaceObjectsNetwork {

    public static final PositionPoint LNG_LAT_1_1 = new PositionPoint(1, 1);
    public static final PositionPoint LNG_LAT_2_1 = new PositionPoint(2, 1);

    //
    //         c
    //  s-fcb-----tx1(has site, COR and usage points)
    //
    public static void initialiseToPatch(Services services) {
        NetworkService network = services.networkService();

        network.add(unknownWireInfo);

        EnergySource s = createEnergySource(network, "s");
        Breaker fcb = createBreaker(network, "fcb", "fcb", 2, PhaseCode.A, locationOf(network, "fcb-loc", LNG_LAT_1_1), null);
        PowerTransformer tx1 = createPowerTransformer(network, "tx1", "tx1", 1, PhaseCode.A, locationOf(network, "tx1-loc", LNG_LAT_2_1));

        createOperationalRestriction(network);
        createSite(network, "site", tx1);
        createUsagePoint(network, tx1, null, 1);
        createUsagePoint(network, tx1, null, 2);

        AcLineSegment c = createAcLineSegment(network, "c", "c", PhaseCode.A, unknownWireInfo, locationOf(network, "c-loc", LNG_LAT_1_1, LNG_LAT_2_1));

        createFeeder(network, null, "f001", "f001", fcb.getTerminal(2));

        connect(network, s.getTerminal(1), fcb.getTerminal(1), "cn1");
        connect(network, fcb.getTerminal(2), c.getTerminal(1), "cn1");
        connect(network, c.getTerminal(2), tx1.getTerminal(1), "cn1");

        Tracing.setPhases().run(network);
        Tracing.assignEquipmentContainersToFeeders().run(network);
    }

    //
    // From:
    //
    //
    //         c
    //  s-fcb-----tx1(has site, COR and usage points)
    //
    //
    // To:
    //
    //
    //         c
    //  s-fcb-----tx2(has site, COR and usage points)
    //
    //
    //
    public static JsonObject replaceTxGeoJson() {
        return createPointFeature(LNG_LAT_2_1,
            defaultTransformerProperties("tx2", "tx2", PatchLayer.transformers, 1)
                .put(REPLACE_ASSET_GIS, "tx1")
        );
    }

}
