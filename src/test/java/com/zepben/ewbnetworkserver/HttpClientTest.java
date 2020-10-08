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
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.testutils.junit.SystemLogExtension;
import com.zepben.vertxutils.json.JsonUtils;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

import static com.zepben.collectionutils.CollectionUtils.mapOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

@EverythingIsNonnullByDefault
@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
public class HttpClientTest {

    @RegisterExtension
    public SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private final HttpResponse<String> response = mock(HttpResponse.class);
    private final GetRequest getRequest = mock(GetRequest.class);
    private final HttpRequestProvider requestProvider = mock(HttpRequestProvider.class);
    private final HttpClient httpClient = new HttpClient(requestProvider);
    private final Callbacks callbacks = spy(new Callbacks());

    private final String url = "my url";
    private final String body = "body text";
    private final HttpResponseStatus failureStatus = HttpResponseStatus.BAD_REQUEST;
    @Nullable private Class<?> expectedExceptionClass;

    @Test
    public void makesGetRequests() throws Exception {
        Map<String, String> headers = mapOf("header1", "value1", "header2", "value2");

        httpClient.get(url, headers, callbacks::responseHandler, callbacks::failureHandler, callbacks::exceptionHandler);

        verify(requestProvider, times(1)).get(url);

        verify(getRequest, times(1)).header("header1", "value1");
        verify(getRequest, times(1)).header("header2", "value2");

        validateCallbacks(1, 0, 0);
    }

    @Test
    public void handlesFailuresInGetRequests() throws Exception {
        doReturn(failureStatus.code()).when(response).getStatus();

        httpClient.get(url, Collections.emptyMap(), callbacks::responseHandler, callbacks::failureHandler, callbacks::exceptionHandler);

        verify(requestProvider, times(1)).get(url);

        validateCallbacks(0, 1, 0);
    }

    @Test
    public void handlesExceptionsInGetRequest() throws Exception {
        expectedExceptionClass = UnirestException.class;
        doThrow(new UnirestException("test exception")).when(requestProvider).get(any());

        httpClient.get(url, Collections.emptyMap(), callbacks::responseHandler, callbacks::failureHandler, callbacks::exceptionHandler);

        verify(requestProvider, times(1)).get(url);

        validateCallbacks(0, 0, 1);
    }

    @Test
    public void handlesExceptionsInProcessingCallback() throws Exception {
        expectedExceptionClass = JsonUtils.ParsingException.class;
//        doReturn(JsonUtils.extractString(new JsonObject(), "missingKey")).when(callbacks).responseHandler(any());
        doThrow(JsonUtils.ParsingException.class).when(callbacks).responseHandler(any());

        httpClient.get(url, Collections.emptyMap(), callbacks::responseHandler, callbacks::failureHandler, callbacks::exceptionHandler);

        verify(requestProvider, times(1)).get(url);

        validateCallbacks(1, 0, 1);
    }

    private void validateCallbacks(int expectedResponseCalls, int expectedFailureCalls, int expectedExceptionCalls) throws Exception {
        verify(callbacks, times(expectedResponseCalls)).responseHandler(any());
        verify(callbacks, times(expectedFailureCalls)).failureHandler(any(), any());
        verify(callbacks, times(expectedExceptionCalls)).exceptionHandler(any());
    }

    public HttpClientTest() throws Exception {
        doReturn(getRequest).when(requestProvider).get(any());
        doReturn(response).when(getRequest).asString();
        doReturn(body).when(response).getBody();
        doReturn(HttpResponseStatus.OK.code()).when(response).getStatus();
    }

    class Callbacks {

        @SuppressWarnings("RedundantThrows")
        Void responseHandler(String responseBody) throws JsonUtils.ParsingException {
            assertThat(responseBody, equalTo(body));
            return null;
        }

        Void failureHandler(HttpResponseStatus status, String responseBody) {
            assertThat(status, equalTo(failureStatus));
            assertThat(responseBody, equalTo(body));
            return null;
        }

        Void exceptionHandler(Exception e) {
            assertThat(e.getClass(), equalTo(expectedExceptionClass));
            return null;
        }

    }

}
