# p2 CUDF Resolver

## What is it?

A frontend to p2 that allows p2 to resolve Linux dependencies. The input
and output format is [CUDF](http://www.mancoosi.org/cudf/). This format
has been designed by the [Mancoosi European
project](http://www.mancoosi.org/) to foster improvements in dependency
resolution solvers. Supporting a different format can be done trivially,
just contact us.

p2cudf did perform quite well during the [Mancoosi internal Solver
Competition (MiSC)](http://www.mancoosi.org/misc-nice-2010). More
information about the aim and the outcome of MiSC on the project [blog
entry](http://blog.mancoosi.org/index.php/2010/02/04/41-running-a-first-mancoosi-internal-solver-competition-outcomes-and-lessons-learned).
The results during the [Mancoosi International Competition (MISC) are
also available](http://www.mancoosi.org/misc-2010/).

## How to get it?

For the moment, we provide a all-in-one jar including all dependencies
for easy testing.

<http://eclipse.org/equinox/p2/p2CUDF/org.eclipse.equinox.p2.cudf-1.14.jar>

June 16, 2010: here are the solvers submitted to
[MISC](http://www.mancoosi.org/misc-2010/)

  - for the [paranoid
    criteria](http://eclipse.org/equinox/p2/p2CUDF/p2cudf-paranoid-1.6.tar)
  - for the [trendy
    criteria](http://eclipse.org/equinox/p2/p2CUDF/p2cudf-trendy-1.6.tar)

Changelog:

  - **September 21,2011**: 1.14 release. Bugfix release of the version
    1.13 that participated to
    [MISC2011](http://www.mancoosi.org/misc-2011/results/index.html).
    Fixed empty solution output and +sum() user criteria.
  - **December 28, 2010**: 1.11 release.Cleaned up version of the code
    available on December 15.
  - **December 15, 2010**: Fixed bug in lexico optimization when a value
    of 0 is found for one criteria. Reduced the slicing to properly
    handle not up to date criteria. Added back support for paranoid and
    trendy criteria.
  - **November 24,2010**: Fixed maximization in user criteria. The
    solver now explicitly mentions if the solution found is optimal or
    not.
  - **November 21,2010**: Moved from PBO to a specific lexico
    optimization procedure. As such, the PBO encoding is not longer
    available for external solvers.
    [Release 1.8](http://eclipse.org/equinox/p2/p2CUDF/p2cudf-1.8.jar)
    is the latest one with such feature. Only user defined criteria are
    supported.
  - **November 16, 2010**: Bugfix release for cases using the
    unmet_recommends criterion or the predefined trendy criteria.
  - **November 8, 2010**: Support for user defined optimization criteria
    as defined by [MISC
    Live 3](http://www.mancoosi.org/misc-live/20101126/).
  - **July 27, 2010**: Bugfix release after analyzing the results of
    MISC. Right encoding of self conflicts. Limit management of
    recommends to objective functions using it. Fixed the encoding of
    optionality. Default timeout is now 5 minutes, instead of 2000
    conflicts. Allows custom objective function design.
  - **June 12, 2010**: Fixed optimization function format in opb
    generated file. Fixed recommends score. Introduced management of
    keep property.
  - **June 2, 2010**: Updated trendy criteria with recommends. Updated
    SAT4J to release 2.2.0. Support for keep property.
  - **April 9, 2010**: Improved running time: assumption-based
    satisfiability removed, updated version of SAT4J.
  - **February 11, 2010**: Fixed optimization function for trendy track.
    Computes MISC scores for each track (in verbose mode). Improved
    backend PB solver.
  - **February 9, 2010**: The solver is now fully deterministic (see bug
    299840). Does only have a non zero exit code in case of
    configuration problem (no longer when no solution is found).
  - **January 24, 2010**: The solver can now be stopped using SIGTERM or
    Ctrl-C: it will display the best solution found so far. The
    explanation in case of failure now appears in the output file (if
    -explain is used).

<!-- end list -->

  - **January 15, 2010**: Initial release.

## How to use it?

The resolver uses the following command line:

``` bash

java -jar p2cudf.jar [flags] inputFile [outputFile]
-obj (paranoid | trendy | p2 | <user defined>)     The objective function to be used to resolve the problem. p2 is used by default.
                                  Users can define their own: +new,-changed,-notuptodate,-unmet_recommends,-removed
-timeout <number>(c|s)            The time out after which the solver will stop. e.g. 10s stops after 10 seconds,
                                  10c stops after 10 conflicts.
                                  Default is set to 200c for p2 and 2000c for other objective functions.
-sort                             Sorts the output.
-explain                          Provides one reason of the unability to fullfil the request
-verbose                          Displays the state of the solver on the standard output
-encoding                         Translates the CUDF request into a pseudo boolean optimization problem

inputFile          The fully path to a CUDF formatted file describing the universe and the query.
outputFile        The fully path to a CUDF formatted file describing the packages to install. If no output file is provided, the solution is displayed on STDOUT.
```

The following command line launches the solver for 100 seconds on the
problem foo.cudf using the p2 optimization criteria.

``` bash
java -Xmx512m -jar p2cudf.jar -timeout 100s -obj p2 foo.cudf output.cudf
```

The main advantage of using a conflict based timeout is that the results
of the solver should be identical on all architectures, while the result
provided by a time based timeout is likely to differ from one machine to
the other.



## Objective functions

paranoid: The paranoid objective function will focus on returning a
solution with the least change possible from the original solution.

trendy: The trendy objective function will focus on installing the most
up to date version for each package.

It is possible to use a custom optimization criteria using those
criterion:

  - new: number of newly installed packages
  - changed: number of packages that changed (installed, removed, or
    version change)
  - notuptodate: number of installed packages for which there exists a
    newer version not installed
  - unmet_recommends: number of packages recommended but not installed.
  - removed: number of packages that were installed and are no longer
    installed.
  - sum(property): sum up the values of attribute "property". Obviously,
    those values are expected to be a number.

Each criterion can be either maximized by prefixing it by + or minimized
by prefixing it by -.

Paranoid is equivalent to -removed,-changed and trendy is equivalent to
-removed,-notuptodate,-unmet_recommends,-new.

See [MISC optimization criteria for more
details](http://www.mancoosi.org/misc-live/20101126/criteria/).

## Where can I get the code?

Since August 2011, p2cudf is now hosted on its own git repository

Former versions of the source code can still be found on equinox
incubator CVS repository.

repository: :pserver:anonymous@dev.eclipse.org:/cvsroot/rt/

module: org.eclipse.equinox/incubator/p2/demos/misc-conf-2010/



## Relationship with p2 in Eclipse

This solver is designed and maintained by p2 committers. It is a
simplified version of the code being shipped in Eclipse p2 because CUDF
is simpler than p2 metadata, but it still uses our SAT based
([SAT4J](http://sat4j.org)) approach like described in our
[publication](http://www.cril.univ-artois.fr/spip/publications/iwoce907-leberre.pdf).



## Reporting bugs?

Create a bug in the p2 component in [Equinox's
Bugzilla](https://bugs.eclipse.org/bugs/enter_bug.cgi?product=Equinox).



## Getting in touch with the developers?

Just send an email to the developer mailing list:
[p2-dev@eclipse.org](https://dev.eclipse.org/mailman/listinfo/p2-dev).



## License?

This code is made available under the terms of the EPL:
<http://eclipse.org/org/documents/epl-v10.php>

[Category:Equinox_p2](Category:Equinox_p2 "wikilink")