The following set of system properties can be enabled to debug http /
https connection when p2 is setup to use the Apache Http Client 4 (this
is the default case in the eclipse SDK)

`-Dorg.eclipse.ecf.provider.filetransfer.httpclient4.browse.connectTimeout=120000 (HEAD)`
`-Dorg.eclipse.ecf.provider.filetransfer.httpclient4.retrieve.connectTimeout=120000 (GET)`
`-Dorg.eclipse.ecf.provider.filetransfer.httpclient4.retrieve.connectTimeout=120000`
`-Dorg.eclipse.ecf.provider.filetransfer.httpclient4.retrieve.readTimeout=120000`

Timeout controls for JRE-http based transport. The JRE-http transport is
automatically used when we are connecting to an NTLM proxy.

`- org.eclipse.ecf.provider.filetransfer.retrieve.connectTimeout=15000`
`- org.eclipse.ecf.provider.filetransfer.retrieve.readTimeout=1000`
`- org.eclipse.ecf.provider.filetransfer.retrieve.retryAttempts=20`
`- org.eclipse.ecf.provider.filetransfer.retrieve.closeTimeout=1000`
`- org.eclipse.ecf.provider.filetransfer.browse.connectTimeout=3000`
`- org.eclipse.ecf.provider.filetransfer.browse.readTimeout=1000`

The number of retrying attempts(Socket Timeout and Reset exception) for
ECF based transport implementation. The default value is no retrying.
Since **Juno M2**.

`-Dorg.eclipse.equinox.p2.transport.ecf.retry=5`

[Transport debugging / tracing](Category:Equinox_p2 "wikilink")