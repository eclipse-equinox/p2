/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.jarprocessor.ant.JarProcessorTask;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.*;
import org.eclipse.osgi.util.NLS;

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
			this.unsign = Boolean.parseBoolean(unsign);
		}
	}

	private URI repository = null;

	private boolean pack = false;
	private boolean repack = false;
	private SigningOptions signing = null;
	private JarProcessorTask jarProcessor = null;

	public void execute() throws BuildException {
		File file = URIUtil.toFile(repository);
		if (file == null || !file.exists()) {
			throw new BuildException(NLS.bind(
					Messages.ProcessRepo_must_be_local, repository.toString()));
		}
		if (pack | repack | signing != null) {
			if (jarProcessor == null)
				jarProcessor = new JarProcessorTask();
			if (signing != null) {
				jarProcessor.setAlias(signing.alias);
				jarProcessor.setKeypass(signing.keypass);
				jarProcessor.setKeystore(signing.keystore);
				jarProcessor.setStorepass(signing.storepass);
				jarProcessor.setUnsign(signing.unsign);

				if (signing.alias != null && signing.alias.length() > 0
						&& !signing.alias.startsWith("${")) //$NON-NLS-1$
					jarProcessor.setSign(true);
			}
			jarProcessor.setPack(pack);
			jarProcessor.setNormalize(repack);
			jarProcessor.setInputFolder(new File(repository));
			jarProcessor.setProject(getProject());
			jarProcessor.execute();
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
		}
	}

	public void setRepositoryPath(String repository) {
		try {
			this.repository = URIUtil.fromString(repository);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(NLS.bind(
					Messages.ProcessRepo_location_not_url, repository));
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

	public void addConfiguredPlugin(IUDescription iu) {
		if (jarProcessor == null)
			jarProcessor = new JarProcessorTask();

		String path = "plugins/" + iu.getId() + '_' + iu.getVersion(); //$NON-NLS-1$
		File repo = new File(repository);
		File plugin = new File(repo, path);
		if (!plugin.exists())
			plugin = new File(repo, path + ".jar"); //$NON-NLS-1$

		if (plugin.exists()) {
			jarProcessor.addInputFile(plugin);
		}
	}

	public void addConfiguredFeature(IUDescription iu) {
		if (jarProcessor == null)
			jarProcessor = new JarProcessorTask();

		String path = "features/" + iu.getId() + '_' + iu.getVersion(); //$NON-NLS-1$
		File repo = new File(repository);
		File feature = new File(repo, path);
		if (!feature.exists())
			feature = new File(repo, path + ".jar"); //$NON-NLS-1$

		if (feature.exists()) {
			jarProcessor.addInputFile(feature);
		}
	}
}
