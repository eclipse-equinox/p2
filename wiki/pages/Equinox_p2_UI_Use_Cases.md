This page captures thoughts about the kinds of users using the Eclipse
SDK p2 UI and the things these users are trying to do.

__TOC__

## Background

One goal in building the Eclipse 3.4 p2 UI was to simplify the number of
steps and concepts for a less technical user of Eclipse. We often called
this user "the RCP app" user and were somewhat influenced by "Samantha,"
a design persona developed by Mary Beth Raven to use in the UI design
for some Lotus products (see [Lotus
personas](http://www-128.ibm.com/developerworks/blogs/page/marybeth/20060426)).
We also often discussed "current UM users" - the more technical Eclipse
SDK users who want to closely manage their configuration and want to
know the detailed ramifications of a provisioning operation.

As we made design and implementation tradeoffs to meet the 3.4 schedule,
it became rather cumbersome to discuss some of these tradeoffs given we
had not developed a vocabulary for talking about these personas. (For an
example of the swirl, see
[Bug 224472](https://bugs.eclipse.org/bugs/show_bug.cgi?id=224472)).

To that end, we'll develop some *simple* personas to use in discussing
the various scenarios and problems/improvements. Rather than
interviewing a sample user base, I'm using bug reports, mailing list
traffic, walkthroughs, and other feedback to define these users
initially, and I welcome any and all help to iterate on these. Note that
I'm not focusing on the personal (age, hobbies) and background
(education) info of the users initially. If there is interest in further
developing these personas, by all means please contribute.

The personas are defined in terms of the Eclipse SDK and products built
on top of the SDK. We realize that some RCP app users don't fit these
definitions at all, and alternate UI's will be built where appropriate
in certain product. The UI class libraries are being designed to support
reuse of the building blocks and different levels of
integration/composition. But that is a topic for another page...

Any similarity between the persona names and real Eclipse SDK users is
completely coincidental

## Eclipse Personas

### Steve

Steve is a power user of the Eclipse C++ and device tooling, using a
Linux-based dev environment. He has intimate knowledge of underlying OS
and device technology, and Eclipse is only one of many tools he is using
in his job. He is familiar with Linux-based package management and
provisioning tools. He is also involved in the development of an Eclipse
client integration plug-in for his toolset, though this is a very small
part of his job. He does write the occasional bug report against Eclipse
and keeps tabs on the availability of function that he cares about. He
updates to new milestone builds when important new function appears, and
he might even take an integration build or maintenance branch build in
order to test or verify a bug report he cares about. He does not
routinely browse for new plug-ins, although he might add a plug-in that
he read about if he felt it would help his day-to-day tasks. He doesn't
really care how his Eclipse system is composed as long as the platform
is performing well and he can provide the information needed in a bug
report to help figure out a problem.

Things Steve needs to do regularly:

  - Update and manage his Linux environment
  - Check for updates in Eclipse when he hears of a bug fix or build he
    is interested in.
      - He is watching for different kinds of updates (milestone build,
        I-build, patch, etc.)
      - He needs to be able to quickly determine which update is
        important, needs to sift through what's available to find the
        specific one he wants.
  - Submit information in bug reports about exactly what version of
    Eclipse he is running

Things Steve does occasionally:

  - Install a plug-in he read about on the web
  - Contribute improvements or fixes to an Eclipse plug-in developed at
    his company

### Laurel

Laurel is a developer of modeling products for Eclipse, using Eclipse
Modeling Tools as a base for her work. She is an active participant in
the Eclipse community, keeping up-to-date on release planning and being
a vocal submitter and commenter in bugzilla. She prides herself on being
among the first to try out a new release of a plug-in and blog about it.
She frequently installs plug-ins to check them out, and has submitted
several bugs against Update Manager (and p2) to help simplify tasks. She
has a good working knowledge of the various Eclipse components and
projects. While she is not too concerned with the underlying mechanics
of the install (directory locations, etc.), she does want to know what
components are being installed and why. For example, if she wanted to
install a plug-in that required an upgrade to GEF, she would want to
know this, because she's informed about the different versions and their
impact on her environment.

Things Laurel does regularly:

  - Install new plug-ins found on the web
      - She wishes she could do this in one step rather than Add Site,
        Browse, Install
  - Check for updates to what she has
      - She wants to know what underlying components might be upgraded
        in the process of upgrading her software
  - Uninstall plug-ins after trying them out
  - She rarely restarts Eclipse after an install or uninstall because
    she does it so often

### Dave

Dave is an IT specialist in charge of internal deployment of a
commercial product based on the Eclipse SDK. He wants to try out any and
all updates or add-ons to the Eclipse environment before deciding
whether to deploy them. He is conservative in deploying new
functionality. He currently supports three different configurations of
the product, based on his end users' day to day work. In one
configuration, users are not allowed to update at all. This is
accomplished by limiting the update repositories but he would prefer
that in this configuration, the update UI did not appear at all. He
maintains an internal update repository for deploying approved Eclipse
updates, patches, or add-ons for the other two configurations. It is
vital that Dave can configure the product so that users cannot update or
add plug-ins from any other sources. He configures Eclipse to use
automatic updating. Most of Dave's users aren't familiar with the
Eclipse community. There are some Eclipse-savvy users that install and
maintain their own Eclipse SDK, but Dave does not support these
installs. However, he looks to this subset of users to help him
beta-test new function and configurations.

Things Dave does regularly:

  - Debugs problems in user installs, relying on configuration
    information from the platform
  - Needs to quickly identify which configuration a user is using, which
    updates they applied

Things Dave does occasionally:

  - Evaluates new plug-ins for inclusion in the standard installation
      - Add update sites found on the web
      - Tries out the plug-in and determines whether it should be added
        to the base install
          - Needs to be able to easily install/uninstall a particular
            set of plug-ins into his environment to run his performance
            test suites and analyze resource consumption
          - Wants to be sure that "trial installs" don't leave baggage
            in his test platform. He will be concerned if things he has
            uninstalled still appear in the plugins folder and will lose
            trust of the p2 technology if he feels he must install from
            scratch in order to guarantee a clean testbed
      - Downloads software into staging sites for internal test installs
        (rather than installing from external sites)

### Ellen

Ellen is one of Dave's users. She uses a product based on the Eclipse
SDK and doesn't have any idea what Eclipse is. She updates her software
when instructed to and otherwise doesn't care about the composition of
her product.

Things Ellen does occasionally:

  - When Ellen receives an update notification, she clicks the popup and
    updates the software, but only if she has seen an announcement from
    Dave that an update is available and should be installed.
  - If Ellen has a problem with her software, she contacts support. She
    needs to be able to communicate what versions of things are
    installed in a way that will make sense to Dave.

### Please add your input about these or other personas

Please include your name and project so we can contact you for further
information. We're looking for input about missing users and tasks, or
information to flesh out the existing personas. Consider this a
brainstorming area, we'll clean it up later.

  - Mike Haller, Platform/RCP Application - our users are like Ellen,
    they are in a highly controlled environment and don't update until
    told. In half of the environments, they even have no permission to
    install updates on their local disk. Initial installation of a new
    feature using an update site or updates of existing features still
    require too many steps/clicks and looks complicated, which annoys
    Ellen. My Ellen is not required to know about local/archived/remote
    update sites, features and versions. The product used by my Ellen is
    built and released by Laurel :)

<!-- end list -->

  - In addition to Ellen, I'd like to add Arthur. Arthur is our customer
    who has bought/acquired(\!) our commercial RCP app. He had to use
    NSIS to install it (something had to ensure he had a JRE and
    Bonjour) but after that he will never come back to our website to
    check for the latest version. Instead, when updates are available, a
    small, unobtrusive dialogue box will appear. If he chooses to
    'Install updates', then without any further interaction his app will
    be incrementally updated (just the plugins which have changed,
    keeping the xfer size down) and restarted automatically. The
    behaviour will be almost identical to the brilliant Sparkle
    framework available on the mac - no fuss, no confusion, just end
    user bliss...

\[Susan comment: I'm not sure how different Arthur's tasks are from
Ellen as far as updating goes. He did the initial install and has more
knowledge of the platform, but otherwise I think their needs are the
same???...seamless and unobtrusive automatic updating.\]

  - Miles Parker, PDE/Plugin Developer. (This is a recent and very
    typical user fro me.) Herbert is a developer and technologist
    (university researcher for example) who has experience in a broad
    variety of tools and platforms. He has used Eclipse a bit in the
    past, and may use JDT on a regular basis, but he has had a bad
    experience w/ p2. He does understand software imperfections and is
    not afraid to look under the hood. But he also has a level of
    expectation about how software should work. When something goes
    wrong, he wants to know why and what can be done about it; to be
    presented with all of the information he needs to know to diagnose
    an issue in a simple and orderly way. The challenge with Herbert is
    that he has limited time and a long memory. If he gets over
    aggravation threshold I'll lose him as potential user. And of course
    for every user that requests help there are probably 5 who I'll
    never hear from. (One possible functional improvement that occurred
    to me is to have ability to send feedback to plugin developers on p2
    issues so that we can get notified when failures occur.) Selected
    quotes from "Herbert":

> "I'm setting this up on a \[windows\] box (which is just about the
> worst development platform on earth btw...thank God for a jvm and
> eclipse :) ) ... i do have an os x box, ubuntu linux, and
> solaris..running as well... Thank you for the detail on Eclipse as
> i've yet to make a major switch to Ganymede; I have an extensive
> Europa setup that i didn't want to goon. Moreover, i'm used
> to....dumping features and plugins: I'll have to wise up on the new
> Ganymede install MO... "

> Much later: "here's some quick notes on the install:..Use the Ganymede
> Discovery Site to install GMF, not the update site recommended by the
> GMF page --- dependency errors abound otherwise and you'll have no
> chance..Install \[Dependency\] using the archive option....their
> update site is vaporware at the moment.. \[Dependency 2\] -- initially
> reports "no update site found" then i refresh and it shows up...then
> eclipse crashed with some bs error...After a restart i get a single
> clean \[My Plugin\] folder that then installs flawlessly.The eclipse
> plugin manager is incredibly unstable and buggy; i've decided that i
> hate it at the moment :)"

