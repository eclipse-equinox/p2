# Things to improve

## Fallback to the JRE HTTP client (NTLM proxies)

The ECF HttpClient providers report HTTP status 477 when a proxy requests NTLM and the provider is not configured for it.
p2 reacts indirectly: `RepositoryStatusHelper.checkJREHttpClientRequired` throws `JREHttpClientRequiredException`, and `RepositoryTransport` then calls `Activator.useJREHttpClient()`.
That unregisters the `http` and `https` factories through `IFileTransferProtocolToFactoryMapper`, so that ECF falls back to the JRE based provider.

Once the ECF transport code is part of this bundle, this case should be handled directly in the transport instead of through a status code and a provider registry.
