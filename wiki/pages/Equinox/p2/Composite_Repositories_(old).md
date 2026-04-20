''Note: This page and these Ant tasks are now deprecated. Please see the
new [Composite
Repositories](Equinox/p2/Composite_Repositories_\(new\) "wikilink") page
for updated information. ''

## What are composite repositories?

Composite repositories are an easy way to group together multiple
repositories that exist in different physical locations.

## Integration with the build process (Ant tasks)

We have created Ant tasks to aid with this integration into build
scripts.

Note that there are different tasks for the artifact and metadata
repositories... each must be called separately.

### Artifact Repository Tasks

#### Task: Create

Create a composite repository at a specified location.
**Taskname:** `p2.composite.artifact.repository.create`
**Attribute:** `location` - the location of the repository
**Attribute:** `name` - the name of the repository
**Attribute:** `compressed` - *true* (default) if the repository should
be compressed, *false* otherwise.
**Attribute:** `failOnExists` - *true* if the operation should fail if a
composite repository already exists at the location, *false* (default)
otherwise.

#### Task: Add

Add a child repository to an existing composite repository.
\* **Taskname:** `p2.composite.artifact.repository.add`
\* **Attribute:** `location` - the location of the composite
repository
\* **Attribute:** `child` - the location of the child to add

#### Task: Remove

Remove a specific child repository or all children repositories from the
specified composite repository. Note that only one of *child* or
*allChildren* is mandatory.
**Taskname:** `p2.composite.artifact.repository.remove`
**Attribute:** `location` - the location of the composite repository
**Attribute:** `child` - the location of the child to remove
**Attribute:** `allChildren` - *true* if all the children should be
removed and *false* (default) otherwise.

#### Task: Validate

Validate the contents of a repository using a comparator.
**Taskname:** `p2.composite.artifact.repository.validate`
**Attribute:** `location` - the location of the composite repository
**Attribute:** `comparator` - the ID of the comparator to use for the
comparison

#### Example

    <!-- create a composite repo -->
    <p2.composite.artifact.repository.create name="My Composite Artifact Repository" location="${location}" />

    <!-- add 3 children -->
    <p2.composite.artifact.repository.add location="${location}" child="${one}" />
    <p2.composite.artifact.repository.add location="${location}" child="${two}" />
    <p2.composite.artifact.repository.add location="${location}" child="${three}" />

    <!-- remove a child -->
    <p2.composite.artifact.repository.remove location="${location}" child="${two}" />

    <!-- call the validator -->
    <p2.composite.artifact.repository.validate location="${location}" />

### Metadata Repository Tasks

#### Task: Create

Create a composite repository at a specified location.
**Taskname:** `p2.composite.metadata.repository.create`
**Attribute:** `location` - the location of the repository
**Attribute:** `name` - the name of the repository
**Attribute:** `compressed` - *true* (default) if the repository should
be compressed, *false* otherwise.
**Attribute:** `failOnExists` - *true* if the operation should fail if a
composite repository already exists at the location, *false* (default)
otherwise.

#### Task: Add

Add a child repository to an existing composite repository.
\* **Taskname:** `p2.composite.metadata.repository.add`
\* **Attribute:** `location` - the location of the composite
repository
\* **Attribute:** `child` - the location of the child to add

#### Task: Remove

Remove a specific child repository or all children repositories from the
specified composite repository. Note that only one of *child* or
*allChildren* is mandatory.
**Taskname:** `p2.composite.metadata.repository.remove`
**Attribute:** `location` - the location of the composite repository
**Attribute:** `child` - the location of the child to remove
**Attribute:** `allChildren` - *true* if all the children should be
removed and *false* (default) otherwise.

#### Example

    <!-- create a composite repo -->
    <p2.composite.metadata.repository.create name="My Composite Metadata Repository" location="${location}" />

    <!-- add 3 children -->
    <p2.composite.metadata.repository.add location="${location}" child="${one}" />
    <p2.composite.metadata.repository.add location="${location}" child="${two}" />
    <p2.composite.metadata.repository.add location="${location}" child="${three}" />

    <!-- remove a child -->
    <p2.composite.metadata.repository.remove location="${location}" child="${two}" />

### Extra Notes

  - If a composite repository is co-located with a regular repository
    then you may have to use the full URI (including filename) when
    loading the repository. (bug 247566)

[Composite Repositories](Category:Equinox_p2 "wikilink")