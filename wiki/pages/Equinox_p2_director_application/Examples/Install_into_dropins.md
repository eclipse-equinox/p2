To install the GMF SDK and all its prereqs from the Ganymede Update site
directly into the
[dropins](Equinox_p2_Getting_Started#Dropins "wikilink") folder, run
this:

    <nowiki>
    #!/bin/bash
    ############### start of configuration block ###############
    workspace=/tmp/workspace-clean-34
    eclipseroot=~/eclipse/34clean/p2director
    installTargetFeatureGroup=org.eclipse.gmf.sdk.feature.group
    bundlepoolDir=$eclipseroot/eclipse/dropins/${installTargetFeatureGroup}/eclipse
    arc=`uname -m`
    eclipseFancyName=helios
    metadataRepository=http://download.eclipse.org/releases/$eclipseFancyName
    artifactRepository=$metadataRepository

    #eclipseTarGz=eclipse-SDK-3.4-linux-gtk.tar.gz
    # this one isn't the smallest for testing though:
    # http://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/heliossr1
    eclipseTarGz=/tmp/eclipse-jee-helios-SR1-linux-gtk-x86_64.tar.gz

    #vm=/opt/sun-java2-5.0/bin/java
    #vm=/opt/ibm-java2-5.0/bin/java

    # let's use default Java (explicit path)
    vm=`which java`

    ############## end of configuration block ###############


    pushd $eclipseroot >/dev/null
    if [[$#_-eq_0|$# -eq 0]]; then
        rm -fr eclipse $workspace $bundlepoolDir
        echo "[`date +%H:%M:%S`] Unpack $eclipseTarGz ...";
        tar xzf $eclipseTarGz
    fi


    echo ""
    echo "Using:       vm=$vm and workspace=$workspace";
    echo "Installing:  ${installTargetFeatureGroup}";
    echo "Destination: $bundlepoolDir";
    echo ""

    echo "[`date +%H:%M:%S`] Running p2.director ... ";
    #  -console -noexit -debug
    ./eclipse/eclipse -vm $vm -nosplash \
      -data $workspace -consolelog -clean \
      -application org.eclipse.equinox.p2.director \
      -metadataRepository $metadataRepository \
      -artifactRepository $artifactRepository \
      -installIU ${installTargetFeatureGroup} \
      -destination $eclipseroot \
      -bundlepool $bundlepoolDir \
      -profile ${installTargetFeatureGroup}.profile \
      -profileProperties org.eclipse.update.install.features=true \
      -p2.os linux -p2.ws gtk -p2.arch $arc \
      -vmargs \
        -Declipse.p2.data.area=$eclipseroot/eclipse/p2 \
        -Xms128M -Xmx256M -XX:PermSize=128M -XX:MaxPermSize=256M
    echo "Installed sizes: "; du -shc $eclipseroot/eclipse $bundlepoolDir

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