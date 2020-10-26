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
import com.zepben.cimbend.cim.iec61968.assetinfo.CableInfo;
import com.zepben.cimbend.cim.iec61968.assetinfo.OverheadWireInfo;
import com.zepben.cimbend.cim.iec61968.assetinfo.WireInfo;
import com.zepben.cimbend.cim.iec61968.assets.AssetOwner;
import com.zepben.cimbend.cim.iec61968.common.Location;
import com.zepben.cimbend.cim.iec61968.common.Organisation;
import com.zepben.cimbend.cim.iec61968.common.PositionPoint;
import com.zepben.cimbend.cim.iec61968.customers.*;
import com.zepben.cimbend.cim.iec61968.metering.Meter;
import com.zepben.cimbend.cim.iec61968.metering.UsagePoint;
import com.zepben.cimbend.cim.iec61968.operations.OperationalRestriction;
import com.zepben.cimbend.cim.iec61970.base.core.*;
import com.zepben.cimbend.cim.iec61970.base.diagramlayout.Diagram;
import com.zepben.cimbend.cim.iec61970.base.diagramlayout.DiagramObject;
import com.zepben.cimbend.cim.iec61970.base.diagramlayout.DiagramObjectPoint;
import com.zepben.cimbend.cim.iec61970.base.diagramlayout.DiagramObjectStyle;
import com.zepben.cimbend.cim.iec61970.base.wires.*;
import com.zepben.cimbend.customer.CustomerService;
import com.zepben.cimbend.diagram.DiagramService;
import com.zepben.cimbend.network.NetworkService;

import javax.annotation.Nullable;
import java.time.Instant;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@EverythingIsNonnullByDefault
@SuppressWarnings({"UnusedReturnValue", "ConstantConditions", "SameParameterValue"})
public class TestObjectCreators {

    public static PowerTransformer createPowerTransformer(NetworkService networkService,
                                                          String mRID,
                                                          String name,
                                                          int numTerminals,
                                                          PhaseCode phaseCode,
                                                          @Nullable Location location) {
        PowerTransformer tx = new PowerTransformer(mRID);
        tx.setName(name);
        tx.setLocation(location);

        createTerminals(networkService, tx, numTerminals, phaseCode);
        networkService.add(tx);

        return tx;
    }

    public static void createPowerTransformerEnd(NetworkService networkService,
                                                 PowerTransformer tx,
                                                 BaseVoltage baseVoltage,
                                                 int terminal) {
        PowerTransformerEnd end = new PowerTransformerEnd(tx.getMRID() + "-end" + (tx.numEnds() + 1));
        end.setPowerTransformer(tx);
        end.setBaseVoltage(baseVoltage);
        end.setTerminal(tx.getTerminal(terminal));

        tx.addEnd(end);

        networkService.add(end);
    }

    public static void createUsagePoint(NetworkService networkService, PowerTransformer tx, @Nullable AssetOwner assetOwner, int i) {
        UsagePoint usagePoint = new UsagePoint("supply_point_" + i);
        networkService.add(usagePoint);

        tx.addUsagePoint(usagePoint);
        usagePoint.addEquipment(tx);

        for (int j = 1; j <= i; ++j)
            createMeter(networkService, assetOwner, i, usagePoint, j);
    }

    public static void createMeter(NetworkService networkService, @Nullable AssetOwner assetOwner, int i, UsagePoint usagePoint, int j) {
        Meter meter = new Meter("meter_" + i + "_" + j);
        meter.setName("acme_" + i + "_" + j);
        if (assetOwner != null)
            meter.addOrganisationRole(assetOwner);

        networkService.add(meter);

        usagePoint.addEndDevice(meter);
        meter.addUsagePoint(usagePoint);
    }

    public static EnergySource createEnergySource(NetworkService networkService, String mRID) {
        return createEnergySource(networkService, mRID, null);
    }

    public static EnergySource createEnergySource(NetworkService networkService, String mRID, @Nullable Location location) {
        return createEnergySource(networkService, mRID, PhaseCode.A, null);
    }

