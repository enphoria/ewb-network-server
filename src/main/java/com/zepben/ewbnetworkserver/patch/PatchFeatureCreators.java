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
import com.zepben.cimbend.cim.iec61968.common.PositionPoint;
import com.zepben.cimbend.cim.iec61970.base.auxiliaryequipment.AuxiliaryEquipment;
import com.zepben.cimbend.cim.iec61970.base.auxiliaryequipment.FaultIndicator;
import com.zepben.cimbend.cim.iec61970.base.core.*;
import com.zepben.cimbend.cim.iec61970.base.wires.*;
import com.zepben.ewbnetworkserver.Services;
import com.zepben.ewbnetworkserver.geojson.GeoJson;
import com.zepben.vertxutils.json.JsonUtils;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nullable;
import java.util.*;

import static com.zepben.cimbend.common.extensions.IdentifiedObjectExtensionsKt.typeNameAndMRID;
import static com.zepben.ewbnetworkserver.ObjectCreators.createLocation;
import static com.zepben.ewbnetworkserver.ObjectCreators.createTerminals;
import static com.zepben.ewbnetworkserver.PatchProcessor.unknownPerLengthSequenceImpedance;
import static com.zepben.ewbnetworkserver.PatchProcessor.unknownWireInfo;
import static com.zepben.ewbnetworkserver.patch.PatchProperties.*;
import static com.zepben.ewbnetworkserver.patch.TypeConverters.isNormallyOpenFromNormalState;
import static com.zepben.ewbnetworkserver.patch.TypeConverters.parseVoltage;
import static com.zepben.vertxutils.json.JsonUtils.*;

@EverythingIsNonnullByDefault
public class PatchFeatureCreators {

    private final Services services;
    private final LoadManipulations loadManipulations;

    public PatchFeatureCreators(Services services, LoadManipulations loadManipulations) {
        this.services = services;
        this.loadManipulations = loadManipulations;
    }

    public void addRecloser(GeoJson geoJson, Map<String, PhaseCode> assetPhases, PatchResult patchResult) throws JsonUtils.ParsingException {
        Recloser recloser = populate(new Recloser(geoJson.gisId()), geoJson, OPERATING_VOLTAGE, assetPhases, patchResult, true);
        tryCopyLinks(recloser, Switch.class, geoJson, patchResult, this::copyLinks);
    }

    public void addCapacitor(GeoJson geoJson, Map<String, PhaseCode> assetPhases, PatchResult patchResult) throws JsonUtils.ParsingException {
        LinearShuntCompensator capacitor = populate(new LinearShuntCompensator(geoJson.gisId()), geoJson, OPERATING_VOLTAGE, assetPhases, patchResult, false);
        tryCopyLinks(capacitor, ConductingEquipment.class, geoJson, patchResult, this::copyLinks);
    }

    public void addFaultIndicator(GeoJson geoJson, PatchResult patchResult) throws JsonUtils.ParsingException {
        FaultIndicator faultIndicator = populate(new FaultIndicator(geoJson.gisId()), geoJson, patchResult);
        tryCopyLinks(faultIndicator, FaultIndicator.class, geoJson, patchResult, this::copyLinks);
    }

    public void addLine(GeoJson geoJson, Map<String, PhaseCode> assetPhases, PatchResult patchResult) throws JsonUtils.ParsingException {
        List<PositionPoint> lngLats = geoJson.geometry().coordinates();

        AcLineSegment acLineSegment = new AcLineSegment(geoJson.gisId());
        acLineSegment.setName(geoJson.gisId());
        acLineSegment.setLocation(createLocation(services, geoJson.gisId() + "-loc", lngLats));
        acLineSegment.setPerLengthSequenceImpedance(unknownPerLengthSequenceImpedance);
        acLineSegment.setAssetInfo(unknownWireInfo);
        acLineSegment.setLength(0);
        acLineSegment.setBaseVoltage(baseVoltageOf(parseVoltage(geoJson, OPERATING_VOLTAGE, "22000", onInvalidValue(patchResult), logOnDefaultValue(patchResult))));

        PhaseCode phaseCode = phaseCodeForVoltage(acLineSegment);
        assetPhases.put(geoJson.gisId(), phaseCode);

        createTerminals(services, acLineSegment, phaseCode, 2);

        services.networkService().add(acLineSegment);

        tryCopyLinks(acLineSegment, AcLineSegment.class, geoJson, patchResult, this::copyLinks);
    }

