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
import com.zepben.ewbnetworkserver.geojson.GeoJson;
import com.zepben.vertxutils.json.JsonUtils;

import static com.zepben.ewbnetworkserver.patch.PatchProperties.NORMAL_STATE;

@EverythingIsNonnullByDefault
public class TypeConverters {

    public static int parseVoltage(GeoJson geoJson, String property, String defaultValue, InvalidValueHandler onInvalidValue, GeoJson.DefaultValueHandler onDefaultValueUsed) throws JsonUtils.ParsingException {
        String voltageStr = geoJson.getStringProperty(property, defaultValue, onDefaultValueUsed);
        try {
            if (voltageStr.equals("lv"))
                return 415;
            else if (voltageStr.endsWith("kv"))
                return (int) (Double.parseDouble(voltageStr.substring(0, voltageStr.length() - 2)) * 1000);
            else
                return Integer.parseInt(voltageStr);
        } catch (NumberFormatException e) {
            onInvalidValue.handle(geoJson, property, voltageStr, defaultValue);
            if (defaultValue.equals("lv"))
                return 415;
            else
                return Integer.parseInt(defaultValue);
        }
    }

    public static boolean isNormallyOpenFromNormalState(GeoJson geoJson, InvalidValueHandler onInvalidValue, GeoJson.DefaultValueHandler onDefaultValueUsed) throws JsonUtils.ParsingException {
        String normalState = geoJson.getStringProperty(NORMAL_STATE, "closed", onDefaultValueUsed);
        switch (normalState) {
            case "open":
                return true;
            case "closed":
                return false;
            default:
                onInvalidValue.handle(geoJson, NORMAL_STATE, normalState, "closed");
                return false;
        }
    }

    @FunctionalInterface
    public interface InvalidValueHandler {

        void handle(GeoJson geoJson, String property, String unknownValue, String defaultValue);

    }

}
