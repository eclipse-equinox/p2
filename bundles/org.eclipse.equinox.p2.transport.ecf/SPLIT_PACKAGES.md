# Split packages relevant for this bundle

`Import-Package` is wired to exactly one exporter, so an importer of a split package only sees the classes of that bundle.
ECF itself avoids this with `Require-Bundle: org.eclipse.ecf`, which re-exports `org.eclipse.ecf.identity`.

## `org.eclipse.ecf.core.util`

Exported by two bundles, both at version `3.6.0` and without further attributes:

* `org.eclipse.ecf.identity`: `ECFException`, `ECFRuntimeException`, `Trace`, `LogHelper`, `Base64`, `StringUtils` and others.
* `org.eclipse.ecf`: `Proxy`, `ProxyAddress`, `Event` and others.

Which half is needed:

* This bundle: the identity half, only for `ECFException`, and only indirectly (see the pitfall below).
* Oomph: the `org.eclipse.ecf` half (`Proxy`, `ProxyAddress`).
* The ECF `filetransfer` API: both halves.

Since nothing distinguishes the exporters, the only way to select one is `bundle-symbolic-name` on the import, for example `org.eclipse.ecf.core.util;bundle-symbolic-name="org.eclipse.ecf.identity"`.

## `org.eclipse.ecf.core.util.reflection`

Exported by `org.eclipse.ecf` (`2.3.0`) and `org.eclipse.ecf.sharedobject` (`2.2.100`).
The versions differ, so an import range can select one.
Not used by p2 or Oomph.

## Pitfall: multi-catch of ECF exceptions

The ECF file transfer exceptions extend `ECFException`.
For `catch (A | B e)` the compiler records the common supertype, here `ECFException`, in the class.
The JVM then needs that class by name when it verifies the class, and fails with `NoClassDefFoundError: org/eclipse/ecf/core/util/ECFException` if the bundle does not see the package.
Use separate `catch` blocks instead (see `FileInfoReader`).
