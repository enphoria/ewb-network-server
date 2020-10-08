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
import io.vertx.core.json.JsonObject;

import java.util.Optional;

@EverythingIsNonnullByDefault
public class PatchFeature {

    private final Patch patch;
    private final GeoJson geoJson;

    static Optional<PatchFeature> parse(Patch patch, String key, JsonObject featureJson) {
        try {
            return Optional.of(new PatchFeature(patch, GeoJson.parse(featureJson)));
        } catch (IllegalArgumentException | JsonUtils.ParsingException e) {
            patch.result().addError("Ignoring malformed %s feature: %s `%s`", key, e.getMessage(), featureJson.encode());
            return Optional.empty();
        }
    }

    public Patch patch() {
        return patch;
    }

    public GeoJson geoJson() {
        return geoJson;
    }

    private PatchFeature(Patch patch, GeoJson geoJson) {
        this.patch = patch;
        this.geoJson = geoJson;
    }

}
