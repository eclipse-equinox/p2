/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin.rcp;

import org.eclipse.jface.action.*;
import org.eclipse.jface.util.Util;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	IAction prefsAction, aboutAction, quitAction;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	protected void makeActions(IWorkbenchWindow window) {
		quitAction = ActionFactory.QUIT.create(window);
		prefsAction = ActionFactory.PREFERENCES.create(window);
		aboutAction = ActionFactory.ABOUT.create(window);
	}

	protected void fillMenuBar(IMenuManager menuBar) {
		IMenuManager fileMenu = new MenuManager(ProvAdminUIMessages.ApplicationActionBarAdvisor_FileMenuName, "file"); //$NON-NLS-1$
		menuBar.add(fileMenu);
		ActionContributionItem quitItem = new ActionContributionItem(quitAction);
		quitItem.setVisible(!Util.isMac());
		fileMenu.add(quitItem);

		IMenuManager windowMenu = new MenuManager(ProvAdminUIMessages.ApplicationActionBarAdvisor_WindowMenuName, "window"); //$NON-NLS-1$
		menuBar.add(windowMenu);
		ActionContributionItem prefsItem = new ActionContributionItem(prefsAction);
		prefsItem.setVisible(!Util.isMac());
		windowMenu.add(prefsItem);

		IMenuManager helpMenu = new MenuManager(ProvAdminUIMessages.ApplicationActionBarAdvisor_HelpMenuName, "help"); //$NON-NLS-1$
		menuBar.add(helpMenu);
		helpMenu.add(aboutAction);
	}

}
