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

import com.zepben.ewb.filepaths.EwbDataFilePaths;
import com.zepben.testutils.junit.SystemLogExtension;
import com.zepben.testutils.mockito.DefaultAnswer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class EwbDataFilePathsHelperTest {

    @RegisterExtension
    public SystemLogExtension systemOutRule = SystemLogExtension.SYSTEM_OUT.captureLog().muteOnSuccess();

    private final EwbDataFilePaths ewbDataFilePaths = mock(EwbDataFilePaths.class, DefaultAnswer.of(Path.class, Paths.get("./something/that/is/invalid")));
    private final EwbDataFilePathsHelper ewbDataFilePathsHelper = new EwbDataFilePathsHelper(ewbDataFilePaths);

    @Test
    public void worksForToday() {
        int numDays = 10;

        configureValidPaths(LocalDate.now(ZoneId.systemDefault()), LocalDate.now(ZoneId.systemDefault()));

        assertThat(ewbDataFilePathsHelper.findClosestDateWithDbs(LocalDate.now(ZoneId.systemDefault()), 0), equalTo(LocalDate.now(ZoneId.systemDefault())));
        assertThat(ewbDataFilePathsHelper.findClosestDateWithDbs(LocalDate.now(ZoneId.systemDefault()), numDays), equalTo(LocalDate.now(ZoneId.systemDefault())));
        assertThat(ewbDataFilePathsHelper.findClosestDateWithDbs(LocalDate.now(ZoneId.systemDefault()).plusDays(numDays), numDays), equalTo(LocalDate.now(ZoneId.systemDefault())));
    }

    @Test
    public void findsClosestDate() {
        configureValidPaths(LocalDate.now(ZoneId.systemDefault()).minusDays(1), LocalDate.now(ZoneId.systemDefault()).minusDays(1));
        configureValidPaths(LocalDate.now(ZoneId.systemDefault()).minusDays(2), LocalDate.now(ZoneId.systemDefault()).minusDays(2));

        assertThat(ewbDataFilePathsHelper.findClosestDateWithDbs(LocalDate.now(ZoneId.systemDefault()), 10), equalTo(LocalDate.now(ZoneId.systemDefault()).minusDays(1)));
    }

    @Test
    public void onlySearchesBackwards() {
        configureValidPaths(LocalDate.now(ZoneId.systemDefault()).plusDays(1), LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        configureValidPaths(LocalDate.now(ZoneId.systemDefault()).minusDays(2), LocalDate.now(ZoneId.systemDefault()).minusDays(2));

        assertThat(ewbDataFilePathsHelper.findClosestDateWithDbs(LocalDate.now(ZoneId.systemDefault()), 10), equalTo(LocalDate.now(ZoneId.systemDefault()).minusDays(2)));
    }

    @Test
    public void hasToFindBothFiles() {
        configureValidPaths(LocalDate.now(ZoneId.systemDefault()).minusDays(1), LocalDate.now(ZoneId.systemDefault()).minusDays(2));
        configureValidPaths(LocalDate.now(ZoneId.systemDefault()).minusDays(3), LocalDate.now(ZoneId.systemDefault()).minusDays(3));

        assertThat(ewbDataFilePathsHelper.findClosestDateWithDbs(LocalDate.now(ZoneId.systemDefault()), 10), equalTo(LocalDate.now(ZoneId.systemDefault()).minusDays(3)));
    }

    @Test
    public void returnsNullIfNotFound() {
        assertThat(ewbDataFilePathsHelper.findClosestDateWithDbs(LocalDate.now(ZoneId.systemDefault()), 10), nullValue());
    }

    private void configureValidPaths(LocalDate validNetworkDate, LocalDate validIdCorrelatorDate) {
        doReturn(Paths.get(".")).when(ewbDataFilePaths).networkModel(validNetworkDate);
        doReturn(Paths.get(".")).when(ewbDataFilePaths).correlations(validIdCorrelatorDate);
    }

}
