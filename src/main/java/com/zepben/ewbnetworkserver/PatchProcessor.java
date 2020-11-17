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

import com.google.common.collect.Lists;
import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.cimbend.cim.iec61968.assetinfo.OverheadWireInfo;
import com.zepben.cimbend.cim.iec61968.assets.Pole;
import com.zepben.cimbend.cim.iec61968.assets.Streetlight;
import com.zepben.cimbend.cim.iec61968.common.Location;
import com.zepben.cimbend.cim.iec61968.common.PositionPoint;
import com.zepben.cimbend.cim.iec61970.base.auxiliaryequipment.FaultIndicator;
import com.zepben.cimbend.cim.iec61970.base.core.*;
import com.zepben.cimbend.cim.iec61970.base.wires.*;
import com.zepben.cimbend.network.NetworkService;
import com.zepben.cimbend.network.model.PhaseDirection;
import com.zepben.cimbend.network.tracing.ConnectivityResult;
import com.zepben.cimbend.network.tracing.PhaseSelector;
import com.zepben.cimbend.network.tracing.Tracing;
import com.zepben.ewbnetworkserver.geojson.GeoJson;
import com.zepben.ewbnetworkserver.patch.*;
import com.zepben.nearestlocation.LocationUtility;
import com.zepben.vertxutils.json.JsonUtils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zepben.cimbend.common.extensions.IdentifiedObjectExtensionsKt.typeNameAndMRID;
import static com.zepben.collectionutils.CollectionUtils.mapOf;
import static com.zepben.ewbnetworkserver.ObjectCreators.createTerminals;
import static com.zepben.ewbnetworkserver.patch.PatchProperties.GIS_ID;
import static com.zepben.vertxutils.json.JsonUtils.*;
import static java.lang.Math.abs;

@EverythingIsNonnullByDefault
public class PatchProcessor {

    public static final OverheadWireInfo unknownWireInfo = new OverheadWireInfo();
    public static final PerLengthSequenceImpedance unknownPerLengthSequenceImpedance = new PerLengthSequenceImpedance();

    private static final Logger logger = LoggerFactory.getLogger("ewb-network-server");

    private final Services services;
    private final HttpClient httpClient;
    private final String api;
    private final String authHeader;
    private final PatchFeatureCreators patchFeatureCreators;
    private final PatchTerminationProcessor patchTerminationProcessor;
    private final FeederProcessor feederProcessor;

    private final Map<String, PhaseCode> assetPhases = new HashMap<>();

    PatchProcessor(Dependencies dependencies) {
        services = dependencies.services();
        httpClient = dependencies.httpClient();
        api = dependencies.api();
        authHeader = dependencies.authHeader();
        patchFeatureCreators = dependencies.patchFeatureCreators();
        patchTerminationProcessor = dependencies.patchTerminationProcessor();
        feederProcessor = dependencies.feederProcessor();

        services.networkService().add(unknownWireInfo);
        services.networkService().add(unknownPerLengthSequenceImpedance);
    }

    @Nullable
    List<PatchResult> applyPatches() {
        if (api.isEmpty())
            return Collections.emptyList();

        logger.info("   Requesting patches from '{}'...", api);

        return httpClient.get(api,
            mapOf("Authorization", authHeader),
            this::processPatches,
            this::failureHandler,
            this::exceptionHandler);
    }

    private List<PatchResult> processPatches(String responseBody) throws JsonUtils.ParsingException {
        logger.info("   Applying patches...");
        List<Patch> patches = convertToObjectList(new JsonArray(responseBody))
            .stream()
            .map(Patch::parse)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .sorted(Comparator.comparing(Patch::id))
            .collect(Collectors.toList());

        List<PatchFeature> addFeatures = new ArrayList<>();
        List<PatchFeature> addTerminations = new ArrayList<>();
        List<CutConductorAction> cutConductorActions = new ArrayList<>();
        List<PatchFeature> removeFeatures = new ArrayList<>();
        List<PatchFeature> removeTerminations = new ArrayList<>();

        patches.forEach(patch -> sortFeatures(patch, addFeatures, addTerminations, cutConductorActions, removeFeatures, removeTerminations));

        process(addFeatures, this::processAddFeature);
        process(addTerminations, this::processAddFeature);
        process(cutConductorActions, this::processCutConductorAction);
        process(removeTerminations, this::processRemoveFeature);
        process(removeFeatures, this::processRemoveFeature);

        logger.info("   Patches applied.");

        return patches
            .stream()
            .map(Patch::result)
            .sorted(Comparator.comparing(PatchResult::patchId))
            .collect(Collectors.toList());
    }

