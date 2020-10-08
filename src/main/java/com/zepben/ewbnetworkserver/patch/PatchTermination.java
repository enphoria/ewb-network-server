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

package com.zepben.ewbnetworkserver.patch;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.cimbend.cim.iec61970.base.auxiliaryequipment.AuxiliaryEquipment;
import com.zepben.cimbend.cim.iec61970.base.core.ConductingEquipment;

import java.util.List;

@EverythingIsNonnullByDefault
public class PatchTermination {

    private final String gisId;
    private final String mRID;
    private final List<ConductingEquipment> conductingEquipment;
    private final List<AuxiliaryEquipment> auxiliaryEquipment;

    public PatchTermination(String gisId, String mRID, List<ConductingEquipment> conductingEquipment, List<AuxiliaryEquipment> auxiliaryEquipment) {
        this.gisId = gisId;
        this.mRID = mRID;
        this.conductingEquipment = conductingEquipment;
        this.auxiliaryEquipment = auxiliaryEquipment;
    }

    public String gisId() {
        return gisId;
    }

    public String mRID() {
        return mRID;
    }

    public List<ConductingEquipment> conductingEquipment() {
        return conductingEquipment;
    }

    public List<AuxiliaryEquipment> auxiliaryEquipment() {
        return auxiliaryEquipment;
    }

}
