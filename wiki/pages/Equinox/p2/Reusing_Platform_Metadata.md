This page outlines the current shape of the metadata for the
"org.eclipse.platform.ide" product. This product is a subset of the
Eclipse SDK. The SDK adds JDT, PDE, CVS and Help, but everything that
requires special configuration exists in the base platform. This makes
the platform ideal for general reuse.

The current RC1 metadata for org.eclipse.platform.ide looks something
like this:

<table>
<thead>
<tr class="header">
<th><p>IU Name</p></th>
<th><p>Provided Contents/Actions</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>org.eclipse.platform.ide</p></td>
<td><p>addRepository : add default repositories<br />
mdkir : create the dropins folder</p></td>
</tr>
<tr class="even">
<td><table>
<tbody>
<tr class="odd">
<td><p>org.eclipse.platform.feature.group</p></td>
<td><p>The features and bundles that make up the platform</p></td>
</tr>
<tr class="even">
<td><p>org.eclipse.p2.user.ui.feature.group</p></td>
<td><p>The features and bundles that make up p2</p></td>
</tr>
<tr class="odd">
<td><p>org.eclipse.rcp.configuration.feature.group</p></td>
<td><p>Eclipse branded launchers with appropriate requirements on the launcher fragments as well as CUs for setting <code>-startup</code> and <code>--launcher.library</code>.</p></td>
</tr>
<tr class="even">
<td><p>toolingorg.eclipse.configuration</p></td>
<td><p>Set <b><code>osgi.instance.area.default=@user.home/workspace</code></b> (os != macosx)</p></td>
</tr>
<tr class="odd">
<td><p>toolingorg.eclipse.configuration.macosx</p></td>
<td><p>Set <b><code>osgi.instance.area.default=@user.home/Documents/workspace</code></b> (os == macosx)</p></td>
</tr>
<tr class="even">
<td><p>toolingorg.eclipse.platform.ide.configuration</p></td>
<td></td>
</tr>
<tr class="odd">
<td><table>
<tbody>
<tr class="odd">
<td><p>toolingorg.eclipse.platform.ide.config.[ws.os.arch]</p></td>
<td><p>Properties for config.ini<br />
osgi.bundles.defaultStartLevel<br />
osgi.splashPath<br />
eclipse.application<br />
eclipse.product<br />
eclipse.buildId</p></td>
</tr>
<tr class="even">
<td><p>toolingorg.eclipse.platform.ide.ini.[ws.os.arch]</p></td>
<td><p>Entries for the launcher .ini file<br />
program: -showplash org.eclipse.platform --launcher.XXMaxPermSize 256m<br />
vm: -Xmx40m -Xmx256m</p></td>
</tr>
<tr class="odd">
<td><p>tooling[ws.os.arch][bundle-id]</p></td>
<td><p>Start level information for individual bundles</p></td>
</tr>
</tbody>
</table></td>
<td></td>
</tr>
</tbody>
</table></td>
<td></td>
</tr>
</tbody>
</table>


\==Using org.eclipse.platform.ide As-Is == In general, the
org.eclipse.platform.ide is not attractive for reuse in other products
as it includes product specific branding and settings. However, in the
special case of products (like EPP) that look the same as eclipse but
change the default eclipse.product, then we could reuse
org.eclipse.platform.ide directly. The epp.product file would specify a
program argument `-product` which would override the entry in the
config.ini file. As well, because org.eclipse.platform.ide includes
launcher, we should set the product build to not include launchers.

This would be done using a
[p2.inf](Equinox/p2/Engine/Touchpoint_Instructions#Authoring_touchpoint_data "wikilink")
file which should be placed beside the product file. The p2.inf file
should also tell pde.build not to generate defaults CUs for start
levels, because we will already get start levels from the platform.ide.

    builder/build.properties : includeLaunchers=false
    epp.product
       Program Arguments: -product epp.package.cpp
       Include features : CDT, etc...
    p2.inf
       #tell pde.build not to generate start levels
       org.eclipse.pde.build.append.startlevels=false

       #add requirement on org.eclipse.platform.ide
       requires.1.namespace=org.eclipse.equinox.p2.iu
       requires.1.name=org.eclipse.platform.ide
       requires.1.range=[3.5.0.I20090513-2000,3.5.0.I20090513-2000]
       requires.1.greedy=true

## Proposed Modifications

We propose that the IU `toolingorg.eclipse.platform.ide.configuration`
be made more generally reusable. Currently, it contains the following
product specific settings:

  - config.ini settings: osgi.splashPath,
    eclipse.application,eclipse.product
  - All program and vm arguments are arguably product specific, but
    `-showsplash org.eclipse.platform` is especially so.

We propose that these product specific setting be moved into the
`org.eclipse.platform.ide` IU, leaving the configuration IU as more
generally reusable.

## Links

  - [Touchpoint
    Instructions](Equinox/p2/Engine/Touchpoint_Instructions "wikilink")
  - [Bug 276125 - Structure configuration metadata for
    reuse](https://bugs.eclipse.org/bugs/show_bug.cgi?id=276125)

[Reusing Platform Metadata](Category:Equinox_p2 "wikilink")