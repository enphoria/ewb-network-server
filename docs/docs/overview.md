---
id: overview
title: Overview
slug: /
---

The EWB Network Server provides API access for operating on an EWB Network.

This document describes how to configure and run the EWB Network Server

---
## Command Line Options

> Configuration options for the EWB Network Server are provided as command line arguments.

| Usage | Description | Required | Default |
| --- | --- | --- | --- |
| `-auth, --patch-auth-header <AUTH_HEADER>` | the HTTP auth header to use with the patch server | No | None |
| `-c, --current-date <DATE>` | the date to use as the current date | No | Current Date |
| `-cors, --cors <CORS_DEFINITION>` | set the cors definition | No | 0 |
| `-d, --days-to-search <NUM_DAYS>` | the number of days to search for a network database in the EWB data root | No | 0 | 
| `-debug, --debug-routing <FILE>` | enable route debugging. Request bodies will be saved to the specified file | No | None |
| `-e, --ewb-data-root <DIRECTORY>` | the directory of the EWB data root | Yes | |
| `-ga, --grpc-auth <CLIENT_AUTH> <TRUST_PATH>` | `CLIENT_AUTH` controls the client authentication requirements (OPTIONAL/REQUIRE). `TRUST_PATH` specifies the path to the trusted certificate for verifying the remote endpoint's certificate | No | | 
| `-gp, --grpc-port <PORT>` | The port number for the gRPC server | No | |
| `-gt, --grpc-tls <CERT_PATH> <KEY_PATH>` |  `CERT_PATH` specifies the path to the certificate to use, and `KEY_PATH` specifies the path to the private key for the certificate. | No | |
| `-h, --help` | shows the help message | No | |
| `-o, --output <FILE>` | the network build status file (including path) | No | `ewb-network-server-status.json` | 
| `-p, --port <PORT>` | the port number the REST API will listen on. | Yes | |
| `-pa, --patch-api <API_ENDPOINT>` | the patch server end point (including server, port and url) | No | none |
| `-s3, --s3-bucket <BUCKET>` | the AWS S3 bucket name to create the output in. If specified, the default S3 connection will be used | No | none 
| `-t, --timeout <SECONDS>` | the timeout (in seconds) for receiving data from the patch API. Zero to disable | No | 60
