<h2>

Rough Notes

</h2>

<h3>

Build specific suggestions

</h3>

  - Product based builds with p2
  - Using repository tools such as the slicer, repo2runnable and mirror
    to build smaller components from existing repositories
  - process.p2.repo
  - comparator
  - remove.iu
  - Building composite repositories
  - Repository management
  - Using the publisher
  - Assembling product out of pre-existing components (no compiling)
  - Overview of pde build process to create metadata for product builds
      - setting start levels
      - specifying launchers
      - configuration properties

<h3>

P2 focus

</h3>

  - p2 for RCP developers (everything you need to know to make use of p2
    in RCP -- including at build + packaging time, at run-time, to
    provide self updating apps, etc...).
  - self updating - headless and non-headless. See also [the RCP update
    long
    talk](https://www.eclipsecon.org/submissions/2010/view_talk.php?id=1205)
    . I am thinking something along the lines of [Equinox/p2/Adding
    Self-Update to an RCP
    Application](Equinox/p2/Adding_Self-Update_to_an_RCP_Application "wikilink")
    (which will be updated before EclipseCon to cover the current state
    of the world). It would be really helpful to include adding
    self-update support as part of a sequence:
      - build your first app and include self-update support
      - add a new feature to your app, rebuild it, put it in the repo
        and show the self-update working
  - Something similar for OSGi developers. (Obviously things are very
    similar, but we would have to pick an example and focus on it during
    the tutorial).


Grumpy has left a new comment on your post "p2 tutorial RFC":

I'd really like to see someone explore using P2 for mass (as in
pushing/pulling bundles to around 180 global branches) deployment
scenarios. It seems to me there would be lots of tooling above and
beyond P2 to handle. It would be nice to have someone explore various
paths. Another challenging area is the intersection of bundle
distribution and role based access (governing who can get what bundles).

kmoir\> This seems similar to what [Sonatype's Nexus
talk](https://www.eclipsecon.org/submissions/2010/view_talk.php?id=1505)
is discussing.




<h3>

Notes from Ian and Kim's Friday Jan 15 discussion regarding the
tutorial.

</h3>

Discovering p2, building an add-on explorer for your application.  

This would cover:

1\. Building and Assembling different configurations of your application

2\. Building and publishing add-ons (or plug-ins or bundles, or whatever
term your prefer)

3\. Using the p2 UI to discover the add-ons


Detailed information

1\. Building and Assembling different configurations of your
application: Here we would show how you can build a master feature to a
p2 repository. You then build multiple "products". Instead of using CVS
for the products source, you use the master repo (already built). Doing
this you can build an SDK, and Standard product (or whatever we want).

2\. Building publishing add-ons Here we would show you use your SDK
(that you built in \#1) as a target, and you write a cool add-on bundle.
You then build the add on and publish it to a repo.

3\. Using the p2 UI to discover the add-ons Here we demonstrate the new
Mylyn UI (that is coming to p2) for discovering stuff.


  - 30 minute introduction to p2 - overview of terminology, structure of
    a repo etc.
      - Kim will ask PC if other p2 talks can be scheduled before our
        tutorial so people have a better background. (Susan's update and
        extensions, Pascal's p2 api)
  - Ian will send Kim the latest Hyperbola code to take a look the
    examples

15 minutes for each exercise

1.  First exercise - exporting from IDE and creating a repo. Describe p2
    properties that you can set in your build properties - for example
    for generating, compressing, and naming repos
2.  Second exercise - create a builder so the the build is reproducible.
    Provide product source.  Create build by running the antRunner.
3.  Provision SDK as a target. -Add new bundles - for install something
    to add a toolbar item -Export or build repository -install new add
    on into Hyperbola
4.  Mylyn UI - simplified assumptions - Ian will ping Susan to see when
    it will be integrated into the build


Other content to discuss

  - p2 specific properties in build.properties
  - other useful entries in build.properties
  - Importance of build reproducibility
  - Why we need to have install location with same content as repo. 
    Perhaps mention comparator.
  - Last year there were \~80 people in the the tutorial.  It would be
    great if there could be other people to help out.  Perhaps, Pascal,
    Simon, Jeff, Chris A or Susan would be able to help.


Other issues

  - download content from Yoxos - not sure if Eclipsecon will have
    sufficient bandwidth. Maybe will have a mirror like other
    EclipseCons, perhaps we could piggyback on that. Kim will followup
    with webmasters - yes there will be mirrors.

<!-- end list -->

  - Talked to Pascal regarding his talk - he doesn't think there will be
    time for much background in his talk. He actually suggested that our
    tutorial be a precursor to the API talk.
  - Other topics he suggested
      - lifecycle of repository, best practices for repository
        management, set expectations of users wrt stability of repo
      - dynamically generated repos - for example Eclipse Marketplace.
        XML stream for p2 is generated automatically.
      - authentication to repo - can use underlying mechanism of Apache
        httpd
      - determine what is being covered in this talk to avoid overlap

<h2>

original abstract

</h2>

At first glance, the introduction of p2 into a releng build seems to
make things more complicated. However, for more advanced products, p2
can actually help simplify the build. This tutorial will outline
practices for building, reusing, and managing p2 repositories. We will
show how products can be easily composed from sub-components and we will
look at the Eclipse SDK releng process as an example of how the build
system can be simplified with the use of p2.

  - Product based builds with p2
  - Using repository tools such as the slicer, repo2runnable and mirror
    to build smaller components from existing repositories
  - Building composite repositories
  - Repository management
  - Using the publisher and the director
  - Assembling products out of pre-existing components (no compiling)
  - Overview of the pde build process to create metadata for product
    builds

<h2>

Draft updated abstract

</h2>

Exploring p2

At first glance, the introduction of p2 into your environment can seem
to make things more complicated. However, p2 can actually make the cycle
between development, integration and customer testing more agile. This
tutorial will cover the fundamental concepts behind p2 with the majority
of the time allocated to hands on exercises on how to enable p2 in RCP
applications.

Overview of p2

  - p2 architecture and terminology
  - Anatomy of a repository
  - Using the director and publisher
  - Product based builds with p2
  - Repository management best practices
  - Assembling products out of pre-existing components (no compiling)


Hands on exercises

  - Building and assembling different configurations of your application
  - Building and publishing add-on bundles
  - Export or build your product to a repository, and then install that
    product from the repository you just created.
  - Using repository tools such as the slicer, repo2runnable and mirror
    to build smaller components from existing repositories
  - Using p2 to discover add-ons using the new Mylyn UI (if available in
    3.6M6)




[Equinox p2](Category:Equinox_p2 "wikilink")