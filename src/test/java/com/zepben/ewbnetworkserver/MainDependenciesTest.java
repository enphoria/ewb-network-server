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
import com.zepben.testutils.mockito.DefaultAnswer;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

public class MainDependenciesTest {

    @RegisterExtension
    public SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    @Test
    public void coverage() {
        MainDependencies dependencies = new MainDependencies(mock(CmdArgs.class, DefaultAnswer.of(String.class, "").and(ClientAuth.class, ClientAuth.NONE)));
        assertThat(dependencies.ewbNetworkServerVerticle(), notNullValue());
        assertThat(dependencies.onFailure(), notNullValue());
    }

}
