## OT-Specific Declarations in config.ini

The file `configuration/config.ini` contains fundamental settings for an
OSGi installation. OT/Equinox adds a few additional declarations to this
file, which are described here.

## Mandatory declarations:

Adding mandatory declarations to the file `config.ini` is done through
the "touchpoint" mechanism of the [p2 provisioning
system](:Category:Equinox_p2 "wikilink"), so everything happens
automatically while installing the OTDT. This section only gives
background for those who want to know what's going on behind the scenes.

  - ```
    osgi.hook.configurators.include=org.objectteams.eclipse.transformer.hook.HookConfigurator
    osgi.framework.extensions=org.objectteams.eclipse.transformer.hook
    ```


    These two declarations announce the OT/J bytecode transformers to
    the OSGi framework, by using the hooks described in [Adaptor
    Hooks](Adaptor_Hooks "wikilink").

<!-- end list -->

  - ```
     osgi.classloader.lock=classname
    ```

<!-- end list -->

  -
    This line is also recommended in order to tell the framework to use
    a new locking strategy for all classloaders, which is known to work
    best for OT/Equinox.

<!-- end list -->

  - ```
    ot.equinox=1
    ```

<!-- end list -->

  -
    This simple flag is needed to tell the OTRE that it runs inside
    OSGi.

## Optional application specific declarations

See [Forced Export](OTEquinox/Forced_Export "wikilink")

## OT-Specific Declarations in eclipse.ini

Due to the top voted bug reported against the Sun JVM (see
<https://bugs.eclipse.org/121737>) additionally two vendor-specific
options need to be passed to the JVM (not on MacOS, where these options
would cause the VM to crash), which happens by the following entries in
`eclipse.ini` (after -vmargs):

    -XX:+UnlockDiagnosticVMOptions
    -XX:+UnsyncloadClass

When running on Java 7 or greater these options should no longer be
needed.

[Config Ini](Category:OTEquinox "wikilink")