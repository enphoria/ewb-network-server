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

import ch.qos.logback.classic.Level;
import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.evolve.database.sqlite.DatabaseReader;
import com.zepben.evolve.database.sqlite.tables.TableVersion;
import com.zepben.ewb.filepaths.EwbDataFilePaths;
import com.zepben.ewbnetworkroutes.config.GeoViewConfig;
import com.zepben.ewbnetworkroutes.diagram.geoview.GeoviewRouteGroup;
import com.zepben.ewbnetworkroutes.network.ApplicationInfo;
import com.zepben.ewbnetworkroutes.network.ItemMatcher;
import com.zepben.ewbnetworkroutes.network.NetworkRouteGroup;
import com.zepben.ewbnetworkroutes.network.graphics.NetworkGraphicsRouteGroup;
import com.zepben.ewbnetworkroutes.network.trace.NetworkTraceRouteGroup;
import com.zepben.ewbnetworkroutes.network.translation.IdTranslator;
import com.zepben.ewbnetworkroutes.network.translation.TranslationHelper;
import com.zepben.ewbnetworkserver.patch.LoadManipulations;
import com.zepben.ewbnetworkserver.patch.PatchResult;
import com.zepben.ewbnetworkserver.patch.routes.LoadManipulationsToJson;
import com.zepben.ewbnetworkserver.patch.routes.PatchRouteGroup;
import com.zepben.idcorrelator.IdCorrelator;
import com.zepben.idcorrelator.io.FailedCorrelationInfo;
import com.zepben.idcorrelator.io.IdCorrelatorReadException;
import com.zepben.idcorrelator.io.IdCorrelatorReader;
import com.zepben.vertxutils.routing.RouteRegister;
import com.zepben.vertxutils.routing.RouteRegisterLogger;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@EverythingIsNonnullByDefault
class EwbNetworkServer {

    private static final Logger logger = LoggerFactory.getLogger("ewb-network-server");

    private final Vertx vertx;
    private final Router router;
    private final Consumer<ProgramStatus> onShutdown;
    private final int port;
    private final EwbGrpcServer ewbGrpcServer;
    private final EwbDataFilePaths ewbDataFilePaths;
    private final EwbDataFilePathsHelper ewbDataFilePathsHelper;
    private final LocalDate currentDate;
    private final int daysToSearch;
    private final Function<Path, DatabaseReader> networkDatabaseProvider;
    private final Function<Path, IdCorrelatorReader> idCorrelatorReaderProvider;
    private final String cors;
    private final Services services;
    private final LoadManipulations loadManipulations;
    private final LoadManipulationsToJson loadManipulationsToJson;
    private final IdCorrelator idCorrelator;
    private final PatchProcessor patchProcessor;
    private final RouteDebug routeDebug;
    private final ResultsWriter resultsWriter;

    private final List<HttpServer> httpServers = new ArrayList<>();

    EwbNetworkServer(Dependencies dependencies) {
        vertx = dependencies.vertx();
        router = dependencies.router();
        onShutdown = dependencies.onShutdown();
        port = dependencies.port();
        ewbGrpcServer = dependencies.ewbGrpcServer();
        ewbDataFilePaths = dependencies.ewbDataFilePaths();
        ewbDataFilePathsHelper = dependencies.ewbDataFilePathsHelper();
        currentDate = dependencies.currentDate();
        daysToSearch = dependencies.daysToSearch();
        networkDatabaseProvider = dependencies.networkDatabaseProvider();
        idCorrelatorReaderProvider = dependencies.idCorrelatorReaderProvider();
        cors = dependencies.cors();
        services = dependencies.services();
        loadManipulations = dependencies.loadManipulations();
        loadManipulationsToJson = dependencies.loadManipulationsToJson();
        idCorrelator = dependencies.idCorrelator();
        patchProcessor = dependencies.patchProcessor();
        routeDebug = dependencies.routeDebug();
        resultsWriter = dependencies.resultsWriter();
    }

