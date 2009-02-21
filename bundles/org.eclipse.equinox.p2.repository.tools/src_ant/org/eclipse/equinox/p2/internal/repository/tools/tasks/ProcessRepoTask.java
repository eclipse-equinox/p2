/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.jarprocessor.ant.JarProcessorTask;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.RecreateRepositoryApplication;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;

public class ProcessRepoTask extends Task {

	public static class SigningOptions {
		public String alias;
		public String keystore;
		public String storepass;
		public String keypass;
		public boolean unsign;

		public void setAlias(String alias) {
			this.alias = alias;
		}

		public void setKeystore(String keystore) {
			this.keystore = keystore;
		}

		public void setKeypass(String keypass) {
			this.keypass = keypass;
		}

		public void setStorepass(String storepass) {
			this.storepass = storepass;
		}

		public void setUnsign(String unsign) {
			this.unsign = Boolean.valueOf(unsign).booleanValue();
		}
	}

	private URI repository = null;

	private boolean pack = false;
	private boolean repack = false;
	private SigningOptions signing = null;

	public void execute() throws BuildException {
		File file = URIUtil.toFile(repository);
		if (file == null || !file.exists()) {
			throw new BuildException("Repository must be local: " + repository.toString()); //$NON-NLS-1$
		}
		if (pack | repack | signing != null) {
			JarProcessorTask task = new JarProcessorTask();
			if (signing != null) {
				task.setAlias(signing.alias);
				task.setKeypass(signing.keypass);
				task.setKeystore(signing.keystore);
				task.setStorepass(signing.storepass);
				task.setUnsign(signing.unsign);

				if (signing.alias != null && signing.alias.length() > 0 && !signing.alias.startsWith("${")) //$NON-NLS-1$
					task.setSign(true);
			}
			task.setPack(pack);
			task.setNormalize(repack);
			task.setInputFolder(new File(repository));
			task.setProject(getProject());
			task.execute();
		}

		recreateRepository();
	}

	private void recreateRepository() {
		RepositoryDescriptor descriptor = new RepositoryDescriptor();
		descriptor.setAppend(true);
		descriptor.setFormat(null);
		descriptor.setKind("artifact"); //$NON-NLS-1$
		descriptor.setLocation(repository);

		RecreateRepositoryApplication application = new RecreateRepositoryApplication();
		application.setArtifactRepository(descriptor);
		try {
			application.run(new NullProgressMonitor());
		} catch (ProvisionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setRepositoryPath(String repository) {
		try {
			this.repository = URIUtil.fromString(repository);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Repository location (" + repository + ") must be a URL."); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void setPack(boolean pack) {
		this.pack = pack;
	}

	public void setNormalize(boolean normalize) {
		this.repack = normalize;
	}

	public void addConfiguredSign(SigningOptions options) {
		this.signing = options;
	}
}
