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

/*
 * Copyright Zeppelin Bend Pty Ltd (Zepben). The use of this file and its contents requires explicit permission from Zepben.
 */
package com.zepben.ewbnetworkserver;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.cimbend.cim.iec61968.common.Location;
import com.zepben.cimbend.cim.iec61968.common.PositionPoint;
import com.zepben.cimbend.cim.iec61970.base.core.ConductingEquipment;
import com.zepben.cimbend.cim.iec61970.base.core.PhaseCode;
import com.zepben.cimbend.cim.iec61970.base.core.Terminal;

import java.util.List;

@EverythingIsNonnullByDefault
public class ObjectCreators {

    public static Location createLocation(Services services, String mRID, List<PositionPoint> lngLats) {
        Location location = new Location(mRID);
        lngLats.forEach(location::addPoint);

        services.networkService().add(location);
        return location;
    }

    public static void createTerminals(Services services, ConductingEquipment conductingEquipment, PhaseCode phaseCode, int numTerminals) {
        for (int i = 0; i < numTerminals; ++i)
            createTerminal(services, conductingEquipment, phaseCode);
    }

    public static Terminal createTerminal(Services services, ConductingEquipment conductingEquipment, PhaseCode phaseCode) {
        Terminal terminal = new Terminal(conductingEquipment.getMRID() + "-t" + (conductingEquipment.numTerminals() + 1));
        terminal.setConductingEquipment(conductingEquipment);
        terminal.setPhases(phaseCode);

        conductingEquipment.addTerminal(terminal);
        services.networkService().add(terminal);

        return terminal;
    }

}