    private <T> void process(List<T> items, Processor<T> processor) throws ParsingException {
        for (T item : items)
            processor.process(item);
    }

    private void sortFeatures(Patch patch,
                              List<PatchFeature> addFeatures,
                              List<PatchFeature> addTerminations,
                              List<CutConductorAction> cutConductorActions,
                              List<PatchFeature> removeFeatures,
                              List<PatchFeature> removeTerminations) {
        Map<String, CutConductorAction> cutConductorActionsTracking = new LinkedHashMap<>();

        Function<PatchFeature, CutConductorAction> getOrCreateCutConductorAction = feature ->
            cutConductorActionsTracking.computeIfAbsent(feature.geoJson().actionGroup(), CutConductorAction::new);

        sortFeatures(patch.addFeatures(), getOrCreateCutConductorAction, CutConductorAction::includeAddFeature);
        sortFeatures(patch.removeFeatures(), getOrCreateCutConductorAction, CutConductorAction::includeRemoveFeature);

        cutConductorActionsTracking.values().forEach(cutConductorAction -> {
            if (cutConductorAction.isValid())
                cutConductorActions.add(cutConductorAction);
            else {
                filterValidFeatures(cutConductorAction.addFeatures(), addFeatures, addTerminations, "add", patch, false);
                filterValidFeatures(cutConductorAction.removeFeatures(), removeFeatures, removeTerminations, "remove", patch, true);
            }
        });
    }

    private void filterValidFeatures(List<PatchFeature> candidateFeatures,
                                     List<PatchFeature> validFeatures,
                                     List<PatchFeature> terminationFeatures,
                                     String description,
                                     Patch patch,
                                     boolean allowUnknown) {
        for (PatchFeature feature : candidateFeatures) {
            if (feature.geoJson().patchLayer() == PatchLayer.terminations)
                terminationFeatures.add(feature);
            else if (allowUnknown || (feature.geoJson().patchLayer() != PatchLayer.UNKNOWN))
                validFeatures.add(feature);
            else {
                patch.result().addError(
                    "Ignoring %s feature with unsupported layer '%s'. If this is a valid layer then you should request for support to be added to the patch processor, otherwise correct the source data.",
                    description,
                    feature.geoJson().layer());
            }
        }
    }

    private void sortFeatures(List<PatchFeature> features,
                              Function<PatchFeature, CutConductorAction> getCutConductorAction,
                              BiConsumer<CutConductorAction, PatchFeature> includeFeature) {
        features.forEach(feature -> {
            CutConductorAction cutConductorAction = getCutConductorAction.apply(feature);
            includeFeature.accept(cutConductorAction, feature);
        });
    }

    private void processCutConductorAction(CutConductorAction action) throws ParsingException {
        //
        // 1. store the connectivity of the original conductor to use for reconnection later.
        // 2. remove the original conductor.
        // 3. add the replacement conductors connecting to previous connectivity nodes.
        // 4. add feeders and phases from any connected terminals that feed into the new conductors.
        //
        Conductor conductorToRemove = getConductorFromFeature(action.removeLineFeature(), "cut");
        if (conductorToRemove == null)
            return;

        Map<Terminal, String> connectivity = extractConnectivity(conductorToRemove);

        processRemoveFeature(action.removeLineFeature());
        processAddFeature(action.addLineFeature1());
        processAddFeature(action.addLineFeature2());

        Conductor conductor1 = connectCutConductor(action.addLineFeature1(), conductorToRemove, connectivity);
        Conductor conductor2 = connectCutConductor(action.addLineFeature2(), conductorToRemove, connectivity);

        setPhasesAndFeeders(conductor1);
        setPhasesAndFeeders(conductor2);
    }

    @Nullable
    private Conductor getConductorFromFeature(PatchFeature patchFeature, String description) {
        GeoJson geoJson = patchFeature.geoJson();
        PatchResult patchResult = patchFeature.patch().result();

        Conductor conductor = services.networkService().get(AcLineSegment.class, geoJson.gisId());
        if (conductor == null) {
            patchResult.addError("Ignoring request to %s conductor '%s' which was not found.", description, geoJson.gisId());
            return null;
        }
        return conductor;
    }

