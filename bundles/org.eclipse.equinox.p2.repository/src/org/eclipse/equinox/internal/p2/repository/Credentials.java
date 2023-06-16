/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 * Contributors:
 * 	IBM Corporation - Initial API and implementation
 *  Cloudsmith Inc - Implementation
 *  Sonatype Inc - Ongoing development
 *  Red Hat Inc. - Bug 460967
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.repository;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.repository.helpers.DebugHelper;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.repository.IRepository;
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

	/**
	 * Cache of auth information that is not persisted, and modified auth info.
	 */
	private static final Map<String, UIServices.AuthenticationInfo> savedAuthInfo = Collections.synchronizedMap(new HashMap<String, UIServices.AuthenticationInfo>());

	/**
	 * Information about retry counts, and prompts canceled by user. The SoftReference is
	 * a Map if not null. The keys are also used as serialization per host.
	 */
	private static Map<String, HostEntry> remembered;

	/**
	 * Serializes pop up of login/password prompt
	 */
	private static final Object promptLock = new Object();

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
	 * @throws LoginCanceledException when the user cancels the login prompt
	 * @throws CoreException if the password cannot be read or saved
	 * @return The authentication info.
	 */
	public static UIServices.AuthenticationInfo forLocation(URI location, boolean prompt) throws LoginCanceledException, CoreException {
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
	 * @throws LoginCanceledException - user canceled the prompt for name/password
	 * @throws CoreException if there is an error
	 */
	public static UIServices.AuthenticationInfo forLocation(URI location, boolean prompt, UIServices.AuthenticationInfo lastUsed) throws LoginCanceledException, CoreException {
		String host = uriToHost(location);
		String nodeKey = URLEncoder.encode(host, StandardCharsets.UTF_8);
		if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
			DebugHelper.debug("Credentials", "forLocation:ENTER", // //$NON-NLS-1$ //$NON-NLS-2$
					new Object[] {"host", location, "prompt", Boolean.toString(prompt)}); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// Must serialize getting stored permissions per host as the location may
		// be prompted right now
		// Start by getting a key to lock on
		HostEntry hostLock = null;
		synchronized (Credentials.class) {
			Map<String, HostEntry> r = getRemembered();
			hostLock = r.get(host);
			if (hostLock == null) {
				hostLock = new HostEntry(0);
				r.put(host, hostLock);
			}
		}
		UIServices.AuthenticationInfo loginDetails = null;
		ISecurePreferences securePreferences = null;
		// synchronize getting secure store with prompting user, as it may prompt.
		synchronized (promptLock) {
			securePreferences = SecurePreferencesFactory.getDefault();
		}

		// serialize the prompting per host
		synchronized (hostLock) {
			try {
				if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
					DebugHelper.debug("Credentials", "forLocation:HOSTLOCK OBTAINED", // //$NON-NLS-1$ //$NON-NLS-2$
							new Object[] {"host", location, "prompt", Boolean.toString(prompt)}); //$NON-NLS-1$ //$NON-NLS-2$
				}

				String nodeName = IRepository.PREFERENCE_NODE + '/' + nodeKey;
				ISecurePreferences prefNode = null;
				try {
					if (securePreferences.nodeExists(nodeName))
						prefNode = securePreferences.node(nodeName);
				} catch (IllegalArgumentException e) {
					// if the node name is illegal/malformed (should not happen).
					throw internalError(e);
				} catch (IllegalStateException e) {
					// thrown if preference store has been tampered with
					throw internalError(e);
				}
				if (!prompt) {
					try {
						if (prefNode != null) {
							String username = prefNode.get(IRepository.PROP_USERNAME, null);
							String password = prefNode.get(IRepository.PROP_PASSWORD, null);
							if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
								if (username != null && password != null) {
									DebugHelper.debug("Credentials", "forLocation:PREFNODE FOUND - USING STORED INFO", // //$NON-NLS-1$ //$NON-NLS-2$
											new Object[] {"host", location, "prompt", Boolean.toString(prompt)}); //$NON-NLS-1$ //$NON-NLS-2$
								}
							}

							// if we don't have stored connection data just return a null auth info
							if (username != null && password != null)
								return new UIServices.AuthenticationInfo(username, password, true);
						}
						if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
							DebugHelper.debug("Credentials", "forLocation:PREFNODE NOT FOUND - RETURN FROM MEMORY", // //$NON-NLS-1$ //$NON-NLS-2$
									new Object[] {"host", location, "prompt", Boolean.toString(prompt)}); //$NON-NLS-1$ //$NON-NLS-2$
						}
						return restoreFromMemory(nodeName);
					} catch (StorageException e) {
						throw internalError(e);
					}
				}
				// need to prompt user for user name and password
				// first check (throw exception) if having a remembered cancel
				checkRememberedCancel(host);

				// check if another thread has modified the credentials since last attempt
				// made by current thread - if so, try with latest without prompting
				if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
					UIServices.AuthenticationInfo latest = restoreFromMemory(nodeName);
					boolean useLatest = false;
					if (latest != null && lastUsed != null)
						if (!(latest.getUserName().equals(lastUsed.getUserName()) && latest.getPassword().equals(lastUsed.getPassword())))
							useLatest = true;
					if (useLatest)
						DebugHelper.debug("Credentials", "forLocation:LATER INFO AVAILABLE - RETURNING IT", // //$NON-NLS-1$ //$NON-NLS-2$
								new Object[] {"host", location, "prompt", Boolean.toString(prompt)}); //$NON-NLS-1$ //$NON-NLS-2$
				}

				UIServices.AuthenticationInfo latest = restoreFromMemory(nodeName);
				if (latest != null)
					if (lastUsed == null || !(latest.getUserName().equals(lastUsed.getUserName()) && latest.getPassword().equals(lastUsed.getPassword())))
						return latest;

				// check if number of prompts have been exceeded for the host - if so
				// do a synthetic Login canceled by user
				// (The alternative is to return "latest" until retry login gives up with
				// authentication failed - but that would waste time).
				if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
					if (getPromptCount(host) >= RepositoryPreferences.getLoginRetryCount()) {
						if (lastUsed == null && latest == null)
							DebugHelper.debug("Credentials", "forLocation:NO INFO - SYNTHETIC CANCEL", // //$NON-NLS-1$ //$NON-NLS-2$
									new Object[] {"host", location}); //$NON-NLS-1$
						return latest == null ? lastUsed : latest; // keep client failing on the latest known
					}
					DebugHelper.debug("Credentials", "forLocation:LATER INFO AVAILABLE - RETURNING IT", // //$NON-NLS-1$ //$NON-NLS-2$
							new Object[] {"host", location, "prompt", Boolean.toString(prompt)}); //$NON-NLS-1$ //$NON-NLS-2$

				}
				if (getPromptCount(host) >= RepositoryPreferences.getLoginRetryCount()) {
					if (lastUsed == null && latest == null)
						throw new LoginCanceledException();
					return latest == null ? lastUsed : latest; // keep client failing on the latest known
				}
				IProvisioningAgent agent = ServiceHelper.getService(Activator.getContext(), IProvisioningAgent.class);
				UIServices adminUIService = agent.getService(UIServices.class);

				if (adminUIService != null)
					synchronized (promptLock) {
						try {
							if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
								DebugHelper.debug("Credentials", "forLocation:PROMPTLOCK OBTAINED", // //$NON-NLS-1$ //$NON-NLS-2$
										new Object[] {"host", location}); //$NON-NLS-1$
							}

							// serialize the popping of the dialog itself
							loginDetails = lastUsed != null ? adminUIService.getUsernamePassword(host, lastUsed) : adminUIService.getUsernamePassword(host);
							//null result means user canceled password dialog
							if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
								if (loginDetails == UIServices.AUTHENTICATION_PROMPT_CANCELED)
									DebugHelper.debug("Credentials", "forLocation:PROMPTED - USER CANCELED (PROMPT LOCK RELEASED)", // //$NON-NLS-1$ //$NON-NLS-2$
											new Object[] {"host", location}); //$NON-NLS-1$
							}
							if (loginDetails == UIServices.AUTHENTICATION_PROMPT_CANCELED) {
								rememberCancel(host);
								throw new LoginCanceledException();
							} else if (loginDetails == null) {
								throw new LoginCanceledException();
							}
							//save user name and password if requested by user
							if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
								if (loginDetails.saveResult())
									DebugHelper.debug("Credentials", "forLocation:SAVING RESULT", // //$NON-NLS-1$ //$NON-NLS-2$
											new Object[] {"host", location}); //$NON-NLS-1$
							}

							if (loginDetails.saveResult()) {
								if (prefNode == null)
									prefNode = securePreferences.node(nodeName);
								try {
									prefNode.put(IRepository.PROP_USERNAME, loginDetails.getUserName(), true);
									prefNode.put(IRepository.PROP_PASSWORD, loginDetails.getPassword(), true);
									prefNode.flush();
								} catch (StorageException e1) {
									throw internalError(e1);
								} catch (IOException e) {
									throw internalError(e);
								}
							} else {
								// if persisted earlier - the preference should be removed
								if (securePreferences.nodeExists(nodeName)) {
									if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
										DebugHelper.debug("Credentials", "forLocation:REMOVING PREVIOUSLY SAVED INFO", // //$NON-NLS-1$ //$NON-NLS-2$
												new Object[] {"host", location}); //$NON-NLS-1$
									}

									prefNode = securePreferences.node(nodeName);
									prefNode.removeNode();
									try {
										prefNode.flush();
									} catch (IOException e) {
										throw internalError(e);
									}
								}
							}
							saveInMemory(nodeName, loginDetails);
						} finally {
							if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
								DebugHelper.debug("Credentials", "forLocation:PROMPTLOCK RELEASED", // //$NON-NLS-1$ //$NON-NLS-2$
										new Object[] {"host", location}); //$NON-NLS-1$
							}
						}
					}
				incrementPromptCount(host);
			} finally {
				if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
					DebugHelper.debug("Credentials", "forLocation:HOSTLOCK RELEASED", // //$NON-NLS-1$ //$NON-NLS-2$
							new Object[] {"host", location}); //$NON-NLS-1$
				}
			}

		}

		return loginDetails;
	}

	private static String uriToHost(URI location) {
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
				host = IPath.fromOSString(location.toString()).removeLastSegments(1).toString();
			else
				// it is an opaque URI - details are unknown - can only use entire string.
				host = location.toString();
		}
		return host;
	}

	/**
	 * Returns authentication details stored in memory for the given node name,
	 * or <code>null</code> if no information is stored.
	 */
	private static UIServices.AuthenticationInfo restoreFromMemory(String nodeName) {
		return savedAuthInfo.get(nodeName);
	}

	/**
	 * Saves authentication details in memory so user is only prompted once per (SDK) session
	 */
	private static void saveInMemory(String nodeName, UIServices.AuthenticationInfo loginDetails) {
		savedAuthInfo.put(nodeName, loginDetails);
	}

	/**
	 * Remember the fact that the host was canceled.
	 * @param host
	 */
	private static void rememberCancel(String host) {
		Map<String, HostEntry> r = getRemembered();
		if (r != null)
			r.put(host, new HostEntry(-1));
	}

	/**
	 * Throws LoginCancledException if the host was previously canceled, and the information
	 * is not stale.
	 * @param host
	 * @throws LoginCanceledException
	 */
	private static void checkRememberedCancel(String host) throws LoginCanceledException {
		Map<String, HostEntry> r = getRemembered();
		if (r != null) {
			Object x = r.get(host);
			if (x != null && x instanceof HostEntry)
				if (((HostEntry) x).isCanceled()) {
					if (DebugHelper.DEBUG_REPOSITORY_CREDENTIALS) {
						DebugHelper.debug("Credentials", "checkRememberCancel:PREVIOUSLY CANCELED", // //$NON-NLS-1$ //$NON-NLS-2$
								new Object[] {"host", host}); //$NON-NLS-1$
					}

					throw new LoginCanceledException();
				}
		}

	}

	/**
	 * Increments the prompt count for host. If information is stale, the count is restarted
	 * at 1.
	 * @param host
	 */
	private static void incrementPromptCount(String host) {
		Map<String, HostEntry> r = getRemembered();
		if (r != null) {
			HostEntry value = r.get(host);
			if (value == null)
				r.put(host, value = new HostEntry(1));
			else {
				if (value.isStale())
					value.reset();
				value.increment();
			}
		}
	}

	/**
	 * Returns prompt count for host, except if information is stale in which case 0 is returned.
	 * @param host
	 * @return number of time prompt has been performed for a host (or 0 if information is stale)
	 */
	private static int getPromptCount(String host) {
		Map<String, HostEntry> r = getRemembered();
		if (r != null) {
			HostEntry value = r.get(host);
			if (value != null && !value.isStale())
				return value.getCount();
		}
		return 0;

	}

	/**
	 * Clears the cached information about prompts for all login/password and
	 * canceled logins.
	 */
	public static synchronized void clearPromptCache() {
		if (remembered == null)
			return;
		Map<String, HostEntry> r = remembered;
		if (r == null || r.isEmpty())
			return;
		// reset entries rather than creating a new empty map since the entries
		// are also used as locks
		for (HostEntry entry : r.values())
			entry.reset();
	}

	/**
	 * Clears the cached information for location about prompts for login/password and
	 * canceled logins.
	 * @param location the repository location
	 */
	public static synchronized void clearPromptCache(URI location) {
		clearPromptCache(uriToHost(location));
	}

	/**
	 * Clears the cached information for host about prompts for login/password and
	 * canceled logins.
	 * @param host a host as returned from uriToHost for a location
	 */
	public static synchronized void clearPromptCache(String host) {
		if (remembered == null)
			return;
		Map<String, HostEntry> r = remembered;
		if (r == null)
			return;
		HostEntry value = r.get(host);
		if (value != null)
			value.reset();
	}

	private static synchronized Map<String, HostEntry> getRemembered() {
		if (remembered == null)
			remembered = Collections.synchronizedMap(new HashMap<>());
		return remembered;
	}

	private static class HostEntry {
		long timestamp;
		int count;

		public HostEntry(int count) {
			this.count = count;
			this.timestamp = System.currentTimeMillis();
		}

		public boolean isCanceled() {
			return count == -1 && !isStale();
		}

		public boolean isStale() {
			// a record is stale if older than 3 minutes
			return System.currentTimeMillis() - timestamp > 1000 * 60 * 3;
		}

		public int getCount() {
			return count;
		}

		public void increment() {
			if (count != -1)
				count++;
		}

		public void reset() {
			count = 0;
			timestamp = System.currentTimeMillis();
		}
	}

	/**
	 * Get default "InternalError" ProvisionException.
	 * @param t
	 * @return a default "InternalError"
	 */
	public static ProvisionException internalError(Throwable t) {
		return new ProvisionException(new Status(IStatus.ERROR, Activator.ID, //
				ProvisionException.INTERNAL_ERROR, Messages.repoMan_internalError, t));
	}
}
