Like other parts of ECF, the [filetransfer API](ECF_API_Docs "wikilink")
has a provider architecture, allowing multiple implementations to be
used for filetransfer (i.e. via http, ftp, and others). In ECF
3.0/Eclipse 3.5 the primary provider is based upon [Apache
httpclient 3.1](http://hc.apache.org/httpclient-3.x/index.html). This
was introduced in the ECF 3.0/Eclipse 3.5 cycle because the previous
provider that was based upon the JRE URLConnection implementation proved
insufficiently reliable (i.e. see
[bug 166179](https://bugs.eclipse.org/bugs/show_bug.cgi?id=166079)).

Unfortunately, the Apache httpclient implementation, although more
robust than the URLConnection-based provider, does not support NTLMv2
proxies directly (for an explanation of why, see
[here](http://wiki.apache.org/jakarta-httpclient/FrequentlyAskedNTLMQuestions#head-d318851e40156de248e72fbb15bbce52a5e18f2c)).

For NTLMv2 Proxies, that require username and password for access the
workaround is to

1.  Disable the ECF httpclient provider.
2.  Provide the NTLMv2 proxy authentication info (proxyhost, domain,
    username, and password)

In ECF 3.0/Galileo both can be done via system properties provided to
Eclipse on startup. Here is an example using 'myproxy', 'mydomain',
'myusername', and 'mypassword':

    -Dorg.eclipse.ecf.provider.filetransfer.excludeContributors=org.eclipse.ecf.provider.filetransfer.httpclient
    -Dhttp.proxyPort=8080
    -Dhttp.proxyHost=myproxy
    -Dhttp.proxyUser=mydomain\myusername
    -Dhttp.proxyPassword=mypassword
    -Dhttp.nonProxyHosts=localhost|127.0.0.1

The first property disables the httpclient provider (and so uses the
URLConnection-based provider, which does have support for NTLMv2
proxies), and the next 5 properties are as specified by Sun for the
[URLConnection-based
provider](http://java.sun.com/j2se/1.4.2/docs/guide/net/properties.html).

In the future, it is likely that with other providers (e.g. the [Apache
httpcore client...i.e.
bug 251740](https://bugs.eclipse.org/bugs/show_bug.cgi?id=251740) or
[Jetty's asynch filetransfer
implementation](http://docs.codehaus.org/display/JETTY/Jetty+HTTP+Client)
we will be able to [support NTLMv2 proxies...i.e.
bug 252002](https://bugs.eclipse.org/bugs/show_bug.cgi?id=252002) more
directly. Further, in future versions the Eclipse platform [proxy API
should support NTLMv2 as well...i.e.
bug 269832](https://bugs.eclipse.org/bugs/show_bug.cgi?id=269832).

Actually, it is unlikely that the Apache HTTP Client or [HTTP
Components](http://hc.apache.org/httpcomponents-client/ntlm.html) will
ever support NTLMv2. They have never supported it even though the
[technical information](http://davenport.sourceforge.net/ntlm.html) has
been available for years. They are currently concerned with the legal
issues.

[Category:Eclipse Communication
Framework](Category:Eclipse_Communication_Framework "wikilink")
[Category:EclipseRT](Category:EclipseRT "wikilink") [Category:Draft
Documentation](Category:Draft_Documentation "wikilink")
[Category:Equinox p2](Category:Equinox_p2 "wikilink")