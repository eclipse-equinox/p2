[Equinox p2](Equinox_p2 "wikilink") needs the ability to filter and
select what gets installed. This page contains some rambling thoughts on
filters.

### Different reasons for filtering

There are various reasons why we need filtering capabilities:

1.  Filters based on properties of my installation environment.
      - os, ws, nl, arch
      - It makes sense to specify these filters directly on the affected
        IU Often this is an implementation detail of the producer, and
        is not a concern for a consumer (group). Today perhaps
        core.resources requires win32, and tomorrow perhaps it will not.
2.  Filters as a way slicing up functionality (core, UI, doc, help, etc)
      - It is the producer's role to define what slices are available
      - It is the consumer's role to decide what slices they want
      - A consumer higher in the dependency chain may want to override a
        decision made lower down in the chain.
      - A consumer higher in the chain may want to constrict/simply the
        set of choices available lower down

### Different ways to implement filters

There are different ways of exposing filters in the metadata. If you
think of provisioning metadata as a dependency graph, filters can be
defined on edges and/or on nodes:

1.  Node filter. Filter defined directly on the IU. This satisfies use
    1) above. This doesn't work for 2), because different consumers may
    want to apply filters different.

<!-- end list -->

1.  Edge filter. Filter defined on a RequiredCapability. This doesn't
    make sense for reason 1) because it exposes producer implementation
    details on the consumer. However, it's useful for case 2). Each
    "edge" would have filters of form (key=value), where a match will
    result in the edge being traversed during an install. The filter is
    evaluated against an environment that is passed down from the
    producer. Each producer on the route to an edge can alter the
    environment (think dynamic scoping). Since multiple producers can
    define a requirement on the same downstream IU, each IU may be
    visited multiple times with different filter environments.

[Filters](Category:Equinox_p2 "wikilink")