# Overview

p2 includes a rudimentary mechanism for obtaining download statistics to
track transfers from an artifact repository. This page describes how to
enable statistics gathering for artifacts in your p2 repositories.

The p2 download statistics mechanism has the following characteristics:

1.  Statistics collection is "best effort": transfers do not fail if
    stats could not be collected
2.  Each artifact repository controls how and where statistics are
    collected for transfers from that repository.

If artifact repository A is gathering statistics, and an artifact is
transferred from repository A to B, and then from repository B to C,
repository A will only obtain statistics for the transfer from A to B.

1.  No personal information is collected. Essentially the statistics
    just summarize the transfers that have occurred from that
    repository.
2.  The repository maintainer can control the granularity of statistics
    gathered. For example they can record every single artifact
    transfer, or only transfers of particular artifacts. In general it
    is best to minimize the set of artifacts that are tracked, since
    each artifact transfer that is recorded incurs an extra round trip
    from the client to the repository.

# Enabling stats in your repository

There are two steps to enable p2 download statistics gathering for your
repository:

1\. In the artifact repository that you want to track downloads from,
add a **p2.statsURI** property specifying the statistics URL (in
artifacts.jar):

` `<repository name='Update Site' type='org.eclipse.equinox.p2.artifact.repository.simpleRepository' version='1'>
`   `<properties size='3'>
`     `<property name='p2.timestamp' value='1269575706171'/>
`     `<property name='p2.compressed' value='true'/>
`     `**<property name='p2.statsURI' value='http://your.stats.server/stats'/>**

To generate it with Tycho, use the [extraArtifactRepositoryProperties
parameter of the assemble-repository
mojo](https://ci.eclipse.org/tycho/job/tycho-sitedocs/ws/target/staging/tycho-p2/tycho-p2-repository-plugin/assemble-repository-mojo.html#extraArtifactRepositoryProperties).

2\. Make your artifact metadata contain the **download.stats** property
for each IU that you want to gather stats for. You can pick one plugin
in your feature for example:

` `<artifact classifier='osgi.bundle' id='test.plugin1' version='1.0.0.201003261255'>
`   `<properties size='3'>
`     `<property name='artifact.size' value='0'/>
`     `<property name='download.size' value='1757'/>
`     `**<property name='download.stats' value='test.plugin1.bundle'/>**
`   `</properties>
` `</artifact>

Every other repository consuming your artifact will receive the same
metadata nad property, so if the repo has tracking enabled as described
in step 1., then it will automatically enable download stats for
artifacts that already set the `download.stats` property. It's
recommended that the project that *produces* the artifact (builds the
jar and the metadata) takes care of adding the `download.stats` and
consumer should just reuse it without altering the metadata of artifacts
they don't "own".

To generate this repository with Tycho, see [the
generateDownloadStatsProperty parameter of the p2-metadata
mojo](https://ci.eclipse.org/tycho/job/tycho-sitedocs/ws/target/staging/tycho-p2/tycho-p2-plugin/p2-metadata-mojo.html#generateDownloadStatsProperty).

In this example, after a successful download a HEAD request will be
issued to:

` `<http://your.stats.server/stats/test.plugin.1.bundle>

(value of the **downloads.stats** property appended to the value of the
**p2.statsURI**).

You can either install software on the server at that location to count
the requests, or simply gather the statistics from your web server log
files.

# Customized stats based on the repository that is stats available

Application providers might want to stats the detail download stats,
such as the name of distribution package, distribution version and the
information of host os. The release engineers of applications can
initialize the customized downloading stats data when building release
version.

The value of customized stats should be set in the default profile of
the distrubited applications. See [P2
Director](http://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/guide/p2_director.html)
for how to specify profile property when materializing your application.

For example, the customized downloading stats is below in the p2 profile
of Juno's J2EE windows package,

`     `**<property name='org.eclipse.equinox.p2.stats.parameters' value='package=jee&version=juno&os=win32'/>**

and the value looks like below for Indigo's C/C++ development package,

`     `**<property name='org.eclipse.equinox.p2.stats.parameters' value='package=cdt&version=indigo&os=linux'/>**

In this example, after a successful download a HEAD request will be
issued to:

` `<http://your.stats.server/stats/test.plugin.1.bundle?package=jee&version=juno&os=win32>

# References

[Download stats](Category:Equinox_p2 "wikilink")