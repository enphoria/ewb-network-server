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

import com.zepben.ewbnetworkserver.patch.PatchLayer;
import com.zepben.testutils.junit.SystemLogExtension;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.regex.Pattern;

import static com.zepben.ewbnetworkserver.patch.PatchProperties.*;
import static com.zepben.testutils.exception.ExpectException.expect;
import static com.zepben.vertxutils.json.JsonUtils.ParsingException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class GeoJsonTest {

    @RegisterExtension
    SystemLogExtension systemOut = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    @Test
    void parses() throws ParsingException {
        GeoJson geoJson = GeoJson.parse(createValidGeoJson());

        assertThat(geoJson.geoJsonType(), equalTo(GeoJsonType.Feature));
        assertThat(geoJson.patchLayer(), equalTo(PatchLayer.transformers));
        assertThat(geoJson.layer(), equalTo(PatchLayer.transformers.name()));
        assertThat(geoJson.actionGroup(), equalTo("{\"id\":1}"));
    }

    @Test
    void detectsInvalidFields() {
        validateMalformed("type", IllegalArgumentException.class);
        validateMalformed("geometry", ParsingException.class);
        validateMalformed("properties", ParsingException.class);
    }

    @Test
    void storeOriginalLayerIfInvalid() throws ParsingException {
        JsonObject jsonObject = createValidGeoJson();
        jsonObject.getJsonObject("properties")
            .put(LAYER_ID, "some layer");

        GeoJson geoJson = GeoJson.parse(jsonObject);

        assertThat(geoJson.patchLayer(), equalTo(PatchLayer.UNKNOWN));
        assertThat(geoJson.layer(), equalTo("some layer"));
    }

    @Test
    void detectsMissingRequiredProperties() {
        validateMissingProperty(GIS_ID);
        validateMissingProperty(LAYER_ID);
    }

    private JsonObject createValidGeoJson() {
        return new JsonObject()
            .put("type", GeoJsonType.Feature)
            .put("geometry", new JsonObject()
                .put("type", GeometryType.Point)
                .put("coordinates", new JsonArray()))
            .put("properties", new JsonObject()
                .put(GIS_ID, "12345678")
                .put(LAYER_ID, PatchLayer.transformers)
                .put(ACTION_GROUP, new JsonObject().put("id", 1)));
    }

    private void validateMalformed(String key, Class<? extends Throwable> expectedValueError) {
        JsonObject jsonObject = createValidGeoJson()
            .put(key, "invalid value");

        expect(() -> GeoJson.parse(jsonObject))
            .toThrow(expectedValueError)
            .withMessage(Pattern.compile(".*(" + key + "|invalid value).*"));

        jsonObject.remove(key);

        expect(() -> GeoJson.parse(jsonObject))
            .toThrow(ParsingException.class)
            .withMessage("No value found for required key '" + key + "'.");
    }

    private void validateMissingProperty(String key) {
        JsonObject jsonObject = createValidGeoJson();
        jsonObject.getJsonObject("properties").remove(key);

        expect(() -> GeoJson.parse(jsonObject))
            .toThrow(ParsingException.class)
            .withMessage("No value found for required key '" + key + "'.");
    }

}
