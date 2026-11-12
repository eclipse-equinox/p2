# P2 repository

## Overview

p2 provides an extensible provisioning platform for Eclipse components and Eclipse-based applications. 

It including a state of the art dependency resolver based on SAT4J, a mechanism to perform transactional state changes, an extensible set of actions.

It also provides a provisioning solution for OSGi systems with the ability to manage non-running instances, start level, and allows for bundle pooling.

* 👔 [Eclipse project entry](https://projects.eclipse.org/projects/eclipse.equinox) 

## How to build

To build with repo use the following command:

`mvn clean verify -DskipTests`

To build and run the unit test, use the following command:

`mvn clean verify`

### Building the update site

The update site (p2 repository) containing all p2 bundles and features is only built when the `updatesite` profile is activated:

`mvn clean verify -Pupdatesite -DskipTests`

The site is created in `releng/org.eclipse.equinox.p2.updatesite.repository/target/repository`.
A zipped version is next to it in `releng/org.eclipse.equinox.p2.updatesite.repository/target`.

## How to contribute

See [CONTRIBUTING.md](https://github.com/eclipse-equinox/.github/blob/main/CONTRIBUTING.md)

[![Create Eclipse Development Environment for Equinox P2](https://download.eclipse.org/oomph/www/setups/svg/P2.svg)](
https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/eclipse-equinox/p2/master/releng/org.eclipse.equinox.p2.setup/EquinoxP2Configuration.setup&show=true
"Click to open Eclipse-Installer Auto Launch or drag into your running installer")
