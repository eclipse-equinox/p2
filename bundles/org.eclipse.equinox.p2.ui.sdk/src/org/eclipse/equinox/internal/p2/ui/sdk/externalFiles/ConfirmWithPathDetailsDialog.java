package org.eclipse.equinox.internal.p2.ui.sdk.externalFiles;

import java.io.File;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * @since 3.4
 *
 */
public abstract class ConfirmWithPathDetailsDialog extends MessageDialogWithToggle {

	File targetLocation;
	Composite detailsArea;
	Text location;
	String pathLocationText;

	public ConfirmWithPathDetailsDialog(Shell parentShell, File targetRepoLocation, String message, IPreferenceStore prefStore, String prefKey) {
		super(parentShell, ProvSDKMessages.ProvSDKUIActivator_Question, null, message, NONE, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.SHOW_DETAILS_LABEL}, 0, null, false);
		this.targetLocation = targetRepoLocation;
		pathLocationText = targetRepoLocation.getAbsolutePath();
		setPrefStore(prefStore);
		setPrefKey(prefKey);
	}

	protected Composite createDetailsArea(final Composite parent) {
		Composite composite = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.marginLeft = layout.marginRight = layout.marginTop = layout.marginBottom = 0;
		layout.numColumns = 3;
		composite.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gd);

		// Descriptive text describing the location's usage
		Text text = new Text(composite, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		initializeDialogUnits(text);
		gd.heightHint = convertHeightInCharsToPixels(2);
		gd.horizontalSpan = 3;
		text.setLayoutData(gd);
		text.setText(getLocationDescription());

		// Location text box and browse button
		Label label = new Label(composite, SWT.NONE);
		label.setText(ProvSDKMessages.ConfirmWithPathDetails_SaveIn);
		location = new Text(composite, SWT.BORDER);
		if (pathLocationText != null)
			location.setText(pathLocationText);
		location.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pathLocationText = location.getText().trim();
			}

		});
		location.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Button locationButton = new Button(composite, SWT.PUSH);
		locationButton.setText(ProvSDKMessages.ConfirmWithPathDetails_Browse);
		locationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
				dialog.setMessage(ProvSDKMessages.ConfirmWithPathDetails_SelectDirectory);
				String dir = dialog.open();
				if (dir != null) {
					location.setText(dir);
				}
			}
		});
		return composite;
	}

	protected abstract String getLocationDescription();

	protected void buttonPressed(int id) {
		if (id == IDialogConstants.DETAILS_ID) {
			// was the details button pressed?
			toggleDetailsArea();
		} else {
			super.buttonPressed(id);
		}
	}

	/**
	 * Toggles the unfolding of the details area. This is triggered by the user
	 * pressing the details button.
	 */
	private void toggleDetailsArea() {
		Point windowSize = getShell().getSize();
		Point oldSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		if (detailsArea != null) {
			detailsArea.dispose();
			detailsArea = null;
			getDetailsButton().setText(IDialogConstants.SHOW_DETAILS_LABEL);
		} else {
			detailsArea = createDetailsArea((Composite) getContents());
			getDetailsButton().setText(IDialogConstants.HIDE_DETAILS_LABEL);
			getContents().getShell().layout();
		}
		Point newSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		getShell().setSize(new Point(windowSize.x, windowSize.y + (newSize.y - oldSize.y)));
	}

	private Button getDetailsButton() {
		return getButton(2); // index of details button as specified in the constructor
	}

	public File getTargetLocation() {
		if (pathLocationText != null) {
			File file = new File(pathLocationText);
			if (file.isDirectory() || file.mkdirs())
				return file;
		}
		return targetLocation;
	}
}