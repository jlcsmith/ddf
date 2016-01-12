<!--
/*
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
-->

# GeoWebCache 
## Introduction
This module is a a bundled distribution of the GeoWebCache war. GeoWebCache is a server providing a tile cache and tile service aggregation.  See (http://geowebcache.org) for more information.

By installing this application in DDF, all of the GeoWebCache capabilities are available running in the DDF container.  This includes
- WMS/WMTS endpoints (/geowebcache/service)
- Connections to external tile services configured via RESTful services (/geowebcache/rest)
- Caching of tiles
- Seeding of tiles by zoom level

## Building and deploying
GWC in DDF utilizes the GWC 1.5.0 war: http://sourceforge.net/projects/geowebcache/files/geowebcache/1.5.0/geowebcache-1.5.0-war.zip/download
- Download and install GWC war into your maven repo (this will eventually be installed in the codice maven repo)

`mvn install:install-file -Dfile=geowebcache.war -DgroupId=org.geowebcache -DartifactId=geowebcache -Dversion=1.5.0 -Dpackaging=war`

`mvn clean install`

`cp geowebcache-app-target/geowebcache-app-2.9.0-SNAPSHOT.kar <DDF_HOME>/deploy`

## Usage

### Configure GWC
GWC needs to be configured to utilize one or more *backend* tile services which are exposed via a service endpoint that the Search UI can utilize.

`curl -v -k -XPUT -H "Content-type: text/xml" -d @states.xml "https://localhost:8993/geowebcache/rest/layers/states.xml"`

states.xml:
```
    <wmsLayer>
      <name>states</name>
      <mimeFormats>
        <string>image/gif</string>
        <string>image/jpeg</string>
        <string>image/png</string>
        <string>image/png8</string>
      </mimeFormats>
      <wmsUrl>
        <string>http://demo.opengeo.org/geoserver/topp/wms</string>
      </wmsUrl>
    </wmsLayer>
```
### Configure the Standard Search UI
Add a new Imnagery Provider in the Standard Search UI Config in the Admin UI (/admin)

`{"type" "WMS" "url" "https://localhost:8993/geowebcache/service/wms" "layers" ["states"]  "alpha" 0.5}`
