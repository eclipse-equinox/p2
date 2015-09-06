/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * 	IBM Corporation - initial API and implementation
 * 	Band XI - add more commands
 *		Composent, Inc. - command additions
 *      SAP - bug fixes
 *      Red Hat Inc. - Bug 460967
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.console;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

/**
 * An OSGi console command provider that adds various commands for interacting
 * with the provisioning system.
 */
public class ProvCommandProvider implements CommandProvider {
	private static final String WILDCARD_ANY = "*"; //$NON-NLS-1$
	public static final String NEW_LINE = "\r\n"; //$NON-NLS-1$
	public static final String TAB = "\t"; //$NON-NLS-1$

	// holds the mapping between command name and command description
	private Map<String, String> commandsHelp = null;
	// hold the mappings between command groups and the command names of the commands in the group
	private Map<String, String[]> commandGroups = null;

	private final IProvisioningAgent agent;

	//	private Profile profile;

	public ProvCommandProvider(String profileId, IProvisioningAgent agent) {
		this.agent = agent;
		// look up the profile we are currently running and use it as the
		// default.
		// TODO define a way to spec the default profile to manage
		//		if (profileId != null) {
		//			profile = registry.getProfile(profileId);
		//			if (profile != null)
		//				return;
		//		}
		//		// A default was not defined so manage the first profile we can find
		//		Profile[] profiles = registry.getProfiles();
		//		if (profiles.length > 0)
		//			profile = profiles[0];
	}

