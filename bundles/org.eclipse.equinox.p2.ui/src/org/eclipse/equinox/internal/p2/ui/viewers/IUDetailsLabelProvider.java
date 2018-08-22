/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.viewers;

import java.text.NumberFormat;
import java.util.HashMap;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;

/**
 * Label provider for showing IU's in a table.  Clients can configure
 * what is shown in each column.
 * 
 * @since 3.4
 */
public class IUDetailsLabelProvider extends ColumnLabelProvider implements ITableLabelProvider, IFontProvider {
	final static int PRIMARY_COLUMN = 0;
	final static String BLANK = ""; //$NON-NLS-1$
	private String toolTipProperty = null;
	private FilteredTree filteredTree;
	private boolean useBoldFont = false;
	private boolean showingId = false;

	private IUColumnConfig[] columnConfig;
	Shell shell;
	HashMap<IIUElement, Job> jobs = new HashMap<>();

	public IUDetailsLabelProvider() {
		this(null, null, null);
	}

	public IUDetailsLabelProvider(FilteredTree filteredTree, IUColumnConfig[] columnConfig, Shell shell) {
		this.filteredTree = filteredTree;
		if (columnConfig == null)
			this.columnConfig = ProvUI.getIUColumnConfig();
		else
			this.columnConfig = columnConfig;
		for (IUColumnConfig config : this.columnConfig)
			if (config.getColumnType() == IUColumnConfig.COLUMN_ID) {
				showingId = true;
				break;
			}
		this.shell = shell;
	}

	@Override
	public String getText(Object obj) {
		return getColumnText(obj, PRIMARY_COLUMN);
	}

	@Override
	public Image getImage(Object obj) {
		return getColumnImage(obj, PRIMARY_COLUMN);
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		int columnContent = IUColumnConfig.COLUMN_ID;
		if (columnIndex < columnConfig.length) {
			columnContent = columnConfig[columnIndex].getColumnType();
		}

		IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
		if (iu == null) {
			if (columnIndex == 0) {
				if (element instanceof ProvElement)
					return ((ProvElement) element).getLabel(element);
				return element.toString();
			}
			return BLANK;
		}

		switch (columnContent) {
			case IUColumnConfig.COLUMN_ID :
				return iu.getId();
			case IUColumnConfig.COLUMN_NAME :
				// Get the iu name in the current locale
				String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
				if (name != null)
					return name;
				// If the iu name is not available, we return blank if we know know we are
				// showing id in another column.  Otherwise we return id so the user doesn't
				// see blank iu's.  
				if (showingId)
					return BLANK;
				return iu.getId();
			case IUColumnConfig.COLUMN_DESCRIPTION :
				// Get the iu description in the current locale
				String description = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
				if (description != null)
					return description;
				return BLANK;
			case IUColumnConfig.COLUMN_VERSION :
				// If it's an element, determine if version should be shown
				if (element instanceof IIUElement) {
					if (((IIUElement) element).shouldShowVersion())
						return iu.getVersion().toString();
					return BLANK;
				}
				// It's a raw IU, return the version
				return iu.getVersion().toString();
			case IUColumnConfig.COLUMN_PROVIDER :
				return iu.getProperty(IInstallableUnit.PROP_PROVIDER, null);
			case IUColumnConfig.COLUMN_SIZE :
				if (element instanceof IIUElement && ((IIUElement) element).shouldShowSize())
					return getIUSize((IIUElement) element);
				return BLANK;
		}
		return BLANK;
	}

	@Override
	public Image getColumnImage(Object element, int index) {
		if (index == PRIMARY_COLUMN) {
			if (element instanceof ProvElement)
				return ((ProvElement) element).getImage(element);
			if (ProvUI.getAdapter(element, IInstallableUnit.class) != null)
				return ProvUIImages.getImage(ProvUIImages.IMG_IU);
		}
		return null;
	}

	private String getIUSize(final IIUElement element) {
		long size = element.getSize();
		// If size is already known, or we already tried
		// to get it, don't try again
		if (size != ProvUI.SIZE_UNKNOWN)
			return getFormattedSize(size);
		if (!jobs.containsKey(element)) {
			Job resolveJob = new Job(element.getIU().getId()) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;

					if (shell == null || shell.isDisposed())
						return Status.CANCEL_STATUS;

					element.computeSize(monitor);

					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;

					// If we still could not compute size, give up
					if (element.getSize() == ProvUI.SIZE_UNKNOWN)
						return Status.OK_STATUS;

					if (shell == null || shell.isDisposed())
						return Status.CANCEL_STATUS;

					shell.getDisplay().asyncExec(() -> {
						if (shell != null && !shell.isDisposed())
							fireLabelProviderChanged(new LabelProviderChangedEvent(IUDetailsLabelProvider.this, element));
					});

					return Status.OK_STATUS;
				}
			};
			jobs.put(element, resolveJob);
			resolveJob.setSystem(true);
			resolveJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					jobs.remove(element);
				}
			});
			resolveJob.schedule();
		}
		return ProvUIMessages.IUDetailsLabelProvider_ComputingSize;
	}

	private String getFormattedSize(long size) {
		if (size == ProvUI.SIZE_UNKNOWN || size == ProvUI.SIZE_UNAVAILABLE)
			return ProvUIMessages.IUDetailsLabelProvider_Unknown;
		if (size > 1000L) {
			long kb = size / 1000L;
			return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_KB, NumberFormat.getInstance().format(Long.valueOf(kb)));
		}
		return NLS.bind(ProvUIMessages.IUDetailsLabelProvider_Bytes, NumberFormat.getInstance().format(Long.valueOf(size)));
	}

	public void setToolTipProperty(String propertyName) {
		toolTipProperty = propertyName;
	}

	public String getClipboardText(Object element, String columnDelimiter) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < columnConfig.length; i++) {
			if (i != 0)
				result.append(columnDelimiter);
			result.append(getColumnText(element, i));
		}
		return result.toString();
	}

	public void setUseBoldFontForFilteredItems(boolean useBoldFont) {
		this.useBoldFont = useBoldFont;
	}

	@Override
	public String getToolTipText(Object element) {
		IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
		if (iu == null || toolTipProperty == null)
			return null;
		return iu.getProperty(toolTipProperty, null);
	}

	@Override
	public Font getFont(Object element) {
		if (filteredTree != null && useBoldFont) {
			return FilteredTree.getBoldFont(element, filteredTree, filteredTree.getPatternFilter());
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Job job : jobs.values())
			job.cancel();
	}

}
