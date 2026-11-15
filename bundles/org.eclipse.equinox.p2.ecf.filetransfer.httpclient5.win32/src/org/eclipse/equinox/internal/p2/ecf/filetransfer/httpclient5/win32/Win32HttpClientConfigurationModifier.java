/****************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   Red Hat Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.internal.provider.filetransfer.httpclient5.win32;

import java.util.Map;
import org.apache.hc.client5.http.auth.AuthSchemeFactory;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.config.RequestConfig.Builder;
import org.apache.hc.client5.http.impl.auth.BasicSchemeFactory;
import org.apache.hc.client5.http.impl.auth.DigestSchemeFactory;
import org.apache.hc.client5.http.impl.auth.KerberosSchemeFactory;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.win.WindowsNTLMSchemeFactory;
import org.apache.hc.client5.http.impl.win.WindowsNegotiateSchemeFactory;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient5.HttpClientModifierAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

// Note that this depends upon HttpClientContext class which is from org.apache.httpcomponents.client5.httpclient5 bundle
@Component
public class Win32HttpClientConfigurationModifier extends HttpClientModifierAdapter {

	public static final String ID = "org.eclipse.ecf.provider.filetransfer.httpclients5.win32"; //$NON-NLS-1$

	public static final String SERVICE_PRINCIPAL_NAME_ATTRIBUTE = "servicePrincipal"; //$NON-NLS-1$

	public static final String SERVICE_PRINCIPAL_NAME_PROPERTY = ID + "." + SERVICE_PRINCIPAL_NAME_ATTRIBUTE; //$NON-NLS-1$

	private String servicePrincipalName;

	@Override
	public HttpClientBuilder modifyClient(HttpClientBuilder builder) {
		HttpClientBuilder winBuilder = builder == null ? HttpClientBuilder.create() : builder;
		Lookup<AuthSchemeFactory> authSchemeRegistry = createAuthSchemeRegistry();
		return winBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
	}

	@Override
	public HttpClientContext modifyContext(HttpClientContext context) {
		Lookup<AuthSchemeFactory> authSchemeRegistry = context.getAuthSchemeRegistry();
		if (authSchemeRegistry == null) {
			authSchemeRegistry = createAuthSchemeRegistry();
		} else {
			authSchemeRegistry = modifyAuthSchemeRegistry(authSchemeRegistry);
		}
		context.setAuthSchemeRegistry(authSchemeRegistry);
		return context;
	}

	public void setServicePrincipalName(String servicePrincipalName) {
		this.servicePrincipalName = servicePrincipalName;
	}

	public String getServicePrincipalName() {
		return servicePrincipalName;
	}

	private Lookup<AuthSchemeFactory> createAuthSchemeRegistry() {
		return setWinAuthSchemes(RegistryBuilder.<AuthSchemeFactory>create()
				.register(StandardAuthScheme.BASIC, BasicSchemeFactory.INSTANCE)
				.register(StandardAuthScheme.DIGEST, DigestSchemeFactory.INSTANCE)
				.register(StandardAuthScheme.KERBEROS, KerberosSchemeFactory.DEFAULT)).build();
	}

	private Lookup<AuthSchemeFactory> modifyAuthSchemeRegistry(Lookup<AuthSchemeFactory> authSchemeRegistry) {
		RegistryBuilder<AuthSchemeFactory> builder = RegistryBuilder.create();
		for (String scheme : new String[] { StandardAuthScheme.BASIC, StandardAuthScheme.DIGEST,
				StandardAuthScheme.KERBEROS }) {
			AuthSchemeFactory provider = authSchemeRegistry.lookup(scheme);
			if (provider != null) {
				builder.register(scheme, provider);
			}
		}
		if (authSchemeRegistry.lookup(StandardAuthScheme.KERBEROS) == null) {
			builder.register(StandardAuthScheme.KERBEROS, KerberosSchemeFactory.DEFAULT);
		}
		setWinAuthSchemes(builder);
		return builder.build();
	}

	@SuppressWarnings("restriction")
	private RegistryBuilder<AuthSchemeFactory> setWinAuthSchemes(RegistryBuilder<AuthSchemeFactory> builder) {
		return builder.register(StandardAuthScheme.NTLM, new WindowsNTLMSchemeFactory(servicePrincipalName))
				.register(StandardAuthScheme.SPNEGO, new WindowsNegotiateSchemeFactory(servicePrincipalName));
	}

	@Activate
	public synchronized void activate(BundleContext context, Map<?, ?> properties) {
		this.servicePrincipalName = getServicePrincipalName(properties);
	}

	private String getServicePrincipalName(Map<?, ?> properties) {
		Object servicePrincipalValue = properties.get(SERVICE_PRINCIPAL_NAME_ATTRIBUTE);
		if (servicePrincipalValue != null) {
			return servicePrincipalValue.toString();
		}
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		if (bundle != null) {
			return bundle.getBundleContext().getProperty(SERVICE_PRINCIPAL_NAME_PROPERTY);
		}
		return System.getProperty(SERVICE_PRINCIPAL_NAME_PROPERTY);
	}

	@Override
	public Builder modifyRequestConfig(Builder config, HttpClientContext context, Map<?, ?> options) {
		return null;
	}

}
