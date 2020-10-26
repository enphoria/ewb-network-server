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

import com.mashape.unirest.http.Unirest;
import com.zepben.annotations.EverythingIsNonnullByDefault;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Consumer;

@EverythingIsNonnullByDefault
public class Main {

    @Nullable private static EwbNetworkServer ewbNetworkServer = null;

    @FunctionalInterface
    interface MainDependencyProvider {

        Dependencies create(CmdArgs cmdArgs);

    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (ewbNetworkServer != null)
                ewbNetworkServer.stop();
        }));

        ProgramStatus programStatus = run(args, MainDependencies::new, Main::logAppInfo);

        // If run returned OK then the exit will be delayed until the main verticle is complete.
        if (programStatus != ProgramStatus.OK)
            shutdownAndExit(programStatus);
    }

    private static Logger logger = LoggerFactory.getLogger("ewb-network-server");

    static void logAppInfo() {
        String msg = String.format("# %s v%s #",
            Main.class.getPackage().getImplementationTitle(),
            Main.class.getPackage().getImplementationVersion());

        logger.info(msg);
    }

    static ProgramStatus run(String[] args, MainDependencyProvider dependencyProvider, Runnable logAppInfo) {
        // Tell vert.x we are using SLF4J for logging.
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        logAppInfo.run();

        CmdArgs cmdArgs = new CmdArgs();
        try {
            cmdArgs.parse(args);
        } catch (ParseException e) {
            return showHelpAndExit(cmdArgs.options(), e.getMessage());
        }

        if (cmdArgs.isHelpRequested())
            return showHelpAndExit(cmdArgs.options(), null);

        try {
            Dependencies dependencies = dependencyProvider.create(cmdArgs);

            ewbNetworkServer = dependencies.ewbNetworkServerVerticle();
            if (!ewbNetworkServer.load())
                return ProgramStatus.FAILED_TO_START;

            Unirest.setTimeouts(10000, cmdArgs.timeout() * 1000);
            ewbNetworkServer.startHttpServer()
                .setHandler(event -> {
                    if (event.failed())
                        dependencies.onFailure().accept(ProgramStatus.FAILED_TO_START);
                });

            ewbNetworkServer.startGrpcServer();
        } catch (Exception e) {
            logger.error("Failed to start network server: {}", e.getMessage(), e);
            return ProgramStatus.FAILED_TO_START;
        }
        return ProgramStatus.OK;
    }

    private static ProgramStatus showHelpAndExit(Options options, @Nullable String error) {
        String header = "\nEWB Network Server v" + Main.class.getPackage().getImplementationVersion() + ":\nProvides API access for operating on an EWB network.\n\n";
        if (error != null)
            header = "\n" + error + "\n" + header;

        String footer = "\nCopyright Zeppelin Bend Pty Ltd 2018.\n";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ewb-network-server", header, options, footer, true);

        return (error != null) ? ProgramStatus.INVALID_COMMAND_LINE : ProgramStatus.SHOW_HELP;
    }

    static void shutdown(ProgramStatus programStatus) {
        try {
            Unirest.shutdown();
        } catch (IOException e) {
            logger.error("Failed to shutdown Unirest background task.");
        }

        logger.info("Done: {}", programStatus);
    }

    static void shutdownAndExit(ProgramStatus programStatus) {
        shutdown(programStatus);
        System.exit(programStatus.value());
    }

    interface Dependencies {

        EwbNetworkServer ewbNetworkServerVerticle();

        Consumer<ProgramStatus> onFailure();

    }

}
