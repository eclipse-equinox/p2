This page collects information on working with and running the automated
and manual tests for [p2](p2 "wikilink"). The p2 team regularly captures
test coverage data for its automated tests. A summary of coverage for
past milestones is available at
[Equinox/p2/Test_Coverage](Equinox/p2/Test_Coverage "wikilink")

# Automated tests

To run the automated tests, start by checking out
org.eclipse.equinox.p2.releng from /cvsroot/rt/org.eclipse.equinox/p2.
Then import one of the project sets in this project depending on whether
you want to use the extssh (for committers) or pserver (for others)
connection method. Select the appropriate project set file and click
"Import Project Set" from the context menu.

The p2 tests all require Java 5 or later to run. The tests are all
regularly run on Windows, Linux, and Mac, but should run on other
platforms as well. Please enter bug reports for test failures on any
os/ws/arch combination supported by Equinox.

Structurally all but the UI tests fall under the single AutomatedTests
suite in org.eclipse.equinox.p2.tests. The UI tests have their own
separate AutomatedTests entry point because they use the GUI test runner
as opposed to the headless test runner. This means there are two suites
to run to exercise all the tests. Note that the UI tests also exercise
core code so it is valuable to run both suites even when changes are
made in core areas.

With the exception of the End2EndTest, all of these tests are quite fast
to run, so we should all be in the habit of running these tests before
releasing any code changes. If you're on a slow connection you can
either comment out the End2EndTest, or kill the test run when the last
core test is started (this test always runs last).

Most tests subclass the common base class `AbstractProvisioningTest`.
This class has a large number of utility methods for simplifying
interaction with p2, and for p2-specific assertions. Here is a very
simple test that illustrates some of the convenience methods on
`AbstractProvisioningTest`:

``` java
IInstallableUnit toInstall= createIU("TestIU");
IInstallableUnit[] toInstallArray = new IInstallableUnit[] {toInstall};
createTestMetdataRepository(toInstallArray);
IDirector director = createDirector();
IProfile profile= createProfile("TestProfile", null, null);
ProfileChangeRequest request = new ProfileChangeRequest(profile);
request.addInstallableUnits(toInstallArray);
director.provision(request, null, getMonitor());
assertProfileContains("1.1", profile, toInstallArray);
```

Note that these convenience methods also help with test cleanup. For
example, any profile created with the `createProfile()` method will be
automatically removed in the test's tearDown method.

## Framework admin tests

Due to the fact that frameworkadmin has no knowledge of or dependency on
p2, its tests are stored in a separate project:
org.eclipse.equinox.frameworkadmin.test. However, its main test suite is
linked into the AutomatedTests class in org.eclipse.equinox.p2.tests, so
no extra step is needed to run these tests.

## Installer tests

## Repository tests

We have a fairly good collection of \*real\* metadata repositories,
artifact repositories, and legacy update site repositories in
org.eclipse.equinox.p2.tests/testData{artifactRepo,metadataRepo,updatesite}.
This makes it fairly easy to write tests involving various kinds of
well-formed and invalid repositories. Feel free to add further
reasonably sized repositories here for testing other code paths, failure
cases, regression tests, etc. Typically we try to write tests against
local repositories to ensure good test performance, but there are a
small number of tests that access repositories on <http://eclipse.org>
for testing issues that only arise on remote repositories.

## Dropins reconciler tests

To run the reconciler tests, there is a one-time setup required because
it requires copies of platform runtime binaries. You need to do the
following:

1.  Download the 3.5 version of the Eclipse project platform runtime
    binary zip for your os/ws/arch.
2.  Download the latest good build of the Eclipse project platform
    runtime binary zip for your os/ws/arch.
3.  Use a system property to specify the location of this zip in the
    test launch configuration
    (-Dorg.eclipse.equinox.p2.reconciler.tests.platform.archive=<some_path>eclipse-platform-<someversion>-win32.zip).
4.  Use a system property to specify the location of 3.5 zip in the test
    launch configuration
    (-Dorg.eclipse.equinox.p2.reconciler.tests.35.platform.archive=<some_path>eclipse-platform-3.5-win32.zip).
5.  Export the org.eclipse.equinox.p2.tests.verifier bundle into your
    running IDE and close the project from the workspace.

The "all p2 tests" launch configuration in the p2.tests bundle has these
properties set, but you'll likely need to tweak the path of the zip for
your machine.

**Note:** Running the reconciler tests in your workspace means that you
are testing the code in the archive you unzipped, \*not\* the code in
your workspace. In order to truly test the code in your workspace you
need to create JARs and replace them in the Zip file. This is a
temporary solution until we figure out what else to do... it is awkward
for testing locally but it works when running the automated test suites
as part of the build process.

