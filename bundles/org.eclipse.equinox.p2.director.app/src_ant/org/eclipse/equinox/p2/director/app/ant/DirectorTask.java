/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.director.app.ant;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.util.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.director.app.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.VersionedId;
import org.eclipse.osgi.util.NLS;

/**
 * An Ant task to call the p2 Director application.
 * 
 * @since 1.0
 */
public class DirectorTask extends Task implements ILog {
	private static final String ANT_PREFIX = "${"; //$NON-NLS-1$

	private boolean roaming;
	private boolean list;
	private String profile, flavor, os, ws, nl, arch, uninstallIU;
	private String extraArguments;
	private File destination, bundlePool, agentLocation;
	private URI metadataRepository, artifactRepository;
	private List ius = new ArrayList();
	private String outputProperty;
	private StringBuffer outputBuffer = null;

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

	/*
	 * (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			if (outputProperty != null)
				outputBuffer = new StringBuffer();

			// collect the arguments and call the application
			DirectorApplication application = new DirectorApplication();
			application.setLog(this);
			application.run(getArguments());
		} catch (Exception e) {
			getProject().log(Messages.problem_CallingDirector, e, Project.MSG_ERR);
			throw new BuildException(Messages.problem_CallingDirector, e);
		} finally {
			if (outputBuffer != null) {
				getProject().setNewProperty(outputProperty, outputBuffer.toString());
			}
		}
	}

	public void addConfiguredIu(IUDescription iu) {
		ius.add(iu);
	}

	private String[] getArguments() {
		List<String> result = new ArrayList<String>();
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
			result.add(metadataRepository.toString());
		}
		if (artifactRepository != null) {
			result.add("-artifactRepository"); //$NON-NLS-1$
			result.add(artifactRepository.toString());
		}
		if (flavor != null) {
			result.add("-flavor"); //$NON-NLS-1$
			result.add(flavor);
		}
		if (ius.size() > 0) {
			result.add("-installIUs"); //$NON-NLS-1$
			StringBuffer arg = new StringBuffer();
			for (Iterator iterator = ius.iterator(); iterator.hasNext();) {
				IUDescription object = (IUDescription) iterator.next();
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
			result.add(Application.COMMAND_NAMES[Application.COMMAND_LIST]);
		}

		if (uninstallIU != null) {
			result.add(Application.COMMAND_NAMES[Application.COMMAND_UNINSTALL]);
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
			list = Boolean.valueOf(value).booleanValue();
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
			roaming = Boolean.valueOf(value).booleanValue();
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

	public void log(String msg) {
		if (outputBuffer != null) {
			outputBuffer.append(msg);
			if (!msg.endsWith("\n")) //$NON-NLS-1$
				outputBuffer.append(StringUtils.LINE_SEP);
		}
		super.log(msg, Project.MSG_INFO);
	}

	public void log(IStatus status) {
		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++)
				log(children[i]);
		}

		log(status.getMessage(), status.getException(), Project.MSG_ERR);
		if (outputBuffer != null) {
			outputBuffer.append(status.getMessage());
			outputBuffer.append(StringUtils.LINE_SEP);
		}

	}

	public void setOutputProperty(String property) {
		this.outputProperty = property;
	}

	public void close() {
		// ILog#close(),  nothing to do here
	}
}
