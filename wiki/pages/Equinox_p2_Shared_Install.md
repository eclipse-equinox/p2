## Summary of Needs

For certain installations of Eclipse, there will exist the notion of a
shared installation -- this may be in the case of a Linux system where a
base set of software is installed via packages (perhaps RPMs), or may be
in a Maya deployment where shared profiles are defined in a central
server. In both cases, it is necessary to perform reconciliation between
the shared profile and the user's current instantiation of the profile
including any modifications they may have made.

Beyond performing reconciliation, when a reconciliation can not be made,
the user will need to be presented with the list of software that is no
longer compatible after reconciliation similar to what happens when you
upgrade to a new major version of FireFox. When the user is presented
with the list of software that is no longer compatible, the option could
then be presented to automatically search for newer versions of that
software.

## Proposed Solutions Discussed

### Custom Configurator with Intelligence

One option would be to provide a custom configurator that replaces the
standard one used in the provisioning work. This configurator could be
structured to check the shared profile source and then compare against
the current list of bundles. Since the configurator deals at the bundle
level, and the reconciliation needs to happen at the IU-level, this
would require a lot of mapping and complex logic.

### Reconciler on Launch

Another option is to have a simple configurator start the runtime with
only enough plugins to run the reconcilation logic. This would work with
IUs and see if reconciliation is needed between the shared profile and
the user's instantiation of this profile. The benefit of this solution
is that the reconciler can leverage the provisioning agent, but there is
the potential for impact to the Eclipse startup time (needs
investigation to understand impact).

### Simple Configurator with optional Reconciler

A third option is to have a simple configurator that knows about a
shared bundle list and the user's bundle list. This configurator could
then check if the shared bundle list has changed, and if it has, only
use the shared list. The reconciler could then be launched on startup to
allow updating the user's addons to the profile. This approach would
partially work but not necessarily handle some cases such as those
required by Maya where checking if the shared profile has changed is
more complex.

## Current Approach

For a first implementation, we have decided to attempt the Reconciler on
Launch approach. Our thought is to have an Activator start that performs
the reconciliation running on a bare-bones OSGi runtime. The reconciler
would check for differences and update the user's profile as needed.
Finally it would either continue to load the extra bundles into the
current runtime or if some base software has changed, potentially reload
the base OSGi runtime and then launch the desired application.

To begin, we are creating two bundles:

  - **org.eclipse.equinox.prov.shared** - contains the activator and
    code to look up the shared profile
  - **org.eclipse.equinox.prov.reconciler** - contains the reconciler to
    resolve the differences between two profiles

In addition, the shared bundle will have an extension point allowing an
ISharedProfileProvider to be specified. In the case of Maya this would
have an implementation that coordinates with the central server while
the shared linux install would have a version that retrieves the shared
profile that might have been installed via a packaging mechanism.

To enable the reconciliation to take place, there may be additional
properties stored in config.ini or alternate configuration file that are
used by the activator, profile provider, and reconciler.

## Case study: How to create a shared install for 3 os/arch/ws combinations using a bundle pool

The content in previous chapters seems to be dealing with having two
profiles to synchronize. Another definition of a **shared install**, at
least for me, is the want of **sharing all common content of multiple
eclipse installations**.

So, in this case study I'd like to create an installation structure that
throws all common content into a common *bundle pool*, well, I'd prefer
the term *artifact pool*. Here is the structure I'd like to create:

`  * root dir`
`     * install location for windows`
`     * install location for solaris`
`     * install location for linux`
`     * common location`

, with the *common location* containing all common things, like
artifacts and readmes and such, and the *install locations* containing
merely the OS-specific parts, and a profile each to maintain the
installation (and the pool).

### Prerequisites: a repository containing an eclipse product for all OS's

In order to create the installation structure above I'm going to create
a repository containing IUs and artifacts for all the OS's I want. Since
Eclipse 3.5 such a repository can be created as a *byproduct* of a
standard pde build. How to set up pdebuild for such a product is beyond
the scope of this article, but these properties are worth mentioning,
setting them in your build(.properties) will generate a p2 repo as well
as some zips containing the build result:

