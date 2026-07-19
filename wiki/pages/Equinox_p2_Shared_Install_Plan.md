# Scenario: read-only de-compressed upstream download

Eclipse installed read-only in <location> (say, **/opt/eclipse** or
**C:\\Program Files\\Eclipse**)

## Startup

  - check for \<user.home\>/.eclipse/p2/bundles.txt
  - NO:
      - load shared bundles.txt
  - YES:
      - is the user bundles.txt newer than the shared one?
          - YES (when we enter this, the end goal is for the framework
            to be running at least all the bundles from the shared
            bundles.txt)
              - attempt to load it
                  - SUCCESS:
                      - run, yay
                  - FAIL:
                      - run shared bundles.txt
                      - present some sort of notice to user (upon
                        startup?)
          - NO:
              - reconcile

## Provisioning Operation

  - materialize profile from running bundles.txt (and metadata
    repositories on disk for root IU inference)
  - Notes:
      - no manipulation allowed for shared IUs

## Open Issues

  - how do we make shared IUs immutable?

## Interesting stuff

The user bundles.txt lists all the bundles to run.

# Scenario: install from Linux distribution packages

Note 1: I'm going to say "rpm" here but I really mean any distribution
package Note 2: actual paths are negotiable; I just wanted to get some
potentials down

## Disk layout

See below under startup.

## Interesting stuff

  - both "products" (ex. Eclipse SDK, RSSOwl) and "add-on" RPMs (ex.
    CDT, Mylyn, RSSOwl extension) will include their own metadata
    repository

## "product" startup

  - only difference from above is that shared bundles.txt is split among
    base product and any add-ons

Example:

Eclipse SDK (a "product"):

    /usr/bin/eclipse
    /usr/share/eclipse/bundlepool/(lots of stuff)
    /usr/lib/eclipse/fragmentbundlepool
    /usr/share/eclipse/p2/bundles/sdk.txt
    /usr/share/eclipse/p2/metadataRepositories/sdk.xml

CDT (an IDE "add-on"):

    /usr/share/eclipse/bundlepool/*cdt*
    /usr/lib/eclipse/fragmentbundlepool/*cdt*
    /usr/share/eclipse/p2/bundles/cdt.txt
    /usr/share/eclipse/p2/metadataRepositories/cdt.xml

RSSOwl (a "product"):

    /usr/bin/rssowl
    /usr/share/rssowl/bundlepool/(stuff)
    /usr/lib/rssowl/fragmentbundlepool
    /usr/share/rssowl/p2/bundles/rssowl.txt (points to Eclipse bundles for dependencies)
    /usr/share/rssowl/p2/metadataRepositories/rssowl.xml

CoolThingy (an RSSOwl "add-on"):

    /usr/share/rssowl/bundlepool/*coolthingy*
    /usr/lib/rssowl/fragmentbundlepool/*coolthingy*
    /usr/share/rssowl/p2/bundles/coolthingy.txt
    /usr/share/rssowl/p2/metadataRepositories/coolthingy.xml

  - aggregate "product"'s /usr/share/<product>/p2/bundles/\*.txt
  - do \<user.home\> stuff as above

## provisioning operation

  - same as in above situation

## Open Issues

  - as above, how do we mark shared IUs as immutable?
  - can we split the install into /usr/lib, /usr/share, /usr/bin, etc.
  - will we need a custom configurator that deals with the split
    bundles.txt or should we have a variable like osgi.bundles.path
    where it looks for bundles.txt files and aggregates them?
  - is the profile materialization (for provisioning operations an UI
    presentation) acceptable?
  - will this still allow distributions to split up SDK by feature? I
    think so ... maybe we'll have a bundles.txt per feature

## Pros

  - no %post fragility
  - always correct, always starts up (well, unless base bundles.txt gets
    corrupted somehow)

## Cons

  - startup cost?
  - complexity?
  - diverge from eclipse.org downloads?

[Shared Install](Category:Equinox_p2 "wikilink")