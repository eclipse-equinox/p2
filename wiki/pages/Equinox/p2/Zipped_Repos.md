With 3.5M6, there are zipped p2 repos for the RCP, CVS, JDT and PDE
(Runtime and SDK) features available with each build.

These zipped repos can be either used to provision your install or
provision your PDE target. See
[Equinox/p2/Metadata_Consumption](Equinox/p2/Metadata_Consumption "wikilink")
for background information on the motivation and value of providing
repositories in this format.

These repos have a format like this

    artifacts.jar
    content.jar
    plugins/
    pluginA.jar
    pluginB.jar
    pluginC.jar
    features/
    featureA.jar
    featureB.jar

The zipped repo is not in the format eclipse/plugins etc. so they cannot
just be just unzipped over your existing install or into your dropins
folder. You need to use the p2 user ui to provision them into your
install or pde target.

### Headless: Provisioning your install from a zipped p2 repo

To provision your install through the p2 director, you can use the
zipped repo like this (note the usage of <jar:file>: and the exclamation
point):

eclipsec -application org.eclipse.equinox.p2.director -repository
<jar:file:path_to.zip>\! -list

### UI: Provisioning your install from a zipped p2 repo

To provision your install, select
<strong>Help</strong>-\><strong>Install New Software</strong> and select
<strong>Add</strong> to add a new repository. Select
<strong>Archive</strong> to point to the p2 repo archive you've
downloaded from the build page.

![Image:Repo1.JPG](images/Repo1.JPG "Image:Repo1.JPG")

For example, if you wish to provision from the M6 JDT source zipped repo

![Image:Repo2.JPG](images/Repo2.JPG "Image:Repo2.JPG")

Select Open. You'll see the contents of the repository, identical to one
on a remote http repository.

![Image:Repo3.jpg](images/Repo3.jpg "Image:Repo3.jpg")

Proceed to provision your install as if you were provisioning from a
remote repository.

### Provisioning your target from a zipped repo

When setting your target, select <strong>Add</strong> and select
<strong>Repository or update site</strong>

![Image:pde1.jpg](images/pde1.jpg "Image:pde1.jpg")

Select <strong>Manage Site</strong>

![Image:Pde2.jpg](images/Pde2.jpg "Image:Pde2.jpg")

Select <strong>Archive</strong> and proceed to select the zipped archive
on your filesystem. Proceed to provision your target as if you were
provisioning from a remote repository.

Note: Please see bug
[bug 268210](https://bugs.eclipse.org/bugs/show_bug.cgi?id=268210) for
issues related to provisioning from these repos for your PDE target.
Currently, you can't provision from these repos in PDE because the
repository browser doesn't show categories.

![Image:pde3.jpg](images/pde3.jpg "Image:pde3.jpg")

[Zipped Repos](Category:Equinox_p2 "wikilink")
