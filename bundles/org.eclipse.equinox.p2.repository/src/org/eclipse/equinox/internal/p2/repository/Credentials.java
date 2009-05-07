/*******************************************************************************
 * Copyright (c) 2009, IBM Corporation and others.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 * Contributors:
 * 	IBM Corporation - Initial API and implementation
 *  Cloudsmith Inc - Implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.repository;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.filetransfer.UserCancelledException;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI.AuthenticationInfo;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.security.storage.*;

/**
 * Credentials handles AuthenticationInfo that can be used to established an
 * ECF connection context. An AuthenticationInfo is obtained for a URI buy looking
 * in a store, if none is provided the user is optionally prompted for the information. 
 */
public class Credentials {
	public static class LoginCanceledException extends Exception {
		private static final long serialVersionUID = 1L;

	}

	private static final Map savedAuthInfo = Collections.synchronizedMap(new HashMap());

	/**
	 * Returns the AuthenticationInfo for the given URI. This may prompt the
	 * user for user name and password as required.
	 * 
	 * If the URI is opaque, the entire URI is used as the key. For non opaque URIs, 
	 * the key is based on the host name, using a host name of "localhost" if host name is
	 * missing.
	 *
	 * @param location - the file location requiring login details
	 * @param prompt - use <code>true</code> to prompt the user instead of
	 * looking at the secure preference store for login, use <code>false</code>
	 * to only try the secure preference store
	 * @throws UserCancelledException when the user cancels the login prompt
	 * @throws CoreException if the password cannot be read or saved
	 * @return The authentication info.
	 */
	public static AuthenticationInfo forLocation(URI location, boolean prompt) throws LoginCanceledException, CoreException {
		return forLocation(location, prompt, null);
	}

	/**
	 * Returns the AuthenticationInfo for the given URI. This may prompt the
	 * user for user name and password as required.
	 * 
	 * If the URI is opaque, the entire URI is used as the key. For non opaque URIs, 
	 * the key is based on the host name, using a host name of "localhost" if host name is
	 * missing.
	 * 
	 * This method allows passing a previously used AuthenticationInfo. If set, the user interface
	 * may present the information "on file" to the user for editing.
	 * 
	 * @param location - the location for which to obtain authentication information
	 * @param prompt - if true, user will be prompted for information
	 * @param lastUsed - optional information used in an previous attempt to login
	 * @return AuthenticationInfo, or null if there was no information available
	 * @throws UserCancelledException - user canceled the prompt for name/password
	 * @throws CoreException if there is an error
	 */
	public static AuthenticationInfo forLocation(URI location, boolean prompt, AuthenticationInfo lastUsed) throws LoginCanceledException, CoreException {
		ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();

		// if URI is not opaque, just getting the host may be enough
		String host = location.getHost();
		if (host == null) {
			String scheme = location.getScheme();
			if (URIUtil.isFileURI(location) || scheme == null)
				// If the URI references a file, a password could possibly be needed for the directory
				// (it could be a protected zip file representing a compressed directory) - in this
				// case the key is the path without the last segment.
				// Using "Path" this way may result in an empty string - which later will result in
				// an invalid key.
				host = new Path(location.toString()).removeLastSegments(1).toString();
			else
				// it is an opaque URI - details are unknown - can only use entire string.
				host = location.toString();
		}
		String nodeKey;
		try {
			nodeKey = URLEncoder.encode(host, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e2) {
			// fall back to default platform encoding
			try {
				// Uses getProperty "file.encoding" instead of using deprecated URLEncoder.encode(String location) 
				// which does the same, but throws NPE on missing property.
				String enc = System.getProperty("file.encoding");//$NON-NLS-1$
				if (enc == null)
					throw new UnsupportedEncodingException("No UTF-8 encoding and missing system property: file.encoding"); //$NON-NLS-1$
				nodeKey = URLEncoder.encode(host, enc);
			} catch (UnsupportedEncodingException e) {
				throw RepositoryStatusHelper.internalError(e);
			}
		}
		String nodeName = IRepository.PREFERENCE_NODE + '/' + nodeKey;
		ISecurePreferences prefNode = null;
		try {
			if (securePreferences.nodeExists(nodeName))
				prefNode = securePreferences.node(nodeName);
		} catch (IllegalArgumentException e) {
			// if the node name is illegal/malformed (should not happen).
			throw RepositoryStatusHelper.internalError(e);
		} catch (IllegalStateException e) {
			// thrown if preference store has been tampered with
			throw RepositoryStatusHelper.internalError(e);
		}
		if (!prompt) {
			try {
				if (prefNode != null) {
					String username = prefNode.get(IRepository.PROP_USERNAME, null);
					String password = prefNode.get(IRepository.PROP_PASSWORD, null);
					// if we don't have stored connection data just return a null auth info
					if (username != null && password != null)
						return new IServiceUI.AuthenticationInfo(username, password, true);
				}
				return restoreFromMemory(nodeName);
			} catch (StorageException e) {
				throw RepositoryStatusHelper.internalError(e);
			}
		}
		//need to prompt user for user name and password
		IServiceUI adminUIService = (IServiceUI) ServiceHelper.getService(Activator.getContext(), IServiceUI.class.getName());
		AuthenticationInfo loginDetails = null;
		if (adminUIService != null)
			loginDetails = lastUsed != null ? adminUIService.getUsernamePassword(host, lastUsed) : adminUIService.getUsernamePassword(host);
		//null result means user canceled password dialog
		if (loginDetails == null)
			throw new LoginCanceledException();
		//save user name and password if requested by user
		if (loginDetails.saveResult()) {
			if (prefNode == null)
				prefNode = securePreferences.node(nodeName);
			try {
				prefNode.put(IRepository.PROP_USERNAME, loginDetails.getUserName(), true);
				prefNode.put(IRepository.PROP_PASSWORD, loginDetails.getPassword(), true);
				prefNode.flush();
			} catch (StorageException e1) {
				throw RepositoryStatusHelper.internalError(e1);
			} catch (IOException e) {
				throw RepositoryStatusHelper.internalError(e);
			}
		} else {
			// if persisted earlier - the preference should be removed
			if (securePreferences.nodeExists(nodeName)) {
				prefNode = securePreferences.node(nodeName);
				prefNode.removeNode();
				try {
					prefNode.flush();
				} catch (IOException e) {
					throw RepositoryStatusHelper.internalError(e);
				}
			}
			saveInMemory(nodeName, loginDetails);
		}
		return loginDetails;
	}

	/**
	 * Returns authentication details stored in memory for the given node name,
	 * or <code>null</code> if no information is stored.
	 */
	private static AuthenticationInfo restoreFromMemory(String nodeName) {
		return (AuthenticationInfo) savedAuthInfo.get(nodeName);
	}

	/**
	 * Saves authentication details in memory so user is only prompted once per session
	 */
	private static void saveInMemory(String nodeName, AuthenticationInfo loginDetails) {
		savedAuthInfo.put(nodeName, loginDetails);
	}

}
