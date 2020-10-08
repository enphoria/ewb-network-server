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

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.request.GetRequest;
import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.ewbnetworkserver.patch.FeederProcessor;
import com.zepben.ewbnetworkserver.patch.LoadManipulations;
import com.zepben.ewbnetworkserver.patch.PatchFeatureCreators;
import com.zepben.ewbnetworkserver.patch.PatchTerminationProcessor;
import io.netty.handler.codec.http.HttpResponseStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EverythingIsNonnullByDefault
@SuppressWarnings({"unchecked", "SameParameterValue", "ResultOfMethodCallIgnored"})
class MockPatchProcessorDependencies implements PatchProcessor.Dependencies {

    private final Services services;
    private final PatchFeatureCreators patchFeatureCreators;
    private final FeederProcessor feederProcessor;
    private final PatchTerminationProcessor patchTerminationProcessor;
    private final HttpRequestProvider requestProvider = mock(HttpRequestProvider.class);
    private final HttpResponse<String> response = mock(HttpResponse.class);

    private final HttpClient httpClient = spy(new HttpClient(requestProvider));
    private String api = "api endpoint";
    private String authHeader = "";

    MockPatchProcessorDependencies(Services services, String patch) {
        this.services = services;
        this.patchFeatureCreators = spy(new PatchFeatureCreators(services, mock(LoadManipulations.class)));
        this.feederProcessor = spy(new FeederProcessor());
        this.patchTerminationProcessor = spy(new PatchTerminationProcessor(services, feederProcessor));
        GetRequest request = mock(GetRequest.class);

        try {
            doReturn(request).when(requestProvider).get(any());
            doReturn(response).when(request).asString();
            doReturn(HttpResponseStatus.OK.code()).when(response).getStatus();
            doReturn(patch).when(response).getBody();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    MockPatchProcessorDependencies api(String api) {
        this.api = api;
        return this;
    }

    @Override
    public String authHeader() {
        return authHeader;
    }

    void authHeader(String authHeader) {
        this.authHeader = authHeader;
    }

    HttpResponse<String> response() {
        return response;
    }

}
