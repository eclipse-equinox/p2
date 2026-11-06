This document describes some of the conceptual differences between
Equinox [p2](p2 "wikilink") and the original Eclipse provisioning
solution, known as *Update Manager*. This document focuses on
differences that may be of interest in the context of treatment of
licenses and end user agreements. This is a technical document that does
not specifically address legal issues.

# Update Manager

Update Manager (UM) was the install/update technology used in the
Eclipse platform from the 2.0 through 3.3 releases. UM had the following
basic characteristics:

  - The atomic unit of installable or updateable function is called a
    *feature*. Features are referenced by a symbolic string id and four
    part version identifier just like an Eclipse plug-in.
  - A feature can *require* or *include* other features, and/or other
    plug-ins. Require/Include clauses referenced the symbolic id of the
    feature or plug-in it required/included
  - When a feature is installed, all included features and plug-ins are
    installed automatically. Required features/plug-ins had to be
    explicitly selected by the end-user (although UM had a facility to
    "guess" and auto-select required features in the install wizard at
    install-time).
  - Features contain translatable license text suitable for displaying
    license terms, or click-through agreements that must be accepted
    prior to installing.
  - When installing a feature, the license agreement(s) for the features
    chosen by the user are presented prior to install. License
    agreements for *included* features are never shown.
  - Features are installed from Web sites known as *update sites*.
    Update sites are identified by a URL, and contain some collection of
    features and plug-ins.
  - Features can reference update sites in two ways:
      - Features can reference the URL of an *update site*, which is the
        site that is used when a feature is updated.
      - Features can reference the URLs of *discovery sites*. These URLs
        are made available to the end-user. They typically contain other
        content that may be of interest (but is not strictly required),
        by users of a feature.
  - Install or update operations happen in the context of a specific
    single update site. For installs, the user selects the update site
    to install from. For updates, the site to use for the update is
    specified by the feature.
  - An update site can specify additional sites called *associate sites*
    that may provide content during an install initiated at that site.

# p2

[p2](p2 "wikilink") is the install/update technology used by the Eclipse
platform since the 3.4 release. p2 has absolutely no dependency on
update manager code. It is a complete stand-alone implementation of an
Eclipse provisioning solution. p2 has the following basic
characteristics:

  - The atomic (and only) unit of installable or updateable function is
    called an *installable unit* (IU). Installable units have a symbolic
    string id and a version identifier.
  - IUs have *provided capabilities* and *required capabilities*. A
    capability is a generic concept represented by a String namespace, a
    String name, and a version number. A short-hand notation for writing
    capabilities is the form "namespace/name/version". Some examples of
    capabilities:
      - java.package/org.xml.sax/1.0 - a capability that describes
        version 1.0 of the Java package 'org.xml.sax'
      - osgi.bundle/org.eclipse.swt/3.4.0.v20080630 - a capability that
        describes version 3.4.0.v20080630 of the OSGi bundle with id
        'org.eclipse.swt'.
  - IUs do not typically reference other IUs directly (they can, but
    this is the exception rather than the norm)
  - When an IU is installed, a resolution engine examines the required
    capabilities of the IU, and attempts to find other IUs that provide
    capabilities matching its required capabilities. This is repeated
    until a completely satisfied system is found.
  - IUs contain translatable license text suitable for displaying
    license terms, or click-through agreements that must be accepted
    prior to installing.
  - When installing a set of IUs, the license text for all IUs being
    installed is shown and must be accepted by the end user.
  - IUs are installed from repositories that are typically Web sites,
    but could also be local files, databases, or other storage
    mechanisms. Repositories are identified by a URI (Uniform Resource
    Identifier).
  - p2 maintains a set of active repositories that may all be consulted
    when a provisioning operation occurs. Provisioning operations are
    not executed in the context of a specific repository.
  - Repositories can specify the URIs of additional repositories, that
    are automatically added to p2's set of active repositories.
  - The end-user does not select or potentially even know which
    repositories are involved in any given install or update operation.

# Important Differences

The following are some important behavioural differences between UM and
p2:

## Links between units of installable content

In UM, features explicity referenced other features and plug-ins by
name. Installation worked somewhat like a phone book - the referenced
entity was looked up by name and was unique. In p2, IUs only refer to
generic capabilities. A resolver matches provided capabilities from one
IU to required capabilities of another. This works like a broker - you
know the kind of thing or service you want, and the broker finds a
provider that meets your needs. There is no direct link between provider
and consumer.

## User control of install locations

In UM, the user must specifically choose the URL of a site to install
from. In p2, the user chooses only what they want to install, and p2
will endeavour to install it using all repositories it has available. A
user of p2 may never know where the content they are installing is
coming from. The user can typically see and manage the set of available
repositories, but a product administrator may lock this down to prevent
the user from modifying the set of repositories.

## Variety of installable content

UM could only install features, and features could only contain other
features, or Eclipse plug-ins. p2 can install any set of bytes:
plug-ins, features, native code such as application launchers, readme
files, etc. An IU may not in fact contain any bytes to be installed. An
IU can simply set an OS registry key or system property and not actually
lay down any files.

## Granularity of installable content

Again, UM can only install features, which must contain license text. p2
can install at any granularity, including individual plug-ins, which
today do not typically carry license text.

## Bundle pooling

UM mostly stores plug-ins in the "top-level (root) directory" of eclipse
(e.g. c:\\eclipse\\). p2 promotes an approach called bundle pooling
where several instances of eclipse can share their plug-ins into a
common location independent of the "top-level directory".

[Legal Issues](Category:Equinox_p2 "wikilink")