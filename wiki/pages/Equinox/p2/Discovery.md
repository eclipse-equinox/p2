## Description

The purpose of the P2 Discovery mechanism is to provide a simplified and
branded front-end for the P2 provisioning platform. Discovery can be
used as a

  - tool, to display and install from existing P2 repositories
  - tool, to display and install from catalogs that point to P2
    repositories
  - framework, to build branded installer UIs

### Branded presentation of a P2 repository

In this mode discovery acts as an alternative front-end to the P2
install UI without advanced features such as update site selection,
grouping etc. All displayed meta-data comes from a single P2 repository.
This is mainly useful for providing a UI for installing and updating
extensions in RCP applications where all relevant features and meta-data
are available from a single repository.

### Catalog presentation of several P2 repositories with custom meta-data

In this mode discovery displays a catalog of items that point to an
installable feature and P2 repository where that feature is available.
This supports installing extensions where the the displayed meta-data is
potentially different from what is presented in the P2 repository and
has more details that are relevant at install time (e.g. price,
screenshot, update sites).

### Existing Implementations

  - [Mylyn Connector
    Discovery](http://wiki.eclipse.org/Mylyn/Discovery): Catalog
    presentation of Mylyn task repository connectors.

<!-- end list -->

  - [Subversive SVN Team Provider Connector
    Discovery](http://www.polarion.com/products/svn/subversive/connector_discovery.php):
    Catalog presentation of Subversive extensions.

<!-- end list -->

  - [Eclipse Marketplace Client](http://www.eclipse.org/proposals/mpc/):
    Custom catalog that is accessed through a web service.

## Getting Discovery

### Binaries

The Equinox P2 Discovery feature is available from the Helios and
platform update sites:

  - <http://download.eclipse.org/releases/staging/>
  - <http://download.eclipse.org/eclipse/updates/3.6milestones/>

### Sources

Sources are in the P2 CVS repository: [Instructions for getting
started](http://wiki.eclipse.org/Equinox_p2_Getting_Started_for_Developers)

  - org.eclipse.equinox.p2.discovery: Core data model.
  - org.eclipse.equinox.p2.discovery.compatibility: Support for
    extension-point based catalogs.
  - org.eclipse.equinox.p2.tests.discovery: Tests.
  - org.eclipse.equinox.p2.ui.discovery: Discovery commands, wizard and
    viewer. P2 integration.

## Integrating Discovery

  - Displaying a P2 repository:

`Catalog catalog = new Catalog();`
`catalog.setEnvironment(DiscoveryCore.createEnvironment());`
`catalog.setVerifyUpdateSiteAvailability(false);`

`// add strategy for retrieving remote catalog`
`RepositoryDiscoveryStrategy strategy = new RepositoryDiscoveryStrategy();`
`strategy.addLocation(new URI("`<http://location/of/p2/repo>`"));`
`catalog.getDiscoveryStrategies().add(strategy);`

`CatalogConfiguration configuration = new CatalogConfiguration();`
`configuration.setShowTagFilter(false);`

`DiscoveryWizard wizard = new DiscoveryWizard(catalog, configuration);`
`WizardDialog dialog = new WizardDialog(WorkbenchUtil.getShell(), wizard);`
`dialog.open();`

  - Displaying a directory:

`Catalog catalog = new Catalog();`
`catalog.setEnvironment(DiscoveryCore.createEnvironment());`
`catalog.setVerifyUpdateSiteAvailability(false);`

`// look for descriptors from installed bundles`
`catalog.getDiscoveryStrategies().add(new BundleDiscoveryStrategy());`

`// look for remote descriptor`
`RemoteBundleDiscoveryStrategy remoteDiscoveryStrategy = new RemoteBundleDiscoveryStrategy();`
`remoteDiscoveryStrategy.setDirectoryUrl("`<http://location/of/directory.xml>`");`
`catalog.getDiscoveryStrategies().add(remoteDiscoveryStrategy);`

`CatalogConfiguration configuration = new CatalogConfiguration();`
`configuration.setShowTagFilter(false);`

`DiscoveryWizard wizard = new DiscoveryWizard(catalog, configuration);`
`WizardDialog dialog = new WizardDialog(WorkbenchUtil.getShell(), wizard);`
`dialog.open();`

## Extending Discovery

All discovery classes are in internal packages and marked as x-internal.
Still discovery is designed with extensibility in mind and integrators
are encouraged to provide feedback.

The discovery core is designed to allow clients to implement custom
strategies for populating catalogs.

`AbstractDiscoveryStrategy`: Base class for implementing strategies that
populate catalogs. Reference implementations are
`RemoteBundleDiscoveryStrategy` (extension point based catalog) and
`RepositoryDiscoveryStrategy` (P2 repository based catalog).

The UI is composed of extensible components.

`DiscoveryWizard`: Provides a wizard based workflow for installing
extensions that embeds `CatalogViewer` in a wizard page.

`CatalogViewer`: A list based viewer with custom controls for displaying
and installing catalog items.

## Plan

Discovery will be shipped as part of Helios. The first version is
expected for Helios M6.

[All discovery
bugs](https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced;short_desc=discovery;short_desc_type=allwordssubstr;component=p2;product=Equinox)

  - : \[publisher\] Integrate branding icon in the metadata

[Category:Equinox_p2](Category:Equinox_p2 "wikilink")