I include the quotes \*not\* to pile on but to give some insight into
user's debugging and evaluation process.

\[Susan comment:\] Miles, I wonder if Herbert, Steve, and Karen could be
merged into one persona. What they have in common is that they are very
knowledgeable and proficient in their tools and OS, consider Eclipse
update just a minor tool in their workflow (and probably wish it worked
like their favorite platform package mgmt software). The only real
difference as written is that some want to understand the underlying dir
structure/exactly what is being installed, and some do not, they just
want it to work and be stable. Thoughts?....

\[Miles response:\] I'm not sure. It is true that somedays I feel like
Steve, sometimes Herbert and sometimes Karen. :) The primary difference
is that Steve and Karen have strong knowledge and buy-in for Eclipse.
Herbert is a sophisticated user, but he is not proficient at Eclipse to
the extent that things that we take for granted may not be clear. For
example, decoding plugin/feature versioning or even knowing the
difference between the two. He may not know what the dependency chain is
and might want to see that in a transparent way. He wants a clean way to
zero everything back and detect wether things are hopelessly messed up.
He might want partial solutions to get things up and running. "OK, I see
that GMF is missing a dependency, but let me install the rest so I can
take a look at this tool". He is closer to Karen than Steve and perhaps
they can be combined, but there is something essential about Herbert in
that he is a relative newcomer. This is key when you consider that
herbert is a potential platform conquest. Karen and Steve and myself
aren't going anywhere, we have too much invested. But Herbert could be
one of those people that say "I tried Eclipse, but..".

