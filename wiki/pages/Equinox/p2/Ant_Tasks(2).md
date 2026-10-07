The new format is available in builds after April 26, 2009 (Eclipse
3.5M7+). This wiki may be out of date. See also:

  - <http://help.eclipse.org/juno/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_repositorytasks.htm>
  - <http://help.eclipse.org/juno/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_mirror.html>

## Mirror Task

The p2.mirror task is used to copy the contents of one repository to
another. The location of the destination repository must be modifiable,
and if a destination repository of a type is defined then a source for
that type must also be present

''Note that in the examples in this section the tasks contain a

</source>

tag. This tag should not contain a space - it is present due to a
limitation in the wiki software.''



### Simple Examples

A task to mirror only the contents of a metadata repository at a given
location:

``` xml
  <p2.mirror>
   <repository location="file:/myDestination" name="A new repository" kind="M" />
   <source>
     <repository location="http://aSource/" kind="M" />
   < /source>
  </p2.mirror>
```

The task to mirror the artifact contents is very similar changing only
the 'kind' attribute of the repositories:

``` xml
<p2.mirror>
  <repository location="file:/myDestination" name="A new repository" kind="A" />
  <source>
    <repository location="http://aSource/" kind="A" />
  < /source>
</p2.mirror>
```

A co-located repository can be specified by omitting the kind attribute
on the repository, in this example the contents of both the artifact and
metadata repositories at the location will be mirrored:

``` xml
<p2.mirror>
  <repository location="file:/myDestination" name="A new repository" />
  <source>
    <repository location="http://aSource/" />
  < /source>
</p2.mirror>
```

The repositories in the tasks can be a mixture specified kinds and
co-located.



### Multiple Repositories

The source or destination metadata and artifact repositories need not be
co-located, although the tasks are limited to at most one artifact and
one metadata destination repository.

``` xml
<p2.mirror>
  <repository location="file:/myArtifactDestination" name="A new repository" kind="A" />
  <repository location="file:/myMetaDestination" name="A new repository" kind="M" />
  <source>
    <repository location="http://aSource/" />
  < /source>
</p2.mirror>
```

Multiple source locations can also be defined:

``` xml
<p2.mirror>
  <repository location="file:/myArtifactDestination" name="A new repository" kind="A" />
  <repository location="file:/myMetaDestination" name="A new repository" kind="M" />
  <source>
    <repository location="http://aSource2/" />
    <repository location="http://aSourceMeta/" kind="M" />
    <repository location="http://aSourceArtifact/" kind="A" />
  < /source>
</p2.mirror>
```



### Fileset Repositories

The source repositories can also be defined through a FileSet (see
Apache Ant documentation for defining a fileset), though if the
location(s) in the filesetshould be a specific kind of repository that
should also be specified by again using the kind attribute:

``` xml
<p2.mirror>
  <repository location="file:/myDestination" name="A new repository" />
  <source>
    <repository location="http://aSource/" />
    <fileset kind="m" .... />
  < /source>
  <iu id="tooling.osgi.bundle.default" version="1.0.0" />
  <slicingoptions platformfilter="win32,win32,x86" />
</p2.mirror>
```



### Repository Format

By default if the destination repositories already exist then the new
data is appended, this can be prevented by adding the append attribute
with the value false:

``` xml
<p2.mirror>
  <repository location="file:/myArtifactDestination" name="A new repository" kind="A" append="false" />
  <repository location="file:/myMetaDestination" name="A new repository" kind="M" append="false" />
  <source>
    <repository location="http://aSource/" />
  < /source>
</p2.mirror>
```

To create a new repository using the properties of an existing
repository the format attribute is added to the repository element. This
can be used to prevent storing pack200'd artifacts in the .blobstore,
preserving them as siblings (if they were siblings in the stated site
from which to pull the format).

``` xml
<p2.mirror>
  <repository location="file:/myArtifactDestination" name="A new repository" kind="A" format="http://somerepo/" />
  <repository location="file:/myMetaDestination" name="A new repository" kind="M" append="false" />
  <source>
    <repository location="http://aSource/" />
  < /source>
</p2.mirror>
```



### Partial Mirroring

Its also possible to partially mirror a repository, the simplest method
is to specify an individual IU which will result in the default slicing
options being used. If an artifact destination repository is defined
then the artifacts required by the IUs will also be mirrored.

``` xml
<p2.mirror>
  <repository location="file:/myDestination" name="A new repository" />
  <source>
    <repository location="http://aSource/" />
  < /source>
  <iu id="tooling.osgi.bundle.default" version="1.0.0" />
</p2.mirror>
```

