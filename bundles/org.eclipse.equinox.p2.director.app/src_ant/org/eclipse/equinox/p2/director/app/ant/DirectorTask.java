/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.p2.director.app.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;
import org.eclipse.equinox.internal.p2.director.app.ILog;
import org.eclipse.equinox.internal.p2.director.app.Messages;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.osgi.util.NLS;

/**
 * An Ant task to call the p2 Director application.
 *
 * @since 1.0
 */
public class DirectorTask extends Task implements ILog {
	private static final String ANT_PREFIX = "${"; //$NON-NLS-1$
	private static final String ENTRY = "!ENTRY "; //$NON-NLS-1$
	private static final String SUBENTRY = "!SUBENTRY "; //$NON-NLS-1$
	private static final String logEntry = "{0} {1} {2}\n"; //$NON-NLS-1$
	private static final String MESSAGE = "!MESSAGE "; //$NON-NLS-1$

	private boolean roaming;
	private boolean list;
	private String profile, flavor, os, ws, nl, arch, uninstallIU;
	private String extraArguments;
	private File destination, bundlePool, agentLocation;
	private URI metadataRepository, artifactRepository;
	private final List<IUDescription> ius = new ArrayList<>();
	private String outputProperty;
	private StringBuffer outputBuffer = null;
	private File logFile = null;

	public static class IUDescription {
		private String id = null;
		private String version = null;

		public VersionedId getVersionedId() {
			return new VersionedId(id, version);
		}

		public void setId(String id) {
			this.id = id;
		}

		public void setVersion(String version) {
			if (version.length() > 0 && !version.startsWith(ANT_PREFIX))
				this.version = version;
		}
	}

	@Override
	public void execute() throws BuildException {
		Object result = null;
		try {
			if (outputProperty != null)
				outputBuffer = new StringBuffer();

			// collect the arguments and call the application
			DirectorApplication application = new DirectorApplication();
			application.setLog(this);
			result = application.run(getArguments());
		} catch (Exception e) {
			getProject().log(Messages.problem_CallingDirector, e, Project.MSG_ERR);
			throw new BuildException(Messages.problem_CallingDirector, e);
		} finally {
			log(outputBuffer);
		}
		if (result != null && !result.equals(IApplication.EXIT_OK)) {
			getProject().log(Messages.problem_CallingDirector, Project.MSG_ERR);
			throw new BuildException(Messages.problem_CallingDirector);
		}
	}

	public void addConfiguredIu(IUDescription iu) {
		ius.add(iu);
	}

	private String[] getArguments() {
		List<String> result = new ArrayList<>();
		if (roaming)
			result.add("-roaming"); //$NON-NLS-1$
		if (profile != null) {
			result.add("-profile"); //$NON-NLS-1$
			result.add(profile);
		}
		if (extraArguments != null) {
			StringTokenizer tokenizer = new StringTokenizer(extraArguments);
			while (tokenizer.hasMoreTokens()) {
				result.add(tokenizer.nextToken());
			}
		}
		if (destination != null) {
			result.add("-destination"); //$NON-NLS-1$
			result.add(destination.getAbsolutePath());
		}
		if (bundlePool != null) {
			result.add("-bundlePool"); //$NON-NLS-1$
			result.add(bundlePool.getAbsolutePath());
		}
		if (agentLocation != null) {
			result.add("-shared"); //$NON-NLS-1$
			result.add(agentLocation.getAbsolutePath());
		}
		if (metadataRepository != null) {
			result.add("-metadataRepository"); //$NON-NLS-1$
			result.add(URIUtil.toUnencodedString(metadataRepository));
		}
		if (artifactRepository != null) {
			result.add("-artifactRepository"); //$NON-NLS-1$
			result.add(URIUtil.toUnencodedString(artifactRepository));
		}
		if (flavor != null) {
			result.add("-flavor"); //$NON-NLS-1$
			result.add(flavor);
		}
		if (ius.size() > 0) {
			result.add("-installIUs"); //$NON-NLS-1$
			StringBuilder arg = new StringBuilder();
			for (Iterator<IUDescription> iterator = ius.iterator(); iterator.hasNext();) {
				IUDescription object = iterator.next();
				arg.append(object.getVersionedId().toString());
				if (iterator.hasNext())
					arg.append(',');
			}
			result.add(arg.toString());
		}
		if (os != null) {
			result.add("-p2.os"); //$NON-NLS-1$
			result.add(os);
		}
		if (ws != null) {
			result.add("-p2.ws"); //$NON-NLS-1$
			result.add(ws);
		}
		if (arch != null) {
			result.add("-p2.arch"); //$NON-NLS-1$
			result.add(arch);
		}
		if (nl != null) {
			result.add("-p2.nl"); //$NON-NLS-1$
			result.add(nl);
		}
		if (list) {
			result.add("-list"); //$NON-NLS-1$
		}

		if (uninstallIU != null) {
			result.add("-uninstallIU"); //$NON-NLS-1$
			result.add(uninstallIU);
		}
		return result.toArray(new String[result.size()]);
	}