    private Map<Terminal, String> extractConnectivity(Conductor conductor) {
        return conductor.getTerminals()
            .stream()
            .filter(Terminal::isConnected)
            .collect(Collectors.toMap(t -> t, this::getConnectivityNodeId));
    }

    private String getConnectivityNodeId(Terminal terminal) {
        String connectivityNodeId = terminal.connectivityNodeId();
        return connectivityNodeId != null ? connectivityNodeId : "";
    }

    @Nullable
    private List<PatchResult> failureHandler(HttpResponseStatus status, String responseBody) {
        logger.error("   Failed to retrieve patches: {} [{}] - {}", status.reasonPhrase(), status.code(), responseBody);
        return null;
    }

    @Nullable
    private List<PatchResult> exceptionHandler(Exception e) {
        logger.error("   Exception while trying to apply patches: {}", e.getMessage(), e);
        return null;
    }

    private void processAddFeature(PatchFeature patchFeature) throws ParsingException {
        GeoJson geoJson = patchFeature.geoJson();
        PatchResult patchResult = patchFeature.patch().result();

        if (isIdAlreadyInUse(geoJson.gisId(), geoJson.patchLayer(), patchResult))
            return;

        switch (geoJson.patchLayer()) {
            case acr:
                patchFeatureCreators.addRecloser(geoJson, assetPhases, patchResult);
                break;
            case annotationTextBuilder:
            case builderConduit:
            case hvSwitchBoard:
            case lvSwitchBoard:
            case poles:
            case publicLights:
            case servicePits:
            case substation:
            case substationCoverage:
                // Do not parse.
                break;
            case capacitor:
                patchFeatureCreators.addCapacitor(geoJson, assetPhases, patchResult);
                break;
            case faultIndicators:
                patchFeatureCreators.addFaultIndicator(geoJson, patchResult);
                break;
            case hvBusBars:
            case hvCables:
            case hvLines:
            case lvBusBars:
            case lvCables:
            case lvCablesService:
            case lvLines:
                patchFeatureCreators.addLine(geoJson, assetPhases, patchResult);
                break;
            case hvCircuitBreakers:
            case lvCircuitBreakers:
                patchFeatureCreators.addBreaker(geoJson, assetPhases, patchResult);
                break;
            case hvSwitches:
            case lvSwitches:
                patchFeatureCreators.addDisconnector(geoJson, assetPhases, patchResult);
                break;
            case regulator:
                patchFeatureCreators.addRegulator(geoJson, assetPhases, patchResult);
                break;
            case supplyPoints:
                patchFeatureCreators.addSupplyPoint(geoJson, assetPhases, patchResult);
                break;
            case terminations:
                patchTerminationProcessor.connect(geoJson, assetPhases, patchResult);
                break;
            case transformers:
                patchFeatureCreators.addTransformer(geoJson, assetPhases, patchResult);
                break;
            case txProt:
                patchFeatureCreators.addFuse(geoJson, assetPhases, patchResult);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported layer '%s' used for add feature.", geoJson.patchLayer()));
        }
    }

    private void processRemoveFeature(PatchFeature patchFeature) throws ParsingException {
        GeoJson geoJson = patchFeature.geoJson();
        PatchResult patchResult = patchFeature.patch().result();

        switch (geoJson.patchLayer()) {
            case acr:
                removeAsset(geoJson, Recloser.class, patchResult, true);
                break;
            case faultIndicators:
                removeAsset(geoJson, FaultIndicator.class, patchResult, true);
                break;
            case capacitor:
                removeAsset(geoJson, LinearShuntCompensator.class, patchResult, true);
                break;
            case hvBusBars:
            case hvCables:
            case hvLines:
                removeAsset(geoJson, AcLineSegment.class, patchResult, true);
                break;
            case hvCircuitBreakers:
            case hvSwitches:
                removeAsset(geoJson, Switch.class, patchResult, true);
                break;
            case lvBusBars:
            case lvCables:
            case lvCablesService:
            case lvLines:
                removeAsset(geoJson, AcLineSegment.class, patchResult, false);
                break;
            case lvCircuitBreakers:
            case lvSwitches:
            case txProt:
                removeAsset(geoJson, Switch.class, patchResult, false);
                break;
            case poles:
                removeAsset(geoJson, Pole.class, patchResult, false);
                break;
            case publicLights:
                removeAsset(geoJson, Streetlight.class, patchResult, false);
                break;
            case regulator:
            case transformers:
                removeAsset(geoJson, PowerTransformer.class, patchResult, true);
                break;
            case supplyPoints:
                removeAsset(geoJson, EnergyConsumer.class, patchResult, true);
                break;
            case terminations:
                patchTerminationProcessor.disconnect(geoJson, patchResult);
                break;
            default:
                removeAsset(geoJson, IdentifiedObject.class, patchResult, false);
        }
    }

