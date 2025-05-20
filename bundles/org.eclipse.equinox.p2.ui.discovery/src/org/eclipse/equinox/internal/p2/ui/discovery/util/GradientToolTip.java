/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
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
package org.eclipse.equinox.internal.p2.ui.discovery.util;

import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryUi;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * A custom {@link ToolTip} that applies a gradient to the contents.
 *
 * @author Shawn Minto
 */
public abstract class GradientToolTip extends ToolTip {

	private CommonColors colors;

	public GradientToolTip(Control control, int style, boolean manualActivation) {
		super(control, style, manualActivation);
		initResources(control);
	}

	public GradientToolTip(Control control) {
		super(control);
		initResources(control);
	}

	private void initResources(Control control) {
		colors = DiscoveryUi.getCommonsColors();
	}

	@Override
	protected final Composite createToolTipContentArea(Event event, final Composite parent) {
		GradientCanvas gradient = new GradientCanvas(parent, SWT.NONE);
		gradient.setSeparatorVisible(false);
		GridLayout headLayout = new GridLayout();
		headLayout.marginHeight = 0;
		headLayout.marginWidth = 0;
		headLayout.horizontalSpacing = 0;
		headLayout.verticalSpacing = 0;
		headLayout.numColumns = 1;
		gradient.setLayout(headLayout);

		gradient.setBackgroundGradient(new Color[] {colors.getGradientBegin(), colors.getGradientEnd()}, new int[] {100}, true);

		createToolTipArea(event, gradient);

		// force a null background so that the gradient shines through
		for (Control c : gradient.getChildren()) {
			setNullBackground(c);
		}

		return gradient;
	}

	private void setNullBackground(final Control outerCircle) {
		outerCircle.setBackground(null);
		if (outerCircle instanceof Composite) {
			((Composite) outerCircle).setBackgroundMode(SWT.INHERIT_FORCE);
			for (Control c : ((Composite) outerCircle).getChildren()) {
				setNullBackground(c);
			}
		}
	}

	protected abstract Composite createToolTipArea(Event event, Composite parent);
}
