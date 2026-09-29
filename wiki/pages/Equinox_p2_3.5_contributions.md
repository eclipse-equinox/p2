The content of this page is superseded by the Galileo feature list
[1](http://wiki.eclipse.org/Equinox/p2/Galileo/Features)

## IBM

  - **Metadata improvement**
    Provide additional metadata construct to separate the line-up
    information from the raw dependency information.

<!-- end list -->

  - **API**
    Define an API for p2 and provide some reusable UI building blocks

## Cloudsmith

  - **[Meta data
    authoring](Equinox_p2_Metadata_Authoring "wikilink")**
    **Remote repositories**
    reference to wiki page TBD - includes search/browse/publish
  - **Installer**
    Buckminster has an installer, seems a waste to have duplicate
    efforts.
  - **Inclusion of more links in IU**
    We think it is a good idea to include more "bookmarks" (in the form
    of an OPML) into an IU - this to enable links to not only License
    and Copyright, but any set of useful links and RSS feeds to
    information that a publisher sees fit. We have an implementation of
    this in Buckminster that can be modified and contributed.
  - **installation/materialization of source/projects**
    Buckminster has this, again, seems a waste to have duplicate
    efforts.
  - **version type support**
    Currently p2 supports only OSGi type versions, we think versions
    needs to be typed (OSGi, Triplet,...). We would like to contribute
    the implementation (if someone else does not add this), as we want
    to be able to use p2 for technologies other than OSGi.

## Siemens AG

  - **Better RCP support**
    It's is great that 3.4 can create p2 repositories in the product
    export wizard. Whas's missing is PDE support for the director
    application. Suggestion: Include a "Create installation from created
    repository" that prepares all the infrastructure for self-updating
    of an exported RCP application
  - **Provide more customizable end user UI**
    The current software update dialogs used in Eclipse have great
    potential to be reusable in domain specific RCP applications. It
    would be great if things like "Add update site" could to be
    optional.

## Genuitec, LLC

  - **Tentative: Resolution error reporting improvements (p2 & Sat4J
    integration)**
    We would like to improve the error reports that p2 returns when a
    resolution failure occurs. This includes reducing the problem to a
    minimum error problem, improving Sat4J's error syntax, and modifying
    p2 to use the error report from Sat4J and return better error
    reports to the user.

## Contributor name

  - **Area of interest**
    Details about it

[3.5 contributions](Category:Equinox_p2 "wikilink")