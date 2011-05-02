/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.discovery;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.operations.DiscoveryInstallOperation;
import org.eclipse.equinox.internal.p2.ui.discovery.util.CommonColors;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.Messages;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @author David Green
 */
public abstract class DiscoveryUi {

	public static final String ID_PLUGIN = "org.eclipse.equinox.p2.ui.discovery"; //$NON-NLS-1$

	private static CommonColors commonColors;

	private DiscoveryUi() {
		// don't allow clients to instantiate
	}

	public static boolean install(List<CatalogItem> descriptors, IRunnableContext context) {
		try {
			IRunnableWithProgress runner = new DiscoveryInstallOperation(descriptors);
			context.run(true, true, runner);
		} catch (InvocationTargetException e) {
			IStatus status = new Status(IStatus.ERROR, DiscoveryUi.ID_PLUGIN, NLS.bind(Messages.ConnectorDiscoveryWizard_installProblems, new Object[] {e.getCause().getMessage()}), e.getCause());
			StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			return false;
		} catch (InterruptedException e) {
			// canceled
			return false;
		}
		return true;
	}

	public static CommonColors getCommonsColors() {
		if (commonColors == null) {
			commonColors = new CommonColors(Display.getDefault(), JFaceResources.getResources());
		}
		return commonColors;
	}

}