    public static EnergySource createEnergySource(NetworkService networkService, String mRID, PhaseCode phaseCode, @Nullable Location location) {
        EnergySource energySource = new EnergySource(mRID);
        energySource.setName(mRID);
        energySource.setLocation(location);

        createTerminals(networkService, energySource, 1, phaseCode);
        networkService.add(energySource);

        phaseCode.singlePhases().forEach(phase -> {
            EnergySourcePhase energySourcePhase = new EnergySourcePhase(mRID + "-" + phase.name());
            energySourcePhase.setPhase(phase);
            energySourcePhase.setEnergySource(energySource);

            energySource.addPhase(energySourcePhase);
            networkService.add(energySourcePhase);
        });

        return energySource;
    }

    public static void createTerminals(NetworkService networkService, ConductingEquipment conductingEquipment, int numTerminals) {
        createTerminals(networkService, conductingEquipment, numTerminals, PhaseCode.A);
    }

    public static void createTerminals(NetworkService networkService, ConductingEquipment conductingEquipment, int numTerminals, PhaseCode phaseCode) {
        for (int i = 0; i < numTerminals; ++i) {
            Terminal terminal = new Terminal(conductingEquipment.getMRID() + "-t" + (conductingEquipment.numTerminals() + 1));
            terminal.setPhases(phaseCode);
            terminal.setConductingEquipment(conductingEquipment);

            conductingEquipment.addTerminal(terminal);
            networkService.add(terminal);
        }
    }

    public static Junction createNode(NetworkService networkService, String mRID, int numTerminals) {
        return createNode(networkService, mRID, numTerminals, null);
    }

    public static Junction createNode(NetworkService networkService, String mRID, int numTerminals, @Nullable Location location) {
        Junction node = new Junction(mRID);
        node.setName(mRID);
        node.setLocation(location);

        createTerminals(networkService, node, numTerminals);
        networkService.add(node);

        return node;
    }

    public static Breaker createSwitch(NetworkService networkService, String mRID, boolean openStatus) {
        Breaker node = new Breaker(mRID);
        node.setName(mRID);
        node.setNormallyOpen(openStatus);
        node.setOpen(openStatus);

        createTerminals(networkService, node, 2);
        networkService.add(node);

        return node;
    }

    public static AcLineSegment createAcLineSegment(NetworkService networkService, String mRID, String name, PhaseCode phaseCode, WireInfo wireInfo, @Nullable Location location) {
        AcLineSegment acLineSegment = new AcLineSegment(mRID);
        acLineSegment.setName(name);
        acLineSegment.setAssetInfo(wireInfo);
        acLineSegment.setLocation(location);

        createTerminals(networkService, acLineSegment, 2, phaseCode);
        networkService.add(acLineSegment);

        return acLineSegment;
    }

    public static Junction createJunction(NetworkService networkService,
                                          String mRID,
                                          String name,
                                          int numTerminals,
                                          PhaseCode phaseCode,
                                          @Nullable Location location) {
        Junction junction = new Junction(mRID);
        junction.setName(name);
        junction.setLocation(location);

        createTerminals(networkService, junction, numTerminals, phaseCode);
        networkService.add(junction);

        return junction;
    }

    public static Disconnector createDisconnector(NetworkService networkService, String mRID, String name) {
        Disconnector disconnector = new Disconnector(mRID);
        disconnector.setName(name);

        networkService.add(disconnector);

        return disconnector;
    }

    public static Disconnector createDisconnector(NetworkService networkService,
                                                  String mRID,
                                                  String name,
                                                  int numTerminals,
                                                  PhaseCode phaseCode,
                                                  @Nullable Location location,
                                                  @Nullable Substation substation) {
        Disconnector disconnector = new Disconnector(mRID);
        disconnector.setName(name);
        disconnector.setNormallyOpen(false);
        disconnector.setOpen(false);
        disconnector.setLocation(location);

        if (substation != null) {
            disconnector.addContainer(substation);
            substation.addEquipment(disconnector);
        }

        createTerminals(networkService, disconnector, numTerminals, phaseCode);
        networkService.add(disconnector);

        return disconnector;
    }

    public static PerLengthSequenceImpedance createPerLengthSequenceImpedance(NetworkService networkService, String mRID, double r, double x, double r0, double x0) {
        PerLengthSequenceImpedance perLengthSequenceImpedance = new PerLengthSequenceImpedance(mRID);
        perLengthSequenceImpedance.setR(r);
        perLengthSequenceImpedance.setX(x);
        perLengthSequenceImpedance.setR0(r0);
        perLengthSequenceImpedance.setX0(x0);

        networkService.add(perLengthSequenceImpedance);

        return perLengthSequenceImpedance;
    }

