/*******************************************************************************
* Copyright (c) 2007, 2010 compeople AG and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	compeople AG (Stefan Liebig) - initial API and implementation
*******************************************************************************/
package org.eclipse.equinox.p2.repository.artifact.spi;

import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;

/**
 * @since 2.0
 */
public class ProcessingStepDescriptor implements IProcessingStepDescriptor {

	private final String processorId; //the operation to be applied (e.g: unpack, md5, signature verification, etc.)
	private final String data; //data requested for the processing (eg. expected checksum)
	private final boolean required; //whether the step is optional or not

	/**
	 * Create a processing step description.
	 * 
	 * @param processorId
	 * @param data
	 * @param required
	 */
	public ProcessingStepDescriptor(String processorId, String data, boolean required) {
		super();
		this.processorId = processorId;
		this.data = data;
		this.required = required;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getProcessorId() {
		return processorId;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getData() {
		return data;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((processorId == null) ? 0 : processorId.hashCode());
		result = prime * result + (required ? 1231 : 1237);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof IProcessingStepDescriptor))
			return false;
		final IProcessingStepDescriptor other = (IProcessingStepDescriptor) obj;
		if (data == null) {
			if (other.getData() != null)
				return false;
		} else if (!data.equals(other.getData()))
			return false;
		if (processorId == null) {
			if (other.getProcessorId() != null)
				return false;
		} else if (!processorId.equals(other.getProcessorId()))
			return false;
		if (required != other.isRequired())
			return false;
		return true;
	}

	/**
	 * Returns a string representation of this descriptor for debugging purposes only.
	 * @return a string representation ofthe processing step descriptor
	 */
	public String toString() {
		return "Processor: " + processorId + (required ? "(req)" : "(notReq)") + " ,data: " + data; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
	}
}
