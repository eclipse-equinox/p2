This example will fetch Subversive, SVNKit, and the connectors from
their respective sites into a local p2 repo (mirror).

If you also want to fetch all lower-level Eclipse requirements, set
`followStrict="false"` .

From the local repo, a site.xml will be generated.

Finally, that site.xml will be used to produce metadata.

The resulting site will then be zipped.

Here's the p2.mirror task:

``` xml
<property name="working.dir" value="/tmp/partial-repo-mirror" />
<p2.mirror destination="file:/${working.dir}" description="Subversive All-In-One Repo">
  <source>
    <repository location="http://eclipse.svnkit.com/1.2.x/" />
    <repository location="http://www.polarion.org/projects/subversive/download/eclipse/2.0/update-site/" />
    <repository location="http://download.eclipse.org/technology/subversive/0.7/update-site/" />
    <repository location="http://download.eclipse.org/rt/ecf/3.0/3.5/repo/" />
    <repository location="http://download.eclipse.org/releases/galileo/" />
    <repository location="http://download.cloudsmith.com/galileoplus/" />
  < /source>
  <iu id="org.tmatesoft.svnkit.feature.group" />
  <iu id="com.sun.jna.feature.group" />

  <iu id="org.polarion.eclipse.team.svn.connector.feature.group" />
  <iu id="org.polarion.eclipse.team.svn.connector.svnkit16.feature.group" />

  <iu id="org.eclipse.team.svn.resource.ignore.rules.jdt.feature.group" />
  <iu id="org.eclipse.team.svn.feature.group" />

  <iu id="org.eclipse.ecf" />
  <iu id="org.eclipse.ecf.filetransfer" />
  <iu id="org.eclipse.ecf.identity" />
  <iu id="org.eclipse.ecf.provider.filetransfer" />
  <iu id="org.eclipse.ecf.provider.filetransfer.httpclient" />
  <iu id="org.eclipse.ecf.provider.filetransfer.httpclient.ssl" />
  <iu id="org.eclipse.ecf.provider.filetransfer.ssl" />
  <iu id="org.eclipse.ecf.ssl" />

  <slicingOptions includeFeatures="true" followStrict="true"/>
</p2.mirror>
```

  - [The rest of the script can be seen
    here](http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.dash/athena/org.eclipse.dash.commonbuilder/org.eclipse.dash.common.releng/tools/scripts/partialMirrorFromRepo.xml?root=Technology_Project&view=markup).
    It requires Ant-Contrib.

<i>NOTE: While the example code above is licensed under the Eclipse
Public License (EPL), some of the software fetched by <b>RUNNING</b> the
above example is decidedly NOT.</i>

[Category:Releng](Category:Releng "wikilink") [Ant Tasks/Partial
Mirroring/Example/Galileo](Category:Equinox_p2 "wikilink")