    public void addBreaker(GeoJson geoJson, Map<String, PhaseCode> assetPhases, PatchResult patchResult) throws JsonUtils.ParsingException {
        Breaker breaker = populate(new Breaker(geoJson.gisId()), geoJson, OPERATING_VOLTAGE, assetPhases, patchResult, true);
        tryCopyLinks(breaker, Switch.class, geoJson, patchResult, this::copyLinks);
    }

    public void addDisconnector(GeoJson geoJson, Map<String, PhaseCode> assetPhases, PatchResult patchResult) throws JsonUtils.ParsingException {
        Disconnector disconnector = populate(new Disconnector(geoJson.gisId()), geoJson, OPERATING_VOLTAGE, assetPhases, patchResult, true);
        tryCopyLinks(disconnector, Switch.class, geoJson, patchResult, this::copyLinks);
    }

    public void addRegulator(GeoJson geoJson, Map<String, PhaseCode> assetPhases, PatchResult patchResult) throws JsonUtils.ParsingException {
        PowerTransformer regulator = populate(new PowerTransformer(geoJson.gisId()), geoJson, OPERATING_VOLTAGE, assetPhases, patchResult, false);
        createEnds(regulator, parseVoltage(geoJson, OPERATING_VOLTAGE, "22000", onInvalidValue(patchResult), logOnDefaultValue(patchResult)));
        tryCopyLinks(regulator, PowerTransformer.class, geoJson, patchResult, this::copyLinks);
    }

    public void addSupplyPoint(GeoJson geoJson, Map<String, PhaseCode> assetPhases, PatchResult patchResult) throws JsonUtils.ParsingException {
        EnergyConsumer energyConsumer = populate(new EnergyConsumer(geoJson.gisId()), geoJson, OPERATING_VOLTAGE, assetPhases, patchResult, false);
        processLoad(geoJson, patchResult);
        tryCopyLinks(energyConsumer, EnergyConsumer.class, geoJson, patchResult, this::copyLinks);
    }

    public void addTransformer(GeoJson geoJson, Map<String, PhaseCode> assetPhases, PatchResult patchResult) throws JsonUtils.ParsingException {
        PowerTransformer distTransformer = populate(new PowerTransformer(geoJson.gisId()), geoJson, PRIMARY_VOLTAGE, assetPhases, patchResult, false);
        createEnds(distTransformer, parseVoltage(geoJson, SECONDARY_VOLTAGE, "lv", onInvalidValue(patchResult), logOnDefaultValue(patchResult)));
        processLoad(geoJson, patchResult);
        tryCopyLinks(distTransformer, PowerTransformer.class, geoJson, patchResult, this::copyLinks);
    }

    public void addFuse(GeoJson geoJson, Map<String, PhaseCode> assetPhases, PatchResult patchResult) throws JsonUtils.ParsingException {
        Fuse fuse = populate(new Fuse(geoJson.gisId()), geoJson, PRIMARY_VOLTAGE, assetPhases, patchResult, true);
        tryCopyLinks(fuse, Switch.class, geoJson, patchResult, this::copyLinks);
    }

    private <T extends AuxiliaryEquipment> T populate(T it, GeoJson geoJson, PatchResult patchResult) throws JsonUtils.ParsingException {
        it.setName(geoJson.getStringProperty(DESCRIPTION, it.getMRID(), logOnDefaultValue(patchResult)));
        it.setLocation(createLocation(services, it.getMRID() + "-loc", Collections.singletonList(geoJson.geometry().coordinate())));

        services.networkService().tryAdd(it);

        return it;
    }

