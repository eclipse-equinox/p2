## Greater flexibility around touchpoints and actions

Currently an IU references a single touchpoint and then may only use
actions from that touchpoint while performing the provisioning operation
in the Engine. As a result of this restriction we've ended up with
duplicate actions (e.g. chmod, mkdir, rmdir) in both the "eclipse" and
"native" touchpoints. In addition, any user wanting to do anything
outside of what's scoped by our current actions is forced to write their
own touchpoint and collection of actions.

There is a bug open covering this requirement here --
<https://bugs.eclipse.org/bugs/show_bug.cgi?id=203323> (completed)

Approaches under consideration:

  - Add an "actions" extension point (touchpint reference and IU
    authoring metadata). This changes the action/touchpoint relationship
    so that an action now refers to a touchpoint instead of the other
    way around.

`-name`
`-version`
`-class`
`-touchpointType`
`-touchpointVersion`

  - Adjust the Engine's action lookup to use actions extensions. This
    might force us to adjust some of the lifecycle events for
    touchpoints as they should only be involved in phases where there
    are associated actions.
  - When referencing an action in the metadata we want to add an
    "actions" attribute to help disambiguate actions with the same name
    from different providers. As a result the Touchpoint Data
    instructions will likely have to be updated to include information
    to disambiguate actions.

<instruction key='configure'
  import='org.eclipse.equinox.p2.touchpoint.eclipse.setStartLevel;version=[1.0,2.0),
   org.eclipse.equinox.p2.touchpoint.eclipse.markStarted;version=[1.0,2.0)'>
` setStartLevel(startLevel:2);markStarted(started: true);`
</instruction>

  - To try to be backwards compatible with existing action metadata,
    upon encountering an unqualified action name the actionlookup
    algorithm will give the IU's touchpoint a chance to qualify the
    action name. For example the eclipse touchpoint might qualify the
    action name "installBundle" into the qualified name
    "org.eclipse.equinox.p2.touchpoint.eclipse.installBundle".

Some questions we're not sure about:

  - What if anything do we want to do about substitutability of actions?

## Authoring and validation metadata in touchpoints, actions and phases

We have a general lack of validation mechanisms that provide the user
the oppportunity to apply corrective action. For example, there is no
way to indicate the need for user interaction when required
configuration properties are missing. In addition there is a shortage of
useful metadata to allow one to construct a useful IU authoring
environment without having to know the inner details of phases,
touchpoints, and actions.

Approaches under consideration:

  - Add "description" elements to the touchpoint and action extensions
    to allow one to add a localizable description of the item.
  - Consider adding a "phases" extension just to hold metadata useful
    for authoring and validation
  - Add some paramater validation metadata. For example a touchpoint
    might want to add some metadata to describe it's profile properties
    requirements. The user might then get a specially purposed UI to add
    those properties.

`-profileProperties`
` -name`
` -description (localizable)`
` -type (string, number, boolean)`
` -use (optional, required)`
` -default (implies optional)`

  - One good source we already have for some of this metadata is
    [Equinox/p2/Engine/Touchpoint
    Instructions](Equinox/p2/Engine/Touchpoint_Instructions "wikilink").
  - Add new validation phases similar to sizing for unconfigure,
    uninstall, property, install, configure. This will allow us to
    create a validation phase set the ui or other clients can run to
    determine if there are elements used by the engine that need to be
    first installed or configured.

## Install Handler like mechanism

We currently lack a general mechanism to allow a user to use actions
that are not currently installed/avialable in the agent while in the
process of an install. This is particularly important since Update
Manager had Install Handlers for this usecase.

Approaches under consideration:

  - Provide is a special "dynamicAction" that allow one to reference an
    artifact in the IU and then load a special class from a jar file and
    run it. This is similar to what we had with InstallHandlers and as a
    consequence will have the same limitations around authoring and
    build.
  - Sort out a way to let us install the necessary bundles that contain
    the actions and touchpoints we need to proceed in an install. This
    might be a workflow we drive through the UI or just do behind the
    scenes in a nested transaction.

[Category:Equinox_p2](Category:Equinox_p2 "wikilink")