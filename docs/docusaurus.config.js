/*
 * Copyright 2021 Zeppelin Bend Pty Ltd
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

const zepbenDocusaurusPreset = require("@zepben/docusaurus-preset");

module.exports = {
    title: "Network Server",
    url: "https://zepben.github.io/evolve/docs/network-server",
    baseUrl: "/evolve/docs/network-server/",
    onBrokenLinks: "throw",
    favicon: "img/favicon.ico",
    organizationName: "zepben",
    projectName: "network-server",
    themeConfig: {
        ...zepbenDocusaurusPreset.defaultThemeConfig,
        colorMode: {
            defaultMode: "light",
            disableSwitch: false,
            respectPrefersColorScheme: true,
        },
        navbar: {
            logo: {
                alt: "Zepben",
                src: "img/logo.svg",
                srcDark: "img/logo-dark.svg",
                href: "https://www.zepben.com/",
            },
            items: [
                {
                    to: "https://zepben.github.io/evolve/docs",
                    label: "Evolve",
                    position: "left",
                },
                {
                    to: "api",
                    activeBasePath: "api",
                    label: "API",
                    position: "left",
                },
                {
                    to: "release-notes",
                    activeBasePath: "release-notes",
                    label: "Release Notes",
                    position: "right",
                },
                {
                    type: "docsVersionDropdown",
                    position: "right",
                },
                {
                    href: "https://github.com/zepben/network-server/",
                    position: 'right',
                    className: 'header-github-link',
                    'aria-label': 'GitHub repository',
                },
            ],
        },
        footer: {
            style: "dark",
            links: [],
            copyright: `Copyright © ${new Date().getFullYear()} Zeppelin Bend Pty. Ltd.`,
        },
    },
    presets: [
        [
            "@zepben/docusaurus-preset",
            {
                theme: {
                    customCss: require.resolve("./src/css/custom.css"),
                },
                docs: {
                    routeBasePath: '/',
                    sidebarPath: require.resolve("./sidebars.js"),
                },
            },
        ],
    ],
};
