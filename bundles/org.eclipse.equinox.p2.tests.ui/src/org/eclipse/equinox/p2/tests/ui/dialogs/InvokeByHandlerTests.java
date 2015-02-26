/*******************************************************************************
 *  Copyright (c) 2008, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import org.eclipse.core.commands.*;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * Tests for invoking the p2 wizards by command id.
 * Other plug-ins do this, this test reminds us that if the handler
 * ids change, there are repercussions.
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=263262
 */
public class InvokeByHandlerTests extends AbstractProvisioningUITest {

	private static final String INSTALL = "org.eclipse.equinox.p2.ui.sdk.install";
	private static final String UPDATE = "org.eclipse.equinox.p2.ui.sdk.update";

	public void testInstallHandler() throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				Display.getDefault().getActiveShell().close();
			}

		});
		runCommand(INSTALL);

	}

	public void testUpdateHandler() throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				Display.getDefault().getActiveShell().close();
			}

		});
		runCommand(UPDATE);
	}

	private void runCommand(String commandId) throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException {
		ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
		Command command = commandService.getCommand(commandId);
		if (!command.isDefined()) {
			return;
		}
		IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
		handlerService.executeCommand(commandId, null);
	}
}
