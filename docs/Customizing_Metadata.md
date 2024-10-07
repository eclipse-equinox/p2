Equinox/p2/Customizing Metadata
===============================

Contents
--------

*   [1 Customizing Installable Unit Metadata](#Customizing-Installable-Unit-Metadata)
    *   [1.1 Advice file format](#Advice-file-format)
    *   [1.2 Capability Advice:](#Capability-Advice:)
    *   [1.3 Update descriptor advice:](#Update-descriptor-advice:)
    *   [1.4 Property Advice:](#Property-Advice:)
    *   [1.5 Touchpoint Instruction Advice:](#Touchpoint-Instruction-Advice:)
        *   [1.5.1 Variable Substitutions](#Variable-Substitutions)
    *   [1.6 Additional Installable Unit Advice:](#Additional-Installable-Unit-Advice:)
*   [2 Category Generation Using p2.inf](#Category-Generation-Using-p2.inf)
    *   [2.1 Categorizing plug-ins](#Categorizing-plug-ins)
    *   [2.2 Nested categories](#Nested-categories)

Customizing Installable Unit Metadata
=====================================

Advice file format
------------------

An Installable Unit can be augmented at generation time by writing a p2 advice file (p2.inf). The format of this file is java properties file containing key=value pairs. In Eclipse 3.5, touchpoint advice files can be placed:

*   In bundles (META-INF/p2.inf): The instructions are added to the installable unit for the bundle
*   In features (a p2.inf file co-located with the feature.xml): The instructions are added to the installable unit for the feature group
*   In products (a p2.inf file co-located with the .product file): The instructions are added to the root installable unit for that product.

Two special value parameters are supported:

*   $version$ - returns the string form of the containing IU's version
*   $qualifier$ - returns just the string form of the qualifier of the containing IU's version

For the 3.5 release the p2.inf file can be used to augment Installable Units: capabilities, properties, and instructions. In addition support is provided for defining additional installable units that are in some way related to the container IU.

  
Properties in this file often contain <#> index segments. These index segments serve to separate groups of similar properties from each other. The actual value of these indexes are not important, only that a given set of properties which all refer to the same logical item all use the same index. The following example is talking about two seperate properties, each uses a different index to distinguish it from the other.


```
 properties.0.name = testName1
 properties.0.value = testValue1
 properties.1.name = testName2
 properties.1.value = testValue2
```

Capability Advice:
------------------

There are three different type of capability advice:

*   "provides" - these are capabilities that an IU will offer to satisfy the needs of other IUs
*   "requires" - these are the capabilities that an IU requires from other IUs in order to resolve correctly
*   "metaRequirements" - these are capabilities that the IU puts on the profile that must already be installed before this IU can be installed.

Capability advice will "replace" an existing capability of the same type on the IU if the name/namespace match.

```
 provides.<#>.namespace = <namespace>
 provides.<#>.name = <name>
 provides.<#>.version = <version> _(optional / default: 1.0.0)_

 requires.<#>.namespace = <namespace>
 requires.<#>.name = <name>
 requires.<#>.range = <range> _(optional / default: 0.0.0)\]_
 requires.<#>.matchExp = <p2QL expression> (note that in this case the namespace, name and range attributes are not 
 requires.<#>.greedy = <true|false> _(optional / default: true)_
 requires.<#>.optional = <true|false> _(optional / default: false)_
 requires.<#>.multiple = <true|false>  _(optional / default: false)_
 requires.<#>.filter = <ldap filter> _(optional)_
```

Negative requirements can be published by setting min and max values on the requirement to 0. For example

```
 requires.<#>.namespace = org.eclipse.equinox.p2.iu
 requires.<#>.name = some.feature.feature.group
 requires.<#>.min = 0
 requires.<#>.max = 0

 metaRequirements.<#>.namespace = <namespace>
 metaRequirements.<#>.name = <name>
 metaRequirements.<#>.range = <range> _(optional / default: 0.0.0)_
 metaRequirements.<#>.matchExp = {p2QL expression} (note that in this case the namespace, name and range attributes are not used)
 metaRequirements.<#>.greedy = <true|false> _(optional / default: true)_
 metaRequirements.<#>.optional = <true|false> _(optional / default: false)_
 metaRequirements.<#>.multiple = <true|false>  _(optional / default: false)_
```

Where <#> is an index for the property, <namespace>, and <name> are the associated named strings, <version> and <range> are version and version range strings respectively.

For example:

```
 provides.0.namespace = testNamespace1
 provides.0.name = testName1
 provides.0.version = 1.2.3.$qualifier$
 provides.1.namespace = testNamespace2
 provides.1.name = testName2
 provides.1.version = $version$

 requires.0.namespace = testNamespace1
 requires.0.name = testName1
 requires.0.range = \[1.2.3.$qualifier$, 2)
 requires.0.greedy = true
 requires.0.optional = true
 requires.0.multiple = true
 requires.1.namespace = testNamespace2
 requires.1.name = testName2
 requires.1.range = \[$version$, $version$\]
 requires.1.greedy = false

 metaRequirements.0.namespace = testNamespace1
 metaRequirements.0.name = testName1
 metaRequirements.0.range = \[1.2.3, 2)
 metaRequirements.0.greedy = true
 metaRequirements.0.optional = true
 metaRequirements.0.multiple = true
 metaRequirements.1.namespace = testNamespace2
 metaRequirements.1.name = testName2
 metaRequirements.1.range = $version$
 metaRequirements.1.greedy = false
```



Update descriptor advice:
-------------------------

The update descriptor advice allows to override the default update descriptor generated by p2. Typically this is useful if an IU has been renamed and automatic update detection is still desired.

```
 update.id = <id of IU>
 update.range = <range of the IU being updated>
 update.matchExp = {a match expression identifying the IU being updated}. (When this is specified the values of id and range are ignored) 
 update.severity = <0|1>
```

To allow updating a renamed artifact, you must provide a matchExp that matches both the old and new artifact names. For example, when renaming \*old\* feature to \*new\*:

```
 update.matchExp = providedCapabilities.exists(pc | \
   pc.namespace == 'org.eclipse.equinox.p2.iu' && \
     (pc.name == 'old.feature.group' || \
       (pc.name == 'new.feature.group' && pc.version ~= range('\[0.0.0,$version$)'))))
```

These match expressions are written in the [p2 Query Language p2ql](/Equinox/p2/Query_Language_for_p2 "Equinox/p2/Query Language for p2").

Property Advice:
----------------

```
 properties.<#>.name = <propertyName>
 properties.<#>.value = <propertyValue>
```

Where <#> is an index for the property, <propertyName>, and <propertyValue> hold the name and value strings for the property.

For example:

```
 properties.0.name = testName1
 properties.0.value = testValue1
 properties.1.name = testName2
 properties.1.value = testValue2
```


Touchpoint Instruction Advice:
------------------------------

```
 instructions.<phase> = <raw actions>
 instructions.<phase>.import = <qualified action name> \[,<qualified action name>\]* _(optional)_
```


Where <phase> is a p2 phases (collect, configure, install, uninstall, unconfigure, etc).

[Note:](/index.php?title=Note:&action=edit&redlink=1 "Note: (page does not exist)")

*   The <raw actions> will be "appended" to the end of any instructions already being generated.
*   The qualified action names for the IU's touchpoint type are implicitly imported. All other actions need to be imported.

For example:

```
 instructions.install = \
    ln(targetDir:@artifact,linkTarget:foo/lib.1.so,linkName:lib.so);\
    chmod(targetDir:${artifact.location},targetFile:lib/lib.so,permissions:755);
 instructions.install.import= \
    org.eclipse.equinox.p2.touchpoint.natives.ln,\
    org.eclipse.equinox.p2.touchpoint.natives.chmod
```

### Variable Substitutions

The parameters passed to the touchpoints may contain variables whose values are substituted. The following variables are supported. This is the complete list of supported variables as of Oxygen.

*   ${installFolder} the root folder to which the bundle is being installed. This is the folder that contains the 'plugins' and 'features' folders.
*   ${forced} usually "false". This may be "true" for the unconfigure and uninstall phases to ensure exceptions don't prevent the action and is probably of little value as a variable substitution.
*   ${phaseId} eg. "install"
*   ${artifact.location} the full file name of the target location to which the jar will be installed.
*   ${lastResult} the result of the execution of the action for the previous phase. The result is substituted only if it

is a String.

Additional Installable Unit Advice:
-----------------------------------

In addition to customizing attributes of the containing IU one can also author addtional installable units that work with the container IU. Typically this mechanism is used to author an IUFragment that customizes the containing IU or one of it's dependencies.

```
 iu.<#>.id= <identifier> 
 iu.<#>.version= <version> _(optional)_
```

Where <#> is an index for the Installable Unit, so multiple Installable Units can be declared. A fairly full range of IU customizations are supported including:

```
 id
 version
 singleton
 copyright
 licenses
 filter
 touchpoint
 update
 artifacts
 properties
 provides
 requires
 metaRequirements
 hostRequirements
 instructions
```

To illustrate all the various settings for these customizations here's a more complete example of: (unit.0) a minimal IU and (unit.1) a full featured IU:

```
 units.0.id = testid0
 units.0.version = 1.2.3

 units.1.id = testid1
 units.1.version = 1.2.4
 units.1.singleton = true
 units.1.copyright = testCopyright
 units.1.copyright.location = [http://localhost/test](http://localhost/test)
 units.1.filter = test=testFilter
 units.1.touchpoint.id = testTouchpointId
 units.1.touchpoint.version = 1.2.5
 units.1.update.match = p2QL expression describing the update. When this is specified update.id and update.range are ignored.
 units.1.update.id = testid1
 units.1.update.range = (1,2)
 units.1.update.severity = 2
 units.1.update.description = some description
 units.1.artifacts.0.id = testArtifact1
 units.1.artifacts.0.version = 1.2.6
 units.1.artifacts.0.classifier = testClassifier1
 units.1.artifacts.1.id = testArtifact2
 units.1.artifacts.1.version = 1.2.7
 units.1.artifacts.1.classifier = testClassifier2
 units.1.licenses.0 = testLicense
 units.1.licenses.0.location = [http://localhost/license](http://localhost/license)
 units.1.properties.0.name = testName1
 units.1.properties.0.value = testValue1
 units.1.properties.1.name = testName2
 units.1.properties.1.value = testValue2
 units.1.requires.0.namespace = testNamespace1
 units.1.requires.0.name = testName1
 units.1.requires.0.range = \[1.2.3.$qualifier$, 2)
 units.1.requires.0.greedy = true
 units.1.requires.0.optional = true
 units.1.requires.0.multiple = true
 units.1.requires.1.namespace = testNamespace2
 units.1.requires.1.name = testName2
 units.1.requires.1.range = $version$
 units.1.requires.1.greedy = false
 units.1.requires.1.optional = false
 units.1.metaRequirements.0.namespace = testNamespace1
 units.1.metaRequirements.0.name = testName1
 units.1.metaRequirements.0.range = \[1.2.3.$qualifier$, 2)
 units.1.metaRequirements.0.greedy = true
 units.1.metaRequirements.0.optional = true
 units.1.metaRequirements.0.multiple = true
 units.1.metaRequirements.1.namespace = testNamespace2
 units.1.metaRequirements.1.name = testName2
 units.1.metaRequirements.1.range = $version$
 units.1.metaRequirements.1.greedy = false
 units.1.metaRequirements.1.optional = false
 units.1.provides.0.namespace = testNamespace1
 units.1.provides.0.name = testName1
 units.1.provides.0.version = 1.2.3.$qualifier$
 units.1.provides.1.namespace = testNamespace2
 units.1.provides.1.name = testName2
 units.1.provides.1.version = $version$
 units.1.instructions.configure = addProgramArg(programArg:-startup); addProgramArg(programArg:@artifact);
 units.1.instructions.unconfigure = removeProgramArg(programArg:-startup); removeProgramArg(programArg:@artifact);)
 units.1.instructions.unconfigure.import = some.removeProgramArg
 units.1.hostRequirements.0.namespace = testNamespace1
 units.1.hostRequirements.0.name = testName1
 units.1.hostRequirements.0.range = \[1.2.3.$qualifier$, 2)
 units.1.hostRequirements.0.greedy = true
 units.1.hostRequirements.0.optional = true
 units.1.hostRequirements.0.multiple = true
```

Category Generation Using p2.inf
================================

The p2 UI allows for hierarchical organization of Installable Units based on the concept of "categories" where the children of categories are what's installable. On occasion we might want to take finer grained control of the contents of a category and what it contains. For example we might want to support further categorization of a features contents to allow individual plugins to be installed instead of the more typical features.

To support this we can tag a feature as a category as follows:

```
 properties.1.name=org.eclipse.equinox.p2.type.category
 properties.1.value=true
```

Another possibility is to use "additional IU advice" to create a specialized category IU like this:

```
 units.1.id=my.product.category
 units.1.version=1.0.0
 units.1.provides.1.namespace=org.eclipse.equinox.p2.iu
 units.1.provides.1.name=my.product.category
 units.1.provides.1.version=1.0.0
 units.1.properties.1.name=org.eclipse.equinox.p2.type.category
 units.1.properties.1.value=true
 units.1.properties.2.name=org.eclipse.equinox.p2.name
 units.1.properties.2.value=My Category Name
 requires.1.namespace=org.eclipse.equinox.p2.iu
 requires.1.name=my.product
 requires.1.range=\[1.0.0,1.0.0\]
 requires.1.greedy=true
```

Categorizing plug-ins
---------------------

The following file contains two projects showing how to create a category to show plug-ins [File:CategorizedPlugins.zip](/File:CategorizedPlugins.zip "File:CategorizedPlugins.zip").

Nested categories
-----------------

**Work in Progress**

*   p2 parser and publisher for category.xml to support nested categories: [https://bugs.eclipse.org/bugs/show_bug.cgi?id=406902](https://bugs.eclipse.org/bugs/show_bug.cgi?id=406902)
*   PDE category editor to support nested categories: [https://bugs.eclipse.org/bugs/show_bug.cgi?id=296392](https://bugs.eclipse.org/bugs/show_bug.cgi?id=296392)

**Current workaround** The following file contains a number of projects showing how to create nested categories [File:NestedCategories.zip](/File:NestedCategories.zip "File:NestedCategories.zip").

