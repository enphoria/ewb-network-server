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

import com.zepben.cimbend.cim.iec61970.base.auxiliaryequipment.AuxiliaryEquipment;
import com.zepben.cimbend.cim.iec61970.base.auxiliaryequipment.FaultIndicator;
import com.zepben.cimbend.cim.iec61970.base.core.ConductingEquipment;
import com.zepben.cimbend.cim.iec61970.base.core.ConnectivityNode;
import com.zepben.cimbend.cim.iec61970.base.core.IdentifiedObject;
import com.zepben.cimbend.cim.iec61970.base.core.PhaseCode;
import com.zepben.cimbend.cim.iec61970.base.wires.*;
import com.zepben.cimbend.common.ServiceDifferences;
import com.zepben.cimbend.database.sqlite.DatabaseReader;
import com.zepben.cimbend.diagram.DiagramServiceComparator;
import com.zepben.cimbend.network.NetworkService;
import com.zepben.cimbend.network.NetworkServiceComparator;
import com.zepben.cimbend.network.tracing.ConnectivityResult;
import com.zepben.ewbnetworkserver.data.*;
import com.zepben.ewbnetworkserver.patch.PatchResult;
import com.zepben.testutils.junit.SystemLogExtension;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.zepben.collectionutils.CollectionUtils.mapOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsMapWithSize.anEmptyMap;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"SameParameterValue", "ConstantConditions", "ResultOfMethodCallIgnored"})
public class PatchProcessorTest {

    @RegisterExtension
    public SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private MockPatchProcessorDependencies dependencies;

    @Test
    public void appliesPatchesInIdOrder() {
        PatchProcessor patchProcessor = createPatchProcessor(LargeNetworkTestData.networkToPatch(), LargeNetworkTestData.allOperationsPatch());

        List<PatchResult> patchResults = patchProcessor.applyPatches();

        assertThat(patchResults, notNullValue());
        assertThat(patchResults.size(), equalTo(4));
        assertThat(patchResults.stream().map(PatchResult::patchId).collect(Collectors.toList()), contains(1, 2, 3, 4));
        assertThat(patchResults.stream().flatMap(pr -> pr.errors().stream()).collect(Collectors.toList()), empty());

        validatePatchedNetwork(dependencies.services(), LargeNetworkTestData.patchedNetwork());
    }

    @Test
    public void canRemoveTerminations() {
        PatchProcessor patchProcessor = createPatchProcessor(LargeNetworkTestData.networkToPatch(), LargeNetworkTestData.allOperationsPatch());

        patchProcessor.applyPatches();

        validateConnections(dependencies.services(), "10000004", "20000003", "20000007", "20000008");
        validateConnections(dependencies.services(), "10000008", "20000007");
        validateConnections(dependencies.services(), "10000009", "20000008");
        validateConnections(dependencies.services(), "20000007", "10000004", "10000008", "20000008");
        validateConnections(dependencies.services(), "20000008", "10000004", "10000009", "20000007");

        doReturn(LargeNetworkTestData.removeTerminationsPatch()).when(dependencies.response()).getBody();
        patchProcessor.applyPatches();

        validateConnections(dependencies.services(), "10000004", "20000003");
        validateConnections(dependencies.services(), "10000008");
        validateConnections(dependencies.services(), "10000009");
        validateConnections(dependencies.services(), "20000007");
        validateConnections(dependencies.services(), "20000008");
    }

    @Test
    public void usesAuthHeadersIfRequested() {
        PatchProcessor patchProcessor = createPatchProcessor(LargeNetworkTestData.networkToPatch(),
            LargeNetworkTestData.allOperationsPatch(),
            () -> dependencies.authHeader("auth header"));

        assertThat(patchProcessor.applyPatches(), notNullValue());

        verify(dependencies.httpClient(), times(1)).get(any(), eq(mapOf("Authorization", "auth header")), any(), any(), any());
    }

    @Test
    public void canBeDisabled() {
        PatchProcessor patchProcessor = createPatchProcessor(LargeNetworkTestData.networkToPatch(),
            LargeNetworkTestData.allOperationsPatch(),
            () -> dependencies.api("").authHeader(""));

        assertThat(patchProcessor.applyPatches(), empty());

        verify(dependencies.httpClient(), never()).get(any(), any(), any(), any(), any());
    }