    boolean load() {
        LocalDate date = ewbDataFilePathsHelper.findClosestDateWithDbs(currentDate, daysToSearch);
        if (date == null) {
            logger.error("Failed to find network model and id correlations file within '{}' days of '{}'", daysToSearch, currentDate);
            return false;
        }

        boolean status = loadNetwork(date)
            && applyPatches()
            && loadIdCorrelator(date);

        if (status)
            createRoutes();

        return status;
    }

    Future<Void> startHttpServer() {
        logger.info("Starting HTTP server on port {}...", port);

        // Dodgyness to work around a bug in vert.x. See https://zepben.atlassian.net/browse/ERIS-1188
        ch.qos.logback.classic.Logger vertxLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ConnectionBase.class);
        vertxLogger.setLevel(Level.OFF);

        // Get HTTP server options
        HttpServerOptions serverOptions = new HttpServerOptions()
            .setPort(port)
            .setCompressionSupported(true);

        // Handler for HTTP server creation result
        Future<Void> future = Future.future();
        httpServers.add(vertx.createHttpServer(serverOptions)
            .requestHandler(router)
            .exceptionHandler(this::serverExceptionHandler)
            .listen(result -> {
                if (result.succeeded()) {
                    logger.info("HTTP server started");
                    future.complete();
                } else {
                    logger.error("Failed to start HTTP server.");
                    future.fail(result.cause());
                }
            }));

