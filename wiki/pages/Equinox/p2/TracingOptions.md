## Introduction

For tracing where things are being downloaded from *both* of the
following must be specified in an options file:

  - Generally enable tracing for p2.core:

` org.eclipse.equinox.p2.core/debug=true`

  - Tracing where things are being downloaded from:

` org.eclipse.equinox.p2.core/artifacts/mirrors=true`

Then invoke the desired p2 application with ` -debug
 `*`path_to_your_options_file`*.

## All Debug Options

  - p2 Core

`   # this setting must be enabled for the other options to be usable`
`   org.eclipse.equinox.p2.core/debug=false`
`   org.eclipse.equinox.p2.core/artifacts/mirrors=false`
`   org.eclipse.equinox.p2.core/core/parseproblems=false`
`   org.eclipse.equinox.p2.core/core/removeRepo=false`
`   org.eclipse.equinox.p2.core/engine/installregistry=false`
`   org.eclipse.equinox.p2.core/engine/profilepreferences=false`
`   org.eclipse.equinox.p2.core/events/client=false`
`   org.eclipse.equinox.p2.core/generator/parsing=false`
`   org.eclipse.equinox.p2.core/metadata/parsing=false`
`   org.eclipse.equinox.p2.core/planner/encoding=false`
`   org.eclipse.equinox.p2.core/planner/operands=false`
`   org.eclipse.equinox.p2.core/planner/projector=false`
`   org.eclipse.equinox.p2.core/publisher=false`
`   org.eclipse.equinox.p2.core/reconciler=false`
`   org.eclipse.equinox.p2.core/ui/default=false`
`   org.eclipse.equinox.p2.core/updatechecker=false`

  - p2 Engine

`   org.eclipse.equinox.p2.engine/certificatechecker/unsigned=false`
`   org.eclipse.equinox.p2.engine/certificatechecker/untrusted=false`
`   org.eclipse.equinox.p2.engine/engine/debug=false`
`   org.eclipse.equinox.p2.engine/enginesession/debug=false`
`   org.eclipse.equinox.p2.engine/profileregistry/debug=false`

  - p2 Repository

`   org.eclipse.equinox.p2.repository/credentials/debug=false`
`   org.eclipse.equinox.p2.repository/keyservice/debug=false`
`   org.eclipse.equinox.p2.repository/transport/debug=false`

## Sources

  - [p2.core
    (Tracing.java)](https://git.eclipse.org/c/equinox/rt.equinox.p2.git/tree/bundles/org.eclipse.equinox.p2.core/src/org/eclipse/equinox/internal/p2/core/helpers/Tracing.java)
  - [p2.engine
    (DebugHelper.java)](https://git.eclipse.org/c/equinox/rt.equinox.p2.git/tree/bundles/org.eclipse.equinox.p2.engine/src/org/eclipse/equinox/internal/p2/engine/DebugHelper.java)
  - [p2.repository
    (DebugHelper.java)](https://git.eclipse.org/c/equinox/rt.equinox.p2.git/tree/bundles/org.eclipse.equinox.p2.repository/src/org/eclipse/equinox/internal/p2/repository/helpers/DebugHelper.java)

## See also

[FAQ_How_do_I_use_the_platform_debug_tracing_facility](FAQ_How_do_I_use_the_platform_debug_tracing_facility "wikilink")

[TracingOptions](Category:Equinox_p2 "wikilink")