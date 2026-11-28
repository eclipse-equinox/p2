This page captures interesting changes each week to the [Equinox
provisioning](http://www.eclipse.org/equinox/incubator/provisioning)
effort going on in the [Equinox
incubator](http://www.eclipse.org/equinox/incubator/).

*Note:* Status updates are now provides in the minutes of
[Equinox/p2/Meetings](Equinox/p2/Meetings "wikilink")

### Week of 20080310

  - Integration of SAT4J
  - Bug fixing in Mac builds
  - Working on getting SDK tests running on p2

### Week of 20080303

  - Integrated into the Eclipse SDK (yay\!)
  - Refining support for dropins, different shapes in dropins
  - Repository refresh API
  - Repository event API
  - Support for adding folders and update sites

### Week of 20080224

  - Moved graduated p2 bundles to new [Equinox CVS
    Structure](Equinox_CVS_Structure "wikilink")
  - Moved p2 bugs to new Equinox p2 bugzilla component
  - Complete review of metadata formats, namespaces, ids
  - Switched groups and fragments to be identified via properties rather
    than capabilities
  - Testing and bug fixing on dropins and update site compatibility
    support

### Week of 20080217

  - New install registry format
  - Support for hiding UM UI contributions when p2 is installed
  - Support for building the Eclipse SDK using p2
  - Ongoing work on moving to SAT4J resolver
  - Some refactoring/deletion of small bundles
  - Refining mirror support

### Week of 20080211

  - Rename of API packages to internal.provisional
  - Multi-threaded downloads
  - Mirror support

### Week of 20080107

  - John is testing this week
  - UI performance
      - lazy repo loading by all UI model elements
      - end user UI gets summary repo info from manager and fetches rest
        in background
      - tweak UI queryable interfaces to avoid reaching for repo or
        profile too often
  - Finished up compression of metadata and artifact repository content
  - Finished up lazily populating and flushing profile/install registry
  - Investigating signature verification, checking certificate trust
  - Investigating local caching of metadata repositories

### Week of 20071231

  - Reducing retained memory footprint
      - Released changes in artifact repository manager and install
        registry to do lazy loading and soft references.
      - Released changes in UI to avoid retaining references to Profile
        and repository objects (mainly admin UI).
  - Making progress on support for interacting with legacy update sites
  - Adding support for compression of metadata repositories
  - Metadata API cleanup, addition of factory methods and singletons to
    reduce duplication
  - Bug fixing
  - UI performance
      - All model elements now reference profile id's, not profile
        instances
      - Sizing phase run in the background for install dialog

### Week of 20071210

  - Prepared M4 deliverable

### Week of 20071203

  - Dave is testing this week using [Equinox p2
    tests](Equinox_p2_tests "wikilink")
  - UI
      - move all profile modification dialogs to wizards and use update
        manager wizard graphics
      - license UI and simple hook for remembering licenses
      - UI for revert in the end user UI

### Week of 20071126

  - Susan is testing this week
  - Support for update site categories
  - Profiling and optimization of memory usage
  - UI - "remind me later" options for automatic updating

### Week of 20071119

  - Andrew O is testing this week
  - Investigating SAT-solvers to replace director resolution algorithm
  - New IQueryable API and implementation
  - Various API cleanup
  - Working on garbage collection
  - Review/pruning of 1.0 plan
  - Improved sorting/grouping of IUs in end user UI
  - Bundle pool as real artifact repository
  - Implementation work on directory watcher
  - UI defines most viewer content in terms of queries

### Week of 20071112

  - Pascal is testing this week
  - UI work on Available IU presentation/navigation
      - sorting, duplicates, categories
      - investigating using IQueryable for client definition of content
        providers (groups, categories, etc.)
  - Initial work on artifact GC
  - Exploration with pseudo boolean SAT solver. Discussion with SAT4J
    community.
  - API cleanup.
  - New query API.

### Week of 20071105

  - Completed the [Equinox p2 1.0
    Features](Equinox_p2_1.0_Features "wikilink") sheet.
  - Completed the [Equinox p2 1.0 Technical
    Specs](Equinox_p2_1.0_Technical_Specs "wikilink").
  - Investigate using SAT solvers.
  - API cleanup started.
  - UI cleanup and performance investigations (deferred content
    providers, etc.)

### Week of 20071129

  - Prepared M3 deliverable

### Week of 20071015

  - Refinement on the questions [Equinox Provisioning Plan\#Questions
    for M3](Equinox_Provisioning_Plan#Questions_for_M3 "wikilink").
  - Patch prototyping the governor to restart the discussion in that
    space.
  - Work on the 3.4 feature list to be circulated among us next week.
  - Support for cross-platform provisioning from a single repository
  - Finished removal of dependency on Javascript
  - Support for running generator against update site
  - Refinement of UI workflows
  - Artifact repository optimizer for supporting pack200

### Week of 20071008

  - Simple SWT-based installer
  - Overhaul of director/planner API and implementation
  - Provisioning symposium at [Eclipse Summit
    Europe 2007](http://eclipsesummit.org/summiteurope2007/)
  - Working on replacement for Javascript in the Engine
  - Working on replacement for XStream

### Week of 20071001

  - Renamed bundles and packages from org.eclipse.equinox.prov.\* to
    org.eclipse.equinox.p2.\*
  - Overhaul of artifact/metadata repository API
  - New director API to decouple director from engine and allow
    introspection
  - Discussion and rethinking of entry-point concept

### Week of 20070924

  - Most p2 committers at [Equinox Summit
    2007](Equinox_Summit_2007 "wikilink")
  - 3.4 M3 planning

### Week of 20070917

  - Resolved remaining issues with absolute paths
  - Prepared M2 deliverable (agent, and metadata/artifacts for Eclipse
    SDK 3.4 M2)

### Week of 20070910

  - Separation of properties views in admin and end-user UI
  - UI support for update from admin and end-user UI

### Week of 20070903

  - UI support for colocated repositories
  - UI uses install oracle to prequalify an install
  - Branding of eclipse.ini/eclipse.exe files
  - Generate proper metadata and artifacts for a JRE
  - Investigation of running p2 in a Foundation 1.1 environment
  - Review and cleanup of artifact/metadata repository APIs
  - UI generates entry point IU's on install

### Week of 20070827

### Week of 20070820

  - Better error reporting for unsatisfied dependencies from director
  - Initial support for rollback
  - Initial detection of update between states and improved computation
    of operations
  - Initial support for entry points
  - Initial oracle API to allow for filtering of non-installable things
  - Initial UI with end user workflow (incomplete)

### Week of 20070813

  - Added support for selectors (see
    [bug 200104](https://bugs.eclipse.org/bugs/show_bug.cgi?id=200104)
    for details)
  - Discussion on role of the director on equinox-dev
  - Discussion on the [role of the
    engine](http://wiki.eclipse.org/Equinox_Provisioning_Engine)
  - Discussion and implementation of post-processing of downloaded
    artifacts (see
    \[<https://bugs.eclipse.org/bugs/show_bug.cgi?id=197644> bug 197644
    for details)
  - Support for keeping track of the resolved state of IUs
  - Support for multiple versions and singletons in the dependency
    expander
  - Discussions of update/install UI workflows from other RCP apps
  - Refactor UI plug-ins to separate common code, end-user UI, and admin
    UI.

### Week of 20070806

  - Added automated director tests
  - Released support for platform filters
  - Improved progress reporting and cancelation
  - Design discussions on support for shared installs
  - Prepared and release Provisioning M1a containing Eclipse SDK 3.4 M1

### Week of 20070730

  - Created build scripts for building provisioning metadata and agent
  - Prepared and released [Equinox p2 M1](Equinox_p2_M1 "wikilink")

### Week of 20070723

  - Created administrator RCP application (agent UI)
  - Created UI views for browsing and manipulating metadata
    repositories, artifact repositories, and install profiles

### Week of 20070716

  - Investigation on hooking the metadata generation in the SDK build
  - Refactor closure computation of director into a specific class
  - Implement a new algorithm for the closure computation, however
    recommendations have not been moved there
  - More work on the uninstall for optional IUs
  - MetadataHelper replaced with query facility
  - Initial commit of the UI work
  - Setup build infrastructure
  - Wrap MD generator in ant task

### Week of 20070709

  - Initial implementation of the recommendations (aka constraints)
    released. See doc
    <http://wiki.eclipse.org/Equinox_Provisioning_Recommendation_descriptors>.
  - Initial release of the write API for metadata repository
    (https://bugs.eclipse.org/bugs/show_bug.cgi?id=194674) and starts
    usage of it
  - First version of uninstall working
  - Add progress monitor for UI work
  - Setup of build

### Week of 20070702

  - Combined IDependency and RequiredCapability into one interface
  - Refactored NameBasedDependency and FilterBasedDependency into
    RequiredCapability objects
  - Fixed various bugs with filters on RequiredCapability, and added
    filtering JUnit tests
  - Implemented full translation of version ranges into filters

[Status](Category:Equinox_p2 "wikilink")