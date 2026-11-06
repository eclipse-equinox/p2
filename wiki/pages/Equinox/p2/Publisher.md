The Publisher is the means by which deployable entities get added to
repositories. It consists of an extensible set of publishing actions,
applications and Ant tasks that allow users to generate p2 repositories
from a number of different sources.

## Publisher Actions

The publisher applications and Ant tasks each execute a set of publisher
actions. This section describes the behaviour of some of these actions.

### BundlesAction

The `BundlesAction` generates the p2 metadata for OSGi bundles.

**Optional runtime dependencies** (i.e. an `resolution:=optional`
directive in `Require-Bundle` or `ImportPackage` headers) are
interpreted in the following way:

  - In Indigo and earlier, an optional greedy dependency was generated,
    i.e. the optionally required bundle or package was installed
    whenever possible.
  - Since Juno M1, an optional non-greedy dependency is generated, i.e.
    the optional run-time dependency will not *cause* any additional
    installation. At runtime, the optional requirement will only be
    satisfied, if there was some other, mandatory requirement to the
    provider, e.g. through an already installed feature.

If the old behaviour is desired, i.e. an optional dependency shall be
satisfied during installation whenever possible, the dependency can be
annotated with an additional directive:
`resolution:=optional;x-installation:=greedy`.

Note: Optional greedy dependencies should be avoided, because they don't
really give the users of your bundle the choice whether they want the
optional content installed or not (see
[bug 247099](https://bugs.eclipse.org/bugs/show_bug.cgi?id=247099)).
Instead, you should offer additional features, e.g. "MyTool integration
with XYZ", which have mandatory requirements to the optional content and
hence cause the installation if the user chooses to install the feature.

### ProductAction

The `ProductAction` translates a PDE product configuration to an IU,
which, when installed with the [p2
director](Equinox/p2/Director_application "wikilink"), results in an
Eclipse or RCP application installation. By including various other
actions, the product action also generates a bunch of technical IUs
which tell p2 to create an Eclipse layout when installing.

**File format extensions**: The product action supports the following
extensions to the PDE product configuration format:

  - By setting the attribute `type="mixed"` in the product element, the
    product content is defined through both features and bundles listed
    in the product. (Introduced in Juno
    M1/[bug 325622](https://bugs.eclipse.org/bugs/show_bug.cgi?id=325622);
    official support in PDE requested in
    [bug 325614](https://bugs.eclipse.org/bugs/show_bug.cgi?id=325614))

## Headless Applications

The Publisher consists of a number of headless (no GUI) Eclipse
Applications that can be used to generate metadata from a variety of
sources. Examples of such applications include:

  - FeaturesAndBundles Publisher: Generates metadata from a set of
    features and bundles
  - Product Publisher: Generates metadata from a .product file
  - Category Publisher: Generates categories for an existing repository
  - UpdateSite Publisher: Generates metadata from an UpdateSite
  - Install Publisher: Generates metadata from an existing Eclipse
    install

### UpdateSite Publisher Application

The UpdateSite Publisher Application
(org.eclipse.equinox.p2.publisher.UpdateSitePublisher) is a headless
application that is capable of generating metadata (p2 repositories)
from an update site containing a site.xml, bundles and features. The
application can be invoked as follows:

<tt>

` java -jar `<targetProductFolder>`/plugins/org.eclipse.equinox.launcher_*.jar`
` -application org.eclipse.equinox.p2.publisher.UpdateSitePublisher`
` -metadataRepository `<file:/><some location>`/repository`
` -artifactRepository `<file:/><some location>`/repository`
` -source /<location with a site.xml>`
` -configs gtk.linux.x86`
` -compress`
` -publishArtifacts`

</tt>

### Features And Bundles Publisher Application

The Features and Bundles Publisher Application
(org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher) is a
headless application that is capable of generating metadata (p2
repositories) from pre-built Eclipse bundles and features. The
application can be invoked as follows:

<tt>

`   java -jar `<targetProductFolder>`/plugins/org.eclipse.equinox.launcher_*.jar`
`   -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher`
`   -metadataRepository `<file:/><some location>`/repository`
`   -artifactRepository `<file:/><some location>`/repository`
`   -source /`<location with a plugin and feature directory>
`   -configs gtk.linux.x86`
`   -compress`
`   -publishArtifacts`

</tt>

In this example, the plugins in
**/<location with a plugin and feature directory>/plugins** and features
in **/<location with a plugin and feature directory>/features** will be
published in the **<file:/><some location>/repository** repository. The
artifacts will also be published, and the repositories (artifacts.xml
and content.xml) compressed.

### Product Publisher

The Product Publisher Application
(org.eclipse.equinox.p2.publisher.ProductPublisher) is a headless
application that is capable of generating product configuration
metadata. The product publisher does not publish the bundles or features
that constitute the product. The application can be invoked as follows:

<tt>

`   -console -consolelog -application org.eclipse.equinox.p2.publisher.ProductPublisher`
`   -metadataRepository `<file:/home/irbull/Desktop/temp/mail1>
`   -artifactRepository `<file:/home/irbull/Desktop/temp/mail1>
`   -productFile /home/irbull/workspaces/p2/mail/mail.product`
`   -append`
`   -publishArtifacts`
`   -executables /home/irbull/eclipse/delta/eclipse/features/org.eclipse.equinox.executable_3.3.200.v20090426-1530-7M-Fm-FI3UouOdcoUJz-7oc`
`   -flavor tooling`
`   -configs gtk.linux.x86`
`   -pluginVersionsAdvice finalPluginVersions.properties`
`   -featureVersionsAdvice finalFeaturesVersions.properties`

</tt>

**Note:** There are currently a few oustanding (but workable) issues
with this application:

  - You must list any bundles to start on the configuration tab.
  - Any features / bundles should be specified with "Exact" versions (no
    qualifiers) or 0.0.0 for latest
  - The executables feature must be specified
  - Flavor must be specified (if unsure, use tooling)
  - Unchecking "Include native launchers" has no effect

**Note:** "-configs <spec>" specifies the environment properties,
supported by this product. If the product is not platform-specific,
"-configs ANY" or "-configs ANY.ANY.ANY" (all variants of the "ANY"
string literal with small or capital letters can be used) should be
passed as an option. One immediate effect in this case, for example, is
that the start configuration of bundles, specified in the product
definition, will be applied on provisioning on any platform.

### Category Publisher

The Category Publisher Application
(org.eclipse.equinox.p2.publisher.CategoryPublisher) is a headless
application that is capable of categorizing a set of Installable Units
in a given repository. The categorization is driven from a category
file. The application can be invoked as follows:

<tt>

`   -console -consolelog -application org.eclipse.equinox.p2.publisher.CategoryPublisher`
`   -metadataRepository `<file:/><repo location>`/repository`
`   -categoryDefinition `<file:/home/irbull/workspaces/p2/mail/category.xml>
`   -categoryQualifier`
`   -compress`

</tt>

This application will use the categories defined in category.xml to
categorize the metadata in **<file:/><repo location>/repository** with
the categories defined in **category.xml**. This command will compress
the repository. PDE offers an editor for creating the category.xml file;
See File -\> New -\> Plug-in Development -\> Category Definition.

### Parameters

Third column specifies which application support the parameter
(U=UpdateSite Publisher, FAB=FeaturesAndBundles Publisher, P=Product
Publisher, C=Category Publisher).

<table>
<tbody>
<tr class="odd">
<td><p>-pluginVersionsAdvice <file></p></td>
<td><p>specifies a file from which plugin version advice should be read</p></td>
<td><p>?</p></td>
</tr>
<tr class="even">
<td><p>-featureVersionsAdvice <file></p></td>
<td><p>specifies a file from which feature version advice should be read</p></td>
<td><p>?</p></td>
</tr>
<tr class="odd">
<td><p>-metadataRepository <url></p></td>
<td><p>location of the metadata repository to write</p></td>
<td><p><strong>all</strong></p></td>
</tr>
<tr class="even">
<td><p>-artifactRepository <url></p></td>
<td><p>location of the artifact repository to write</p></td>
<td><p><strong>all</strong></p></td>
</tr>
<tr class="odd">
<td><p>-source &lt;dir(?)&gt;</p></td>
<td><p>&lt;location with a site.xml (U) or with features and plugins dirs (FAB) &gt;</p></td>
<td><p>U</p></td>
</tr>
<tr class="even">
<td><p>-configs <specs></p></td>
<td><p>a triplet of environment properties for os, ws and arch, or ANY to support all platforms</p></td>
<td><p>U, FAB, P</p></td>
</tr>
<tr class="odd">
<td><p>-compress</p></td>
<td><p>create compressed jars rather than plain xml</p></td>
<td><p><strong>all</strong>?</p></td>
</tr>
<tr class="even">
<td><p>-publishArtifacts</p></td>
<td><p><em>option is unclear, artifacts are always published?</em></p></td>
<td><p>U, FAB, P ?</p></td>
</tr>
<tr class="odd">
<td><p>-append</p></td>
<td><p>append artifacts to an existing repository</p></td>
<td><p>?</p></td>
</tr>
<tr class="even">
<td><p>-executables</p>
<dir></td>
<td><p>The location of the executables feature. This is the feature that is used for branding and publishing the executable</p></td>
<td><p>P</p></td>
</tr>
<tr class="odd">
<td><p>-flavor <spec></p></td>
<td><p>"if unsure, use tooling"</p></td>
<td><p>P ?</p></td>
</tr>
<tr class="even">
<td><p>-categoryDefinition <url></p></td>
<td><p>specifies a file containing category definitions</p></td>
<td><p>C</p></td>
</tr>
<tr class="odd">
<td><p>-categoryQualifier</p></td>
<td><p>specifies the id of the category</p></td>
<td><p>C</p></td>
</tr>
<tr class="even">
<td><p>-contextMetadata</p></td>
<td><p>specifies context metadata (e.g. repository used as basis for category publisher)</p></td>
<td><p>C, ?</p></td>
</tr>
<tr class="odd">
<td><p>-reusePack200Files</p></td>
<td><p><strong>if</strong> the -publishArtifacts option is also set: include .pack.gz files</p></td>
<td><p>FAB, ??</p></td>
</tr>
</tbody>
</table>

## Ant Tasks

The publisher consists of two ant tasks for creating metadata. The first
ant task (p2.publish.featuresAndBundles) is used to create metadata from
pre-build bundles and features, while the second task
(p2.publish.product) is used to create metadata from a .product file.

### Default Attributes

<table cellspacing="1" cellpadding="2" width="95%" align="center">

<tr>

<td>

The `p2.publish.*` ant tasks outlined below all support the following
attributes:

<table border="5" cellspacing="0" cellpadding="1" width="95%" align="center">

<tr>

<td>

`metadataRepository`

</td>

<td>

A URL specifying the metadata repository to publish to.

</td>

</tr>

<tr>

<td>

`artifactRepository`

</td>

<td>

A URL specifying the artifact repository to publish to.

</td>

</tr>

<tr>

<td>

`repository`

</td>

<td>

Sets both metadataRepository and artifactRepository.

</td>

</tr>

<tr>

<td>

`metadataRepositoryName`

</td>

<td>

When creating a new metadata repository, sets the name.

</td>

</tr>

<tr>

<td>

`artifactRepositoryName`

</td>

<td>

When creating a new artifact repository, sets the name.

</td>

</tr>

<tr>

<td>

`repositoryName`

</td>

<td>

Sets both metadataRepositoryName and artifactRepositoryName.

</td>

</tr>

<tr>

<td>

`append`

</td>

<td>

Whether to append to the repository. (Default is "true")

</td>

</tr>

<tr>

<td>

`compress`

</td>

<td>

When creating a new repository, whether or not to compress the metadata.
(Default is "false")

</td>

</tr>

<tr>

<td>

`publishArtifacts`

</td>

<td>

Whether or not to publish the artifacts. (Default is "true")

</td>

</tr>

<tr>

<td>

`reusePackedFiles`

</td>

<td>

Whether or not to include discovered Pack200 files in the repository.
(Default is "false")

</td>

</tr>

<tr>

<td>

<contextRepository>

</td>

<td>

Nested elements specifying context repositories, supports the following
attributes:

<table cellspacing="0" cellpadding="2" border="1" width="100%">

<tr>

<td>

`location`

</td>

<td>

A URL specifying the location of the repository.

</td>

</tr>

<tr>

<td>

`artifact`

</td>

<td>

"true" or "false": whether or not there is an artifact repository at
this location.

</td>

</tr>

<tr>

<td>

`metadata`

</td>

<td>

"true" or "false": whether or not there is a metadata repository at this
location.

</td>

</tr>

</table>

If a given context repository contains metadata for one of the features
or bundles that are being published, then that metadata

`               will be re-used instead of generating new metadata.`

</td>

</tr>

</table>

</td>

</tr>

</table>

### Features and Bundles Publisher Task

The Features and Bundles Publisher Task (p2.publish.featuresAndBundles)
is an ant task that is capable of generating metadata (p2 repositories)
from pre-build Eclipse bundles and features. Here is an example of how
the ant task can be used:

``` xml
  <p2.publish.featuresAndBundles
    metadataRepository="file:/repository/location"
    artifactRepository="file:/repository/location"
    publishArtifacts="true"
    compress="true"
    source="/bundles/and/features/location/">
```

In addition to the default arguments, the feature and bundles task
supports the following:

<table border="5" cellspacing="0" cellpadding="1" width="95%" align="center">

<tr>

<td>

`source`

</td>

<td>

A folder containing plugins and features subfolders to publish.

</td>

</tr>

<tr>

<td>

<features>

</td>

<td>

A nested fileset element specifying the locations of binary features to
publish.

</td>

</tr>

<tr>

<td>

<bundles>

</td>

<td>

A nested fileset element specifying the locations of binary plug-ins to
publish.

</td>

</tr>

</table>

### Product Publisher Task

The Product Publisher Task (p2.publish.product) is an ant task that is
capable of generating product configuration metadata. The product
publisher does not publish the bundles or features that constitute the
product. Here is an example of how the ant task can be used:

``` xml
  <p2.publish.product
    metadataRepository="file:/repository/location"
    artifactRepository="file:/repository/location"
    publishArtifacts="true"
    compress="true"
    flavor="tooling"
    executables="/delta/pack/location/eclipse/features/org.eclipse.equinox.executable_3.3.200.v20090507-7M-Fm-FI3UouOdgtbIvrva"
    productFile="/product/file/location/sample.product">
        <config ws="gtk" os="linux" arch="x86" />
  </p2.publish.product>
```

In addition to the default arguments, the product publishing task
supports the following:

<table border="5" cellspacing="0" cellpadding="1" width="95%" align="center">

<tr>

<td>

`flavor`

</td>

<td>

Set the flavor for the p2 metadata, default is "tooling". Products
should consider using a unique flavor if they have special requirements
for bundle start levels.

</td>

</tr>

<tr>

<td>

`productFile`

</td>

<td>

The location of the .product file describing the product.

</td>

</tr>

<tr>

<td>

`executables`

</td>

<td>

The location of the executables feature. This is the feature that is
used for branding and publishing the executable

</td>

</tr>

<tr>

<td>

<config>

</td>

<td>

Nested elements specifying configurations supported by this product.
Config elements specify ws, os & arch:

<div align="center">

<config ws="gtk" os="linux" arch="x86" />

</div>

<div>

Use <config ws="ANY" os="ANY" arch="ANY"/> if the product can support
any platform.

</div>

</td>

</tr>

<tr>

<td>

<advice>

</td>

<td>

Nested elements specifying specifying additional advice to use when
creating the product. Currently the accepted kinds of advice are
"featureVersions" and "pluginVersions".

<div align="center">

<advice kind="featureVersions" file="finalFeaturesVersions.properties" />
<advice kind="pluginVersions" file="finalPluginsVersions.properties" />

</div>

`       PDE/Build will generate these version properties files when the builder sets the property `<a href="pde_version_qualifiers.htm#final_versions">`generateVersionsLists"`</a>`.`
`       `

</td>

</tr>

</table>

## PDE Build

PDE/Build has built in support for publishing p2 metadata using the p2
Publisher.

The new publisher integration gathers your features and bundles from
source and publishes them directly to a p2 repository. To use the new
functionality, the builder should define the property:

<strong>p2.gathering = true</strong>

Setting this property will change the build in a significant manner:

  - Feature builds produce a single p2 repository that is a group of all
    the configurations being built.
  - Product builds produce a properly installed fully enabled p2
    product. (And optionally the corresponding repository.)

During the build, all metadata and artifacts will be published into a
build repository defined by the property p2.build.repo. The default
location for this repository is ${buildDirectory}/buildRepo.

Once all the metadata and artifacts are published into this repository,
the final assemble and packaging scripts will mirror and/or install from
this repository into the locations that will become the archives
produced by the build. This final mirroring and installation can be
skipped using skipMirroring and skipDirector properties, in which case
the build results would all just be in the build repository.

### Feature Builds

Defining the new property: <strong>p2.gathering = true</strong> will
cause a few changes for feature builds. In particular, the build will
(optionally) produce a single p2 repository which is a group of all the
platforms. <strong>(By default, PDE/Build outputs a p2 repository as
part of the feature build.)</strong>

The following is a list of related properties:

<table border="5" cellspacing="0" cellpadding="2">

<tr>

<td>

<b>`p2.gathering`</b>

</td>

<td>

Set to <b>`true`</b> to turn on p2 publisher based builds.

</td>

</tr>

<tr>

<td>

`p2.build.repo`

</td>

<td>

The local build time p2 repository, default is
`${buildDirectory}/buildRepo`. Results will be mirrored from here to the
final archive location.

</td>

</tr>

<tr>

<td>

`groupConfigurations`

</td>

<td>

`p2.gathering=true` has the implicit effect of setting
`groupConfigurations=true`. To control the
<a href="pde_controlling_output.htm">output format</a> of the archive
use the `group.group.group` configuration.

</td>

</tr>

<tr>

<td>

`p2.metadata.repo`
`p2.artifact.repo`

</td>

<td>

These properties were associated with `generate.p2.metadata` and have no
affect on feature builds when `p2.gathering=true` because the default
behaviour in this case is to create a p2 repository. (However, these
properties do affect <a href="pde_p2_productbuilds.htm">product
builds</a>).

</td>

</tr>

<tr>

<td>

`p2.metadata.repo.name`
`p2.artifact.repo.name`

</td>

<td>

Optional, these properties will be used to name the final feature
repository.

</td>

</tr>

<tr>

<td>

`p2.compress`

</td>

<td>

Set to `true` to compress the final feature repository xml into a jar.

</td>

</tr>

<tr>

<td>

`p2.context.repos`

</td>

<td>

Define context repositories.

</td>

</tr>

<tr>

<td>

`repoBaseLocation`

</td>

<td>

A folder containing repositories to transform using p2.repo2runnable.

</td>

</tr>

<tr>

<td>

`transformedRepoLocation`

</td>

<td>

The folder containing the output of p2_repo2runnable.

</td>

</tr>

<tr>

<td>

`p2.category.site`

</td>

<td>

A URL to a site.xml file used to define categories.

</td>

</tr>

<tr>

<td>

`p2.category.definition`

</td>

<td>

A URL to a category.xml file used to define categories.

</td>

</tr>

<tr>

<td>

`p2.category.prefix`

</td>

<td>

Define a prefix to ensure unique ids for category IUs generated from
site/category files that don't use unique names.

</td>

</tr>

<tr>

<td>

`skipMirroring`

</td>

<td>

Skip the mirroring step, no final archive is created. Build results are
found in `${p2.build.repo}`.

</td>

</tr>

</table>

### Product Builds

Defining the new property: <strong>p2.gathering = true</strong> will
cause a few changes for product builds. In particular, the build will
produce a properly installed, fully p2 enabled, product. A p2 repository
can also be (optionally) produced as an output of the build. <strong>(By
default, PDE/Build does not output a p2 repository as part of the
product build.)</strong>

The following is a list of related properties.

<table border="5" cellspacing="0" cellpadding="2">

<tr>

<td>

<b>`p2.gathering`</b>

</td>

<td>

Set to <b>`true`</b> to turn on p2 publisher based builds.

</td>

</tr>

<tr>

<td>

`p2.build.repo`

</td>

<td>

The local build time p2 repository, default is
`${buildDirectory}/buildRepo`. Results will be mirrored from here to the
final archive location.

</td>

</tr>

<tr>

<td>

`p2.metadata.repo`
`p2.artifact.repo`

</td>

<td>

By default for product builds, the final archives are the installed
products and metadata and artifacts are left in the `${p2.build.repo}`.
If `p2.metadata.repo` and `p2.artifact.repo` are defined, then

`       the artifacts and metadata for the product will be mirrored from the build repository.`

</td>

</tr>

<tr>

<td>

`p2.metadata.repo.name`
`p2.artifact.repo.name`

</td>

<td>

Optional, these properties will be used to name the final repository
when `p2.metadata.repo` and `p2.artifact.repo` are used.

</td>

</tr>

<tr>

<td>

`p2.compress`

</td>

<td>

Set to `true` to compress the final repository xml into a jar.

</td>

</tr>

<tr>

<td>

`p2.flavor`

</td>

<td>

The flavor of the product, used as a qualifier on the configuration
metadata for the product. If unsure, use <strong>tooling</strong>.

</td>

</tr>

<tr>

<td>

`p2.product.qualifier`

</td>

<td>

The qualifier to use when replacing "1.0.0.<i>qualifier</i>" in a
product's version. If not set, the qualifier will be based on
`forceContextQualifier` or the timestamp.

</td>

</tr>

<tr>

<td>

`p2.context.repos`

</td>

<td>

Define context repositories.

</td>

</tr>

<tr>

<td>

`repoBaseLocation`

</td>

<td>

A folder containing repositories to transform using p2.repo2runnable.

</td>

</tr>

<tr>

<td>

`transformedRepoLocation`

</td>

<td>

The folder containing the output of p2_repo2runnable.

</td>

</tr>

<tr>

<td>

`p2.category.site`

</td>

<td>

A URL to a site.xml file used to define categories.

</td>

</tr>

<tr>

<td>

`p2.category.definition`

</td>

<td>

A URL to a category.xml file used to define categories.

</td>

</tr>

<tr>

<td>

`p2.category.prefix`

</td>

<td>

Define a prefix to ensure unique ids for category IUs generated from
site/category files that don't use unique names.

</td>

</tr>

<tr>

<td>

`skipMirroring`

</td>

<td>

Skip the final mirroring from `${p2.build.repo}` to
`${p2.metadata.repo}`.

</td>

</tr>

<tr>

<td>

`skipDirector`

</td>

<td>

Skip the call to the director. No installed products will be produced.
If `p2.metadata.repo` and `p2.artifact.repo` are defined, those
repositories will contain the product metadata and artifacts, otherwise
`${p2.build.repo}` will contain the results.

</td>

</tr>

<tr>

<td>

`p2.director.log`

</td>

<td>

Location of a log file to log the results of the director call.

</td>

</tr>

<tr>

<td>

`p2.director.profile`

</td>

<td>

The name to use for the p2 profile created by the director. Generally it
is a good idea to name this something related to your product. Default
is "profile".

</td>

</tr>

<tr>

<td>

`p2.director.extraArgs`

</td>

<td>

Extra arguments to pass to the directory. Default is
"`-profileProperties org.eclipse.update.install.features=true`".

</td>

</tr>

</table>

## Extensible API

The p2 Publisher can invoked programatically using its extensible API.
<strong>(The publisher API is currently provisional and subject to
change)</strong>

The publisher is structured as a series of <strong>actions</strong> and
<strong>advice</strong>. The publisher can be invoked as follows:

``` java
 IPublisherInfo publisherInfo = ...;
 IPublisherAction[] actions = ...;
 Publisher publisher = new Publisher( publisherInfo );
 publisher.publish( actions, progressMonitor );
```

### Publisher Info

The publisher info provides a context to use when publishing. The
publisher info object describes such things as:

  - The metadata repository where the IUs should be published
  - The artifact repository where the artifacts should be published
  - Artifact publishing options (overwrite existing artifacts, etc...)

### Publisher Actions

There are a number of pre-defined actions for publishing well known
constructs. The following table describes some of the available actions.

<table border=1>

<tr>

<td>

BundlesAction

</td>

<td>

Publish IUs for all of the bundles in a given set of locations or
described by a set of bundle descriptions.

</td>

</tr>

<tr>

<td>

FeaturesAction

</td>

<td>

Publish IUs for all of the features in the given set of locations.

</td>

</tr>

<tr>

<td>

SiteXMLAction

</td>

<td>

ction which processes a site.xml and generates categories.

</td>

</tr>

</table>

In addition to the provided actions, additional actions can be created
by extending `AbstractPublisherAction`.

### Example

The following example shows a custom publisher that publishes bundles.

``` java
 package org.example.publisher;

 import java.io.File;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.Collections;

 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.equinox.app.IApplication;
 import org.eclipse.equinox.app.IApplicationContext;
 import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
 import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
 import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
 import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
 import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
 import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.SimpleArtifactRepositoryFactory;
 import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.SimpleMetadataRepositoryFactory;
 import org.eclipse.equinox.p2.publisher.IPublisherAction;
 import org.eclipse.equinox.p2.publisher.IPublisherInfo;
 import org.eclipse.equinox.p2.publisher.Publisher;
 import org.eclipse.equinox.p2.publisher.PublisherInfo;
 import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;

 /**
  * This simple publisher example demonstrates how to use the publisher
  * API to publish a directory of bundles.
  * @throws URISyntaxException
  * @throws ProvisionException
  */
 public class PublisherExample implements IApplication {

    public Object start(IApplicationContext context) throws Exception {
        IPublisherInfo info = createPublisherInfo();
        IPublisherAction[] actions = createActions();
        Publisher publisher = new Publisher(info);
        publisher.publish(actions, new NullProgressMonitor());
        return null;
    }

    public void stop() {

    }

    public static IPublisherInfo createPublisherInfo() throws ProvisionException, URISyntaxException {
        PublisherInfo result = new PublisherInfo();

        // Create the metadata repository.  This will fail if a repository already exists here
        IMetadataRepository metadataRepository = new SimpleMetadataRepositoryFactory().create(new URI("file:/location to/repository"), "Sample Metadata Repository", MetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, Collections.EMPTY_MAP);

        // Create the artifact repository.  This will fail if a repository already exists here
        IArtifactRepository artifactRepository = new SimpleArtifactRepositoryFactory().create(new URI("file:/location to/repository"), "Sample Artifact Repository", ArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, Collections.EMPTY_MAP);

        result.setMetadataRepository(metadataRepository);
        result.setArtifactRepository(artifactRepository);
        result.setArtifactOptions(IPublisherInfo.A_PUBLISH | IPublisherInfo.A_INDEX);
        return result;
    }


    public static IPublisherAction[] createActions() {
        IPublisherAction[] result = new IPublisherAction[1];
        File[] bundleLocations = new File[1];
        bundleLocations[0] =  new File("/location to bundles/");
        BundlesAction bundlesAction = new BundlesAction(bundleLocations);
        result[0] = bundlesAction;
        return result;
    }
 }
```

## Getting Involved

There are a number of ways to get involved with the development of The
Publisher. In particular:

  - Experiment with and give feedback on the publisher's provisional API
  - Ensure the publisher Java Docs are accurate (and provide feedback /
    patches where needed)
  - Experiment with the publisher's ANT tasks and headless applications
  - Review the list of outstanding publisher bugs [Open Publisher
    Bugs](https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=publisher&classification=RT&product=Equinox&component=p2&long_desc_type=allwordssubstr&long_desc=&bug_file_loc_type=allwordssubstr&bug_file_loc=&status_whiteboard_type=allwordssubstr&status_whiteboard=&keywords_type=allwords&keywords=&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&emailtype1=substring&email1=&emailtype2=substring&email2=&bugidtype=include&bug_id=&votes=&chfieldfrom=&chfieldto=Now&chfieldvalue=&cmdtype=doit&order=Reuse+same+sort+as+last+time&field0-0-0=noop&type0-0-0=noop&value0-0-0=)

The publisher developer discusses take place on the [p2-dev
list](https://dev.eclipse.org/mailman/listinfo/p2-dev)

[Publisher](Category:Equinox_p2 "wikilink")