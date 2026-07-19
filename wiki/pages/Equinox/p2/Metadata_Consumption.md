# Overview

Many eclipse.org projects now produce update sites containing p2
metadata. Now that everyone is producing this metadata, the obvious
question that arises is how do people building products and packages
consume this metadata? Everyone is accustomed to consuming Eclipse
project output in the convenient form of zip files:

  - zips are easily replicated to company mirrors, which reduces
    bandwidth costs for both producers and consumers
  - zips are a reliable and consistent input for builds. If you keep the
    input zips around, you can reproduce an old build easily and
    reliably
  - power users can hack together applications by unzipping various zips
    into the Eclipse dropins folder

However, there are also numerous advantages to consuming project output
in the form of p2 repositories:

  - Repositories can use pack200 to drastically reduce transfer costs
    and disk footprint
  - Repositories contain p2 metadata that would otherwise need to be
    generated on the fly by p2 on first startup
  - As projects start to produce and exploit more advanced p2 metadata,
    it can no longer be generated on the fly (think chmod and sym-link
    metadata for example)
  - A project can produce a single repository containing all of their
    project's output. Consumers then have the flexibility to install
    only the portions they need. In the past this consumer flexibility
    was only possible by having the producer provide numerous zip files
    containing the different permutations of their project output. These
    large collections of zips are a maintenance nightmare for producers,
    and lead to slower builds and higher disk usage.

On the other hand, remote repositories don't make for a reliable build
input. They expose builds to transient communication failures, they may
change or be deleted, they add to bandwidth costs if they are consumed
remotely on every build, etc.

So, how do projects offer all of the advantages of both zips and p2
repositories? The answer: zipped p2 repositories. Simply take the p2
repositories you are producing today, zip them up, and make them
available on your project download page. Consumers can then download
these repositories, and use them offline in all the same ways they use
either zips or remote repositories today.

# FAQ

The following are some questions and answers related to zipped p2
repositories.

  - What does a zipped repository look like?
    Zipped repositories contain the p2 files content.jar and
    artifacts.jar, in addition to the usual plugins and features
    directories. These sub-directories contain plugins and features
    entirely in jarred form (or .jar.pack200.gz files). These are
    therefore slightly different from the typical zips most projects
    produce today, which contain features and plug-ins in "runnable"
    form (some plug-ins are JARs, some are unzipped). See
    [Equinox/p2/Equinox_p2_zipped_repos](Equinox/p2/Equinox_p2_zipped_repos "wikilink")
    for more details on the file format.

<!-- end list -->

  - I'm a power user. How do I install stuff from a zipped repository?
    Unzip the repository somewhere on your disk. Open Help \> Install
    New Software, and type/drag/select the repository location. The list
    below will be populated with all the content of that repository.
    Install away. It is also possible to avoid the unzipping step by
    entering a jar URI pointing to the archive (see "What is the URI of
    a zipped repo?").

<!-- end list -->

  - Can I use a zipped repository in the dropins folder?
    Yes. Simply unzip the repository into a sub-directory of the dropins
    folder.

<!-- end list -->

  - Can I use a zipped repository in my build?
    Yes. Since Galileo M5, PDE/Build supports fetching artifacts
    directly from p2 repositories (local or remote), and compiling
    against p2 repositories. See the [Eclipse M5 New and
    Noteworthy](http://download.eclipse.org/eclipse/downloads/drops/S-3.5M5-200902021535/eclipse-news-M5.html#pde-build-fetch)
    for details.

<!-- end list -->

  - What is the URI of a zipped repo?
    Without being unzipped, zipped repos can be used as input in various
    places in p2. The URI format for these is a jar URI of the form
    jar:<file URI>\!/ (e.g. <jar:file:///Users/Pascal/repo.zip!/> )

<!-- end list -->

  - How do I set up a target platform using zipped repositories

[Metadata Consumption](Category:Equinox_p2 "wikilink")