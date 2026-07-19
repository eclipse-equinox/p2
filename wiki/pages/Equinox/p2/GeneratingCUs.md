# Generating Configuration Metadata

There are bundles that require special configuration in order to work
properly. Such configuration depends on the product they are included
in, however most of the time, such configuration information is reusable
across different products. Most commonly this configuration information
is start levels, however it can include actions for which there is no
current mechanism to generate configuration metadata.

  - People composing products don't necessarily know the details how all
    the bundles need to be configured
  - People composing product would like to reuse pre-existing
    configuration information

The proposed solution is allow a feature to include a p2.inf specifying
information on how configure bundles. CUs will then be generated and
included in the feature.

### Configuring RCP

org.eclipse.rcp includes a few bundles that need configuring, the normal
configuration is as follows:

  - org.eclipse.core.runtime needs to be started
  - org.eclipse.equinox.common needs start level 2
  - org.eclipse.equinox.simpleconfigurator needs start level 1
  - org.eclpse.update.configurator should have a property
    "org.eclipse.update.reconcile=false"
  - org.eclipse.equinox.launcher needs to add a -startup program
    argument
  - org.eclipse.equinox.launcher.\* fragments need to add
    --launcher.library program argument

To do this, create a new feature org.eclipse.rcp.configure

  - build.properties : bin.includes is empty, there is no
    rcp.configure.jar (see [Other
    Considerations](#Other_Considerations "wikilink") below)
  - includes org.eclipse.rcp (optional)
  - contains a p2.inf with required properties

When generating metadata for org.eclipse.rcp.configure, the result is

` org.eclipse.rcp.configure.feature.group`
`     - includes org.eclipse.rcp.feature.group`
`     - includes generated CUs`

People authoring an rcp product can include org.eclipse.rcp.configured
in their product to get rcp plus the required CUs

## Configuring Features

In general there are decisions around how to structure configuration
features

1.  Is this feature intended for general consumption by others?
2.  What other features are required/included by this feature.

Consider org.eclipse.platform, it includes org.eclipse.rcp and
org.eclipse.p2.user.ui. Both rcp and p2.user.ui require configuration,
but nothing directly in platform does. However, since
org.eclipse.platform is commonly reused by others, it would be useful to
have

`org.eclipse.platform.configured`
`   -includes org.eclipse.rcp.configured`
`   -includes org.eclipse.equinox.p2.user.ui.configured`

org.eclipse.platform.configured is generally reusable, people don't need
to know that if they include platform they need to include configuration
for rcp and p2.user.ui. They can just include platform.configured.

We can also decide to require other .configured features instead of
including them. Consider features that don't form products on their own
but are commonly bundled into other products. If such a feature required
configuration of its own, it could require configuration of platform
instead of including it.

## Configuring Products

Products should be organized so that the product IU includes a
product.config IU. The product.config IU then includes all necessary
feature.configured IUs (who in turn include all the CUs). The
product.config IU is then reusable by other products.

  - org.eclipse.platform.ide is a product, contains
    org.eclipse.platform.ide.configured
      - org.eclipse.sdk.ide is a product, include
        org.eclipse.platform.ide.configured
          - com.acme.awesomeness is a product, include
            org.eclipse.sdk.ide.configured

# p2.inf format

See also [Authoring Touchpoint
Data](Equinox/p2/Engine/Touchpoint_Instructions#Authoring_touchpoint_data "wikilink")
The format for specifying configuration information for other bundles
could be similar to source generation in pde.build. We need to be able
to do the following:

1.  specify bundle id (& version?)
2.  specify the name for the new CU
3.  specify touchpoint instructions
4.  perhaps specify a config filter that the new CU will receive

Consider perhaps the following:

`configure.prefix = `<cuPrefix>
`configure.`<phase>`@<bundle.id>=`<actions>

With the result being a CU named "<cuPrefix>.\<bundle.id\>" with host
\<bundle.id\> and the given touchpoint actions.

### Other Considerations

If we expect people to simply include the configured features in the
products/features then this requires actual configured.feature jars for
build purposes. If the configured features are simply IUs in the
metadata without an actual jar artifact, then people need to be able to
specify includes/requires on IUs directly.

[Generating CUs](Category:Equinox_p2 "wikilink")