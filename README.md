<img src="https://wcm.io/images/favicon-16@2x.png"/> wcm.io Testing
======
[![Build Status](https://travis-ci.org/wcm-io/wcm-io-testing.png?branch=develop)](https://travis-ci.org/wcm-io/wcm-io-testing)
[![Code Coverage](https://codecov.io/gh/wcm-io/wcm-io-testing/branch/develop/graph/badge.svg)](https://codecov.io/gh/wcm-io/wcm-io-testing)

Helper tools for supporting Unit Tests, Integration test and test automation in AEM-based projects.

Documentation: https://wcm.io/testing/<br/>
Issues: https://wcm-io.atlassian.net/browse/WTES<br/>
Wiki: https://wcm-io.atlassian.net/wiki/<br/>
Continuous Integration: https://travis-ci.org/wcm-io/wcm-io-testing/


## Build from sources

If you want to build wcm.io from sources make sure you have configured all [Maven Repositories](https://wcm.io/maven.html) in your settings.xml.

See [Travis Maven settings.xml](https://github.com/wcm-io/wcm-io-testing/blob/master/.travis.maven-settings.xml) for an example with a full configuration.

Then you can build using

```
mvn clean install
```
