This page tries to collect scenarios and use cases for controlling the
various locations used by p2. This includes managing the location
initial location of a profile, p2's data area, shared locations, the
location into which additional function is installed and the
relationship to the bundle pool etc.

Each details a scenario and is followed by one or more approaches. In
some cases the scenario is not currently supported and a bug report may
be listed.

## Install

### Select location on install

It should be possible to select the target location when installing
plug-ins from within Eclipse. The functionality can be compared to
selecting an extension location when installing plug-ins in the old
Update Manager.

#### User Happiness

Remember the keynote at EclipseCon 2005? If you take control away from a
user, it will make him/her unhappy. Some users are used to organize
content on their systems the way they want to organize it. There are
plenty reasons for this. For example, a user might want to place
plug-ins from a single project into a separate folder - one folder
containing the ECF, another containing Webtools. Later on he might want
to share different folders between different runtime and development
configurations.

Another user might want to organize plug-ins in a way he is used to.
Maybe he just wants to align it better with the OS. But it is also
possible that he simply wants to keep things separated (eg. don't mix
experimental plug-ins from project XYZ with stable plug-ins from project
ABC). Yet another option is to share some plug-ins between different
runtime instances (eg. an image viewer plug-in and Subversion tooling
for every instance but PDT only in one).

Therefore, a user should be able to keep control over the
layout/organization of software on his computer and the p2 Update UI
should assist him.

#### Usability

Another advantage are several usability improvements within Eclipse. The
concept of extension locations has been well adopted within Eclipse. For
example, PDE allows to select plug-ins in wizards, dialogs and editors
by grouping them based on their install locations. This improves the
usability in PDE when working with multiple Eclipse based
applications/projects in multiple workspaces. For example, one can
easily compose a target runtime simply by selecting the different
plug-in locations - in one workspace it's Equinox + ECF in another it's
the Platform + Webtools.

### Mac OS X layout

The current Eclipse.app is pretty badly laid out for Mac OS X systems,
as is the location of the bundles. Macs have several hierarchies of
places where things can be expected to be placed/read from, in the
following order:

  - \~/Library/Application Support/*Equinox/Bundles* - accessible by the
    current user, probably read-write
  - /Library/Application Support/*Equinox/Bundles* - shared across
    multiple users on the current computer, probably read-only
  - /Network/Library/Application Support/*Equinox/Bundles* - shared
    across multiple computers, probably read-only

Although it should be possible to define these as 'dropins', there
doesn't seem to be any easy/obvious way to have multiple such
directories.

There's a bunch of secondary issues for the Mac bundle, such as that
everything it needs should be inside the Eclipse.app rather than dropped
outside it; and logs shouldn't be written inside, but rather outside in
\~/Library/Logs. P2 is somewhat inflexible and assumes a wrietable
location for the application isnatll folder when extracting e.g.
configuration.ini. Even if that directory is writable (e.g. running with
admin priviledges) it should defer to some per-user location instead.
(The common argument - that we want to install multiple different
versions of Eclipse - is almost completely unnecessary when running as a
'normal' user, but rather something that Eclipse developers might do
more frequently.)

For a 'default' Eclipse install, it might make sense to have
Eclipse.app/Contents/Resources/*Equinox/Bundles/* as the location for
key bundles, like ECF, OSGi.jar etc.

See [Where to put
files](http://developer.apple.com/DOCUMENTATION/MacOSX/Conceptual/BPFileSystem/Articles/WhereToPutFiles.html)
and [Library
directory](http://developer.apple.com/DOCUMENTATION/MacOSX/Conceptual/BPFileSystem/Articles/LibraryDirectory.html)
on Apple's site for further info.

## Scenarios

### Multiple installations with shared plugins

For people who use the Eclipse IDE for several different things, it has
previously been possible to have several distinct installations of
sharing \*some\* of their plugins.

The reason is that if you need very different functionality you do not
want to clutter up your Eclipse install with everything.

For instance if you work with both Ruby and Java you might want one
installation with a lot of Ruby specific plugins. But for Java you want
a host of different plugins. Trying to mash it all into one install will
likely generate a number of technical issues, and it will eat up a lot
of RAM and become a whole lot slower.

But on the other hand you might need shared plugins like for SVN or
basic technology plugins. And previously you could share those easily -
along with the basic install - and only upgrade the shared stuff once.
Now you have to maintain each installation separately and you waste a
lot of hard disk space as well.

Even when the specific location of the shared stuff is not important, it
is important to users to be able to define functional groups of plug-ins
that can be installed/uninstalled as a group (Ruby stuff, Java stuff,
etc.) And having downloaded these things before, a scripted/headless
install is not enough. The user wants to know that these are kept
locally. (See also
[Equinox_p2_UI_Use_Cases\#Dave](Equinox_p2_UI_Use_Cases#Dave "wikilink")
for a discussion of this requirement in terms of the p2 user persona,
Dave).

### Recovery from failures

Another thing that using Extension Locations gave me was that if the
Eclipse install failed, for some reason, it would be easy to reinstall
everything again. I have already once had a situation with Eclipse 3.4
where I simply couldn't update it anymore though P2 resulting in a
complete reinstall of the entire thing. Before 3.4 I had all plugins -
that was not part of the Eclipse distribution - in a number of Extension
Locations. That made it easy to reinstall Eclipse as I would just unzip
it again and point it at the Extension Locations I needed.

Similarly it was easy to remove a new plugin if I made sure to place it
in a new Extension Location. I guess you could say I had a sort of
quarantine Extension Location where I would place new stuff in. If they
messed anything up I could easily get rid of them, even if the Update
Manager wouldn't (I had cases of that, but to be fair not seen a similar
situation in P2 yet)

### Distribution in an restricted environment

We have to distribute an initial eclipse application with a defined set
of bundles via an alternative software pooling mechanism, which is based
on MSI technology. This mechanism installs software under a special
account in the program files directory, where the user has no write
permissions. If the user wants to install additional bundles we used the
extension location and pointed to a directory where the user has write
permissions. This directory must be set by the user, a predined
directory (e.g.) the user's home directory is not applicable because of
disc quota policies (only equinox.configuration.area is located in
user's home). In general the user installs the additional plugins in
D:\\Data\\\[USERNAME\] but not every user has a d-drive, so the location
must be set by the user.

[Location Selection](Category:Equinox_p2 "wikilink")