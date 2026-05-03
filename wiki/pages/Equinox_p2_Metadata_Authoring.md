This page describes the design of the proposed p2 meta data authoring
project.

# Metadata authoring proposal

The problems this project should solve:

  - Many types are automatically "adapted" to installable units (e.g.
    features and plugins), but there is no direct authoring available.
  - Existing "adapted" installable units may have meta data that needs
    to be modified/augmented in order for the unit to be useful - there
    is a mechanism available to provide advice to the resolution
    mechanism, but authoring of such advice is not.

I think the work consists of:

  - An XML definition for "authored installable unit" (most likely the
    <unit> format in the local meta data repository). It should be
    possible to create such an "iu" file with any name without
    requirements that it is in some special project or folder.
  - An XML definition for "advice to adapted installable unit" (as I am
    not sure what is needed here, a separate definition is perhaps
    needed).
  - An editor for Installable Unit
  - Ad editor for Installable Unit advice (is perhaps the same editor).
  - A way to test the installable unit (resolution, install-ability)
  - A way to export/publish installable units

# Files and Formats

A new Installable Unit file can be created with the New IU Wizard.

![Image:p2authWiz.png](images/p2authWiz.png "Image:p2authWiz.png")

It is possible to browse for the container, and then create the file:

![Image:p2authNewIU.png](images/p2authNewIU.png "Image:p2authNewIU.png")

## The format of the "IU" file

It seems obvious to reuse the xml format from the local meta data
repository as it already describes an installable unit.

    <?xml version='1.0' encoding='UTF-8'?>
    <?InstallableUnit class='org.eclipse.equinox.internal.p2.metadata.InstallableUnit' version='1.0.0'?>
    <installable version="1.0.0">
         <unit>
               <!-- like local metadata repository for 'unit' element -->
         </unit>
    </installable>

In order to make it easier to reuse the parser/writer for the local
metadata repository, a new root element "<installable>" is used to wrap
a single "<unit>" element. This also opens up the possibility to include
other elements (than unit) if required.

## Naming convention

To make it easy to detect the "IU" files an ".iu" extension should be
required.

## Writer/Parser design

It was possible to crete a prototype that read and writes this format by
reusing the metadata repository XMLReader and XMLWriter. It is however a
bit unclear if the intention is to allow the InstallableUnit to be used
by an editor, or if these classes should be considered internal/private.

# The IU Editor

The IU editor is written with PDE Forms. The editor has support for:

  - Validation of (most) fields with interactive error reporting
  - Undo/Redo support that also moves focus to the modified page/field

## Overview Page

![Image:p2authOverviewPage.png](images/p2authOverviewPage.png
"Image:p2authOverviewPage.png")

### Namespace

The namespace is the naming scope of the installable unit's name. From
what has been understood by looking at some installable, these are
examples of namespaces:

  - org.eclipse.equinox.p2.iu
  - org.eclipse.equinox.p2.eclipse.type
  - osgi.bundle
  - java.package
  - org.eclipse.equinox.p2.localization

#### Namespace Questions

  - Should the user be allowed to type anything in the namespace field?
  - If not allowed to type anything - where are the valid namespaces
    found?
  - Can the set of namespaces be extended?
  - Does namespace have a valid format (looks like it follows package
    name format)?
  - Can namespace be left empty?
  - There are two namespace properties - one called namespace_flavor,
    and one namespace_IU_ID - is there a description of how these are
    used?

#### Namespace Implementation

  - Namespace is required
  - The namespace field is validated as a structured name (i.e. like a
    java package name)

### ID

#### ID Questions

  - What is ID - how is it different from name?
  - What is the valid format?
  - What determines the format - can it be different in different name
    spaces?

#### ID Implementation

  - ID Is required
  - Any string input is accepted

### Name

#### Name Questions

  - What is the valid format of a name?
  - Is format determined by p2 for all namespaces, or can name format
    vary between different namespaces?
  - If it can vary, how is the namespace/name validation extended to new
    namespaces?

#### Name Implementation

The assumption is that the name should follow structured java naming -
i.e. a pattern that:

  - name can consist of multiple parts where parts are separated by a
    "."
  - each part must be at least one char long
  - each part must begin with a-zA-Z_$
  - subsequent chars in a part can be a-zA-Z0-9_$

### Version

#### Version Implementation

  - Version is an OSGi version and is validated as one.
  - Version is required.

### Provider

Provider is an optional information about the provider (name of
organization or individual) providing the unit.

#### Provider Implementation

  - Optional string value. No validation.

### Filter

This is assumed to be an LDAP filter expression.

#### Filter Questions

  - Is the assumption correct (LDAP filter expression)?
  - Is it meaningful to provide structured input of the filter (i.e.
    like feature editor with separate sections for platform, arch,
    window system, arch and language)?
  - is p2 open and can filter on an expandable set of variables, or is
    it a fixed set (platform, arch, window system, arch and language)?

#### Filter Implementation

  - The field uses a LDAP filter validator that conforms to the RFC 2254
    for textual representation of LDAP filter with the following
    exceptions:
  - OCTAL STRING input is not handled
  - attribute options (e.g.";binary") is not handled
  - use of extensions or reference to LDAP matching rules is not handled
  - (currently) the attribute names are restricted to US ASCII letters,
    digits and the hyphen '-' character and '.' to separate parts in a
    structured name. (RFC 2254 specifies that ISO 10646 should be used,
    and that "letters", "digits" and "hyphens" are allowed.)