    @Test
    public void usesDefaultValuesForOptionalPatchProperties() {
        PatchProcessor patchProcessor = createPatchProcessor(LargeNetworkTestData.networkToPatch(), NoOptionalValuesPatch.patch());

        List<PatchResult> patchResults = patchProcessor.applyPatches();

        assertThat(patchResults, notNullValue());
        assertThat(patchResults.size(), equalTo(1));
        assertThat(patchResults.stream().map(PatchResult::patchId).collect(Collectors.toList()), contains(1));
        assertThat(patchResults.stream().flatMap(pr -> pr.errors().stream()).collect(Collectors.toList()), empty());
        assertThat(patchResults.stream().flatMap(pr -> pr.warnings().stream()).collect(Collectors.toList()),
            containsInAnyOrder("Feature 'id1': Property 'NORMAL_STATE' not found, defaulting to 'closed'.",
                "Feature 'id1': Property 'DESCRIPTION' not found, defaulting to 'id1'.",
                "Feature 'id1': Property 'OPERATING_VOLTAGE' not found, defaulting to '22000'.",
                "Feature 'id2': Property 'DESCRIPTION' not found, defaulting to 'id2'.",
                "Feature 'id2': Property 'Primary Voltage' not found, defaulting to '22000'.",
                "Feature 'id2': Property 'Secondary Voltage' not found, defaulting to 'lv'.",
                "Feature 'id3': Property 'OPERATING_VOLTAGE' not found, defaulting to '22000'."));

        validateAsset(Recloser.class, "id1", "id1", PhaseCode.ABC, false, 22000);
        validateAsset(PowerTransformer.class, "id2", "id2", PhaseCode.ABC, false, 22000, 415);
        validateAsset("id3", PhaseCode.ABC, 22000);
    }

    @Test
    public void canCreateAllTypes() {
        PatchProcessor patchProcessor = createPatchProcessor(new Services(), AddAllTypesPatch.patch());

        List<PatchResult> patchResults = patchProcessor.applyPatches();

        assertThat(patchResults, notNullValue());
        assertThat(patchResults.size(), equalTo(1));
        assertThat(patchResults.stream().map(PatchResult::patchId).collect(Collectors.toList()), contains(1));
        assertThat(patchResults.stream().flatMap(pr -> pr.errors().stream()).collect(Collectors.toList()), empty());

        validateAsset(Recloser.class, "id1", "name1", PhaseCode.ABCN, false, 415);
        validateNetworkIgnored("id2");
        validateNetworkIgnored("id3");
        validateAsset(LinearShuntCompensator.class, "id4", "id4", PhaseCode.ABC, false, 22000);
        validateAsset(FaultIndicator.class, "id5", "id5");
        validateAsset(AcLineSegment.class, "id6", "id6", PhaseCode.X, false, 12700);
        validateAsset(AcLineSegment.class, "id7", "id7", PhaseCode.ABC, false, 6600);
        validateAsset(Breaker.class, "id8", "name8", PhaseCode.ABC, false, 6600);
        validateAsset(AcLineSegment.class, "id9", "id9", PhaseCode.ABC, false, 6600);
        validateAsset(Disconnector.class, "id10", "name10", PhaseCode.ABC, true, 6600);
        validateNetworkIgnored("id11");
        validateAsset(AcLineSegment.class, "id12", "id12", PhaseCode.ABCN, false, 415);
        validateAsset(AcLineSegment.class, "id13", "id13", PhaseCode.ABCN, false, 415);
        validateAsset(AcLineSegment.class, "id14", "id14", PhaseCode.ABCN, false, 415);
        validateAsset(Breaker.class, "id15", "name15", PhaseCode.ABCN, false, 415);
        validateAsset(AcLineSegment.class, "id16", "id16", PhaseCode.ABCN, false, 415);
        validateAsset(Disconnector.class, "id17", "name17", PhaseCode.ABCN, false, 415);
        validateNetworkIgnored("id18");
        validateNetworkIgnored("id19");
        validateNetworkIgnored("id20");
        validateAsset(PowerTransformer.class, "id21", "name21", PhaseCode.ABC, false, 11000, 11000);
        validateNetworkIgnored("id22");
        validateNetworkIgnored("id23");
        validateNetworkIgnored("id24");
        validateAsset(EnergyConsumer.class, "id25", "id25", PhaseCode.ABC, false, 22000);
        ConnectivityNode cn26 = dependencies.services().networkService().get(ConnectivityNode.class, "id26");
        validateNetworkIgnored("id27");
        validateAsset(PowerTransformer.class, "id28", "name28", PhaseCode.ABC, false, 11000, 415);
        validateAsset(PowerTransformer.class, "id29", "name29", PhaseCode.ABC, false, 11000, 12700);
        validateAsset(PowerTransformer.class, "id30", "name30", PhaseCode.ABC, false, 11000, 11000);
        validateAsset(Fuse.class, "id31", "name31", PhaseCode.ABC, false, 22000);

        assertThat(cn26.getTerminals().stream().map(t -> t.getConductingEquipment().getMRID()).collect(Collectors.toList()), containsInAnyOrder("id7", "id8"));

        assertThat(dependencies.patchTerminationProcessor().hasUsedId("id27"), equalTo(true));
        assertThat(dependencies.services().networkService().get(FaultIndicator.class, "id5").getTerminal().getConductingEquipment(),
            equalTo(dependencies.services().networkService().get(ConductingEquipment.class, "id6")));
    }

