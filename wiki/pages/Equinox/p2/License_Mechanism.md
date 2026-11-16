This working document describes the problems with and intended solutions
for the existing licensing mechanism.



# Problem Description

The two problems with the current license mechanism are:

1.  Authoring
      - Each time a license changes, two changes must be made in every
        feature. A feature.xml change and a license.html resource
        change.
      - The formats of the two files are different, preventing a simple
        copy/paste exercise.
      - There may be several files in a feature associated with
        licensing.  For example, our features seem to contain a
        license.html file and an epl-v10.html file
2.  Repetition of license text in the meta data repository
      - Each feature in the meta data repository has an inlined copy of
        the license text which '''may '''be causing performance issues. 
        (Each feature actually has license text inlined twice in the
        meta data. Once in the feature.group IU and once in the
        feature.jar IU. This may just be a bug that needs to be fixed)



# Solution Proposal

It is suggested that these two issues be decoupled and solved
independantly.

## Authoring Solution

The authoring problem can be addressed by PDE build at build time.

1.  All license.html files are removed from the feature definitions
    under source code control.
2.  Features are changed such that they optionaly point at a license URI
    instead of specifying their own license text.
3.  PDE build is changed so that at build time the license is retrieved
    from the location referenced by the license URI
4.  The retrieved license information is used to generate the
    license.html file and injected into the feature.xml file.
5.  The feature build process and P2 repository generation process
    proceed as normal.

**Issue**
It was suggested that the license URI would point to a server that would
''download ''the license information at build time.  It would be the
users responsibility to manage what information was served by the server
at build time.
I feel pretty strongly that the license needs to be under version
control so that builds are reliable and reproducable.  Arguably
specifing an arbitrary URI does not prevent the user from doing this but
I believe it provides too much leeway and too little guidance resulting
in a brittle (server down) and not reproducable (no control over license
content from build to build) build process.
I propose that the license information be contained in a License
Project. The License Project would be created in Eclipse and placed
under version control. This will allow current feature tooling and
future P2 tooling to present the user with licenses to choose from when
defining the license for a feature or IU.
The feature or IU will not reference a particular version of the License
Project. Instead, the correct version of the license will be checked out
of version control at build time using the customer's regular mechanism
for specifiying check out tags. This will allow the user to change the
vesion of the a license with a single build time change while preserving
the integrity and reproducability of builds.
**Question:** Are there any Eclipse.org rules about the license
information for features needing to reside in the repository in the same
location as the feature itself? That is, if a user does a CVS check-out
on a feature does the feature license have to be checked out at the same
time/available at the same location?
**Answer:** I got a couple of names at Eclipse.org from Duong and have
sent email asking the question

### **License Artifacts**

The license feature may contain one or more artificats.  These include
files such as:

  - license.html
  - epl-v10.html
  - ...

These files will exist in the repository as part of the new "License
Feature".  These files must be included in the License Features
build.properties file just as any binary file that requires inclusion
must be.  These files will **not** be referenced explicitly in the
build.properties of the Payload Feature.  Instead, at build time the
Payload Feature will include all files in its own build.properties
**plus** all files from its referenced License Feature's
build.properties file EXCEPT for:

  - feature.properties
  - feature.xml

### **Root File Build Properties**

A feature may include root entries in the build.properties file that
allows files to be installed in the products root directory.  Since
these files may be license files a mechanism for dealing with this is
required.

  - At author time the files will exist in the License Feature
  - The files will be in the bin.includes of the License Feature's build
    properties
  - The files will **not** be root entries in the License Feature.  The
    "rootness" of a file is a property of a feature or product, not a
    feature of a license.
  - The files **will** be root entires in the Payload Feature
  - As above, the files will **not** be in the Payload Feature's
    bin.includes

At build time the implementation will need a hint to locate a root entry
that exists in a referenced license feature.  This hint will be provided
by an extension to the Root File syntax.  We will support the new
keywoard license.  Examples are:

  - root=license:<file:license.html>
  - root.folder.foo= license:bar


## Repetition in Meta data repository

Since the above proposed authouring solution does not change the meta
data generation, the license text will be duplicated in line for each
feature in the repository.
The first order of business should be investigation to determine if the
repeated licenses is actually a significant problem requiring a fix.
There are several pieces of technology in place that may be mitigating
the issue. It could be that the repeated license while appearing
problematic does not, in fact, pose a serious performance problem.

These existing mitigating technologies are:

1.  Zipped meta data.
      - On disk, and in transmission the meta data is zipped. Presumably
        repeated identical licenses within a single file compress very
        will.

<!-- end list -->

1.  String Tables
      - In memory, P2 uses string tables. Mutliple identical licenses
        should not grow memory foot print beyond that needed to store
        string table references.

That being said, there are still potential runtime performance issues:

1.  Reading and unzipping the content.jar file.
2.  Performance of string table with very large strings. Since each
    license is a relativly large string resolving a reference in the
    string table requires the execution of the degenerate path of
    String.equals(). With enough copies of a license in the string table
    there may be noticable perforamnce problems resolving each refrence.

The true performance impact of the repeated license should be
determined.  It could be the case that although the repeated license
text appears to have significant footprint and performance issues, the
reality may be otherwise.
**Question:** What is our approach going to be to see if these are
really issues? Performance tests? Profiling? etc.

[License Mechanism](Category:Equinox_p2 "wikilink")