2006/12/18

This bundle contains API related with FwHandler.

-------------------------
* Packages
- org.eclipse.core.fwhandler: contains APIs for handling framework (hereafter shortened as fw)
   without Configurator.

- org.eclipse.core.configurator: contains APIs related with configurator which configures bundle's lifecycle on a fw.

- org.eclipse.core.fwhandler.configurator: contains APIs for handling fw with Configurator.

-------------------------
* Developers are recommended to read the comments (or JavaDocs) in the following order.

1. FwHandlerAdmin in org.eclipse.core.fwhandler.
2. Other APIs in org.eclipse.core.fwhandler.
3. Configurator in org.eclipse.core.configurator.
4. ConfiguratorManipilatorAdmin and ConfiguratorManipilator in org.eclipse.core.configurator.
5. APIs in org.eclipse.core.fwhandler.configurator

-------------------------
* Other remarks

Examples of implementation of FwHandlerAdmin are "org.eclipse.core.fwhandler.equinox" bundle
 and "org.eclipse.core.fwhandler.kf" bundle. 
An example of implementation of Configurator is "org.eclipse.core.simpleConfigurator" bundle.
An example of implementation of ConfiguratorManipulatorAdmin is "org.eclipse.core.fwhandler.simpleConfigurator.manipulator" bundle.

An example using FwHandlerAdmin is "org.eclipse.core.fwhandler.examples" bundle.
