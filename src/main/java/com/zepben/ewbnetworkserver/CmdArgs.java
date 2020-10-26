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

import com.google.common.base.Enums;
import com.zepben.annotations.EverythingIsNonnullByDefault;
import com.zepben.commandlinearguments.CmdArgsBase;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@EverythingIsNonnullByDefault
class CmdArgs extends CmdArgsBase {

    @Nullable private Integer port = null;
    @Nullable private String ewbDataRoot = null;
    @Nullable private LocalDate currentDate = null;
    @Nullable private Integer daysToSearch = null;
    @Nullable private String patchApi = null;
    @Nullable private Integer timeout = null;
    @Nullable private String patchAuthHeader = null;
    @Nullable private String s3Bucket = null;
    @Nullable private String output = null;
    @Nullable private String cors = null;
    @Nullable private String routeDebugFile = null;
    @Nullable private Integer grpcPort = null;
    @Nullable private String grpcCertPath = null;
    @Nullable private String grpcKeyPath = null;
    @Nullable private ClientAuth grpcClientAuth = null;
    @Nullable private String grpcTrustPath = null;

    int port() {
        return ensureOptionInitialised(port);
    }

    public Integer grpcPort() {
        return ensureOptionInitialised(grpcPort);
    }

    public String grpcCertPath() {
        return ensureOptionInitialised(grpcCertPath);
    }

    public String grpcKeyPath() {
        return ensureOptionInitialised(grpcKeyPath);
    }

    public ClientAuth grpcClientAuth() {
        return ensureOptionInitialised(grpcClientAuth);
    }

    public String grpcTrustPath() {
        return ensureOptionInitialised(grpcTrustPath);
    }

    String ewbDataRoot() {
        return ensureOptionInitialised(ewbDataRoot);
    }

    LocalDate currentDate() {
        return ensureOptionInitialised(currentDate);
    }

    int daysToSearch() {
        return ensureOptionInitialised(daysToSearch);
    }

    String patchApi() {
        return ensureOptionInitialised(patchApi);
    }

    int timeout() {
        return ensureOptionInitialised(timeout);
    }

    String patchAuthHeader() {
        return ensureOptionInitialised(patchAuthHeader);
    }

    String s3Bucket() {
        return ensureOptionInitialised(s3Bucket);
    }

    String output() {
        return ensureOptionInitialised(output);
    }

    String cors() {
        return ensureOptionInitialised(cors);
    }

    String routeDebugFile() {
        return ensureOptionInitialised(routeDebugFile);
    }

