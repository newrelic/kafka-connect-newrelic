# New Relic Kafka Connect Sink Connector Release Notes #

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## 1.1.0 (2021-04-14)
### Added
- Adding new Logs Sink Connector for sending Kafka messages to New Relic Logs.
- Added new `nr.timeout` parameter for all connectors. Default is 2 seconds.

### Changed
 - Updating [Telemetry SDK](https://github.com/newrelic/newrelic-telemetry-sdk-java) version from 0.6.0 to 0.12.0
 - Added simple [GitHub action](https://github.com/newrelic/kafka-connect-newrelic/actions) that runs tests on commits and PR's

## 1.0.0 (2020-09-30)
### Added
- Initial version