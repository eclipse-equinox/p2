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
  <units size='183'>
    <unit id='aaa' version='1.0.1'>
      <update id='aaa' range='[0.0.0,1.0.1)' severity='0'/>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.name' value='Aaa_1'/>
        <property name='file.name' value='C:\temp\zzz.plat\eclipse\dropins\aaa_1.0.1.jar'/>
        <property name='file.lastModified' value='1266342894486'/>
      </properties>
      <provides size='3'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='aaa' version='1.0.1'/>
        <provided namespace='osgi.bundle' name='aaa' version='1.0.1'/>
        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' version='1.0.0'/>
      </provides>
      <artifacts size='1'>
        <artifact classifier='osgi.bundle' id='aaa' version='1.0.1'/>
      </artifacts>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='manifest'>
            Bundle-ManifestVersion: 2&#xA;Bundle-SymbolicName: aaa;singleton:=true&#xA;Require-Bundle: org.eclipse.ui&#xA;Bundle-Name: Aaa_1&#xA;Manifest-Version: 1.0&#xA;Bundle-Version: 1.0.1&#xA;
          </instruction>
        </instructions>
      </touchpointData>
    </unit>
  </units>
  <iusProperties size='183'>
    <iuProperties id='aaa' version='1.0.1'>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.type.lock' value='1'/>
        <property name='org.eclipse.equinox.p2.internal.inclusion.rules' value='OPTIONAL'/>
        <property name='org.eclipse.equinox.p2.reconciler.dropins' value='true'/>
      </properties>
    </iuProperties>
  </iusProperties>
</profile>
