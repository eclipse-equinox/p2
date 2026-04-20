This document review the implication of p2 on what/how people build and
how the consumption of the build output is affected.

<table>
<thead>
<tr class="header">
<th><p>Items that are being produced today</p></th>
<th><p>Same items in the context of p2</p></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>Plugins / features ready to run, referred as FTR. FTRs are used in the following ways:</p>
<p>a) Unzipping, and running with -clean</p>
<p>b) Creating ‘products’:</p>
<p>b.1) by extracting in a links folder</p>
<p>b.2) by unzipping on top of an eclipse install and rezipping</p>
<p>b.3) by using pde packager</p>
<p>c) Creating bigger FTRs</p>
<p>c.1) by unzipping and rezipping</p>
<p>c.2) by using packager</p>
<p>d) Replacing thing in an existing base</p></td>
<td><p>With p2, FTR should preferably contain a content.xml. The</p>
<p><code> motivation is to have consistency among the IUs that are being used when</code><br />
<code> installing from a repo or not. This also avoids problems if the metadata</code><br />
<code> generator evolves and produces different IUs since what could have been</code><br />
<code> published.</code></p>
<p> </p>
<p>a) This is supported by the drop-ins folder support. We</p>
<p><code> need to ensure that it works appropriately when there is a content.xml. There</code><br />
<code> are some restrictions since the content.xml could apply, therefore it is</code><br />
<code> recommended for people to use a links folder structure rather than wild</code><br />
<code> unzipping.</code></p>
<p>b) Because</p>
<p><code> of the p2 folder and the bundles.txt at the root of the product, and the content.xm,</code><br />
<code> some of these scenarios  become non-trivial, since the bundles.txt has to be</code><br />
<code> created, the content.xmls could collide if multiple things were being</code><br />
<code> unzipped. But let’s review the details</code></p>
<p>b.1) This scenario does not</p>
<p><code> change. The initial startup of the application will discover the content of</code><br />
<code> the links folder and all p2 data structures (bundles.txt, p2 folder) will be</code><br />
<code> updated appropriately.</code></p>
<p>b.2) Because of the p2 metadata</p>
<p><code> and the bundles.txt at the root of the product, and the content.xml, this</code><br />
<code> scenario becomes non-trivial (there may be some answers from the shared</code><br />
<code> install scenario).</code></p>
<p>The current recommendation here</p>
<p><code> is to use links folders (b.1). It is a change, but it actually represents an</code><br />
<code> improvement since things can just be dropped.</code></p>
<p>b.3) In this case the packager becomes a</p>
<p><code> provisioning operation and nothing special has to happen. What happens when</code><br />
<code> no content.xml exist?</code></p>
<p>c.1) The only difficulty here is on merging the</p>
<p><code> content.xml. However this should be doable by running the repository</code><br />
<code> mirroring tool. What happens when no content.xml exist?</code></p>
<p>c.2) In this case the packager also relies on the</p>
<p><code> repository mirroring tool. What happens when no content.xml exist?</code></p>
<p>d) This will be supported with the generation of patch</p>
<p><code> metadata.</code></p></td>
</tr>
<tr class="even">
<td><p>Update sites</p>
<p>d) the update site is directly published for users to</p>
<p><code> connect to.</code></p>
<p>e) the update site is zipped and published for the user to</p>
<p><code> download and use as a local site</code></p></td>
<td><p>Both scenarios are supported by producing appropriate p2</p>
<p><code> metadata as part of the build.</code></p></td>
</tr>
<tr class="odd">
<td><p>Eclipse-based products and how they are built</p>
<p>f) product build in PDE</p>
<p>g) adding a few things on top of eclipse (e.g.</p>
<p><code> WTP all in one, EPP packages)</code></p>
<p>h) homebrewed</p>
<p>i) eclipse base + links folder containing</p>
<p><code> plug-ins in a runnable form</code></p></td>
<td><p>Products need to have a corresponding IU (it aggregates</p>
<p><code> the features, the plugins and the various configuration bits).</code></p>
<p>f) product build needs to be made p2 aware to produce the</p>
<p><code> product IU and also create the product by invoking a p2 operation.</code></p>
<p>g) same difficulty than b.2. The recommended way is to use</p>
<p><code> a provisioning operation </code></p>
<p>h) the way to go is to provide the various archives of the</p>
<p><code> product, a .product file and invoke a p2-izer operation. May not be supported</code><br />
<code> immediately.</code></p>
<p>i) See b.1.</p></td>
</tr>
</tbody>
</table>

[build and consumption](Category:Equinox_p2 "wikilink")