    public void validateNetworkIgnored(String mRID) {
        assertThat(dependencies.services().networkService().get(IdentifiedObject.class, mRID), nullValue());
    }

    @Test
    public void handlesUnknownValuesInProperties() {
        PatchProcessor patchProcessor = createPatchProcessor(LargeNetworkTestData.networkToPatch(), InvalidPatches.unknownValuesPatch());

        List<PatchResult> patchResults = patchProcessor.applyPatches();

        assertThat(patchResults, notNullValue());
        assertThat(patchResults.size(), equalTo(1));
        assertThat(patchResults.stream().map(PatchResult::patchId).collect(Collectors.toList()), contains(1));
        assertThat(patchResults.stream().flatMap(pr -> pr.errors().stream()).collect(Collectors.toList()), empty());
        assertThat(patchResults.stream().flatMap(pr -> pr.warnings().stream()).collect(Collectors.toList()),
            containsInAnyOrder("Feature 'id1': Ignoring invalid value 'unknown1' for property 'NORMAL_STATE', defaulting to 'closed'.",
                "Feature 'id1': Ignoring invalid value 'unknown2' for property 'OPERATING_VOLTAGE', defaulting to '22000'.",
                "Feature 'id2': Ignoring invalid value 'unknown3' for property 'Primary Voltage', defaulting to '22000'.",
                "Feature 'id2': Ignoring invalid value 'unknown4' for property 'Secondary Voltage', defaulting to 'lv'.",
                "Feature 'id3': Ignoring invalid value 'unknown5' for property 'OPERATING_VOLTAGE', defaulting to '22000'."));

        validateAsset(Recloser.class, "id1", "name1", PhaseCode.ABC, false, 22000);
        validateAsset(PowerTransformer.class, "id2", "name2", PhaseCode.ABC, false, 22000, 415);
        validateAsset("id3", PhaseCode.ABC, 22000);
    }

    @Test
    public void handlesInvalidBodies() {
        PatchProcessor patchProcessor = createPatchProcessor(LargeNetworkTestData.networkToPatch(), "{}");

        assertThat(patchProcessor.applyPatches(), nullValue());

        assertThat(systemOutRule.getLog(), containsString("Exception while trying to apply patches:"));
    }

    @Test
    public void handlesExceptions() {
        PatchProcessor patchProcessor = createPatchProcessor(LargeNetworkTestData.networkToPatch(), LargeNetworkTestData.allOperationsPatch());

        doThrow(new RuntimeException("test message")).when(dependencies.response()).getStatus();

        assertThat(patchProcessor.applyPatches(), nullValue());

        assertThat(systemOutRule.getLog(), containsString("Exception while trying to apply patches: test message"));
    }