If only windows IUs and artifacts were required then a slicingOptions
element needs to be defined with a platform filter:

``` xml
<p2.mirror>
  <repository location="file:/myDestination" name="A new repository" />
  <source>
    <repository location="http://aSource/" />
  < /source>
  <iu id="tooling.osgi.bundle.default" version="1.0.0" />
  <slicingoptions platformfilter="win32,win32,x86" />
</p2.mirror>
```

For a working example of partial mirroring, see [Equinox/p2/Ant
Tasks/Partial
Mirroring/Example](Equinox/p2/Ant_Tasks/Partial_Mirroring/Example "wikilink").

### Comparison

There are several comparators (eg. MD5, JarComparator) available which
can be used to compare the contents of a repository, when these are used
the artifact must either be present in the destination repository, or a
baseline repository must be defined.

``` xml
<p2.mirror>
  <repository location="file:/myDestination" name="A new repository" />
  <source>
    <repository location="http://aSource/" />
  < /source>
  <comparator comparator="org.eclipse.equinox.artifact.md5.comparator" comparatorLog="/home/user/myFile.xml">
    <repository location="http://baseline/" />
  </comparator>
</p2.mirror>
```

## Composite Repository Task

The p2.composite.repository task can be used to create or to modify the
child repos of a composite repository

### Create

To create a composite repository and add a single child:

``` xml
<p2.composite.repository>
  <repository location="file:/myDestination" name="A new repository" kind="M" />
  <add>
    <repository location="http://aSource/" kind="M" />
  </add>
</p2.composite.repository>
```

The task can be told to fail if a repository already exists at the
location to prevent accidental modification by adding the failOnExists
attribute

``` xml
<p2.composite.repository failOnExists="true">
  <repository location="file:/myDestination" name="A new repository" kind="M" />
  <add>
    <repository location="http://aSource/" kind="M" />
  </add>
</p2.composite.repository>
```

As with the mirror task it is possible to specify a mixture of
repositories, in this particular example a co-located repository will be
created with the appropriate children added:

``` xml
<p2.composite.repository failOnExists="true">
  <repository location="file:/myDestination" name="A new repository" />
  <add>
    <repository location="http://aSource/" kind="M" />
    <repository location="http://aSource2/" kind="A" />
  </add>
</p2.composite.repository>
```

### Modify

The syntax to add a child to an existing repository is the same as
creating a new one:

``` xml
<p2.composite.repository>
  <repository location="file:/myDestination" name="A new repository" />
  <add>
    <repository location="http://aSource/" kind="M" />
    <repository location="http://aSource2/" kind="A" />
  </add>
</p2.composite.repository>
```

Children can also be removed from a composite repository:

``` xml
<p2.composite.repository>
  <repository location="file:/myDestination" name="A new repository" />
  <remove>
    <repository location="http://aSource/" kind="M" />
    <repository location="http://aSource2/" kind="A" />
  </remove>
</p2.composite.repository>
```

Pre-existing children can be removed from the repository prior to adding
new children by using the append attribute:

``` xml
<p2.composite.repository>
  <repository location="file:/myDestination" name="A new repository" append="false" />
  <add>
    <repository location="http://aSource/" kind="M" />
    <repository location="http://aSource2/" kind="A" />
  </add>
</p2.composite.repository>
```

### Validate

To ensure that the composite repository has consistent contents an
artifact comparator can be used

``` xml
<p2.composite.repository validate="org.eclipse.equinox.artifact.md5.comparator">
  <repository location="file:/myDestination" name="A new repository" append="false" />
  <add>
    <repository location="http://aSource/" kind="M" />
    <repository location="http://aSource2/" kind="A" />
  </add>
</p2.composite.repository>
```

## Repo2Runnable

Ant task which calls the "repo to runnable" application. This
application takes an existing p2 repository (local or remote), iterates
over its list of IUs, and fetches all of the corresponding artifacts to
a user-specified location. Once fetched, the artifacts will be in
"runnable" form... that is directory-based bundles will be extracted
into folders and packed JAR files will be un-packed.

``` xml
<p2.repo2runnable>
  <repository location="file:/myDestination" />
  <source>
    <repository location="http://mySource" />
  < /source>
</p2.repo2runnable>
```

