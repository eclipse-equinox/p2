This page will provide a place for interested parties to express their
API requests against p2 for the 3.5 timeframe. A wiki page is more
practical than bug reports due to the numerous changes that will be
made. Once requirements have stabilized, bug reports will be opened
against Equinox p2.

## Source Locations

PDE has support to track source locations in the target platform.
However, others, who may not have access to PDE need access to the
source bundles of the platform. For example JDT requires the JUnit
source bundle
[Bug 225594](https://bugs.eclipse.org/bugs/show_bug.cgi?id=225594).

Before p2, PDE could search all installed bundles for source bundles.
However, p2 does not install source bundles. Instead, p2 provides a file
<eclipseInstall>\\eclipse\\configuration\\org.eclipse.equinox.source\\source.info
that lists all of the source bundles in the platform. In 3.4 PDE hard
codes the location of this file, reads it and adds the source bundles to
the target platform.

In 3.5, PDE would like to see p2 provide API to get a list of known
source bundles (what is currently stored in the source.info file). That
way the list is available outside of PDE.

[Category:Equinox p2](Category:Equinox_p2 "wikilink")