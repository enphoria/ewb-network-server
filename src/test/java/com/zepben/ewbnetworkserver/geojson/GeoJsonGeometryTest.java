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

import com.zepben.evolve.cim.iec61968.common.PositionPoint;
import com.zepben.testutils.junit.SystemLogExtension;
import com.zepben.vertxutils.json.JsonUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.regex.Pattern;

import static com.zepben.testutils.exception.ExpectException.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

class GeoJsonGeometryTest {

    @RegisterExtension
    SystemLogExtension systemOut = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    @Test
    void parsesPointCoordinate() throws JsonUtils.ParsingException {
        GeoJsonGeometry geometry = GeoJsonGeometry.parse(createValidPointGeometry());

        assertThat(geometry.geometryType(), equalTo(GeometryType.Point));
        assertThat(geometry.coordinate(), equalTo(new PositionPoint(1.1, 2.2)));
        expect(geometry::coordinates)
            .toThrow(JsonUtils.ParsingException.class);
    }

    @Test
    void parsesLineCoordinates() throws JsonUtils.ParsingException {
        GeoJsonGeometry geometry = GeoJsonGeometry.parse(createValidLineGeometry());

        assertThat(geometry.geometryType(), equalTo(GeometryType.LineString));
        assertThat(geometry.coordinates(), contains(new PositionPoint(1.1, 2.2), new PositionPoint(3.3, 4.4)));
        expect(geometry::coordinate)
            .toThrow(JsonUtils.ParsingException.class);
    }

    @Test
    void detectsInvalidFields() {
        validateMalformed("type", IllegalArgumentException.class);
        validateMalformed("coordinates", JsonUtils.ParsingException.class);
    }

    private JsonObject createValidPointGeometry() {
        return new JsonObject()
            .put("type", GeometryType.Point)
            .put("coordinates", new JsonArray()
                .add(1.1)
                .add(2.2));
    }

    private JsonObject createValidLineGeometry() {
        return new JsonObject()
            .put("type", GeometryType.LineString)
            .put("coordinates", new JsonArray()
                .add(new JsonArray()
                    .add(1.1)
                    .add(2.2))
                .add(new JsonArray()
                    .add(3.3)
                    .add(4.4)));
    }

    private void validateMalformed(String key, Class<? extends Throwable> expectedValueError) {
        JsonObject jsonObject = createValidPointGeometry()
            .put(key, "invalid value");

        expect(() -> GeoJsonGeometry.parse(jsonObject))
            .toThrow(expectedValueError)
            .withMessage(Pattern.compile(".*(" + key + "|invalid value).*"));

        jsonObject.remove(key);

        expect(() -> GeoJsonGeometry.parse(jsonObject))
            .toThrow(JsonUtils.ParsingException.class)
            .withMessage("No value found for required key '" + key + "'.");
    }

}