[Here's a more complete Ant script
example](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.dash/athena/org.eclipse.dash.commonbuilder/org.eclipse.dash.common.releng/tools/scripts/p2repoFormatToRunnableSDKFormat.xml?root=Technology_Project&view=markup),
which converts an update zip (repo) to SDK zip (runnable).

Or, use this task via commandline. This example will fetch all the
features and plugins from the listed update sites / zips and dump those
files in "runnable" format into `/tmp/unpacked`.

`./eclipse -nosplash -consolelog -application org.eclipse.equinox.p2.repository.repo2runnable \`
`   -source `<jar:file:/home/nboldt/eclipse/35clean/GEF-Update-3.5.0RC2.zip\!/>` \`
`   -source GEF-Update-N200905281802/`
`   -source emf-sdo-xsd-Update-2.4.2.zip`
`   -source `<http://download.eclipse.org/tools/ve/updates/1.4>
`   -destination /tmp/unpacked`

Works with:

  - local or remote p2 repo or update site
  - local **archived** p2 repo or update site (zip) - ***remote archived
    site not supported***
  - absolute (jar:file:, file:, http:) or relative paths
  - sites and zips containing pack200'd jars

See also
[bug 277504](https://bugs.eclipse.org/bugs/show_bug.cgi?id=277504#c16).

## Common Task Elements

### Repositories

The input format used by repositories in p2 Ant tasks:

``` xml
<repository location="file:///Users/Pascal/Downloads/builds/transfer/files/updates/3.5-I-builds/I20090203-1200" append="true" compressed="true"
    format="file:///Users/Pascal/Downloads/builds/transfer/files/updates/3.5-I-builds/I20090203-1200" kind="metadata" name="" remove="false" />
```

In general the location is always required, other attributes may not be
used by individual tasks.

| Attribute  | Type                     | Default | Description                                                                                                                                                                                                                    |
| ---------- | ------------------------ | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| location   | URI                      | null    | The location of the repository                                                                                                                                                                                                 |
| append     | boolean                  | true    | Determines if the task should append to an existing repository or empty it first                                                                                                                                               |
| compressed | boolean                  | true    | Determines if the repository should be compressed                                                                                                                                                                              |
| format     | URI                      | null    | Location of a repository to copy format from. This can be used to prevent storing pack200'd artifacts in the .blobstore, preserving them as siblings (if they were siblings in the stated site from which to pull the format). |
| kind       | "metadata" or "artifact" | Both    | Specifies the type of the repository. Default is to add both a metadata and artifact repository                                                                                                                                |
| name       | String                   | null    | The name of the repository                                                                                                                                                                                                     |
| remove     | boolean                  | false   | Defines if the repository be removed                                                                                                                                                                                           |

### Installable Units

``` xml
<iu id="tooling.osgi.bundle.default" version="1.0.0" />
```

| Attribute | Type    | Default | Description                        |
| --------- | ------- | ------- | ---------------------------------- |
| id        | String  | none    | The InstallableUnit identifier     |
| version   | Version | none    | The version of the InstallableUnit |

### SlicingOptions

``` xml
  <slicingoptions followOnlyFilteredRequirements="true" followStrict="true" includeFeatures="false"
      includeNonGreedy="false" includeOptional="true" platformfilter="win32,win32,x86" latestVersionOnly="false" />
```

| Attribute                      | Type                 | Default | Description                                                                                                                                                                                                                                                                    |
| ------------------------------ | -------------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| filter                         | comma separated list | none    | A comma separated list of filter options for the slicer                                                                                                                                                                                                                        |
| followOnlyFilteredRequirements | boolean              | false   |                                                                                                                                                                                                                                                                                |
| followStrict                   | boolean              | false   | This influences (among other things?) if the dependencies of features are part of the mirror operation. A value of true will exclude these dependencies. With dependencies the plug-ins and features are meant that are referenced in the "requires" element of a feature.xml. |
| includeFeatures                | boolean              | true    | Include features (org.eclipse.update.install.features)                                                                                                                                                                                                                         |
| includeNonGreedy               | boolean              | true    |                                                                                                                                                                                                                                                                                |
| includeOptional                | boolean              | true    |                                                                                                                                                                                                                                                                                |
| platformFilter                 | os,ws,arch           | none    | Filter based on the platform                                                                                                                                                                                                                                                   |
| latestVersionOnly              | boolean              | false   | Set to "true" to filter the resulting set of IUs to only included the latest version of each Installable Unit. By default, all versions satisfying dependencies are included.                                                                                                  |

[Ant Tasks](Category:Equinox_p2 "wikilink")