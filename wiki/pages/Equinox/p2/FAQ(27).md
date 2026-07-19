## What is p2?

p2 is a *provisioning platform* for Eclipse-based applications.  It
replaces the older Update Manager as the mechanism for managing an
Eclipse installation.  *Provisioning* is the act of finding and
installing new functionality, and updating or removing existing
functionality; it is distinct from *building*.

p2 manages *artifacts*, such as bundles, features, and products; you can
think of these as bags of bytes.  p2 not only stores these artifacts, it
also stores *metadata* about these artifacts, such as version
information, cryptographic signatures, dependencies between artifacts,
platform specifics, and special installation requirements.

Every p2 artifact is uniquely identified by an identifier and version
number. For example, the Equinox OSGi container from the Indigo release
is a bundle whose identifier is `org.eclipse.osgi` and version
`3.7.0.v20110110`. p2 assumes that two artifacts with the same
identifier and same version number <em>are the same artifact</em>.

An artifact is made available to p2 by *publishing* the artifact to a
repository, a process that also adds any metadata about the artifact. p2
comes with several publishers that know how to extract metadata for
bundles, features, and products. Most Eclipse build systems (e.g.,
Tycho, Buckminister, PDE/Build) can automatically publish the artifacts
during a build. Other publishers could be created using p2's APIs to
handle other types of artifacts (e.g., Windows DLLs).

The p2 *Director* is responsible for installing a set of artifacts. The
Director uses the metadata about an artifact, called an *installable
unit* or *IU*, to determine how the artifact should be installed and to
include any dependencies of those artifacts. Thus installing a product
requires installing its associated features, which requires installing
the specified bundles; installing the product also requires installing
the platform-specific executable.

The Director is a wrapper around several lower-level p2 components. A
*Profile* records the IUs installed. An *Agent* uses the *Planner* to
determine the IUs to be managed, and uses the *Engine* to actually
perform the sequence of installation steps. These installation steps are
handled by *touchpoints* specific to each type of artifact.

See the [p2 concepts guide](Equinox/p2/Concepts "wikilink") for more
details about these different terms.

## Accessing Repositories

### Why won't p2 replace an updated bundle in a repository?

p2 assumes that two artifacts with the exact same identifier and version
*are the same artifact*. Once an artifact has been published to a
repository, it cannot be replaced. This restriction acts both as a
performance optimization, to avoid unnecessary downloads, as well as a
consistency check. For example, the Equinox OSGi container from the
Indigo release is a bundle whose identifier is `org.eclipse.osgi` and
version `3.7.0.v20110110`. Any update to an artifact must be published
with a new version number.



### How can I find the features that include a particular bundle?

Using the [p2 Query Language](Query_Language_for_p2 "wikilink") from the
[OSGi console](Equinox/p2/Console_Users_Guide "wikilink"), substitute
the appropriate repository URL and BUNDLEID:

    $ eclipse -console -noSplash -noExit -vmargs -Declipse.application.launchDefault=false
    osgi> provaddrepo http://download.eclipse.org/eclipse/updates/3.7-I-builds
    osgi> provlpquery * "select(parent | parent.properties['org.eclipse.equinox.p2.type.group'] == true
       && parent.requirements.exists(rc | everything.exists(iu | iu ~= rc &&
           iu.id == 'BUNDLEID')))" true

For wildcards one may use:

    osgi> provlpquery * "select(parent | parent.properties['org.eclipse.equinox.p2.type.group'] == true
       && parent.requirements.exists(rc | everything.exists(iu | iu ~= rc &&
           iu.id ~= /*BUNDLEID*/)))" true

The `prov`\* commands shown above are only available once the
`org.eclipse.equinox.p2.console` bundle has been started. See the [p2
Console Guide](Equinox/p2/Console_Users_Guide "wikilink") for how to
start the bundle.

Credit to Paul Webster.

## Integration

### How do I incorporate p2 into my RCP app?

