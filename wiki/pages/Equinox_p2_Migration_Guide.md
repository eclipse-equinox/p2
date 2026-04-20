This is a work in progress, and subject to change.

## Before & After

Below is a comparison chart showing how to organize your extensions,
plugins & features, using a p2-free Eclipse and a p2-enabled Eclipse
(3.4M6+).

<table>

<tr valign="top">

<th>

</th>

<th width="50%">

Eclipse 3.2 through Eclipse 3.4M5

</th>

<th width="50%">

Eclipse 3.4M6+

</th>

</tr>

<tr valign="top">

<th>

 
Eclipse
folder

</th>

<td>

`eclipse/`
`  plugins/  (plugin folders & jars)`
`  features/ (feature folders)`

</td>

<td>

`eclipse/dropins/`*`somefolder`*`/eclipse/`
`  plugins/  (plugin folders & jars)`
`  features/ (feature folders)`

*- or, simpler -*

`eclipse/dropins/eclipse/`
`  plugins/  (plugin folders & jars)`
`  features/ (feature folders)`

Note that the old layout is also still supported, but with the new
dropins folder, you have more control and it's easier to clean up
unwanted extensions (eg., by deleting the whole *somefolder* folder).

</td>

</tr>

<tr valign="top">

<th>

 
.link
files

</th>

<td>

`eclipse/links/ (*.link files)`

</td>

<td>

`eclipse/dropins/ (*.link files)`

As before, each .link file contains a path to a folder in which
`eclipse/features/` and `eclipse/plugins/` can be found. For example:

`path=/home/nickb/eclipse/phpeclipse`

*- or -*

`path=X:/home/nickb/eclipse/ecf`

Note that the old layout is also still supported.

</td>

</tr>

<tr valign="top">

<th>

 
Extension
folders

</th>

<td>

`.../`*`somefolder`*`/eclipse/`
`  plugins/  (plugin folders & jars)`
`  features/ (feature folders)`
`  .eclipseextension`

These folders, including the `.eclipseextension` file, are created when
doing an Update Manager install to a folder other than the base eclipse/
directory.

If you want to create the `.eclipseextension` file by hand, [here's
how](http://divby0.blogspot.com/2007/06/managing-plugins-and-features-with-link.html).
You can also just open the `.eclipseproduct` file from the root of your
Eclipse install with Eclipse's `File > Open File...` then do a `File >
Save As...` to rename it.

To import a pre-existing folder, use `Help > Software Updates > Manage
Configuration > Add an Extension Location`.

</td>

<td>

`.../`*`somefolder`*`/eclipse/`
`  plugins/  (plugin folders & jars)`
`  features/ (feature folders)`

External Extension Locations aren't supported anymore. You can only
import the old extension directories content into the Eclipse plugins
and features directories. All files from that external Extension
Locations will then be copied into the folders under the Eclipse root
folder. For the import the .eclipseextension file is no longer required.

You can unpack a zip and point Eclipse at it. Or, even easier, point
Eclipse at the zip and it'll handle the unzip for you.

To import such a folder, use `Help > Software Updates... > Available
Features > Manage sites... > Add... > Local...` (or `Archive...`, for an
unpacked zip.)

*This doesn't feel the same as old "Extension Location". After adding a
local feature you still have to manually select available features and
explicitly install them, while with old update manager, linked
"Extension Locations" were installed automatically. Furthermore there is
no more UI support to install plugins into Extension Locations, all
plugins go directly under the Eclipse root directory.*

</td>

</tr>

</table>

### See Also

  - [Equinox p2 - Getting
    Started](Equinox_p2_Getting_Started "wikilink")
  - [Equinox p2 - Getting Started with
    Dropins](Equinox_p2_Getting_Started#Dropins "wikilink")
  - [Equinox p2 Testcases](Equinox_p2_tests "wikilink")

[Migration](Category:Equinox_p2 "wikilink")