\[Susan response:\] I got rid of Karen and merged some of the salient
points about directory structure, clean removal of plugins, etc. to
Dave. This was based on Markus' comments below. Now I'm left to figure
out how Herbert fits in. Do you mean that Herbert hasn't bought into
Eclipse as an IDE but enough compelling plug-ins would cause him to do
so? Something like "I wanted to install Mylyn because I heard it makes
Eclipse a more usable IDE but I couldn't get past the stupid update UI
to add Mylyn and try it out?"

\[Miles response:\] Sorry guys, I wasn't getting change updates so lost
track. Susan, I think you've captured most of what I was thinking for
Herbert with Steve except for the grumpiness. :) Again key issue was
neophyte Eclipse user but experience diagnosing other systems. I think
the new use cases cover this quite nicely.

\[Markus comment\] I'd like to expand Dave's role slightly, in trying
out plugins

  - \>Evaluates new plug-ins for inclusion in the standard installation
      - \>Adds update sites found on the web
      - Installs plugins into his work installation (IMHO the best way
        to try them - use them during your own work) - preferably into
        an appropriate extension location
      - Needs to remove them from his installation again (preferably
        deleting them)
      - Has used link files so far for the detailed control they give
      - \>Downloads software into staging sites for internal test
        installs (rather than installing from external sites)