    private void removeAsset(GeoJson geoJson,
                             Class<? extends IdentifiedObject> expectedClass,
                             PatchResult patchResult,
                             boolean logIfMissing) throws ParsingException {
        String id = extractRequiredString(geoJson.properties(), GIS_ID);
        IdentifiedObject identifiedObject = services.networkService().get(IdentifiedObject.class, id);
        if (identifiedObject == null) {
            if (logIfMissing)
                patchResult.addWarning("Ignoring request to remove %s '%s' which was not found.", expectedClass.getSimpleName(), id);
            return;
        }

        if (!expectedClass.isAssignableFrom(identifiedObject.getClass())) {
            patchResult.addError("Ignoring request to remove %s '%s' because the item found has an incorrect type '%s'.",
                expectedClass.getSimpleName(),
                id,
                identifiedObject.getClass().getSimpleName());
            return;
        }

        removeAssetLinkages(expectedClass.cast(identifiedObject), patchResult);
    }

    private void removeAssetLinkages(IdentifiedObject identifiedObject, PatchResult patchResult) {
        services.diagramService().getDiagramObjects(identifiedObject.getMRID()).forEach(services.diagramService()::remove);

        if (identifiedObject instanceof ConductingEquipment) {
            ConductingEquipment conductingEquipment = (ConductingEquipment) identifiedObject;
            patchResult.addAffectedFeedersFromAsset(conductingEquipment);

            feederProcessor.removeDownstream(conductingEquipment);
            Tracing.removePhases().run(conductingEquipment);

            conductingEquipment.getTerminals().forEach(terminal -> {
                services.networkService().disconnect(terminal);
                services.networkService().remove(terminal);
            });
        }

        if (identifiedObject instanceof Equipment) {
            Equipment equipment = (Equipment) identifiedObject;
            equipment.getContainers().forEach(container -> container.removeEquipment(equipment));
            equipment.getOperationalRestrictions().forEach(operationalRestriction -> operationalRestriction.removeEquipment(equipment));
        }

        services.networkService().tryRemove(identifiedObject);

        if (identifiedObject instanceof PowerSystemResource) {
            PowerSystemResource powerSystemResource = (PowerSystemResource) identifiedObject;
            Location location = powerSystemResource.getLocation();
            if (location != null)
                services.networkService().remove(location);
        }
    }

    private boolean isIdAlreadyInUse(String id, PatchLayer patchLayer, PatchResult patchResult) {
        boolean exists = services.networkService().contains(id) || patchTerminationProcessor.hasUsedId(id);

        if (exists)
            patchResult.addError("Unable to add %s '%s', the id is already in use.", patchLayer, id);

        return exists;
    }

    @Nullable
    private Conductor connectCutConductor(PatchFeature patchFeature,
                                          Conductor originalConductor,
                                          Map<Terminal, String> connectivity) {
        PatchResult patchResult = patchFeature.patch().result();

        Conductor conductor = getConductorFromFeature(patchFeature, "reconnect cut");
        if (conductor == null)
            return null;

        List<Terminal> originalTerminals = new ArrayList<>(originalConductor.getTerminals());

        Location location = Objects.requireNonNull(conductor.getLocation());
        List<PositionPoint> positionPoints = location.getPoints();
        List<PositionPoint> originalPositionPoints = Objects.requireNonNull(originalConductor.getLocation()).getPoints();

        if (first(positionPoints).equals(first(originalPositionPoints)))
            connectCutConductor(conductor, positionPoints, originalPositionPoints, originalTerminals, connectivity, patchResult);
        else if (first(positionPoints).equals(last(originalPositionPoints)))
            connectCutConductor(conductor, positionPoints, reverseCopy(originalPositionPoints), Lists.reverse(originalTerminals), connectivity, patchResult);
        else if (last(positionPoints).equals(first(originalPositionPoints)))
            connectCutConductor(conductor, reversePath(location), originalPositionPoints, originalTerminals, connectivity, patchResult);
        else if (last(positionPoints).equals(last(originalPositionPoints)))
            connectCutConductor(conductor, reversePath(location), reverseCopy(originalPositionPoints), Lists.reverse(originalTerminals), connectivity, patchResult);
        else
            patchResult.addError("Failed to reconnect cut conductor '%s', original conductor '%s' pathing does not align with new pathing.", conductor.getMRID(), originalConductor.getMRID());

        return conductor;
    }

