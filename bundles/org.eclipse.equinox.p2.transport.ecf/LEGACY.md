# Things to improve

## Fallback to the JRE HTTP client (NTLM proxies)

The ECF HttpClient providers report HTTP status 477 when a proxy requests NTLM and the provider is not configured for it.
p2 reacts indirectly: `RepositoryStatusHelper.checkJREHttpClientRequired` throws `JREHttpClientRequiredException`, and `RepositoryTransport` then calls `Activator.useJREHttpClient()`.
That unregisters the `http` and `https` factories through `IFileTransferProtocolToFactoryMapper`, so that ECF falls back to the JRE based provider.

Once the ECF transport code is part of this bundle, this case should be handled directly in the transport instead of through a status code and a provider registry.

## Windows native authentication (NTLM/SPNEGO)

The ECF bundle `org.eclipse.ecf.provider.filetransfer.httpclient5.win32` (Windows only) plugs into the HttpClient 5 provider through two internal services.
`Win32HttpClientConfigurationModifier` registers the native Windows NTLM and SPNEGO schemes of Apache HttpClient (`org.apache.hc.client5.http.impl.win`, SSPI via JNA).
`Win32NTLMProxyHandler` lets NTLM and SPNEGO proxy challenges through, instead of reporting status 477 and falling back to the JRE client.
This allows single sign-on against NTLM/Kerberos proxies and servers with the credentials of the logged-in user.

Neither this bundle nor the Apache HttpClient Windows support has automated tests upstream, and p2 has none either.
The decision logic (when 477 is reported, which schemes are registered) can be tested on any platform.
The actual SSPI authentication needs a Windows machine.

Possible ways to test it manually, to be decided later:

* Windows Server and Windows 11 evaluation images from the Microsoft Evaluation Center (VirtualBox, Hyper-V, VMware), e.g. a domain controller with IIS (Windows Authentication) and a domain joined client.
* A short lived Windows VM in Azure.
* A scheduled GitHub Actions job on a Windows runner against a local IIS with a local account, which covers NTLM only, because hosted runners are not domain joined.
