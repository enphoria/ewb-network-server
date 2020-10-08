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

package com.zepben.ewbnetworkserver.patch.routes;

import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.ewbnetworkserver.patch.LoadManipulations;
import com.zepben.vertxutils.json.filter.FilterSpecification;
import com.zepben.vertxutils.routing.*;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

@EverythingIsNonnullByDefault
public class LoadManipulationsRoute implements Handler<RoutingContext> {

    private final LoadManipulations loadManipulations;
    private final LoadManipulationsToJson loadManipulationsToJson;

    @EverythingIsNonnullByDefault
    public enum AvailableRoute implements VersionableRoute {
        LOAD_MANIPULATIONS(RouteVersion.since(1));

        private final RouteVersion rv;

        AvailableRoute(RouteVersion rv) {
            this.rv = rv;
        }

        @Override
        public RouteVersion routeVersion() {
            return rv;
        }
    }

    static Function<LoadManipulationsRoute.AvailableRoute, Route> routeFactory(LoadManipulations loadManipulations,
                                                                               LoadManipulationsToJson loadManipulationsToJson) {

        LoadManipulationsRoute route = new LoadManipulationsRoute(loadManipulations, loadManipulationsToJson);

        return availableRoute -> {
            if (availableRoute == LoadManipulationsRoute.AvailableRoute.LOAD_MANIPULATIONS) {
                return Route.builder()
                    .method(GET)
                    .path("/load-manipulations")
                    .queryParams(Params.FILTER)
                    .addBlockingHandler(route)
                    .build();
            }
            throw new IllegalArgumentException("INTERNAL ERROR: Missing route factory method.");
        };
    }

    @Override
    public void handle(RoutingContext context) {
        FilterSpecification filterSpecification = RoutingContextEx.getQueryParams(context).get(Params.FILTER);

        Respond.withJson(context, OK, loadManipulationsToJson.convert(loadManipulations), filterSpecification);
    }

    private LoadManipulationsRoute(LoadManipulations loadManipulations, LoadManipulationsToJson loadManipulationsToJson) {
        this.loadManipulations = loadManipulations;
        this.loadManipulationsToJson = loadManipulationsToJson;
    }

}
