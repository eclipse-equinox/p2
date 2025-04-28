# Equinox p2 Query Language

### Background

As p2 gets more widely used, we predict that repositories will grow larger and larger over time.
The Galileo repository alone contains 3866 IU's (in one single service release).
Starting with Helios, we plan to keep everything over time which will put the IU count close to 10.000.
And that's just one year of Eclipse IP approved material.
OSGi is gaining in popularity and p2 is getting increasingly popular.
Some companies plan to map the central maven repository as p2.
We can soon expect to see p2 repositories with over a 100.000 IU's in them.

#### The Problem

p2 has a query mechanism today that makes it hard to create a repository implementation that is based on a database.
It is also diffiult to create an efficient client/server solution.
The reason for this is that most of the queries are written as implementations of the IQuery interface, either as ContextQuery derivates that need access to all rows in the database, or as MatchQuery derivates with a java method to which all rows need to be passed.

There is no uniform way to translate these queries into an expression that can be understood by a database or by something running in another process.

Some attempts have been made to rectify this and a repository implementation could potentially make some intelligent decisions based on information derived from looking at the query (what class it is, and in some cases, what properties that are exposed).
While this is an improvement, the general problem remains; 
A large amount of the queries are still more or less completely black-boxed and it's enough with one single such query to force a full selection of everything from the database/remote server.

The p2QL expression language discussed here is an attempt to rectify this problem.
It can be used in two ways:

