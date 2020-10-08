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

package com.zepben.ewbnetworkserver.patch.routes;

import com.zepben.ewbnetworkserver.patch.LoadManipulations;
import com.zepben.ewbnetworkserver.patch.LoadOperation;
import com.zepben.ewbnetworkserver.patch.LoadType;
import com.zepben.testutils.junit.SystemLogExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class LoadManipulationsToJsonTest {

    @RegisterExtension
    SystemLogExtension systemOut = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private final LoadManipulations loadManipulations = new LoadManipulations();
    private final LoadManipulationsToJson loadManipulationsToJson = new LoadManipulationsToJson();

    @Test
    void convertsToJson() {
        loadManipulations.add("1", LoadOperation.Added, LoadType.Load, 1.1, 1);
        loadManipulations.add("2", LoadOperation.Added, LoadType.Load, 2.2, 1);

        assertThat(loadManipulationsToJson.convert(loadManipulations).encode(),
            equalTo("{\"loadManipulations\":[{\"mRID\":\"1\",\"values\":[1.1]},{\"mRID\":\"2\",\"values\":[2.2]}]}"));
    }

}
