The p2 metadata generator is a utility tool that generates metadata and
artifact repositories from a given input. The generator is available
both as an Eclipse application, and as an Ant task.

**Note: The Metadata Generator is deprecated, use the
[Publisher](Equinox/p2/Publisher "wikilink").**

# Generator Application

The generator application is contained in bundle
org.eclipse.equinox.p2.metadata.generator. This bundle is part of the
Eclipse SDK, and is also available in the p2 [Admin
UI](Equinox_p2_Getting_Started_Admin_UI "wikilink"). The generator can
be invoked using the generic Eclipse launcher as follows:

eclipse -application
org.eclipse.equinox.p2.metadata.generator.EclipseGenerator
<generatorArgs>

The generator application has three primary modes of operation:

1.  Generating metadata for a folder containing bundles and features
    (-source argument)
2.  Generating metadata for a traditional Eclipse update site
    (-updateSite argument)
3.  Generating metadata for an existing Eclipse application that does
    not contain p2 metadata (-config argument)

# Generating metadata from an update manager site

The following example shows how to generate metadata from an
pre-existing update site located in "d:\\ganymedeM5". <tt>

`   -application org.eclipse.equinox.p2.metadata.generator.EclipseGenerator`
`   -updateSite d:/ganymedeM5/`
`   -site `<file:d:/ganymedeM5/site.xml>
`   -metadataRepository `<file:d:/ganymedeM5/>
`   -metadataRepositoryName "Ganymede Update Site"`
`   -artifactRepository `<file:d:/ganymedeM5/>
`   -artifactRepositoryName "Ganymede Artifacts"`
`   -compress`
`   -append`
`   -reusePack200Files`
`   -noDefaultIUs`
`   -vmargs -Xmx256m`

</tt>

[Here's another
example](Equinox_p2_Metadata_Generator/Example "wikilink").

Want to simplify that down to a shell script with a couple of inputs --
site name ("Ganymede") and site path ("d:/ganymedeM5/")? Try [this
script](http://dev.eclipse.org/viewcvs/index.cgi/releng-common/tools/scripts/buildUpdateSiteMetadata.sh?root=Modeling_Project&content-type=text%2Fplain&view=co).
To run this you need either Eclipse, [the
p2-agent](http://download.eclipse.org/eclipse/equinox/drops/S-3.4M7-200805020100/index.php#Launchers),
or [releng.basebuilder from tag M7_34 or
later](Platform-releng-basebuilder#May_5.2C_2008 "wikilink").

You can also run this from the <java> task in Ant:

```
 <java jar="@{launcherjar}" fork="true" timeout="10800000" taskname="p2"
   jvm="${java.home}/bin/java" failonerror="false" maxmemory="256m">
   <classpath>
     <fileset dir="${builder.build.path}/plugins"
       includes="org.eclipse.equinox.launcher_*.jar,
         org.eclipse.equinox.p2.metadata.generator_*.jar"/>
     <pathelement location="${builder.build.path}/plugins" />
   </classpath>
   <arg line=" org.eclipse.equinox.launcher.Main" />
   <arg line=" -application org.eclipse.equinox.p2.metadata.generator.EclipseGenerator" />
   <arg line=" -updateSite ${updateSiteJarDir}/ -site file:${updateSiteJarDir}/site.xml" />
   <arg line=" -metadataRepository file:${updateSiteJarDir}/ -metadataRepositoryName &quot;My Updates&quot;" />
   <arg line=" -artifactRepository file:${updateSiteJarDir}/ -artifactRepositoryName &quot;My Artifacts&quot;" />
   <arg line=" -noDefaultIUs -compress -reusePack200Files" />
 </java>
```

Be sure to give the JVM enough heap space (-Xmx argument), since
insufficient heap space can lead it to delete your valuable plugins\!
See .

## Arguments describing the input

  - \-source <path> :the path to a folder containing plugins and folders
    to generate p2 metadata for
    \-updateSite <path> :the path of a traditional update site to
    generate p2 metadata for
    \-config <path> :the path of an Eclipse application that is not
    p2-enabled
    \-exe <path> :the location of the application launcher executable
    \-launcherConfig <path> :the path of the launcher configuration file
    (eclipse.ini)
    \-features <path> :the location of features to generate metadata
    for
    \-bundles <path> : the location of bundles to generate metadata
    for
    \-base <path> :a base location containing a plugins/ and features/
    directory
    \-p2.os :the operating system of the application metadata is being
    generated for
    \-site <path> :the URL of a site.xml file used to generator
    categories

## Arguments describing the output

  - \-metadataRepository :the URL to a writable metadata repository that
    will contain the produced installable units
    \-metadataRepositoryName :a user friendly name for the metadata
    repository
    \-artifactRepository :the URL to a writable artifact repository that
    will contain the produced artifacts
    \-artifactRepositoryName :a user friendly name for the artifact
    repository
    \-publishArtifacts :flag indicating whether the artifacts should be
    published to the repository. When this flag is not set, the actual
    bytes underlying the artifact will not be copied, but the repository
    index will be created. When this option is not specified, it is
    recommended to set the artifactRepository to be in the same location
    as the source (-source)
    \-publishArtifactRepository
    \-append :flag indicating that repositories will be appended to
    \-root :The name of the installable unit referring to all the IUs
    that have been added to the repository during the run
    \-rootVersion :The version of the root installable unit
    \-flavor :the flavor associated with the configuration units
    generated. (This will be removed for 1.0)
    \-inplace :causes the metadata and artifact repositories to be
    written into the source location
    \-noDefaultIUs :flag to indicate the default configuration units
    should not be created
    \-compress :cause the repositories to store their index in
    compressed form
    \-reusePack200Files :Specifying -reusePack200 does not require you
    to have pack200 files on the server, nor does it cause pack200 files
    to be created. When this option is specified, the generator looks
    for pack.gz files and if available it creates an entry for them in
    the artifacts.jar.

# Generator Ant Task

The generator Ant task is called "p2.generator". This task is also
available in the org.eclipse.equinox.p2.metadata.generator bundle.

[Metadata generator](Category:Equinox_p2 "wikilink")