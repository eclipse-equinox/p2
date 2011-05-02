/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.mirroring;

import java.io.*;
import java.util.Date;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.internal.p2.persistence.XMLWriter;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public class XMLMirrorLog implements IArtifactMirrorLog {
	private static final String DEFAULT_FORMAT = "canonical"; //$NON-NLS-1$
	// Constants used in XML tags
	private static final String LOG = "log"; //$NON-NLS-1$
	private static final String TIME_ATTRIBUTE = "time"; //$NON-NLS-1$
	private static final String DESCRIPTOR_ELEMENT = "descriptor"; //$NON-NLS-1$
	private static final String DESCRIPTOR_CLASSIFIER_ATTRIBUTE = "classifier"; //$NON-NLS-1$
	private static final String DESCRIPTOR_FORMAT_ATTRIBUTE = "format"; //$NON-NLS-1$
	private static final String DESCRIPTOR_ID_ATTRIBUTE = "id"; //$NON-NLS-1$
	private static final String DESCRIPTOR_VERSION_ATTRIBUTE = "version"; //$NON-NLS-1$
	private static final String STATUS_ELEMENT = "status"; //$NON-NLS-1$
	private static final String STATUS_SEVERITY_ATTRIBUTE = "severity"; //$NON-NLS-1$
	private static final String STATUS_MESSAGE_ATTRIBUTE = "message"; //$NON-NLS-1$

	private int minStatus = IStatus.OK;
	private XMLWriter writer;
	private OutputStream outputStream;
	private boolean consoleMessage = false;

	public XMLMirrorLog(String location, int minStatus, String root) {
		this.minStatus = minStatus;

		try {
			outputStream = new FileOutputStream(location);
			writer = new XMLWriter(outputStream, null);
			if (root != null)
				writer.start(root.toLowerCase());
			else
				writer.start(LOG);
			writer.attribute(TIME_ATTRIBUTE, new Date());
		} catch (UnsupportedEncodingException e) {
			exceptionOccurred(e);
		} catch (FileNotFoundException e) {
			exceptionOccurred(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.artifact.mirror.IArtifactMirrorLog#log(org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor, org.eclipse.core.runtime.IStatus)
	 */
	public void log(IArtifactDescriptor descriptor, IStatus status) {
		if (status.getSeverity() < minStatus)
			return;
		// Start descriptor tag
		if (writer != null) {
			writer.start(DESCRIPTOR_ELEMENT);
			writer.attribute(DESCRIPTOR_ID_ATTRIBUTE, descriptor.getArtifactKey().getId());
			writer.attribute(DESCRIPTOR_CLASSIFIER_ATTRIBUTE, descriptor.getArtifactKey().getClassifier());
			writer.attribute(DESCRIPTOR_VERSION_ATTRIBUTE, descriptor.getArtifactKey().getVersion());
			if (descriptor.getProperties().get(IArtifactDescriptor.FORMAT) != null)
				writer.attribute(DESCRIPTOR_FORMAT_ATTRIBUTE, descriptor.getProperties().get(IArtifactDescriptor.FORMAT));
			else
				writer.attribute(DESCRIPTOR_FORMAT_ATTRIBUTE, DEFAULT_FORMAT);
		} else
			// Creation of the XML writer failed, dump results to the console
			System.out.println(descriptor);

		log(status);

		// Close descriptor tag
		if (writer != null)
			writer.end();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.artifact.mirror.IArtifactMirrorLog#log(org.eclipse.core.runtime.IStatus)
	 */
	public void log(IStatus status) {
		if (status.getSeverity() < minStatus)
			return;

		if (writer != null) {
			// Start status tag
			writer.start(STATUS_ELEMENT);
			// Set severity attribute
			switch (status.getSeverity()) {
				case IStatus.OK :
					writer.attribute(STATUS_SEVERITY_ATTRIBUTE, "OK"); //$NON-NLS-1$
					break;
				case IStatus.INFO :
					writer.attribute(STATUS_SEVERITY_ATTRIBUTE, "INFO"); //$NON-NLS-1$
					break;
				case IStatus.WARNING :
					writer.attribute(STATUS_SEVERITY_ATTRIBUTE, "WARNING"); //$NON-NLS-1$
					break;
				case IStatus.ERROR :
					writer.attribute(STATUS_SEVERITY_ATTRIBUTE, "ERROR"); //$NON-NLS-1$
					break;
				case IStatus.CANCEL :
					writer.attribute(STATUS_SEVERITY_ATTRIBUTE, "CANCEL"); //$NON-NLS-1$
					break;
				default :
					writer.attribute(STATUS_SEVERITY_ATTRIBUTE, status.getSeverity());
			}
			// Set  message attribute
			writer.attribute(STATUS_MESSAGE_ATTRIBUTE, status.getMessage());
		} else
			// Creation of the XML writer failed, dump results to the console
			System.out.println(status);

		// Log children statuses
		IStatus[] nestedStatus = status.getChildren();
		if (nestedStatus != null)
			for (int i = 0; i < nestedStatus.length; i++)
				log(nestedStatus[i]);

		// Close status tag
		if (writer != null)
			writer.end();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.artifact.mirror.IArtifactMirrorLog#close()
	 */
	public void close() {
		try {
			if (writer != null) {
				// Close opening tag & flush results
				writer.end();
				writer.flush();
			}
		} finally {
			if (outputStream != null)
				try {
					// Close output stream
					outputStream.close();
				} catch (IOException e) {
					exceptionOccurred(e);
				}
		}
	}

	/*
	 * Show an error message if this the first time
	 */
	private void exceptionOccurred(Exception e) {
		if (!consoleMessage) {
			System.err.println(Messages.MirrorLog_Exception_Occurred);
			e.printStackTrace(System.err);
			System.err.println(Messages.MirrorLog_Console_Log);
			consoleMessage = true;
		}
	}
}
