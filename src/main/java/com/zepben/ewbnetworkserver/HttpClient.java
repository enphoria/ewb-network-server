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
import com.zepben.vertxutils.json.JsonUtils;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@EverythingIsNonnullByDefault
class HttpClient {

    private final HttpRequestProvider requestProvider;

    HttpClient(HttpRequestProvider requestProvider) {
        this.requestProvider = requestProvider;
    }

    <R> R get(String url,
              Map<String, String> headers,
              ResponseHandler<R> responseHandler,
              BiFunction<HttpResponseStatus, String, R> failureHandler,
              Function<Exception, R> exceptionHandler) {
        try {
            GetRequest request = requestProvider.get(url);
            headers.forEach(request::header);

            HttpResponse<String> response = request.asString();
            if (response.getStatus() == HttpResponseStatus.OK.code())
                return responseHandler.handle(response.getBody());
            else
                return failureHandler.apply(HttpResponseStatus.valueOf(response.getStatus()), response.getBody());
        } catch (Exception e) {
            return exceptionHandler.apply(e);
        }
    }

    @FunctionalInterface
    interface ResponseHandler<R> {

        R handle(String responseBody) throws JsonUtils.ParsingException;

    }

}
