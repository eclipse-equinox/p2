/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.osgi.util.NLS;

public class ActionFactory {

	private static Map actions;

	static {
		actions = new HashMap();
		actions.put(CollectAction.ID, CollectAction.class);
		actions.put(InstallBundleAction.ID, InstallBundleAction.class);
		actions.put(UninstallBundleAction.ID, UninstallBundleAction.class);
		actions.put(AddSourceBundleAction.ID, AddSourceBundleAction.class);
		actions.put(RemoveSourceBundleAction.ID, RemoveSourceBundleAction.class);
		actions.put(InstallFeatureAction.ID, InstallFeatureAction.class);
		actions.put(UninstallFeatureAction.ID, UninstallFeatureAction.class);
		actions.put(SetLauncherNameAction.ID, SetLauncherNameAction.class);
		actions.put(AddProgramArgumentAction.ID, AddProgramArgumentAction.class);
		actions.put(RemoveProgramArgumentAction.ID, RemoveProgramArgumentAction.class);
		actions.put(SetStartLevelAction.ID, SetStartLevelAction.class);
		actions.put(MarkStartedAction.ID, MarkStartedAction.class);
		actions.put(SetFrameworkDependentPropertyAction.ID, SetFrameworkDependentPropertyAction.class);
		actions.put(SetFrameworkIndependentPropertyAction.ID, SetFrameworkIndependentPropertyAction.class);
		actions.put(SetProgramPropertyAction.ID, SetProgramPropertyAction.class);
		actions.put(AddJVMArgumentAction.ID, AddJVMArgumentAction.class);
		actions.put(RemoveJVMArgumentAction.ID, RemoveJVMArgumentAction.class);
		actions.put(MkdirAction.ID, MkdirAction.class);
		actions.put(RmdirAction.ID, RmdirAction.class);
		actions.put(LinkAction.ID, LinkAction.class);
		actions.put(ChmodAction.ID, ChmodAction.class);
	}

	public static ProvisioningAction create(String actionId) {
		Class clazz = (Class) actions.get(actionId);
		if (clazz != null) {
			try {
				return (ProvisioningAction) clazz.newInstance();
			} catch (InstantiationException e) {
				LogHelper.log(Util.createError(NLS.bind(Messages.action_not_instantiated, actionId), e));
				return null;
			} catch (IllegalAccessException e) {
				LogHelper.log(Util.createError(NLS.bind(Messages.action_not_instantiated, actionId), e));
				return null;
			}
		}
		return null;
	}
}
