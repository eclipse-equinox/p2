# Introduction

## Purpose

The aim of this document is to list and explain some functionalities
needed for a Metadata Repository Editor.

## Context

In Eclipse 3.4 context, a new framework, named p2, replaces the Update
Manager. Anyware wants to provide to team leaders a user-friendly tool,
to help them to manage an "œunder control" Eclipse configuration for all
the team members.

Such a tool will be a Metadata Repository Editor, allowing user to
easily configure a file named content.xml, provided by an update site to
install/update softwares in Eclipse.

A \[p2-dev\] mailing-list member, Henrik Lindberg, has developed an
editor (http://wiki.eclipse.org/Equinox_p2_Metadata_Authoring) to
allow users to easily configure an Installable Unit, which is a part of
a Metadata Repository. We will explain how we will include this tool in
ours.

## Acronyms and abbreviations

<table>
<thead>
<tr class="header">
<th><center>
<p>Acronym</p>
</center></th>
<th><center>
<p>Definition</p>
</center></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><center>
<p><strong>IU</strong></p>
</center></td>
<td><p>Installable Unit : a part of a Metadata Repository. It contains required and provided capabilities, artifacts, touchpoints...</p></td>
</tr>
<tr class="even">
<td><center>
<p><strong>EMF</strong></p>
</center></td>
<td><p>Eclipse Modelling Framework</p></td>
</tr>
<tr class="odd">
<td><center>
<p><strong>MVC</strong></p>
</center></td>
<td><p>Model-View-Controller</p></td>
</tr>
</tbody>
</table>

# Editor general description

## General behavior

This editor is a standard Eclipse editor. It allows to edit a Metadata
repository object, and to save it as a *content.xml* file.

Once the user has set some parameters required for his project, he can
provide an Update Site for members of a team, allowing them to use the
same Eclipse context.

## Users

Users are project managers, team leaders, system administrators...

## Use case scenario.

### Scenario 1:

The team leader wants to set a unique configuration for all his team
members. He knows that the project will require some libraries or
plug-ins sets: resources that p2 calls Installable Units.

Through an Eclipse wizard, he builds a new Update Site Model : it
creates a new *.mr* file. This file is the input file our Metadata
Repository Editor expects.

Then in editor view, a multi page editor is opened, inviting the user to
fill or customize what he wants to.

The first tab is an Overview of the Metadata Repository : name,
description, version... A button allows to build a concrete update site
form the currently edited *.mr* file.

The second tab allows to add/edit/remove IUs for this repository. This
tab is a kind of Master/Details, to quickly see and edit some relevant
properties on the selected IU. For a complete IU edition, the user click
on "Edit" button to open another editor, provided by Henrik Lindberg.
This is inspired by the Update Site editor, where double-clicking on a
feature opens the editor for this feature.

## General constraints of architecture

To make simple a data binding mechanism between the model (a repository)
and the previous interface, we decided to use EMF

But, as p2 models are not designed by EMF, we make the choice to not
modify p2 APIs with EMF. So a p2 IMetadataRepository is first converted
into an EMF model. This EMF model is edited with our Metadata Repository
Editor, and saved as a *.mr* file. That's why we include in the editor
the opportunity to "build" the Metadata Repository: the EMF Metadata
repo EObject, is converted into a p2 object, and we use p2 API to
serialize this p2 object to an xml file.

![Image:Img1.PNG](images/Img1.PNG "Image:Img1.PNG")

# Detailed description

## Data description

Here is an Ecore diagram, showing the data model behind the editor.

![Image:Img2.jpg](images/Img2.jpg "Image:Img2.jpg")

The Metadata Repository Editor will allow to edit some of the
MetadataRepository attributes, and the installableUnits relation.

Installable Unit edition will be delegated to another editor. Some
properties and attributes will be still editable in the Metadata
Repository Editor.

## User Interfaces

Our Metadata Repository editor is a multi-tab page, displayed in the
Eclipse editor view.

  - 1<sup>st</sup> tab must allow to edit general properties for the
    repository
  - 2<sup>nd</sup> tab must display Installable Units for the repository
  - It must open a more detailed editor when editing an IU. This
    operation can be performed with a button and/or a double-click on an
    IU.
  - It must support an undo/redo mechanism
  - The tab title in the editor view must display a dirty marker (\*)
    when modifications have been done.
  - The editor must flag incorrect values, such as incorrect value
    number,
  - The editor must disallow any Save operation while editors has
    incorrect values

### Wizard for new Metadata Repository

A wizard is necessary to allow the user to create a repository from
scratch.

  - The wizard shall be available in*' File/New/p2/Metadata
    Repository*'.
  - It shall ask for

<!-- end list -->

  - *.mr* file name : mandatory field
  - Repository name : mandatory field, a user friendly name for the
    metadata repository
  - Repository version : default to 1.0.0

<!-- end list -->

  - Validating the wizard must initialize a new simple project (no
    peculiar nature), containing a *.mr* file, and directly open the
    Metadata Repository Editor for the new created file.

### Export/Import a model

  - After editing a Metadata Repository, the user should be able to
    export it as a p2 Metadata Repository, building a *content.xml*
    file.
  - In the other way, the user should be able to load an existing p2
    Metadata Repository, giving a valid *content.xml* file.

These options could be available by 2 ways :

  - A right click on the *.mr* file could display 2 specific options :
      - "Build Metadata Repository" : the user should add some
        installable units to the current repository; otherwise a
        repository has no sense. So if the user clicks on this option,
        if no installable unit is associated, the user is invited to add
        any installable unit. If any installable unit is filled for this
        repository, clicking on this option open a popup asking the user
        where he wants to deploy its site. Once the location is entered,
        a file content.xml is generated at this place. This file
        contains all information filled through our editor.
      - "Load Metadata Repository" : build a *.mr* file from a
        *content.xml* file (location given by the user), to edit its
        contents through the editor.
  - 2 icons in overview tab could allow the user to perform an import or
    export.

### Overview page

Here is a sketch of how the first tab could be like.

<center>

![Image:Img3.jpg](images/Img3.jpg "Image:Img3.jpg")

</center>

Overview tab display those fields.

  - Name is default filled with the repository name from the wizard
  - Description is a summary for the metadata repository
  - Version is the current version for the metadata repository
  - Type is not displayed, but set to
    org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository
    (TBD)

### Installable units page

Here is a sketch of how the second tab could be like.

<center>

![Image:Img4.jpg](images/Img4.jpg "Image:Img4.jpg")

</center>

  - On the left, a list viewer must display all IUs associated to the
    current repository. For a new repository, this list is empty. The
    list can be displayed as a Tree, using the IU *group* property to
    build such an organization.

<!-- end list -->

  - Add button :
      - Always enabled.
      - Allows to add an installable unit, using a dialog to select them
        from a distant site, or IU present in the workspace (.iu file
        ?).
  - Remove button :
      - Enable only when any IU is selected.
      - Remove the association between the edited repository and the
        selected IU.
  - Edit button :
      - Enable on only when any IU is selected and when this IU is
        available as a *.iu* file in the user's workspace
      - Uses another editor (maybe Henrik Lindberg's one :
        org.eclipse.equinox.p2.authoring).
      - When closing this other editor, our editor shall be consequently
        updated.

Details section displays some details about the selected IU. They can be
updated. Properties are also available here (this is not the case in the
external IU editor). The user can add new ones or override them.

[Metadata Repository Authoring](Category:Equinox_p2 "wikilink")