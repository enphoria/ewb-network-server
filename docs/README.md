# Network Server Documentation Site

This website is built using [Docusaurus 2](https://v2.docusaurus.io/), a modern static website generator.

### Installation

```
$ npm ci
```

### Setting up the docs

The site requires the api spec files from the `ewb-network-routes` to be copied into the `static/spec` dir.  

### Local Development

```
$ npm start
```

This command starts a local development server and open up a browser window. Most changes are reflected live without having to restart the server.

### Build

```
$ npm build
```

This command generates static content into the `build` directory and can be served using any static contents hosting service.

### Updating Documentation

The network related API swagger [specification](static/spec/network) under the documentation project needs to be same as the specs for the version of
the [ewb-network-routes](https://github.com/zepben/ewb-network-routes/tree/main/specs) used. On upgrading the dependency, the network spec need to be reconciled.
