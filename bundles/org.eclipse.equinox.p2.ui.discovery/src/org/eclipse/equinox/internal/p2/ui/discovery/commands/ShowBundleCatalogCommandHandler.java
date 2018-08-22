/*******************************************************************************
 * Copyright (c) 2009, 2017 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.discovery.commands;

import java.util.*;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.compatibility.BundleDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.compatibility.RemoteBundleDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryWizard;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * A command that causes the {@link DiscoveryWizard} to appear in a dialog.
 * 
 * @author David Green
 */
public class ShowBundleCatalogCommandHandler extends AbstractHandler {

	private static final String ID_PARAMETER_DIRECTORY = "org.eclipse.equinox.p2.ui.discovery.commands.DirectoryParameter"; //$NON-NLS-1$

	private static final String ID_PARAMETER_TAGS = "org.eclipse.equinox.p2.ui.discovery.commands.TagsParameter"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) {
		Set<Tag> tags = new LinkedHashSet<>();
		String tagString = event.getParameter(ID_PARAMETER_TAGS);
		if (tagString != null) {
			String[] tagIds = tagString.split("\\s*,\\s*"); //$NON-NLS-1$
			for (String id : tagIds) {
				String[] text = id.split("=", 2); //$NON-NLS-1$
				Tag tag;
				if (text.length > 1) {
					tag = new Tag(text[0], text[1]);
				} else {
					tag = new Tag(id, id);
				}
				tags.add(tag);
			}
		}

		Catalog catalog = new Catalog();

		// look for descriptors from installed bundles
		catalog.getDiscoveryStrategies().add(new BundleDiscoveryStrategy());

		// look for remote descriptor
		String directoryUrl = event.getParameter(ID_PARAMETER_DIRECTORY);
		if (directoryUrl != null && directoryUrl.length() > 0) {
			RemoteBundleDiscoveryStrategy remoteDiscoveryStrategy = new RemoteBundleDiscoveryStrategy();
			remoteDiscoveryStrategy.setDirectoryUrl(directoryUrl);
			catalog.getDiscoveryStrategies().add(remoteDiscoveryStrategy);
		}

		catalog.setEnvironment(DiscoveryCore.createEnvironment());
		catalog.setVerifyUpdateSiteAvailability(true);
		catalog.setTags(new ArrayList<>(tags));

		CatalogConfiguration configuration = new CatalogConfiguration();
		configuration.setShowTagFilter(tags.size() > 0);
		configuration.setSelectedTags(tags);

		DiscoveryWizard wizard = new DiscoveryWizard(catalog, configuration);
		WizardDialog dialog = new WizardDialog(WorkbenchUtil.getShell(), wizard);
		dialog.open();

		return null;
	}

}
