/*******************************************************************************
 * Copyright (c) 2014, 2016 Rapicorp, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Rapicorp, Inc - application to collect native packages from an existing install
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives;

import java.io.*;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.CheckAndPromptNativePackage;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceReference;

public class NativePackageExtractionApplication implements IApplication {
	//Keys used in the file created by the application
	private static final String PROP_LAUNCHER_NAME = "launcherName"; //$NON-NLS-1$
	private static final String PROP_ARCH = "arch"; //$NON-NLS-1$
	private static final String PROP_DEPENDS = "depends"; //$NON-NLS-1$

	//Internal constants
	private static final String DEFAULT_VERSION_CONSTRAINT = "ge"; //$NON-NLS-1$
	private static final String _ACTION_ID = "_action_id_"; //$NON-NLS-1$
	private static final String PROP_P2_PROFILE = "eclipse.p2.profile"; //$NON-NLS-1$
	private static final Integer EXIT_ERROR = 13;

	//Constants for arguments
	private static final String OPTION_TO_ANALYZE = "-toAnalyze"; //$NON-NLS-1$
	private static final String OPTION_RESULT_FILE = "-output"; //$NON-NLS-1$

	//Values provided as a parameter to the application
	private File installation;
	private File resultFile;

	//Values derived
	private IProvisioningAgent targetAgent;
	private String profileId;

	//Data collected by the application
	private Properties extractedData = new Properties();

	private Properties installCommandsProperties = new Properties();

	private boolean stackTrace = false;

	public Object start(IApplicationContext context) throws Exception {
		try {
			processArguments((String[]) context.getArguments().get("application.args")); //$NON-NLS-1$
			initializeServices();
			NativeTouchpoint.loadInstallCommandsProperties(installCommandsProperties, "debian"); //$NON-NLS-1$
			NativeTouchpoint.loadInstallCommandsProperties(installCommandsProperties, "windows"); //$NON-NLS-1$
			collectData();
			persistInformation();
		} catch (CoreException e) {
			deeplyPrint(e.getStatus(), System.err, 0);
			return EXIT_ERROR;
		}
		return IApplication.EXIT_OK;
	}

	private void processArguments(String[] args) throws CoreException {
		if (args == null || args.length == 0) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.NativePackageExtractionApplication_MissingParameters, OPTION_TO_ANALYZE, OPTION_RESULT_FILE)));
		}
		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)
			String opt = args[i];

			if (OPTION_TO_ANALYZE.equals(opt)) {
				installation = new File(getRequiredArgument(args, ++i));
				if (!installation.exists())
					throw new CoreException(new Status(IStatus.ERROR, Activator.ID, Messages.NativePackageExtractionApplication_FolderNotFound + installation.getAbsolutePath()));
				continue;
			}

			if (OPTION_RESULT_FILE.equals(opt)) {
				resultFile = new File(getRequiredArgument(args, ++i));
				continue;
			}
		}
	}

	private static String getRequiredArgument(String[] args, int argIdx) throws CoreException {
		if (argIdx < args.length) {
			String arg = args[argIdx];
			if (!arg.startsWith("-")) //$NON-NLS-1$
				return arg;
		}
		throw new ProvisionException(NLS.bind(Messages.NativePackageExtractionApplication_MissingValue, args[argIdx - 1]));
	}

	private void collectData() {
		IProfileRegistry registry = (IProfileRegistry) targetAgent.getService(IProfileRegistry.SERVICE_NAME);
		IProfile p = registry.getProfile(profileId);
		collectArchitecture(p);
		collectLauncherName(p);
		collectDebianDependencies(p);
	}

	private void persistInformation() throws CoreException {
		try {
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(resultFile));
			try {
				extractedData.store(os, "Data extracted from eclipse located at " + installation); //$NON-NLS-1$
			} finally {
				if (os != null)
					os.close();
			}
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.ID, Messages.NativePackageExtractionApplication_PersistencePb + resultFile.getAbsolutePath(), e));
		}
	}

	private void collectLauncherName(IProfile p) {
		String launcherName = p.getProperty("eclipse.touchpoint.launcherName"); //$NON-NLS-1$
		if (launcherName == null)
			launcherName = ""; //$NON-NLS-1$
		extractedData.put(PROP_LAUNCHER_NAME, launcherName);
	}

	private void collectArchitecture(IProfile p) {
		String environments = p.getProperty(IProfile.PROP_ENVIRONMENTS);
		if (environments != null) {
			for (StringTokenizer tokenizer = new StringTokenizer(environments, ","); tokenizer.hasMoreElements();) { //$NON-NLS-1$
				String entry = tokenizer.nextToken();
				int i = entry.indexOf('=');
				String key = entry.substring(0, i).trim();
				String value = entry.substring(i + 1).trim();
				if (!key.equals("osgi.arch")) //$NON-NLS-1$
					continue;

				if ("x86_64".equals(value)) { //$NON-NLS-1$
					extractedData.put(PROP_ARCH, "amd64"); //$NON-NLS-1$
					return;
				}
				if ("x86".equals(value)) { //$NON-NLS-1$
					extractedData.put(PROP_ARCH, "x86"); //$NON-NLS-1$
					return;
				}
			}
		}
		extractedData.put(PROP_ARCH, ""); //$NON-NLS-1$
	}

	private void collectDebianDependencies(IProfile p) {
		String depends = ""; //$NON-NLS-1$
		IQueryResult<IInstallableUnit> allIUs = p.available(QueryUtil.ALL_UNITS, new NullProgressMonitor());
		Iterator<IInstallableUnit> a = allIUs.iterator();
		while (a.hasNext()) {
			IInstallableUnit iu = a.next();
			Collection<ITouchpointData> tpdata = iu.getTouchpointData();
			for (ITouchpointData data : tpdata) {
				Map<String, ITouchpointInstruction> allInstructions = data.getInstructions();
				for (ITouchpointInstruction instruction : allInstructions.values()) {
					StringTokenizer tokenizer = new StringTokenizer(instruction.getBody(), ";"); //$NON-NLS-1$
					while (tokenizer.hasMoreTokens()) {
						Map<String, String> parsedInstructions = parseInstruction(tokenizer.nextToken());
						if (parsedInstructions != null && parsedInstructions.get(_ACTION_ID).endsWith(CheckAndPromptNativePackage.ID)) {
							if ("debian".equals(parsedInstructions.get(ActionConstants.PARM_LINUX_DISTRO))) { //$NON-NLS-1$
								depends += formatAsDependsEntry(parsedInstructions.get(ActionConstants.PARM_LINUX_PACKAGE_NAME), parsedInstructions.get(ActionConstants.PARM_LINUX_PACKAGE_VERSION), parsedInstructions.get(ActionConstants.PARM_LINUX_VERSION_COMPARATOR)) + ',';
							}
						}
					}
				}
			}
		}
		//pre-prend a comma, and remove the last one
		if (depends.length() > 0)
			depends = ',' + depends.substring(0, depends.length() - 1);

		extractedData.put(PROP_DEPENDS, depends);
	}

	private String formatAsDependsEntry(String packageId, String version, String versionComparator) {
		String result = packageId;
		if (versionComparator == null)
			versionComparator = DEFAULT_VERSION_CONSTRAINT;
		if (version != null) {
			result += '(' + getUserFriendlyComparator(versionComparator) + ' ' + version + ')';
		}
		return result;
	}

	private String getUserFriendlyComparator(String comparator) {
		return installCommandsProperties.getProperty(comparator, ""); //$NON-NLS-1$
	}

	//Code copied from the InstructionParser class
	private Map<String, String> parseInstruction(String statement) {
		Map<String, String> instructions = new HashMap<String, String>();

		int openBracket = statement.indexOf('(');
		int closeBracket = statement.lastIndexOf(')');
		if (openBracket == -1 || closeBracket == -1 || openBracket > closeBracket)
			return null;
		instructions.put(_ACTION_ID, statement.substring(0, openBracket).trim());

		String nameValuePairs = statement.substring(openBracket + 1, closeBracket);
		if (nameValuePairs.length() == 0)
			return instructions;

		StringTokenizer tokenizer = new StringTokenizer(nameValuePairs, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String nameValuePair = tokenizer.nextToken();
			int colonIndex = nameValuePair.indexOf(":"); //$NON-NLS-1$
			if (colonIndex == -1)
				return null;
			String name = nameValuePair.substring(0, colonIndex).trim();
			String value = nameValuePair.substring(colonIndex + 1).trim();
			instructions.put(name, value);
		}
		return instructions;
	}

	private void initializeServices() throws ProvisionException {
		ServiceReference<IProvisioningAgentProvider> agentProviderRef = Activator.getContext().getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider provider = Activator.getContext().getService(agentProviderRef);

		URI p2DataArea = new File(installation, "p2").toURI(); //$NON-NLS-1$
		targetAgent = provider.createAgent(p2DataArea);
		targetAgent.registerService(IProvisioningAgent.INSTALLER_AGENT, provider.createAgent(null));

		Activator.getContext().ungetService(agentProviderRef);
		if (profileId == null) {
			if (installation != null) {
				File configIni = new File(installation, "configuration/config.ini"); //$NON-NLS-1$
				InputStream in = null;
				try {
					Properties ciProps = new Properties();
					in = new BufferedInputStream(new FileInputStream(configIni));
					ciProps.load(in);
					profileId = ciProps.getProperty(PROP_P2_PROFILE);
				} catch (IOException e) {
					// Ignore
				} finally {
					if (in != null)
						try {
							in.close();
						} catch (IOException e) {
							// Ignore;
						}
				}
				if (profileId == null)
					profileId = installation.toString();
			}
		}
		if (profileId != null)
			targetAgent.registerService(PROP_P2_PROFILE, profileId);
	}

	private static void appendLevelPrefix(PrintStream strm, int level) {
		for (int idx = 0; idx < level; ++idx)
			strm.print(' ');
	}

	private void deeplyPrint(CoreException ce, PrintStream strm, int level) {
		appendLevelPrefix(strm, level);
		if (stackTrace)
			ce.printStackTrace(strm);
		deeplyPrint(ce.getStatus(), strm, level);
	}

	private void deeplyPrint(IStatus status, PrintStream strm, int level) {
		appendLevelPrefix(strm, level);
		String msg = status.getMessage();
		strm.println(msg);
		Throwable cause = status.getException();
		if (cause != null) {
			strm.print("Caused by: "); //$NON-NLS-1$
			if (stackTrace || !(msg.equals(cause.getMessage()) || msg.equals(cause.toString())))
				deeplyPrint(cause, strm, level);
		}

		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++)
				deeplyPrint(children[i], strm, level + 1);
		}
	}

	private void deeplyPrint(Throwable t, PrintStream strm, int level) {
		if (t instanceof CoreException)
			deeplyPrint((CoreException) t, strm, level);
		else {
			appendLevelPrefix(strm, level);
			if (stackTrace)
				t.printStackTrace(strm);
			else {
				strm.println(t.toString());
				Throwable cause = t.getCause();
				if (cause != null) {
					strm.print("Caused by: "); //$NON-NLS-1$
					deeplyPrint(cause, strm, level);
				}
			}
		}
	}

	public void stop() {
		//We don't handle application stopping
	}

}
