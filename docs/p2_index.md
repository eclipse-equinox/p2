# Equinox p2 `p2.index`


## What is the `p2.index` file?

The `p2.index` file provides a description of the kind of repository available at a particular location; simple or composite.

The presence of this file in a repository will cause p2 to replace its default lookup order for repository to match the one advised in the file.
This only happens the first time a repository is accessed, as p2 then caches "what worked before" and uses that on subsequent accesses.
See [bug 347448](https://bugs.eclipse.org/bugs/show_bug.cgi?id=347448) for more detail, links, and history behind this advisory file.
The latest version of the [b3 aggregator](/Eclipse_b3/aggregator/manual "Eclipse b3/aggregator/manual") adds an appropriate p2.index file as it publishes its p2 repository (not sure about other p2 repository publishers).

For example the example `p2.index` contents below will cause p2 to look for compositeContent.jar, and if not found, then compositeContent.xml and would skip looking for content.jar or content.xml as it normally would look for them first.
(And, then, naturally, if it needed artifacts from that site, would look for compositeArtifacts.jar and if not found compositeArtifacts.xml and would skip looking for artifacts.jar and artifacts.xml).

 version = 1
 metadata.repository.factory.order = compositeContent.xml,\\!
 artifact.repository.factory.order = compositeArtifacts.xml,\\!

### How does it help me?

As a provider of a repository, it does not help you directly, but it helps your users to get a better experience and also slightly reduce the number of hits your server will be subject to.

### What happens if I get it wrong ?

Should you get the content of this file wrong, the repository will fail loading.
For example specifying compositeContent.xml where the repository is a content.xml will cause p2 to only look for compositeContent.xml and never look for the content.xml.

### How many files for composite repositories?

Given that a composite repository is just a repository that refers to other repositories, the full benefit of `p2.index` can only be achieved if every child repo has the file (with only a few exceptions to this general rule) but the benefit is greatest, or most important, for composite sites as that is where the "default rules" are changed the most.

### Why say 'xml' instead of 'jar' in factory name?

The content of `p2.index` does not reflect whether the files are zipped or not.

To quote the output of the b3 aggregator:

Please note that the values in this file denotes repository factories and not files.
The factory `<name>.xml` will look for both the `<name>.jar`

and the `<name>.xml` file, in that order

For example for a simple repository that contains artifacts.jar and content.jar, the `p2.index` would still name: "artifacts.xml" and "content.xml" as the factories.

As an aside, jarred versions of p2 content and artifacts files should always be provided for production sites, when the files are of any substantial size.

### Examples

Note, your `p2.index` file should contain the minimum data possible (that is, no comments) since even though it is small, it currently is requested hundreds of thousands of times on enterprise systems and currently is not cached (See [bug 310546](https://bugs.eclipse.org/bugs/show_bug.cgi?id=310546)).

### Example for composite repository

Use this content in your `p2.index` file, if your repository location contains compositeContent.jar/xml and compositeArtifacts.jar/xml.
```
version = 1
metadata.repository.factory.order = compositeContent.xml,\\!
artifact.repository.factory.order = compositeArtifacts.xml,\\!
```

### Example for simple repository

Use this content in your `p2.index` file, if your repository location contains content.jar/xml and artifacts.jar/xml.
```
version = 1
metadata.repository.factory.order = content.xml,\\!
artifact.repository.factory.order = artifacts.xml,\\!
```

### New in Mars Release: XZ Example for simple repository

In the Mars, June 2015 release (Eclipse 4.5), p2 was modified to be able to read metadata files that have been compressed using XZ compression, not only the "zip" (or, jar) compression.
(See [bug 464614](https://bugs.eclipse.org/bugs/show_bug.cgi?id=464614)).
This applies only to "simple repositories", that is, the "content.xml" and "artifacts.xml" files.

For the foreseeable future, the expectation is that sites which use the XZ compression must also use the traditional zip (jar) compression, so that older versions of Eclipse (or, p2) will be able to read them, even though the older versions do not yet have the XZ support.
In order to flag a site as having both the XZ compressed format, and the zip (jar) compressed format, the following `p2.index` file would be used.
```
version=1
metadata.repository.factory.order= content.xml.xz,content.xml,!
artifact.repository.factory.order= artifacts.xml.xz,artifacts.xml,!
```

Note: as of this writing, neither p2 (see [bug 467779](https://bugs.eclipse.org/bugs/show_bug.cgi?id=467779)) nor any other known publisher actually produces the XZ compressed files.
But, they are easy to produce "after the fact", since XZ is widely available on all Operating System.
The recommendation, based on informal observation, is that the xml version of the metadata files be compressed with the '-e' option of XZ.
For example,
```
xz -e content.xml    (converts the xml file to one named content.xml.xz)
xz -e artifacts.xml  (converts the xml file to one named artifacts.xml.zx)
```

**Remember, if you add the [`p2.mirrors.url`](/Equinox/p2/p2.mirrorsURL "Equinox/p2/p2.mirrorsURL") property to your artifacts.jar/xml file at some point after the XZ compressed version has first been created, do not forget to re-create your XZ compressed version, so that it too has the correct value.**  


### Example for a mixed repository

Use this content in your `p2.index` file, if your repository location contains a content.jar but a compositeArtifacts.jar.

This case should be rare, but does happen to be used in some of early Simultaneous Release repositories.
(Indigo, the 2012 release, was the last Simultaneous Release repository that used it).
```
version = 1
metadata.repository.factory.order=content.xml,\\!
artifact.repository.factory.order=compositeArtifacts.xml,\\!
```

