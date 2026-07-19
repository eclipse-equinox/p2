The following examples show how to install some feature.group (eg.,
org.eclipse.gef.all.feature.group) into the default eclipse/ folder
using the built-in SDKProfile profile.

This matches the [p2 Update
UI](Equinox_p2_Update_UI_Users_Guide "wikilink")'s default behaviour.

## Ant script

Use p2.director to install from a zipped p2 repo (Update zip) into the
Eclipse install used to run headless JUnit tests. This zip contains p2
metadata + feature jars + packed plugin jars ().

The Master zip contains no p2 metadata but has both jars and jar.pack.gz
bundles. Using both, the director need not worry about unpacking the
packed jars ().

<target name="install">
`  `<property name="p2dirTmp" value="${buildDir}/testing/p2dirTmp" />
`  `<mkdir dir="${p2dirTmp}/eclipse"/>
`  `<unzip src="${buildDirectory}/${buildLabel}/${MasterZip}" dest="${p2dirTmp}" overwrite="true" />
`  `<unzip src="${buildDirectory}/${buildLabel}/${updateZip}" dest="${p2dirTmp}/eclipse" overwrite="false"/>
`  `<echo>`Install ${mainFeatureToBuildID} feature to ${install}`</echo>
`  `<ant target="run.director" antfile="${helper}">
`    `<property name="p2.director.installIU" value="${mainFeatureToBuildID}.feature.group" />
`    `<property name="p2.director.install.path" value="${install}/eclipse" />
`    `<property name="p2.director.destination" value="${install}/eclipse" />
`    `<property name="p2.director.input.repo" value="${p2dirTmp}/eclipse" />
`  `</ant>
</target>

<target name="run.director">
`  `<property name="p2.director.extraArgs" value="" />
`  `<property name="p2.director.installIU" value="${mainFeatureToBuildID}.feature.group" />
`  `<property name="p2.director.install.path" value="${install}/eclipse" />
`  `<property name="p2.director.destination" value="${install}/eclipse" />
`  `<property name="p2.director.input.repo" value="${p2dirTmp}/eclipse" />
`  `<echo>
`p2.director.destination  = ${p2.director.destination}`
`p2.director.input.repo   = ${p2.director.input.repo}`
`p2.director.install.path = ${p2.director.install.path}`
</echo>
`  `<mkdir dir="${p2.director.install.path}" />
`  `
`  `<chmod perm="ugo+rwx" file="${p2.director.destination}/eclipse" />
`  `<exec executable="${p2.director.destination}/eclipse" failonerror="true" dir="${p2.director.destination}" timeout="900000" taskname="p2.dir">
`    `<arg line=" -vm ${JAVA_HOME}/bin/java" />
`    `<arg line=" -application org.eclipse.equinox.p2.director.app.application" />
`    `<arg line=" -nosplash" />
`    `<arg line=" --launcher.suppressErrors" />
`    `<arg line=" -consoleLog" />
`    `<arg line=" -flavor tooling" />
`    `
`    `<arg line=" -roaming" />
`    `<arg line=" -profile SDKProfile" />
`    `
`    `<arg line=" -destination ${p2.director.install.path}" />
`    `<arg line=" -bundlepool ${p2.director.destination}" />
`    `<arg line=" -installIU ${p2.director.installIU}" />
`    `<arg line=" -metadataRepository file:${p2.director.input.repo},file:${p2.director.destination}/p2/org.eclipse.equinox.p2.engine/profileRegistry/SDKProfile.profile"/>
`    `<arg line=" -artifactRepository file:${p2.director.input.repo},file:${p2.director.destination}" />
`    `<arg line=" -profileProperties org.eclipse.update.install.features=true" />
`    `<arg line=" -vmargs" />
`    `<arg line=" -Declipse.p2.data.area=${p2.director.destination}/p2" />
`    `<arg line=" -Declipse.p2.MD5Check=false" />
`  `</exec>
</target>

## Bash script

Use p2.director to install the GMF SDK feature and all its prereqs from
the Ganymede Update site into eclipse/ using default SDKProfile.

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
      -profile SDKProfile
      -profileProperties org.eclipse.update.install.features=true \
      -p2.os linux -p2.ws gtk -p2.arch x86 \
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

## See Also

  - [Starting Eclipse Commandline With Equinox
    Launcher](Starting_Eclipse_Commandline_With_Equinox_Launcher "wikilink").

[Director](Category:Equinox_p2 "wikilink")
[Category:Equinox_p2_Director](Category:Equinox_p2_Director "wikilink")