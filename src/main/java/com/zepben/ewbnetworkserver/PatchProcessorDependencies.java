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

import com.mashape.unirest.http.Unirest;
import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.ewbnetworkserver.patch.FeederProcessor;
import com.zepben.ewbnetworkserver.patch.LoadManipulations;
import com.zepben.ewbnetworkserver.patch.PatchFeatureCreators;
import com.zepben.ewbnetworkserver.patch.PatchTerminationProcessor;

@EverythingIsNonnullByDefault
class PatchProcessorDependencies implements PatchProcessor.Dependencies {

    private final Services services;
    private final PatchFeatureCreators patchFeatureCreators;
    private final FeederProcessor feederProcessor;
    private final PatchTerminationProcessor patchTerminationProcessor;
    private final HttpClient httpClient = new HttpClient(Unirest::get);
    private final String api;
    private final String authHeader;

    PatchProcessorDependencies(Services services, LoadManipulations loadManipulations, CmdArgs cmdArgs) {
        this.services = services;
        patchFeatureCreators = new PatchFeatureCreators(services, loadManipulations);
        feederProcessor = new FeederProcessor();
        patchTerminationProcessor = new PatchTerminationProcessor(services, feederProcessor);

        api = cmdArgs.patchApi();
        authHeader = cmdArgs.patchAuthHeader();
    }

    @Override
    public Services services() {
        return services;
    }

    @Override
    public PatchFeatureCreators patchFeatureCreators() {
        return patchFeatureCreators;
    }

    @Override
    public FeederProcessor feederProcessor() {
        return feederProcessor;
    }

    @Override
    public PatchTerminationProcessor patchTerminationProcessor() {
        return patchTerminationProcessor;
    }

    @Override
    public HttpClient httpClient() {
        return httpClient;
    }

    @Override
    public String api() {
        return api;
    }

    @Override
    public String authHeader() {
        return authHeader;
    }

}
