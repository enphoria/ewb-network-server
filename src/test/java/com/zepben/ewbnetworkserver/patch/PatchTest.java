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

import com.zepben.testutils.junit.SystemLogExtension;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.zepben.ewbnetworkserver.data.PatchCreators.createLineFeature;
import static com.zepben.ewbnetworkserver.data.PatchCreators.defaultLineProperties;
import static com.zepben.ewbnetworkserver.patch.PatchProperties.GIS_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PatchTest {

    @RegisterExtension
    public SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    @Test
    public void extractsDetailsFromJson() {
        JsonObject jsonFeature1 = createLineFeature(Collections.emptyList(), defaultLineProperties("1", PatchLayer.hvLines, 1));
        JsonObject jsonFeature2 = createLineFeature(Collections.emptyList(), defaultLineProperties("2", PatchLayer.hvLines, 1));
        JsonObject jsonFeature3 = createLineFeature(Collections.emptyList(), defaultLineProperties("3", PatchLayer.hvLines, 1));
        JsonObject jsonFeature4 = createLineFeature(Collections.emptyList(), defaultLineProperties("4", PatchLayer.hvLines, 1));

        JsonObject jsonObject = new JsonObject()
            .put("id", 23)
            .put("add", new JsonArray()
                .add(jsonFeature1)
                .add(jsonFeature2)
            )
            .put("remove", new JsonArray()
                .add(jsonFeature3)
                .add(jsonFeature4)
            );

        Patch patch = Patch.parse(jsonObject).orElseThrow(AssertionError::new);

        assertThat(patch.id(), equalTo(23));
        assertThat(patch.addFeatures().size(), equalTo(2));
        assertThat(patch.removeFeatures().size(), equalTo(2));

        validateFeatures(patch, patch.addFeatures(), "1", "2");
        validateFeatures(patch, patch.removeFeatures(), "3", "4");
    }

    public void validateFeatures(Patch patch, List<PatchFeature> features, String... expectedIds) {
        features.forEach(feature -> assertThat(feature.patch(), equalTo(patch)));
        assertThat(features.stream().map(feature -> feature.geoJson().properties().getString(GIS_ID)).collect(Collectors.toList()), contains(expectedIds));
    }

    @Test
    public void throwsWithInvalidJson() {
        Optional<Patch> patch = Patch.parse(new JsonObject());

        assertThat(patch.isPresent(), equalTo(false));
        assertThat(systemOutRule.getLog(), containsString("Failed to parse patch: No value found for required key 'id'."));
    }

}
