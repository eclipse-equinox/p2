# Shared Install

One of the work items on the [Equinox p2 Juno
plan](Equinox/Plan/Juno/p2 "wikilink") is to improve the Shared Install
story. This wiki page will be used to collect a list of current problems
related to the Shared Install story, as well as potential solutions and
even brain-storming new and crazy ideas for improving the situation.

The umbrella bug report for the plan item is
[Bug 358471](http://bugs.eclipse.org/358471).

## Current Issues

### Missing Repositories ([Bug 249133](http://bugs.eclipse.org/249133))

**Problem** - Currently when you start Eclipse as a user, you are
presented with an empty list of update repositories even though the
admin user has set them in the base install.

**Discussion/Ideas**

  - Transient repositories - read parent's repos but consider them
    transient and don't write them out
  - Cascaded repositories
  - RepoMgr loads from shared area and user's area and always writes
    everything to user area

### User's Locally Installed Features Lost on Base Update ([Bug 304132](http://bugs.eclipse.org/304132))

**Problem** - If the user is running in shared install mode and has
their own features installed and the admin user updates something in the
base location, then the user's locally installed features are lost.

**Discussion/Ideas**

  - Keep separate metadata for the base location and the user location.
    At start up p2 reads both sets of metadata and merges them.
      - The user metadata should not contain the parts from the base
        location (unless these parts are actually installed in both
        places)

### User's Configuration Changes Lost Across Major Updates ([Bug 329587](http://bugs.eclipse.org/329587))

**Problem** - The calculation of the default user's config area is based
on the value in the shipped .eclipseproduct file, along with the hash of
the install folder for the product you are running. If the value in this
file is incremented with each release, a new configuration area will be
chosen (by default) and user changes will be lost. **Discussion/Ideas**

  -
## Random Thoughts and Ideas

### Have a Global Shared Install Mode

When discussing shared install and looking at some of the code, there
are multiple places where we do the same things like calculate if we are
in shared mode, calculate paths, etc etc. It would be a lot of work but
one interesting idea would be to basically determine early on that we're
in shared mode, create an object with the context that we need, and then
pass that along/query it for information later. Kind of a global "shared
mode" switch being flipped. It is unclear if this logic would be in a
new bundle that somehow extended the current behaviour or something
else.

## Linux Installations

### Fedora (RPM)

#### Installing Without Moving Artefacts

The biggest challenge when using the p2director in combination with RPM
is the movement of files and artefacts. RPM would like to know the files
which are being added so that they can be removed during uninstallation.
RPM also has a verification functionality which checks the integrity of
a file on system by comparing it with the one which was installed (there
are exceptions for configuration files and the like).

So one thing which would make p2 and rpm work together nicely is if were
possible to install artefacts into the eclipse installation without
moving the artefacts on disk. For example:

The platform rpm would install the platform plugins into:

` /usr/lib/eclipse/{feature,plugins}`

Then a plugin rpm like eclipse-jdt can install things (in a runnable
state) into:

` /usr/lib/eclipse/repos/jdt (or any other suitable location inside or outside the eclipse directory)`

Then run the director to install things from this location (bundle
pool?) into /usr/lib/eclipse/configuration/.../bundles.info et. al.
without moving the artefacts of course.

#### Verification (Dry Run)

The other thing needed for rpm is to be able to verify that the
provisioning will go through successfully before actually doing it. This
is important because the rpm is doing this as root user, and we would
like to make sure that the process will not damage the users already
installed plugins (rpms) especially if it is due to a packgers mistake.
I believe -verifyOnly accomplishes this. If so, then this is just meant
to emphasize its importance.

#### Garbage Collection During Uninstallation

There are a lot of files created by the reconciler (can I say by p2 ?)
during installation of a new plugin.

Here are some examples:

<code>

    /usr/lib64/eclipse/p2/org.eclipse.equinox.p2.core/cache/artifacts.xml
    /usr/lib64/eclipse/configuration/org.eclipse.core.runtime/.extraData.1
    /usr/lib64/eclipse/configuration/org.eclipse.core.runtime/.orphans.1
    /usr/lib64/eclipse/configuration/org.eclipse.core.runtime/.namespaces.1
    /usr/lib64/eclipse/configuration/org.eclipse.core.runtime/.contributors.1
    /usr/lib64/eclipse/configuration/org.eclipse.core.runtime/.contributions.1
    /usr/lib64/eclipse/configuration/org.eclipse.core.runtime/.manager
    /usr/lib64/eclipse/configuration/org.eclipse.core.runtime/.manager/.fileTable.8
    /usr/lib64/eclipse/configuration/org.eclipse.core.runtime/.manager/.fileTableLock
    /usr/lib64/eclipse/configuration/org.eclipse.core.runtime/.manager/.fileTable.7
    /usr/lib64/eclipse/configuration/org.eclipse.core.runtime/.table.1
    /usr/lib64/eclipse/configuration/org.eclipse.core.runtime/.mainData.1
    /usr/lib64/eclipse/configuration/org.eclipse.osgi/bundles/79/data/listener_1925729951
    /usr/lib64/eclipse/configuration/org.eclipse.osgi/bundles/79/data/listener_1925729951/content.jar
    /usr/lib64/eclipse/configuration/org.eclipse.osgi/bundles/79/data/listener_1925729951/artifacts.jar
    /usr/lib64/eclipse/configuration/org.eclipse.osgi/bundles/79/data/cache.timestamps
    /usr/lib64/eclipse/configuration/org.eclipse.osgi/bundles/79/data/timestamps190749078
    /usr/lib64/eclipse/configuration/org.eclipse.osgi/bundles/70/data/487635717/content.jar
    /usr/lib64/eclipse/configuration/org.eclipse.osgi/bundles/70/data/487635717/artifacts.xml
    /usr/lib64/eclipse/configuration/org.eclipse.osgi/bundles/90/data/-55163747/artifacts.xml
    /usr/lib64/eclipse/configuration/org.eclipse.osgi/bundles/90/data/-55163747/content.xml
    /usr/lib64/eclipse/configuration/org.eclipse.osgi/bundles/90/data/764898892/artifacts.xml
    /usr/lib64/eclipse/configuration/org.eclipse.equinox.app/.manager/.fileTableLock
    /usr/lib64/eclipse/configuration/org.eclipse.update/platform.xml

</code>

Since rpm does not know about these files, it will not remove them
during uninstallation, and it leaves a lot of dangling files in the
system. It would be nice if p2 had functionality to remove these files
so that it can be used during the removal of a plugin rpm or the main
platform rpm.

There are also the profile files:

<code>

    /usr/lib64/eclipse/p2/org.eclipse.equinox.p2.engine/profileRegistry/PlatformProfile.profile/1317651766592.profile.gz
    /usr/lib64/eclipse/p2/org.eclipse.equinox.p2.engine/profileRegistry/PlatformProfile.profile/1317651842776.profile.gz

</code>

These would have to be cleaned up more carefully. I don't have a
solution for this but it would help rpm if the file was overwritten
instead of renamed. I have not thought about this a lot but I thought I
would mention it.

[Shared Install Improvements (Juno)](Category:Equinox_p2 "wikilink")