`  * p2.gathering=true`
`  * p2.metadata.repo=file:`<repo location>
`  * p2.artifact.repo=file:`<repo location>
`  * p2.flavor=tooling`

My pdebuild is then launched with this command:

```
    <java>
        -jar plugins/org.eclipse.equinox.launcher_<version>.jar
        -buildfile plugins/org.eclipse.pde.build_<version>/scripts/productBuild/productBuild.xml
        -application org.eclipse.ant.core.antRunner
        -DjavacDebugInfo=on
        -DjavacVerbose=false
        -DjavacFailOnError=true
        -Dbuilder=<build properties file>
        -Dconfigs="win32,win32,x86 linux,gtk,x86 linux,gtk,x86_64 solaris,gtk,sparc"
        -DbuildId=ignore
```

Once the build finished successfully by product will be ready for the OS
combinations seen in the configs property above.

### Installing with the bundlepool option

Using the **director** now I can install from the generated repository.
The way I do it is by using Eclipse PDE, creation a new *Eclipse
Application* launch config, with the application
**org.eclipse.equinox.p2.director**.

Specifying *-help* as commandline option will respond with the list of
available commands, in the end I install with the following set of
options:

`  * -repository `<location of my generated repo>
`  * -installIU `<IU name>` ('' hint: use -list to find out what IUs were available for install '')`
`  * -destination C:/WindRiver/destination_linux (`*`this``   ``is``
 ``my``   ``install``   ``location``   ``with``   ``things``   ``like``
 ``the``   ``launcher``   ``exes`*`)`
`  * -bundlePool C:/WindRiver/bundlePool (`*`this``   ``is``   ``my``
 ``pool``   ``with``   ``all``   ``the``   ``bundles,``   ``usable``
 ``by``   ``every``   ``install`*`)`
`  * -profile TestProfileLinux (`*`I``   ``do``   ``generate``
 ``one``   ``profile``   ``per``   ``installation`*`)`
`  * -p2.os linux (`*`This``   ``install``   ``is``   ``for``
 ``linux`*`)`
`  * -p2.ws gtk (`*`This``   ``install``   ``is``   ``for``
 ``gnome`*`)`
`  * -p2.arch x86 (`*`This``   ``install``   ``is``   ``for``
 ``32bit``   ``intel`*`)`

This director call will already give me an install structure like the
desired one:

`  * root dir (`*`C:/WindRiver`*`)`
`     * install location for linux (`*`C:/WindRiver/destination_linux`*`)`
`     * common location (`*`C:/WindRiver/bundlePool`*`)`

Now, repeating the director call with some argument changes will also
give me solaris and windows installations, these changes

`  * -destination C:/WindRiver/destination_win32`
`  * -profile TestProfileWin32`
`  * -p2.os win32`
`  * -p2.ws win32`
`  * -p2.arch x86`

for example will create a windows installation for my product, with the
bundles being put into the pool.

### Limitations when running the installation

Trying to launch the application from *C:/WindRiver/destination_win32*
proves the installation to be sane, every bundle is resolved
successfully and the product is launched successfully. **p2 is great
:)**

However, there are **severe limitations** yet to be overcome. Zipping up
this installation and putting it into some completely different
directory is not going to work. This *roaming* is not possible with
bundle pools. The reason is absolute paths in some areas, preventing you
from moving around the installation.

One such absolute path can be found in the **launcher.ini** file for
your product. The file contains the option

`  * -install C:/WindRiver/destination_win32`

however, I was able to move my application and then launch it fine by
changing this absolute path to a relative one: *-install
../destination_win32*. After some tests I found out that
"../destination_win32" will only work in cases where the PWD is the
install directory, launching the product from anywhere else renders this
path invalid. That's weird though, the .ini contains other relative
paths, like the one for the launcher jar, so I guess those work fine.

Another location with an absolute path is the **profile** itself, it
contains a reference to the bundle pool and will be broken if you move
anything. So this will rule out any updates to the bundle pool I guess.

### Result

As a result of this case study I think I'd take away the fact that in
principle everything is working fine, p2 is doing a great job there.
Still, the lack of a roaming option for such installs is a bummer, it
prevents me from using this great concept internally\!

[Shared Install](Category:Equinox_p2 "wikilink")