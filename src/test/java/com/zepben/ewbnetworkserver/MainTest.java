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

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import com.zepben.testutils.junit.SystemLogExtension;
import io.vertx.core.Future;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.function.Consumer;

import static com.zepben.collectionutils.CollectionUtils.arrayOf;
import static com.zepben.ewbnetworkserver.ProgramStatus.FAILED_TO_START;
import static com.zepben.ewbnetworkserver.data.CmdArgsTestData.minimumArgs;
import static com.zepben.ewbnetworkserver.data.CmdArgsTestData.validArgs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;

public class MainTest {

    @RegisterExtension
    public final SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private final MockMainDependencies dependencies = spy(new MockMainDependencies());
    private final EwbNetworkServer ewbNetworkServer = dependencies.ewbNetworkServerVerticle();
    private final Consumer<ProgramStatus> onFailure = dependencies.onFailure();

    private final Main.MainDependencyProvider dependenciesProvider = cmdArgs -> dependencies;
    private final Runnable nullLogger = () -> {};

    @BeforeEach
    public void setUp() {
        clearInvocations(dependencies);
    }

    @Test
    public void main() {
    }

    @Test
    public void coverage() {
        //noinspection InstantiationOfUtilityClass
        new Main();
        Main.logAppInfo();
    }

    @Test
    public void runs() {
        doReturn(true).when(ewbNetworkServer).load();
        doReturn(Future.succeededFuture()).when(ewbNetworkServer).startHttpServer();

        assertThat(Main.run(minimumArgs(), dependenciesProvider, nullLogger), IsEqual.equalTo(ProgramStatus.OK));

        String console = systemOutRule.getLog();
        assertThat(console, equalTo(""));

        verify(dependencies, times(1)).ewbNetworkServerVerticle();
        verify(ewbNetworkServer, times(1)).load();
        verify(ewbNetworkServer, times(1)).startHttpServer();
        verify(onFailure, never()).accept(any());
    }

    @Test
    public void showsHelpWhenAsked() {
        assertThat(Main.run(arrayOf("-h"), dependenciesProvider, nullLogger), IsEqual.equalTo(ProgramStatus.SHOW_HELP));

        String console = systemOutRule.getLog();
        assertThat(console, containsString("usage: ewb-network-server"));
        assertThat(console, containsString("Copyright Zeppelin Bend Pty Ltd"));

        verify(dependencies, never()).ewbNetworkServerVerticle();
        verify(ewbNetworkServer, never()).load();
        verify(ewbNetworkServer, never()).startHttpServer();
        verify(onFailure, never()).accept(any());
    }

    @Test
    public void showsHelpWithUnknownCommandLineOptions() {
        assertThat(Main.run(arrayOf("-q"), dependenciesProvider, nullLogger), equalTo(ProgramStatus.INVALID_COMMAND_LINE));

        String console = systemOutRule.getLog();
        assertThat(console, containsString("usage: ewb-network-server"));
        assertThat(console, containsString("Unrecognized option: -q"));
        assertThat(console, containsString("Copyright Zeppelin Bend Pty Ltd"));

        verify(dependencies, never()).ewbNetworkServerVerticle();
        verify(ewbNetworkServer, never()).load();
        verify(ewbNetworkServer, never()).startHttpServer();
        verify(onFailure, never()).accept(any());
    }

    @Test
    public void showsHelpWithMissingCommandLineOptions() {
        assertThat(Main.run(arrayOf(), dependenciesProvider, nullLogger), equalTo(ProgramStatus.INVALID_COMMAND_LINE));

        String console = systemOutRule.getLog();
        assertThat(console, containsString("usage: ewb-network-server"));
        assertThat(console, containsString("Missing required option: port"));
        assertThat(console, containsString("Copyright Zeppelin Bend Pty Ltd"));

        verify(dependencies, never()).ewbNetworkServerVerticle();
        verify(ewbNetworkServer, never()).load();
        verify(ewbNetworkServer, never()).startHttpServer();
        verify(onFailure, never()).accept(any());
    }

    @Test
    public void showsHelpWithInvalidCommandLineOptions() {
        assertThat(Main.run(arrayOf("-p", "80", "-e", "path", "-d", "abc"), dependenciesProvider, nullLogger), equalTo(ProgramStatus.INVALID_COMMAND_LINE));

        String console = systemOutRule.getLog();
        assertThat(console, containsString("usage: ewb-network-server"));
        assertThat(console, containsString("Invalid integer 'abc' for argument days-to-search"));
        assertThat(console, containsString("Copyright Zeppelin Bend Pty Ltd"));

        verify(dependencies, never()).ewbNetworkServerVerticle();
        verify(ewbNetworkServer, never()).load();
        verify(ewbNetworkServer, never()).startHttpServer();
        verify(onFailure, never()).accept(any());
    }

    @Test
    public void handlesFailuresInTheServerLoad() {
        doReturn(false).when(ewbNetworkServer).load();

        assertThat(Main.run(validArgs(), dependenciesProvider, nullLogger), equalTo(FAILED_TO_START));

        String console = systemOutRule.getLog();
        assertThat(console, isEmptyString());

        verify(dependencies, times(1)).ewbNetworkServerVerticle();
        verify(ewbNetworkServer, times(1)).load();
        verify(ewbNetworkServer, never()).startHttpServer();
        verify(onFailure, never()).accept(any());
    }

    @Test
    public void handlesFailuresInTheServerStartViaCallback() {
        doReturn(true).when(ewbNetworkServer).load();
        doReturn(Future.failedFuture("test failure")).when(ewbNetworkServer).startHttpServer();

        assertThat(Main.run(validArgs(), dependenciesProvider, nullLogger), equalTo(ProgramStatus.OK));

        verify(dependencies, times(1)).ewbNetworkServerVerticle();
        verify(ewbNetworkServer, times(1)).load();
        verify(ewbNetworkServer, times(1)).startHttpServer();
        verify(onFailure, times(1)).accept(FAILED_TO_START);
    }

    @Test
    public void handlesExceptionsInTheServerInitialisation() {
        doThrow(new RuntimeException("Test error")).when(ewbNetworkServer).load();

        assertThat(Main.run(validArgs(), dependenciesProvider, nullLogger), equalTo(FAILED_TO_START));

        String console = systemOutRule.getLog();
        assertThat(console, containsString("Test error"));

        verify(dependencies, times(1)).ewbNetworkServerVerticle();
        verify(ewbNetworkServer, times(1)).load();
        verify(ewbNetworkServer, never()).startHttpServer();
        verify(onFailure, never()).accept(any());
    }

    @Test
    @ExpectSystemExitWithStatus(/*FAILED_TO_START.value()*/ -1)
    public void exitsWithStatus() {
        Main.shutdownAndExit(FAILED_TO_START);
    }

}
