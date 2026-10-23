To install the GMF SDK and all its prereqs from the Ganymede Update site
into any folder then link to it using a .link file in the
[dropins](Equinox_p2_Getting_Started#Dropins "wikilink") folder, run
this:

    <nowiki>
    #!/bin/bash
    workspace=/tmp/workspace-clean-34
    eclipseroot=~/eclipse/34clean/p2director
    installTargetFeatureGroup=org.eclipse.gmf.sdk.feature.group
    bundlepoolDir=$eclipseroot/${installTargetFeatureGroup}/eclipse

    pushd $eclipseroot >/dev/null
    if [[$#_-eq_0|$# -eq 0]]; then
        rm -fr eclipse $workspace $bundlepoolDir
        eclipse=eclipse-SDK-3.4-linux-gtk.tar.gz
        echo "[`date +%H:%M:%S`] Unpack $eclipse ...";
        tar xzf $eclipse
    fi

    vm=/opt/sun-java2-5.0/bin/java
    #vm=/opt/ibm-java2-5.0/bin/java

    echo ""
    echo "Using:       vm=$vm and workspace=$workspace";
    echo "Installing:  ${installTargetFeatureGroup}";
    echo "Destination: $bundlepoolDir";
    echo ""

    echo "[`date +%H:%M:%S`] Running p2.director ... ";
    #  -console -noexit -debug
    ./eclipse/eclipse -vm $vm -nosplash \
      -data $workspace -consolelog -clean \
      -application org.eclipse.equinox.p2.director.app.application \
      -metadataRepository http://download.eclipse.org/releases/ganymede \
      -artifactRepository http://download.eclipse.org/releases/ganymede \
      -installIU ${installTargetFeatureGroup} \
      -destination $eclipseroot \
      -bundlepool $bundlepoolDir \
      -profile ${installTargetFeatureGroup}.profile \
      -profileProperties org.eclipse.update.install.features=true \
      -p2.os linux -p2.ws gtk -p2.arch x86 \
      -vmargs \
        -Declipse.p2.data.area=$eclipseroot/eclipse/p2 \
        -Xms128M -Xmx256M -XX:PermSize=128M -XX:MaxPermSize=256M
    echo "Installed sizes: "; du -shc $eclipseroot/eclipse $bundlepoolDir

    echo ""
    echo "[`date +%H:%M:%S`] Link ${bundlepoolDir} from dropins/"
    echo "path=${bundlepoolDir}" > $eclipseroot/eclipse/dropins/${installTargetFeatureGroup}.link

    echo "";
    echo -n "[`date +%H:%M:%S`] Starting Eclipse workbench (to verify installation) ...";
    #  -console -noexit -debug
    ./eclipse/eclipse -vm $vm \
      -data $workspace -consolelog -clean \
      -vmargs \
        -Xms128M -Xmx256M -XX:PermSize=128M -XX:MaxPermSize=256M

    popd >/dev/null
    echo " done."
    </nowiki>

If you need to translate this to Windows command syntax or wrap it with
an Ant script, see [Starting Eclipse Commandline With Equinox
Launcher](Starting_Eclipse_Commandline_With_Equinox_Launcher "wikilink").

[Director](Category:Equinox_p2 "wikilink")
[Category:Equinox_p2_Director](Category:Equinox_p2_Director "wikilink")