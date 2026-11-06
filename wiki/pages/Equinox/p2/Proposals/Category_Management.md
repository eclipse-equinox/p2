**Problem:** How to present category information to users in p2

There are several different ways that producers can author p2
repositories, and many of the workflows result in repositories without a
proper categorization. Currently (Eclipse 3.5 M6) only software found in
a category is displayed.

# Workflows

**Workflows:**

1.  Creating a site.xml, adding features / categories and selecting
    export
2.  Creating a feature.xml, selecting export
3.  Creating a product.xml, selecting export
4.  PDE Build for a product
5.  PDE Build for a feature
6.  PDE Build for a bundle
7.  Export a bundle (or feature) and use the publisher to create
    metadata for them

# Considerations

**Considerations:**

1.  Authors want to control what IUs a user can see (don't show
    everything)
2.  Features can be categorized in three different ways:
    1.  Added to a category in a site.xml
    2.  Added to a site.xml (no category) -- shows in other /
        uncategorized
    3.  Not added to a site.xml (this means a user explicitly did not
        want the feature to be shown)

# Solution 1: Address the problem in the UI

This problem can be addressed in the UI. In order to address this
problem in the UI, all installable software will have to be shown (not
just categorized software)

**Pros:**

1.  Works on existing repositories
2.  No work for those publishing metadata
3.  Similar workflow to Eclipse 3.4.x

**Cons:**

1.  If metadata authors really don't want particular software to be
    shown, this cannot be expressed
2.  Inconsistent workflow with Eclipse 3.3

# Solution 2: Address the problem in the publisher

This problem can be address in the publisher. In order to address this
problem in the publisher, metadata authors will be required to provide
categorization when building their repositories.

For example, PDE could consider a site.xml file next to a feature /
product file to be the categorization for this. If the site.xml file
exists, it is used a build time.

**Pros:**

1.  Authors can provide categorization for features and products
2.  Works similar to Eclipse 3.3 (no category, not listing)

**Cons:**

1.  Currently site.xml must exist in a UpdateProject (there are bugs if
    you don't have an updatesite project)
2.  Old p2 repositories may have to be updated
3.  What if we don't have a site.xml?

## What if no categorization is available

If no site.xml is available we could:

1.  Create a default category
2.  Prompt the user for a category (or make it part of the export
    wizard)
3.  Do nothing (no categories)

[Category Management](Category:Equinox_p2 "wikilink")