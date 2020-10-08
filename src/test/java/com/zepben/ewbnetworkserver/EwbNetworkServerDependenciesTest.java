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

import com.zepben.awsutils.S3;
import com.zepben.testutils.junit.SystemLogExtension;
import com.zepben.testutils.mockito.DefaultAnswer;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;

public class EwbNetworkServerDependenciesTest {

    @RegisterExtension
    public SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private CmdArgs cmdArgs;
    private final Callbacks callbacks = spy(new Callbacks());
    private final S3 s3 = mock(S3.class);

    @Test
    public void coverage() throws Exception {
        EwbNetworkServerDependencies dependencies = createDependencies("");

        assertThat(dependencies.onShutdown(), notNullValue());
        assertThat(dependencies.ewbDataFilePaths(), notNullValue());
        assertThat(dependencies.ewbDataFilePathsHelper(), notNullValue());
        assertThat(dependencies.currentDate(), notNullValue());
        assertThat(dependencies.daysToSearch(), notNullValue());
        assertThat(dependencies.networkDatabaseProvider(), notNullValue());
        assertThat(dependencies.idCorrelatorReaderProvider(), notNullValue());
        assertThat(dependencies.cors(), notNullValue());

        assertThat(dependencies.networkDatabaseProvider().apply(Paths.get("./")), notNullValue());
        assertThat(dependencies.idCorrelatorReaderProvider().apply(Paths.get("./")), notNullValue());

        JsonObject jsonObject = new JsonObject();
        dependencies.resultsWriter().save(jsonObject);

        verify(callbacks, times(1)).saveFile(Paths.get(cmdArgs.output()), jsonObject.toBuffer().getBytes());
        verify(s3, never()).putObject(any(), any(), any());
    }

    @Test
    public void s3Coverage() throws Exception {
        EwbNetworkServerDependencies dependencies = createDependencies("value");

        JsonObject jsonObject = new JsonObject();
        dependencies.resultsWriter().save(jsonObject);

        verify(callbacks, never()).saveFile(any(), any());
        verify(s3, times(1)).putObject(cmdArgs.s3Bucket(), cmdArgs.output(), jsonObject.toString());
    }

    private EwbNetworkServerDependencies createDependencies(String s3Bucket) {
        cmdArgs = mock(CmdArgs.class,
            DefaultAnswer
                .of(String.class, "")
                .and(LocalDate.class, LocalDate.now(ZoneId.systemDefault())));

        doReturn(s3Bucket).when(cmdArgs).s3Bucket();

        return new EwbNetworkServerDependencies(cmdArgs, s -> {}, callbacks::saveFile, deps -> s3);
    }

    private static class Callbacks {

        void saveFile(Path path, byte[] data) {
        }

    }

}
