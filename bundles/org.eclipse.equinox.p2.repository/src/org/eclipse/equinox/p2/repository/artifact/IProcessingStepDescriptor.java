/******************************************************************************* 
* Copyright (c) 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.repository.artifact;

import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;

/**
 * Describes a processing step. Processing steps are pieces of code that participate
 * in the the transfer of an artifact between artifact repositories. A step may alter
 * the shape of the artifact from its storage format in the repository (such as performing
 * compression), or it may perform additional checks on the transferred bytes such as 
 * checksums or signature verification.
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients. Instead subclass the {@link ProcessingStepDescriptor}.
 * @see IArtifactDescriptor#getProcessingSteps()
 * @since 2.0
 */
public interface IProcessingStepDescriptor {

	/**
	 * Returns the fully qualified id of the processing step extension.
	 * 
	 * @return The fully qualified processing step extension id
	 */
	public abstract String getProcessorId();

	/**
	 * An argument that is passed to the processing step instance. The structure
	 * and content of the data is specific to the particular processing step being used.
	 * @return the processing step data
	 */
	public abstract String getData();

	/**
	 * Returns whether the successful execution of this processing step is
	 * required for the transfer to be successful. If the processing step extension
	 * is not installed, or fails to execute, then the artifact transfer will fail if the
	 * step is required. Failure of optional steps will result in warnings but not prevent
	 * the transfer from succeeding.
	 * 
	 * @return <code>true</code> if the transfer will fail if this step does not succeed,
	 * and <code>false</code> otherwise
	 */
	public abstract boolean isRequired();

}