    private <T extends ConductingEquipment> T populate(T it, GeoJson geoJson, String voltageKey, Map<String, PhaseCode> assetPhases, PatchResult patchResult, boolean logDefaultNormalState) throws JsonUtils.ParsingException {
        boolean isNormallyOpen = isNormallyOpenFromNormalState(geoJson, onInvalidValue(patchResult), logDefaultNormalState ? logOnDefaultValue(patchResult) : (geo, property, defaultValue) -> {
        });

        it.setName(geoJson.getStringProperty(DESCRIPTION, it.getMRID(), logOnDefaultValue(patchResult)));
        it.setLocation(createLocation(services, it.getMRID() + "-loc", Collections.singletonList(geoJson.geometry().coordinate())));
        it.setBaseVoltage(baseVoltageOf(parseVoltage(geoJson, voltageKey, "22000", onInvalidValue(patchResult), logOnDefaultValue(patchResult))));

        if (it instanceof Switch) {
            Switch s = (Switch) it;
            s.setNormallyOpen(isNormallyOpen);
            s.setOpen(isNormallyOpen);
        } else {
            it.setNormallyInService(!isNormallyOpen);
            it.setInService(!isNormallyOpen);
        }

        PhaseCode phaseCode = phaseCodeForVoltage(it);
        assetPhases.put(it.getMRID(), phaseCode);

        createTerminals(services, it, phaseCode, 2);
        services.networkService().tryAdd(it);

        return it;
    }

    private PhaseCode phaseCodeForVoltage(ConductingEquipment conductingEquipment) {
        switch (conductingEquipment.getBaseVoltageValue()) {
            case 12700:
                return PhaseCode.X;
            case 415:
                return PhaseCode.ABCN;
            default:
                return PhaseCode.ABC;
        }
    }

    private void createEnds(PowerTransformer powerTransformer, int secondaryVoltage) {
        createEnd(powerTransformer, powerTransformer.getBaseVoltage());
        createEnd(powerTransformer, baseVoltageOf(secondaryVoltage));

        powerTransformer.setBaseVoltage(null);
    }

    private void createEnd(PowerTransformer powerTransformer, @Nullable BaseVoltage voltage) {
        int endNumber = powerTransformer.numEnds() + 1;

        PowerTransformerEnd end = new PowerTransformerEnd(powerTransformer.getMRID() + "-end" + endNumber);
        end.setPowerTransformer(powerTransformer);
        end.setBaseVoltage(voltage);
        end.setTerminal(powerTransformer.getTerminal(endNumber));

        powerTransformer.addEnd(end);
        services.networkService().add(end);
    }

    private BaseVoltage baseVoltageOf(int nominalVoltage) {
        String mRID = "bv" + nominalVoltage;
        BaseVoltage baseVoltage = services.networkService().get(BaseVoltage.class, mRID);
        if (baseVoltage != null)
            return baseVoltage;

        baseVoltage = new BaseVoltage(mRID);
        baseVoltage.setNominalVoltage(nominalVoltage);

        services.networkService().add(baseVoltage);
        return baseVoltage;
    }

    private void processLoad(GeoJson geoJson, PatchResult patchResult) {
        try {
            extractOptionalObjectList(geoJson.properties(), LOAD_DATA)
                .ifPresent(loadData -> loadData
                    .forEach(jsonObject -> processLoad(geoJson.gisId(), jsonObject, patchResult)));
        } catch (JsonUtils.ParsingException e) {
            patchResult.addWarning("Ignoring invalid '%s' for feature '%s': %s.", LOAD_DATA, geoJson.gisId(), e.getMessage());
        }
    }

    private void processLoad(String mRID, JsonObject jsonObject, PatchResult patchResult) {
        try {
            LoadOperation loadOperation = LoadOperation.valueOf(extractRequiredString(jsonObject, LOAD_OPERATION));
            LoadType loadType = LoadType.valueOf(extractRequiredString(jsonObject, LOAD_TYPE));
            double loadQuantity = parseLoadQuantity(extractRequiredString(jsonObject, LOAD_QUANTITY));
            Optional<Double> loadDiversificationFactor = extractOptionalDouble(jsonObject, LOAD_DIVERSIFICATION_FACTOR);

            if (!loadDiversificationFactor.isPresent())
                patchResult.addWarning("No '%s' value found for manipulation of '%s', defaulting to 1.", LOAD_DIVERSIFICATION_FACTOR, mRID);

            loadManipulations.add(mRID, loadOperation, loadType, loadQuantity, loadDiversificationFactor.orElse(1.0));
        } catch (JsonUtils.ParsingException | IllegalArgumentException e) {
            patchResult.addWarning("Ignoring invalid load manipulation for feature '%s': %s.", mRID, e.getMessage());
        }
    }

