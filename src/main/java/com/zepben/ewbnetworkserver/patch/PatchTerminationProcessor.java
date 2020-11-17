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

package com.zepben.ewbnetworkserver.patch;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.cimbend.cim.iec61968.common.Location;
import com.zepben.cimbend.cim.iec61968.common.PositionPoint;
import com.zepben.cimbend.cim.iec61970.base.auxiliaryequipment.AuxiliaryEquipment;
import com.zepben.cimbend.cim.iec61970.base.core.ConductingEquipment;
import com.zepben.cimbend.cim.iec61970.base.core.IdentifiedObject;
import com.zepben.cimbend.cim.iec61970.base.core.PhaseCode;
import com.zepben.cimbend.cim.iec61970.base.core.Terminal;
import com.zepben.cimbend.cim.iec61970.base.wires.*;
import com.zepben.cimbend.network.NetworkService;
import com.zepben.cimbend.network.tracing.ConnectivityResult;
import com.zepben.cimbend.network.tracing.Tracing;
import com.zepben.ewbnetworkserver.Services;
import com.zepben.ewbnetworkserver.geojson.GeoJson;
import com.zepben.vertxutils.json.JsonUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.zepben.cimbend.common.extensions.IdentifiedObjectExtensionsKt.typeNameAndMRID;
import static com.zepben.ewbnetworkserver.ObjectCreators.createTerminal;
import static com.zepben.ewbnetworkserver.patch.PatchProperties.CONNECTED_ASSETS;
import static com.zepben.ewbnetworkserver.patch.PatchProperties.CONNECTED_ASSET_ID;
import static com.zepben.util.OptionalHelpers.firstOf;
import static com.zepben.vertxutils.json.JsonUtils.extractRequiredObjectList;
import static com.zepben.vertxutils.json.JsonUtils.extractRequiredString;

@EverythingIsNonnullByDefault
public class PatchTerminationProcessor {

    private final Services services;
    private final FeederProcessor feederProcessor;

    private final Map<Class<? extends ConductingEquipment>, Integer> maxTerminalsByType = new HashMap<>();

    private final Map<String, PatchTermination> terminations = new HashMap<>();

    private final Map<String, Map<PositionPoint, Terminal>> usedTerminalLocations = new HashMap<>();

    public PatchTerminationProcessor(Services services, FeederProcessor feederProcessor) {
        this.services = services;
        this.feederProcessor = feederProcessor;

        maxTerminalsByType.put(Junction.class, Integer.MAX_VALUE);
        maxTerminalsByType.put(Disconnector.class, 2);
        maxTerminalsByType.put(Jumper.class, 2);
        maxTerminalsByType.put(LinearShuntCompensator.class, 2);
        maxTerminalsByType.put(Breaker.class, 2);
        maxTerminalsByType.put(AcLineSegment.class, Integer.MAX_VALUE);
        maxTerminalsByType.put(PowerTransformer.class, 2);
        maxTerminalsByType.put(Fuse.class, 2);
        maxTerminalsByType.put(EnergySource.class, 1);
        maxTerminalsByType.put(EnergyConsumer.class, 2);
        maxTerminalsByType.put(Recloser.class, 2);
    }

    public boolean hasUsedId(String id) {
        return terminations.containsKey(id);
    }

    public void connect(GeoJson geoJson, Map<String, PhaseCode> assetPhases, PatchResult patchResult) throws JsonUtils.ParsingException {
        List<String> connectedIds = extractConnectedAssetIds(geoJson);
        if (connectedIds.size() < 2) {
            patchResult.addWarning("Insufficient assets to connect with termination '%s', found %d, requires at least 2. Future modifications/removals will generate errors.",
                geoJson.gisId(),
                connectedIds.size());
            terminations.put(geoJson.gisId(), null);
            return;
        }

        List<ConductingEquipment> conductingEquipment = new ArrayList<>();
        List<AuxiliaryEquipment> auxiliaryEquipment = new ArrayList<>();
        getEquipment(geoJson.gisId(), connectedIds, conductingEquipment, auxiliaryEquipment, patchResult);

        PatchTermination patchTermination = null;
        if (connectedIds.size() != (conductingEquipment.size() + auxiliaryEquipment.size()))
            addInvalidTerminationError(geoJson.gisId(), connectedIds, conductingEquipment, auxiliaryEquipment, patchResult);
        else if (conductingEquipment.isEmpty())
            patchResult.addWarning("Unable to connect multiple AuxiliaryEquipment without a ConductingEquipment for termination '%s'. Future modifications/removals will generate errors.", geoJson.gisId());
        else if (auxiliaryEquipment.isEmpty())
            patchTermination = connectConductingEquipment(geoJson.gisId(), conductingEquipment, geoJson.geometry().coordinate(), assetPhases, patchResult);
        else if (conductingEquipment.size() == 1)
            patchTermination = connectAuxiliaryEquipment(geoJson.gisId(), auxiliaryEquipment, conductingEquipment.get(0), geoJson.geometry().coordinate(), patchResult);
        else
            patchResult.addError("Unable to connect AuxiliaryEquipment to multiple ConductingEquipment for termination '%s'. Future modifications/removals will generate errors.", geoJson.gisId());

        terminations.put(geoJson.gisId(), patchTermination);
    }

