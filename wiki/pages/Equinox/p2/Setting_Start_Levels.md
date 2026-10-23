When using p2, a bundle can use a
[p2.inf](Equinox/p2/Customizing_Metadata "wikilink") file to provide
[touchpoint
instructions](Equinox/p2/Engine/Touchpoint_Instructions "wikilink") to
mark its own start level. However, if a bundle is expected to be reused
in different products, then there may be conflicting requirements for
what level it should actually be started at. Because of this, it is a
good idea to not have the start level instructions as part of the bundle
itself.

p2 has the concept of a Configuration Unit (CU). A CU is really an
[Installable Unit](Installable_Units "wikilink") that provides
configuration information . What we want is to use the p2.inf to
generate a CU that provides the configuration information for a
particular bundle. This CU will take the form of an [Installable Unit
Fragment](Installable_Units#Installable_Unit_fragments "wikilink").

By default, the generated metadata published for a product contains a
default fragment which attaches to any OSGi bundle. It looks something
like this:

    <unit id='tooling.osgi.bundle.default' version='1.0.0' singleton='false'>
       <hostRequirements size='1'>
          <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='0.0.0' multiple='true' greedy='false'/>
       </hostRequirements>
       <properties size='1'>
          <property name='org.eclipse.equinox.p2.type.fragment' value='true'/>
       </properties>
       <provides size='1'>
          <provided namespace='org.eclipse.equinox.p2.iu' name='tooling.osgi.bundle.default' version='1.0.0'/>
       </provides>
       <requires size='1'>
          <required namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' range='0.0.0' multiple='true' greedy='false'/>
       </requires>
       <touchpoint id='null' version='0.0.0'/>
       <touchpointData size='1'>
          <instructions size='4'>
             <instruction key='install'>installBundle(bundle:${artifact})</instruction>
             <instruction key='uninstall'>uninstallBundle(bundle:${artifact})</instruction>
             <instruction key='configure'>setStartLevel(startLevel:4);</instruction>
          </instructions>
       </touchpointData>
    </unit>

This fragment provides instructions for 3 separate phases of a p2
install. During the install phase, the bundle must be installed into the
OSGi runtime, similarly it will be uninstalled during the uninstall
phase. And during the configure phase, the start level is set.

This is the kind of CU we need to generate for our bundle. PDE/Build
does this automatically when [exporting
products](http://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/pde_p2_configuringproducts.htm).
It generates a p2.inf for the .product file. When doing this manually,
the p2.inf used will normally be for the feature that contains the
bundle we are interested in.

### The p2.inf

The p2.inf must do two things: create the CU fragment and create a
requirement from the feature to the CU fragment. The requirement is
needed to make sure that the fragment gets included when installing the
feature. Here is an example p2.inf file that creates a fragment for the
bundle `org.example.bundle` and sets the start level to 2 and marks the
bundle to be auto-started.

    #create a requirement on the fragment we are creating
    requires.0.namespace=org.eclipse.equinox.p2.iu
    requires.0.name=configure.org.example.bundle
    requires.0.range=[$version$,$version$]
    requires.0.greedy=true

    #create a IU fragment named configure.org.example.bundle
    units.0.id=configure.org.example.bundle
    units.0.version=$version$
    units.0.provides.1.namespace=org.eclipse.equinox.p2.iu
    units.0.provides.1.name=configure.org.example.bundle
    units.0.provides.1.version=$version$
    units.0.instructions.install=org.eclipse.equinox.p2.touchpoint.eclipse.installBundle(bundle:${artifact});
    units.0.instructions.uninstall=org.eclipse.equinox.p2.touchpoint.eclipse.uninstallBundle(bundle:${artifact});
    units.0.instructions.unconfigure=org.eclipse.equinox.p2.touchpoint.eclipse.setStartLevel(startLevel:-1); \
                                     org.eclipse.equinox.p2.touchpoint.eclipse.markStarted(started:false);
    units.0.instructions.configure=org.eclipse.equinox.p2.touchpoint.eclipse.setStartLevel(startLevel:2); \
                                   org.eclipse.equinox.p2.touchpoint.eclipse.markStarted(started:true);
    units.0.hostRequirements.1.namespace=osgi.bundle
    units.0.hostRequirements.1.name=org.example.bundle
    units.0.hostRequirements.1.range=[3.6.0.v20100503,3.6.0.v20100503]
    units.0.hostRequirements.1.greedy=false
    units.0.hostRequirements.2.namespace=org.eclipse.equinox.p2.eclipse.type
    units.0.hostRequirements.2.name=bundle
    units.0.hostRequirements.2.range=[1.0.0,2.0.0)
    units.0.hostRequirements.2.greedy=false
    units.0.requires.1.namespace=osgi.bundle
    units.0.requires.1.name=org.example.bundle
    units.0.requires.1.range=[3.6.0.v20100503,3.6.0.v20100503]
    units.0.requires.1.greedy=false

### Comments

  - $version$ will be replaced by the version of the container
    associated with the p2.inf, in this case the feature.
  - Here we are using fully qualified touchpoint actions like
    "org.eclipse.equinox.p2.touchpoint.eclipse.installBundle" to avoid
    confusion. This is not strictly necessary because the fragment is
    merged with the host IU. By default, IUs for osgi bundles have
    <touchpoint id='org.eclipse.equinox.p2.osgi' /> which is the type of
    the Eclipse touchpoint (org.eclipse.equinox.p2.touchpoint.eclipse).
  - See the [Customizing
    Metadata](Equinox/p2/Customizing_Metadata "wikilink") page for
    details on the p2.inf format.
  - Currently, only one CU fragment can be attached to an IU (with the
    exception of translation fragments in Helios). If more than one CU
    fragment is available (ie, the default `tooling.osgi.bundle.default`
    and your custom `configure.org.example.bundle` fragment), then the
    fragment with the highest number of satisfied host requirements is
    used. This is why we provide 2 host requirements for our CU as
    compared to only one used by the default CU. If more than one CU for
    a given bundle has the same number of host requirements, then which
    CU gets chosen by p2 is unpredictable.

[Start Levels](Category:Equinox_p2 "wikilink")