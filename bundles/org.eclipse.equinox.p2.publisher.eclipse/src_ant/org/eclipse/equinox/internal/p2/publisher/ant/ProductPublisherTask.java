/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.VersionAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;

public class ProductPublisherTask extends AbstractPublishTask {

	public static class ConfigElement {
		public String os;
		public String ws;
		public String arch;

		public void setOs(String os) {
			this.os = os;
		}

		public void setWs(String ws) {
			this.ws = ws;
		}

		public void setArch(String arch) {
			this.arch = arch;
		}

		public String toString() {
			return ws + '.' + os + '.' + arch;
		}
	}

	public static class AdviceElement {
		public String kind;
		public String file;

		public void setKind(String kind) {
			this.kind = kind;
		}

		public void setFile(String file) {
			this.file = file;
		}
	}

	private String flavor;
	private String productFile;
	private String executables;
	private String jreLocation;
	private final List<ConfigElement> configurations = new ArrayList<ConfigElement>(3);
	private final List<AdviceElement> advice = new ArrayList<AdviceElement>(3);

	public void execute() throws BuildException {
		try {
			initializeRepositories(getInfo());
		} catch (ProvisionException e) {
			throw new BuildException("Unable to configure repositories", e); //$NON-NLS-1$
		}

		IProductDescriptor productDescriptor = null;
		try {
			productDescriptor = new ProductFile(productFile);
		} catch (Exception e) {
			if (productFile == null)
				throw new IllegalArgumentException("unable to load product file"); //$NON-NLS-1$
		}

		if (flavor == null || flavor.startsWith(ANT_PROPERTY_PREFIX))
			flavor = "tooling"; //$NON-NLS-1$

		IPublisherAction action = new ProductAction(source, productDescriptor, flavor, executables != null ? new File(executables) : null, jreLocation != null ? new File(jreLocation) : null);
		new Publisher(getInfo()).publish(new IPublisherAction[] {action}, new NullProgressMonitor());
	}

	protected PublisherInfo getInfo() {
		String[] configStrings = new String[configurations.size()];
		for (int i = 0; i < configurations.size(); i++) {
			configStrings[i] = configurations.get(i).toString();
		}

		PublisherInfo info = super.getInfo();
		info.setConfigurations(configStrings);
		processAdvice(info);
		return info;
	}

	protected void processAdvice(PublisherInfo info) {
		for (AdviceElement element : advice) {
			if (element.kind == null || element.file == null)
				continue;

			if (element.kind.equals("featureVersions") || element.kind.equals("pluginVersions")) { //$NON-NLS-1$ //$NON-NLS-2$
				VersionAdvice versionAdvice = new VersionAdvice();
				versionAdvice.load(IInstallableUnit.NAMESPACE_IU_ID, element.file, element.kind.startsWith("features") ? ".feature.group" : null); //$NON-NLS-1$ //$NON-NLS-2$
				info.addAdvice(versionAdvice);
			}
		}
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	public void setProductFile(String productFile) {
		this.productFile = productFile;
	}

	public void setExecutables(String executables) {
		this.executables = executables;
	}

	public void setJreLocation(String jreLocation) {
		this.jreLocation = jreLocation;
	}

	public void setSource(String source) {
		super.source = source;
	}

	public void addConfiguredConfig(ConfigElement config) {
		this.configurations.add(config);
	}

	public void addConfiguredAdvice(AdviceElement element) {
		this.advice.add(element);
	}

}
