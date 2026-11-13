/****************************************************************************
 * Copyright (c) 2011 Composent and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.identity;

import java.net.URI;
import java.util.UUID;
import org.eclipse.core.runtime.Assert;

/**
 * URI ID class.
 * 
 * @since 3.0
 */
public class URIID extends BaseID implements IResourceID {

	/**
	 * @since 3.7
	 */
	public static class URIIDNamespace extends Namespace {

		private static final long serialVersionUID = 115165512542491014L;

		/**
		 * @since 3.8
		 */
		public static final String UUID_PROTOCOL = "uuid";

		public URIIDNamespace(String name, String desc) {
			super(name, desc);
		}

		public URIIDNamespace() {
			super(URIID.class.getName(), "URIID Namespace"); //$NON-NLS-1$
		}

		public ID createInstance(Object[] parameters) throws IDCreateException {
			try {
				String init = getInitStringFromExternalForm(parameters);
				if (init != null)
					return new URIID(this, new URI(init));
				if (parameters[0] instanceof URI)
					return new URIID(this, (URI) parameters[0]);
				if (parameters[0] instanceof String)
					return new URIID(this, new URI((String) parameters[0]));
				throw new IDCreateException("Cannot create URIID");
			} catch (Exception e) {
				throw new IDCreateException(URIIDNamespace.this.getName() + " createInstance()", e); //$NON-NLS-1$
			}
		}

		/**
		 * @since 3.8
		 */
		public ID createRandomUUID() throws IDCreateException {
			return createInstance(new Object[] { UUID_PROTOCOL + ":" + UUID.randomUUID().toString() });
		}

		public String getScheme() {
			return "uri";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seeorg.eclipse.ecf.core.identity.Namespace#
		 * getSupportedParameterTypesForCreateInstance()
		 */
		public Class<?>[][] getSupportedParameterTypes() {
			return new Class[][] { { String.class }, { URI.class } };
		}
	}

	private static final long serialVersionUID = 7328962407044918278L;
	private URI uri;

	/**
	 * @since 3.9
	 */
	public URIID() {

	}

	public URIID(Namespace namespace, URI uri) {
		super(namespace);
		Assert.isNotNull(uri);
		this.uri = uri;
	}

	protected int namespaceCompareTo(BaseID o) {
		if (this == o)
			return 0;
		if (!this.getClass().equals(o.getClass()))
			return Integer.MIN_VALUE;
		return this.uri.compareTo(((URIID) o).uri);
	}

	protected boolean namespaceEquals(BaseID o) {
		if (this == o)
			return true;
		if (!this.getClass().equals(o.getClass()))
			return false;
		return this.uri.toString().equals((((URIID) o).uri).toString());
	}

	protected String namespaceGetName() {
		return uri.toString();
	}

	protected int namespaceHashCode() {
		return uri.toString().hashCode() ^ getClass().hashCode();
	}

	public URI toURI() {
		return uri;
	}

	public String toString() {
		return "URIID [uri=" + uri + "]";
	}

}
