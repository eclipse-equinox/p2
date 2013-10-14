Running sharedinstall tests requires an application ("Verifier") 
that is installed in the independent Eclipse-like installation. The Verifier
checks if certain P2 operations in a shared install scenario ends with success
(f.e. it checks whether certain bundles are installed).

* REALLY IMPORTANT NOTE *
The Verifier is not built during compile time or testing time. It needs to be built
manually after each modification and be made available in a test repository located 
in bundles/org.eclipse.equinox.p2.tests/testData/sharedInstall/repo.

The steps to build the Verifier (and other bundles from that test repo are):
1. Import projects from the repo source subtree bundles/org.eclipse.equinox.p2.tests/testData/sharedInstall/source
   There is 6 subprojects there (2 test bundles, 2 test features, Verifier feature and an update site).
2. Open site.xml located in a site project, and press 'Build All'.
3. Eclipse will generate a p2 repository (directories "features" and "plugins", 
   and jars "artifacts.jar" and "content.jar").
4. Remove old content from  bundles/org.eclipse.equinox.p2.tests/testData/sharedInstall/repo,
   copy new content there, and commit to repository.