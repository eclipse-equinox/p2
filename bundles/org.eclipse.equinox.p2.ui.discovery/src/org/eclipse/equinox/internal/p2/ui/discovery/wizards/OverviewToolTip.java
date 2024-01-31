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

package org.eclipse.equinox.internal.p2.ui.discovery.wizards;

import java.net.URL;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.model.Overview;
import org.eclipse.equinox.internal.p2.ui.discovery.util.GradientToolTip;
import org.eclipse.equinox.internal.p2.ui.discovery.util.WorkbenchUtil;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * @author David Green
 */
class OverviewToolTip extends GradientToolTip {

	private final Overview overview;

	private final AbstractCatalogSource source;

	private final Control parent;

	private final Image leftImage;

	public OverviewToolTip(Control control, AbstractCatalogSource source, Overview overview, Image leftImage) {
		super(control, ToolTip.RECREATE, true);
		Assert.isNotNull(source);
		Assert.isNotNull(overview);
		this.parent = control;
		this.source = source;
		this.overview = overview;
		this.leftImage = leftImage;
		setHideOnMouseDown(false); // required for links to work
	}

	@Override
	protected Composite createToolTipArea(Event event, final Composite parentComposite) {
		GridLayoutFactory.fillDefaults().applyTo(parentComposite);

		Composite container = new Composite(parentComposite, SWT.NULL);
		container.setBackground(null);

		Image image = null;
		if (overview.getScreenshot() != null) {
			image = computeImage(source, overview.getScreenshot());
			if (image != null) {
				final Image fimage = image;
				container.addDisposeListener(e -> fimage.dispose());
			}
		}
		final boolean hasLearnMoreLink = overview.getUrl() != null && overview.getUrl().length() > 0;

		final int borderWidth = 1;
		final int fixedImageHeight = 240;
		final int fixedImageWidth = 320;
		final int heightHint = fixedImageHeight + (borderWidth * 2);
		final int widthHint = fixedImageWidth;

		final int containerWidthHintWithImage = 650;
		final int containerWidthHintWithoutImage = 500;

		GridDataFactory.fillDefaults().grab(true, true).hint(image == null ? containerWidthHintWithoutImage : containerWidthHintWithImage, SWT.DEFAULT).applyTo(container);

		GridLayoutFactory.fillDefaults().numColumns((leftImage != null) ? 3 : 2).margins(5, 5).spacing(3, 0).applyTo(container);

		if (leftImage != null) {
			Label imageLabel = new Label(container, SWT.NONE);
			imageLabel.setImage(leftImage);
			int imageWidthHint = leftImage.getBounds().width + 5;
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).hint(imageWidthHint, SWT.DEFAULT).applyTo(imageLabel);
		}

		String summary = overview.getSummary();

		Composite summaryContainer = new Composite(container, SWT.NULL);
		summaryContainer.setBackground(null);
		GridLayoutFactory.fillDefaults().applyTo(summaryContainer);

		GridDataFactory gridDataFactory = GridDataFactory.fillDefaults().grab(true, true).span(image == null ? 2 : 1, 1);
		if (image != null) {
			gridDataFactory.hint(widthHint, heightHint);
		}
		gridDataFactory.applyTo(summaryContainer);

		StyledText summaryLabel = new StyledText(summaryContainer, SWT.WRAP | SWT.READ_ONLY | SWT.NO_FOCUS);
		summaryLabel.setText(summary);
		Point size = summaryLabel.computeSize(widthHint, SWT.DEFAULT);
		if (size.y > heightHint - 20) {
			summaryLabel.dispose();
			summaryLabel = new StyledText(summaryContainer, SWT.WRAP | SWT.READ_ONLY | SWT.NO_FOCUS | SWT.V_SCROLL);
			summaryLabel.setText(summary);
		}
		summaryLabel.setBackground(null);

		GridDataFactory.fillDefaults().grab(true, true).align(SWT.BEGINNING, SWT.BEGINNING).applyTo(summaryLabel);

		if (image != null) {
			final Composite imageContainer = new Composite(container, SWT.BORDER);
			GridLayoutFactory.fillDefaults().applyTo(imageContainer);

			GridDataFactory.fillDefaults().grab(false, false).align(SWT.CENTER, SWT.BEGINNING).hint(widthHint + (borderWidth * 2), heightHint).applyTo(imageContainer);

			Label imageLabel = new Label(imageContainer, SWT.NULL);
			GridDataFactory.fillDefaults().hint(widthHint, fixedImageHeight).indent(borderWidth, borderWidth).applyTo(imageLabel);
			imageLabel.setImage(image);
			imageLabel.setBackground(null);
			imageLabel.setSize(widthHint, fixedImageHeight);

			// creates a border
			imageContainer.setBackground(parentComposite.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		}
		if (hasLearnMoreLink) {
			Link link = new Link(summaryContainer, SWT.NULL);
			GridDataFactory.fillDefaults().grab(false, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(link);
			link.setText(Messages.ConnectorDescriptorToolTip_detailsLink);
			link.setBackground(null);
			link.setToolTipText(NLS.bind(Messages.ConnectorDescriptorToolTip_detailsLink_tooltip, overview.getUrl()));
			link.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					WorkbenchUtil.openUrl(overview.getUrl(), IWorkbenchBrowserSupport.AS_EXTERNAL);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
		}
		if (image == null) {
			// prevent overviews with no image from providing unlimited text.
			Point optimalSize = summaryContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			if (optimalSize.y > (heightHint + 10)) {
				((GridData) summaryContainer.getLayoutData()).heightHint = heightHint;
				container.layout(true);
			}
		}
		// hack: cause the tooltip to gain focus so that we can capture the escape key
		//       this must be done async since the tooltip is not yet visible.
		Display.getCurrent().asyncExec(() -> {
			if (!parentComposite.isDisposed()) {
				parentComposite.setFocus();
			}
		});
		return container;
	}

	private Image computeImage(AbstractCatalogSource discoverySource, String imagePath) {
		URL resource = discoverySource.getResource(imagePath);
		if (resource != null) {
			ImageDescriptor descriptor = ImageDescriptor.createFromURL(resource);
			Image image = descriptor.createImage();
			return image;
		}
		return null;
	}

	public void show(Control titleControl) {
		Point titleAbsLocation = titleControl.getParent().toDisplay(titleControl.getLocation());
		Point containerAbsLocation = parent.getParent().toDisplay(parent.getLocation());
		Rectangle bounds = titleControl.getBounds();
		int relativeX = titleAbsLocation.x - containerAbsLocation.x;
		int relativeY = titleAbsLocation.y - containerAbsLocation.y;

		relativeY += bounds.height + 3;
		show(new Point(relativeX, relativeY));
	}

}
