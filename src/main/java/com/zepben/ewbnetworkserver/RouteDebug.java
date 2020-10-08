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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;

import static java.nio.charset.StandardCharsets.UTF_8;

@EverythingIsNonnullByDefault
class RouteDebug {

    private static Logger logger = LoggerFactory.getLogger("ewb-network-server");

    private final boolean isDebugging;
    private final Path routeDebugFile;
    private final FileWriter fileWriter;

    RouteDebug(Dependencies dependencies) {
        isDebugging = !dependencies.routeDebugFile().isEmpty();
        routeDebugFile = Paths.get(dependencies.routeDebugFile());
        fileWriter = dependencies.fileWriter();
    }

    boolean isDebugging() {
        return isDebugging;
    }

    void saveRequestBody(RoutingContext context) {
        Buffer body = context.getBody();
        if (body != null && body.length() > 0) {
            HttpMethod method = context.request().method();
            String uri = context.request().uri();
            String ls = System.lineSeparator();
            String time = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();

            String info = String.format("%s %s %s%s", time, method, uri, ls);
            byte[] writeBytes = Buffer.buffer(info.length() + body.length())
                .appendBytes(info.getBytes(UTF_8))
                .appendBuffer(body)
                .appendBytes(ls.getBytes(UTF_8))
                .appendBytes(ls.getBytes(UTF_8))
                .getBytes();

            try {
                fileWriter.write(routeDebugFile, writeBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                logger.warn("Failed to save route body to debug file.");
            }
        }

        context.next();
    }

    @FunctionalInterface
    interface FileWriter {

        void write(Path path, byte[] bytes, OpenOption... options) throws IOException;

    }

    interface Dependencies {

        String routeDebugFile();

        FileWriter fileWriter();

    }

}
