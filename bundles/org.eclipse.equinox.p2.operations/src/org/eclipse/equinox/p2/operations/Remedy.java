/*******************************************************************************
 * Copyright (c) 2013, 2017 Red Hat, Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;

/**
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same. Please do not use this API without
 * consulting with the p2 team.
 * </p>
 * @since 2.3
 * @noreference
 */
public class Remedy {

	private RemedyConfig config;
	private ProfileChangeRequest request;
	private int beingInstalledRelaxedWeight;
	private int installationRelaxedWeight;
	private final IProfileChangeRequest originalRequest;
	private final List<RemedyIUDetail> iusDetails;

	public List<RemedyIUDetail> getIusDetails() {
		return iusDetails;
	}

	public IProfileChangeRequest getOriginalRequest() {
		return originalRequest;

	}

	public Remedy(IProfileChangeRequest originalRequest) {
		this.originalRequest = originalRequest;
		this.iusDetails = new ArrayList<>();
	}

	public RemedyConfig getConfig() {
		return config;
	}

	public void setConfig(RemedyConfig config) {
		this.config = config;
	}

	public ProfileChangeRequest getRequest() {
		return request;
	}

	public void setRequest(ProfileChangeRequest request) {
		this.request = request;
	}

	public int getBeingInstalledRelaxedWeight() {
		return beingInstalledRelaxedWeight;
	}

	public void setBeingInstalledRelaxedWeight(int beingInstalledRelaxedWeight) {
		this.beingInstalledRelaxedWeight = beingInstalledRelaxedWeight;
	}

	public int getInstallationRelaxedWeight() {
		return installationRelaxedWeight;
	}

	public void setInstallationRelaxedWeight(int installationRelaxedWeight) {
		this.installationRelaxedWeight = installationRelaxedWeight;
	}

	public void addRemedyIUDetail(RemedyIUDetail iuDetail) {
		iusDetails.add(iuDetail);
	}

}