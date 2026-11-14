# Admin UI RCP App

![P2-admin-ui-RCP.png](P2-admin-ui-RCP.png "P2-admin-ui-RCP.png") If
you're looking to start up the [Equinox p2 Admin
UI](Equinox_p2_Admin_UI_Users_Guide "wikilink") from the commandline
using Java instead of eclipse.exe, here's one way:

`#!/bin/bash`
`vm=/opt/sun-java2-5.0/bin/java`
`eclipsehome=~/eclipse/p2`
`workspace=$eclipsehome/workspace`

`pushd $eclipsehome >/dev/null`
`if [[$#_-eq_0|$# -eq 0]]; then`
`       rm -fr $eclipsehome/eclipse $workspace`
`       mkdir -p $eclipsehome/eclipse`
`       p2=equinox-p2-agent-3.4-linux.tar.gz`
`       echo "Unpack $p2..."`
`       tar xzf $p2 -C eclipse`
`fi`
`cp=$(find $eclipsehome -name "org.eclipse.equinox.launcher_*.jar" | sort | tail -1);`
`$vm -cp $cp org.eclipse.equinox.launcher.Main -data $workspace \`
`  -consolelog -clean -debug -console -noexit \`
`  -vmargs -Xms128M -Xmx256M -XX:PermSize=128M -XX:MaxPermSize=256M`

`popd >/dev/null`

# Admin UI Eclipse Perspective

![P2-admin-ui-in-Ganymede.png](P2-admin-ui-in-Ganymede.png
"P2-admin-ui-in-Ganymede.png") To run the [Equinox p2 Admin
UI](Equinox_p2_Admin_UI_Users_Guide "wikilink") from within a full
Eclipse install, you need to install some extra plugins into Eclipse, or
install Eclipse into the p2 agent. The latter is [documented
here](Equinox_p2_Admin_UI_Users_Guide#Overview_of_the_Eclipse_Provisioning_RCP_Agent "wikilink").
The former can be done like this:

`#!/bin/bash`
`vm=/opt/sun-java2-5.0/bin/java`
`eclipsehome=~/eclipse/p2_eclipse;`
`workspace=$eclipsehome/workspace`

`pushd $eclipsehome >/dev/null`
`if [[$#_-eq_0|$# -eq 0]]; then`
`       rm -fr $eclipsehome/eclipse $workspace;`
`       mkdir -p $eclipsehome/eclipse;`
`       eclipse=eclipse-SDK-3.4-linux-gtk.tar.gz`
`       echo "Unpack $eclipse...";`
`       tar xzf $eclipse`
`       p2=equinox-p2-agent-3.4-linux.tar.gz`
`       echo "Unpack $p2 into dropins"`
`       mkdir -p $eclipsehome/eclipse/dropins/p2`
`       tar xzf $p2 -C $eclipsehome/eclipse/dropins/p2`
`       rm -fr $eclipsehome/eclipse/dropins/p2/{dropins,libcairo-swt.so,p2,artifacts.xml,configuration,eclipse,eclipse.ini}`
`fi`
`cp=$(find $eclipsehome -name "org.eclipse.equinox.launcher_*.jar" | sort | tail -1);`
`$vm -cp $cp org.eclipse.equinox.launcher.Main -data $workspace \`
` -consolelog -clean -debug -console -noexit \`
` -vmargs -Xms128M -Xmx256M -XX:PermSize=128M -XX:MaxPermSize=256M`

`popd >/dev/null`

Once started, open the Provisioning perspective.

-----

If you want to do the same with an Ant or Cmd/Bat script, see [Starting
Eclipse Commandline With Equinox
Launcher](Starting_Eclipse_Commandline_With_Equinox_Launcher "wikilink")
for how to write an equivalent script.

# See Also

  - [Equinox p2 Admin UI Users
    Guide](Equinox_p2_Admin_UI_Users_Guide "wikilink")
  - [Equinox p2 Getting Started Admin
    UI](Equinox_p2_Getting_Started_Admin_UI "wikilink")
  - [:Category:Equinox p2](:Category:Equinox_p2 "wikilink")

[Category:Releng](Category:Releng "wikilink")
[Category:Equinox](Category:Equinox "wikilink")
[Category:Equinox_p2](Category:Equinox_p2 "wikilink")
[Category:Java](Category:Java "wikilink")
[Category:Launcher](Category:Launcher "wikilink")