    @Override
    protected void addCustomOptions(Options options) {
        options.addOption(Option
            .builder("p")
            .longOpt("port")
            .hasArg()
            .argName("PORT")
            .desc("the port number the REST API will listen on.")
            .build());

        options.addOption(Option
            .builder("e")
            .longOpt("ewb-data-root")
            .hasArg()
            .argName("DIRECTORY")
            .desc("the directory of the EWB data root.")
            .build());

        options.addOption(Option
            .builder("c")
            .longOpt("current-date")
            .hasArg()
            .argName("DATE")
            .desc("the date to use as the current date. (DEFAULT: today).")
            .build());

        options.addOption(Option
            .builder("d")
            .longOpt("days-to-search")
            .hasArg()
            .argName("NUM_DAYS")
            .desc("the number of days to search for a network database in the EWB data root. (DEFAULT: 0).")
            .build());

        options.addOption(Option
            .builder("pa")
            .longOpt("patch-api")
            .hasArg()
            .argName("API_ENDPOINT")
            .desc("the patch server end point (including server, port and url). (DEFAULT: none).")
            .build());

        options.addOption(Option
            .builder("t")
            .longOpt("timeout")
            .hasArg()
            .argName("SECONDS")
            .desc("the timeout (in seconds) for receiving data from the patch API. Zero to disable. (DEFAULT: 60).")
            .build());

        options.addOption(Option
            .builder("auth")
            .longOpt("patch-auth-header")
            .hasArg()
            .argName("AUTH_HEADER")
            .desc("the HTTP auth header to use with the patch server. (DEFAULT: none).")
            .build());

        options.addOption(Option
            .builder("s3")
            .longOpt("s3-bucket")
            .hasArg()
            .argName("BUCKET")
            .desc("the AWS S3 bucket name to create the output in. If specified, the default S3 connection will be used. (DEFAULT: none).")
            .build());

        options.addOption(Option
            .builder("o")
            .longOpt("output")
            .hasArg()
            .argName("FILE")
            .desc("the network build status file (including path). (DEFAULT: ewb-network-server-status.json).")
            .build());

        options.addOption(Option
            .builder("cors")
            .longOpt("cors")
            .hasArg()
            .argName("CORS_DEFINITION")
            .desc("set the cors definition.")
            .build());

        options.addOption(Option
            .builder("debug")
            .longOpt("debug-routing")
            .hasArg()
            .argName("FILE")
            .desc("Enable route debugging. Request bodies will be saved to the specified file.. (DEFAULT: none).")
            .build());

        options.addOption(Option
            .builder("gp")
            .longOpt("grpc-port")
            .hasArg()
            .argName("PORT")
            .desc("The port number for the gRPC server.")
            .build());

        options.addOption(Option
            .builder("gt")
            .longOpt("grpc-tls")
            .argName("CERT_PATH> <KEY_PATH")
            .numberOfArgs(2)
            .desc("CERT_PATH specifies the path to the certificate to use, and KEY_PATH specifies the path to the " +
                "private key for the certificate.")
            .build());

        options.addOption(Option
            .builder("ga")
            .longOpt("grpc-auth")
            .argName("CLIENT_AUTH> <TRUST_PATH")
            .numberOfArgs(2)
            .desc("CLIENT_AUTH controls the client authentication requirements {OPTIONAL|REQUIRE]. TRUST_PATH " +
                "specifies the path to the trusted certificate for verifying the remote endpoint's certificate.")
            .build());

    }

    @Override
    protected void extractCustomOptions() throws ParseException {
        port = getRequiredIntArg("port", 1, 65535);
        ewbDataRoot = getRequiredStringArg("ewb-data-root");
        currentDate = getOptionalDateArg("current-date").orElse(LocalDate.now(ZoneId.systemDefault()));
        daysToSearch = getOptionalIntArg("days-to-search", 0).orElse(0);
        patchApi = getOptionalStringArg("patch-api").orElse("");
        timeout = getOptionalIntArg("timeout", 0).orElse(60);
        patchAuthHeader = getOptionalStringArg("patch-auth-header").orElse("");
        s3Bucket = getOptionalStringArg("s3-bucket").orElse("");
        output = getOptionalStringArg("output").orElse("ewb-network-server-status.json");
        cors = getOptionalStringArg("cors").orElse("");
        routeDebugFile = getOptionalStringArg("debug-routing").orElse("");
        grpcPort = getRequiredIntArg("grpc-port", 1, 65535);

        if (Objects.equals(grpcPort, port))
            throw new ParseException("grpc-port cannot be the same number as port.");

        Optional<List<String>> tlsArgs = getOptionalStringArgList("grpc-tls");
        if (tlsArgs.isPresent()) {
            grpcCertPath = tlsArgs.get().get(0);
            grpcKeyPath = tlsArgs.get().get(1);
        } else {
            grpcCertPath = "";
            grpcKeyPath = "";
        }

        Optional<List<String>> authArgs = getOptionalStringArgList("grpc-auth");
        if (authArgs.isPresent()) {
            grpcClientAuth = Enums.getIfPresent(ClientAuth.class, authArgs.get().get(0)).orNull();
            if (grpcClientAuth == null)
                throw new ParseException("Unknown CLIENT_AUTH value '" + authArgs.get().get(0) + "', expected OPTIONAL or REQUIRE.");

            grpcTrustPath = authArgs.get().get(1);
        } else {
            grpcClientAuth = ClientAuth.NONE;
            grpcTrustPath = "";
        }
    }

}
