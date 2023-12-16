/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - ongoing implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository.helpers;

import java.io.File;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.osgi.util.NLS;

public class RepositoryHelper {

	private static final Pattern WORKSPACE_KEY_PATTERN = Pattern.compile(
			"\\s*<key href=\"(file:/.*)\\.metadata/\\.plugins/org\\.eclipse\\.oomph\\.setup/workspace\\.setup#/\"/>"); //$NON-NLS-1$

	protected static final String FILE_SCHEME = "file"; //$NON-NLS-1$

	/**
	 * If the provided URI can be interpreted as representing a local address (no
	 * schema, or one letter schema) but is missing the file schema, a new URI is
	 * created which represents the local file.
	 *
	 * @param location the URI to convert
	 * @return the converted URI, or the original
	 */
	public static URI localRepoURIHelper(URI location) {
		if (location == null)
			return null;
		if (location.getScheme() == null) // Probably a local path: /home/user/repo

			location = (new File(location.getPath())).getAbsoluteFile().toURI();
		else if (location.getScheme().length() == 1)
			// Probably a windows path: C:\repo
			location = (new File(URIUtil.toUnencodedString(location))).toURI();
		else if (!FILE_SCHEME.equalsIgnoreCase(location.getScheme()))
			// This else must occur last!
			return location;

		// Zipped repository?
		String lowerCase = location.toString().toLowerCase();
		if (lowerCase.endsWith(".jar") || lowerCase.endsWith(".zip")) //$NON-NLS-1$//$NON-NLS-2$
			return URIUtil.toJarURI(location, null);
		return location;
	}

	/**
	 * Determine if the repository could be used as a valid destination (eg, it is
	 * modifiable)
	 *
	 * @param repository the repository to test
	 * @return the repository
	 */
	public static <T> IRepository<T> validDestinationRepository(IRepository<T> repository) {
		if (!repository.isModifiable())
			throw new IllegalStateException(NLS.bind(Messages.DestinationNotModifiable, repository.getLocation()));
		return repository;
	}

