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
import com.zepben.cimbend.database.sqlite.DatabaseReader;
import com.zepben.ewb.filepaths.EwbDataFilePaths;
import com.zepben.ewbnc.NetworkConsumerService;
import com.zepben.ewbnetworkserver.patch.LoadManipulations;
import com.zepben.ewbnetworkserver.patch.routes.LoadManipulationsToJson;
import com.zepben.idcorrelator.IdCorrelator;
import com.zepben.idcorrelator.MapBackedIdCorrelator;
import com.zepben.idcorrelator.io.IdCorrelatorReader;
import com.zepben.idcorrelator.io.json.IdCorrelatorJSONReaderWriter;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Function;

@EverythingIsNonnullByDefault
class EwbNetworkServerDependencies implements EwbNetworkServer.Dependencies {

    private final Vertx vertx = Vertx.vertx();
    private final Router router = Router.router(vertx);
    private final Consumer<ProgramStatus> onShutdown;
    private final int port;
    private final EwbGrpcServer ewbGrpcServer;
    private final EwbDataFilePaths ewbDataFilePaths;
    private final EwbDataFilePathsHelper ewbDataFilePathsHelper;
    private final LocalDate currentDate;
    private final int daysToSearch;
    private final String cors;
    private final Services services = new Services();
    private final LoadManipulations loadManipulations = new LoadManipulations();
    private final LoadManipulationsToJson loadManipulationsToJson = new LoadManipulationsToJson();
    private final IdCorrelator idCorrelator = MapBackedIdCorrelator.newCorrelator();
    private final PatchProcessor patchProcessor;
    private final RouteDebug routeDebug;
    private final EwbNetworkServer.ResultsWriter resultsWriter;

    EwbNetworkServerDependencies(CmdArgs cmdArgs, Consumer<ProgramStatus> onShutdown, FileWriter fileWriter, Function<S3Dependencies, S3> s3Provider) {
        this.onShutdown = onShutdown;
        port = cmdArgs.port();
        ewbGrpcServer = new EwbGrpcServer(cmdArgs.grpcPort(),
            cmdArgs.grpcCertPath(),
            cmdArgs.grpcKeyPath(),
            cmdArgs.grpcClientAuth(),
            cmdArgs.grpcTrustPath(),
            new NetworkConsumerService(services.networkService()));
        ewbDataFilePaths = new EwbDataFilePaths(cmdArgs.ewbDataRoot());
        ewbDataFilePathsHelper = new EwbDataFilePathsHelper(ewbDataFilePaths);
        currentDate = cmdArgs.currentDate();
        daysToSearch = cmdArgs.daysToSearch();
        cors = cmdArgs.cors();
        patchProcessor = new PatchProcessor(new PatchProcessorDependencies(services, loadManipulations, cmdArgs));
        routeDebug = new RouteDebug(new RouteDebugDependencies(cmdArgs));

        if (cmdArgs.s3Bucket().isEmpty())
            resultsWriter = json -> fileWriter.write(Paths.get(cmdArgs.output()), json.toBuffer().getBytes());
        else {
            S3 s3 = s3Provider.apply(new S3Dependencies());
            resultsWriter = json -> s3.putObject(cmdArgs.s3Bucket(), cmdArgs.output(), json.toString());
        }
    }

    @Override
    public Vertx vertx() {
        return vertx;
    }

    @Override
    public Router router() {
        return router;
    }

    @Override
    public Consumer<ProgramStatus> onShutdown() {
        return onShutdown;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public EwbGrpcServer ewbGrpcServer() {
        return ewbGrpcServer;
    }

    @Override
    public EwbDataFilePaths ewbDataFilePaths() {
        return ewbDataFilePaths;
    }

    @Override
    public EwbDataFilePathsHelper ewbDataFilePathsHelper() {
        return ewbDataFilePathsHelper;
    }

    @Override
    public LocalDate currentDate() {
        return currentDate;
    }

    @Override
    public int daysToSearch() {
        return daysToSearch;
    }

    @Override
    public Function<Path, DatabaseReader> networkDatabaseProvider() {
        return path -> new DatabaseReader(path.toString());
    }

    @Override
    public Function<Path, IdCorrelatorReader> idCorrelatorReaderProvider() {
        return path -> new IdCorrelatorJSONReaderWriter(path, false);
    }

    @Override
    public String cors() {
        return cors;
    }

    @Override
    public Services services() {
        return services;
    }

    @Override
    public LoadManipulations loadManipulations() {
        return loadManipulations;
    }

    @Override
    public LoadManipulationsToJson loadManipulationsToJson() {
        return loadManipulationsToJson;
    }

    @Override
    public IdCorrelator idCorrelator() {
        return idCorrelator;
    }

    @Override
    public PatchProcessor patchProcessor() {
        return patchProcessor;
    }

    @Override
    public RouteDebug routeDebug() {
        return routeDebug;
    }

    @Override
    public EwbNetworkServer.ResultsWriter resultsWriter() {
        return resultsWriter;
    }

    @FunctionalInterface
    interface FileWriter {

        void write(Path path, byte[] data) throws Exception;

    }

}
