Well, the "love" part might be premature :) but are confident we'll end
up there.

This page is a central place to document some notes about what we in WTP
have learned about P2, which bugs effect us, little "tricks", etc.
Warning: this is a fast evolving area, due to bugs, miscommunication,
and ignorance, so take these notes as hints ... nothing is forever ...
and update this page as you find more information or errors in the
information here.

## Intro

In theory, shouldn't effect you at all. You could or should be able to
do things as always, except there might be some bugs, and there are
reasons to try to make use of it. One reason is to help the platform
find bugs early so they get fixed by their release. Closer to home ...
help find bugs in _our_ code that only show up in the conditions
encouraged by P2.

Ever wonder what P2 stands for? As best I can tell "Provisioning
Platform" (I think I know why they did not want to call it PP ). We in
WTP won't be taking advantage of the platform aspects of it directly
(that is, not using their APIs) but its my understanding that the EPP
Project will be making their "all in one" installs using P2, which
should be much better then the old Update Manager.

## Running WTP

Here's one way to create a running version of WTP. Unzip the Eclipse SDK
in some directory, say /M6. You'll see the familiar directories,
eclipse/plugins and eclipse/features but some new ones too ... the main
new one is an empty directory called eclipse/dropins. This is by design
the place to "drop in" other zip files or plugins that are not installed
via the P2 User Interface.

I unzip wtp-sdk and all our pre-reqs into separate subdirectories, such
as wtp, emf, xsd, gef, dtp, relengtool, testFramework. So ... you end up
with a hierarchy similar to

    <nowiki>
    /M6/eclipse
    /M6/eclipse/plugins
    /M6/eclipse/features
    /M6/eclipse/dropins
    /M6/eclipse/dropins/wtp
    /M6/eclipse/dropins/emf
    /M6/eclipse/dropins/xsd
    /M6/eclipse/dropins/.... etc.

    And, each of those subdirectories end up looking as they did before ... for example,

    /M6/eclipse/dropins/wtp
    /M6/eclipse/dropins/wtp/eclipse
    /M6/eclipse/dropins/wtp/eclipse/plugins
    /M6/eclipse/dropins/wtp/eclipse/features
    </nowiki>

I suspect there will eventually be new bugs open due to increased path
lengths. :)

Now, start eclipse like you usually would. You should have a fully
functioning WTP.

This structure has the advantage that it _should_ be easier to update
pieces of your install, say just with a new WTP build, without touching
the rest of it.

'''Tip: ''' The first time you start, it'll take a lot longer than it
used to. See bug
[224579](https://bugs.eclipse.org/bugs/show_bug.cgi?id=224579). But,
subsequent restarts should be as fast or faster -- as long as you do not
use -clean (-clean clears the timestamp cache and it goes through the
whole re-discover process again). So, the tip is, don't use -clean
routinely (as I used to do) but only if you know you really need it.
This should be fixed eventually. (But in fact, we had to remove -clean
from all our JUnit tests ... or, else they could not complete in a
reasonable amount of time. See bug
[224269](https://bugs.eclipse.org/bugs/show_bug.cgi?id=224269)).

## Self Hosting WTP Development

With the above install, you can use the plugins you normally develop and
commit and it will "compile and run against" that version you are self
hosting on. (If trouble, go to PDE Preferences, Target Platform, and
press "reset" (not reload). Check on "show by location" and you should
see all your sub-directories in dropins. (If not, open a bug, or comment
in this instructions, or both).

I think there are issues with running against a completely different
target, installed in a different directory (see bug
[225148](https://bugs.eclipse.org/bugs/show_bug.cgi?id=225148)). Even
that can be worked around by "running" the target, so all the meta data
is updated. But, check the bug ... that may change with more
information. Maybe it's just me. :) Or, update the bug if you find out
more\!

## Bugs we've discovered in our code due to P2

My guess is others have similar bugs, so hopefully other projects and
adopters can learn from us. These bugs all had to do with making
incorrect assumptions about the locations of where things were
installed.

One bug was some old pre-osgi-bundle code (that had been copied several
places) assumed some things about the name and length of the
"eclipse/plugins" directory. These were easy to fix using the correct
APIs. See bug
[224148](https://bugs.eclipse.org/bugs/show_bug.cgi?id=224148).

We have some code which makes assumptions about location of where
features are installed. It currently works with P2, but we should never
assume anything about features, and may eventually look if there's some
P2 API way of accomplishing the update that is being done. See bug
[224441](https://bugs.eclipse.org/bugs/show_bug.cgi?id=224441).

## Build and JUnit tests

Some changes to our build infrastructure were made, which I'll note
here, in case it helps others.

  - Compile time: There were not any changes required, which makes
    sense, but still confused me. The reason is because when we are
    compiling our code (with JDT) it is not actually running our code
    (and P2 only has to do installing), and it finds the source where
    ever we tell it to. This should not change.

<!-- end list -->

  - JUnit Test time: We use the Eclipse Platform's test framework --
    which for some reason still uses the file system, instead of
    extensions, to find and run test plugins -- so we had to change some
    of our infrastructure to unzip into different locations and look for
    test.xml files in different locations. It turns out, if we'd simply
    waited until M6 was released, we would not have had to do this ...
    but, would have still wanted to do it, in order to better test our
    code, in P2-like conditions.

## Disabling P2

Please see
[bug 224908](https://bugs.eclipse.org/bugs/show_bug.cgi?id=224908) for
scripts and procedures to disable P2.

## Can a site be both an update manager site, and a P2 repository?

Yes. We do this for the Ganymede site, but anyone can do it for any
site.

This does, however, lead to slightly different appearance, or behavior
depending on if you use, or not, the explicit 'site.xml' at the end of
the update site URL.

I found this comment from John Arthorne, in
[bug 236142](https://bugs.eclipse.org/bugs/show_bug.cgi?id=236142) to be
very helpful in understand why this is and it can help guide site
creators in making their choices.

    <nowiki>
    ------ Comment  #4 From John Arthorne  2008-06-06 17:26:04 -0400  [reply] -------

    Since you have both a content.jar and a site.xml at that location, you actually
    have two repositories. You have a classic update site, and a p2 repository
    (these files don't know/care about each other).  Classic update sites don't
    contain a site name (site.xml has no name element), so these sites are
    "nameless". p2 repositories contain a name element so they have a
    human-readable name.

    You should typically see the same categories in both cases if the content.jar
    was built directly from the site.xml at the same location. I'm not sure why
    you're seeing a difference here.

    If you just specify the directory as the URL (such as
    http://download.eclipse.org/releases/ganymede/), we have to guess which
    repository you meant (either site.xml or content.jar). Our guessing algorithm
    first looks for a content.jar or a content.xml, and if found it loads that p2
    repository. If there is neither, it looks for site.xml and loads a classic
    update site.
    </nowiki>

## References

  - [Equinox p2 Migration Guide](Equinox_p2_Migration_Guide "wikilink")
  - [Equinox p2 - Getting
    Started](Equinox_p2_Getting_Started "wikilink")
  - [Equinox p2 - Getting Started with
    Dropins](Equinox_p2_Getting_Started#Dropins "wikilink")
  - [Equinox p2 Testcases](Equinox_p2_tests "wikilink")

[Category:Eclipse Web Tools Platform
Project](Category:Eclipse_Web_Tools_Platform_Project "wikilink")
[Category:Equinox_p2](Category:Equinox_p2 "wikilink")