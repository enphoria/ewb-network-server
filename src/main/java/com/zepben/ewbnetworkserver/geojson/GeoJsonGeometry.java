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

package com.zepben.ewbnetworkserver.geojson;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.cimbend.cim.iec61968.common.PositionPoint;
import com.zepben.vertxutils.json.JsonUtils;
import com.zepben.vertxutils.json.JsonValueExtractors;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.zepben.vertxutils.json.JsonUtils.*;

@EverythingIsNonnullByDefault
public class GeoJsonGeometry {

    private final GeometryType geometryType;
    private final JsonArray coordinates;

    static GeoJsonGeometry parse(JsonObject jsonObject) throws JsonUtils.ParsingException {
        GeometryType geometryType = GeometryType.valueOf(extractRequiredString(jsonObject, "type"));
        JsonArray coordinates = extractRequiredArray(jsonObject, "coordinates");

        return new GeoJsonGeometry(geometryType, coordinates);
    }

    public GeometryType geometryType() {
        return geometryType;
    }

    public PositionPoint coordinate() throws JsonUtils.ParsingException {
        return toLngLat(coordinates);
    }

    public List<PositionPoint> coordinates() throws JsonUtils.ParsingException {
        List<JsonArray> jsonArrays = convertToList(coordinates, JsonArray::getJsonArray);
        List<PositionPoint> lngLats = new ArrayList<>();

        for (JsonArray jsonArray : jsonArrays)
            lngLats.add(toLngLat(jsonArray));

        return Collections.unmodifiableList(lngLats);
    }

    private GeoJsonGeometry(GeometryType geometryType, JsonArray coordinates) {
        this.geometryType = geometryType;
        this.coordinates = coordinates;
    }

    private PositionPoint toLngLat(JsonArray jsonArray) throws JsonUtils.ParsingException {
        List<Double> doubles = convertToList(jsonArray, JsonValueExtractors::getDouble, 2);
        return new PositionPoint(doubles.get(0), doubles.get(1));
    }

}