	public void setArch(String value) {
		arch = value;
	}

	public void setArtifactRepository(String value) {
		try {
			artifactRepository = URIUtil.fromString(value);
		} catch (URISyntaxException e) {
			log(NLS.bind(Messages.problem_repoMustBeURI, value), e, Project.MSG_ERR);
		}
	}

	public void setBundlePool(String value) {
		bundlePool = new File(value);
	}

	public void setDestination(String value) {
		destination = new File(value);
	}

	public void setFlavor(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX))
			flavor = value;
	}

	public void setList(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX))
			list = Boolean.parseBoolean(value);
	}

	public void setMetadataRepository(String value) {
		try {
			metadataRepository = URIUtil.fromString(value);
		} catch (URISyntaxException e) {
			log(NLS.bind(Messages.problem_repoMustBeURI, value), e, Project.MSG_ERR);
		}
	}

	public void setNl(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX))
			nl = value;
	}

	public void setOs(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX))
			os = value;
	}

	public void setProfile(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX))
			profile = value;
	}

	public void setExtraArguments(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX)) {
			extraArguments = value;
		}

	}

	public void setRoaming(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX))
			roaming = Boolean.parseBoolean(value);
	}

	public void setUninstallIU(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX))
			uninstallIU = value;
	}

	public void setWs(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX))
			ws = value;
	}

	public void setAgentLocation(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX))
			agentLocation = new File(value);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void log(String msg) {
		if (outputBuffer != null) {
			outputBuffer.append(msg);
			if (!msg.endsWith("\n")) //$NON-NLS-1$
				outputBuffer.append('\n');
		}
		super.log(msg, Project.MSG_INFO);
	}

	@Override
	public void log(IStatus status) {
		log(0, status);
		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (IStatus child : children) {
				log(1, child);
			}
		}
	}

	private void log(int depth, IStatus status) {
		log(status.getMessage(), status.getException(), Project.MSG_ERR);
		if (outputBuffer != null) {
			if (depth == 0)
				outputBuffer.append(ENTRY);
			else
				outputBuffer.append(SUBENTRY);
			outputBuffer.append(NLS.bind(logEntry, new String[] {status.getPlugin(), Integer.toString(status.getSeverity()), Integer.toString(status.getCode())}));
			outputBuffer.append(MESSAGE);
			outputBuffer.append(status.getMessage());
			outputBuffer.append('\n');
		}
	}

	private void log(StringBuffer buffer) {
		if (outputBuffer == null)
			return;

		String logString = outputBuffer.toString();
		getProject().setNewProperty(outputProperty, logString);

		if (logFile != null) {
			File parentFile = logFile.getParentFile();
			if (parentFile != null && !parentFile.exists())
				parentFile.mkdirs();

			try (FileWriter writer = new FileWriter(logFile)) {

				writer.write(logString);

			} catch (IOException e) {
				getProject().log(NLS.bind(Messages.unableToWriteLogFile, logFile.getAbsolutePath()), e, Project.MSG_WARN);
			}
		}
	}

	public void setOutputProperty(String property) {
		this.outputProperty = property;
	}

	public void setLogFile(String file) {
		if (file != null && !file.startsWith(ANT_PREFIX)) {
			this.logFile = new File(file);
			if (!logFile.isAbsolute()) {
				logFile = new File(getProject().getBaseDir(), file);
			}
		}
	}

	@Override
	public void close() {
		// ILog#close(),  nothing to do here
	}
}