## Publisher tests

The publishers test make use of the
[EasyMock](http://www.easymock.org/EasyMock2_4_Documentation.html)
testing framework. You'll need to know a bit about EasyMock to
understand and work with these tests.

# Testserver

There is a http testserver bundle called
org.eclipse.equinox.p2.testserver. It has servlets capable of
introducing all sorts of errors in the communication, as well as
providing a secure context with Basic authentication that can be used to
proxy real content.

### Getting and running

Checkout the org.eclipse.equinox.p2.testserver project from CVS, and use
the launch configuration in the project to start the server. The launch
configuration specifies port 8080. You can then visit
<http://localhost:8080/public/index.html> for an overview of the
available services. (Look at the index.html for the latest information).

You will also need: org.eclipse.equinox.http bundle. If you use thep2
team project sets in the p2 releng project you will get everything you
need.

A simple thing to try out is to use the "never" service to test
authentication dialogs. Simply enter <http://localhost:8080/never> as a
repository URL in Install New Software dialog.

### Automated Tests with Testserver

Currently there is one testsuite that uses testserver. Take a look at
o.e.e.p2.tests.metadata.repository.AllServerTests, it runns a test in
the same package called AuthTest.

### Services

The following services are currently available:

  - mounting testdata from bundle with and without authentication
  - mounting eclipse updates 3.4 as a proxy with and without
    authentication
  - truncator that truncates all files
  - molestor that turns files into garbage
  - decelerator that chops up communication in small packages and adds
    delay
  - timeout that acts like a "black hole"
  - status that returns the status code specified in the URL
  - redirector that nests redirects until a final redirect

### The index.html content from the bundle

You can access all files under the "webfiles" folder in this bundle via
either a "public" or "private" pseudo-root. To access using a login,
password, you simply use the pseudo-root "/private", and you will be
asked to log in. The credentials are: user: <b>Aladdin</b>, password:
<b>open sesame</b>.

The following real content is registered:

  - /proxy/private/ - goes to
    <http://http://download.eclipse.org/eclipse/updates/3.4>, but
    requires authentication.
  - /proxy/public/ - goes to
    <http://http://download.eclipse.org/eclipse/updates/3.4>, (useful in
    redirects).
  - /proxy/flipFlop/ - goes to
    <http://http://download.eclipse.org/eclipse/updates/3.4>, but fails
    authentication every second attempt.
  - /proxy/truncated - goes to updates/3.4, but truncates all files
  - /proxy/molested - goes to updates/3.4, but generates gibberish for
    all files
  - /proxy/decelerate - goes to updates/3.4, but delivers files in very
    small delayed packets - delay increases.
  - /proxy/decelerate2 - same as /proxy/decelerate, but delay kicks in
    when 80% of a file has been delivered
  - /proxy/modified/... - goes to updates/3.4, but delivers various
    errors in "last modified" (see below)
      - .../zero - all times are returned as 0
      - .../old - all times are very old
      - .../now - all times are the same as the request time
      - .../future - all times are in the future (which is illegal in
        HTTP)
      - .../bad - the time is not a date at all - the client should
        throw an error
  - /proxy/length/... - goes to updates/3.4, but delivers various
    content length errors (see below)
      - .../zero - length is reported as 0 (but all content written to
        stream)
      - .../less - less than the correct size is reported (all content
        written)
      - .../more - double the correct size is reported (but only
        available content is written)

The content listed (further) below is also available and can be accessed
under:

  - /public
  - /private - requires login
  - /never - impossible to login
  - /flipFlop
  - /truncated
  - /molested

Content

  - .../ar/simple - a simple artifact repo with a feature and a plugin,
    uses artifact.xml
  - .../mdr/composite - a composite meta data repository - consisting of
    two mdrs
  - .../mdr/composite/one - a regular meta data repostory, uses
    content.xml
  - .../mdr/composite/two - a regular meta data repostory, uses
    content.xml
  - .../updatesite - a classic update site with site.xml, features and
    plugins subdirectory

The following URLs are also available

  - /timeout/ - a black hole - sleeps an our and produces no response.
  - /status/nnn/xxxxx - returns the status-code "nnn", and text/html
    content, The xxxxx part is ignored. e.g.
    <http://localhost:8080/status/500/ignored/part>
  - /redirect/nnn\[/location\] - redirects nnn times and then optionally
    redirects to the 'location' - example
    <http://localhost:8080/redirect/3/public/index.html>

# Manual tests

The p2 team maintains a set of manual smoke test scenarios for
exercising functionality difficult to capture in automated tests. See
the [manual test scripts](Equinox/p2/UI_Manual_Tests "wikilink") for
more details.

[Testing](Category:Equinox_p2 "wikilink")