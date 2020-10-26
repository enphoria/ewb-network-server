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
import com.zepben.cimbend.customer.CustomerService;
import com.zepben.cimbend.diagram.DiagramService;
import com.zepben.cimbend.measurement.MeasurementService;
import com.zepben.cimbend.network.NetworkService;

@EverythingIsNonnullByDefault
public class Services {

    private final NetworkService networkService = new NetworkService();
    private final DiagramService diagramService = new DiagramService();
    private final CustomerService customerService = new CustomerService();
    private final MeasurementService measurementService = new MeasurementService();

    public NetworkService networkService() {
        return networkService;
    }

    public DiagramService diagramService() {
        return diagramService;
    }

    public CustomerService customerService() {
        return customerService;
    }

    public MeasurementService measurementService() {
        return measurementService;
    }

}
