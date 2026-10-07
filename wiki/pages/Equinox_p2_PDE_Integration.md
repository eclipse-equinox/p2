This page is intended to document changes required in the PDE component
to support integration of p2 tooling into the platform.

These items should be addressed in the 3.4 time frame. The current
priority is to make progress in all areas to support the smooth
introduction of p2 tooling into the platform. P2 tooling must be able to
work with PDE. More complete solutions can be looked at afterwards.

## 3.4 Plan

1.  Support p2 metadata creation during site building, feature building,
    plugin export, product export
2.  Allow target provisioning from p2 repositories
3.  Separate installable bundles and source bundles when launching
4.  Read p2 bundles.txt data on startup to build target platform
    (completed 3.4 M4)
5.  Write p2 bundles.txt data on Eclipse launches (completed 3.4 M5)
6.  Write p2 bundles.txt data on OSGi launches with start level data
    (completed 3.4 M5)

## Metadata Creation

See [216637](https://bugs.eclipse.org/bugs/show_bug.cgi?id=216637%7CBug)

PDE Build will have support to generate p2 metadata in 3.4 M5. PDE UI
must update the site builder, feature builder, plugin export, and
product export tooling to invoke the PDE Build support.

The properties that must be set are as follows:

  - generate.p2.metadata = true
  - p2.metadata.repo =
    [file://${buildDirectory}/repo](file://$%7BbuildDirectory%7D/repo)
  - p2.artifact.repo =
    [file://${buildDirectory}/repo](file://$%7BbuildDirectory%7D/repo)
  - p2.flavor = tooling
  - p2.publish.artifacts = false

There will be no changes to the editors UI. Metadata generation will be
transparent to the user and always take place in a p2 hosts. We may add
an option in the preferences to turn off metadata generation as it may
cause problems for certain RCP development scenarios.

## p2 Target Provisioning

See [Bug 204347](https://bugs.eclipse.org/bugs/show_bug.cgi?id=204347)

PDE UI added an extension point in the 3.3 timeframe that allows
implementors to add new target provisioners to the list available when
pressing the Add... button on the Target Platform preference page. The
extension point allows a wizard page contribution and expects a
directory to be returned. This directory should contain any new bundles
that should be added to the target platform.

Currently the target provisioner has support for loading from the file
system and update sites. The file system option will remain the same.
The update site provisioner is very limited in features and will be
removed. The new provisioner, which will support p2 repositories, will
also be able to provision from update sites using support already
available in p2.

We are hoping to reuse viewers and other UI support from other areas of
p2. The UI will allow users to browse through the features/groups of a
repository (or update site) and select what to add to the target. The
bundles/artifacts associated with the selected features/groups will be
downloaded to a directory and this directory will be returned to PDE UI.

Code for this feature will be added in org.eclipse.equinox.p2.target.

There already exists a site editor that allows update sites to be
converted to the p2 equivalent. The site editor generates metadata for
p2.

We want the ability to provide the metadata generator with 'advice'.
This advice allows p2 to know more about how to install/launch IUs. For
example, the start level could be specified, or a port to run on. We
want to prompt for this information.

## Separate installable bundles and source bundles

See \[<https://bugs.eclipse.org/bugs/show_bug.cgi?id=217304>| Bug
217304\]

In 3.4 PDE introduced the concept of source bundles where source can be
packaged in OSGi bundles instead of features. Because they are OSGi
bundles and are in the plugins directory, Eclipse installs them on
startup. p2 would like to avoid installing them, as they contain no
code. To do this they will not be added to the bundles.txt.

However, without the source bundles being specified in the bundles.txt,
PDE will not know where to look for source. p2 will generate a second
file containing a list of source bundles. PDE will have to read this
file to find source bundle locations, these locations will have to be
added to the target. PDE will also need support added to write out the
source bundles file when launching an Eclipse runtime or OSGi launch.

The format of source.bundles.txt will be the same as bundles.txt.
However, the symbolic name and version entries will be left blank, the
start level will be -1 and the autostart will be false.

## Build Target Platform from bundles.txt

See \[<https://bugs.eclipse.org/bugs/show_bug.cgi?id=209260>| Bug
209260\] Completed for 3.4 M4

PDE will read bundles.txt on startup and resolve the target platform
using the information found.

## Write bundles.txt on Launch

See \[<https://bugs.eclipse.org/bugs/show_bug.cgi?id=210539>| Bug
210539\] Completed for 3.4 M5

When launching using an Eclipse runtime launch configuration or an OSGi
launch configuration in a p2 host, a bundles.txt file will be written
out in the same location as the config.ini. The generated config.ini
will contain the property
org.eclipse.equinox.simpleconfigurator.configUrl pointing to the
location of the bundles.txt. The bundles.txt will contain an entry for
each bundle in the launched runtime along with a start level and
auto-start setting. Except for a few hard coded exceptions, Eclipse
runtime launches will have every bundle's start level set to the
default. When launching OSGi, the start level and autostart settings are
taken from the launch configuration. There is currently no support for
users to change whether bundles.txt is generated or where the file is
placed.

[PDE](Category:Equinox_p2 "wikilink")
[Category:PDE](Category:PDE "wikilink")
[Category:PDE/Build](Category:PDE/Build "wikilink")