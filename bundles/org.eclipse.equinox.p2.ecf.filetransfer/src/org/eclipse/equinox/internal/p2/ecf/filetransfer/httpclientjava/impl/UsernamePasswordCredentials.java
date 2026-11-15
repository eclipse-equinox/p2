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

public class UsernamePasswordCredentials extends Credentials {

	private final class UserPrincipal implements Principal {
		private final String username;

		private UserPrincipal(String username) {
			this.username = username;
		}

		@Override
		public String getName() {
			return username;
		}
	}

	public UsernamePasswordCredentials(String username, char[] password) {
		this.principal = new UserPrincipal(username);
		this.password = password;
	}

}
