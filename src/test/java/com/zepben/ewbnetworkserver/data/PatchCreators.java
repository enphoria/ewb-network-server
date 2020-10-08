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
import com.zepben.ewbnetworkserver.geojson.GeometryType;
import com.zepben.ewbnetworkserver.patch.PatchLayer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Stream;

import static com.zepben.ewbnetworkserver.patch.PatchLayer.*;
import static com.zepben.ewbnetworkserver.patch.PatchProperties.*;
import static com.zepben.vertxutils.json.Collectors.toJsonArray;

@SuppressWarnings({"SameParameterValue"})
@EverythingIsNonnullByDefault
public class PatchCreators {

    public static JsonArray createPatchResponse(JsonObject... patches) {
        JsonArray array = new JsonArray();

        for (JsonObject patch : patches)
            array.add(patch);

        return array;
    }

    public static JsonObject createPatch(int id, List<JsonObject> addFeatures, List<JsonObject> removeFeatures) {
        return new JsonObject()
            .put("id", id)
            .put("add", new JsonArray(addFeatures))
            .put("remove", new JsonArray(removeFeatures));
    }

    public static JsonObject createPointFeature(PositionPoint positionPoint, JsonObject properties) {
        return new JsonObject()
            .put("type", "Feature")
            .put("geometry", new JsonObject()
                .put("type", GeometryType.Point)
                .put("coordinates", formatPositionPoint(positionPoint)))
            .put("properties", properties);
    }

    public static JsonObject createLineFeature(List<PositionPoint> positionPoints, JsonObject properties) {
        return new JsonObject()
            .put("type", "Feature")
            .put("geometry", new JsonObject()
                .put("type", GeometryType.LineString)
                .put("coordinates", positionPoints
                    .stream()
                    .map(PatchCreators::formatPositionPoint)
                    .collect(toJsonArray())))
            .put("properties", properties);
    }

    public static JsonObject createBaseProperties(String gisId, PatchLayer patchLayer) {
        return new JsonObject()
            .put(GIS_ID, gisId)
            .put(LAYER_ID, patchLayer.name());
    }

    public static JsonObject defaultTransformerProperties(String gisId, String description, PatchLayer patchLayer, int actionGroup) {
        return createBaseProperties(gisId, patchLayer)
            .put(DESCRIPTION, description)
            .put(RATING, "100kVA")
            .put(PRIMARY_VOLTAGE, "11kV")
            .put(SECONDARY_VOLTAGE, "LV")
            .put(LOAD_DATA, new JsonArray())
            .put(ACTION_GROUP, new JsonObject().put("id", actionGroup));
    }

    public static JsonObject defaultSwitchProperties(String gisId, String description, PatchLayer patchLayer, int actionGroup) {
        return createBaseProperties(gisId, patchLayer)
            .put(DESCRIPTION, description)
            .put(OPERATING_VOLTAGE, (patchLayer == lvCircuitBreakers) || (patchLayer == lvSwitches) ? "LV" : "6.6kV")
            .put(NORMAL_STATE, "closed")
            .put(ACTION_GROUP, new JsonObject().put("id", actionGroup));
    }

    public static JsonObject defaultLineProperties(String gisId, PatchLayer patchLayer, int actionGroup) {
        return createBaseProperties(gisId, patchLayer)
            .put(OPERATING_VOLTAGE, ((patchLayer == lvBusBars) || (patchLayer == lvCables) || (patchLayer == lvLines) || (patchLayer == lvCablesService)) ? "LV" : "6.6kV")
            .put(ACTION_GROUP, new JsonObject().put("id", actionGroup));
    }

    public static JsonObject createTerminationProperties(String gisId, String... connectedAssets) {
        return createBaseProperties(gisId, terminations)
            .put(CONNECTED_ASSETS, Stream.of(connectedAssets)
                .map(id -> new JsonObject()
                    .put(CONNECTED_ASSET_ID, id))
                .collect(toJsonArray()));
    }

    private static JsonArray formatPositionPoint(PositionPoint positionPoint) {
        return new JsonArray()
            .add(positionPoint.getXPosition())
            .add(positionPoint.getYPosition());
    }

}
