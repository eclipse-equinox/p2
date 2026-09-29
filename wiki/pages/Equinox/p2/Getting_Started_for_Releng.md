The Eclipse Ganymede (3.4) release introduces [Equinox
p2](Equinox_p2 "wikilink"), a new platform for installing, managing, and
updating Eclipse-based applications and extensions. p2 offers
significantly improved functionality over the platform Update Manager
used in previous releases. Leveraging this new functionality requires
some changes for people building and assembling Eclipse-based
applications and add-ons. This article helps buildmeisters and release
engineers to get started with adopting p2. See also [Equinox p2 Getting
Started](Equinox_p2_Getting_Started "wikilink") for an introduction to
p2 from an end-user perspective.

# Why should I make changes to adopt p2?

p2 offers significant advances over the Update Manager found in previous
versions of the platform. Some advances include:

  - The ability to install complete Eclipse-based applications from
    scratch, above just augmenting and updating already installed
    applications.
  - The ability to install and manage an application from either inside
    the application or from another process.
  - The ability to install and manage artifacts other than plug-ins and
    features, such as launchers, root files such as licenses,
    configuration files, Java VMs, etc.
  - Dynamic discovery and high-fidelity resolution of software
    dependencies.

These advances require additional metadata about the software being
installed above the information in the feature.xml and site.xml files
used by Update Manager. Feature.xml files provide an incomplete picture
of software dependencies - they are essentially a coarse-grained subset
of the dependencies expressed in the plug-in manifest files
(MANIFEST.MF). p2 metadata includes a complete description of these
dependencies, allowing it to be 100% accurate about resolving
dependencies at install-time. With p2, you can pick a single plug-in to
install, and p2 will be able to compute the complete set of plug-ins and
other artifacts required to satisfy the runtime requirements of that
plug-in. To accomplish this, p2 needs to either obtain or create this
high-fidelity dependency information at install-time.

Update Manager only supported adding additional content, or upgrading
existing content, in an Eclipse-based application. It did not allow you
to provision an entire application from scratch, including launchers and
other files that were not plug-ins or features. p2 can install and
update entire applications, including all these non plug-in/feature
bits. To accomplish this, p2 needs to obtain or create the necessary
metadata to reason about these artifacts and their dependencies.

The reason build and release engineering teams should take steps to
adopt p2 is to produce this p2 metadata that allows p2 to support this
new level of functionality.

# What do I need to do to adopt p2?

The following sections describe steps to adopting p2 depending on what
kind of outputs your project produces.

## Zips of plug-ins and features

If your project produces zips of plug-ins and features for consumption
by others, no action is strictly required. p2 provides a number of ways
for an end-user to install such zips:

  - Drag into the "Software Updates" dialog in the UI
  - Unzip into the eclipse/dropins folder
  - Unzip directly on eclipse root directory (generally not recommended
    as it makes it difficult to distinguish added content from the base
    application).

However, when p2 encounters raw plug-ins and features, it needs to do
some processing on the content to compute p2 metadata. This results in a
slower experience for the end user when the content is first installed.
You can optimize this by creating a p2 repository that contains your
plugins and features. A p2 repository is created automatically if you
export plug-ins and features from your IDE using the PDE plug-in export
wizard. This adds a *content.xml* and *artifacts.xml*. p2 also provides
a *p2.generator* Ant task or stand-alone application that can be invoked
programmatically or from a build script to produce a p2 repository. See
[Equinox p2 Metadata
Generator](Equinox_p2_Metadata_Generator "wikilink") for more
information about the p2 generator application.

## Update sites

If you currently produce an Eclipse Update Site (site.xml), there is
also no action required. p2 can install plug-ins and features directly
from existing update sites produced for earlier releases of the Eclipse
Platform. However, p2 needs to produce its metadata on the fly when a
simple update site is encountered. This results in a slower experience
for the end user when installing content from update sites. To eliminate
this slowness, it is recommended that you *p2-enable* your update site
by running the p2 update site publisher on it. This will add p2 metadata
in the form of a content.xml and artifacts.xml file to your update site.
See [Equinox/p2/Publisher](Equinox/p2/Publisher "wikilink") for more
information about the p2 update site publisher application.

### Artifacts.xml mapping rule change

