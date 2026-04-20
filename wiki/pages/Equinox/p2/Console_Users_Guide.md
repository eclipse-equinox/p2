## Using P2 Provisioning Commands

The P2 Provisioning Commands are console commands enabled by the
presence of P2 that allow you to perform common provisioning operations,
such as managing the repository and profiles, installing and removing
components, etc.

### Prerequisites

To have the Eclipse console available, you have to add **-console** as a
program argument either in the *Eclipse.ini* file of your default
Eclipse installation, or, in the *Argument* tab in your Eclipse IDE. See
[Eclipse Runtime
Options](http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html)
for more details

### Enabling the Provisioning Commands

To run with the provisioning console commands enabled, make sure that
the P2 console bundle is available and started in the process to which
you want to connect the console:

> 1\. Start the console.
> 2\. Find the ID of the P2 console bundle by using the command ss (in
> the example below the P2 console bundle ID is "68"):
>
> >
> >
> >     osgi> ss p2.console
> >
> >     Framework is launched.
> >
> >     id      State       Bundle
> >     68      <<LAZY>>    org.eclipse.equinox.p2.console_1.0.300.v20110104-0005
>
> 3\. Start the bundle:
>
> >
> >
> >     osgi> start 68
>
> 4\. To list all the commands, type **man** in the console line. The P2
> Provisioning Commands all start with "prov".

## P2 Provisioning Commands

#### Repository Commands

>   - ` provaddrepo  `<repository URI> : Adds both metadata and artifact
>     repository at URI
>
> <!-- end list -->
>
>   - ` provdelrepo  `<repository URI> : Deletes metadata and artifact
>     repository at URI
>
> <!-- end list -->
>
>   - ` provaddmetadatarepo  `<repository URI> : Adds a metadata
>     repository at URI
>
> <!-- end list -->
>
>   - ` provdelmetadatarepo  `<repository URI> : Deletes a metadata
>     repository at URI
>
> <!-- end list -->
>
>   - ` provaddartifactrepo  `<repository URI> : Adds an artifact
>     repository at URI
>
> <!-- end list -->
>
>   - ` provdelartifactrepo  `<repository URI> : Deletes an artifact
>     repository URI
>
> <!-- end list -->
>
>   - `provlg [`<repository URI>`  <iu id | *> <version range | *>] ` :
>     Lists all IUs with group capabilities in the given repository or
>     in all repositories if URI is omitted
>
> <!-- end list -->
>
>   - `provlr [`<repository URI>`  <iu id | *> <version range | *>] ` :
>     Lists all metadata repositories, or the contents of a given
>     metadata repository
>
> <!-- end list -->
>
>   - `provlar [`<repository URI>`]` : Lists all artifact repositories,
>     or the contents of a given artifact repository
>
> <!-- end list -->
>
>   - `provliu [<repository URI | *> <iu id | *> <version range | *>]` :
>     Lists the IUs that match the pattern in the given repo. \* matches
>     all
>
> <!-- end list -->
>
>   - ` provlquery <repository URI | *>  `<expression>`  [ true | false
>     ] ` : Lists the IUs that match the query expression in the given
>     repo. \* matches all. The expression is expected to be a boolean
>     match expression unless the third argument is true, in which case
>     the expression is a full query.

#### Profile Registry Commands

>   - ` provaddprofile  `<profileid>`   `<location>`   `<flavor> : Adds
>     a profile with the given profileid, location and flavor
>
> <!-- end list -->
>
>   - ` provdelprofile  `<profileid> : Deletes a profile with the given
>     profileid
>
> <!-- end list -->
>
>   - `provlp [<profileid | *>]` : Lists all profiles, or the contents
>     of the profile at the given profile
>
> <!-- end list -->
>
>   - `provlgp [`<profileid>`]` : Lists all IUs with group capabilities
>     in the given profile, or current profile if profileid is omitted
>
> <!-- end list -->
>
>   - `provlpts [`<profileid>`]` : Lists timestamps for given profile,
>     or if no profileid given then the default profile timestamps are
>     reported
>
> <!-- end list -->
>
>   - ` provlpquery <profileid | this>  `<expression>`  [ true | false
>     ] ` : Lists the IUs that match the query expression in the given
>     profile. The expression is expected to be a boolean match
>     expression unless the third argument is true, in which case the
>     expression is a full query

#### Install Commands

>   - ` provinstall  `<InstallableUnit>`   `<version>`   `<profileid> :
>     installs an IU to the profileid. If no profileid is given,
>     installs into the default profile.
>
> <!-- end list -->
>
>   - ` provremove  `<InstallableUnit>`   `<version>`   `<profileid> :
>     Removes an IU from the profileid. If no profileid is given,
>     installs into the default profile.
>
> <!-- end list -->
>
>   - ` provrevert  `<profileTimestamp>`  [ `<profileid>`]` : Reverts to
>     a given profile time stamp for an optional profileId

## Examples

**Prerequisite:** Start the P2 console bundle

    osgi> ss p2.console

    Framework is launched.

    id      State       Bundle
    68      <<LAZY>>    org.eclipse.equinox.p2.console_1.0.300.v20101108

    osgi> start 68

To install **EGit** you will have to:

> 1\. List IUs with group capabilities to check the exact name and
> version of the EGit IU:
>
>     < ... omitted ...>
>     org.eclipse.egit.feature.group 0.8.4
>     < ... omitted ...>
>
> 2\. Install the EGit IU:
>
>     provinstall org.eclipse.egit.feature.group 0.8.4
>
> 3\. Apply the changes
>
>     confapply
>
> 4\. Open GIT repository browser in Eclipse

To install [Eclipse Memory Analyzer Tool
(MAT)](http://www.eclipse.org/mat) in Eclipse you have to:

> 1\. Add MAT update site:
>
>     osgi> provaddrepo http://download.eclipse.org/mat/1.0/update-site/
>
> 2\. List IUs with group capabilities to check the exact name and
> version of the required IU:
>
>     < ... omitted ...>
>     org.eclipse.mat.feature.feature.group 1.0.100.201012150941
>     < ... omitted ...>
>
> 3\. Install the MAT IU:
>
>     provinstall org.eclipse.mat.feature.feature.group 1.0.100.201012150941
>
> 4\. Restart Eclipse to allow P2 reconciler to install the new bundles
> (this is often needed for UI contributions such as MAT)
> 5\. You may now use your new MAT perspective

[Console Users Guide](Category:Equinox_p2 "wikilink")