    private List<PositionPoint> reversePath(Location location) {
        List<PositionPoint> positionPoints = reverseCopy(location.getPoints());
        location.clearPoints();
        positionPoints.forEach(location::addPoint);

        return location.getPoints();
    }

    private void connectCutConductor(Conductor conductor,
                                     List<PositionPoint> path,
                                     List<PositionPoint> originalPath,
                                     List<Terminal> originalTerminals,
                                     Map<Terminal, String> connectivity,
                                     PatchResult patchResult) {
        List<Terminal> terminalsToConnect = getTerminalsToConnect(path, originalPath, originalTerminals, patchResult);

        if (terminalsToConnect.size() > 1)
            createTerminals(services, conductor, assetPhases.getOrDefault(conductor.getMRID(), PhaseCode.ABC), terminalsToConnect.size() - 1);

        for (int i = 0; i < terminalsToConnect.size(); ++i) {
            connect(Objects.requireNonNull(conductor.getTerminal(i + 1)),
                terminalsToConnect.get(i),
                connectivity);
        }

        Tracing.setPhases().run(conductor, services.networkService().listOf(Breaker.class));
    }

    private List<Terminal> getTerminalsToConnect(List<PositionPoint> path,
                                                 List<PositionPoint> originalPath,
                                                 List<Terminal> originalTerminals,
                                                 PatchResult patchResult) {
        List<Terminal> terminalsToConnect = new ArrayList<>();
        terminalsToConnect.add(originalTerminals.get(0));

        if (originalTerminals.size() == 2)
            return terminalsToConnect;

        PositionPoint endPoint = path.get(path.size() - 1);
        int index = 0;
        boolean found = false;
        while (!found && (index < originalPath.size() - 2)) {
            if (endPoint.equals(originalPath.get(index + 1))) {
                ++index;
                found = true;
            } else if (onLineBetween(originalPath.get(index), originalPath.get(index + 1), endPoint))
                found = true;
            else
                ++index;
        }

        if (!found) {
            patchResult.addError("Failed to find cut point on original conductor, connectivity could not be re-established.");
            return terminalsToConnect;
        }

        List<PositionPoint> includePath = originalPath.subList(0, index + 1);
        List<PositionPoint> excludePath = originalPath.subList(index + 1, originalPath.size());

        originalTerminals.subList(1, originalTerminals.size() - 1)
            .stream()
            .filter(terminal -> shouldConnectTerminal(terminal, includePath, excludePath, patchResult))
            .forEach(terminalsToConnect::add);

        return terminalsToConnect;
    }

    private void connect(Terminal terminal, Terminal originalTerminal, Map<Terminal, String> connectivity) {
        services.networkService().connect(terminal, connectivity.get(originalTerminal));
    }

    private void setPhasesAndFeeders(@Nullable Conductor conductor) {
        if (conductor == null)
            return;

        setPhasesAndFeeders(conductor, Terminal::normalPhases);
        setPhasesAndFeeders(conductor, Terminal::currentPhases);
    }

    private void setPhasesAndFeeders(Conductor conductor, PhaseSelector phaseSelector) {
        Set<Terminal> connectedFeedTerminals = new HashSet<>();
        conductor.getTerminals().forEach(terminal -> NetworkService.connectedTerminals(terminal)
            .stream()
            .map(ConnectivityResult::getToTerminal)
            .filter(connectedTerminal -> terminal.getPhases().singlePhases().stream()
                .map(phase -> phaseSelector.status(connectedTerminal, phase))
                .anyMatch(phaseStatus -> phaseStatus.direction().has(PhaseDirection.OUT)))
            .forEach(connectedFeedTerminals::add)
        );

        connectedFeedTerminals.forEach(terminal -> {
            Tracing.setPhases().run(Objects.requireNonNull(terminal.getConductingEquipment()), services.networkService().listOf(Breaker.class));
            feederProcessor.applyDownstream(terminal.getConductingEquipment());
        });
    }

