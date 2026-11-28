### Workspace Setup

The build has been integrated into the Eclipse SDK builds so the build
scripts are located in the Eclipse SDK Releng team's eclipsebuilder
project. We will modify a few of the property values before we are able
to run the build. Check out the project from CVS with the following
information:

    server: dev.eclipse.org
    repository: /cvsroot/eclipse
    project: org.eclipse.releng.eclipsebuilder

Next create a new file
`org.eclipse.releng.eclipse.builder/equinox/buildConfigs/equinox.prov/local_run.xml`
with the following contents:

``` xml
<project name="local run" default="run">
   <target name="run">
      <tstamp>
         <format property="buildId" pattern="yyyyMMdd-HHmm" />
      </tstamp>
      <property name="postingDirectory" value="/home/username" />
      <property name="p2.root" value="${postingDirectory}" />
      <property name="p2.output.base" value="${p2.root}/equinox.p2.build" />
      <property name="equinoxPostingDirectory" value="${p2.output.base}" />
      <property name="java15-home" value="/path/to/java/sun_1.5.0-sr11/jre" />
      <property name="buildLabel" value="${buildId}" />

      <property name="build.timestamp" value="I20071031-0800" />

      <property name="sdk.archive" value="/path/to/eclipse-SDK-${build.timestamp}-linux-gtk.tar.gz" />
      <property name="rcp.archive" value="/path/to/eclipse-RCP-${build.timestamp}-linux-gtk.tar.gz" />
      <property name="rcp.delta.archive" value="/path/to/eclipse-RCP-${build.timestamp}-delta-pack.zip" />
      <property name="releng.tools.archive" value="/path/to/org.eclipse.releng.tools-${build.timestamp}.zip" />

      <property name="updateSite" value="/path/to/update/site" />
      <mkdir dir="${updateSite}" />

      <ant antfile="run.xml"/>

      <property name="p2.result" value="/path/to/result/dir" />
      <property name="p2.result.builds" value="${p2.result}/builds/${buildLabel}" />
      <mkdir dir="${p2.result.builds}" />
      <copy todir="${p2.result.builds}">
         <fileset dir="${p2.output.base}/${buildId}">
            <include name="**/*" />
         </fileset>
      </copy>
   </target>
</project>
```

Make sure you download the appropriate files and change all the paths in
the file you just created\!

Next you will create a new Eclipse Application and run the AntRunner and
point it to your new file:

1.  Run Settings...
2.  create new Eclipse Application
3.  change the application to be `org.eclipse.ant.core.antRunner`
4.  add the build file to the command-line arguments: `-buildfile
    ${resource_loc:/org.eclipse.releng.eclipsebuilder/equinox/buildConfigs/equinox.prov/local_run.xml}`

*Note:* you must have the CVS executable available from the command-line
in order for the checkout to work.

Then run your new launch configuration to build.

### Output

  - The Agent zip will be in
    *${prov.output.base}/equinox-prov-agent-<timestamp>-win32.zip*
  - The Metadata and Artifact repositories will be in
    *${prov.output.base}/servers*

### What Happens

1.  build features for the director, metadata generator, and
    self-hosting bundles
2.  build the agent from a product
3.  generate the metadata for the agent
4.  run the director and install the agent from the generated metadata
    and artifacts
5.  zip up the agent install
6.  generate the metadata for the self-hosting bundles and Eclipse SDK

### TODO

  - integrate with the regular Platform builds
  - write code to determine the output file of the build
  - should enhance so instead of building several different things, we
    should build one big feature and then use the packager to put
    together the things that we need

[Building](Category:Equinox_p2 "wikilink")