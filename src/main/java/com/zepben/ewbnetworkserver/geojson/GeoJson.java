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
import com.zepben.ewbnetworkserver.patch.PatchLayer;
import com.zepben.vertxutils.json.JsonUtils;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

import static com.zepben.ewbnetworkserver.patch.PatchProperties.*;
import static com.zepben.vertxutils.json.JsonUtils.*;

@EverythingIsNonnullByDefault
public class GeoJson {

    private final GeoJsonType geoJsonType;
    private final GeoJsonGeometry geometry;
    private final JsonObject properties;
    private final String gisId;
    private final PatchLayer patchLayer;
    private final String layer;
    private final String actionGroup;

    public static GeoJson parse(JsonObject jsonObject) throws JsonUtils.ParsingException {
        GeoJsonType geoJsonType = GeoJsonType.valueOf(extractRequiredString(jsonObject, "type"));
        GeoJsonGeometry geometry = GeoJsonGeometry.parse(extractRequiredObject(jsonObject, "geometry"));
        JsonObject properties = extractRequiredObject(jsonObject, "properties");

        String gisId = extractRequiredString(properties, GIS_ID);
        String layer = extractRequiredString(properties, LAYER_ID);
        String actionGroup = extractOptionalObject(properties, ACTION_GROUP).orElse(new JsonObject()).encode();

        PatchLayer patchLayer;
        try {
            patchLayer = PatchLayer.valueOf(layer);
        } catch (IllegalArgumentException ignored) {
            patchLayer = PatchLayer.UNKNOWN;
        }

        return new GeoJson(geoJsonType, properties, geometry, gisId, patchLayer, layer, actionGroup);
    }

    public GeoJsonType geoJsonType() {
        return geoJsonType;
    }

    public GeoJsonGeometry geometry() {
        return geometry;
    }

    public JsonObject properties() {
        return properties;
    }

    public String gisId() {
        return gisId;
    }

    public PatchLayer patchLayer() {
        return patchLayer;
    }

    public String layer() {
        return layer;
    }

    public String actionGroup() {
        return actionGroup;
    }

    public String getStringProperty(String property, String defaultValue, DefaultValueHandler onDefaultValueUsed) throws JsonUtils.ParsingException {
        Optional<String> value = extractOptionalString(properties, property);

        if (!value.isPresent())
            onDefaultValueUsed.handle(this, property, defaultValue);

        return value.orElse(defaultValue).toLowerCase();
    }

    private GeoJson(GeoJsonType geoJsonType, JsonObject properties, GeoJsonGeometry geometry, String gisId, PatchLayer patchLayer, String layer, String actionGroup) {

        this.geoJsonType = geoJsonType;
        this.properties = properties;
        this.geometry = geometry;
        this.gisId = gisId;
        this.patchLayer = patchLayer;
        this.layer = layer;
        this.actionGroup = actionGroup;
    }

    @FunctionalInterface
    public interface DefaultValueHandler {

        void handle(GeoJson geoJson, String property, String defaultValue);

    }

}
