<?xml version='1.0' encoding='UTF-8'?>
<?profile version='1.0.0'?>
<profile id='PlatformProfile' timestamp='1271428845693'>
  <properties size='7'>
    <property name='org.eclipse.equinox.p2.installFolder' value='C:\temp\zzz.plat.36\eclipse'/>
    <property name='org.eclipse.equinox.p2.cache' value='C:\temp\zzz.plat.36\eclipse'/>
    <property name='org.eclipse.update.install.features' value='true'/>
    <property name='org.eclipse.equinox.p2.roaming' value='true'/>
    <property name='org.eclipse.equinox.p2.environments' value='osgi.ws=win32,osgi.arch=x86,osgi.os=win32'/>
    <property name='eclipse.touchpoint.launcherName' value='eclipse'/>
    <property name='org.eclipse.equinox.p2.cache.extensions' value='file:/C:/temp/zzz.plat.36/eclipse/.eclipseextension|file:/C:/temp/zzz.plat.36/eclipse/configuration/org.eclipse.osgi/bundles/161/data/listener_1925729951/'/>
  </properties>

  <units size='18'>
  
      <unit id='b' version='1.0.0' singleton='false'>
      <update id='b' range='[0.0.0,1.0.0)' severity='0'/>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.name' value='B'/>
      </properties>
      <provides size='4'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='b' version='1.0.0'/>
        <provided namespace='osgi.bundle' name='b' version='1.0.0'/>
        <provided namespace='java.package' name='b' version='0.0.0'/>
        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' version='1.0.0'/>
      </provides>
      <artifacts size='1'>
        <artifact classifier='osgi.bundle' id='b' version='1.0.0'/>
      </artifacts>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='manifest'>
            Bundle-ManifestVersion: 2&#xA;Export-Package: b&#xA;Bundle-SymbolicName: b&#xA;Bundle-Name: B&#xA;Manifest-Version: 1.0&#xA;Bundle-Version: 1.0.0
          </instruction>
        </instructions>
      </touchpointData>
    </unit>
  
      <unit id='a' version='1.0.0'>
      <update id='a' range='[0.0.0,1.0.0)' severity='0'/>
      <properties size='1'>
        <property name='org.eclipse.equinox.p2.name' value='A'/>
      </properties>
      <provides size='3'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='a' version='1.0.0'/>
        <provided namespace='osgi.bundle' name='a' version='1.0.0'/>
        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' version='1.0.0'/>
      </provides>
      <requires size='2'>
        <required namespace='java.package' name='b' range='0.0.0'/>
      </requires>
      <artifacts size='1'>
        <artifact classifier='osgi.bundle' id='a' version='1.0.0'/>
      </artifacts>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='manifest'>
            Bundle-Name: A&#xA;Bundle-Version: 1.0.0&#xA;Bundle-SymbolicName: a; singleton:=true&#xA;Import-Package: b&#xA;Manifest-Version: 1.0&#xA;Bundle-ManifestVersion: 2
          </instruction>
        </instructions>
      </touchpointData>
    </unit>
  
  
      <unit id='aFeature.feature.group' version='1.0.0' singleton='false'>
      <update id='aFeature.feature.group' range='[0.0.0,1.0.0)' severity='0'/>
      <properties size='4'>
        <property name='org.eclipse.equinox.p2.name' value='AFeature'/>
        <property name='org.eclipse.equinox.p2.description' value='[Enter Feature Description here.]'/>
        <property name='org.eclipse.equinox.p2.description.url' value='http://www.example.com/description'/>
        <property name='org.eclipse.equinox.p2.type.group' value='true'/>
      </properties>
      <provides size='1'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='aFeature.feature.group' version='1.0.0'/>
      </provides>
      <requires size='2'>
        <required namespace='org.eclipse.equinox.p2.iu' name='a' range='[1.0.0,1.0.0]'/>
        <required namespace='org.eclipse.equinox.p2.iu' name='aFeature.feature.jar' range='[1.0.0,1.0.0]'>
          <filter>
            (org.eclipse.update.install.features=true)
          </filter>
        </required>
      </requires>
      <touchpoint id='null' version='0.0.0'/>
      <licenses size='1'>
        <license uri='http://www.example.com/license' url='http://www.example.com/license'>
          [Enter License Description here.]
        </license>
      </licenses>
      <copyright uri='http://www.example.com/copyright' url='http://www.example.com/copyright'>
        [Enter Copyright Description here.]
      </copyright>
    </unit>
    
    <unit id='aFeature.feature.jar' version='1.0.0'>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.name' value='AFeature'/>
        <property name='org.eclipse.equinox.p2.description' value='[Enter Feature Description here.]'/>
        <property name='org.eclipse.equinox.p2.description.url' value='http://www.example.com/description'/>
      </properties>
      <provides size='3'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='aFeature.feature.jar' version='1.0.0'/>
        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='feature' version='1.0.0'/>
        <provided namespace='org.eclipse.update.feature' name='aFeature' version='1.0.0'/>
      </provides>
      <filter>
        (org.eclipse.update.install.features=true)
      </filter>
      <artifacts size='1'>
        <artifact classifier='org.eclipse.update.feature' id='aFeature' version='1.0.0'/>
      </artifacts>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='zipped'>
            true
          </instruction>
        </instructions>
      </touchpointData>
      <licenses size='1'>
        <license uri='http://www.example.com/license' url='http://www.example.com/license'>
          [Enter License Description here.]
        </license>
      </licenses>
      <copyright uri='http://www.example.com/copyright' url='http://www.example.com/copyright'>
        [Enter Copyright Description here.]
      </copyright>
    </unit>
  
  
    <unit id='hi' version='1.0.0'>
      <update id='hi' range='[0.0.0,1.0.0)' severity='0'/>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.name' value='Hi'/>
        <property name='file.lastModified' value='1268770264173'/>
      </properties>
      <provides size='3'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='hi' version='1.0.0'/>
        <provided namespace='osgi.bundle' name='hi' version='1.0.0'/>
        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' version='1.0.0'/>
      </provides>
      <requires size='2'>
      </requires>
      <artifacts size='1'>
        <artifact classifier='osgi.bundle' id='hi' version='1.0.0'/>
      </artifacts>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='manifest'>
            Bundle-ManifestVersion: 2&#xA;Bundle-Version: 1.0.0&#xA;Bundle-Activator: hi.Activator&#xA;Manifest-Version: 1.0&#xA;Bundle-SymbolicName: hi; singleton:=true&#xA;Bundle-Name: Hi&#xA;Bundle-ActivationPolicy: lazy&#xA;
          </instruction>
        </instructions>
      </touchpointData>
    </unit>
  </units>
  
  <iusProperties size='5'>
    <iuProperties id='aFeature.feature.group' version='1.0.0'>
      <properties size='2'>
        <property name='org.eclipse.equinox.p2.internal.inclusion.rules' value='STRICT'/>
        <property name='org.eclipse.equinox.p2.type.root' value='true'/>
      </properties>
    </iuProperties>
    <iuProperties id='b' version='1.0.0'>
      <properties size='2'>
        <property name='org.eclipse.equinox.p2.type.lock' value='1'/>
        <property name='org.eclipse.equinox.p2.reconciler.dropins' value='true'/>
      </properties>
    </iuProperties>
    <iuProperties id='hi' version='1.0.0'>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.type.lock' value='1'/>
        <property name='org.eclipse.equinox.p2.internal.inclusion.rules' value='OPTIONAL'/>
        <property name='org.eclipse.equinox.p2.reconciler.dropins' value='true'/>
      </properties>
    </iuProperties>
  </iusProperties>
</profile>
