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

import com.zepben.ewbnetworkserver.geojson.GeoJson;
import com.zepben.ewbnetworkserver.geojson.GeoJsonType;
import com.zepben.ewbnetworkserver.geojson.GeometryType;
import com.zepben.testutils.junit.SystemLogExtension;
import com.zepben.vertxutils.json.JsonUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.zepben.ewbnetworkserver.patch.PatchProperties.*;
import static com.zepben.ewbnetworkserver.patch.TypeConverters.isNormallyOpenFromNormalState;
import static com.zepben.ewbnetworkserver.patch.TypeConverters.parseVoltage;
import static com.zepben.testutils.exception.ExpectException.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

public class TypeConvertersTest {

    @RegisterExtension
    public SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private final TypeConverters.InvalidValueHandler onInvalid = mock(TypeConverters.InvalidValueHandler.class);
    private final GeoJson.DefaultValueHandler onDefault = mock(GeoJson.DefaultValueHandler.class);

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    public void coverage() {
        new TypeConverters();
    }

    @Test
    public void parsesVoltages() throws Exception {
        assertThat(parseVoltage(geoJsonWithProp(OPERATING_VOLTAGE, "lv"), OPERATING_VOLTAGE, "22kv", onInvalid, onDefault), equalTo(415));
        assertThat(parseVoltage(geoJsonWithProp(OPERATING_VOLTAGE, "22kv"), OPERATING_VOLTAGE, "invalid", onInvalid, onDefault), equalTo(22000));
        assertThat(parseVoltage(geoJsonWithProp(OPERATING_VOLTAGE, "6600"), OPERATING_VOLTAGE, "invalid", onInvalid, onDefault), equalTo(6600));

        GeoJson invalidPropGeoJson1 = geoJsonWithProp(OPERATING_VOLTAGE, "abc");
        assertThat(parseVoltage(invalidPropGeoJson1, OPERATING_VOLTAGE, "6600", onInvalid, onDefault), equalTo(6600));

        GeoJson invalidPropGeoJson2 = geoJsonWithProp(OPERATING_VOLTAGE, "def");
        assertThat(parseVoltage(invalidPropGeoJson2, OPERATING_VOLTAGE, "22000", onInvalid, onDefault), equalTo(22000));

        GeoJson missingPropGeoJson = geoJsonWithoutProps();
        assertThat(parseVoltage(missingPropGeoJson, OPERATING_VOLTAGE, "lv", onInvalid, onDefault), equalTo(415));

        expect(() -> parseVoltage(geoJsonWithProp(OPERATING_VOLTAGE, 123), OPERATING_VOLTAGE, "lv", onInvalid, onDefault))
            .toThrow(JsonUtils.ParsingException.class);

        verify(onInvalid, times(2)).handle(any(), any(), any(), any());
        verify(onInvalid, times(1)).handle(invalidPropGeoJson1, OPERATING_VOLTAGE, "abc", "6600");
        verify(onInvalid, times(1)).handle(invalidPropGeoJson2, OPERATING_VOLTAGE, "def", "22000");

        verify(onDefault, times(1)).handle(any(), any(), any());
        verify(onDefault, times(1)).handle(missingPropGeoJson, OPERATING_VOLTAGE, "lv");
    }

    @Test
    public void convertsIsNormallyOpenFromNormalState() throws Exception {
        assertThat(isNormallyOpenFromNormalState(geoJsonWithProp(NORMAL_STATE, "open"), onInvalid, onDefault), equalTo(true));
        assertThat(isNormallyOpenFromNormalState(geoJsonWithProp(NORMAL_STATE, "closed"), onInvalid, onDefault), equalTo(false));

        GeoJson invalidPropGeoJson = geoJsonWithProp(NORMAL_STATE, "oops");
        assertThat(isNormallyOpenFromNormalState(invalidPropGeoJson, onInvalid, onDefault), equalTo(false));

        GeoJson missingPropGeoJson = geoJsonWithoutProps();
        assertThat(isNormallyOpenFromNormalState(missingPropGeoJson, onInvalid, onDefault), equalTo(false));

        expect(() -> isNormallyOpenFromNormalState(geoJsonWithProp(NORMAL_STATE, 1), onInvalid, onDefault))
            .toThrow(JsonUtils.ParsingException.class);

        verify(onInvalid, times(1)).handle(any(), any(), any(), any());
        verify(onInvalid, times(1)).handle(invalidPropGeoJson, NORMAL_STATE, "oops", "closed");

        verify(onDefault, times(1)).handle(any(), any(), any());
        verify(onDefault, times(1)).handle(missingPropGeoJson, NORMAL_STATE, "closed");
    }

    private GeoJson geoJsonWithProp(String property, String value) throws JsonUtils.ParsingException {
        GeoJson geoJson = geoJsonWithoutProps();
        geoJson.properties().put(property, value);

        return geoJson;
    }

    private GeoJson geoJsonWithProp(String property, int value) throws JsonUtils.ParsingException {
        GeoJson geoJson = geoJsonWithoutProps();
        geoJson.properties().put(property, value);

        return geoJson;
    }

    private GeoJson geoJsonWithoutProps() throws JsonUtils.ParsingException {
        return GeoJson.parse(new JsonObject()
            .put("type", GeoJsonType.Feature)
            .put("geometry", new JsonObject()
                .put("type", GeometryType.Point)
                .put("coordinates", new JsonArray())
            )
            .put("properties", new JsonObject()
                .put(GIS_ID, "12345678")
                .put(LAYER_ID, PatchLayer.acr))
        );
    }

}
