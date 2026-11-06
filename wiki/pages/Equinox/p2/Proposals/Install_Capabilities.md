This page outlines a proposed functionality as requested in

<h2>

Motivation

</h2>

Eclipse is experiencing a tension between **encapsulation** and
**flexibility**.

  - Encapsulation: is the basis for many fundamental properties of our
    software and of the development process. Encapsulation is enforced
    by various rules and tools (compiler, OSGi, API tooling, etc.)
    Flexibility: is the basis for composing different plug-ins in ways
    that were not anticipated (nor actively supported) by the developers
    of a plug-in (accessing internal classes, bytecode weaving,
    reflection are the most obvious cases).

The eclipse community isn't fully open about this tension. We tend to
say that all plug-ins play by the rules of encapsulation, but if these
rules were to be fully enforced we'd have to discontinue a significant
number of established plug-ins which currently *cannot* be squeezed into
these rules.

Given that we don't want to fully give up the virtue of encapsulation we
need a way to make ourselves honest about it.

<h2>

Funny things that plug-ins do

</h2>

This is a list of potential effects of installing a plug-in which may
not be expected by users believing in the well-encapsulated-black-box
story:

  - p2 touchpoint actions
      - modify eclipse.ini / config.ini
      - perform file system actions
      - ...
  - plug-ins accessing internal classes of other plug-ins
  - bytecode weaving (like OT/Equinox, equinox aspects etc.)
      - insert hooks into other plug-ins' byte code
      - change accessibility of classes/methods/fields
  - use reflection in order to break just any rule

Other items that may scare some users are:

  - p2 may install more than what was requested (including the use of
    repositories not explicitly enabled, this caused the outcry in )
  - unsigned jars may be downloaded and installed

<h2>

Avoid the extremes

</h2>

Completely abandoning the rules about encapsulation may cause death by
chaotic evolution, whereas strictly enforcing all rules may cause death
by stagnation.

As a solution I propose to give control to the consumers of our
technology, which in the end means encapsulation need not be a matter of
all-or-nothing but will become subject to negotiation (for more
background see this paper on [Gradual
Encapsulation](http://www.jot.fm/issues/issue_2008_12/article3.pdf)).

As the core concept for new kinds of interactions I propose **install
capabilities**.

<h2>

Enter: "Install Capabilities"

</h2>

Similar to regular **capabilities** in p2, *install capabilities* can be
used to declare requirements of a plug-in. However, an install
capabilities does not refer to another plug-in that needs to be
installed, but these are capabilities that a plug-in requests from the
framework in order to perform its installation and start-up.

The following steps outline the generation and consumption of metadata
relating to install capabilities.

<h3>

Building

</h3>

All requests for install capabilities should be known at the time of
building. Some are already available (e.g., p2.inf for touchpoint
instructions, or OT/Euqinox aspectBindings from plugin.xml). Some are
actually detected *during* building (discouraged access of internals).
All this information should be collected in the artifact's metadata.

<h3>

Installing

</h3>

One way to grant requested install capabilities would be via user
interaction: the p2 install wizard should therefor display information
about these requests and ask the user for permission. Any requests not
confirmed by the user will mean that the requesting plug-in cannot be
installed.

<h3>

Pre-configured policies

</h3>

In order to avoid the additional clicks during install, Eclipse could be
pre-configured with a policy that grants certain permissions in advance.
Policiy files could be provided that reflect a company's policy, e.g.

<h2>

Details

</h2>

<h3>

Metadata

</h3>

In the long run it would be great to define a universal format for all
kinds of install capabilities. For first experiments existing
information (like touchpoint instructions) could be used as is. Other
requests could be added as simple properties. The following might be
fairly easy to add during building:

  - a flag if a jar is (not) signed
  - number of forbidden/discouraged access warnings

Some install capabilities might actually carry quite some details, but a
common theme would be to mention a set of affected plug-ins (relevant
for access to internals, bytecode weaving and perhaps more).

<h3>

UI

</h3>

A common worry against this proposal is the clutter of UI with yet more
questions that no user will actually want to look at. Actually, the main
point behind this proposal is: we don't know what all the users will
actually care about, and it's not likely that all users will have the
exact same concerns. Therefor, the UI must be as unobtrusive as
possible, yet providing the opportunity to explore matters at will.

I've heard two proposals so far, how this can be integrated into the
existing UI:

  - the license dialog is already a stop where users must confirm before
    proceeding
  - in a similar vein also step 2 of the install wizard ("Installation
    Details") is related: by saying "Review the items to be installed"
    this page, too, asks for a confirmation after a solution to the
    install request has been found.

Whichever page is chosen I propose to just add:

  - a text line reporting that specific install capabilities have been
    requested

<!-- end list -->

  -
    this text should be a link to a full report of requests

<!-- end list -->

  - a checkbox for confirming all requests

In the future the detail page (report) could be extended to provide a
nice hierarchical drill-down so a user can gradually dive into the
details and confirm all or just a subset of those requests. Whenever any
requests are not confirmed, p2 should go back and try to find a solution
without the denied plug-ins.

<h3>

Policy files

</h3>

I propose to use a common file format for storing all granted requests,
either resulting from clicking in the UI or as pre-configured policy
files. These files will also come handy if some details should be
enforced at runtime (e.g., bytecode weaving indeed only affects the
declared plug-ins). A simple variant of these files would need to record
these bits of information:

  - requesting plug-in
  - ID of the requested install capability
  - affected plug-in

It's probably a good idea to support a detail-field where details
specific to a given install capability can be stored (e.g., list of
affected classes for internal-access).

[Install Capabilities](Category:Equinox_p2 "wikilink")