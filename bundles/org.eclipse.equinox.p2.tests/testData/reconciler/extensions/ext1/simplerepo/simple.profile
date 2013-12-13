<?xml version='1.0' encoding='UTF-8'?>
<?profile version='1.0.0'?>
<profile id='DefaultProfile' timestamp='1386012937274'>
    <units size='1'>
    <unit id='zzz' version='1.0.0' singleton='false'>
      <update id='zzz' range='[0.0.0,1.0.0)' severity='0'/>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.name' value='ZZZ Plug-in'/>
      </properties>
      <provides size='3'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='zzz' version='1.0.0'/>
        <provided namespace='osgi.bundle' name='zzz' version='1.0.0'/>
        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' version='1.0.0'/>
      </provides>
      <artifacts size='1'>
        <artifact classifier='osgi.bundle' id='zzz' version='1.0.0'/>
      </artifacts>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='manifest'>
            Bundle-ManifestVersion: 2&#xA;Bundle-RequiredExecutionEnvironment: J2SE-1.5&#xA;Bundle-SymbolicName: zzz&#xA;Bundle-Name: Zzz Plug-in&#xA;Manifest-Version: 1.0&#xA;Bundle-Version: 1.0.0
          </instruction>
        </instructions>
      </touchpointData>
    </unit>
    </units>
</profile>