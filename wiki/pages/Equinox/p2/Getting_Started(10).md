As of Eclipse project build I20080305 (shortly before Eclipse
3.4/[Ganymede](Ganymede "wikilink") M6), the Eclipse SDK contains a new
provisioning system called [Equinox/p2](Equinox/p2 "wikilink"). p2
replaces Update Manager as a mechanism for managing your Eclipse
install, searching for updates, and installing new functionality. This
document will help you getting started with p2. If you want to explore
some of the capabilities of p2 that are not exposed to end users in the
Eclipse SDK, see also [Equinox/p2/Getting Started Admin
UI](Equinox_p2_Getting_Started_Admin_UI "wikilink").

## p2 user interface

In the workbench, p2 replaces the **Help \> Software Updates** menu
entry from Update Manager. The resulting dialog shows the features that
are installed, and the features that are available to install. You can
add additional update sites by clicking **Manage sites** in the
**Available features** pane. Or, simply drag and drop a URL link in a
browser to the **Available features** pane. For example, dragging this
link (http://download.eclipse.org/eclipse/testUpdates) will add the
eclipse test updates site. Select any set of available features and
click **Install** to install them into the current running system.

[Equinox/p2/Update UI Users
Guide](Equinox/p2/Update_UI_Users_Guide "wikilink") provides more
information about how the UI works.

You must modify the p2 UI code if you want to [Run the p2 UI in a
self-hosted
workbench](Equinox/p2/Getting_Started#Running_the_p2_UI_from_a_self-hosted_workbench "wikilink").

## p2 basics

### Disk layout

Before [p2](Equinox/p2 "wikilink"), many Eclipse users circumvented
Update Manager and installed new plug-ins by dumping them in the
*eclipse/plugins/* directory and restarting with the -clean command line
argument. There are many drawbacks to this "wild west" approach that we
won't get into here, but suffice it to say that this approach to
installation is not recommended. Although p2 will detect plug-ins added
directly to the *plugins* folder (with an associated startup performance
cost), alterations to plug-ins installed by p2 in this directory is
**not** supported. If you manually remove a plug-in installed by p2, or
attempt to replace with a different version, p2 will not detect it and
may be broken.

The short rule of thumb is: if you added something manually, you can
remove it manually. If you installed via p2, you should uninstall via
p2. The shorter rule of thumb is: don't mess with the plugins folder if
you can avoid it. p2 provides a new dropins folder that is much more
powerful and allows separation of content managed by p2 from content
managed by other means (described below).

When you install a p2-enabled Eclipse application, you will notice some
new files and directories that didn't exist before. Here is a subset of
a typical Eclipse install tree with some of this new content
highlighted:

` eclipse/`
`   configuration/`
`     config.ini`
`     `**`org.eclipse.equinox.simpleconfigurator/`**
`       `**`bundles.info`**
`   `**`dropins/`**
`   features/`
`   `**`p2/`**
`   plugins/`
`   eclipse.exe`
`   eclipse.ini`
`   ...`

The file **bundles.info** contains a list of all the plug-ins installed
in the current system. On startup, all the plug-ins listed in this file
are given to OSGi as the exact set of plug-ins to run with.

Any extra plug-ins in the *plugins* directory or elsewhere are ignored.
If you really want to force Eclipse to startup with a particular set of
bundles installed, you could manually edit this file to have the
contents you need. However, unless you're just hacking around or
testing, editing this file is **not recommended**. However, it's useful
to know about this file so you can see exactly what is installed in the
system you are running. Typically, p2 is the interface between this file
and the rest of the world. Clients initiate provisioning requests to p2
via API, the GUI or some of the facilities described below, and as a
result p2 may install or uninstall bundles from the OSGi runtime by
updating the bundles.info file.

The new **dropins** folder is where you can drop in extra plug-ins if
you don't want to use the p2 user interface. See the
[dropins](Equinox/p2/Getting_Started#Dropins "wikilink") section for
more details. For backwards compatibility, p2 will also detect extra
plug-ins dropped into the *plugins* directory, and install any
discovered bundles into the system.

Note that the above tree is just a sample layout of an Eclipse-based
application. Very little of this structure is actually guaranteed to
take this shape. The configuration directory can be stored separately
from the rest of the install using the -configuration command line
argument. The bundles and features may be in a shared [bundle
pool](Equinox/p2/Getting_Started#Bundle_pooling "wikilink") elsewhere on
disk. The eclipse.ini and eclipse.exe files may be branded with
different names. The p2 directory may also be stored elsewhere when a
single management agent is configuring multiple applications. In short,
you should never write code that makes assumptions about the relative
placement of files and directories in an Eclipse install.

### Dropins

Provisioning operations should generally occur using the p2 UI, or by
invoking [p2
tools](http://help.eclipse.org/ganymede/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/p2_director.html)
or APIs. However, there are situations where scripts need to install
plugins and features via file system operations, and have the new
content dynamically discovered by the system either at startup, or while
running. To support this kind of low-level manipulation of the system,
p2 supports the notion of *watched directories*. A watched directory is
a place where a user or script can drop files and have them discovered
by p2. Various policies can be applied to watched directories to
configure when they are checked for new content, and whether to eagerly
install discovered content.

The Eclipse platform ships with a default watched directory called
*dropins*. The dropins folder is configured to be scanned during
startup, and for changes to be immediately applied to the running
system. Thus the dropins folder can be used much like the *plugins*
directory was used in the past.

A subtle twist on old behavior here is that plug-ins and features added
to the *dropins* folder are properly installed into the system rather
than being forced in. This means p2 has an opportunity to confirm that
the new plug-in doesn't conflict with other installed plug-ins, and it
can even go out and fetch any missing prerequisites of the newly dropped
in plug-ins. This also means you can later use the GUI to install extra
functionality that depends on the plug-ins in the *dropins* folder,
since p2 knows about them and can reason about their dependencies. In
other words, new plug-ins installed via the *dropins* folder behave
exactly like plug-ins installed via the user interface. Note that
updating plug-ins which are located under the *dropins* folder using the
p2 UI will result in the updated plug-ins being saved under the main
eclipse/plugins and eclipse/features folders and not under the *dropins*
hierarchy as siblings to the older versions of the plug-ins, as might be
expected.

#### Supported dropins formats

The dropins folder supports a variety of layouts, depending on the scale
of your application and the desired degree of separation of its parts.
The simplest layout is to just drop plug-ins in either jar or directory
format directly into the dropins folder:

` eclipse/`
`   dropins/`
`     org.eclipse.core.tools_1.4.0.200710121455.jar`
`     org.eclipse.releng.tools_3.3.0.v20070412/`
`       plugin.xml`
`       tools.jar`
`       ... etc ...`
`   ...`

You can also drop in the traditional Eclipse application or extension
layout directly in the dropins folder:

` eclipse/`
`   dropins/`
`     eclipse/`
`       features/`
`       plugins/`

If you have various different components being dropped in, and you want
to keep them separate, you can add an additional layer of folders
immediately below the dropins folder that contain traditional Eclipse
extensions:

` eclipse/`
`   dropins/`
`     emf/`
`       eclipse/`
`         features/`
`         plugins/`
`     gef/`
`       eclipse/`
`         features/`
`         plugins/`
`     ... etc ...`

Finally, you can add link files as in the traditional Eclipse links
folder:

` eclipse/`
`   dropins/`
`     emf.link`

#### Debugging dropins

If you are attempting to use dropins, but your bundles are not being
found, first ensure `org.eclipse.equinox.ds` and
`org.eclipse.equinox.p2.reconciler.dropins` are marked to auto-start.

Resolution errors with dropins are silently ignored. To enable useful
logging messages, place the following tracing options in your `.options`
file:

``` text
org.eclipse.equinox.p2.core/debug=true
org.eclipse.equinox.p2.core/reconciler=true
```

and then run with "`-debug path/to/.options`"

### Bundle pooling

Prior to p2, each Eclipse application had its own private *plugins*
directory where the application's software was kept. This had the
drawback that systems with two or more Eclipse-based applications
installed ended up with significant duplication of software and other
artifacts. Furthermore, the common pieces had to be upgraded separately
for each application, often resulting in slow downloads of software
already available elsewhere on the local system.

To escape from this duplication problem, p2 natively supports the notion
of bundle pooling. When using bundle pooling, multiple applications
share a common *plugins* directory where their software is stored. There
is no duplication of content, and no duplicated downloads when upgrading
software. A Windows system configured to use bundle pooling would have a
layout something like this:

` Application1/`
`   configuration/`
`     config.ini`
`     ... other configuration files for Application1...`
`   Application1.exe`
`   Application1.ini`
` Application2/`
`   configuration/`
`     config.ini`
`     ... other configuration files for Application2...`
`   Application2.exe`
`   Application2.ini`
` ...`
` Documents and Settings`
`   Username`
`     .p2/`
`       org/eclipse.equinox.p2.core`
`       org/eclipse.equinox.p2.director`
`       org/eclipse.equinox.p2.engine`
`       org/eclipse.equinox.p2.touchpoint.eclipse`
`         plugins/      <-- shared bundle pool`

With this layout, all the software is stored in a single *plugins*
directory outside of the application install area. p2 takes care of
ensuring that the bundles needed by the various applications in the
system are present in the bundle pool.

In Windows 7 SP1 Mars release the structure is more like the following.

    C:\Users\{user name}\.eclipse\org.eclipse.platform_4.5.0_1881578221_win32_win32_x86_64
       configuration
          .settings
          org.eclipse.core.runtime
          org.eclipse.e4.ui.css.swt.theme
          .
          .
          . {10 more...}
       features
          org.eclipse.cdt.gdb_8.7.0.201506070905
          org.eclipse.cdt.gnu.build_8.7.0.201506070905
          org.eclipse.cdt.gnu.debug_8.7.0.201506070905
          .
          .
          . {219 more...}
       p2
          org.eclipse.equinox.p2.core
          org.eclipse.equinox.p2.engine
          org.eclipse.equinox.p2.repository
          pools.info
          profiles.info
       plugins
          org.eclipse.cdt.core.win32.x86_64_5.3.0.201506070905
          org.eclipse.epf.common.html_1.5.0.v20130128_0851
          org.eclipse.epf.common_1.5.0.v20130128_0851
          .
          .
          . {1,145 more...}
       artifacts.xml

### Installer

There is now an [Equinox/p2/Installer](Equinox/p2/Installer "wikilink")
that will install the Eclipse project SDK. The installer supports both
traditional standalone installs, and installs using [bundle
pooling](Equinox/p2/Getting_Started#Bundle_pooling "wikilink").

## Running the p2 UI from a self-hosted workbench

We do not yet support a properly configured, self-hosted p2 system (see
). This means that if you run a self-hosted workbench, the launched
Eclipse will not be p2 aware. The p2 UI will notice this and tell you
that it cannot open.

If your desire is simply to develop or otherwise play with the new UI,
and you don't care that the self hosted workbench is not provisioned,
you can load the **org.eclipse.equinox.p2.ui.sdk** project and change
this code in **ProvSDKUIActivator**.

` ProvSDKUIActivator.ANY_PROFILE = true;`

This will allow you launch the UI, but please note that the UI will be
operating on the installation profile of the **host workbench**.

## Interaction with legacy Update Manager

In Eclipse platform version 3.4, the old Update Manager still exists
under the covers for backwards compatibility. You can even re-enable the
old Update user interface by enabling the "Classic Update" capability on
the General \> Capabilities preference page.

However, users will rarely have a need for enabling Update Manager,
because p2 is able to install from any update site that was designed for
Update Manager. Add the update site you want to use by dragging a URL
from a browser into the **Available Features** page in the '''Help \>
Software Updates ''' dialog. The features from that site will be added
to the available features list, and from there you can install them into
the system.

### Links Folder

Update Manager supported the notion of an eclipse/links folder, where
\*.link files could be dropped in to connect extensions to an
Eclipse-based application. For backwards compatibility the links folder
is supported by p2 with the same behavior. This is really just a subset
of the functionality available in the
[Dropins](Equinox/p2/Getting_Started#Dropins "wikilink") folder.

## Removing p2

[Equinox/p2/Removal](Equinox/p2/Removal "wikilink") describes the steps
to completely remove p2 from the platform and revert to using the
original Eclipse Update Manager. In addition, for Eclipse project 3.4
builds, there is a link on the right hand side of the download page
called "Eclipse Classic Update Manager". This gives a link to a script
that can be used to remove p2 from any Eclipse SDK, platform binary, or
platform SDK build.

Removing p2 from the Eclipse SDK is no longer supported in
[Galileo](Galileo "wikilink") (3.5) or later. However, the platform
itself is not tied to any particular provisioning technology, so
applications are free to choose an alternative provisioning system (or
none) depending on their requirements.

[Getting Started](Category:Equinox_p2 "wikilink")