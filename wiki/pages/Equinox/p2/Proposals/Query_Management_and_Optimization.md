**Problem:** Query and result sets are too tightly coupled.

**Objective:** Decouple the query from the results it returns

**Goals:**

1.  Allow those implementing IQueryable to implement optimized versions
    of the query
2.  Return results in a lazy / incremental way

See [Bug 256418](https://bugs.eclipse.org/bugs/show_bug.cgi?id=256418)
for more information.

While p2 is designed to support alternate storage for things like IUs,
providing optimized mechanisms for querying these locations is not
currently possible. This is because the Query and the Results
(Collector) are too tightly coupled and there is no guarantee about the
semantics of a Query. This (working draft) proposal will help identify
the current limitations and present concrete suggestions on how to
address these issues.

This proposal contains five (5) work areas:

1.  Allow for composite queries. This includes both Querying the results
    of a previous query and composing several queries up front.
2.  Remove the complexities surrounding collectors. Many collectors act
    as queries, hampering the repositories ability to create custom
    implementations
3.  Remove the requirement that the collector passed in is the collector
    returned (possibly even remove the collector argument to IQueryable)
4.  Define standard queries as "non-opaque" meaning repository
    implementers could supply custom implementations using SQL or some
    other knowledge about the "thing" being queried
5.  Design QueryResults (Collectors) as a Future, that is, something
    that can continually gather results and the caller can check
    available results, listen for results and get the results. This also
    includes QueryStatus,

## Support Composite Queries

While p2 does have a compound query, this query does not work if the
compounded queries require more context than "isMatch" can support. For
example, queries that have overridden perform() will not work. To solve
this, there are two things that should be done:

1.  Make the collectors themselves Queryable ([Bug
    \#260112](https://bugs.eclipse.org/bugs/show_bug.cgi?id=260112))
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
2.  Change CompoundQuery so the perform logic properly calls perform()
    on each Compounded Query
    ([Bug 260012](https://bugs.eclipse.org/bugs/show_bug.cgi?id=260012))
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
3.  Separate "IsMatchQueries" from "ContextQueries" (this also discussed
    in
    ([Bug 260012](https://bugs.eclipse.org/bugs/show_bug.cgi?id=260012))
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")

## Simplify the Collectors

Currently, many collectors have been implemented as "Queries". This
means that the collectors have the logic of whether or not something
should be included in the result set. This implementation allows
collectors to essentially be used as "composite queries". This has a
number of drawbacks, including:

1.  Query implementers cannot account for this when creating alternative
    query implementations.
2.  This adds complexity when trying to design asynchronous query
    results
3.  Limits reuse as some queries use the Query class isMatch method
    while other use the collector accept method.

To address this issue, we propose re-writing complex collectors (those
that override the accept method), in terms of a Query. In particular,
this affects:

  - HasMatchCollector (This should be refactored into an Acceptor), not
    real query logic here ![Image:Glass.gif](images/Glass.gif
    "Image:Glass.gif")
  - LatestIUVersionCollector
    ([Bug 261460](https://bugs.eclipse.org/bugs/show_bug.cgi?id=260214))![Image:Ok_green.gif](images/Ok_green.gif
    "Image:Ok_green.gif")
  - AvailableIUCollector
    ([Bug 260950](https://bugs.eclipse.org/bugs/show_bug.cgi?id=260105))
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
  - LatestIUVersionElementCollector
    ([Bug 260950](https://bugs.eclipse.org/bugs/show_bug.cgi?id=260105))
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
  - CategoryElementCollector
    ([Bug 260950](https://bugs.eclipse.org/bugs/show_bug.cgi?id=260105))
    ![Image:Ok_green.gif](images/Ok_green.gif "Image:Ok_green.gif")
  - InstalledIUCollector
    ([Bug 260950](https://bugs.eclipse.org/bugs/show_bug.cgi?id=260105))![Image:Ok_green.gif](images/Ok_green.gif
    "Image:Ok_green.gif")
  - ProductQuery.Collector (This is a latestIU collector, it can be
    re-written to use CompositeCollector) ![Image:Glass.gif](images/Glass.gif
    "Image:Glass.gif")
  - IUPropertyUtils.localeFragmentCollector
    ([Bug 262042](https://bugs.eclipse.org/bugs/show_bug.cgi?id=262042))
    ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")
  - IUPropertyUtils.hostLocalizationCollector
    ([Bug 262042](https://bugs.eclipse.org/bugs/show_bug.cgi?id=262042))
    ![Image:Progress.gif](images/Progress.gif "Image:Progress.gif")

There are also a few other queries that provide a custom mechanism for
storing the results (i.e. group IUs into categories, etc...). These
should also be reviewed.

## Remove the collector argument to IQueryable\#query()

Once the collectors have been simplified, there will be no need for the
client to force a collector on the receiver, and the receiver will be
free to construct a collector however they see fit.
([Bug 256355](https://bugs.eclipse.org/bugs/show_bug.cgi?id=256355))
![Image:Glass.gif](images/Glass.gif "Image:Glass.gif")

## Non Opaque Queries

To help repository implementers craft custom queries to represent many
of the "standard" p2 queries, a set of non-opaque (i.e. transparent)
queries should be provided. These queries should have well documented
semantics and allow query implementers to access the properties of the
query without explicitly depending on the Query itself.

([Bug 256412](https://bugs.eclipse.org/bugs/show_bug.cgi?id=256412))
![Image:Glass.gif](images/Glass.gif "Image:Glass.gif")

To support alternative implementations of Queries, we propose adding the
following two methods to Query.java:

``` java
/**
 * Indicates whether or not this Query is Transparent.  The properties of Transparent queries can be
 * accessed via the <link>getProperty</link> method.  Each transparent query should provide a
 * get<PropertyName>() method for each parameter. In addition to this, each property should be describe as a
 * constant at the top of the class.  For example, the CapabilityQuery should define:
 *
 * public static final String REQUIRED_CAPABILITIES = "RequiredCapabilities";
 *
 * public IRequiredCapability[] getRequiredCapabilities();
 *
 * This query should also override isTransparent to return "true".
 *
 */
public boolean isTransparent {
    return false;
}

/**
 * Returns a particular property of a given Query.
 */
public Object getProperty(String property) {
  // Reflectively look up the property and call the getter to get it
  // If it fails, return null;
}
```

Implementors of IQueryable can use these methods to construct
alternative mechanisms of querying their data. For example, a DB backed
IQueryable may use these methods to construct an SQL statement.

``` java
class MyRepository implements IQueryable {
  public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
    if (query.isTransparent() && query.getClass().getName().equals("CapabilityQuery") {
        Object o = query.getProperty("RequiredCapabilities");
        if ( o != null ) {
           IRequiredCapability[] capabilities = (IRequiredCapability[]);
           SQLStatement statement = constructCapabilityQuery(capabilities);
           executeSQL(statement);
           return results;
        }
    }
    return query.perform(getIterator, collector);
  }
}
```

## Design Query Results (Collectors) as a Future

This needs to be filled in, but here are some basic requirements:

  - Support asynchronous data collection, that is, don't block when the
    query perform happens
  - Collect a number of items at one time
      - Support Polling (query.isDataAvailable(int numberOfDatum))
      - Support blocking (query.waitUntilDataIsAvailable(int
        numberOfDatum))
  - Restart query (Possibly add a new query to the collector, and
    restart the query)
  - End Query
  - Get the status of the Query (working, done, broken, etc..)
    ([Bug 256435](https://bugs.eclipse.org/bugs/show_bug.cgi?id=256435))

[Proposals/Query Management and
Optimization](Category:Equinox_p2 "wikilink")