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

import com.zepben.cimbend.cim.iec61970.base.core.ConductingEquipment;
import com.zepben.cimbend.database.sqlite.DatabaseReader;
import com.zepben.ewb.filepaths.EwbDataFilePaths;
import com.zepben.ewbnetworkserver.patch.PatchResult;
import com.zepben.idcorrelator.io.FailedCorrelationInfo;
import com.zepben.idcorrelator.io.IdCorrelatorReadException;
import com.zepben.idcorrelator.io.IdCorrelatorReader;
import com.zepben.testutils.junit.SystemLogExtension;
import com.zepben.testutils.mockito.DefaultAnswer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.zepben.ewbnetworkserver.TestObjectCreators.createFeederMock;
import static com.zepben.vertxutils.json.JsonUtils.extractRequiredObjectList;
import static com.zepben.vertxutils.json.JsonUtils.extractRequiredStringList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("FieldCanBeLocal")
public class EwbNetworkServerTest {

    @RegisterExtension
    public SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private final int NUM_EXPECTED_NETWORK_ROUTES = 26;
    private final int NUM_EXPECTED_PATCH_ROUTES = 1;
    private final int NUM_EXPECTED_ROUTES = NUM_EXPECTED_NETWORK_ROUTES + NUM_EXPECTED_PATCH_ROUTES;
    private final int NUM_CORS_ROUTES = 1;
    private final int NUM_DEBUG_ROUTES = 3;

    private final MockEwbNetworkServerDependencies dependencies = MockEwbNetworkServerDependencies.create();
    private final Vertx vertx = dependencies.vertx();
    private final Router router = dependencies.router();
    private final Consumer<ProgramStatus> onShutdown = dependencies.onShutdown();
    private final EwbDataFilePaths ewbDataFilePaths = dependencies.ewbDataFilePaths();
    private final EwbDataFilePathsHelper ewbDataFilePathsHelper = dependencies.ewbDataFilePathsHelper();
    private final Function<Path, DatabaseReader> networkDatabaseProvider = dependencies.networkDatabaseProvider();
    private final Function<Path, IdCorrelatorReader> idCorrelatorReaderProvider = dependencies.idCorrelatorReaderProvider();
    private final RouteDebug routeDebug = dependencies.routeDebug();
    private final PatchProcessor patchProcessor = dependencies.patchProcessor();
    private final EwbNetworkServer.ResultsWriter resultsWriter = dependencies.resultsWriter();

    private final DatabaseReader databaseReader = dependencies.databaseReader();
    private final IdCorrelatorReader idCorrelatorReader = dependencies.idCorrelatorReader();

    private final LocalDate validDate = dependencies.currentDate().minusDays(2);

    private final HttpServer httpServer = mock(HttpServer.class, Mockito.RETURNS_SELF);

    private Path networkDatabasePath;
    private Path idCorrelatorPath;

