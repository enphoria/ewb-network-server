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
import com.zepben.awsutils.S3;
import com.zepben.awsutils.S3Dependencies;

import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.function.Function;

@EverythingIsNonnullByDefault
class MainDependencies implements Main.Dependencies {

    private final EwbNetworkServer ewbNetworkServer;
    private final Consumer<ProgramStatus> onFailure;

    MainDependencies(CmdArgs cmdArgs) {
        Consumer<ProgramStatus> onShutdown = Main::shutdown;
        EwbNetworkServerDependencies.FileWriter fileWriter = Files::write;
        Function<S3Dependencies, S3> s3Provider = S3::new;
        EwbNetworkServerDependencies dependencies = new EwbNetworkServerDependencies(cmdArgs, onShutdown, fileWriter, s3Provider);

        ewbNetworkServer = new EwbNetworkServer(dependencies);
        onFailure = Main::shutdownAndExit;
    }

    @Override
    public EwbNetworkServer ewbNetworkServerVerticle() {
        return ewbNetworkServer;
    }

    @Override
    public Consumer<ProgramStatus> onFailure() {
        return onFailure;
    }

}
