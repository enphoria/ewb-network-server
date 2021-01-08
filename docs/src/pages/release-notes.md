#### Release History

| Version | Date |
| --- | :--- |
|[2.0.0](#v200) | `10 November 2020` |
|[1.2.0](#v120) | `8 November 2019` |
|[1.1.0](#v110) | `21 June 2019` |
|1.0.0 | `7 March 2019` |

---

### v2.0.0

##### Breaking Changes
* Fault indicators are no longer included in network traces (including feeder assets).
* Updated database version.
* Updated to use new patch API JSON format and layers.

##### New Features
* Added a new trace to find conducting equipment with attached usage points downstream from the
  specified conducting equipment, with an optional conducting equipment stop condition to support
  "between" tracing. If there is no path between from -> to, a trace in the opposite direction will
  also be performed (i.e. to -> from)
* Support for load profile manipulation has been added to the patch processor. The final manipulation value is `quantity / df`.
* A REST API has been added to retrieve load manipulations at `/ewb/patch/api/v1/load-manipulations`.
  See the [online documentation](https://docs.zepben.com) for details. 

##### Enhancements
* The mapbox vector tiles now include conductor paths if available.

##### Fixes
* None.

##### Notes
* None.

---

### v1.2.0

##### Breaking Changes
* Updated to use v11.0 network database.

##### New Features
* Underground/Overhead conductors are now supported. A flag _**isUnderground**_ has been added to all JSON payloads that
  return conductor details.  
* Conductor styling has been added in the Mapbox Vector Tile feature properties. You can use the _**lineColor**_, _**lineWidth**_
  and _**isUnderground**_ properties of conductor features to control the rendering of map layers.
* Added an isolation trace API that can find all assets within a proposed isolation area.

##### Enhancements
* HV supply point assets now include connected meter information like other supply points.
* A new field _**rating**_ has been added to all JSON payloads that return transformer details.

##### Fixes
* Phases are now being inferred when the core configuration drops phases.

##### Notes
* None.

---

### v1.1.0

##### Breaking Changes
* Updated to use v10.0 network database.

##### New Features
* Patch actions are now processed in order based on type (add, modify then remove).
* Support for system tags (e.g. GIS) has been added to all routes that use asset ID's.
* Filtering has been added to all routes.
* Routes that return terminals now include the terminals lat/lon.
* Added support for cut conductor processing.

##### Enhancements
* Feeder start points can now be the feeder circuit breaker, but it will trace the zone if there is one.
* No longer warning about primary sources on feeders if the primary source is the feeder start point.
* Unknown layers now result in an error rather than aborting.

##### Fixes
* None.

##### Notes
* None.