# Required Capabilities

A list of required capabilities is shown and the user can add/remove,
and move items up/down. Selecting an item opens the detail editor. Icons
are set based on namespace. There is currently no repository lookup.

![Image:p2authReqCap.png](images/p2authReqCap.png "Image:p2authReqCap.png")

# Provided Capabilities

This is implemented in the prototype by showing a list of provided
capabilities. Entries can be added/removed, and moved up/down and
edited.

![Image:p2authProvidedCap.png](images/p2authProvidedCap.png
"Image:p2authProvidedCap.png")

# Artifact Keys

Artifact keys can be added/removed, and moved up/down and edited. There
is currently no repository lookup.

![Image:p2authArtifactPage.png](images/p2authArtifactPage.png
"Image:p2authArtifactPage.png")

#### Artifact Questions

  - What is "Classifier" - can this be a drop down list?
  - What determines what the classifier can be?
  - What determines its format? - it is now validated as a structured
    name

# Information

The editor has editing of the information copyright notice, license
agreement, and description. Some boilerplate text is inserted as a
starting point.

![Image:p2authInfoPage.png](images/p2authInfoPage.png
"Image:p2authInfoPage.png")

# Touchpoint Page

An installable Unit can be installed into one touchpoint. The IU meta
data consists of a reference to the touchpoint (Touchpoint Type), and
describes a set of actions/instructions to execute on the referenced
touchpoint. Currently, two touchpoint types (native, and eclipse/osgi)
have been implemented. The native touchpoint has aprox 5 different
actions, and the eclipse touchpoint has aprox 20. Some of these actions
take parameters.

Here is a list of the [actions per
touchpoint](Equinox/p2/Engine/Touchpoint_Instructions "wikilink")

The editor allows blocks of instructions to be added (touchpoint data),
and each such instruction block allows editing of actions per
instruction (aka. phase). There is meta data that describes the
touchpoints available (native 1.0.0, and eclipse 1.0.0) which makes it
possible to show better labels, list available actions etc.

![Image:p2authTouchpoint1.png](images/p2authTouchpoint1.png
"Image:p2authTouchpoint1.png")

Instructions and actions are added by pressing add, and selcting in the
popup menu that appears:

![Image:p2authAddpopup.png](images/p2authAddpopup.png
"Image:p2authAddpopup.png")

The available actions are displayed, and user can select the action to
add.

![Image:p2authSelectAction.png](images/p2authSelectAction.png
"Image:p2authSelectAction.png")

If the user switches between touchpoint types - I decided to keep
actions previously added (the alternative would be to remove actions
that does not apply). Instead a message is displayed above the area for
editing the action. (I really wanted to use the same error reporting as
used elsewhere, but ran into a design flaw that made it difficult to
keep this paricular type of problem in sync).

![Image:p2authTouchpointWithError.png](images/p2authTouchpointWithError.png
"Image:p2authTouchpointWithError.png")

#### Handling of Unknown Touchpoint Type

The editor handles an IU file with an unknown touchpoint type by
allowing editing of existing actions, but not adding new. Actions for an
unknown touchpoint type are shown with the parameter names instead of
formatted labels. When the original IU has an unknown type, that type is
selectable in the type box (this to support a user trying to change the
touchpoint type to something else and realizing that it is best to stick
with the original). If the file is saved with a known touchpoint type,
it is not possible to get the unknown type back again without editing
the XML directly.

#### Touchpoint Questions

  - Do you think it is ok to keep actions and instructions when
    switching touchpoint type even if they do not apply, and let the
    user delete them, or should all non applicable instructions and
    actions be removed (undoable))?

#### Touchpoint Notes

  - p2 has a bug (link to issue TBD) that merges multiple instrucion
    blocks into one when reading the meta data. (The editor is not to
    blame if you run into this problem)
  - There is an enhancment request logged (link to issue TBD) - for
    being able to save the name of an instruction block (aka
    TouchpointData). The editor assigns labels called "Instruction block
    n" where n is the block number.
  - There is an issue with parameter values containing "," that causes
    p2 meta data to go off track. The editor therefore filters out all
    "," characters from parameter value fields for actions.
  - There is no validation of input fields for actions - this is
    somewhat difficult as runtime substitution of "${variable}" is
    supported.
  - Actions can be moved up/down within an instruction.
  - Instructions can not be moved - the editor displays all
    phases/instructions (empty instructions are not written to XML)
  - Instruction blocks can be moved up/down

# Update

The editor has an "Update" page where information about what IU's the
specified IU is an update of.

![Image:p2authUpdatePage.png](images/p2authUpdatePage.png
"Image:p2authUpdatePage.png")

#### Update Questions

  - What is the valid values for severity?

#### Update Implementation

  - The severity can now be set to a value \>= 0
  - The description field should probably be longer.

# TODO

  - lookup of required capabilities from meta data repositories
  - lookup of artifacts from artifact repository
  - lookup of things from workspace
  - handle persistent problem markers (at least managed by the editor)
  - "test/build/install"
  - handle fragment
  - handle patch
  - Are there additional properties that should be editable (lock?
    contact email?)

[Metadata Authoring](Category:Equinox_p2 "wikilink")