By default, a SimpleArtifactRepository (artifacts.xml / artifacts.jar)
looks for plug-ins and features in a subfolder of the repository. This
is driven by mapping rules found in the artifacts.xml. this is a filter
where the first entry to match wins (rules have to be specified from the
most specific to the least). Here are for example the default rules for
the Galileo repository:

<mappings size="5">
`    `<rule filter='(& (classifier=osgi.bundle) (format=packed))' output='${repoUrl}/plugins/${id}_${version}.jar.pack.gz'/>
`    `<rule filter='(& (classifier=osgi.bundle))' output='${repoUrl}/plugins/${id}_${version}.jar'/>
`    `<rule filter='(& (classifier=binary))' output='${repoUrl}/binary/${id}_${version}'/>
`    `<rule filter='(& (classifier=org.eclipse.update.feature) (format=packed))' output='${repoUrl}/features/${id}_${version}.jar.pack.gz'/>
`    `<rule filter='(& (classifier=org.eclipse.update.feature))' output='${repoUrl}/features/${id}_${version}.jar'/>
</mappings>

It is possible to change those rules to have a filter matching for a
particular artifact and change the output to point to a specific
location. Rules are specified in the *mappings* section of the
artifacts.xml file. For example to make p2 to download the
`org.sat4j.pb` plug-in through the eclipse.org download script (which
provides download tracking) add the following line:

<rule filter='(& (id=org.sat4j.pb) (classifier=osgi.bundle))'
    output='http://www.eclipse.org/downloads/download.php?file=/eclipse/updates/3.5milestones/S-3.5RC3-200905282000/plugins/${id}_${version}.jar'/>

To make p2 download the `org.eclipse.platform` feature using the
eclipse.org download script, add the following line:

<rule filter='(& (id=org.eclipse.platform) (classifier=org.eclipse.update.feature)'
    output='http://www.eclipse.org/downloads/download.php?file=/eclipse/updates/3.5milestones/S-3.5RC3-200905282000/features/${id}_${version}.jar'/>

Order is important.

## Complete applications

If you currently produce your own Eclipse-based application, the action
required for adopting p2 depends on exactly how you are building the
application.

### Applications created by dropping bundles on top of an existing application

If you currently create your own application by dropping extra plug-ins
or features into an existing application (such as the Eclipse Platform
application), then no action is required. When the application first
starts, your bundles will be detected and corresponding p2 metadata will
be generated.

However, there are some advantages to p2-enabling your application. This
involves creating p2 metadata to represent the bundles in your
application, and also to represent your application as a whole within p2
metadata. This would enable p2 to install or update your entire
application, including non-bundle configuration files, as a single unit.

To p2-enable your application, run the [Equinox p2 Metadata
Generator](Equinox_p2_Metadata_Generator "wikilink") on it. This will
produce a p2 repository containing both p2 metadata for your
application, and an artifact repository containing the bundles and other
artifacts in your application. From this repository a p2 install
operation can be executed to install your application (for example with
the [Equinox p2 Installer](Equinox_p2_Installer "wikilink"), or the
[Equinox p2 director
application](Equinox_p2_director_application "wikilink")).

### Applications created by using PDE product build

Add the following properties to your **build.properties** file:

`generate.p2.metadata=true`
`p2.metadata.repo = `[`file:${buildDirectory}/repo`](file:$%7BbuildDirectory%7D/repo)
`p2.artifact.repo = `[`file:${buildDirectory}/repo`](file:$%7BbuildDirectory%7D/repo)
`p2.metadata.repo.name = Meta Repo Name`
`p2.artifact.repo.name = Artifact Repo Name`
`p2.flavor = tooling`
`p2.publish.artifacts=true`

This will generate a metadata/artifact repository that can be used for
installing the application. You can test this by creating your own
*installer.properties* file pointing to the newly created repository and
use the [Equinox p2 Installer](Equinox_p2_Installer "wikilink"). For
instance:

`eclipse.p2.rootId=foo.bar.product`
`eclipse.p2.metadata=`<file:///var/build/product/repo>
`eclipse.p2.artifacts=`<file:///var/build/product/repo>
`eclipse.p2.launcherName=product`
`eclipse.p2.autoStart=true`

Then launch the installer using:

`p2installer -vmargs -Dorg.eclipse.equinox.p2.installDescription=`<file:installer.properties>

[Releng](Category:Equinox_p2 "wikilink")