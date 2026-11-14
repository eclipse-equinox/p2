Equinox p2 now produces a small SWT-based installer that can be used to
install the Eclipse SDK. This article gives details on how to use the
installer.

## Why should I use the installer?

**NOTE**: The installer does not seem to be downloadable using the
instructions on this page. Apparently [it is not regularly maintained
right now](http://www.eclipse.org/forums/index.php/t/276250/) and the
[builds for it have been
disabled](http://dev.eclipse.org/mhonarc/lists/equinox-dev/msg07435.html).
[Older versions and the current source
code](http://dev.eclipse.org/mhonarc/lists/equinox-dev/msg07435.html) of
the installer are available, and the [director
application](Equinox/p2/Director_application "wikilink") exposes roughly
the same capabilities, though at the command line rather than through a
GUI.

-----

Traditionally Eclipse software is obtained in the form of a zip or
tar.gz file that is simply extracted onto disk and run. While this
simplicity has its advantages, there are also some disadvantages for
users:

  - Downloading a 100+ MB zip file over HTTP can be painful on a slow
    connection
  - If you already have some or all of the plug-ins on your local disk,
    you are downloading content unnecessarily
  - More advanced compression techniques can't be used, because there is
    no opportunity to add the decompression code at the other end
  - You must pick a single mirror at the start, and can't switch mirrors
    in case of failure
  - There is no opportunity to check code certificates to ensure the
    software you are downloading was created by a trusted source

The installer leverages p2's inherent support for multi-threading,
pack200 compression, dynamic mirror balancing, and certificate
validation to address these problems.

## How do I use the installer?

![P2-installer-wizard.png](P2-installer-wizard.png
"P2-installer-wizard.png")

  - Get the installer for your platform from the [equinox download
    page](http://download.eclipse.org/equinox/). The installer zips are
    listed under the Provisioning section, once you select your version.
  - Unzip the installer anywhere on your local disk, eg., `/opt/eclipse`
  - Run the p2 installer executable, eg., `/opt/eclipse/p2installer`
  - In the install wizard, select where you want to install the
    software, eg., `~/eclipse`
  - Select whether to do a Stand-alone or Shared install:

:\*If doing a Stand-alone install, you can delete the installer
directory (`/opt/eclipse`) when done, as your target directory
(`~/eclipse`) will contain a full Eclipse install.

:\*If doing a Shared install, all the shared Eclipse SDK plugins will be
stored in a default directory, e.g. (`C:\Documents and
Settings\Administrator\.p2`) on Windows XP.

## How do I configure the installer?

The installer uses the Java system property
`org.eclipse.equinox.p2.installDescription` to fetch the URL of the
installer properties file. By default this is set to an [installer
website](http://download.eclipse.org/eclipse/testUpdates/sdk-installer.properties).

  - Edit the `p2installer.ini` file to point to a local properties file,
    for example `installer.properties`, like so:

` -vmargs -Dorg.eclipse.equinox.p2.installDescription=./installer.properties`

  - Then modify `installer.properties` with the properties you want. In
    particular you will probably want to change the
    eclipse.p2.bundleLocation and the [update
    site](Eclipse_Project_Update_Sites "wikilink") you use.

#### Example:

Here the site is set to eclipse 3.6, the bundle pool folder is
"bundle-pool" and the directory where eclipse.exe gets installed is
"eclipse-win32"

    eclipse.p2.metadata=http://download.eclipse.org/eclipse/updates/3.6
    eclipse.p2.artifacts=http://download.eclipse.org/eclipse/updates/3.6
    eclipse.p2.bundleLocation=../bundle-pool
    eclipse.p2.installLocation=../eclipse-win32
    eclipse.p2.flavor=tooling
    eclipse.p2.profileName=Eclipse SDK
    eclipse.p2.launcherName=eclipse
    eclipse.p2.rootId=org.eclipse.sdk.ide
    eclipse.p2.autoStart=true

#### Note also:

You can use an installer.properties file to fetch plugins into the
p2.touchpoint.eclipse folder, but you cannot then install those plugins
unless they contain a Product (that is, they include a eclipse
executable).

The following will work to download the EMF SDK plugins, but will NOT
work to install an EMF-enabled Eclipse install, as the sdk.feature.group
does not include the Eclipse SDK itself.

`eclipse.p2.metadata=`<http://download.eclipse.org/modeling/emf/updates/releases/>
`eclipse.p2.artifacts=`<http://download.eclipse.org/modeling/emf/updates/releases/>
`eclipse.p2.flavor=`
`eclipse.p2.profileName=EMF SDK`
`eclipse.p2.launcherName=eclipse`
`eclipse.p2.rootId=org.eclipse.emf.sdk.feature.group`
`eclipse.p2.autoStart=true`

### Supported installer properties

The following properties are currently supported in the install
description file:

  - eclipse.p2.rootId
    The root [Installable Unit](Installable_Units "wikilink") to be
    installed
  - eclipse.p2.rootVersion
    The version of the root installable unit to install. If this
    property is omitted, the installer will select the newest available
    IU that matches the *rootId* property.
  - eclipse.p2.metadata
    The URL of the metadata repository to install from
  - eclipse.p2.artifacts
    The URL of the artifact repository to install from
  - eclipse.p2.launcherName
    The name of the application launcher (e.g., *eclipse.exe*). This is
    used in conjunction with the *autoStart* property to launch the
    installed application
  - eclipse.p2.autoStart
    A boolean property specifying if the installed application should be
    launched immediately after install.
  - eclipse.p2.profileName
    A human readable name of the product being installed. This value is
    used when referring to the product in the installer dialog.
  - eclipse.p2.installLocation
    The application instal location. If ommitted, the user is prompted
    for a location
  - eclipse.p2.agentLocation
    The p2 agent data location directory
  - eclipse.p2.bundleLocation
    The application bundle pool directory




[Installer](Category:Equinox_p2 "wikilink")