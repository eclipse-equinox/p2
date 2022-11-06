/*******************************************************************************
 * Copyright (c) 2018, 2018 Mykola Nikishov.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mykola Nikishov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.checksum;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;

/**
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class MessageDigestProcessingStep extends ProcessingStep {

	protected MessageDigest messageDigest;
	private static final int BUFFER_SIZE = 16 * 1024;
	private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

	@Override
	public final void write(int b) throws IOException {
		getDestination().write(b);

		boolean isBufferFull = buffer.remaining() == 0;
		if (isBufferFull) {
			processBufferredBytes();
		}

		buffer.put((byte) b);
	}

	private void processBufferredBytes() {
		buffer.flip();
		updateDigest();
		buffer.clear();
	}

	private void updateDigest() {
		messageDigest.update(buffer);
	}

	@Override
	public final void close() throws IOException {
		processBufferredBytes();
		String digestString = digest();
		onClose(digestString);
		super.close();
	}

	private String digest() {
		byte[] digestBytes = messageDigest.digest();
		return ChecksumHelper.toHexString(digestBytes);
	}

	protected abstract void onClose(String digestString);

}
