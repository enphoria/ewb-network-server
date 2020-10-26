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
import com.zepben.cimbend.database.sqlite.DatabaseReader;
import com.zepben.ewb.filepaths.EwbDataFilePaths;
import com.zepben.ewbnetworkserver.patch.LoadManipulations;
import com.zepben.ewbnetworkserver.patch.PatchResult;
import com.zepben.ewbnetworkserver.patch.routes.LoadManipulationsToJson;
import com.zepben.idcorrelator.IdCorrelator;
import com.zepben.idcorrelator.io.IdCorrelatorReader;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "FieldCanBeLocal", "UnusedReturnValue"})
@EverythingIsNonnullByDefault
class MockEwbNetworkServerDependencies implements EwbNetworkServer.Dependencies {

    @Mock(answer = Answers.RETURNS_MOCKS)
    @Nullable
    private Vertx vertx;

    @Mock(answer = Answers.RETURNS_MOCKS)
    @Nullable
    private Router router;

    private final DatabaseReader databaseReader = mock(DatabaseReader.class);
    private final IdCorrelatorReader idCorrelatorReader = mock(IdCorrelatorReader.class);
    private final List<PatchResult> patchResults = new ArrayList<>();

    private final Consumer<ProgramStatus> onShutdown = mock(Consumer.class);
    private final int port = 80;
    private final EwbGrpcServer ewbGrpcServer = mock(EwbGrpcServer.class);
    private final EwbDataFilePaths ewbDataFilePaths = mock(EwbDataFilePaths.class);
    private final EwbDataFilePathsHelper ewbDataFilePathsHelper = mock(EwbDataFilePathsHelper.class);
    private final LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());
    private final int daysToSearch = 0;
    private final Function<Path, DatabaseReader> networkDatabaseProvider = mock(Function.class);
    private final Function<Path, IdCorrelatorReader> idCorrelatorReaderProvider = mock(Function.class);
    private String cors = "";
    private final Services services = spy(new Services());
    private final LoadManipulations loadManipulations = spy(new LoadManipulations());
    private final LoadManipulationsToJson loadManipulationsToJson = spy(new LoadManipulationsToJson());
    private final IdCorrelator idCorrelator = mock(IdCorrelator.class);
    private final PatchProcessor patchProcessor = mock(PatchProcessor.class);
    private final RouteDebug routeDebug = mock(RouteDebug.class);
    private final EwbNetworkServer.ResultsWriter resultsWriter = mock(EwbNetworkServer.ResultsWriter.class);

    static MockEwbNetworkServerDependencies create() {
        return spy(new MockEwbNetworkServerDependencies());
    }

    @Override
    public Vertx vertx() {
        return Objects.requireNonNull(vertx);
    }

    @Override
    public Router router() {
        return Objects.requireNonNull(router);
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
        return networkDatabaseProvider;
    }

    @Override
    public Function<Path, IdCorrelatorReader> idCorrelatorReaderProvider() {
        return idCorrelatorReaderProvider;
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

    DatabaseReader databaseReader() {
        return databaseReader;
    }

    IdCorrelatorReader idCorrelatorReader() {
        return idCorrelatorReader;
    }

    void addPatchResult(PatchResult patchResult) {
        patchResults.add(patchResult);
    }

    MockEwbNetworkServerDependencies cors(String cors) {
        this.cors = cors;
        return this;
    }

    MockEwbNetworkServerDependencies setValidDatabasePath(Path validPath) {
        doReturn(databaseReader).when(networkDatabaseProvider).apply(validPath);
        return this;
    }

    MockEwbNetworkServerDependencies setValidIdCorrelatorPath(Path validPath) {
        doReturn(idCorrelatorReader).when(idCorrelatorReaderProvider).apply(validPath);
        return this;
    }

    private MockEwbNetworkServerDependencies() {
        MockitoAnnotations.initMocks(this);

        doReturn(patchResults).when(patchProcessor).applyPatches();
    }

}