There are [three
approaches](http://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/guide/p2_api_overview.htm)
for integrating p2 into your app.

1.  The first approach, called the "[User Interface
    API](http://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/guide/p2_api_overview.htm)",
    is actually a framework for managing updates, complete with wizards
    and configurable scheduling. It is the same framework used by
    Eclipse's update management. Your application provides policies and
    other configuration values to guide this framework. Note that this
    API has a dependency on the Eclipse 3.x Platform UI
    (*org.eclipse.ui*), and requires the Platform UI compatibility layer
    for applications using the Eclipse 4 Application Platform.
2.  The second approach is to use p2's higher-level [Operation
    APIs](http://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/equinox/p2/operations/package-summary.html) to
    drive updates. These APIs provide a thin layer over the Core APIs
    (described next) to describe updates as an atomic install,
    uninstall, or update of an artifact. are more intended for headless
    applications.
3.  The third and final approach, is to use p2's [Core
    APIs](http://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/guide/p2_api_overview.htm),
    where you interact with the p2 agent directly. This is the most
    powerful, but also most complex, of the possible approaches. See the
    worked examples for all approaches in the article on
    "[Equinox/p2/Adding Self-Update to an RCP
    Application](Equinox/p2/Adding_Self-Update_to_an_RCP_Application "wikilink")",
    and the [example
    applications](http://git.eclipse.org/c/equinox/rt.equinox.p2.git/tree/examples).
    (Other examples
    [ex1](https://github.com/adreghiciu/p2/blob/master/bundles/org.eclipse.equinox.p2.console/src/org/eclipse/equinox/internal/p2/console/ProvisioningHelper.java).)

Please note that we *do not recommend* that you invoke the p2 director
from your application with a shell call (e.g., "app.exe -application
org.eclipse.equinox.p2.director …")\!

There is a fourth approach for integrating p2 into your app: using
[dropins](Equinox_p2_Getting_Started#Dropins "wikilink"). DBut dropins
are not recommended, and has been officially deprecated by the p2 team.

### Why aren't my dropins being picked up?

`See the `[`dropins``   ``debugging``
 ``section`](Equinox_p2_Getting_Started#Debugging_dropins "wikilink")` for details on debugging dropins-related issues.`

### Why am I getting errors trying when access p2 services?

p2 relies on the OSGi Declarative Services feature. You must include and
start the `org.eclipse.equinox.ds` bundle. Be sure to read the [p2 Core
API
overview](http://help.eclipse.org/indigo/topic/org.eclipse.platform.doc.isv/guide/p2_overview.htm)
on how to access p2 services.

### Why are my uninstalled bundles being reloaded\!?

    !ENTRY org.eclipse.update.configurator 4 0 2008-07-16 17:20:33.320
    !MESSAGE Could not install bundle plugins/org.eclipse.compare_3.4.0.I20080604.jar   Bundle "org.eclipse.compare" version "3.4.0.I20080604" has already been installed from: reference:file:plugins\org.eclipse.compare_3.4.0.I20080604.jar

Such messages are a symptom of the `org.eclipse.update.configurator`
being installed. The `org.eclipse.update.configurator` causes all
bundles in `plugins` to be installed into the runtime. To prevent this
behaviour, set the `org.eclipse.update.reconcile` property to `false`
for your product.

`org.eclipse.update.configurator` is included as part of the
`org.eclipse.rcp` feature and cannot be removed for historical reasons.

### But why aren't uninstalled bundles/features immediately removed?

p2 does not immediately remove bundles on uninstall for several reasons:

  - <em>Minimizing waste:</em> By retaining the bundles, p2 can save
    time and bandwidth to should these bundles be re-installed.
  - <em>For safety:</em> Many bundles do not properly support
    hot-swapping and removal. Writing bundles that can be uninstalled
    completely is surprisingly difficult, and even harder to test.

In practice the timing of deletion should not matter as p2 ensures that
the bundles will not be re-installed (modulo the
`org.eclipse.update.configurator` issue described elsewhere in this
FAQ).

p2 does periodically perform a garbage collection of uninstalled
features and bundles, typically after having performed another
uninstall. See elsewhere in this FAQ for details on how to explicitly
request a garbage collect.

### Why aren't bundles being removed when their associated feature has been removed?

There are two aspects to this problem.

First, you should ensure that features are recorded when installing a
feature by specifying the "`org.eclipse.update.install.features=true`"
property, such that the bundles have a recorded dependency against the
feature. This property is not the default, so if you are using the p2
director manually it is worth verifying.

Second, you must invoke the p2 garbage collector.\[1\] By default, p2
only performs a garbage collect at its discretion. Normally this is fine
since the actual bundles to be loaded are still known and managed
(either in the `config.ini` file or by the `simpleconfigurator`).

You can cause a garbage collect in one of 3 ways:

  - Configure your product to invoke a GC on startup by adding the
    following line to your product's `plugin_customization.ini`
    (requires the `org.eclipse.equinox.p2.ui.sdk.scheduler` bundle):

<!-- end list -->

    org.eclipse.equinox.p2.ui.sdk.scheduler/gcOnStartup=true

  - Run the garbage collector application:\[2\]

<!-- end list -->

    eclipse -application org.eclipse.equinox.p2.garbagecollector.application -profile SDKProfile

  - Explicitly invoke the p2 garbage collection from code:

<!-- end list -->

    IProvisioningAgentProvider provider = // obtain the IProvisioningAgentProvider using OSGi services
    IProvisioningAgent agent = provider.createAgent(null);  // null = location for running system
    if(agent == null) throw new RuntimeException("Location was not provisioned by p2");
    IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
    if (profileRegistry == null) throw new RuntimeException("Unable to acquire the profile registry service.");
    // can also use IProfileRegistry.SELF for the current profile
    IProfile profile = profileRegistry.getProfile("SDKProfile");
    GarbageCollector gc = (GarbageCollector) agent.getService(GarbageCollector.SERVICE_NAME);
    gc.runGC(profile);

### Why am I unable to fetch IUs from MS IIS?

IIS requires some additional configuration:

1.  Start the IIS Manager and navigate to your repository location
2.  Double-click MIME Types; you should now see the list of all known
    MIME types
3.  Click *Add...* on the Actions on the right side of the manager, type
    ".\*" as the file name extension and "application/x-zip-compressed"
    as the MIME type
4.  Restart the web site

Source:
[1](http://www.eclipse.org/forums/index.php/mv/msg/452001/1021642/#msg_1021642)

## Using p2 for Updating

### How do I revert to a previous installation?

See the separate
[FAQ](FAQ_Revert_an_Update_or_Installation_with_p2 "wikilink").

### Why am I getting dependency satisfaction errors when I update my feature?

Deciphering a cryptic failure message like the following is a rite of
passage on becoming a p2 expert:

    Cannot complete the install because of a conflicting dependency.
    Software being installed: MyTool Feature 2.0.3 (com.mytool.feature 2.0.3)
    Software currently installed: MyTool Feature 2.0.2 (com.mytool.feature 2.0.2)
    Only one of the following can be installed at once:
       com.mytool.feature 2.0.3
       com.mytool.feature 2.0.2
     Cannot satisfy dependency:
       From: MyTool 2.0.2 (com.mytool.product 2.0.2)
       To: MyTool Feature (com.mytool.feature 2.0.2)

To understand and solve this message requires understanding three key
concepts about how p2 manages installations.

1.  <b>Root IUs:</b> When an IU (e.g., a product, feature, or bundle) is
    explicitly provisioned, the IU is recorded in the p2 profile as a
    <em>root IU</em>. A root IU will never change until explicitly
    removed. When a new IU is requested to be installed (i.e., to become
    a new root IU), p2's <em>planner</em> attempts to find a
    configuration that <em>satisfies</em> the dependencies of the new IU
    with the dependencies of the existing root IUs. If no such
    configuration can be found then the installation fails, and the
    planner provides an explanation of the failing constraint, like the
    text above. A little known fact: this planning phase could result in
    a configuration where some dependent IU is downgraded to an older
    version so as to satisfy the dependencies\!
2.  <b>Singleton IUs:</b> Many IUs are marked as <em>singletons</em>,
    such that two versions of an IU cannot be installed at the same
    time. Products and features, for example, are singletons. Although
    OSGi allows installing and running multiple versions of a bundle
    (useful for supporting different versions of
    `org.apache.collections`, for example), certain types of bundles are
    marked as singletons, like an Eclipse plugin that contributes
    extensions to the extension registry, presumably because the
    extension registry does not support differentiating between multiple
    versions of a plugin and extension. Other IUs, like products IUs,
    are singletons as they actually correspond to files on disk (e.g.,
    "`eclipse.exe`"): it's impossible for two different versions of a
    product to be installed at the same time.
3.  <b>Version ranges:</b> p2 expresses dependencies between IUs using
    <em>version ranges</em>. But several of PDE's standard artifacts,
    notably products and features (both of which are also singletons),
    historically specify their dependencies using <em>exact</em> version
    numbers. Thus a product that requires version 2.0.2 of a feature
    cannot be satisfied with version of 2.0.3.
4.  <b>Installation vs Update:</b> Although only an isssue when using
    the lower-level p2 Core APIs, specifying an IU to install (as a root
    IU) does not automatically update any existing versions of that IU.
    Although the p2 planner will consider replacing a non-root IU to
    satisfy a dependency, p2 will never update a root IU. Updating a
    root IU requires explicitly uninstalling the old IU and installing
    the new version. Note that the p2 Install New Software / Update
    Software wizard does detect this situation and transforms
    installations into updates.

With this knowledge, we can now interpret the situation leading to the
error in the example above. The user has requested that feature
`com.mytool.feature` 2.0.3 be installed. Because features are
singletons, only one of `com.mytool.feature` 2.0.2 and 2.0.3 can be
installed at once. Product `com.mytool.product` 2.0.2, which we know is
a singleton and by default specifies exact version dependencies,
requires exactly version 2.0.2 of `com.mytool.feature`. Since products
are typically root IUs, product `com.mytool.product` 2.0.2 cannot be
uninstalled and thus `com.mytool.feature` 2.0.2 cannot be uninstalled,
and so the installation fails. The solution is to relax the
`com.mytool.product` definition to allow a version range on the
`com.mytool.feature` rather than an exact version by complementing the
`.product` file with a carefully-crafted `p2.inf` file.\[3\]

To avoid installation failures:

1.  Specify the minimum set of IUs actually required. Let p2 satisfy the
    remaining dependencies.
2.  Decouple features based on API vs implementation dependencies. If
    your feature does not actually depend on the implementation of a
    particular version of a feature, then don't specify that
    requirement: instead ensure the features are required in a
    higher-level mechanism, like a product. Or specify such feature
    dependencies using the "Dependency" tab of the feature editor (akak
    imports). For example, in Eclipse Platform 4.2, the
    `org.eclipse.rcp` feature has an exact requirement on the
    `org.eclipse.e4.rcp` feature as there is an implementation
    dependency (i.e., org.eclipse.rcp 4.2 may not work with
    org.eclipse.e4.rcp 4.3), but `org.eclipse.e4.rcp` has an import
    dependency using a version range on two EMF features as it is an API
    dependency.
3.  Use `p2.inf` files where necessary to work around PDE artifacts that
    historically specify exact versions.\[4\]

### How can I determine what is a root IU?

You can see the items marked as a root by looking at the latest profile
file in `.../p2/org.eclipse.p2.engine/profileRegistry/`<profileName>`/`
and looking for the string 'org.eclipse.equinox.p2.type.root'.

### How can I tell if an IU is a singleton?

One way is to examine the `content.xml` for the containing repository,
and find the IU's <unit>.

``` xml
<unit id='org.junit.source' version='4.8.2.v4_8_2_v20110321-1705' singleton='false'>
...
</unit>
```

IUs are considered singletons by default.

An excercise for a future reader: construct a p2 query to check the
status of an IU in a repository.

## p2 Update UI

### Why doesn't my feature show as an installed feature?

Only branded features are shown in the <em>About → Installation Details
→ Installed Features</em>. See the following links for background:

  - <https://bugs.eclipse.org/bugs/show_bug.cgi?id=274134#c5>
  - <https://bugs.eclipse.org/bugs/show_bug.cgi?id=348924>
  - <http://ekkescorner.wordpress.com/2010/06/13/brand-your-feature-and-be-part-of-about-eclipse/>

## Where can I learn more?

  - The [p2 wiki](Equinox/p2 "wikilink")
  - Pascal Rapicault has a number of
    [presentations](http://www.slideshare.net/PascalRapicault/) on
    various aspects of p2.
  - Eclipse articles and tutorials tagged with
    [p2](http://www.eclipse.org/resources/?category=p2)
  - Webinars tagged with
    [p2](http://live.eclipse.org/search/node/equinox+p2)

## References

<references />

[Category:Equinox](Category:Equinox "wikilink")
[Category:Equinox_p2](Category:Equinox_p2 "wikilink")
[Category:FAQ](Category:FAQ "wikilink")

1.  <http://dev.eclipse.org/mhonarc/lists/p2-dev/msg00905.html>
2.  <http://www.eclipse.org/forums/index.php?t=msg&goto=543500>
3.  <http://aniefer.blogspot.com/2009/07/composing-and-updating-custom-eclipse.html>
4.  <http://aniefer.blogspot.com/2009/07/composing-and-updating-custom-eclipse.html>