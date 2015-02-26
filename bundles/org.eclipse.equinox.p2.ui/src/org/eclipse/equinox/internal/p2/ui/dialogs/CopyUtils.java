/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.core.expressions.*;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.p2.ui.ICopyable;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.swt.IFocusService;

public class CopyUtils {
	public static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
	public static final String DELIMITER = "\t"; //$NON-NLS-1$
	private static final String NESTING_INDENT = "  "; //$NON-NLS-1$

	// We never test the control ID so we can use the same ID for all controls
	private static final String CONTROL_ID = "org.eclipse.equinox.p2.ui.CopyControlId"; //$NON-NLS-1$

	public static String getIndentedClipboardText(Object[] elements, IUDetailsLabelProvider labelProvider) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < elements.length; i++) {
			if (i > 0)
				buffer.append(NEWLINE);
			appendIndention(buffer, elements[i]);
			buffer.append(labelProvider.getClipboardText(elements[i], DELIMITER));
		}
		return buffer.toString();
	}

	/**
	 * Install a copy popup menu on the specified control and activate the copy handler for the control when
	 * the control has focus.  The handler will be deactivated when the control is disposed.
	 * 
	 * @param copyable the copyable that will perform the copy
	 * @param control  the control on which to install the menu and handler
	 */
	public static void activateCopy(ICopyable copyable, final Control control) {
		IFocusService fs = PlatformUI.getWorkbench().getService(IFocusService.class);
		final IHandlerService hs = PlatformUI.getWorkbench().getService(IHandlerService.class);
		new CopyPopup(copyable, control);
		if (fs != null && hs != null) {
			fs.addFocusTracker(control, CONTROL_ID);
			final IHandlerActivation handlerActivation = hs.activateHandler(CopyHandler.ID, new CopyHandler(copyable), new Expression() {
				public EvaluationResult evaluate(IEvaluationContext context) {
					return context.getVariable(ISources.ACTIVE_FOCUS_CONTROL_NAME) == control ? EvaluationResult.TRUE : EvaluationResult.FALSE;
				}

				public void collectExpressionInfo(final ExpressionInfo info) {
					info.addVariableNameAccess(ISources.ACTIVE_FOCUS_CONTROL_NAME);
				}

			});
			control.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					hs.deactivateHandler(handlerActivation);
				}
			});
		}
	}

	private static void appendIndention(StringBuffer buffer, Object element) {
		Object parent;
		while (element instanceof ProvElement && (parent = ((ProvElement) element).getParent(element)) != null) {
			buffer.append(NESTING_INDENT);
			element = parent;
		}

	}
}