	/**
	 * Adds both a metadata repository and artifact repository
	 */
	public void _provaddrepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		// add metadata repo
		if (ProvisioningHelper.addMetadataRepository(agent, repoURI) == null) {
			interpreter.println("Unable to add metadata repository: " + repoURI);
		} else // add artifact repo at same URL
			if (ProvisioningHelper.addArtifactRepository(agent, repoURI) == null) {
			interpreter.println("Unable to add artifact repository: " + repoURI);
		}
	}

	public void _provdelrepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		ProvisioningHelper.removeMetadataRepository(agent, repoURI);
		ProvisioningHelper.removeArtifactRepository(agent, repoURI);
	}

	/**
	 * Adds a metadata repository.
	 */
	public void _provaddmetadatarepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		if (ProvisioningHelper.addMetadataRepository(agent, repoURI) == null)
			interpreter.println("Unable to add repository: " + repoURI);
	}

	public void _provdelmetadatarepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		ProvisioningHelper.removeMetadataRepository(agent, repoURI);
	}

	public void _provaddartifactrepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		if (ProvisioningHelper.addArtifactRepository(agent, repoURI) == null)
			interpreter.println("Unable to add repository " + repoURI);
	}

	public void _provdelartifactrepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		ProvisioningHelper.removeArtifactRepository(agent, repoURI);
	}

	/**
	 * Install a given IU to a given profile location.
	 */
	public void _provinstall(CommandInterpreter interpreter) {
		String iu = interpreter.nextArgument();
		String version = interpreter.nextArgument();
		String profileId = interpreter.nextArgument();
		if (profileId == null || profileId.equals("this")) //$NON-NLS-1$
			profileId = IProfileRegistry.SELF;
		if (iu == null || version == null || profileId == null) {
			interpreter.println("Installable unit id, version, and profileid must be provided");
			return;
		}
		IProfile profile = ProvisioningHelper.getProfile(agent, profileId);
		if (profile == null) {
			if (profileId.equals(IProfileRegistry.SELF)) {
				interpreter.println("No profile found");
			} else {
				interpreter.println("Profile " + profileId + " not found");
			}
			return;
		}
		IStatus s = null;
		try {
			s = ProvisioningHelper.install(agent, iu, version, profile, new NullProgressMonitor());
		} catch (ProvisionException e) {
			interpreter.println("Installation failed with ProvisionException for " + iu + " " + version);
			interpreter.printStackTrace(e);
			return;
		}
		if (s.isOK())
			interpreter.println("Installation complete for " + iu + " " + version);
		else {
			interpreter.println("Installation failed for " + iu + " " + version);
			interpreter.println(flattenStatus(s.getChildren(), "  "));
		}
	}

	private String flattenStatus(IStatus[] childs, String indent) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; (childs != null) && (i < childs.length); i++) {
			sb.append(indent).append(childs[i].getMessage()).append(NEW_LINE);
			sb.append(flattenStatus(childs[i].getChildren(), indent + "  "));
		}
		return sb.toString();
	}

	/**
	 * Creates a profile given an id, location, and flavor
	 */
	public void _provaddprofile(CommandInterpreter interpreter) {
		String profileId = interpreter.nextArgument();
		String location = interpreter.nextArgument();
		if (profileId == null || location == null) {
			interpreter.println("Id and location must be provided");
			return;
		}
		String environments = interpreter.nextArgument();
		Map<String, String> props = new HashMap<String, String>();
		props.put(IProfile.PROP_INSTALL_FOLDER, location);
		if (environments != null)
			props.put(IProfile.PROP_ENVIRONMENTS, environments);

		try {
			ProvisioningHelper.addProfile(agent, profileId, props);
		} catch (ProvisionException e) {
			interpreter.println("Add profile failed.  " + e.getMessage());
			interpreter.printStackTrace(e);
		}
	}

	/**
	 * Deletes a profile given an id, location, and flavor
	 */
	public void _provdelprofile(CommandInterpreter interpreter) {
		String profileId = interpreter.nextArgument();
		if (profileId == null) {
			interpreter.println("profileid must be provided");
			return;
		}
		ProvisioningHelper.removeProfile(agent, profileId);
	}

	/**
	 * Lists the installable units that match the given URL, id, and/or version.
	 * 
	 * @param interpreter
	 */
	public void _provliu(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		String id = processArgument(interpreter.nextArgument());
		String version = processArgument(interpreter.nextArgument());
		URI repoURL = null;
		if (urlString != null && !urlString.equals(WILDCARD_ANY))
			repoURL = toURI(interpreter, urlString);
		IInstallableUnit[] units = sort(ProvisioningHelper.getInstallableUnits(agent, repoURL, QueryUtil.createIUQuery(id, new VersionRange(version)), null));
		for (int i = 0; i < units.length; i++)
			println(interpreter, units[i]);
	}

	/**
	 * Lists the installable units that match the given URL and query. A third
	 * boolean argument can be provided where <code>true</code> means &quot;full query&quot;
	 * and <code>false</code> means &quote;match query&quote;. The default is <code>false</code>.
	 * 
	 * @param interpreter
	 */
	public void _provlquery(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		String expression = processArgument(interpreter.nextArgument());
		if (expression == null) {
			interpreter.println("Please enter a query");
			return;
		}
		boolean useFull = Boolean.parseBoolean(processArgument(interpreter.nextArgument()));
		URI repoURL = null;
		if (urlString != null && !urlString.equals(WILDCARD_ANY))
			repoURL = toURI(interpreter, urlString);

		IQuery<IInstallableUnit> query = useFull ? QueryUtil.createQuery(expression) : QueryUtil.createMatchQuery(expression);
		IInstallableUnit[] units = sort(ProvisioningHelper.getInstallableUnits(agent, repoURL, query, null));
		// Now print out results
		if (units.length == 0)
			interpreter.println("No units found");
		else {
			for (int i = 0; i < units.length; i++)
				println(interpreter, units[i]);
		}
	}

	/**
	 * Lists the known metadata repositories, or the contents of a given
	 * metadata repository.
	 * 
	 * @param interpreter
	 */
	public void _provlr(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		String id = processArgument(interpreter.nextArgument());
		String version = processArgument(interpreter.nextArgument());
		if (urlString == null) {
			URI[] repositories = ProvisioningHelper.getMetadataRepositories(agent);
			if (repositories != null)
				for (int i = 0; i < repositories.length; i++)
					interpreter.println(repositories[i]);
			return;
		}
		URI repoLocation = toURI(interpreter, urlString);
		if (repoLocation == null)
			return;
		IInstallableUnit[] units = sort(ProvisioningHelper.getInstallableUnits(agent, repoLocation, QueryUtil.createIUQuery(id, new VersionRange(version)), null));
		for (int i = 0; i < units.length; i++)
			println(interpreter, units[i]);
	}

	/**
	 * Lists the group IUs in all known metadata repositories, or in the given
	 * metadata repository.
	 * 
	 * @param interpreter
	 */
	public void _provlg(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		IQueryable<IInstallableUnit> queryable = null;
		if (urlString == null) {
			queryable = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			if (queryable == null)
				return;
		} else {
			URI repoURL = toURI(interpreter, urlString);
			if (repoURL == null)
				return;
			queryable = ProvisioningHelper.getMetadataRepository(agent, repoURL);
			if (queryable == null)
				return;
		}
		IInstallableUnit[] units = sort(queryable.query(QueryUtil.createIUGroupQuery(), null));
		for (int i = 0; i < units.length; i++)
			println(interpreter, units[i]);
	}

	/**
	 * Lists the known artifact repositories, or the contents of a given
	 * artifact repository.
	 * 
	 * @param interpreter
	 */
	public void _provlar(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		if (urlString == null) {
			URI[] repositories = ProvisioningHelper.getArtifactRepositories(agent);
			if (repositories == null)
				return;
			for (int i = 0; i < repositories.length; i++)
				interpreter.println(repositories[i]);
			return;
		}
		URI repoURL = toURI(interpreter, urlString);
		if (repoURL == null)
			return;
		IArtifactRepository repo = ProvisioningHelper.getArtifactRepository(agent, repoURL);
		IQueryResult<IArtifactKey> keys = null;
		try {
			keys = (repo != null) ? repo.query(ArtifactKeyQuery.ALL_KEYS, null) : null;
		} catch (UnsupportedOperationException e) {
			interpreter.println("Repository does not support queries.");
			return;
		}
		if (keys == null || keys.isEmpty()) {
			interpreter.println("Repository has no artifacts");
			return;
		}
		IFileArtifactRepository fileRepo = repo.getAdapter(IFileArtifactRepository.class);
		for (Iterator<IArtifactKey> iterator = keys.iterator(); iterator.hasNext();) {
			IArtifactKey key = iterator.next();
			IArtifactDescriptor[] descriptors = repo.getArtifactDescriptors(key);
			for (int j = 0; j < descriptors.length; j++) {
				IArtifactDescriptor descriptor = descriptors[j];
				File location = null;
				if (fileRepo != null)
					location = fileRepo.getArtifactFile(descriptor);
				println(interpreter, key, location);
			}

		}
	}

	/**
	 * Returns the given string as an URL, or <code>null</code> if the string
	 * could not be interpreted as an URL.
	 */
	private URI toURI(CommandInterpreter interpreter, String urlString) {
		try {
			return new URI(urlString);
		} catch (URISyntaxException e) {
			interpreter.print(e.getMessage());
			interpreter.println();
			return null;
		}
	}

	private String processArgument(String arg) {
		if (arg == null || arg.equals(WILDCARD_ANY))
			return null;
		return arg;
	}

	/**
	 * Lists the known profiles, or the contents of a given profile.
	 * 
	 * @param interpreter
	 */
	public void _provlp(CommandInterpreter interpreter) {
		String profileId = processArgument(interpreter.nextArgument());
		String id = processArgument(interpreter.nextArgument());
		String range = processArgument(interpreter.nextArgument());
		if (profileId == null) {
			IProfile[] profiles = ProvisioningHelper.getProfiles(agent);
			if (profiles == null) {
				interpreter.println("No profile found");
				return;
			}
			for (int i = 0; i < profiles.length; i++)
				interpreter.println(profiles[i].getProfileId());
			return;
		}
		// determine which profile is to be listed
		IProfile target = null;
		if (profileId.equals("this")) //$NON-NLS-1$
			profileId = IProfileRegistry.SELF;
		target = ProvisioningHelper.getProfile(agent, profileId);
		if (target == null) {
			if (profileId.equals(IProfileRegistry.SELF)) {
				interpreter.println("No profile found");
			} else {
				interpreter.println("Profile " + profileId + " not found");
			}
			return;
		}

		// list the profile contents
		IInstallableUnit[] result = sort(target.query(QueryUtil.createIUQuery(id, new VersionRange(range)), null));
		for (int i = 0; i < result.length; i++)
			interpreter.println(result[i]);
	}

	/**
	 * Lists the profile timestamps for a given profile id, if no profile id, the default profile
	 * is used.
	 * 
	 * @param interpreter
	 */
	public void _provlpts(CommandInterpreter interpreter) {
		String profileId = processArgument(interpreter.nextArgument());
		if (profileId == null || profileId.equals("this")) { //$NON-NLS-1$
			profileId = IProfileRegistry.SELF;
		}
		long[] profileTimestamps = ProvisioningHelper.getProfileTimestamps(agent, profileId);
		// if no profile timestamps for given id, print that out and done
		if (profileTimestamps == null || profileTimestamps.length == 0) {
			if (profileId.equals(IProfileRegistry.SELF)) {
				interpreter.println("No profile found");
			} else {
				interpreter.print("No timestamps found for profile ");
				interpreter.println(profileId);
			}
			return;
		}
		// else if there are some timestamps then print them out on separate line
		interpreter.print("Timestamps for profile ");
		interpreter.println(profileId);
		for (int i = 0; i < profileTimestamps.length; i++) {
			interpreter.print("\t"); //$NON-NLS-1$
			interpreter.println(new Long(profileTimestamps[i]));
		}
	}

	/**
	 * Revert a profile to a given timestamp
	 */
	public void _provrevert(CommandInterpreter interpreter) {
		String timestamp = interpreter.nextArgument();
		if (timestamp == null) {
			interpreter.println("Valid timestamp must be provided.  Timestamps can be retrieved via 'provlpts' command.");
			return;
		}
		Long ts = null;
		try {
			ts = new Long(timestamp);
		} catch (NumberFormatException e) {
			interpreter.println("Timestamp " + timestamp + " not valid.  Timestamps can be retrieved via 'provlpts' command.");
			return;
		}
		String profileId = interpreter.nextArgument();
		if (profileId == null || profileId.equals("this"))
			profileId = IProfileRegistry.SELF;

		IProfile profile = ProvisioningHelper.getProfile(agent, profileId);
		if (profile == null) {
			if (profileId.equals(IProfileRegistry.SELF)) {
				interpreter.println("No profile found");
			} else {
				interpreter.println("Profile " + profileId + " not found");
			}
			return;
		}
		IStatus s = null;
		try {
			s = ProvisioningHelper.revertToPreviousState(agent, profile, ts.longValue());
		} catch (ProvisionException e) {
			interpreter.println("revert failed ");
			interpreter.printStackTrace(e);
			return;
		}
		if (s.isOK())
			interpreter.println("revert completed");
		else {
			interpreter.println("revert failed ");
			printErrorStatus(interpreter, s);
		}
	}

	private IInstallableUnit[] sort(IQueryResult<IInstallableUnit> queryResult) {
		IInstallableUnit[] units = queryResult.toArray(IInstallableUnit.class);
		Arrays.sort(units, new Comparator<IInstallableUnit>() {
			public int compare(IInstallableUnit arg0, IInstallableUnit arg1) {
				return arg0.toString().compareTo(arg1.toString());
			}
		});
		return units;
	}

	public void _provlgp(CommandInterpreter interpreter) {
		String profileId = processArgument(interpreter.nextArgument());
		if (profileId == null || profileId.equals("this")) {
			profileId = IProfileRegistry.SELF;
		}
		IProfile profile = ProvisioningHelper.getProfile(agent, profileId);
		if (profile == null) {
			if (profileId.equals(IProfileRegistry.SELF)) {
				interpreter.println("No profile found");
			} else {
				interpreter.println("Profile " + profileId + " not found");
			}
			return;
		}
		IInstallableUnit[] units = sort(profile.query(QueryUtil.createIUGroupQuery(), new NullProgressMonitor()));
		// Now print out results
		for (int i = 0; i < units.length; i++)
			println(interpreter, units[i]);
	}

	/**
	 * Lists the installable units that match the given profile id and query. The id can be
	 * &quot;this&quot; to denote the self profile. A third boolean argument can be provided
	 * where <code>true</code> means &quot;full query&quot; and <code>false</code> means
	 * &quote;match query&quote;. The default is <code>false</code>.
	 * 
	 * @param interpreter
	 */
	public void _provlpquery(CommandInterpreter interpreter) {
		String profileId = processArgument(interpreter.nextArgument());
		if (profileId == null || profileId.equals("this")) {
			profileId = IProfileRegistry.SELF;
		}

		String expression = processArgument(interpreter.nextArgument());
		if (expression == null) {
			interpreter.println("Please enter a query");
			return;
		}

		boolean useFull = Boolean.parseBoolean(processArgument(interpreter.nextArgument()));
		IQuery<IInstallableUnit> query = useFull ? QueryUtil.createQuery(expression) : QueryUtil.createMatchQuery(expression);

		IProfile profile = ProvisioningHelper.getProfile(agent, profileId);
		if (profile == null) {
			if (profileId.equals(IProfileRegistry.SELF)) {
				interpreter.println("No profile found");
			} else {
				interpreter.println("Profile " + profileId + " not found");
			}
			return;
		}
		IInstallableUnit[] units = sort(profile.query(query, new NullProgressMonitor()));
		// Now print out results
		if (units.length == 0)
			interpreter.println("No units found");
		else {
			for (int i = 0; i < units.length; i++)
				println(interpreter, units[i]);
		}
	}

	public void _provremove(CommandInterpreter interpreter) {
		String iu = interpreter.nextArgument();
		String version = interpreter.nextArgument();
		String profileId = interpreter.nextArgument();
		if (profileId == null || profileId.equals("this"))
			profileId = IProfileRegistry.SELF;
		if (version == null) {
			version = Version.emptyVersion.toString();
		}
		if (iu == null) {
			interpreter.println("Installable unit id must be provided");
			return;
		}
		IProfile profile = ProvisioningHelper.getProfile(agent, profileId);
		if (profile == null) {
			if (profileId.equals(IProfileRegistry.SELF)) {
				interpreter.println("No profile found");
			} else {
				interpreter.println("Profile " + profileId + " not found");
			}
			return;
		}
		IStatus s = null;
		try {
			s = ProvisioningHelper.uninstall(agent, iu, version, profile, new NullProgressMonitor());
		} catch (ProvisionException e) {
			interpreter.println("Remove failed with ProvisionException for " + iu + " " + version);
			interpreter.printStackTrace(e);
			return;
		}
		if (s.isOK())
			interpreter.println("Remove complete for " + iu + " " + version);
		else {
			interpreter.println("Remove failed for " + iu + " " + version);
			printErrorStatus(interpreter, s);
		}
	}

	/**
	 * Handles the help command
	 * 
	 * @param intp
	 * @return description for a particular command or false if there is no command with the specified name
	 */
	public Object _help(CommandInterpreter intp) {
		String commandName = intp.nextArgument();
		if (commandName == null) {
			return false;
		}
		String help = getHelp(commandName);

		return help.length() > 0 ? help : false;
	}

	private void printErrorStatus(CommandInterpreter interpreter, IStatus status) {
		interpreter.print("--Error status ");
		interpreter.print("message=" + status.getMessage());
		interpreter.print(",code=" + status.getCode());
		String severityString = null;
		switch (status.getSeverity()) {
			case IStatus.INFO :
				severityString = "INFO";
				break;
			case IStatus.CANCEL :
				severityString = "CANCEL";
				break;
			case IStatus.WARNING :
				severityString = "WARNING";
				break;
			case IStatus.ERROR :
				severityString = "ERROR";
				break;
		}
		interpreter.print(",severity=" + severityString);
		interpreter.print(",bundle=" + status.getPlugin());
		interpreter.println("--");
		Throwable t = status.getException();
		if (t != null)
			interpreter.printStackTrace(t);
		IStatus[] children = status.getChildren();
		if (children != null && children.length > 0) {
			interpreter.println("Error status children:");
			for (int i = 0; i < children.length; i++) {
				printErrorStatus(interpreter, children[i]);
			}
		}
		interpreter.println("--End Error Status--");
	}

	public String getHelp() {
		return getHelp(null);
	}

	/*
	 * Returns either the help message for a particular command, 
	 * or returns the help messages for all commands (if command is not specified)
	 */
	private String getHelp(String commandName) {
		StringBuffer help = new StringBuffer();

		if (commandsHelp == null) {
			initializeCommandsHelp();
		}

		if (commandGroups == null) {
			initializeCommandGroups();
		}

		if (commandName != null) {
			if (commandsHelp.containsKey(commandName)) {
				addCommand(commandName, commandsHelp.get(commandName), help);
			}
			return help.toString();
		}

		addHeader(Messages.Console_help_header, help);
		for (Entry<String, String[]> groupEntry : commandGroups.entrySet()) {
			addHeader(groupEntry.getKey(), help);
			for (String command : groupEntry.getValue()) {
				addCommand(command, commandsHelp.get(command), help);
			}
		}

		return help.toString();
	}

	private void addHeader(String header, StringBuffer help) {
		help.append("---"); //$NON-NLS-1$
		help.append(header);
		help.append("---"); //$NON-NLS-1$
		help.append(NEW_LINE);
	}

	private void addCommand(String command, String description, StringBuffer help) {
		help.append(TAB);
		help.append(command);
		help.append(" "); //$NON-NLS-1$
		help.append(description);
		help.append(NEW_LINE);
	}

	private void initializeCommandsHelp() {
		commandsHelp = new HashMap<String, String>();

		// add commands for repository
		commandsHelp.put("provaddrepo", Messages.Console_help_provaddrepo_description); //$NON-NLS-1$
		commandsHelp.put("provdelrepo", Messages.Console_help_provdelrepo_description); //$NON-NLS-1$
		commandsHelp.put("provaddmetadatarepo", Messages.Console_help_provaddmetadatarepo_description); //$NON-NLS-1$
		commandsHelp.put("provdelmetadatarepo", Messages.Console_help_provdelmetadatarepo_description); //$NON-NLS-1$
		commandsHelp.put("provaddartifactrepo", Messages.Console_help_provaddartifactrepo_description); //$NON-NLS-1$
		commandsHelp.put("provdelartifactrepo", Messages.Console_help_provdelartifactrepo_description); //$NON-NLS-1$
		commandsHelp.put("provlg", Messages.Console_help_provlg_description); //$NON-NLS-1$
		commandsHelp.put("provlr", Messages.Console_help_provlr_description); //$NON-NLS-1$
		commandsHelp.put("provlar", Messages.Console_help_provlar_description); //$NON-NLS-1$
		commandsHelp.put("provliu", Messages.Console_help_provliu_description); //$NON-NLS-1$
		commandsHelp.put("provlquery", Messages.Console_help_provlquery_description); //$NON-NLS-1$

		// add commands for profiles
		commandsHelp.put("provaddprofile", Messages.Console_help_provaddprofile_description); //$NON-NLS-1$
		commandsHelp.put("provdelprofile", Messages.Console_help_provdelprofile_description); //$NON-NLS-1$
		commandsHelp.put("provlp", Messages.Console_help_provlp_description); //$NON-NLS-1$
		commandsHelp.put("provlgp", Messages.Console_help_provlgp_description); //$NON-NLS-1$
		commandsHelp.put("provlpts", Messages.Console_help_provlpts_description); //$NON-NLS-1$
		commandsHelp.put("provlpquery", Messages.Console_help_provlpquery_description); //$NON-NLS-1$

		// add commands for install/uninstall
		commandsHelp.put("provinstall", Messages.Console_help_provinstall_description); //$NON-NLS-1$
		commandsHelp.put("provremove", Messages.Console_help_provremove_description); //$NON-NLS-1$
		commandsHelp.put("provrevert", Messages.Console_help_provrevert_description); //$NON-NLS-1$
	}

	private void initializeCommandGroups() {
		commandGroups = new LinkedHashMap<String, String[]>();
		commandGroups.put(Messages.Console_help_repository_header,
				new String[] {"provaddrepo", "provdelrepo", "provaddmetadatarepo", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"provdelmetadatarepo", "provaddartifactrepo", "provdelartifactrepo", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"provlg", "provlr", "provlar", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"provliu", "provlquery"}); //$NON-NLS-1$ //$NON-NLS-2$

		commandGroups.put(Messages.Console_help_profile_registry_header, new String[] {"provaddprofile", "provdelprofile", //$NON-NLS-1$ //$NON-NLS-2$
				"provlp", "provlgp", "provlpts", "provlpquery"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		commandGroups.put(Messages.Console_help_install_header, new String[] {"provinstall", "provremove", "provrevert"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Prints a string representation of an {@link IInstallableUnit} to the
	 * iterpreter's output stream.
	 */
	public void print(CommandInterpreter interpreter, IInstallableUnit unit) {
		interpreter.print(unit.getId() + ' ' + unit.getVersion());
	}

	/**
	 * Prints a string representation of an {@link IInstallableUnit} to the
	 * iterpreter's output stream, following by a line terminator
	 */
	public void println(CommandInterpreter interpreter, IInstallableUnit unit) {
		print(interpreter, unit);
		interpreter.println();
	}

	private void println(CommandInterpreter interpreter, IArtifactKey artifactKey, File location) {
		interpreter.print(artifactKey.toString() + ' ' + location);
		interpreter.println();
	}
}