    @Test
    public void worksWithValidData() throws Exception {
        addPatchResults();

        configureData(validDate, true, true, true, true);
        EwbNetworkServer ewbNetworkServer = createServer();

        assertThat(ewbNetworkServer.load(), equalTo(true));

        validateProcessCalls(1, 1, 1, 1, NUM_EXPECTED_ROUTES);
        validatePatchResults();

        validateLog("Network loaded [");
        validateLog("Patches applied.");
        validateLog("ID correlations loaded.");
        validateLog("Route handlers initialised.");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void startsHttpServer() throws Exception {
        configureData(validDate, true, true, true, true);
        EwbNetworkServer ewbNetworkServer = createServer();

        doReturn(httpServer).when(vertx).createHttpServer(any(HttpServerOptions.class));
        doAnswer(invocation -> {
            ((Handler<AsyncResult<?>>) invocation.getArgument(0)).handle(Future.succeededFuture());
            return null;
        }).when(httpServer).listen(any());

        Future<Void> future = ewbNetworkServer.startHttpServer();

        assertThat(future.succeeded(), equalTo(true));

        verify(vertx, times(1)).createHttpServer(any(HttpServerOptions.class));

        verify(httpServer, times(1)).requestHandler(any());
        verify(httpServer, times(1)).exceptionHandler(any());
        verify(httpServer, times(1)).listen(any());

        validateLog("HTTP server started");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void stopsHttpServersAndCallsShutdownHandlerOnStop() {
        doReturn(httpServer).when(vertx).createHttpServer(any(HttpServerOptions.class));
        doAnswer(invocation -> {
            ((Future<AsyncResult<?>>) invocation.getArgument(0)).complete();
            return null;
        }).when(httpServer).close(any());

        EwbNetworkServer ewbNetworkServer = createServer();

        ewbNetworkServer.startHttpServer();
        ewbNetworkServer.stop();

        verify(httpServer, times(1)).close(any());

        verify(onShutdown, times(1)).accept(ProgramStatus.OK);
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void configuresOptionalRoutesIfRequested() throws Exception {
        configureData(validDate, true, true, true, true);
        doReturn(true).when(routeDebug).isDebugging();
        dependencies.cors(".*");
        EwbNetworkServer ewbNetworkServer = createServer();

        assertThat(ewbNetworkServer.load(), equalTo(true));

        verify(router, times(NUM_DEBUG_ROUTES + NUM_CORS_ROUTES)).route();
        verify(router, times(NUM_EXPECTED_ROUTES)).route(anyString());

        doReturn(false).when(routeDebug).isDebugging();
        dependencies.cors(".*");
        ewbNetworkServer = createServer();

        clearInvocations(router);
        assertThat(ewbNetworkServer.load(), equalTo(true));

        verify(router, times(NUM_CORS_ROUTES)).route();
        verify(router, times(NUM_EXPECTED_ROUTES)).route(anyString());

        doReturn(true).when(routeDebug).isDebugging();
        dependencies.cors("");
        ewbNetworkServer = createServer();

        clearInvocations(router);
        assertThat(ewbNetworkServer.load(), equalTo(true));

        verify(router, times(NUM_DEBUG_ROUTES)).route();
        verify(router, times(NUM_EXPECTED_ROUTES)).route(anyString());
    }

    @Test
    public void handlesMissingData() throws Exception {
        configureData(null, true, true, true, true);
        EwbNetworkServer ewbNetworkServer = createServer();

        assertThat(ewbNetworkServer.load(), equalTo(false));

        validateProcessCalls(0, 0, 0, 0, 0);

        verify(onShutdown, never()).accept(any());

        validateLog("Failed to find network model and id correlations file within");
    }

    @Test
    public void handlesNetworkLoadFailures() throws Exception {
        configureData(validDate, false, true, true, true);
        EwbNetworkServer ewbNetworkServer = createServer();

        assertThat(ewbNetworkServer.load(), equalTo(false));

        validateProcessCalls(1, 0, 0, 0, 0);
        validateLog("Failed to load network model.");
    }

    @Test
    public void handlesApplyEmptyPatchFailures() throws Exception {
        configureData(validDate, true, true, true, true);
        EwbNetworkServer ewbNetworkServer = createServer();

        assertThat(ewbNetworkServer.load(), equalTo(true));

        validateProcessCalls(1, 1, 1, 1, NUM_EXPECTED_ROUTES);
        validateLog("No patches to apply.");
    }

    @Test
    public void handlesApplyPatchFailures() throws Exception {
        configureData(validDate, true, false, true, true);
        EwbNetworkServer ewbNetworkServer = createServer();

        assertThat(ewbNetworkServer.load(), equalTo(false));

        validateProcessCalls(1, 1, 0, 0, 0);
        validateLog("Failed to apply patches.");
    }

    @Test
    public void handlesSaveResultsFailures() throws Exception {
        configureData(validDate, true, true, false, true);
        EwbNetworkServer ewbNetworkServer = createServer();

        assertThat(ewbNetworkServer.load(), equalTo(false));

        validateProcessCalls(1, 1, 1, 0, 0);
        validateLog("Failed to save patch results: test message");
    }

    @Test
    public void handlesIdCorrelatorLoadFailures() throws Exception {
        configureData(validDate, true, true, true, false);
        EwbNetworkServer ewbNetworkServer = createServer();

        assertThat(ewbNetworkServer.load(), equalTo(false));

        validateProcessCalls(1, 1, 1, 1, 0);
        validateLog("Failed to load idCorrelator caches");
    }

    @Test
    public void handlesExceptionsLoadingIdCorrelatorData() throws Exception {
        configureData(validDate, true, true, true, true);
        EwbNetworkServer ewbNetworkServer = createServer();

        Exception exception = new IdCorrelatorReadException("test message");
        doThrow(exception).when(idCorrelatorReader).read(any());

        assertThat(ewbNetworkServer.load(), equalTo(false));

        validateProcessCalls(1, 1, 1, 1, 0);
        validateLog("Exception caught while reading ID correlations.");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void handlesFailuresInHttpServerListen() {
        doReturn(httpServer).when(vertx).createHttpServer(any(HttpServerOptions.class));
        doAnswer(invocation -> {
            ((Handler<AsyncResult<?>>) invocation.getArgument(0)).handle(Future.failedFuture("test failure"));
            return null;
        }).when(httpServer).listen(any());

        EwbNetworkServer ewbNetworkServer = createServer();
        Future<Void> future = ewbNetworkServer.startHttpServer();

        assertThat(future.failed(), equalTo(true));
        validateLog("Failed to start HTTP server.");
        verify(vertx, times(1)).createHttpServer(any(HttpServerOptions.class));
        verify(httpServer, times(1)).requestHandler(any());
        verify(httpServer, times(1)).exceptionHandler(any());
        verify(httpServer, times(1)).listen(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void logsServerExceptions() throws Exception {
        configureData(validDate, true, true, true, true);
        EwbNetworkServer ewbNetworkServer = createServer();

        doReturn(httpServer).when(vertx).createHttpServer(any(HttpServerOptions.class));
        doAnswer(invocation -> {
            ((Handler<Throwable>) invocation.getArgument(0)).handle(new Throwable("test message"));
            return httpServer;
        }).when(httpServer).exceptionHandler(any());

        ewbNetworkServer.startHttpServer();

        validateLog("Exception caught in HTTP server.");
        validateLog("test message");
    }

    private ConductingEquipment createAsset(String normalFeeder, String currentFeeder) {
        ConductingEquipment asset = mock(ConductingEquipment.class, DefaultAnswer.of(String.class, "asset"));

        doReturn(Collections.singletonList(createFeederMock(normalFeeder))).when(asset).getNormalFeeders();
        doReturn(Collections.singletonList(createFeederMock(currentFeeder))).when(asset).getCurrentFeeders();

        return asset;
    }

    private void configureData(@Nullable LocalDate date,
                               boolean networkStatus,
                               boolean applyPatchesStatus,
                               boolean saveResultsStatus,
                               boolean idCorrelatorStatus) throws Exception {
        if (date != null) {
            networkDatabasePath = Paths.get(date.toString() + "-network-model.sqlite");
            idCorrelatorPath = Paths.get(date.toString() + "-id-correlations.json");

            doReturn(networkDatabasePath).when(ewbDataFilePaths).networkModel(date);
            doReturn(idCorrelatorPath).when(ewbDataFilePaths).correlations(date);
        }

        dependencies
            .setValidDatabasePath(networkDatabasePath)
            .setValidIdCorrelatorPath(idCorrelatorPath);

        doReturn(date).when(ewbDataFilePathsHelper).findClosestDateWithDbs(any(), anyInt());
        doReturn(networkStatus).when(databaseReader).load(any(), any(), any());
        if (!applyPatchesStatus)
            doReturn(null).when(patchProcessor).applyPatches();
        if (!saveResultsStatus)
            doThrow(new Exception("test message")).when(resultsWriter).save(any());
        doReturn(idCorrelatorStatus ? Collections.emptyList() : Collections.singletonList(mock(FailedCorrelationInfo.class))).when(idCorrelatorReader).read(any());
    }

    private EwbNetworkServer createServer() {
        return spy(new EwbNetworkServer(dependencies));
    }

    private void validateLog(String expectedMessage) {
        assertThat(systemOutRule.getLog(), containsString(expectedMessage));
    }

    private void validateProcessCalls(int expectedLoadCalls,
                                      int expectedPatchesCalls,
                                      int expectedReportCalls,
                                      int expectedIdCorrelatorCalls,
                                      int expectedPathRouterCalls) throws Exception {
        verify(ewbDataFilePathsHelper, times(1)).findClosestDateWithDbs(dependencies.currentDate(), dependencies.daysToSearch());

        verify(ewbDataFilePaths, times(expectedLoadCalls)).networkModel(validDate);
        verify(networkDatabaseProvider, times(expectedLoadCalls)).apply(networkDatabasePath);
        verify(databaseReader, times(expectedLoadCalls)).load(any(), any(), any());

        verify(patchProcessor, times(expectedPatchesCalls)).applyPatches();

        verify(resultsWriter, times(expectedReportCalls)).save(any());

        verify(ewbDataFilePaths, times(expectedIdCorrelatorCalls)).correlations(validDate);
        verify(idCorrelatorReaderProvider, times(expectedIdCorrelatorCalls)).apply(idCorrelatorPath);
        verify(idCorrelatorReader, times(expectedIdCorrelatorCalls)).read(any());

        verify(router, never()).route();
        verify(router, times(expectedPathRouterCalls)).route(anyString());

        verify(onShutdown, never()).accept(any());
    }

    private void addPatchResults() {
        dependencies.addPatchResult(new PatchResult(1)
            .addAffectedFeedersFromAsset(createAsset("f1", "f4"))
            .addAffectedFeedersFromAsset(createAsset("f1", "f4"))
            .addAffectedFeedersFromAsset(createAsset("f2", "f1"))
            .addError("error1")
            .addError("error2"));

        dependencies.addPatchResult(new PatchResult(2)
            .addAffectedFeedersFromAsset(createAsset("f3", "f5")));

        dependencies.addPatchResult(new PatchResult(3)
            .addError("error1"));

        dependencies.addPatchResult(new PatchResult(1)
            .addAffectedFeedersFromAsset(mock(ConductingEquipment.class, RETURNS_MOCKS))
            .addError("error1"));
    }

    private void validatePatchResults() throws Exception {
        ArgumentCaptor<JsonObject> argumentCaptor = ArgumentCaptor.forClass(JsonObject.class);
        verify(resultsWriter, times(1)).save(argumentCaptor.capture());

        JsonObject resultsObject = argumentCaptor.getValue();
        List<JsonObject> results = extractRequiredObjectList(resultsObject, "results");

        assertThat(results.size(), equalTo(4));

        validateResult(results.get(0), Arrays.asList("f1", "f2"), Arrays.asList("f1", "f4"), Arrays.asList("error1", "error2"));
        validateResult(results.get(1), Collections.singletonList("f3"), Collections.singletonList("f5"), Collections.emptyList());
        validateResult(results.get(2), Collections.emptyList(), Collections.emptyList(), Collections.singletonList("error1"));
        validateResult(results.get(3), Collections.emptyList(), Collections.emptyList(), Collections.singletonList("error1"));
    }

    private void validateResult(JsonObject result,
                                List<String> expectedNormalFeeders,
                                List<String> expectedCurrentFeeders,
                                List<String> expectedErrors) throws Exception {
        assertThat(extractRequiredStringList(result, "affectedNormalFeeders"), containsInAnyOrder(expectedNormalFeeders.toArray()));
        assertThat(extractRequiredStringList(result, "affectedCurrentFeeders"), containsInAnyOrder(expectedCurrentFeeders.toArray()));
        assertThat(extractRequiredStringList(result, "errors"), containsInAnyOrder(expectedErrors.toArray()));
    }

}