    @Test
    public void handlesUnknownLayers() {
        PatchProcessor patchProcessor = createPatchProcessor(LargeNetworkTestData.networkToPatch(), InvalidPatches.unknownLayersPatch());

        List<PatchResult> patchResults = patchProcessor.applyPatches();

        assertThat(patchResults, notNullValue());
        assertThat(patchResults.size(), equalTo(1));
        assertThat(patchResults.get(0).errors().size(), equalTo(2));
        assertThat(patchResults.get(0).errors().get(0), containsString("Ignoring add feature with unsupported layer 'SOME_LAYER_I_DONT_KNOW'."));
        assertThat(patchResults.get(0).errors().get(1), containsString("Ignoring add feature with unsupported layer 'SOME_OTHER_LAYER'."));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void logsFailures() {
        PatchProcessor patchProcessor = createPatchProcessor(LargeNetworkTestData.networkToPatch(), LargeNetworkTestData.allOperationsPatch());

        doAnswer(invocation -> ((BiFunction<HttpResponseStatus, String, String>) invocation.getArgument(3))
            .apply(HttpResponseStatus.INTERNAL_SERVER_ERROR, "body text"))
            .when(dependencies.httpClient())
            .get(any(), any(), any(), any(), any());

        assertThat(patchProcessor.applyPatches(), nullValue());

        assertThat(systemOutRule.getLog(), containsString("Failed to retrieve patches: Internal Server Error [500] - body text"));
    }

    @Test
    public void removesFromLoopsCorrectly() {
        PatchProcessor patchProcessor = createPatchProcessor(LoopsNetwork.toPatch(), LoopsNetwork.patch());

        patchProcessor.applyPatches();

        // ERIS-1487 - Known bug in the remove phases trace removing items from loops.
//        validatePatchedNetwork(dependencies.services(), LoopsNetwork.patched());
    }

    @Test
    public void canCutConductors() {
        PatchProcessor patchProcessor = createPatchProcessor(CutConductorNetwork.toPatch(), CutConductorNetwork.patch());

        patchProcessor.applyPatches();

        validatePatchedNetwork(dependencies.services(), CutConductorNetwork.patched());
    }

    @Test
    void raisesWarningIfCantConnectTermination() {
        PatchProcessor patchProcessor = createPatchProcessor(ExtraTerminationNetwork.toPatch(), ExtraTerminationNetwork.patch());

        List<PatchResult> patchResults = patchProcessor.applyPatches();

        assertThat(patchResults.stream().flatMap(pr -> pr.errors().stream()).collect(Collectors.toList()), empty());
        assertThat(patchResults.stream().flatMap(pr -> pr.warnings().stream()).collect(Collectors.toList()),
            containsInAnyOrder("Unable to connect Breaker b1 [10000001] to termination '30000003', no viable terminal found."));

        validatePatchedNetwork(dependencies.services(), ExtraTerminationNetwork.patched());
    }

    @Test
    void raisesWarningIfInvalidAssetSpecifiedByTermination() {
        PatchProcessor patchProcessor = createPatchProcessor(InvalidTerminationsNetwork.toPatch(), InvalidTerminationsNetwork.patch());

        List<PatchResult> patchResults = patchProcessor.applyPatches();

        assertThat(patchResults.stream().flatMap(pr -> pr.errors().stream()).collect(Collectors.toList()),
            containsInAnyOrder("Unable to find required assets for termination '30000002'. Failed to find [30000001]."));
        assertThat(patchResults.stream().flatMap(pr -> pr.warnings().stream()).collect(Collectors.toList()),
            containsInAnyOrder("Unable to use ConnectivityNode 30000001 for termination '30000002' as it is not ConductingEquipment or AuxiliaryEquipment."));

        validatePatchedNetwork(dependencies.services(), InvalidTerminationsNetwork.patched());
    }

    @Disabled
    @Test
    void processRealOptionsFileWithoutNetwork() throws IOException {
        systemOutRule.unmute();

        Services services = new Services();
        PatchProcessor patchProcessor = createPatchProcessor(services, new String(Files.readAllBytes(Paths.get("C:\\Working\\pcor\\nb\\options\\options.json")), UTF_8));

        List<PatchResult> patchResults = patchProcessor.applyPatches();

        assertThat(patchResults, not(empty()));
    }

    @Disabled
    @Test
    void processRealOptionsFileWithNetwork() throws IOException {
        //
        // NOTE: Make sure you run this test with the following options:
        //       -Xmx48g -Xms48g
        //
        systemOutRule.unmute();

        Services services = new Services();
        DatabaseReader databaseReader = new DatabaseReader("C:\\Working\\ewb\\data\\powercor\\current-9001\\2020-09-07\\2020-09-07-network-model.sqlite");
        databaseReader.load(services.metadataCollection, services.networkService(), services.diagramService(), services.customerService());

        PatchProcessor patchProcessor = createPatchProcessor(services, new String(Files.readAllBytes(Paths.get("C:\\Working\\pcor\\nb\\options\\options.json")), UTF_8));

        List<PatchResult> patchResults = patchProcessor.applyPatches();

        assertThat(patchResults, not(empty()));
    }

    private PatchProcessor createPatchProcessor(Services services, String patch) {
        return createPatchProcessor(services, patch, () -> { });
    }

    private PatchProcessor createPatchProcessor(Services services, String patch, Runnable customiseDependencies) {
        dependencies = new MockPatchProcessorDependencies(services, patch);
        customiseDependencies.run();
        return new PatchProcessor(dependencies);
    }

    private void validatePatchedNetwork(Services actual, Services expected) {
        ServiceDifferences differences = new NetworkServiceComparator().compare(actual.networkService(), expected.networkService());
        System.out.println(differences.toString());
        assertThat("network: missing from actual", differences.missingFromSource(), empty());
        assertThat("network: missing from expected", differences.missingFromTarget(), empty());
        assertThat("network: unexpected modifications", differences.modifications(), anEmptyMap());

        differences = new DiagramServiceComparator().compare(actual.diagramService(), expected.diagramService());
        System.out.println(differences.toString());
        assertThat("diagram: missing from actual", differences.missingFromSource(), empty());
        assertThat("diagram: missing from expected", differences.missingFromTarget(), empty());
        assertThat("diagram: unexpected modifications", differences.modifications(), anEmptyMap());
    }

    private void validateAsset(Class<? extends ConductingEquipment> expectedClass,
                               String mRID,
                               String expectedName,
                               PhaseCode expectedPhases,
                               boolean expectedOpenStatus,
                               Integer... expectedVoltages) {
        ConductingEquipment conductingEquipment = dependencies.services().networkService().get(ConductingEquipment.class, mRID);
        assertThat(conductingEquipment, instanceOf(expectedClass));
        assertThat(conductingEquipment.getName(), equalTo(expectedName));
        conductingEquipment.getTerminals().forEach(terminal -> assertThat(terminal.getPhases(), equalTo(expectedPhases)));

        if (conductingEquipment instanceof Switch) {
            Switch aSwitch = (Switch) conductingEquipment;
            expectedPhases.singlePhases().forEach(phase -> {
                assertThat(aSwitch.isNormallyOpen(phase), equalTo(expectedOpenStatus));
                assertThat(aSwitch.isOpen(phase), equalTo(expectedOpenStatus));
            });
        } else {
            assertThat(conductingEquipment.getNormallyInService(), not(equalTo(expectedOpenStatus)));
            assertThat(conductingEquipment.getInService(), not(equalTo(expectedOpenStatus)));
        }

        if (conductingEquipment instanceof PowerTransformer) {
            assertThat(((PowerTransformer) conductingEquipment).numEnds(), greaterThanOrEqualTo(1));
            assertThat(((PowerTransformer) conductingEquipment).numEnds(), equalTo(expectedVoltages.length));
            Set<Integer> voltageSet = Arrays.stream(expectedVoltages).collect(Collectors.toSet());
            ((PowerTransformer) conductingEquipment).getEnds().stream().anyMatch(end ->
                Arrays.stream(expectedVoltages).anyMatch(expectedVoltage -> voltageSet.contains(end.getRatedU())
                    || ((end.getBaseVoltage() != null) && voltageSet.contains(end.getBaseVoltage().getNominalVoltage()))
                )
            );
        } else {
            assertThat(expectedVoltages.length, equalTo(1));
            assertThat(conductingEquipment.getBaseVoltageValue(), equalTo(expectedVoltages[0]));
        }
    }

    private void validateAsset(Class<? extends AuxiliaryEquipment> expectedClass,
                               String mRID,
                               String expectedName) {
        AuxiliaryEquipment conductingEquipment = dependencies.services().networkService().get(AuxiliaryEquipment.class, mRID);
        assertThat(conductingEquipment, instanceOf(expectedClass));
        assertThat(conductingEquipment.getName(), equalTo(expectedName));
    }

    private void validateAsset(String mRID, PhaseCode expectedPhases, int expectedVoltage) {
        AcLineSegment conductor = dependencies.services().networkService().get(AcLineSegment.class, mRID);
        assertThat(conductor, notNullValue());
        conductor.getTerminals().forEach(terminal -> assertThat(terminal.getPhases(), equalTo(expectedPhases)));

        assertThat(conductor.getBaseVoltageValue(), equalTo(expectedVoltage));
    }

    private void validateConnections(Services services, String assetId, String... expectedConnectedAssetIds) {
        List<String> connectedAssetIds = NetworkService.connectedEquipment(services.networkService().get(ConductingEquipment.class, assetId))
            .stream()
            .map(ConnectivityResult::getTo)
            .map(IdentifiedObject::getMRID)
            .collect(Collectors.toList());

        if (expectedConnectedAssetIds.length > 0)
            assertThat(connectedAssetIds, containsInAnyOrder(expectedConnectedAssetIds));
        else
            assertThat(connectedAssetIds, empty());
    }

}
