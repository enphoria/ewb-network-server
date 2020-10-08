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

package com.zepben.ewbnetworkserver.data;

import com.zepben.annotations.EverythingIsNonnullByDefault;

import static com.zepben.collectionutils.CollectionUtils.arrayOf;

@EverythingIsNonnullByDefault
public class CmdArgsTestData {

    public static String[] validArgs() {
        return arrayOf("-p", "8080",
            "-e", "ewb/root",
            "-c", "2018-12-03",
            "-d", "100",
            "-pa", "https://pathserver:8080/api?toekn=asfasfsaf",
            "-t", "120",
            "-auth", "type auth",
            "-s3", "bucket name",
            "-o", "output.json",
            "-cors", ".*",
            "-debug", "debug/file.ext");
    }

    public static String[] minimumArgs() {
        return arrayOf("-p", "8080",
            "-e", "ewb/root");
    }

}
