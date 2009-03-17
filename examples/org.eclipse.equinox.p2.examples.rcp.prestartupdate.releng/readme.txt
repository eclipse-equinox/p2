This project is a releng builder for the org.eclipse.equinox.p2.examples.rcp.prestartupdate project.

1) This build requires 3.5M6 or later

2) This build requires the deltapack.  By default it looks beside the eclipse install for "deltapack/eclipse/*".  
   If your deltapack is located elsewhere, set the "deltapack" property or edit the buildProduct.xml file.  The
   version of the deltapack used should match the version of the eclipse that is running.

3) Due to bug 268867, if the buildDirectory/buildRepo is deleted/cleaned, then the repo must also be removed from 
   the workspace Available Software Sites preferences.
    
4) Use the included launch config, or run buildProduct.xml as an ant build using the same JRE as the workspace

5) The build.properties file specifically defines the JRE's for CDC-1.1/Foundation-1.1 and
   J2SE-1.5 because these are the required bundle execution environments in the example.
   The build should be run on a 1.5 VM.