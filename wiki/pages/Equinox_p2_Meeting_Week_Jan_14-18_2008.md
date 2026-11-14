A number of meetings were held the week of Jan 14 - 18, 2008 to make
**DECISIONS** about the content of the p2 1.0 release. All times listed
are in [Eastern Standard
Time](http://timeanddate.com/library/abbreviations/timezones/na/est.html)

# Schedule of topics and meetings

## Monday

### Update manager compatibility

When: Monday 1:30-2:30

Who: Simon, DJ, Dave S, John, Pascal

What:

  - RCP application will be able to run with only update manager, or
    only p2 or both at the same time.
  - The SDK will be shipping all of the org.eclipse.update \* plugins
  - The old update ui will be disabled by default
  - When both p2 and UM are available in one configuration, the two
    representations are kept in sync.
  - In 1.0, groups may not be containing an artifacts for the
    feature.xml (and others). This would allow for a "pure" p2 mode.

<!-- end list -->

  - Questions left to be answered:
      - IUs mocked for plugins published on an update site needs to be
        replaced with complete IUs.
      - Support for install handlers when installing from an update site
        through p2 UI.
      - What were update manager policy files used for? Do we have an
        equivalent mechanism?
      - How do we support feature patches?
      - Do we keep the dropins folder concept?
      - How does the old update UI is made visible or not?

<Simon>

### Tooling

When: Tuesday 2:30-4:30

Who: Darin, Susan, John, Pascal

What:

  - UI Workflows
  - Change to pde ui code
  - Relation PDE / update
  - p2 target provisioner
  - Feature selfhosting replacement
  - Do we keep features
  - editor work

Minutes:

  - Authoring tooling
      - Editors
          - Need to maintain editors for old stuff: update sites,
            features, etc
          - Do we need an editor for authoring IUs directly? No. People
            will continue to author features, etc, and we will generate
            the p2 metadata behind the scenes.
          - Not clear what changes are needed in product editor. Perhaps
            a flag/checkbox to specify whether p2 or UM is being used.
            Also a way to set the start level of bundles.
      - Repositories
      - Should we have tooling in PDE for browsing/publishing to remote
        repositories? No.
  - Publishing
      - Nice to have, but probably not for 1.0
  - Target provisioner
      - Currently there can only be one active target platform. Would
        like to evolve to support multiple active target platforms
      - We will add a p2 target provisioner that can take things from a
        p2 repository, and fetch the IUs selected by the user.

<!-- end list -->

  - Launch tooling
  - Compatibility
  - Build
      - Need to be able to publish/export in either new format or old
        format

<!-- end list -->

  - Suppose I deploy an application that is entirely "new school". It
    contains no features, only IUs. Someone wants to author a feature
    that depends on a group in this new school application. How can the
    user specify this dependency in the feature editor? Currently this
    is broken in a p2-provisioned Eclipse SDK.

### Shape of eclipse

When: Monday 4:30-

Who: Jeff, Susan, Simon, DJ, Andrew N, John, Pascal

What:

  - We will keep the old all-in-one zip downloads. No configuration:
    unzip and run.
  - We will continue to ship the same feature.xmls for the SDK
      - Zip layout is same as Eclipse 3.3, plus p2 stuff:

`- eclipse/`
` - eclipse.exe`
` - plugins/`
` - features/`
` - p2/`

  - Is all the p2 metadata pre-configured or generated on first startup?
      - bundles.txt, profile/install registry, bundle pool
      - Easiest solution is that nothing gets automatically spoofed up
        on startup. Packagers/builders must invoke the "p2-izer" to
        prepare their zips

<!-- end list -->

  - Installer <John>
      - We will provide a simple p2 installer
      - Small (\~3MB) SWT application
      - No native integration
      - It will allow either shared Eclipse install with common bundle
        pool, or standalone "legacy" shape.
      - Installer is either a zip file, or a self-extracting zip
      - No uninstaller, but we need to support profile deletion when the
        user deletes an install manually.
      - What if the format of the installer (self-extracting zip/exe,
        etc.)
      - How do we deal with eclipse friends?

<!-- end list -->

  - how many agents
  - p2-ization, when, what for, what from (See build discussion).
  - Support for Vista
  - Recovery
      - We will provide a recovery application that will bring up the
        end user ui. This application will either be built-in, or it can
        be downloaded separately.
      - We also need to be able to recover from corrupted p2 files, or
        files in artifact repos.

## Tuesday

### User interaction scenarios

When: Tuesday 9:30-12:00

Who: Susan, Tim M, John, Jeff, DJ, Simon

What:

  - Support for drag & drop install <Susan>
  - Do we keep the dropins folder concept <Susan>
  - Do we keep the UI to add extension locations? <Susan>
  - UI for drop-ins folder? <Susan>
      - These are all related. Compelling use case: user got a jar from
        somewhere and wants to add to system
      - Drag a zip/jar/URL/dir to available features list
      - if drag source is a bundle, it goes to drop-ins folder,
        otherwise we are either:
          - adding a repo (make metadata for this zip file and consider
            it a repo)
          - adding content to the default drop-ins folder
      - Drag a zip/jar/URL to installed features list - does above +
        install
      - Repo properties control how often repo content is refreshed, are
        things automatically installed
      - There is a default drop-in location but user can add more via
        these add repo scenarios
      - Need to think about whether removing a repo ever automatically
        uninstalls everything that was in it if "install

automatically" was on. (Equivalent to remove extension location.)

  - Prompting for trusted signers, relationship to licenses <Tim>
      - Licenses is done and always happens as part of the install UI
        (because licenses known up front)
      - Trust is verified during/after collect phase. Prompting happens
        in different places
          - If user pre-downloaded due to autoupdates, then trust should
            be shown along with licenses in update wizard
          - If user has already been in an install/update wizard and
            then does the download, the UI happens after
          - John and Tim will work out details
  - Do we keep the old ui? <Susan>
      - We need to because we can't handle all of the old update sites
      - We need to run in an either/or/both mode, default is p2 UI is on
      - How to handle both of them making UI contributions. Update UI
        would still contribute to menu and probably hide its menu
        contributions in code somewhere if it finds that p2 is
        installed.
  - Verify that all the update manager functionalities are covered in p2
    <Susan>
      - The various history and activity logs will be presented as
        needed in revert UI
      - We think we are covered
  - Do we want to keep the disable functionality, how do we implement
    it?
      - We will not do it for 3.4/1.0

### Metadata

When: Tuesday 2-4

Who: Dave, John, Pascal, Jeff, Susan

What:

  - Metadata structure for an eclipse application <PascaL>
      - product vs extension.
      - Description of a "base" (to support firefox like model and allow
        the uninstallation of everything)
      - UI issues
          - "base" should be distinguished in installed features list.
            For example you can't uninstall it.
          - "Installed Features" terminology - even if not implemented
            as features, the name is ok
          - Available features - only show groups? Yes, and when a
            bundle is dropped into Eclipse a referring group IU will be
            generated. The IU generated for the bundle itself should
            match the original
  - shape of new items:
      - update - need to flesh out more metadata including description
        of update <Pascal>
      - fix pack <Pascal/Jeff>
      - translation of metadata \<Dave S.\>
      - translation for installation of bundles \<Dave S.\>
      - flavor - keep it even though we aren't using yet, change
        "tooling" to "default" <Pascal>

## Wednesday

### Shared installs

When: Wednesday 9:30-10:30

Who: Andrew O, Pascal

What:

  - basic idea:
      - eclipse product (SDK, RCP app (ex. RSSOwl), etc.) layed down on
        disk by some means (RPM installation, etc.)
      - product is a read-only location to users
      - it is desirable that users still have the ability to add their
        own additional bundles
      - some related documents: Equinox_p2_Shared_Install,
        Equinox_p2_Shared_Install_Plan

<!-- end list -->

  - system integrators will ship bundles but no agentData area
  - system integrators will also ship metadata related to the bundles
    (or something to denote what IUs are root IUs)
  - there will be no system-wide profile registry
  - profiles will be spoofed upon invocation of a p2 operation from the
    running bundles (the shared bundles + any user bundles)

<!-- end list -->

  - additional sets of plugins (ex. EMF, CDT)
      - RPM installation -- for example -- of these will need to just
        lay down the bits
      - subsequent startup will need to pick up these bits of
        functionality
      - running some sort of p2 installer post-installation doesn't work
        due to order of installation operations and will be fragile
      - two options: directory watcher (a la links folder) or generating
        partial bundles.txt files to ship with the additional RPMs
          - links folder isn't ideal (Pascal: please add reasons here)
              - "higher-level" (ex. up the stack) dependencies must
                consume \*all\* of directory contents and would force
                people to create mirrored subsets of what they want
      - partial bundles.txt:
          - doesn't force any particular directory layout

<!-- end list -->

  - Outstanding issues:
      - how to spoof a profile (use bundles.txt and metadata?)
      - explore UI interaction
          - writable locations
          - indicating locked things
          - dis-allowing updates to locked things
      - generate subset of bundles.txt for extensions (ex. EMF, CDT,
        etc.)
      - GC interaction (not really a big deal since it won't be able to
        write)

### Ganymede, EPP

When: 10:30-11:30

Who: Thomas H, John, Pascal, Andrew O, Andrew N

What: Installer

  - Introducing a p2 installer for EPP packages has a number of benefits
      - Allows for installing an Eclipse-based application into a common
        bundle pool, reducing client disk footprint
      - Allows optimization of download: pack200 compression, adaptive
        mirror selection, download restart, etc.

Gany-matic

  -   - Need to add a step to the end of Gany-matic to produce p2
        metadata against the Ganymede update site (p2-izer)
      - Longer term (after Ganymede release), teams contributing to the
        release train would produce their own p2 metadata. Train-o-matic
        would then just aggregate/federate that data rather than
        maintain its own p2 repositories. This allows team to contribute
        more customized p2 metadata that cannot be reverse-engineered
        from the update site content.
      - There may be a small number of teams (particularly platform
        team) that may need to produce their own custom p2 metadata for
        Ganymede release. We will investigate how this metadata can be
        picked up by Gany-matic and fed into the p2-izer.

For keener teams that want to produce their own metadata, what do we
tell them to do?

  - Run PDE build which produces p2 metadata in 3.4
  - Invoke the p2 generator directly? (Eclipse app or Ant task)
  - Distinguish between add-on providers and product providers (some
    projects do both)
      - Producers can create a pre-installed product zip that already
        has p2/ folder with all attendant metadata

p2/Ganymede, second meeting

  - Ganymede
      - The gany-matic will be change to produce p2 metadata from the
        final update site.xml.
          - Things to be think about, reuse pack200 jars available from
            the server and publish them as artifacts.

<!-- end list -->

  - EPP
      - Epp produces its packages from a description file. This file
        contains:
          - urls to update sites
          - features that must be included in the package
          - starts from platform zip
          - packages also contains tweak to the eclipse.ini file,
            config.ini (e.g. changing the default perspective)
          - <http://wiki.eclipse.org/EPP/Configuration_File_Format>
      - From this description file, an epp tool drives PDE packager and
        produces the download.
      - p2 is working on a simple installer:
          - this installer is driven by a "response file" (what to
            install, where from, etc.)
          - producing an installer for a product consists in authoring a
            response file. No code change is needed.
      - The EPP team thinks that an simple installer like the one being
        produced by the p2 team coudl be useful. EPP (Markus) and the p2
        will tentatively create an installer, and also maybe a p2-ized
        version of the download for M5.
      - Possible Technical direction:
          - From the package description produces the product IU
            encompassing all aspects of the package.
          - This product IU will be referred to from the response file
            for an installer
          - This product IU will be used to drive the p2 to create the
            final download

### Download technology

When: Wednesday 1-2

Who: Tim W, Scott, Tim M, Thomas H, John, Pascal, Stefan, Matt F.

What:

  - Proxy support. At the ECF level the proxy support is in place but
    not integrated into the ECF builds because org.eclipse.core.net as a
    dependency on org.eclipse.core.runtime. <DJ> is looking into this
    problem. It is being tested by Ryan, an ECF committer, but it needs
    wider testing from the commnuity.

OS level proxy setting detection. Stefan has code for windows.

  - Authentication (basic login/pwd prompting) - ECF has its own API for
    authentication and it is similar to JAAS. p2 will connect to the ECF
    APIs <Tim M>. We have to be careful to the user model.

<!-- end list -->

  - Based on Tim M. patch and request, ECF has added support for
    timestamp. <Tim M>

<!-- end list -->

  - Secure connection (SSL). We need to support SSL, however we have to
    be careful to the user model since we don't want to prompt the user
    for certificate info when it is not expected (i.e. download in the
    background). Matt suggested that we could never prompt and use the
    import / export trust UI out of the security work. \<Tim M.\>

<!-- end list -->

  - Download manager / mirrors. The implementation of a smart download
    manager leveraging mirrors will be implemented behind the artifact
    repository. This allow the algorithm to get access to transport
    level information. Some details to be seen around what happens to
    the output streams. \<Tim W.\>

<!-- end list -->

  - Mirrors. A repository will have a list of mirrors. The list of
    mirrors can be obtained from the original repository but it could be
    overwritten/expanded by data provided to the agent. Tim proposed the
    idea of a mirror service. It may also be necessary to be able to
    sort artifact repos.<Pascal>

### Build

When: Wednesday 3-4

Who: Andrew N, Andrew O, Pascal

What:

  - What do we build from?
  - What is being produced?
  - Product build
  - Packager

Minutes:

Update Sites

  - people have published update sites and we need to convert these
    sites
  - will have a mechanism to connect to an old update site and create
    metadata on the fly
  - this works for most sites (maybe not ones with install
    handlers...still investigating)

<!-- end list -->

  - new format
      - p2 requires new format for an update site (replace site.xml)
      - need to produce these new files
      - also need to produce stand-alone downloads (things the user can
        download and run)
      - for the shared install case we need to produce bundles.txt files
        representing a list of bundles

UpdateSiteGenerator (Convertor?)

  - new tool that we need to build
  - can build p2 site from scratch (not based on update site site.xml)
  - can consume JARs or JARs and packed files or an update site
  - how does this fit in with the SiteOptimizer?
  - can run the old tool then this new tool, or you can just run the new
    tool
  - doesn't matter, its up to the individual build teams to just build
    the pieces
  - this tool should be able to do everything that the SiteOptimizer
    currently does so we don't require people to use the old tool
  - tool is really a convertor, publisher, optimizer
  - should be able to generate deltas, sign the JARs, etc
  - overwrite or append by default?
  - if we don't specify anything repository arg, do we publish in-place?
  - need to be able to process a zip and generate p2 metadata and inject
    those files into the original zip
  - besides creating the files on the side
  - another reason to munge zip files is because some people don't use
    pde/build for their builds

Tooling

  - on the tooling side, we will not have any special editor for the
    sites... people will use the site.xml editor
  - user experience is the same
  - there are some p2 options but mostly they will be non-envasive
    checkboxes, etc.

From the tooling perspective...

  - when you export does it do the right thing?
  - does it export p2 data since we are in a p2 configuration?
  - we can currently sign JARs when you export
  - do we want to use the SiteOptimizer as part of the build?
  - need to add options to the site editor for signing, etc.
  - should "generate p2 metadata" be on by default?
  - does this apply to exporting features and plug-ins too or just
    building update sites?
  - in update manager, a repository has a site.xml and special format,
    but in p2 we have expanded the definition to be zips, etc.
  - we need to decide the format of these xml files that we are
    exporting
  - what do we do if a content.xml already exists?
  - overwrite or append? overwrite
  - when we create an MD5 we have to ensure that its created for the
    right thing (unsigned, signed, packed, etc)

Product Builds

  - what does a product look like?
  - a product file contains information that makes a group of plug-ins a
    product
  - need to be able to generate a product IU (identify the product-ness
    of what we are shipping)
  - input: product file, output: an IU or set of IUs
  - need something which will take the metadata for a product, and call
    the director and install it and zip it
  - this is what we currently do in the p2 builds to produce the
    agent.zip (via Ant calls)

<!-- end list -->

  - pde/build currently makes a product zip directly, now we will make a
    product zip based on this repo
  - pde/build always generates an update site, and then there is a step
    at the end that can install a product and zip it up
  - generator still needs to be changed to ensure that it will generate
    the right metadata to produce the right shape

<!-- end list -->

  - would like to re-use metadata that has already been produced
  - would like to have access to the metadata repository which is
    representing what is in your target platform
  - I'm building bundle/feature "foo", then go and check if we already
    have the metadata for it
  - if you have a pre-defined IU, it is a development time thing and
    already has a qualifier, etc
  - this information might be different from the final build result

Generating p2 Metadata from a ZIP

  - how can we p2'ize a zip file produced by another build process?
    (non-pde/build product build)
  - the SDK is an example
  - currently we take the SDK and the delta pack and have lots of
    special code which handles root files and natives, etc
  - tool will have to take multiple zips (one for each platform, for
    instance) as input
  - will have to have user interaction asking for the product IU, the
    name of the executable, etc.
  - could merge all features and plug-ins and create metadata for them
  - all other files would go into an IU (one per platform)
  - create a top-level IU as the install root

Ant Tasks

  - we have Ant tasks for generator and director
  - had problems since they require system properties and we are calling
    them multiple times
  - also some p2 related problem because of metadata
  - should be doing an exec
  - maybe the tasks themselves should call exec

Mirroring

  - should investigate having a mirroring tool
  - would be useful for gaynamede

Packager

  - what are the implications of p2 on the packager?
  - we still need to talk about this

Creating features

  - spoof up features from IUs
  - we have decided to always ship features
  - shouldn't be too much of a problem since pde/build can produce
    features for us
  - headless build is driven by features

## Thursday

### API

When: Thursday 9-11

Who: Jeff, Susan, John, Pascal, Dave, Simon, DJ

What:

  - Java API
      - Mark all p2 Java API as x-internal and internal.provisional for
        1.0

<!-- end list -->

  - Extension points
      - Document extension points as internal use only

<!-- end list -->

  - Data files
      - content.xml, artifact.xml, bundles.txt, and other file formats
        are not API, but we must handle compatibility. Future releases
        will be required to understand data files produced in p2 1.0.

<!-- end list -->

  - Localization data <Dave>
      - Files containing localized strings must be in a standard format
        that does not change (likely Java properties file format).

<!-- end list -->

  - A method for calling the UI
      - May need either a small Java API or a command handler that
        clients such as RCP applications can hook to open the p2 dialog.

<!-- end list -->

  - Ant tasks and applications used at build time (p2-izer, director
    app, etc)
      - These are not strictly API. Clients will use them, but we
        reserve the right to modify/remove in the future as needed.

<!-- end list -->

  - How many bundles
      - Keep roughly the same bundle separation we have today
      - There are a few bundles that can be merged
      - Some bundles are just tooling, or build-time functionality that
        won't be in SDK.

### Plan for SDK integration

When: Thursday 1-3

Who: Jeff, John, Pascal

Where:

`866-362-7064 (toll-free North America)`
` or (+1) 613-287-8000 (Ottawa and international) participant passcode 892048#`

What:

  - In M5, p2 will provide an installer to install the SDK.

<!-- end list -->

  - Functionality advertised
      - The new UI needs to allow for installation from old update
        sites.
      - PDE UI would be fine
      - PDE build would produce p2 repos when you export through the UI.
        What is the format of the repo? Where do the repo files go?
      - We will be able to update from the I builds.
      - The installer may be easier to do because this would limit the
        massaging of the build.

<!-- end list -->

  - Need to ensure all Eclipse project automated tests pass when
    platform is provisioned by p2.

<!-- end list -->

  - Things to be addressed / discussed
      - Drop-ins folder may not be supported
      - Replacing plug-ins from an install with newer version
      - Controlling the JRE being used
      - Need change the format of the install registry to store the IUs
        in a metadata repo.

### No meeting assigned

  - Robustness
      - recovery
  - Resolver
  - Fwk admin
      - What do we do with it?
      - Will PDE need to use it? Yes.

[Category:Equinox_p2](Category:Equinox_p2 "wikilink")