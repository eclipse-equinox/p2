package org.eclipse.equinox.internal.p2.engine;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;

public class ActionManager implements IRegistryChangeListener {

	private static final String PT_ACTIONS = "actions"; //$NON-NLS-1$
	private static final String ELEMENT_ACTION = "action"; //$NON-NLS-1$
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	private static ActionManager instance;

	public static synchronized ActionManager getInstance() {
		if (instance == null) {
			instance = new ActionManager();
		}
		return instance;
	}

	private HashMap actionMap;

	private ActionManager() {
		RegistryFactory.getRegistry().addRegistryChangeListener(this);
	}

	public ProvisioningAction getAction(String actionId) {
		IConfigurationElement actionElement = (IConfigurationElement) getActionMap().get(actionId);
		if (actionElement != null && actionElement.isValid()) {
			try {
				return (ProvisioningAction) actionElement.createExecutableExtension(ATTRIBUTE_CLASS);
			} catch (CoreException e) {
				//TODO: create Message
				LogHelper.log(new Status(IStatus.ERROR, EngineActivator.ID, "Error creating action with id=" + actionId));
			}
		}
		return null;
	}

	private synchronized Map getActionMap() {
		IExtensionPoint point = RegistryFactory.getRegistry().getExtensionPoint(EngineActivator.ID, PT_ACTIONS);
		IExtension[] extensions = point.getExtensions();
		actionMap = new HashMap(extensions.length);
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				IConfigurationElement actionElement = elements[j];
				if (!actionElement.getName().equals(ELEMENT_ACTION))
					continue;

				String actionId = actionElement.getAttribute(ATTRIBUTE_ID);
				if (actionId == null)
					continue;

				if (actionId.indexOf('.') == -1)
					actionId = actionElement.getNamespaceIdentifier() + "." + actionId; //$NON-NLS-1$

				actionMap.put(actionId, actionElement);
			}
		}
		return actionMap;
	}

	public synchronized void registryChanged(IRegistryChangeEvent event) {
		actionMap = null;
	}
}