	/**
	 * Determine if the location is a syntactically correct repository location.
	 * Intended to be used from the UI when checking validity of user input.
	 *
	 * @throws IllegalArgumentException if location is null
	 */
	public static IStatus checkRepositoryLocationSyntax(URI location) {
		if (location == null)
			throw new IllegalArgumentException("Location cannot be null"); //$NON-NLS-1$
		if (!location.isAbsolute())
			return new Status(IStatus.ERROR, Activator.ID, Messages.locationMustBeAbsolute);
		String scheme = location.getScheme();
		if (scheme == null) {
			return Status.error(Messages.schemeNotProvided);
		}
		try {
			new URL(scheme, "dummy.com", -1, "dummy.txt"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (MalformedURLException e) {
			// check if the scheme is probably provided to ECF but not yet loaded
			IExtensionRegistry reg = RegistryFactory.getRegistry();
			if (reg != null) {
				IExtensionPoint handlersExtensionPoint = reg
						.getExtensionPoint("org.eclipse.ecf.filetransfer.urlStreamHandlerService"); //$NON-NLS-1$
				IConfigurationElement[] configurationElements = handlersExtensionPoint.getConfigurationElements();
				for (IConfigurationElement configurationElement : configurationElements) {
					String protocol = configurationElement.getAttribute("protocol"); //$NON-NLS-1$
					if (Objects.equals(scheme, protocol)) {
						return Status.OK_STATUS;
					}
				}
			}
			return new Status(IStatus.ERROR, Activator.ID, Messages.schemeNotSupported, e);
		}
		return Status.OK_STATUS;
	}

	/**
	 * Create a Composite repository in memory.
	 *
	 * @return the repository or null if unable to create one
	 */
	public static <T> IRepository<T> createMemoryComposite(IProvisioningAgent agent,
			Class<? extends IRepositoryManager<T>> repoManagerClass, String repositoryType) {
		if (agent == null) {
			return null;
		}
		IRepositoryManager<T> manager = agent.getService(repoManagerClass);
		if (manager == null) {
			return null;
		}
		synchronized (manager) {
			// one must synchronize here because even if we check that the manager does not
			// contain a repository, it is still possible that two threads perform the
			// check at the same time! This will result in both threads see "not exits",
			// then both try to create the repos and one of them will fail
			URI repositoryURI;
			do {
				try {
					repositoryURI = new URI("memory:" + UUID.randomUUID()); //$NON-NLS-1$
				} catch (URISyntaxException e) {
					// if we can't create the URI we are all lost...
					return null;
				}
				// very unlikely but check if the manager already has this one and the generate
				// a fresh one...
			} while (manager.contains(repositoryURI));

			try {
				IRepository<T> result = manager.createRepository(repositoryURI, repositoryURI.toString(),
						repositoryType, null);
				manager.removeRepository(repositoryURI);
				return result;
			} catch (ProvisionException e) {
				LogHelper.log(e);
			}
			return null;
		}
	}

	/**
	 * Returns an unmodifiable list of global shared bundle pools, e.g., as used by
	 * the Eclipse Installer.
	 *
	 * @return an unmodifiable list of global shared bundle pools.
	 */
	@SuppressWarnings("nls")
	public static List<Path> getSharedBundlePools() {
		Path agentsInfoLocation = getAgentManagerLocation().resolve("agents.info");
		if (Files.isRegularFile(agentsInfoLocation)) {
			Charset encoding = getSharedAgentEncoding();
			try (Stream<String> agentLocations = Files.lines(agentsInfoLocation, encoding)) {
				List<Path> result = new ArrayList<>();
				agentLocations.map(agentLocation -> Path.of(agentLocation, "pools.info")).filter(Files::isRegularFile)
						.forEach(poolsInfoLocation -> {
							try (Stream<String> poolLocations = Files.lines(poolsInfoLocation, encoding)) {
								poolLocations.map(Path::of)
										.filter(poolLocation -> Files.exists(poolLocation.resolve("artifacts.xml")))
										.forEach(result::add);
							} catch (Exception ex) {
								LogHelper.log(Status.warning("The bundle pools load failed: " + poolsInfoLocation, ex));
							}
						});
				return result;
			} catch (Exception ex) {
				LogHelper.log(Status.warning("The agents load failed: " + agentsInfoLocation, ex));
			}
		}
		return List.of();
	}

	/**
	 * Returns an unmodifiable list of workspace bundle pools, e.g., as used by PDE.
	 *
	 * @return an unmodifiable list of workspace bundle pools.
	 */
	@SuppressWarnings("nls")
	public static List<Path> getWorkspaceBundlePools() {
		Path locations = Path.of(System.getProperty("user.home"),
				".eclipse/org.eclipse.oomph.setup/setups/locations.setup");
		if (Files.isRegularFile(locations)) {
			try (Stream<String> allLocations = Files.lines(locations)) {
				return allLocations.map(location -> {
					Matcher matcher = WORKSPACE_KEY_PATTERN.matcher(location);
					if (matcher.matches()) {
						try {
							return Path.of(URI.create(matcher.group(1)));
						} catch (Exception ex) {
							//$FALL-THROUGH$
						}
					}
					return null;
				}).filter(Objects::nonNull).distinct()
						.map(workspace -> workspace.resolve(".metadata/.plugins/org.eclipse.pde.core/.bundle_pool"))
						.filter(pool -> Files.isRegularFile(pool.resolve("artifacts.xml"))).toList();
			} catch (Exception ex) {
				LogHelper.log(Status.warning("The locations load failed: " + locations, ex));
			}
		}
		return List.of();
	}

	@SuppressWarnings("nls")
	private static Path getAgentManagerLocation() {
		return Path.of(System.getProperty("user.home"), ".eclipse/org.eclipse.oomph.p2");
	}

	@SuppressWarnings("nls")
	private static Charset getSharedAgentEncoding() {
		Path encodingInfo = getAgentManagerLocation().resolve("encoding.info");
		if (Files.isRegularFile(encodingInfo)) {
			try {
				return Charset.forName(Files.readString(encodingInfo).trim());
			} catch (Exception e) {
				//$FALL-THROUGH$
			}
		}
		return Charset.defaultCharset();
	}

}