    public static OverheadWireInfo createOverheadWireInfo(NetworkService networkService) {
        return createOverheadWireInfo(networkService, "");
    }

    public static OverheadWireInfo createOverheadWireInfo(NetworkService networkService, String mRID) {
        OverheadWireInfo overheadWireInfo = new OverheadWireInfo(mRID);

        networkService.add(overheadWireInfo);

        return overheadWireInfo;
    }

    public static CableInfo createCableInfo(NetworkService networkService, String mRID) {
        return createCableInfo(networkService, mRID, 0);
    }

    public static CableInfo createCableInfo(NetworkService networkService, String mRID, int ratedCurrent) {
        CableInfo cableInfo = networkService.get(CableInfo.class, mRID);
        if (cableInfo != null)
            return cableInfo;

        cableInfo = new CableInfo(mRID);
        cableInfo.setRatedCurrent(ratedCurrent);

        networkService.add(cableInfo);

        return cableInfo;
    }

    public static OperationalRestriction createOperationalRestriction(NetworkService networkService) {
        OperationalRestriction restriction = new OperationalRestriction("opres1");
        restriction.setName("Operational Restriction 1");
        restriction.setTitle("title");
        restriction.setAuthorName("authorName");
        restriction.setCreatedDateTime(Instant.MIN);
        restriction.setType("type");
        restriction.setStatus("status");
        restriction.setComment("comment");

        networkService.add(restriction);
        return restriction;
    }

    public static Breaker createBreaker(NetworkService networkService,
                                        String mRID,
                                        String name,
                                        int numTerminals,
                                        PhaseCode phaseCode,
                                        @Nullable Location location,
                                        @Nullable Substation substation) {
        return createBreaker(networkService, mRID, name, numTerminals, phaseCode, false, false, location, substation);
    }

    public static Breaker createBreaker(NetworkService networkService,
                                        String mRID,
                                        String name,
                                        int numTerminals,
                                        PhaseCode phaseCode,
                                        boolean isNormallyOpen,
                                        boolean isOpen,
                                        @Nullable Location location,
                                        @Nullable Substation substation) {
        Breaker breaker = new Breaker(mRID);
        breaker.setName(name);
        breaker.setNormallyOpen(isNormallyOpen);
        breaker.setOpen(isOpen);
        breaker.setLocation(location);

        if (substation != null) {
            breaker.addContainer(substation);
            substation.addEquipment(breaker);
        }

        createTerminals(networkService, breaker, numTerminals, phaseCode);
        networkService.add(breaker);

        return breaker;
    }

    public static GeographicalRegion createGeographicalRegion(NetworkService networkService, int i) {
        return createGeographicalRegion(networkService, "b" + i, "BUSINESS" + i);
    }

    public static GeographicalRegion createGeographicalRegion(NetworkService networkService, String mRID, String name) {
        GeographicalRegion geographicalRegion = new GeographicalRegion(mRID);
        geographicalRegion.setName(name);

        networkService.add(geographicalRegion);

        return geographicalRegion;
    }

    public static SubGeographicalRegion createSubGeographicalRegion(NetworkService networkService, GeographicalRegion geographicalRegion, int i) {
        return createSubGeographicalRegion(networkService, geographicalRegion, geographicalRegion.getMRID() + "-r" + i, geographicalRegion.getName() + " REGION" + i);
    }

    public static SubGeographicalRegion createSubGeographicalRegion(NetworkService networkService, GeographicalRegion geographicalRegion, String mRID, String name) {
        SubGeographicalRegion subGeographicalRegion = new SubGeographicalRegion(mRID);
        subGeographicalRegion.setName(name);
        subGeographicalRegion.setGeographicalRegion(geographicalRegion);

        geographicalRegion.addSubGeographicalRegion(subGeographicalRegion);
        networkService.add(subGeographicalRegion);

        return subGeographicalRegion;
    }

    public static Substation createSubstation(NetworkService networkService, SubGeographicalRegion subGeographicalRegion, int i) {
        return createSubstation(networkService, subGeographicalRegion, subGeographicalRegion.getMRID() + "-z" + i, subGeographicalRegion.getName() + " ZONE" + i);
    }

    public static Substation createSubstation(NetworkService networkService, SubGeographicalRegion subGeographicalRegion, String mRID, String name) {
        Substation substation = new Substation(mRID);
        substation.setName(name);
        substation.setSubGeographicalRegion(subGeographicalRegion);

        subGeographicalRegion.addSubstation(substation);
        networkService.add(substation);

        return substation;
    }

