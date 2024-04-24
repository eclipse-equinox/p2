Equinox/p2/Engine
=================

Contents
--------

*   [1 Overview](#Overview)
*   [2 Engine API](#Engine-API)
*   [3 Usage](#Usage)
*   [4 Touchpoint Instructions, Actions and Variables](#Touchpoint-Instructions.2C-Actions-and-Variables)
*   [5 Extra Notes](#Extra-Notes)

Overview
--------

*   The p2 Engine is the component that supports modification to the contents of an installation profile.
*   Key Concepts:
    *   Profile - Concrete model of an installation (e.g. install directory and the specific IUs installed)
    *   PhaseSet - The collection of in order phases that will be carried out during a call to the Engine.
    *   Phase - Phases are typically named to collect similar operations together. For example the "install" phase.
    *   Operand - A pair of IUs used to represent (before, after) state. The phases use operands to determine what actions to carry out.
    *   Touchpoint - Each IU is associated with a touchpoint which identify for what type of product it's for. For example the bundles for Eclipse are associated with the Eclipse touchpoint.
    *   Action - Actions are the unit of work done by the Engine and are used to modify the contents of a profile

Engine API
----------

The primary means of interaction is through the perform method:

 **public MultiStatus perform(Profile profile, PhaseSet phaseSet, Operand\[\] operands, IProgressMonitor monitor)**
                                       

*   _profile_ is the Profile you want to modify
*   _phaseSet_ provides the set of Phase(s) that you want the engine to run through
*   _operands_ is the set of IU pairs you want to operate over
*   _monitor_ is used to provide incremental status feedback.

Each call to the engine is atomic - either all changes happen or in the event of failure no changes occur. To the caller this is indicated by the MultiStatus object returned - OK signals success, ERROR signals a failure.

Usage
-----

Using a custom PhaseSet is supported but in most cases an instance of the "DefaultPhaseSet" should be used for installation operations. The DefaultPhaseSet is made up of the following in-order Phases:

*   _Collect_ is used to mirror any artifacts identified in the IUs so that they are available for later steps in the installation.
*   _Unconfigure_ is used to remove the configuration information for an IU from the profile or touchpoint specific locations.
*   _Uninstall_ removes the IU from the profile and performs any actions required to remove it from the touchpoint.
*   _Install_ adds the IU to the profile and performs any of the actions required by the touchpoint to perform and installation
*   _Configure_ is used to perform any configuration steps required for the IU.

Each Phase is responsible for determining the appropriate Actions to execute for a particular operand. The collect phase has an implicit behaviour to download the associated artifacts. The unconfigure, uninstall, install, and configure Phases will all look for "instructions" in the touchpoint data section with the same name. For example during the "install" phase the IU's touchpoint data section is queried for "install" instructions.

The format for the instruction section is as follows:

  **actionName( paramName1 : paramValue1, paramName2 : paramValue2, paramName3 : paramValue3);**

An instruction may contain multiple action statements. The Phase will lookup the "actionName" using it's own phase specific actions and also those made available by the associated touchpoint. For example the set of "install" instructions for a bundle might consist of the following:

  **installBundle(bundle:${artifact});**

*   _installBundle_ is the action name
*   _bundle_ is the parameter name
*   _${artifact}_ is the parameter value. The value ${artifact} signifies the use of a pre-defined variable named "artifact".

Touchpoint Instructions, Actions and Variables
----------------------------------------------

See [here](/Equinox/p2/Engine/Touchpoint_Instructions "Equinox/p2/Engine/Touchpoint Instructions") for information on touchpoint instructions.

Extra Notes
-----------

(Be aware that much of this discussion is out of date and was part of the discussion during the design of the p2 engine)

*   Is the engine only invoked for provisioning related operations? For example could it be used to obtain the log file of a particular touchpoint.
    *   For now the answer is no. The engine is only used for provisioning related operations that cause modification of the set of IUs installed in a profile.

*   Some additonal operations that we \*might\* need to invoke on the engine. Current thinking is that introspection operations should occur outside of the scope of an Engine session. We have not dug too deeply here so this might change.
    *   Reboot the targeted touchpoint.
    *   Compute the size of things being downloaded. It would invoke the touchpoint to actually know whether or not the IU should be obtained.
    *   Validation of checks contained in IUs
    *   Qualification, for example discovery of an eclipse install

*   Engine API:
    *   The OSGi DMT Admin service has an interesting API that we should mimic. Specifically DMTAdmin.getSession
        *   A mapping of some of the DMT Admin concepts would require us to look at the API as acting on a single profile instead of a sub-tree.
        *   The DMT Admin service has the concept of a DMTSession for holding the current state for deployment activity. This might be useful internally to track transactional state. It's not clear yet if we want to expose the transactionactional state in the public API however we should leave that option open.
    *   The parameters passed to the engine do not give us flexibility for other operations than Install and Uninstall. {Done}
        *   We still want to keep this grammar as small as possible: install / update / uninstall until we have concrete requirements for more operations.
        *   We're currently passing a "single" operation and set of operands. We should move to a model where we pass a set of operations with internal operands. This allows us to "perform" a mixture of the various operations in a single call to the Engine.
        *   The scope of an Engine "perform" command is a single profile and only one Engine provisioning session should be active on a profile at one time. This has some consequences for transactions that we will need to think about.

*   Engine processing model and phases:
    *   The engine is built on a model where there is a "fixed" set of phases
    *   When running a partiuclar set of operations each operation is in turn asked if it will participate in a particular phase.
    *   Currently the processing is done breadth first, in that first all the IUs are being fetched, then they are all installed and finally configured (where fetch, install, and configure are phases). Is that too strict? Should we allow for some phases to specify how they should be run?
    *   One benefit of breadth first is that ordering of operations is no longer a problem however the phase ordering is static.
    *   Depth first more or less has the opposite characteristics where we have greater control of the ordering of phases that make up an operation however this is at the expense of greater complexity in terms of ordering a set of provisioning operations.
    *   A first try at an in-order set of phases is as follows:
        *   collect
        *   validate
        *   uninstall / unconfigure
        *   update / migrate
        *   install / initconfig
        *   configure
        *   verify

*   Operations / Phases / Actions:
    *   Phase-data is represented in an IUs touchpoint data. An IU's phase-data will eventually consist of a series of atomic "actions". The actual definition of actions should probable be declarative. Currently this is done in JavaScript.
    *   Our mapping of operations to phases is as follows.
        *   Install - Collect, Validate, Install / InitConfig, Configure, Verify.
        *   Update - Collect, Validate, Update / Migrate, Configure, Verify.
        *   Uninstall - Validate, Uninstall / UnConfigure, Verify.