    private double parseLoadQuantity(String loadQuantityString) throws NumberFormatException {
        int index = loadQuantityString.lastIndexOf("=");
        if (index >= 0)
            loadQuantityString = loadQuantityString.substring(index + 1).trim();

        if (loadQuantityString.toLowerCase(Locale.getDefault()).endsWith("kva"))
            loadQuantityString = loadQuantityString.substring(0, loadQuantityString.length() - 3).trim();

        return Double.parseDouble(loadQuantityString);
    }

    private GeoJson.DefaultValueHandler logOnDefaultValue(PatchResult patchResult) {
        return (geoJson, property, defaultValue) ->
            patchResult.addWarning("Feature '%s': Property '%s' not found, defaulting to '%s'.", geoJson.gisId(), property, defaultValue);
    }

    private TypeConverters.InvalidValueHandler onInvalidValue(PatchResult patchResult) {
        return (geoJson, property, unknownValue, defaultValue) ->
            patchResult.addWarning("Feature '%s': Ignoring invalid value '%s' for property '%s', defaulting to '%s'.", geoJson.gisId(), unknownValue, property, defaultValue);
    }

    private Terminal firstTerminal(ConductingEquipment conductingEquipment) {
        return conductingEquipment.getTerminals()
            .stream()
            .findFirst()
            .orElseThrow(IllegalStateException::new);
    }

    private <T extends IdentifiedObject> void tryCopyLinks(T replacement, Class<T> clazz, GeoJson geoJson, PatchResult patchResult, CopyLinks<T> copyLinks) {
        try {
            Optional<String> mRID = extractOptionalString(geoJson.properties(), REPLACE_ASSET_GIS);
            if (!mRID.isPresent())
                return;

            IdentifiedObject identifiedObject = services.networkService().get(IdentifiedObject.class, mRID.get());
            if (clazz.isInstance(identifiedObject))
                copyLinks.process(replacement, clazz.cast(identifiedObject));
            else if (identifiedObject != null)
                patchResult.addWarning("Feature '%s': Unable to copy connectivity from incompatible original %s.", geoJson.gisId(), typeNameAndMRID(identifiedObject));
            else
                patchResult.addWarning("Feature '%s': Unable to copy connectivity from original asset, it was not found in the network.", geoJson.gisId());
        } catch (ParsingException e) {
            patchResult.addWarning("Feature '%s': Unable to copy connectivity from original asset. %s", geoJson.gisId(), e.getMessage());
        }
    }

    private void copyLinks(ConductingEquipment replacement, ConductingEquipment original) {
        copyLinks(replacement, (Equipment) original);

        if (original.numTerminals() == 0)
            return;

        if (original.numTerminals() > replacement.numTerminals()) {
            PhaseCode phaseCode = replacement.numTerminals() > 0 ? firstTerminal(replacement).getPhases() : firstTerminal(original).getPhases();
            createTerminals(services, replacement, phaseCode, original.numTerminals() - replacement.numTerminals());
        }

        List<Terminal> originalTerminals = original.getTerminals();
        List<Terminal> replacementTerminals = replacement.getTerminals();
        for (int i = 0; i < originalTerminals.size(); ++i) {
            Terminal originalTerminal = originalTerminals.get(i);
            if (originalTerminal.isConnected())
                services.networkService().connect(originalTerminal, replacementTerminals.get(i));
        }
    }

    private void copyLinks(AuxiliaryEquipment replacement, AuxiliaryEquipment original) {
        copyLinks(replacement, (Equipment) original);

        replacement.setTerminal(original.getTerminal());
    }

    private void copyLinks(Equipment replacement, Equipment original) {
        original.getContainers().forEach(container -> {
            replacement.addContainer(container);
            container.addEquipment(replacement);
        });

        original.getUsagePoints().forEach(usagePoint -> {
            replacement.addUsagePoint(usagePoint);
            usagePoint.addEquipment(replacement);
        });

        original.getOperationalRestrictions().forEach(operationalRestriction -> {
            replacement.addOperationalRestriction(operationalRestriction);
            operationalRestriction.addEquipment(replacement);
        });

        original.getCurrentFeeders().forEach(feeder -> {
            replacement.addCurrentFeeder(feeder);
            feeder.addEquipment(replacement);
        });
    }

    @FunctionalInterface
    private interface CopyLinks<T extends IdentifiedObject> {

        void process(T replacement, T original);

    }

}
