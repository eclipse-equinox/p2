This is a list of use cases that need to be considered when designing
the [Touchpoint](Equinox/p2/Concepts "wikilink") class. Some of these
items are described in the context of the Eclipse Update Manager
functionality.

  - Manage launch parameters through install handlers. This includes the
    management of a global property file and a user property file. The
    user property file is modified during the enable phase. For the
    multiuser case, it is easy to upgrade a user files during an
    enable/disable phase. It is not really possible to do this during
    the install by an admin. There has also been discussion that the
    enable handlers should be immutable (not depend a known state).

<!-- end list -->

  - Manage JVM features. This had to be done slightly differently
    because JVM features have so many non-standard properties, We added
    and manage a plugin property file for these. While this has worked
    well for us it has a few thorns. There is a question of how to
    override the JVM properties from the command line or more generally
    how to combine JVM properties coming from this file and other
    places. When we attempt to integrate these features into the tooling
    we have the same questions.

<!-- end list -->

  - Manage the branding. Done through the property files modified by
    install handlers.

<!-- end list -->

  - Manage what gets launched. There may be multiple ICONS that
    represent different launch configurations. The ICON command length
    is limited so there needs to be an abstraction to handle this.

<!-- end list -->

  - Manage the invocation of native exe's and bat and .sh. These are a
    problem because we usually lose the ability to track operations, log
    errors, etc.

<!-- end list -->

  - Managing native service states through install handlers so that it
    can be upgraded/installed like any other feature/plugin.

### Other use cases

  - There is a need to be able to just unzip a platform and run. This
    likely implies that there is a need to do dynamic configuration on
    the first launch for a user.

<!-- end list -->

  - For multiuser there is a need to be able to know if we are an admin
    installing into all the shared spaces or if we are just a user
    launching the platform. The actions performed are different.

[Touchpoint Use Cases](Category:Equinox_p2 "wikilink")