/*
 * Copyright 2020 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import useBaseUrl from "@docusaurus/useBaseUrl";
import React from "react";
import "swagger-ui-react/swagger-ui.css";
import Showcase from "../../components/showcase";

const Ui = () => (
    <Showcase
        title={"API Documentation"}
        description={""}
        items={[
            {
                title: "Network",
                description: "API spec for network related REST endpoints",
                siteRelUrl: useBaseUrl("api/network"),
                type: "Network"
            },
            {
                title: "Geo View",
                description: "API spec for geoview related REST endpoints",
                siteRelUrl: useBaseUrl("api/geo-view"),
                type: "Network"
            },
            {
                title: "Graphics",
                description: "API spec for graphics related REST endpoints",
                siteRelUrl: useBaseUrl("api/graphics"),
                type: "Network"
            },
            {
                title: "Trace",
                description: "API spec for trace related REST endpoints",
                siteRelUrl: useBaseUrl("api/trace"),
                type: "Network"
            },
            {
                title: "Patch",
                description: "API spec for patch related REST endpoints",
                siteRelUrl: useBaseUrl("api/patch"),
                type: "Other"
            }
        ]}/>
);

export default Ui;
