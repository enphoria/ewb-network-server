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

import com.zepben.testutils.exception.ExpectException;
import com.zepben.testutils.junit.SystemLogExtension;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;
import java.time.ZoneId;

import static com.zepben.collectionutils.CollectionUtils.arrayOf;
import static com.zepben.ewbnetworkserver.data.CmdArgsTestData.minimumArgs;
import static com.zepben.ewbnetworkserver.data.CmdArgsTestData.validArgs;
import static com.zepben.testutils.exception.ExpectException.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CmdArgsTest {

    @RegisterExtension
    public SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private final CmdArgs cmdArgs = new CmdArgs();

    @Test
    public void ewbDataRoot() throws Exception {
        cmdArgs.parse(validArgs());

        assertThat(cmdArgs.port(), equalTo(8080));
        assertThat(cmdArgs.ewbDataRoot(), equalTo("ewb/root"));
        assertThat(cmdArgs.currentDate(), equalTo(LocalDate.of(2018, 12, 3)));
        assertThat(cmdArgs.daysToSearch(), equalTo(100));
        assertThat(cmdArgs.patchApi(), equalTo("https://pathserver:8080/api?toekn=asfasfsaf"));
        assertThat(cmdArgs.timeout(), equalTo(120));
        assertThat(cmdArgs.patchAuthHeader(), equalTo("type auth"));
        assertThat(cmdArgs.s3Bucket(), equalTo("bucket name"));
        assertThat(cmdArgs.output(), equalTo("output.json"));
        assertThat(cmdArgs.cors(), equalTo(".*"));
        assertThat(cmdArgs.routeDebugFile(), equalTo("debug/file.ext"));
    }

    @Test
    public void defaultOptions() throws Exception {
        cmdArgs.parse(minimumArgs());

        assertThat(cmdArgs.currentDate(), equalTo(LocalDate.now(ZoneId.systemDefault())));
        assertThat(cmdArgs.daysToSearch(), equalTo(0));
        assertThat(cmdArgs.patchApi(), equalTo(""));
        assertThat(cmdArgs.timeout(), equalTo(60));
        assertThat(cmdArgs.patchAuthHeader(), equalTo(""));
        assertThat(cmdArgs.s3Bucket(), equalTo(""));
        assertThat(cmdArgs.output(), equalTo("ewb-network-server-status.json"));
        assertThat(cmdArgs.cors(), equalTo(""));
        assertThat(cmdArgs.routeDebugFile(), equalTo(""));
    }

    @Test
    public void validatesOptions() {
        expect(() -> cmdArgs.parse(arrayOf()))
            .toThrow(ParseException.class)
            .withMessage("Missing required option: port.");

        expect(() -> cmdArgs.parse(arrayOf("-p", "80")))
            .toThrow(ParseException.class)
            .withMessage("Missing required option: ewb-data-root.");

        validateOption("-p", "0", "Integer 0 for argument port is out of range. Expected value in range 1..65535.");
        validateOption("-p", "65536", "Integer 65536 for argument port is out of range. Expected value in range 1..65535.");
        validateOption("-p", "abc", "Invalid integer 'abc' for argument port.");
        validateOption("-c", "abc", "Invalid date 'abc' for argument current-date.");
        validateOption("-d", "abc", "Invalid integer 'abc' for argument days-to-search.");
    }

    @Test
    public void mustParseBeforeUse() {
        validateIllegalOptionUsage(cmdArgs::port);
        validateIllegalOptionUsage(cmdArgs::ewbDataRoot);
        validateIllegalOptionUsage(cmdArgs::currentDate);
        validateIllegalOptionUsage(cmdArgs::daysToSearch);
        validateIllegalOptionUsage(cmdArgs::patchApi);
        validateIllegalOptionUsage(cmdArgs::patchAuthHeader);
        validateIllegalOptionUsage(cmdArgs::s3Bucket);
        validateIllegalOptionUsage(cmdArgs::output);
        validateIllegalOptionUsage(cmdArgs::cors);
        validateIllegalOptionUsage(cmdArgs::routeDebugFile);
    }

    private void validateOption(String option, String value, String expectedMessage) {
        expect(() -> cmdArgs.parse(arrayOf(option, value, "-p", "80", "-e", "path")))
            .toThrow(ParseException.class)
            .withMessage(expectedMessage);
    }

    private void validateIllegalOptionUsage(ExpectException.RunWithException runnable) {
        expect(runnable)
            .toThrow(IllegalStateException.class)
            .withMessage("INTERNAL ERROR: You called an option getter before you parsed the options or when help was requested.");
    }

}