    private boolean shouldConnectTerminal(Terminal terminal, List<PositionPoint> includePath, List<PositionPoint> excludePath, PatchResult patchResult) {
        PositionPoint terminalLocation = getTerminalLocation(terminal, patchResult);
        if (includePath.contains(terminalLocation))
            return true;

        if (excludePath.contains(terminalLocation))
            return false;

        return calcDistanceSquared(terminalLocation, includePath) < calcDistanceSquared(terminalLocation, excludePath);
    }

    @Nullable
    private PositionPoint getTerminalLocation(Terminal terminal, PatchResult patchResult) {
        ConductingEquipment conductingEquipment = Objects.requireNonNull(terminal.getConductingEquipment());

        Location location = conductingEquipment.getLocation();
        if (location == null)
            return null;

        List<PositionPoint> positionPoints = location.getPoints();
        if (positionPoints.isEmpty())
            return null;
        else if (conductingEquipment instanceof AcLineSegment) {
            int index = conductingEquipment.getTerminals().indexOf(terminal);
            if (index == 0)
                return positionPoints.get(0);
            else if (index == conductingEquipment.numTerminals() - 1)
                return positionPoints.get(positionPoints.size() - 1);
            else if (conductingEquipment.numTerminals() == positionPoints.size())
                return positionPoints.get(index);
            else {
                patchResult.addError("UNSUPPORTED OPERATION: Failed to connect %s which was a mid-span terminal of a conductor with pathing information beyond the terminal locations.", typeNameAndMRID(terminal));
                return null;
            }
        } else
            return positionPoints.get(0);
    }

    private double calcDistanceSquared(@Nullable PositionPoint lngLat, List<PositionPoint> path) {
        double x;
        double y;
        if (lngLat != null) {
            x = lngLat.getXPosition();
            y = lngLat.getYPosition();
        } else {
            x = 0;
            y = 0;
        }

        return path
            .stream()
            .map(ll -> LocationUtility.calculateSquaredDistance(ll.getYPosition(), ll.getXPosition(), 0, y, x, 0))
            .min(Double::compare)
            .orElse(Double.MAX_VALUE);
    }

    private boolean onLineBetween(PositionPoint start, PositionPoint end, PositionPoint mid) {
        boolean longitudeIsBetween = ((start.getXPosition() < mid.getXPosition()) && (end.getXPosition() > mid.getXPosition()))
            || ((start.getXPosition() > mid.getXPosition()) && (end.getXPosition() < mid.getXPosition()));

        boolean latitudeIsBetween = ((start.getYPosition() < mid.getYPosition()) && (end.getYPosition() > mid.getYPosition()))
            || ((start.getYPosition() > mid.getYPosition()) && (end.getYPosition() < mid.getYPosition()));

        if (start.getYPosition() == end.getYPosition())
            return longitudeIsBetween;
        else if (start.getXPosition() == end.getXPosition())
            return latitudeIsBetween;
        else if (!longitudeIsBetween || !latitudeIsBetween)
            return false;

        double rise1 = mid.getYPosition() - start.getYPosition();
        double run1 = mid.getXPosition() - start.getXPosition();
        double rise2 = end.getYPosition() - mid.getYPosition();
        double run2 = end.getXPosition() - mid.getXPosition();

        return abs((rise1 * run2) - (rise2 * run1)) < 0.0000001;
    }

    private <T> T first(List<T> list) {
        return list.get(0);
    }

    private <T> T last(List<T> list) {
        return list.get(list.size() - 1);
    }

    private <T> List<T> reverseCopy(List<T> original) {
        return Lists.reverse(new ArrayList<>(original));
    }

    @FunctionalInterface
    interface Processor<T> {

        void process(T item) throws ParsingException;

    }

    interface Dependencies {

        Services services();

        PatchFeatureCreators patchFeatureCreators();

        FeederProcessor feederProcessor();

        PatchTerminationProcessor patchTerminationProcessor();

        HttpClient httpClient();

        String api();

        String authHeader();

    }

}
