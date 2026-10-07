**W.I.P**

After having used p2's provisional API for building p2 related tools, we
have found several issues regarding the category management in 3.5. This
page is an attempt to describe the issue, propose requirements on a
solution, and then a suggested implementation.

# What are the problems

#### Categories are versioned

Versioning of categories is a complication since the categorization
escapes the original repository (repo A) where it was declared. When a
second repository (repo B) mirrors/aggregates the content, it adopts any
categorization as its view of the categorization of its content (unless
it always rewrites them with new IDs - i.e. block them from being
mirrored). When a new version of a categorization is added in the
original repository, and repositories are aggregated A + B to repo C,
what is the categorization in C?

Resolution basics must be bypassed when dealing with category IU's. The
"use the latest version" idiom no longer apply. The UI tooling have
special queries to ensure that the category versions does not get in the
way. The same is true for the Buckminster aggregator, and will be true
for a vast amount of other applications in the future.

Not even in the only place where the categories are used (the UI) can
versions be used. Instead, they just add to the complexity.

The fundamental issue is that versioning of categorization is an odd
concept, there is no way for a user to select a particular version of
categorization, and even if there was, it would just be very confusing.
The categorisation is simply "the respositor's categorization of its
content" - it is always current - it is what it is.

#### Legacy update sites

This is described in [Bug 286736 - Legacy repositories are different
each time they are
read](https://bugs.eclipse.org/bugs/show_bug.cgi?id=286736) This leads
to:

  - In order to understand if categorization has changed, the current
    categorization has to be compared for content.
  - Since it is not possible to know if the category is originally from
    a old update site, comparisons are **always** needed
  - Failure to compare will yield repeated category IU's. One for each
    time the legacy site is read.
  - Any cache of the repository is always out-of-date the next time it
    is read.

#### A Sub-Category is a feature

  - To add to the confusion, a feature that also is a category, is
    considered a sub-category. This means that the sub-category is
    appointed by a version range. How does that fit together with the
    fact that versions don't really apply?
  - This must also mean that not all features can be installed. Or is it
    the opposite, some categories can be installed?

#### A Category is an Installable Unit, but is not installed

This leads to:

  - Exceptional logic whenever a category is found in a result.
  - All usage requires the exceptional handling, but the only use case
    where categories have a role is when presenting a categorized view
    of what to install to the user.
  - A good example of the confusion created by this is the discrepancy
    between how things are installed in the UI versus how it is
    installed using the directory application. The director lacks the
    exceptional handling. Described here [Bug 289380 Install categories
    same from the director /
    UI](https://bugs.eclipse.org/bugs/show_bug.cgi?id=289380).

# Missing Features

The categorization in 3.5 only solves the need to present things to
install in a human friendly way.

  - All users of a repository are presented with the same view
    irrespective of if they are developers or users of a published
    software.
  - As a consequence, it is not possible to organize "technical" things
    for technical people
  - Compared with a modern tag-based search system, the current wizard
    is very limited in what it can do. We need to recognize that P2 can
    grow very large and that much more human friendly and elaborated
    search capabilities will become needed.

# Requirements on a Solution

  - Separation of concern. Keep the descriptive meta-data used by the
    engine for installs and updates clean from other concerns
    (presentation and selection by humans).
  - Enable continous mapping of legacy sites in such a way that they
    appear stable (as long as they don't change).
  - Don't complicate the task of writing other repository converters so
    that P2 can access other types of repositories.
  - It should be possible to modify the categorization of IUs in a
    repository without having to modify the IUs.

# Proposed Solution

Treat categorization as a third type of p2 repository information -
there is already IU (metadata), and artifacts. The new kind represents
un-versioned meta data at the repository level. There is already such
metadata captured in properties in the repository itself.

The property mechanism has limitations in its capability to handle more
complex information, but could otherwise be used for this purpose. A
compromise could be to have a property refer to an artifact (in the
artifact repository) that contains the current categorization.

[Proposals/Categories API](Category:Equinox_p2 "wikilink")