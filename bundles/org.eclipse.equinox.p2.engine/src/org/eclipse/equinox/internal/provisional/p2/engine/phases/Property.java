package org.eclipse.equinox.internal.provisional.p2.engine.phases;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

public class Property extends Phase {

	public class ProfilePropertyAction extends ProvisioningAction {

		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			PropertyOperand propertyOperand = (PropertyOperand) parameters.get(PARM_OPERAND);

			if (propertyOperand.second() == null)
				removeProfileProperty(profile, propertyOperand);
			else
				setProfileProperty(profile, propertyOperand, false);

			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			PropertyOperand propertyOperand = (PropertyOperand) parameters.get(PARM_OPERAND);

			if (propertyOperand.first() == null)
				removeProfileProperty(profile, propertyOperand);
			else
				setProfileProperty(profile, propertyOperand, true);

			return null;
		}

		private void setProfileProperty(Profile profile, PropertyOperand propertyOperand, boolean undo) {

			String value = (String) (undo ? propertyOperand.first() : propertyOperand.second());

			if (propertyOperand instanceof InstallableUnitPropertyOperand) {
				InstallableUnitPropertyOperand iuPropertyOperand = (InstallableUnitPropertyOperand) propertyOperand;
				profile.setInstallableUnitProperty(iuPropertyOperand.getInstallableUnit(), iuPropertyOperand.getKey(), value);
			} else {
				profile.setProperty(propertyOperand.getKey(), value);
			}
		}

		private void removeProfileProperty(Profile profile, PropertyOperand propertyOperand) {
			if (propertyOperand instanceof InstallableUnitPropertyOperand) {
				InstallableUnitPropertyOperand iuPropertyOperand = (InstallableUnitPropertyOperand) propertyOperand;
				profile.removeInstallableUnitProperty(iuPropertyOperand.getInstallableUnit(), iuPropertyOperand.getKey());
			} else {
				profile.removeProperty(propertyOperand.getKey());
			}
		}
	}

	public class UpdateInstallableUnitProfilePropertiesAction extends ProvisioningAction {

		// we do not need to use a memento here since the profile is not persisted unless the operation is successful
		Map originalSourceProperties;
		Map originalTargetProperties;

		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			InstallableUnitOperand iuOperand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);

			IInstallableUnit source = iuOperand.first();
			originalSourceProperties = profile.getInstallableUnitProperties(source);

			IInstallableUnit target = iuOperand.second();
			originalTargetProperties = profile.getInstallableUnitProperties(target);

			profile.addInstallableUnitProperties(target, originalSourceProperties);
			profile.clearInstallableUnitProperties(source);

			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			InstallableUnitOperand iuOperand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);

			IInstallableUnit source = iuOperand.first();
			profile.clearInstallableUnitProperties(source);
			profile.addInstallableUnitProperties(source, originalSourceProperties);

			IInstallableUnit target = iuOperand.second();
			profile.clearInstallableUnitProperties(target);
			profile.addInstallableUnitProperties(target, originalTargetProperties);

			return null;
		}
	}

	public class RemoveInstallableUnitProfilePropertiesAction extends ProvisioningAction {

		// we do not need to use a memento here since the profile is not persisted unless the operation is successful
		Map originalSourceProperties;
		Map originalTargetProperties;

		public IStatus execute(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			InstallableUnitOperand iuOperand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);

			IInstallableUnit source = iuOperand.first();
			originalSourceProperties = profile.getInstallableUnitProperties(source);
			profile.clearInstallableUnitProperties(source);

			return null;
		}

		public IStatus undo(Map parameters) {
			Profile profile = (Profile) parameters.get(PARM_PROFILE);
			InstallableUnitOperand iuOperand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);

			IInstallableUnit source = iuOperand.first();
			profile.clearInstallableUnitProperties(source);
			profile.addInstallableUnitProperties(source, originalSourceProperties);

			return null;
		}
	}

	private static final String PHASE_ID = "property"; //$NON-NLS-1$

	public Property(int weight) {
		super(PHASE_ID, weight);
	}

	protected ProvisioningAction[] getActions(Operand operand) {
		if (operand instanceof PropertyOperand)
			return new ProvisioningAction[] {new ProfilePropertyAction()};

		if (operand instanceof InstallableUnitOperand) {
			InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;
			if (iuOperand.first() != null) {
				if (iuOperand.second() != null) {
					return new ProvisioningAction[] {new UpdateInstallableUnitProfilePropertiesAction()};
				}
				return new ProvisioningAction[] {new RemoveInstallableUnitProfilePropertiesAction()};
			}
		}
		return null;
	}
}