    public static Feeder createFeeder(NetworkService networkService, Substation substation, String mRID, String name, ConductingEquipment feederStartPoint) {
        return createFeeder(networkService, substation, mRID, name, feederStartPoint.getTerminal(1));
    }

    public static Feeder createFeeder(NetworkService networkService, @Nullable Substation substation, String mRID, String name, Terminal feederStartPoint) {
        Feeder feeder = new Feeder(mRID);
        feeder.setName(name);
        feeder.setNormalHeadTerminal(feederStartPoint);

        if (substation != null) {
            substation.addFeeder(feeder);
            feeder.setNormalEnergizingSubstation(substation);
        }

        networkService.add(feeder);

        return feeder;
    }

    public static void createFeeder(NetworkService networkService, Substation substation, String mRID, String name, String... equipmentMRIDs) {
        Feeder feeder = createFeeder(networkService, substation, mRID, name, networkService.get(ConductingEquipment.class, equipmentMRIDs[0]));

        for (String equipmentMRID : equipmentMRIDs) {
            ConductingEquipment conductingEquipment = networkService.get(ConductingEquipment.class, equipmentMRID);
            conductingEquipment.addContainer(feeder);

            feeder.addEquipment(conductingEquipment);
        }
    }

    public static Feeder createFeederMock(String id) {
        Feeder feeder = mock(Feeder.class);
        doReturn(id).when(feeder).getMRID();
        return feeder;
    }

    public static void createSite(NetworkService networkService, String mRID, Equipment... equipment) {
        Site site = new Site(mRID);

        for (Equipment equip : equipment) {
            site.addEquipment(equip);
            equip.addContainer(site);
        }

        networkService.add(site);
    }

    public static EnergySource createSource(NetworkService networkService, String mRID, String name) {
        EnergySource energySource = new EnergySource(mRID);
        energySource.setName(name);
        energySource.setBaseVoltage(baseVoltageOf(networkService, 22000));
        energySource.setLocation(locationOf(networkService, mRID + "-loc", 1, 1));

        EnergySourcePhase energySourcePhase = new EnergySourcePhase(mRID + "-A");
        energySourcePhase.setPhase(SinglePhaseKind.A);
        energySourcePhase.setEnergySource(energySource);

        energySource.addPhase(energySourcePhase);

        createTerminals(networkService, energySource, 2);
        networkService.add(energySource);

        return energySource;
    }

    public static Fuse createFuse(NetworkService networkService,
                                  String mRID,
                                  String name,
                                  int numTerminals,
                                  PhaseCode phaseCode,
                                  @Nullable Location location) {
        Fuse fuse = new Fuse(mRID);
        fuse.setName(name);
        fuse.setLocation(location);

        createTerminals(networkService, fuse, numTerminals, phaseCode);
        networkService.add(fuse);

        return fuse;
    }

    public static void connect(NetworkService networkService, ConductingEquipment conductingEquipment1, ConductingEquipment conductingEquipment2) {
        networkService.connect(conductingEquipment1.getTerminal(2), conductingEquipment2.getTerminal(1));
    }

    public static void connect(NetworkService networkService, Terminal terminal1, Terminal terminal2, String connectivityNodeMRID) {
        networkService.connect(terminal1, connectivityNodeMRID);
        networkService.connect(terminal2, connectivityNodeMRID);
    }

    public static Diagram createDiagram(DiagramService diagramService, String mRID) {
        Diagram diagram = new Diagram(mRID);
        diagramService.add(diagram);
        return diagram;
    }

    public static DiagramObject createDiagramObject(DiagramService diagramService, IdentifiedObject identifiedObject, Diagram diagram, DiagramObjectStyle diagramObjectStyle, double x, double y) {
        return createDiagramObject(diagramService, identifiedObject, diagram, diagramObjectStyle, x, y, 0);
    }

    public static DiagramObject createDiagramObject(DiagramService diagramService, IdentifiedObject identifiedObject, Diagram diagram, DiagramObjectStyle diagramObjectStyle, double x, double y, double rotation) {
        DiagramObject diagramObject = new DiagramObject();
        diagramObject.setIdentifiedObjectMRID(identifiedObject.getMRID());
        diagramObject.setDiagram(diagram);
        diagramObject.addPoint(new DiagramObjectPoint(x, y));
        diagramObject.setStyle(diagramObjectStyle);
        diagramObject.setRotation(rotation);

        identifiedObject.setNumDiagramObjects(identifiedObject.getNumDiagramObjects() + 1);
        diagramService.add(diagramObject);

        return diagramObject;
    }

