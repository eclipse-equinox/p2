package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.net.MalformedURLException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.*;

public class UpdateSingleIUPage extends ProvisioningWizardPage implements ISelectableIUsPage {

	UpdateOperation operation;

	protected UpdateSingleIUPage(UpdateOperation operation, ProvisioningUI ui, ProvisioningOperationWizard wizard) {
		super("UpdateSingleIUPage", ui, wizard); //$NON-NLS-1$
		Assert.isNotNull(operation);
		Assert.isTrue(operation.hasResolved());
		Assert.isTrue(operation.getSelectedUpdates().length == 1);
		Assert.isTrue(operation.getResolutionResult().isOK());
		this.operation = operation;
	}

	public void createControl(Composite parent) {
		IInstallableUnit updateIU = getUpdate().replacement;
		String url = null;
		if (updateIU.getUpdateDescriptor().getLocation() != null)
			try {
				url = URIUtil.toURL(updateIU.getUpdateDescriptor().getLocation()).toExternalForm();
			} catch (MalformedURLException e) {
				// ignore and null URL will be ignored below
			}
		if (url != null) {
			Browser browser = null;
			try {
				browser = new Browser(parent, SWT.NONE);
				browser.setUrl(url);
				return;
			} catch (SWTError e) {
				// Fall through to backup plan.
			}
		}
		// Create a text description of the update.
		Text text = new Text(parent, SWT.MULTI | SWT.V_SCROLL);
		text.setText(getUpdateText(updateIU));
	}

	private String getUpdateText(IInstallableUnit iu) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(new IUDetailsLabelProvider().getClipboardText(getCheckedIUElements()[0], CopyUtils.DELIMITER));
		buffer.append(CopyUtils.NEWLINE);
		buffer.append(CopyUtils.NEWLINE);
		String text = iu.getUpdateDescriptor().getDescription();
		if (text != null)
			buffer.append(text);
		else {
			text = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION);
			if (text != null)
				buffer.append(text);
		}
		return buffer.toString();

	}

	public Object[] getCheckedIUElements() {
		Update update = getUpdate();
		return new Object[] {new AvailableUpdateElement(null, update.replacement, update.toUpdate, getProfileId(), false)};
	}

	public Object[] getSelectedIUElements() {
		return getCheckedIUElements();
	}

	public void setCheckedElements(Object[] elements) {
		// ignored
	}

	@Override
	protected String getClipboardText(Control control) {
		return getUpdate().toString();
	}

	private Update getUpdate() {
		return operation.getSelectedUpdates()[0];
	}

}
