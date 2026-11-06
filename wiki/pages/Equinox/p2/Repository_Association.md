This page is for collecting background information, requirements, and
proposed solutions for the p2 Galileo plan item on improving the
repository association model. Roughly, this problem area has two facets:

1.  Association of repositories with installable units. How are the
    repositories initially seeded, and how does an administrator or
    bundle provider specify/control the repositories used to
    install/update a given IU?
2.  Association of repositories with each other. How do groups of
    repositories interact to perform provisioning operations that span
    multiple repositories?

This document uses the terms "producer" and "consumer" in a particular
way. In this document, "producer" means a developer that is authoring
bundles, features, and associated metadata. The term "consumer" refers
to someone using the artifacts and metadata created by some producer.
The consumer could either be the end user of the software created by the
consumer ("user"), or a third party managing the deployment of the
software to their own end users ("publisher"). This document also refers
to the p2 user
[personas](Equinox_p2_UI_Use_Cases#Eclipse_Personas "wikilink") (Steve,
Laurel, Dave, and Ellen), which describes prototypical examples of
different kinds of p2 consumers.

# Historical Information

## Repository Association in Update Manager

### Overview

Prior to the introduction of p2, Update Manager (UM for short), had
various mechanisms for managing repository associations:

1.  Feature update sites. Each feature specified the update site to be
    used for updating that feature. If a parent feature included a child
    feature, the parent feature's update site would override the child's
    update site.
2.  Feature discovery sites. Each feature could optionally specify one
    or more update sites containing features of interest to users of
    that features. These repositories would be shown to the user at the
    beginning of the install wizard workflow
3.  Associate sites. Each site 'S' could specify additional associate
    sites. Those associate sites would be used in the context of any
    provisioning operation against site 'S'. This is how Update Manager
    handled provisioning operations spanning multiple sites.
4.  Site policy file. A policy file can be specified, which causes
    feature updates to be redirected to an alternate site.

### Problems

The problem with 1 and 2 above is that they represent the producer's
view of what site should be used when updating a feature. A consumer
wishing to control the sites shown to their user, or wishing to use
internal mirrors to save bandwidth, may want to override the producer's
choice of update site. Policy files help with this problem by
redirecting the sites specified for 1 above, but didn't help with
exposure of the producer's choice of discovery sites (2).

One problem with policy files (4) is that they didn't offer strong
control. The policy file had to be set by the end user, which required a
manual user step and was vulnerable to a power user altering it. The had
the additional problem that since the policy file was typically on a
server, the product would default back to the producer's choice of sites
when the policy server is unreachable.

## Repository Association in p2 1.0

### Overview

The only association mechanism in p2 1.0 was repository references. A
repository 'R' could specify additional referenced repositories that
would be added to the list of available repositories the first time 'R'
was loaded. Unlike associate sites, the references weren't constrained
to particular operational scope. A referenced site could be used
directly by the end user to initial provisioning operations not
involving repository 'X'. References could either specify the referenced
site to be "enabled" or "disabled" by default. Overall, repository
references were a hybrid of associate sites, update sites, and discovery
sites with some characteristics of each.

### Compatibility

p2 1.0 had the following strategy to manage compatibility with Update
Manager repository association concepts:

1.  When the p2 generator was run to convert an UM site into a p2
    repository, the site's associate repositories were converted into
    repository references. The update sites for each features would also
    be converted to repository references. Feature discovery sites were
    converted to disabled repository references.
2.  When installing a feature from a legacy UM site (site.xml), the
    site's associate sites were treated like repository references,
    causing them to be added as enabled sites in the repository
    managers. For every feature in the site, its update and discovery
    sites would be added to the list of known repositories (update sites
    enabled, discovery sites disabled)
3.  When discovering features in the plugins/dropins folders, the
    feature's update and discovery sites would be added to the list of
    known repositories (update sites enabled, discovery sites disabled)

### Problems

Some problems with the p2 1.0 model:

1.  Association between repositories was not maintained. When repository
    'R' was removed, its referenced sites were not also removed
2.  A repository could not be constrained to particular operations
    (updates on feature 'F') or scopes (installs/updates involving
    repository 'R').
3.  No way to set the available sites for a given product at build-time,
    or to add additional repositories at install-time.

# User Cases

1.  Permissive install (Steve/Laurel). Specify an initial set of
    repositories for an install, but end user may alter it thereafter.
2.  Controlled install (Ellen). Producer tightly controls available
    repositories. Ellen can't change the repositories, and needn't even
    be aware what repositories exist. A publisher may want to alter the
    producer's choice of repository, but maintain the same simple user
    workflow.
3.  A repository provides IUs (features/plugins), but those IUs have
    dependencies on IUs in other repositories. The publisher needs to be
    able to specify where to get the additional dependencies without
    user interaction. Another party re-publishing the same content may
    want to specify different places to obtain dependencies.
4.  Multiple parties produce repositories for their own content. A
    publisher wants to aggregate all these sites into a single
    repository from the user's perspective without copying the contents
    (e.g., Eclipse release train federating repositories produced by
    individual Eclipse projects).

# References

## Bug reports

  - [bug 234313](https://bugs.eclipse.org/234213) - Need better way to
    model the various UM associate sites
  - [bug 242396](https://bugs.eclipse.org/242396) - Control the
    repository presented to the user
  - [bug 231039](https://bugs.eclipse.org/231039) - \[ui\] In the
    "Software Updates" dialog make all site management functions
    optional and configurable

[Repository Association](Category:Equinox_p2 "wikilink")