        return future;
    }

    void stop() {
        logger.info("Stopping...");
        CompositeFuture.all(httpServers
            .stream()
            .map(httpServer -> {
                Future<Void> future = Future.future();
                httpServer.close(future);
                return future;
            })
            .collect(Collectors.toList())
        ).setHandler(event -> {
            vertx.close();
            onShutdown.accept(ProgramStatus.OK);
        });
    }

    private boolean loadNetwork(LocalDate date) {
        Path networkDbFile = ewbDataFilePaths.networkModel(date);
        logger.info("Loading network from '{}'...", networkDbFile);

        DatabaseReader database = networkDatabaseProvider.apply(networkDbFile);
        if (database.load(services.metadataCollection, services.networkService(), services.diagramService(), services.customerService())) {
            logger.info("Network loaded [v{}].", new TableVersion().getSUPPORTED_VERSION());
            return true;
        } else {
            String msg = "Failed to load network model.";
            logger.error(msg);
            return false;
        }
    }

    private boolean applyPatches() {
        logger.info("Applying patches...");
        List<PatchResult> patchResults = patchProcessor.applyPatches();
        if (patchResults == null) {
            logger.error("Failed to apply patches.");
            return false;
        }

        JsonArray results = new JsonArray();
        patchResults.stream().map(this::toJson).forEach(results::add);

        try {
            resultsWriter.save(new JsonObject().put("results", results));
            if (results.isEmpty())
                logger.info("No patches to apply.");
            else
                logger.info("Patches applied.");
            return true;
        } catch (Exception e) {
            logger.error("Failed to save patch results: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean loadIdCorrelator(LocalDate date) {
        Path correlationsFile = ewbDataFilePaths.correlations(date);
        logger.info("Loading ID correlations from '{}'...", correlationsFile);

        Collection<FailedCorrelationInfo> failedMapInfoList;
        IdCorrelatorReader idCorrelatorReader = idCorrelatorReaderProvider.apply(correlationsFile);
        try {
            failedMapInfoList = idCorrelatorReader.read(idCorrelator);
        } catch (IdCorrelatorReadException ex) {
            logger.error("Exception caught while reading ID correlations.", ex);
            return false;
        }

        if (failedMapInfoList.isEmpty()) {
            logger.info("ID correlations loaded.");
            return true;
        } else {
            String msg = "Failed to load idCorrelator caches: " +
                failedMapInfoList
                    .stream()
                    .map(i -> String.format("[systemTag: %s, coreId: %s, systemId: %s, msg: %s]", i.systemTag(), i.coreId(), i.systemId(), i.details()))
                    .collect(joining("\n"));
            logger.error(msg);
            return false;
        }
    }

    private void createRoutes() {
        logger.info("Initialising route handlers...");

        RouteRegister routeRegister = new RouteRegister(router, "/ewb", false);

        if (!cors.isEmpty())
            setupCors(router);

        if (routeDebug.isDebugging())
            setupRouteDebugging(router, routeRegister);

        IdTranslator idTranslator = new IdTranslator(services.networkService(), idCorrelator);
        GeoViewConfig geoViewConfig = GeoViewConfig.builder().build();
        TranslationHelper translationHelper = new TranslationHelper(idTranslator);
        ItemMatcher itemMatcher = new ItemMatcher(services.networkService(), services.diagramService(), translationHelper);

        VersionInfo versionInfo = new VersionInfo();
        ApplicationInfo applicationInfo = new ApplicationInfo(versionInfo.getTitle(), versionInfo.getVersion());

        routeRegister
            .add(NetworkRouteGroup.api(applicationInfo, services.metadataCollection, services.networkService(), services.diagramService(), services.customerService(), idTranslator, idCorrelator, itemMatcher))
            .add(NetworkTraceRouteGroup.api(services.networkService(), services.diagramService(), services.customerService(), idTranslator))
            .add(NetworkGraphicsRouteGroup.api(services.networkService(), services.diagramService(), services.customerService(), idTranslator, geoViewConfig))
            .add(GeoviewRouteGroup.api(idTranslator, geoViewConfig))
            .add(PatchRouteGroup.api(loadManipulations, loadManipulationsToJson));

        logger.info("Route handlers initialised.");
    }

    private void setupCors(Router router) {
        router.route().handler(CorsHandler.create(cors).allowedMethod(HttpMethod.GET));
    }

    private void setupRouteDebugging(Router router, RouteRegister routeRegister) {
        routeRegister.onAdd(new RouteRegisterLogger(logger));

        router.route().handler(LoggerHandler.create());
        router.route().handler(BodyHandler.create());
        router.route().blockingHandler(routeDebug::saveRequestBody);
    }

    private void serverExceptionHandler(Throwable throwable) {
        logger.error("Exception caught in HTTP server.", throwable);
    }

    private JsonObject toJson(PatchResult patchResult) {
        return new JsonObject()
            .put("patchId", patchResult.patchId())
            .put("affectedNormalFeeders", toJson(patchResult.affectedNormalFeeders()))
            .put("affectedCurrentFeeders", toJson(patchResult.affectedCurrentFeeders()))
            .put("errors", patchResult.errors())
            .put("warnings", patchResult.warnings());
    }

    private JsonArray toJson(Iterable<String> strings) {
        JsonArray array = new JsonArray();
        strings.forEach(array::add);
        return array;
    }

    public void startGrpcServer() {
        logger.info("Starting gRPC API on port {}...", ewbGrpcServer.getPort());

        ewbGrpcServer.start();

        logger.info("gRPC API started on port {}.", ewbGrpcServer.getPort());
    }

    @FunctionalInterface
    interface ResultsWriter {

        void save(JsonObject results) throws Exception;

    }

    interface Dependencies {

        Vertx vertx();

        Router router();

        Consumer<ProgramStatus> onShutdown();

        int port();

        EwbGrpcServer ewbGrpcServer();

        EwbDataFilePaths ewbDataFilePaths();

        EwbDataFilePathsHelper ewbDataFilePathsHelper();

        LocalDate currentDate();

        int daysToSearch();

        Function<Path, DatabaseReader> networkDatabaseProvider();

        Function<Path, IdCorrelatorReader> idCorrelatorReaderProvider();

        String cors();

        Services services();

        LoadManipulations loadManipulations();

        LoadManipulationsToJson loadManipulationsToJson();

        IdCorrelator idCorrelator();

        PatchProcessor patchProcessor();

        RouteDebug routeDebug();

        ResultsWriter resultsWriter();

    }

}