    public static DiagramObject createDiagramObject(DiagramService diagramService, IdentifiedObject identifiedObject, Diagram diagram, DiagramObjectStyle diagramObjectStyle, double x1, double y1, double x2, double y2) {
        DiagramObject diagramObject = new DiagramObject();
        diagramObject.setIdentifiedObjectMRID(identifiedObject.getMRID());
        diagramObject.setDiagram(diagram);
        diagramObject.addPoint(new DiagramObjectPoint(x1, y1));
        diagramObject.addPoint(new DiagramObjectPoint(x2, y2));
        diagramObject.setStyle(diagramObjectStyle);

        identifiedObject.setNumDiagramObjects(identifiedObject.getNumDiagramObjects() + 1);
        diagramService.add(diagramObject);

        return diagramObject;
    }

    public static Tariff createTariff(CustomerService customerService, String name) {
        Tariff tariff = new Tariff();
        tariff.setName(name);

        customerService.add(tariff);

        return tariff;
    }

    public static PricingStructure createPricingStructure(CustomerService customerService, Tariff tariff) {
        PricingStructure pricingStructure = new PricingStructure();
        pricingStructure.addTariff(tariff);

        customerService.add(pricingStructure);

        return pricingStructure;
    }

    public static CustomerAgreement createCustomerAgreement(CustomerService customerService, Customer customer, PricingStructure pricingStructure) {
        CustomerAgreement customerAgreement = new CustomerAgreement();
        customerAgreement.setCustomer(customer);
        customerAgreement.addPricingStructure(pricingStructure);

        customer.addAgreement(customerAgreement);
        customerService.add(customerAgreement);

        return customerAgreement;
    }

    public static Customer createCustomer(CustomerService customerService, CustomerKind kind, String tariffName) {
        Customer customer = new Customer();
        customer.setKind(kind);

        Tariff tariff = createTariff(customerService, tariffName);
        PricingStructure pricingStructure = createPricingStructure(customerService, tariff);
        createCustomerAgreement(customerService, customer, pricingStructure);

        customerService.add(customer);

        return customer;
    }

    public static AssetOwner createAssetOwner(NetworkService networkService, Organisation organisation) {
        AssetOwner assetOwner = new AssetOwner();
        assetOwner.setOrganisation(organisation);

        networkService.add(assetOwner);

        return assetOwner;
    }

    public static Organisation createOrganisation(NetworkService networkService, CustomerService customerService) {
        Organisation organisation = new Organisation();
        organisation.setName(organisation.getMRID() + "-name");

        networkService.add(organisation);
        customerService.add(organisation);

        return organisation;
    }

    public static Meter createMeter(NetworkService networkService, String mRID, String name, AssetOwner assetOwner, Location location, @Nullable String customerMRID) {
        Meter meter = new Meter(mRID);
        meter.setName(name);
        meter.setServiceLocation(location);
        meter.setCustomerMRID(customerMRID);
        meter.addOrganisationRole(assetOwner);

        networkService.add(meter);

        return meter;
    }

    public static Location locationOf(NetworkService networkService, String mRID, double... coords) {
        Location location = new Location(mRID);
        for (int i = 0; i < coords.length; i += 2)
            location.addPoint(new PositionPoint(coords[i], coords[i + 1]));

        networkService.add(location);
        return location;
    }

    public static Location locationOf(NetworkService networkService, String mRID, PositionPoint... positionPoints) {
        Location location = new Location(mRID);
        for (PositionPoint positionPoint : positionPoints)
            location.addPoint(positionPoint);

        networkService.add(location);
        return location;
    }

    public static BaseVoltage baseVoltageOf(NetworkService networkService, int voltage) {
        String mRID = "bv" + voltage;
        BaseVoltage baseVoltage = networkService.get(BaseVoltage.class, mRID);

        if (baseVoltage == null) {
            baseVoltage = new BaseVoltage("bv" + voltage);
            baseVoltage.setNominalVoltage(voltage);

            networkService.add(baseVoltage);
        }

        return baseVoltage;
    }

}
