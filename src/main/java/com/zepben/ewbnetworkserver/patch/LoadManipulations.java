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

package com.zepben.ewbnetworkserver.patch;

import com.google.common.util.concurrent.AtomicDouble;
import com.zepben.annotations.EverythingIsNonnullByDefault;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@EverythingIsNonnullByDefault
public class LoadManipulations {

    Map<String, AtomicDouble> byMRID = new HashMap<>();

    public void add(String mRID, LoadOperation loadOperation, LoadType loadType, double quantity, double diversificationFactor) {
        byMRID
            .computeIfAbsent(mRID, k -> new AtomicDouble())
            .addAndGet((loadOperation == LoadOperation.Added ? 1 : -1)
                * (loadType == LoadType.Load ? 1 : -1)
                * quantity
                / diversificationFactor);
    }

    public void remove(String mRID) {
        byMRID.remove(mRID);
    }

    public Map<String, Double> byMRID() {
        return byMRID.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

}
