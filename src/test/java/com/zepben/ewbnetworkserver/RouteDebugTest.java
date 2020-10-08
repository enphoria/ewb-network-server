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

import com.zepben.testutils.junit.SystemLogExtension;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

public class RouteDebugTest {

    @RegisterExtension
    public SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private final MockRouteDebugDependencies dependencies = new MockRouteDebugDependencies();
    private final RouteDebug routeDebug = new RouteDebug(dependencies);

    private final String debugFile = dependencies.routeDebugFile();
    private final RouteDebug.FileWriter fileWriter = dependencies.fileWriter();
    private final RoutingContext context = mock(RoutingContext.class, RETURNS_DEEP_STUBS);

    @Test
    public void savesRequestBody() throws IOException {
        assertThat(routeDebug.isDebugging(), equalTo(true));

        doReturn(null).when(context).getBody();

        routeDebug.saveRequestBody(context);
        verify(fileWriter, never()).write(any(), any(), any());

        doReturn(Buffer.buffer("")).when(context).getBody();

        routeDebug.saveRequestBody(context);
        verify(fileWriter, never()).write(any(), any(), any());

        doReturn(Buffer.buffer("test body")).when(context).getBody();

        routeDebug.saveRequestBody(context);
        verify(fileWriter, times(1)).write(eq(Paths.get(debugFile)), any(), any());
    }

    @Test
    public void handlesWriteExceptions() throws Exception {
        doThrow(IOException.class).when(fileWriter).write(any(), any(), any());
        doReturn(Buffer.buffer("test body")).when(context).getBody();

        routeDebug.saveRequestBody(context);

        assertThat(systemOutRule.getLog(), containsString("Failed to save route body to debug file."));
    }

}
