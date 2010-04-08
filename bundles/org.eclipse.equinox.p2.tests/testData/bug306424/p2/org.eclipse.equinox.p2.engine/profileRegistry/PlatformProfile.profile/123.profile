<?xml version='1.0' encoding='UTF-8'?>
<?profile version='1.0.0'?>
<profile id='PlatformProfile' timestamp='1266347403310'>
  <properties size='8'>
    <property name='org.eclipse.equinox.p2.installFolder' value='C:\temp\zzz.plat\eclipse'/>
    <property name='org.eclipse.equinox.p2.cache' value='C:\temp\zzz.plat\eclipse'/>
    <property name='org.eclipse.update.install.features' value='true'/>
    <property name='org.eclipse.equinox.p2.roaming' value='true'/>
    <property name='org.eclipse.equinox.p2.flavor' value='tooling'/>
    <property name='org.eclipse.equinox.p2.environments' value='osgi.ws=win32,osgi.os=win32,osgi.arch=x86'/>
    <property name='eclipse.touchpoint.launcherName' value='eclipse'/>
    <property name='org.eclipse.equinox.p2.cache.extensions' value='file:/C:/temp/zzz.plat/eclipse/.eclipseextension|file:/C:/temp/zzz.plat/eclipse/configuration/org.eclipse.osgi/bundles/81/data/listener_1925729951/'/>
  </properties>
  <units size='2'>
    <unit id='a' version='1.0.0' singleton='false'>
      <update id='a' range='[0.0.0,1.0.0)' severity='0'/>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.name' value='A'/>
        <property name='file.name' value='C:\temp\zzz.plat\eclipse\dropins\a_1.0.0.jar'/>
        <property name='file.lastModified' value='1268923175284'/>
      </properties>
      <provides size='3'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='a' version='1.0.0'/>
        <provided namespace='osgi.bundle' name='a' version='1.0.0'/>
        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' version='1.0.0'/>
      </provides>
      <requires size='1'>
        <required namespace='osgi.bundle' name='b' range='0.0.0'/>
      </requires>
      <artifacts size='1'>
        <artifact classifier='osgi.bundle' id='a' version='1.0.0'/>
      </artifacts>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='manifest'>
            Bundle-ManifestVersion: 2&#xA;Bundle-SymbolicName: a&#xA;Require-Bundle: b&#xA;Bundle-Name: A&#xA;Manifest-Version: 1.0&#xA;Bundle-Version: 1.0.0&#xA;
          </instruction>
        </instructions>
      </touchpointData>
    </unit>
    <unit id='b' version='1.0.0' singleton='false'>
      <update id='b' range='[0.0.0,1.0.0)' severity='0'/>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.name' value='B'/>
        <property name='file.name' value='C:\temp\zzz.plat\eclipse\dropins\b_1.0.0.jar'/>
        <property name='file.lastModified' value='1268923175314'/>
      </properties>
      <provides size='3'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='b' version='1.0.0'/>
        <provided namespace='osgi.bundle' name='b' version='1.0.0'/>
        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' version='1.0.0'/>
      </provides>
      <artifacts size='1'>
        <artifact classifier='osgi.bundle' id='b' version='1.0.0'/>
      </artifacts>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='manifest'>
            Bundle-SymbolicName: b&#xA;Bundle-ManifestVersion: 2&#xA;Bundle-Version: 1.0.0&#xA;Bundle-Name: B&#xA;Manifest-Version: 1.0&#xA;
          </instruction>
        </instructions>
      </touchpointData>
    </unit>
  </units>
  <iusProperties size='2'>
    <iuProperties id='a' version='1.0.0'>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.type.lock' value='1'/>
        <property name='org.eclipse.equinox.p2.internal.inclusion.rules' value='OPTIONAL'/>
        <property name='org.eclipse.equinox.p2.reconciler.dropins' value='true'/>
      </properties>
    </iuProperties>
    <iuProperties id='b' version='1.0.0'>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.type.lock' value='1'/>
        <property name='org.eclipse.equinox.p2.internal.inclusion.rules' value='OPTIONAL'/>
        <property name='org.eclipse.equinox.p2.reconciler.dropins' value='true'/>
      </properties>
    </iuProperties>
  </iusProperties>
</profile>