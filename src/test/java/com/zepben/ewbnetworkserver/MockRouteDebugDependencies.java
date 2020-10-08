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

package com.zepben.ewbnetworkserver;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import static org.mockito.Mockito.mock;

@EverythingIsNonnullByDefault
@SuppressWarnings("FieldCanBeLocal")
public class MockRouteDebugDependencies implements RouteDebug.Dependencies {

    private final String routeDebugFile = "body-debug.json";
    private final RouteDebug.FileWriter fileWriter = mock(RouteDebug.FileWriter.class);

    @Override
    public String routeDebugFile() {
        return routeDebugFile;
    }

    @Override
    public RouteDebug.FileWriter fileWriter() {
        return fileWriter;
    }

}