    public void disconnect(GeoJson geoJson, PatchResult patchResult) throws JsonUtils.ParsingException {
        PatchTermination patchTermination = terminations.remove(geoJson.gisId());
        if (patchTermination == null) {
            patchResult.addWarning("Ignoring request to remove unknown or invalid termination '%s'.", geoJson.gisId());
            return;
        }

        List<String> connectedIds = extractConnectedAssetIds(geoJson);
        if (connectedIds.size() != (patchTermination.conductingEquipment().size() + patchTermination.auxiliaryEquipment().size()))
            patchResult.addWarning("Mismatch between assets in termination '%s' add and remove. Disconnecting assets from the add and ignoring the remove.", geoJson.gisId());

        if (patchTermination.auxiliaryEquipment().isEmpty())
            disconnectConductingEquipment(patchTermination);
        else
            disconnectAuxiliaryEquipment(patchTermination);
    }

    private List<String> extractConnectedAssetIds(GeoJson geoJson) throws JsonUtils.ParsingException {
        return extractRequiredObjectList(geoJson.properties(), CONNECTED_ASSETS)
            .stream()
            .map(o -> {
                try {
                    return extractRequiredString(o, CONNECTED_ASSET_ID);
                } catch (JsonUtils.ParsingException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());
    }

    private void getEquipment(String terminationId,
                              List<String> equipmentIds,
                              List<ConductingEquipment> conductingEquipment,
                              List<AuxiliaryEquipment> auxiliaryEquipment,
                              @Nullable PatchResult patchResult) {
        equipmentIds
            .stream()
            .map(mRID -> services.networkService().get(IdentifiedObject.class, mRID))
            .filter(Objects::nonNull)
            .forEach(identifiedObject -> {
                if (identifiedObject instanceof ConductingEquipment)
                    conductingEquipment.add((ConductingEquipment) identifiedObject);
                else if (identifiedObject instanceof AuxiliaryEquipment)
                    auxiliaryEquipment.add((AuxiliaryEquipment) identifiedObject);
                else if (patchResult != null)
                    patchResult.addWarning("Unable to use %s for termination '%s' as it is not ConductingEquipment or AuxiliaryEquipment.", typeNameAndMRID(identifiedObject), terminationId);
            });
    }

    private void addInvalidTerminationError(String id,
                                            List<String> connectedIds,
                                            List<ConductingEquipment> connectedAssets,
                                            List<AuxiliaryEquipment> auxiliaryEquipment,
                                            PatchResult patchResult) {
        Set<String> missingIds = new HashSet<>(connectedIds);

        missingIds.removeAll(connectedAssets.stream().map(IdentifiedObject::getMRID).collect(Collectors.toList()));
        missingIds.removeAll(auxiliaryEquipment.stream().map(IdentifiedObject::getMRID).collect(Collectors.toList()));

        patchResult.addError("Unable to find required assets for termination '%s'. Failed to find %s.", id, missingIds);
    }

    private PatchTermination connectConductingEquipment(String terminationId,
                                                        List<ConductingEquipment> conductingEquipment,
                                                        PositionPoint lngLat,
                                                        Map<String, PhaseCode> assetPhases,
                                                        PatchResult patchResult) {
        List<Terminal> terminals = findTerminalsToConnect(conductingEquipment, terminationId, lngLat, assetPhases, patchResult);
        String connectivityNodeId = calculateConnectivityNodeId(terminationId, terminals);

        if (connectivityNodeId == null) {
            patchResult.addError("Unable to connect %s to '%s'. The candidate terminals are already connected in an incompatible way.", conductingEquipment.stream().map(ConductingEquipment::getMRID).collect(Collectors.toSet()), terminationId);
            return new PatchTermination(terminationId, terminationId, conductingEquipment, Collections.emptyList());
        }

        terminals.forEach(terminal -> connectTerminal(connectivityNodeId, terminal, lngLat, patchResult));

        applyFeedersAndPhases(terminals, patchResult);
        return new PatchTermination(terminationId, connectivityNodeId, conductingEquipment, Collections.emptyList());
    }

    private List<Terminal> findTerminalsToConnect(List<ConductingEquipment> assets,
                                                  String terminationId,
                                                  PositionPoint lngLat,
                                                  Map<String, PhaseCode> assetPhases,
                                                  PatchResult patchResult) {
        List<Terminal> terminals = new ArrayList<>();
        assets
            .stream()
            .filter(AcLineSegment.class::isInstance)
            .map(asset -> findConductorTerminal(terminationId, (AcLineSegment) asset, lngLat, assetPhases, patchResult))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(terminals::add);

        Set<String> connectivityNodeIds = terminals
            .stream()
            .map(Terminal::connectivityNodeId)
            .collect(Collectors.toSet());

        assets
            .stream()
            .filter(ce -> !(ce instanceof AcLineSegment))
            .map(asset -> findNodeTerminal(terminationId, asset, lngLat, connectivityNodeIds, assetPhases, patchResult))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(terminals::add);

        return terminals;
    }

    private Optional<Terminal> findConductorTerminal(String terminationId,
                                                     Conductor conductor,
                                                     PositionPoint lngLat,
                                                     Map<String, PhaseCode> assetPhases,
                                                     PatchResult patchResult) {
        Location location = conductor.getLocation();
        if ((location == null) || (location.numPoints() <= 1)) {
            patchResult.addWarning("Unable to connect %s with an invalid lon/lat path to termination '%s'.", typeNameAndMRID(conductor), terminationId);
            return Optional.empty();
        }

        Optional<Terminal> terminal = firstOf(
            () -> tryReusingTerminal(conductor, lngLat),
            () -> tryFindTerminalAt(conductor, lngLat),
            () -> tryAddingTerminal(conductor, assetPhases)
        );

        if (!terminal.isPresent())
            patchResult.addWarning("INTERNAL ERROR: You should have been able to add a terminal to %s to connect to termination '%s'.", typeNameAndMRID(conductor), terminationId);

        return terminal;
    }

    private Optional<Terminal> findNodeTerminal(String terminationId,
                                                ConductingEquipment node,
                                                PositionPoint lngLat,
                                                Set<String> connectivityNodeIds,
                                                Map<String, PhaseCode> assetPhases,
                                                PatchResult patchResult) {
        Optional<Terminal> terminal = firstOf(
            () -> tryReusingTerminal(node, lngLat),
            () -> tryFindingWithCommonConnectivityId(node, connectivityNodeIds),
            () -> tryFindingSpareTerminal(node),
            () -> tryAddingTerminal(node, assetPhases),
            () -> tryFindingUnusedTerminal(node, patchResult)
        );

        if (!terminal.isPresent())
            patchResult.addWarning("Unable to connect %s to termination '%s', no viable terminal found.", typeNameAndMRID(node), terminationId);

        return terminal;
    }

    private Optional<Terminal> tryReusingTerminal(ConductingEquipment asset, PositionPoint lngLat) {
        Map<PositionPoint, Terminal> terminations = usedTerminalLocations.get(asset.getMRID());
        if (terminations == null)
            return Optional.empty();

        return Optional.ofNullable(terminations.get(lngLat));
    }

    private Optional<Terminal> tryFindTerminalAt(ConductingEquipment conductingEquipment, PositionPoint lngLat) {
        Location location = conductingEquipment.getLocation();
        if ((location == null) || (location.numPoints() == 0))
            return Optional.empty();
        else if (Objects.equals(location.getPoint(0), lngLat))
            return Optional.ofNullable(conductingEquipment.getTerminal(1));
        else if (Objects.equals(location.getPoint(location.numPoints() - 1), lngLat))
            return Optional.ofNullable(conductingEquipment.getTerminal(conductingEquipment.numTerminals()));
        else
            return Optional.empty();
    }

    private Optional<Terminal> tryAddingTerminal(ConductingEquipment asset, Map<String, PhaseCode> assetPhases) {
        Integer maxTerminals = maxTerminalsByType.get(asset.getClass());

        if (maxTerminals == null)
            throw new IllegalArgumentException(String.format("INTERNAL ERROR: You are attempting manipulate an asset [%s] which has not had its maximum terminals specified.", asset.getClass().getSimpleName()));

        if (asset.numTerminals() < maxTerminals)
            return Optional.of(createTerminal(services, asset, assetPhases.getOrDefault(asset.getMRID(), PhaseCode.ABC)));
        else
            return Optional.empty();
    }

    private Optional<Terminal> tryFindingWithCommonConnectivityId(ConductingEquipment conductingEquipment, Set<String> connectivityNodeIds) {
        return conductingEquipment.getTerminals()
            .stream()
            .filter(t -> connectivityNodeIds.contains(t.connectivityNodeId()))
            .findFirst();
    }

    private Optional<Terminal> tryFindingSpareTerminal(ConductingEquipment node) {
        return node.getTerminals()
            .stream()
            .filter(t -> !t.isConnected())
            .findAny();
    }

    private Optional<Terminal> tryFindingUnusedTerminal(ConductingEquipment node, PatchResult patchResult) {
        Map<PositionPoint, Terminal> terminations = usedTerminalLocations.get(node.getMRID());
        if ((terminations == null) || (terminations.size() >= node.numTerminals()))
            return Optional.empty();

        if (node.numTerminals() != terminations.size() + 1)
            patchResult.addWarning("No support has been added for connecting to a base terminal when there are multiple candidate terminals.");

        return node.getTerminals()
            .stream()
            .filter(t -> !terminations.containsValue(t))
            .findFirst();
    }

    private Optional<Terminal> tryFindFirstTerminal(ConductingEquipment conductingEquipment) {
        return conductingEquipment.getTerminals()
            .stream()
            .findFirst();
    }

    @Nullable
    private String calculateConnectivityNodeId(String id, List<Terminal> terminals) {
        List<String> connectivityNodeIds = terminals
            .stream()
            .map(Terminal::connectivityNodeId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        if (connectivityNodeIds.isEmpty())
            return id;
        else if (connectivityNodeIds.size() == 1)
            return connectivityNodeIds.get(0);
        else
            return null;
    }

    private void connectTerminal(String connectivityNodeId,
                                 Terminal terminal,
                                 PositionPoint lngLat,
                                 PatchResult patchResult) {
        ConductingEquipment conductingEquipment = Objects.requireNonNull(terminal.getConductingEquipment());

        if (services.networkService().connect(terminal, connectivityNodeId))
            usedTerminalLocations.computeIfAbsent(conductingEquipment.getMRID(), k -> new HashMap<>()).put(lngLat, terminal);
        else
            patchResult.addError("Failed to connect '%s' to '%s', already connected to '%s'.", conductingEquipment.getMRID(), connectivityNodeId, terminal.connectivityNodeId());
    }

    private void applyFeedersAndPhases(List<Terminal> terminals, PatchResult patchResult) {
        terminals
            .stream()
            .flatMap(terminal -> NetworkService.connectedTerminals(terminal).stream().map(ConnectivityResult::getTo))
            .filter(Objects::nonNull)
            .distinct()
            .forEach(asset -> applyFeedersAndPhases(asset, patchResult));
    }

    private void applyFeedersAndPhases(ConductingEquipment asset, PatchResult patchResult) {
        patchResult.addAffectedFeedersFromAsset(asset);

        Tracing.setPhases().run(asset, services.networkService().listOf(Breaker.class));
        feederProcessor.applyDownstream(asset);
    }

    private PatchTermination connectAuxiliaryEquipment(String terminationId,
                                                       List<AuxiliaryEquipment> auxiliaryEquipment,
                                                       ConductingEquipment conductingEquipment,
                                                       PositionPoint lngLat,
                                                       PatchResult patchResult) {
        PatchTermination patchTermination = new PatchTermination(terminationId, terminationId, Collections.singletonList(conductingEquipment), auxiliaryEquipment);

        Optional<Terminal> terminal = firstOf(
            () -> tryReusingTerminal(conductingEquipment, lngLat),
            () -> tryFindTerminalAt(conductingEquipment, lngLat),
            () -> tryFindFirstTerminal(conductingEquipment));

        if (!terminal.isPresent()) {
            patchResult.addError("No terminals available to connect AuxiliaryEquipment for termination '%s'.", terminationId);
            return patchTermination;
        }

        auxiliaryEquipment.forEach(aux -> {
            Terminal auxTerminal = aux.getTerminal();
            if (auxTerminal == null)
                aux.setTerminal(terminal.get());
            else {
                patchResult.addWarning("Ignoring request to connect %s to %s as it is already connected to %s.",
                    typeNameAndMRID(aux),
                    typeNameAndMRID(terminal.get()),
                    typeNameAndMRID(auxTerminal));
            }
        });

        return patchTermination;
    }

    private void disconnectConductingEquipment(PatchTermination patchTermination) {
        if (patchTermination.gisId().equals(patchTermination.mRID())) {
            services.networkService().disconnect(patchTermination.mRID());
            return;
        }

        patchTermination.conductingEquipment()
            .stream()
            .flatMap(asset -> asset.getTerminals().stream())
            .filter(terminal -> patchTermination.mRID().equals(terminal.connectivityNodeId()))
            .forEach(services.networkService()::disconnect);
    }

    private void disconnectAuxiliaryEquipment(PatchTermination patchTermination) {
        ConductingEquipment conductingEquipment = patchTermination.conductingEquipment().get(0);

        patchTermination.auxiliaryEquipment()
            .stream()
            .filter(auxiliaryEquipment -> auxiliaryEquipment.getTerminal() != null)
            .filter(auxiliaryEquipment -> auxiliaryEquipment.getTerminal().getConductingEquipment() == conductingEquipment)
            .forEach(auxiliaryEquipment -> auxiliaryEquipment.setTerminal(null));
    }

}
