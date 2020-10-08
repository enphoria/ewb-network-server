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
import com.zepben.vertxutils.json.JsonUtils;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.zepben.vertxutils.json.JsonUtils.extractRequiredInt;
import static com.zepben.vertxutils.json.JsonUtils.extractRequiredObjectList;

@EverythingIsNonnullByDefault
public
class Patch {

    private static final Logger logger = LoggerFactory.getLogger("ewb-network-server");

    private final int id;
    private final List<PatchFeature> addFeatures = new ArrayList<>();
    private final List<PatchFeature> removeFeatures = new ArrayList<>();
    private final PatchResult result;

    public static Optional<Patch> parse(JsonObject jsonObject) {
        try {
            Patch patch = new Patch(extractRequiredInt(jsonObject, "id"));

            extractFeatures(jsonObject, "add", patch::includeAdd, patch);
            extractFeatures(jsonObject, "remove", patch::includeRemove, patch);

            return Optional.of(patch);
        } catch (JsonUtils.ParsingException e) {
            logger.error("Failed to parse patch: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public int id() {
        return id;
    }

    public List<PatchFeature> addFeatures() {
        return Collections.unmodifiableList(addFeatures);
    }

    public List<PatchFeature> removeFeatures() {
        return Collections.unmodifiableList(removeFeatures);
    }

    public PatchResult result() {
        return result;
    }

    private void includeAdd(PatchFeature feature) {
        addFeatures.add(feature);
    }

    private void includeRemove(PatchFeature feature) {
        removeFeatures.add(feature);
    }

    private Patch(int id) throws IllegalArgumentException {
        this.id = id;
        result = new PatchResult(id);
    }

    private static void extractFeatures(JsonObject jsonObject, String key, Consumer<PatchFeature> storeFeature, Patch patch) throws JsonUtils.ParsingException {
        extractRequiredObjectList(jsonObject, key)
            .stream()
            .map(feature -> PatchFeature.parse(patch, key, feature))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(storeFeature);
    }

}
