This project is a releng builder for the org.eclipse.equinox.p2.examples.rcp.cloud project.

1) This build requires 3.5M6 or later

2) This build requires the deltapack.  By default it looks beside the eclipse install for "deltapack/eclipse/*".  
   If your deltapack is located elsewhere, set the "deltapack" property or edit the buildProduct.xml file.  The
   version of the deltapack used should match the version of the eclipse that is running.

3) Due to bug 268867, if the buildDirectory/buildRepo is deleted/cleaned, then the repo must also be removed from 
   the workspace Available Software Sites preferences.
    
4) Use the included launch config, or run buildProduct.xml as an ant build using the same JRE as the workspace 