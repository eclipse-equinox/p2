Updating your Eclipse plugins may occasionally result in a b0rk3d
install. This situation usually occurs when somebody hasn't properly
managed versions and inadvertently introduced an incompatibility that
didn't surface during testing. Fortunately Eclipse manages its updates
and installations using a checkpointing mechanism called <em>p2</em>,
and p2 allows selectively uninstalling existing features as well as
reverting to previous checkpoints.

If you can bring up the UI, then you can [revert the change from within
the Eclipse About
dialogs](http://eclipsesource.com/blogs/2009/10/02/reverting-changes-in-an-eclipse-installation-using-p2/).

But sometimes even the UI won't come up. Fortunately can use the
command-line [p2
director](http://help.eclipse.org/indigo/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/p2_director.html)
to revert:

1.  You first need to determine your p2 profile name. Look inside your
    Eclipse installation in the
    `p2/org.eclipse.equinox.p2.engine/profileRegistry/` directory. There
    is likely a single directory called `PPPPPP.profile` (e.g.,
    SDKProfile.profile). The `PPPPPP` is the name of your profile.
2.  Inside this profile directory are a set of files of the form
    `TTTTTT.profile.gz` where `TTTTTT` is a numeric timestamp. These
    timestamps are ordered. The latest timestamp is your current
    profile, resulting from your last installation or update, and the
    second-last timestamp is the profile before you did your last
    installation or update. Remember this second-last timestamp.
3.  You need to gather the p2 repositories where artifacts can be
    resolved from
4.  You will now invoke the p2 director from the command-line with the
    profile name `PPPPPP` and the previous timestamp `TTTTTT` as
    determined in the previous steps.

::\* on Windows, use the `eclipsec.exe` instead of ./eclipse

::\* on Mac builds since Mars (4.5) M6, use
`Eclipse.app/Contents/MacOS/eclipse` instead of ./eclipse

`   ./eclipse -application org.eclipse.equinox.p2.director -profile `<em>`PPPPPP`</em>` -revert `<em>`TTTTT`</em>` \`
`       -noSplash -repository `<em>`comma,separated,repository,urls`</em>

If all the referenced artifacts can be resolved, then your installation
should be reverted to its previous state.

The [p2
director](http://help.eclipse.org/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/p2_director.html)
also supports installing and uninstalling artifacts from the
command-line.

### Example

Eclipse committers frequently update to the latest integration builds.
Occasionally one of the I-builds goes bad. To revert, they'd run
something like:

`   ./eclipse -noSplash -consolelog -application org.eclipse.equinox.p2.director \`
`       -profile SDKProfile -revert 1429305715350 \`
`       -repository `<http://download.eclipse.org/eclipse/updates/4.5-I-builds>

### Revert doesn't work

Unfortunately, because of p2 design flaws, revert usually doesn't work
when you need it. If one of the update sites that contained any of the
artifacts you try to restore doesn't exist any more or doesn't contain
that old artifact any more, then revert fails (, ). As a workaround, you
can try to uninstall and then re-install the artifact.

Example: You have an Eclipse install where you used
<http://download.eclipse.org/eclipse/updates/4.7-I-builds> to update the
Eclipse SDK. Later, you updated to Eclipse SDK I20170109-2000, but you
find a bug that prevents you from doing work with that build. You want
to install Eclipse SDK I20170108-2000 instead, but the broken p2 design
wouldn't let you do that, and revert doesn't work because the original
build is not available any more.

You need a separate install of the p2 director application which can
uninstall I20170109-2000 and then install I20170108-2000. You can't do
that using only your install, because uninstalling Eclipse SDK
I20170109-2000 also uninstalls the p2 director and the eclipse
executable.

Workaround (transcript is for macOS; adapt paths to your setup):

`Paths:`
`/Volumes/Eclipse/4.6-M-builds.app/ -- a compatible p2 director install`
`/Volumes/Eclipse/4.7.app/ -- Eclipse SDK I20170109-2000 + other plug-ins; the broken install`

`$ cd /Volumes/Eclipse/4.6-M-builds.app/Contents/MacOS/`
`$ ./eclipse -application org.eclipse.equinox.p2.director -destination /Volumes/Eclipse/4.7.app/ -uninstallIU org.eclipse.sdk.ide`
`$ ./eclipse -application org.eclipse.equinox.p2.director -destination /Volumes/Eclipse/4.7.app/ -repository `<http://download.eclipse.org/eclipse/updates/4.7-I-builds>` -installIU org.eclipse.sdk.ide/4.7.0.I20170108-2000`

The available versions can be found using -list, e.g.:

`$ ./eclipse -application org.eclipse.equinox.p2.director -destination /Volumes/Eclipse/4.7.app/ -repository `<http://download.eclipse.org/eclipse/updates/4.7-I-builds>` -list org.eclipse.sdk.ide`

[Category:FAQ](Category:FAQ "wikilink")
[Category:Equinox_p2](Category:Equinox_p2 "wikilink")