*   Coexisting with current IQuery / IQueryable
    
    Using the ExpressionContextQuery (implements IQuery), or the ExpressionQuery (implements IMatchQuery), it can coexist with todays IQuery/IQueryable.
    The queries are created based on an expression and an array of parameters.
    See section [#IQuery examples](#IQuery-examples).
    
*   With more explicit separation of concerns
    
    It can also be used in an alternative solution where an expression returns an Iterator.
    This usage scenario is particularly useful when implementing remote repositories or implementations on top of a database.
    It is also a very efficient way of querying for things that you don't want to collect in memory.
    

#### Bugzilla

We discussed the use of a query language at length on the p2 meeting on November 9 and November 16.
The issue is captured in bugzilla [Create a QueryLanguage for p2](https://bugs.eclipse.org/bugs/show_bug.cgi?id=294691).

### Language Design

TODO: Add some text here to explain where the ideas stem from (xtend, xtext, Scala, Java) and other motivation behind the choice of syntax.

#### The Two Major Ingredients of p2ql:

*   The language itself
*   How the language integrates with and interacts with Java objects

##### The Language Itself

A query language at its heart is simply an expression evaluation language for two major kinds of expressions:

*   Iterating over a collection and subsetting it in some way
*   Supplying a boolean expression that is used as a callback when some other object iterates over a collection and computes a subset.

That, then is what p2ql is: a simple language for describing subsets of collections.

##### Language Integration with Java Objects

Since p2ql must execute inside a Java runtime, it must interact with Java objects.
In the context of P2, there are two kinds of Java objects that a query language must interact with:

*   A P2 IQueryable
*   Arbitrary Java objects that are passed in as parameters to the query

#### Implementation

p2ql itself is a small dynamically-typed language in the Smalltalk tradition.
It has receivers (objects) and messages (methods), and whether an object can receive a message is determined dynamically at runtime.
Even so, because it automatically takes advantage of various opportunities for concurrency, it is very fast.
It is also a pure functional language, which means that all variables are final.
As was noted before, it is an expression language--which means that while you can use predefined objects and functions, you cannot define your own objects nor your own named functions.

However, you can define anonymous functions (lambdas, closures, anonymous runnables) in the same way that a SQL "where" clause is really an anonymous function or a nested SQL query is also an anonymous function.

#### Java Bindings

When you define a p2ql query, you are really dynamically defining and running a new method on a P2 IQueryable object, usually a metadata repository.

```
IQuery<IInstallableUnit> q = QueryUtil.createMatchQuery("this.id == $0", id);
metadataRepository.query(q);
```

##### Types of Queries

There are two main kinds of queries.

*   Queries that define a boolean expression to evaluate against every element in the IQueryable.
    This is similar to just defining a "where" clause in SQL.

```
IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery("this.id == $0", id);
```

*   Queries that subset the IQueryable's "collection contents" and return a new collection

```
IQuery<IInstallableUnit> query = QueryUtil.createQuery("everything.select(x | x.id == $0)", id);
```

##### Variable Binding

Within these types of queries, variables are bound in the following ways:

*   In a match query, "this" refers to the "current row object" in the IQueryable.

*   In a generic query, "everything" refers to the IQueryable collection itself.

*   Dot notation follows the following rules:
    *   If the receiver of the message is an Iterable or an IQueryable, the message must be a function that iterates across the collection's elements.
        (These predefined functions are described below.)
    *   If the receiver of the message is a POJO, then the property name denoted by the message is converted into a "getRegurlarProperty" or "isBooleanProperty" Java call and the property value is returned.

So, in the examples above, this.id is the same as writing "rowObject.getId()" in Java.

If you prefer, "this" or "everything" can be left out.
In other words, unqualified property names or function calls always refer to the "this" or the "everything" object.
So, the following queries are equivalent to the first examples.

```
IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery("id == $0", id);
```
```
IQuery<IInstallableUnit> query = QueryUtil.createQuery("select(x | x.id == $0)", id);
```

The $0, $1, ..., $n variables simply refer to the Java parameter objects that were passed into the query.
The same dot operator rules apply to these variables as apply to the "everything" and "this" variables: If the variable is an Iterable, you have to call a function on it that processes its contents.
Otherwise, you can treat it like a Java Bean and the dot operator accesses its properties.

With that introduction, let's take a look at this material again, but expressed more formally.

#### Boolean Queries

Queries can be written as simple predicates such as "id == $0".
When executed, the predicate will be evaluated once for each row in the queried material.
This form is used for the IMatchQuery implementation.
The current value is available in a predefined variable named _this_.
Like in Java, _this_ is normally implicit so that:

```
id == $0
```

is actually the same as:

```
this.id == $0
```

#### Collection Queries

A query can also be declared as something that acts on "everything".
 This is particularly useful when things like search for latest or doing requirements traversal.
 These queries make use of a predefined variable named _everything_.
 When accessed, that variable returns an iterator over all queried material.
 As with _this_ in a predicate query, _everything_ is something that you don't normally need to specify, i.e.:

```
select(x | x.id == $0)
```

is the same as:

```
everything.select(x | x.id == $0)
```

A boolean query can always be written as a collection query.
These two queries are fully equivalent:

```
Boolean: id == $0
Collection:   select(x | x.id == $0)
```

#### Special Operators

*   **Matches operator '~='**  
    
    A large amount of queries involve versions, version ranges, and capability matching.
    So managing that is important.
    Another thing that is important is to be able to support filtering based on ID's.
    All of this is now also built in to the language through the matches operator **~=**.
    It can be though of as the 'satisfies' method on an installable unit or provided capability, as 'is included in' when used with a version and version range, as a plain matches when used with a string and a pattern, or as 'instanceof' when comparing with a class.
    
    The following things can be matched using the operator
    
    | LHS | RHS | Implemented as |
    | --- | --- | --- |
    | IInstallableUnit | IRequirement | lhs.satisfies(rhs) |
    | IInstallableUnit | Filter | rhs.matches(lhs.properties) |
    | IInstallableUnit | IUpdateDescriptor | rhs.isUpdateOf(lhs) |
    | Version | VersionRange | rhs.isIncluded(lhs) |
    | Map | Filter | rhs.match(lhs) |
    | Dictionary | Filter | rhs.match(lhs) |
    | String | SimplePattern | rhs.isMatch(lhs) |
    | String | VersionRange | rhs.isIncluded(version(lhs)) |
    | `<any>` | Class | rhs.isInstance(lhs) |
    | Class | Class | rhs.isAssignableFrom(lhs) |
    
*   **And operator '&&'**  
    This operator checks if the first operand evaluates to a boolean.
    If it is, the it is assumed that all other operands also are booleans and the full evaluation is _true_ if all its operands evaluate to _true_.
    If the first result was not a boolean, then it is assumed that it is a collection and that all other operands also evaluates collections.
    The operator will then function as a **intersect** operator and the result consists those elements that could be found in all collections.
*   **Or operator '||'**  
    This operator checks if the first operand evaluates to a boolean.
    If it is, the it is assumed that all other operands also are booleans and the full evaluation is _false_ if none of its operands evaluate to _true_.
    If the first result was not a boolean, then it is assumed that it is a collection and that all other operands also evaluates collections.
    The operator will then function as a **union** operator and the result is the unique sum of all elements from all collections.

#### Functions

A small number of functions are available to assist with some common problems.
Example: An IRequiredCapability.getFilter() returns a String.
I only want the capabilities that has a filter that match certain properties.
Consequently, I need an instance of an OSGi Filter.
Not a string.
I can do this using the _filter_ function.
Like this:
```
requirements.exists(rc | rc.filter == null || $1 ~= filter(rc.filter))
```

The currently available constructors are:

| name | arguments | creates |
| --- | --- | --- |
| filter | string | an instance of org.osgi.framework.Filter |
| version | string | a p2 Version |
| range | string | a p2 VersionRange |
| class | string | a Class |
| boolean | string | a boolean |
| set | comma separated list of expressions | an instance of java.util.HashSet |
| iquery | IQuery \[, variable \[, collector\]\] | The result of evaluating the query |

A note about the **iquery** constructor.
The constructor operates in one of two modes depending on its IQuery argument.

If the argument implements IMatchQuery, the the constructor will return the boolean result of invoking the isMatch(value) method on that query.
The value is picked from the variable and the default variable is 'item'.
It is considered an error to pass a third argument in combination with an IMatchQuery.

When the query does not implement the IMatchQuery interface, it is considered a context query and its method perform(iterator, collector) will be called.
The iterator is picked from the variable and the default variable is 'everything'.
The collector can be passed in as the third argument.
If no third argument is present, a new collector will be created.

#### Collection Functions

A number of functions was added to enable various manipulations of collections.
A common way of writing collection functions is:

elements.someFunction(element | <expression>)

Here are the functions that are available in the p2QL language:

*   **Select**  
    The **select** function will evaluate its expression, once for each element, and return a new collection containing all elements for which the evaluation result was _true_.
    Example:
    ```
    select(x | x.id == 'org.eclipse.equinox.p2.ql')
    ```
*   **Collect**  
    The **collect** collects the result of evaluating each element in a new collection.
    Example:
    ```
    collect(x | x.id)
    ```
    returns a collection consisting of the value of the id property of each element in the original collection.
    
*   **Exists**  
    The **exists** function will evaluate an expression for each element in the collection until an evaluation yields _true_.
    If that happens, the result of the whole evaluation yields _true_.
    If no such element is found, the whole evaluation yields _false_.
    Example:
    ```
    providedCapabilities.exists(p | p.namespace == 'osgi.bundle')
    ```
*   **All**  
    The **all** function will evaluate an expression for each element until an evaluation yields _false_.
    If that happens, the whole evaluation yields _false_.
    If no such element is found, the whole evaluation yields _true_.
    Example:
    ```
    $0.all(rc | this ~= rc)
    ```
    Assuming $0 is a list of required capabilities, this query asks for items that fulfill all requirements.
*   **First**  
    The **first** function will evaluate an expression for each element in the collection until an evaluation yields _true_.
    If that happens, the result of the whole evaluation yields that element.
    If no such element is found, the whole evaluation yields _null_.
    Example:
    ```
    providedCapabilities.first(p | p.namespace == 'java.package')
    ```
    Returns the first provided capability with namespace 'java.package'.
*   **Flatten**  
    Intended to be applied on collections of collections.
    Yields a single collection with all elements from the source collections, in the order they are evaluated.Example:
    ```
    collect(x | x.requirements).flatten()
    ```
    Yields a collection with all required capabilities from all iu's that the collect was applied on.
*   **Latest**  
    Some queries must make sure that the result only contains the latest version of each found IU.
    The special function _latest_ to makes that possible.
    The function will require that the collection elements implement the IVersionedId interface.
    Here is an example of how to use it:
    ```
    select(x | x.id == $0).latest()
    ```
    As a convenience, it is also possible to write:
    ```
    latest(x | x.id == $0)
    ```
*   **Limit**  
    It is sometimes desirable to limit the number of rows returned by a query.
    The function 'limit' can be used for that:
    ```
    select(...).limit(100)
    ```
*   **Unique**  
    This function will ensure that the resulting collection contains unique elements.
    The function can operate in two modes, with or without a cache argument.
    I.e.,
    ```
    x.unique()
    ```
    or
    ```
    x.unique(cache)
    ```
    The latter expects the argument to be a _java.util.Set_ that it can use to enforce the uniqueness of the element.
    This enables the result to be unique in a larger scope then the collection itself.
*   **Traverse**  
    A common scenario in p2 is that you want to start with a set of roots and then find all items that fulfill the root requirements.
    Those items in turn introduce new requirements so you want to find them too.
    The process continues until no more requirements can be satisfied.
    This type of query can be performed using the **traverse** function.
    The function will evaluate an expression, once for each element, collect elements for which the evaluation returned _true_, then then re-evaluate using the collected result as source of elements.
    No element is evaluated twice.
    This continues until no more elements are found:
    
    $0.traverse(parent | parent.requirements.collect(rc | select(iu | ~= rc)).flatten())
    
    This is of course a fairly naive slicing mechanism.
    See [#Currying](#Currying) below for more advanced examples.

#### Query Parameters

The query must be parameterised so that expression parsing can be done once and then executed multiple times.
A parameter is expressed as $_n_ where _n_ is either the parameter index, originating from 0.

### Basic IQuery Examples

Here are some examples of how to use the expressions with IQuery: Query for all IU's that has an id:

```
IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery("id == $0", id);
```

Query for the latest IU of some specific id:

```
IQuery<IInstallableUnit> query = QueryUtil.createQuery("latest(x | x.id == $0)", id);
```

Query an artifact repository for all keys with a specific classifier:

```
IQuery<IArtifactKey> query = QueryUtil.createMatchQuery(IArtifactKey.class, "classifier == $0", classifier);
```

Query for the latest IU that matches a specific version range.
Since the second parameter is a VersionRange, the ~= operator is interpreted as _isIncluded_:

```
IQuery<IInstallableUnit> query = QueryUtil.createQuery("latest(x | x.id == $0 && x.version ~= $1)", id, range);
```

Query for an IU that has a specific property set:

```
IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery("properties\[$0\] == $1", key, value);
```

The same query, but this time for multiple possible values:

```
IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery("$1.exists(v | properties\[$0\] == v)", key, new Object\[\] { v1, v2, v3 });
```

Query for all categories found in the repository:

```
IQuery<IInstallableUnit> query = QueryUtil.createMatchQueryx("properties\[$0\] == true", IInstallableUnit.PROP\_TYPE\_CATEGORY);
```

Query for all IU's that fulfil at least one of the requirements from another IU.
Since the first parameter is a list of IRequirements, the ~= applied to each each IU using _satisfies_.

```
IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery("$0.exists(rc | this ~= rc)", iu.getRequirements());
```

Query for the latest version of all patches:

```
IQuery<IInstallableUnit> query = QueryUtil.createQuery("latest(x | x ~= $0)", IInstallableUnitPatch.class);
```

Query for all IU's affected by a patch:

```
IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery("$0.exists(rcs | rcs.all(rc | this ~= rc))", patch.getApplicabilityScope());
```

### Localization

An installable unit may have locale specific properties.
Such properties may be stored in the IU itself or in fragments that provide localization for the IU in the 'org.eclipse.equinox.p2.localization' namespace.
p2QL will interpret the member _translations_ applied on an IU as a map of translated properties, hiding all the details
 As an example, this query:

```
translations\['license'\] ~= /\*kommersiellt bruk\*/
```

when used with a default Locale("sv_SE") would yield all IU's that has a license, translated into Swedish, that contains the exact phrase 'kommersiellt bruk'.

A special class named **KeyWithLocale** was added to allow queries for entries that does not match the current locale.
So the above query can be written as:

```
QueryUtil.createMatchQuery("this.translations\[$0\] ~= /\*kommersiellt bruk\*/", new KeyWithLocale("license", new Locale("sv", "SE")))
```

### Currying

Want some add some spice? Probably...

Consider the traversal function and the example we had above:

```
$0.traverse(parent | parent.requirements.collect(rc | select(iu | iu ~= rc)).flatten())
```

Now let us assume that we want to perform this traversal and filter the requirements based on platform.
Our first attempt could look something like this.

```
$0.traverse(parent | parent.requirements.select(rc | rc.filter == null || $1 ~= rc.filter).collect(rc | select(iu | iu ~= rc)).flatten())
```

This would however be very inefficient since many requirements exists in several IU's.
During the traversal there is no point traversing a requirement more then once so it would be good to "remember" the perused requirements.

The p2QL syntax permits something generally referred to as _currying_.
Currying means that you can provide more parameters then the single one that represents the current element to the expression of a collection function.
So far, we've seen examples using the syntax:

```
select(x | <do something with x>)
```

This is actually a short form for the longer:

```
select(_, {x | <do something with x>})
```

The select provides one parameter to each iteration.
It's value is always provided reachable using the special operator '_'.
In this case, the variable x maps to the parameter _ since they have the same positional offset.
I can add more parameters by declaring:

```
select(a, b, _, {x, y, z, <do something with x, y, z>})
```

Variable x will now have the value of a, y the value of b, and z the value of _.

So why is this important? Well, the initializers are just evaluated once for one call to select.
The expression however, is evaluated once for each new value of _.<p> <p>Let us now return to the traversal again.
Because with this syntax, we can actually specify a global set that we can pass to the unique function so that no requirement is perused twice:
```
$0.traverse(set(), _, { rqCache, parent | parent.requirements.unique(rqCache).select(rc | rc.filter == null || $1 ~= rc.filter).collect(rc | select(iu | iu ~= rc)).flatten()})
```

### Performance Comparison: p2QL Traversal Versus the p2 Slicer

A test was performed using the query example above on the Galileo repository, using a specific version of the org.eclipse.sdk.feature.group IU as the root.
The test produced a result with 411 IU's in < 12 ms average.
I compared this with another test that used the p2 Slicer.
The number of IU's produced was exactly the same.
The slicer however, took > 22 ms (it also uses a capability cache internally).
Not very scientific perhaps but repeated tests produce similar results.

I'm sure the Slicer can be optimized and achieve results that might be even slightly better then the traversal but I still think this proves a point.
p2QL Performance is very good at this point.

### Java API

The expression tree created by the parser must be well documented and easy to use so that queries can be created programmatically.
Since all expressions are immutable and without context, they can be combined freely.
Hence, code like this is fully possible:

```
// Create some expressions.
// Note the use of identifiers instead of  indexes for the parameters

IExpressionFactory factory = ExpressionUtil.getFactory();
IExpression item = factory.variable("this");
IExpression cmp1 = factory.equals(factory.member(item, "id"), factory.parameter(0));
IExpression cmp2 = factory.equals(
 factory.at(factory.member(item, "properties"), factory.parameter(1)),
 factory.parameter(2));

IExpression everything = factory.variable("everything");
IExpression lambda = factory.lambda(item, factory.and(new IExpression\[\] {cmp1, cmp2}));
IExpression latest = factory.latest(factory.select(everything, lambda));

// Create the query
IQuery<IInstallableUnit> query = QueryUtil.createQuery<IInstallableUnit>(latest "test.bundle", "org.eclipse.equinox.p2.type.group", Boolean.TRUE);
```

### The [BNF](https://en.wikipedia.org/wiki/Backus%E2%80%93Naur_Form)

```
condition
	: orExpression ( '?' orExpression ':' orExpression )?
	;

orExpression : andExpression ( '||' andExpression )* ;

andExpression : binaryExpression ( '&&' binaryExpression )* ;

binaryExpression : notExpression ( op notExpression )?;

op : '==' | '!=' | '>' | '>=' | '<' | '<=' | '~=' ;

notExpression
	: '!' notExpression
	| collectionExpression
	;

collectionExpression
	: memberExpression ( '.' collectionFunction )*
	;

memberExpression : function ( ( '.' ID ) | ( '\[' memberExpression '\]' ) )* ;

function
	: ( filter | version | range | class) '(' condition ')'
	| set '(' ( condition ( ',' condition )* )? ')'
	| unaryExpression
	;

collectionFunction
	: ( select | reject | collect | exists | all | traverse | first ) '(' lambdaDefinition ')'
	| limit '(' memberExpression ')'
	| unique '(' memberExpression? ')'
	| latest '(' lambdaDefinition? ')'
	| flatten '(' lambdaDefinition? ')'
	;

lambdaDefinition
	: initializer ( ',' initializer )* ( ',' '{' lambda '}' )?
	| '{' lambda '}'
	| lambda
	;

initializer
	: '_'
	| condition
	;

lambda
	: ( ID ( ',' ID )* )? '|' condition
	;

unaryExpression
	: '(' condition ')'
	| '\[' condition ( ',' condition )* '\]' // #array construct
	| STRING
	| INT
	| parameter
	| 'null'
	| 'true'
	| 'false'
	| ID
	;

parameter
	: '$' INT | ID
	;
```
