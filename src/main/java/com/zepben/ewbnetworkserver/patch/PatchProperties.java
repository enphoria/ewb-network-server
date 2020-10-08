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

@EverythingIsNonnullByDefault
public class PatchProperties {

    public static final String LAYER_ID = "__layerId";
    public static final String GIS_ID = "GIS_ID";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String ACTION_GROUP = "__actionGroup";
    public static final String OPERATING_VOLTAGE = "OPERATING_VOLTAGE";
    public static final String PRIMARY_VOLTAGE = "Primary Voltage";
    public static final String SECONDARY_VOLTAGE = "Secondary Voltage";
    public static final String RATING = "RATING";
    public static final String LOAD_DATA = "__loadData";
    public static final String LOAD_OPERATION = "operation";
    public static final String LOAD_TYPE = "type";
    public static final String LOAD_QUANTITY = "quantity";
    public static final String LOAD_DIVERSIFICATION_FACTOR = "df";
    public static final String NORMAL_STATE = "NORMAL_STATE";
    public static final String CONNECTED_ASSETS = "connectedAssets";
    public static final String CONNECTED_ASSET_ID = "id";
    public static final String REPLACE_ASSET_GIS = "__ASSET_GIS";

}
