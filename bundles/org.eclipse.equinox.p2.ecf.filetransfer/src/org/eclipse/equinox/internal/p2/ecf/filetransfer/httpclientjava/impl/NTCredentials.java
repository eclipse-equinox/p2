/****************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.internal.provider.filetransfer.httpclientjava;

import java.security.Principal;

public class NTCredentials extends Credentials {

	private String workstation;
	private String domain;

	private final class NTLMUserPrincipal implements Principal {
		private final String un;

		private NTLMUserPrincipal(String un) {
			this.un = un;
		}

		@Override
		public String getName() {
			return un;
		}
	}

	public NTCredentials(String un, char[] password, String workstation, String domain) {
		this.workstation = workstation;
		this.domain = domain;
		this.principal = new NTLMUserPrincipal(un);
		this.password = password;

	}

	public String getDomain() {
		return domain;
	}

	public String getWorkstation() {
		return workstation;
	}

}
