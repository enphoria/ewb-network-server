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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

class LoadManipulationsTest {

    @RegisterExtension
    SystemLogExtension systemOut = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private final LoadManipulations loadManipulations = new LoadManipulations();

    @Test
    void combinesManipulationsIntoProfiles() {
        loadManipulations.add("tx", LoadOperation.Added, LoadType.Load, 500, 2.5);
        loadManipulations.add("tx", LoadOperation.Added, LoadType.Load, 600, 1.5);
        loadManipulations.add("tx", LoadOperation.Removed, LoadType.Load, 15, 1);
        loadManipulations.add("tx", LoadOperation.Added, LoadType.Generation, 10, 1);
        loadManipulations.add("sp", LoadOperation.Removed, LoadType.Load, 10, 1);
        loadManipulations.add("sp", LoadOperation.Added, LoadType.Generation, 7.5, 1);
        loadManipulations.add("sp", LoadOperation.Removed, LoadType.Generation, 2, 1);

        assertThat(loadManipulations.byMRID().get("tx"), equalTo(200.0 + 400.0 - 15.0 - 10.0));
        assertThat(loadManipulations.byMRID().get("sp"), equalTo(-10.0 - 7.5 + 2.0));
    }

    @Test
    void canBeRemoved() {
        loadManipulations.add("tx", LoadOperation.Added, LoadType.Load, 1, 1);
        loadManipulations.add("sp", LoadOperation.Added, LoadType.Load, 2, 1);

        assertThat(loadManipulations.byMRID().get("tx"), equalTo(1.0));
        assertThat(loadManipulations.byMRID().get("sp"), equalTo(2.0));

        loadManipulations.remove("tx");

        assertThat(loadManipulations.byMRID().get("tx"), nullValue());
        assertThat(loadManipulations.byMRID().get("sp"), equalTo(2.0));
    }

}