\[Susan response:\] Markus, I added some detail to further explain the
steps in evaluating new plug-ins. I stayed away from implementation
terminology (extension location and links) and tried to focus on
requirements, which would be the ability to quickly/easily install and
uninstall, possibly in groups. Does that reflect your intent?

\[Markus response\] Susan, thanks. You have put my concerns very nicely.

## Scenarios

Based on the users above, the following scenarios need to be fleshed
out:

## Get updates

### Dave

  - Dave uses automatic update checking on a daily schedule for all
    three configurations that he supports. This keeps him aware of what
    new versions are available in each supported environment
  - Dave needs to be able to ascertain quickly the nature of the update
      - fixes
      - maintenance upgrades
      - new releases/new function
  - Dave needs to know if accepting an upgrade will upgrade or otherwise
    alter any other component in the system
  - Dave will need to run test suites against any newly upgraded
    configuration
  - Because this process can be quite time-consuming, Dave tends to
    evaluate these upgrades in batches every few weeks, unless it is a
    critical fix that must be evaluated immediately

### Ellen

  - Ellen sees a popup notifying her of new updates
  - Ellen clicks on the popup to see the list of updates
  - Ellen pushes "Finish" and gets on with her work
  - If there are any problems, Ellen will contact support

### Steve

  - Steve gets a bugzilla email that a bug he reported against Eclipse
    has been fixed (finally\!)
  - He is looking for a particular milestone build that has the fix
  - Steve uses "One-click" check for updates to the current system
  - There may be multiple available updates, Steve needs to be able to
    see them all and choose a specific one
  - If there are any dependency errors or other problems in the update,
    Steve will most likely want to bail

out of the update. He has limited time to diagnose problems

### Laurel

  - Laurel checks for updates routinely as well as having automatic
    update checking on a weekly schedule
  - She often has already heard about the update and knows it's coming
  - If she hasn't heard of an update, she wants to quickly ascertain
    whether it's a "bleeding edge" or "maintenance" update
  - If there are dependency errors or other problems, Laurel needs
    enough information to diagnose the problem. She wants to know if an
    update to X requires an update to underlying component Y and would
    like to know why

## Find new stuff

### Laurel

  - Laurel is reading a blog that mentions a new plug-in that is cool.
    She wants to try it.
  - Needs easy way to get the site content into Eclipse
  - Will keep working while it installs and will not restart the system
  - Will take a quick look at the plug-in, evaluate and write about it
  - Wants an easy way to uninstall it
  - Quick evaluation without ever restarting Eclipse
  - Blog away...

### Dave

  - One of Dave's users asks if a new plug-in could be added into the
    standard toolset, sends him a link to the site
  - Needs easy way to get the site content into Eclipse
  - Will restart the system after install
  - Dave wants to know the impact on the running system - disk space,
    memory usage, etc.
  - Runs test suites to confirm that the system runs as expected in the
    presence of the new plug-in
  - Needs to repeat these steps for his other configurations
  - Determination as to which configuration(s) this tool should be
    added, if any
  - If the plug-in is to be considered, he will deploy it to his beta
    configuration users

## Diagnosing problems

### Ellen

  - Ellen is getting an error notification while starting up her
    application
  - Ellen calls support to explain a problem that occurred after
    upgrading her product
  - Support needs information about what she installed
  - She may not remember what she installed, she was just following
    instructions

### Laurel

  - Laurel is getting a workbench error notification after installing
    and applying a plug-in
  - She wants to capture the information quickly and submit a bug
    report. She is going to uninstall the plug-in so she needs
    confidence that the info she is capturing is complete because she
    won't be trying it again
  - She wants to uninstall the plug-in and keep working
  - If the error disappears after uninstall, Laurel is happy

### Dave

  - Dave is getting an error notification after installing a plug-in
  - He wants to uninstall the plug-in and move on (not going to use it).
    He probably won't open a bug about it, but instead will notify the
    person who suggested evaluating the plug-in that it's not ready for
    prime time.
  - Once the error is gone, Dave wants to make sure the plug-in does not
    remain in the system, needs to verify

his configuration

  - Dave will start from scratch if he is not comfortable that the
    uninstall worked

### Steve

  - Steve is getting an Eclipse start-up error
  - He thinks it is related to a recent build update but is not sure
  - He will open a bug and provide the log info
  - He will reinstall his SDK from scratch

## Conclusions

The conclusions have been moved to [Equinox p2 UI 3.5
workflows](Equinox_p2_UI_3.5_workflows "wikilink")

[UI Use Cases](Category:Equinox_p2 "wikilink")