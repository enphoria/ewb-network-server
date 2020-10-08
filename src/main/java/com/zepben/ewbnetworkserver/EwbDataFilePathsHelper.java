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
import com.zepben.ewb.filepaths.EwbDataFilePaths;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@EverythingIsNonnullByDefault
class EwbDataFilePathsHelper {

    private final EwbDataFilePaths ewbDataFilePaths;

    EwbDataFilePathsHelper(EwbDataFilePaths ewbDataFilePaths) {
        this.ewbDataFilePaths = ewbDataFilePaths;
    }

    @Nullable
    LocalDate findClosestDateWithDbs(final LocalDate date, int daysToSearch) {
        if (checkNetworkDbsExistForDate(date))
            return date;

        int offset = 1;
        while (offset <= daysToSearch) {
            LocalDate offsetDate = date.minusDays(offset);
            if (checkNetworkDbsExistForDate(offsetDate))
                return offsetDate;

            ++offset;
        }
        return null;
    }

    private boolean checkNetworkDbsExistForDate(LocalDate date) {
        Path networkModelPath = ewbDataFilePaths.networkModel(date);
        Path idCorrelatorPath = ewbDataFilePaths.correlations(date);

        return Files.exists(networkModelPath) && Files.exists(idCorrelatorPath);
    }

}
