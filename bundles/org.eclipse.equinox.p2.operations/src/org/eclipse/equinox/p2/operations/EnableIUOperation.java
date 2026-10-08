/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.operations.IFailedStatusEvaluator;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * An {@link EnableIUOperation} re-enables previously disabled
 * {@link IInstallableUnit}s by restoring {@link IProfile#PROP_PROFILE_ROOT_IU}.
 * No files are downloaded — the IU already exists on disk.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.8
 */
public class EnableIUOperation extends ProfileChangeOperation {

	private final Collection<IInstallableUnit> toEnable;

	public EnableIUOperation(ProvisioningSession session, Collection<IInstallableUnit> toEnable) {
		super(session);
		this.toEnable = toEnable;
	}

	@Override
	protected void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor) {
		request = ProfileChangeRequest.createByProfileId(session.getProvisioningAgent(), profileId);
		for (IInstallableUnit iu : toEnable) {
			request.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
			request.removeInstallableUnitProfileProperty(iu, DisableIUOperation.PROP_DISABLED);
		}
	}

	@Override
	protected String getProvisioningJobName() {
		return Messages.EnableOperation_ProvisioningJobName;
	}

	@Override
	protected String getResolveJobName() {
		return Messages.EnableOperation_ResolveJobName;
	}

	@Override
	ProvisioningContext getFirstPassProvisioningContext() {
		ProvisioningContext pc = new ProvisioningContext(session.getProvisioningAgent());
		pc.setMetadataRepositories();
		pc.setArtifactRepositories();
		return pc;
	}

	@Override
	IFailedStatusEvaluator getSecondPassEvaluator() {
		return failedPlan -> context;
	}
}
