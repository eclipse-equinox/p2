/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.discovery.wizards;

import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.model.Overview;
import org.eclipse.equinox.internal.p2.ui.discovery.util.ControlListItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/**
 * @author Steffen Pingel
 */
public abstract class AbstractDiscoveryItem<T> extends ControlListItem<T> {

	protected final DiscoveryResources resources;

	public AbstractDiscoveryItem(Composite parent, int style, DiscoveryResources resources, T element) {
		super(parent, style, element);
		this.resources = resources;
	}

	private void hookRecursively(Control control, Listener listener) {
		control.addListener(SWT.Dispose, listener);
		control.addListener(SWT.MouseHover, listener);
		control.addListener(SWT.MouseMove, listener);
		control.addListener(SWT.MouseExit, listener);
		control.addListener(SWT.MouseDown, listener);
		control.addListener(SWT.MouseWheel, listener);
		if (control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) {
				hookRecursively(child, listener);
			}
		}
	}

	protected void hookTooltip(final Control parent, final Widget tipActivator, final Control exitControl, final Control titleControl, AbstractCatalogSource source, Overview overview, Image image) {
		final OverviewToolTip toolTip = new OverviewToolTip(parent, source, overview, image);
		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
					case SWT.MouseHover :
						toolTip.show(titleControl);
						break;
					case SWT.Dispose :
					case SWT.MouseWheel :
						toolTip.hide();
						break;
				}

			}
		};
		tipActivator.addListener(SWT.Dispose, listener);
		tipActivator.addListener(SWT.MouseWheel, listener);
		if (image != null) {
			tipActivator.addListener(SWT.MouseHover, listener);
		}
		Listener selectionListener = new Listener() {
			public void handleEvent(Event event) {
				toolTip.show(titleControl);
			}
		};
		tipActivator.addListener(SWT.Selection, selectionListener);
		Listener exitListener = new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
					case SWT.MouseWheel :
						toolTip.hide();
						break;
					case SWT.MouseExit :
						/*
						 * Check if the mouse exit happened because we move over the
						 * tooltip
						 */
						Rectangle containerBounds = exitControl.getBounds();
						Point displayLocation = exitControl.getParent().toDisplay(containerBounds.x, containerBounds.y);
						containerBounds.x = displayLocation.x;
						containerBounds.y = displayLocation.y;
						if (containerBounds.contains(Display.getCurrent().getCursorLocation())) {
							break;
						}
						toolTip.hide();
						break;
				}
			}
		};
		hookRecursively(exitControl, exitListener);
	}

	@Override
	public void updateColors(int index) {
		// ignore
	}

}
