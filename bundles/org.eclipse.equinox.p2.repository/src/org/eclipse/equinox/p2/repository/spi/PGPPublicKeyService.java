/*******************************************************************************
 * Copyright (c) 2022 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.spi;

import java.util.*;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.util.encoders.Hex;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;

/**
 * A service for managing and searching {@link PGPPublicKey keys}.
 * Implementations may make use of a
 * <a href="https://datatracker.ietf.org/doc/html/draft-shaw-openpgp-hkp-00">key
 * server</a> to fetch up-to-date information about keys. Implementations should
 * generally provide support for caching and efficient lookup of keys,
 * especially lookup based on {@link PGPPublicKey#getKeyID() key ID} because
 * signatures generally use {@link PGPSignature#getKeyID() key IDs} and this is
 * the primary use case.
 *
 * <p>
 * Implementors of this service are responsible for registering the
 * implementation with the {@link IProvisioningAgent provisioning agent} either
 * {@link IProvisioningAgent#registerService(String, Object) explicitly} or via
 * an {@link IAgentServiceFactory agent service factory}.
 * </p>
 *
 * @see PGPPublicKey#getKeyID()
 * @see PGPSignature#getKeyID()
 * @see IAgentServiceFactory
 * @see IProvisioningAgent#registerService(String, Object)
 *
 * @since 2.6
 */
public abstract class PGPPublicKeyService {
	/**
	 * The name used for obtaining a reference to the key service.
	 *
	 * @see IProvisioningAgent#getService(Class)
	 * @see IProvisioningAgent#getService(String)
	 */
	public static final String SERVICE_NAME = PGPPublicKeyService.class.getName();

	/**
	 * Returns the key associated with the given
	 * {@link PGPPublicKey#getFingerprint() fingerprint}.
	 *
	 * @param fingerprint the fingerprint for which to search.
	 * @return the key with the matching fingerprint.
	 *
	 * @see PGPPublicKey#getFingerprint()
	 */
	public PGPPublicKey getKey(byte[] fingerprint) {
		return getKey(toHex(fingerprint));
	}

	/**
	 * Returns the key associated with the given
	 * {@link PGPPublicKey#getFingerprint() fingerprint} represented as a
	 * {@link #toHex(byte[]) hexadecimal} string value.
	 *
	 * @param fingerprint the fingerprint for which to search.
	 * @return the key with the matching fingerprint.
	 *
	 * @see PGPPublicKey#getFingerprint()
	 * @see #toHex(byte[])
	 */
	public abstract PGPPublicKey getKey(String fingerprint);

	/**
	 * Returns the keys associated with the given {@link PGPPublicKey#getKeyID() key
	 * ID}. In general, key ID collisions are possible so implementations must be
	 * tolerant of that.
	 *
	 * @param keyID the key ID for which to search.
	 * @return the keys with the matching key IDs.
	 *
	 * @see PGPPublicKey#getKeyID()
	 * @see PGPSignature#getKeyID()
	 */
	public abstract Collection<PGPPublicKey> getKeys(long keyID);

	/**
	 * Adds the given key to this key service. An implementations may fetch more
	 * up-to-date information about this key from a key server and may return a
	 * different key than the one passed in here. In general an implementation may
	 * also return an existing key, with the same fingerprint, already known to the
	 * key service.
	 *
	 * @param key the key to add.
	 * @return the normalized key available in this key service.
	 */
	public abstract PGPPublicKey addKey(PGPPublicKey key);

	/**
	 * Returns the set of keys that have been verified to have signed the given key.
	 * These are the links in the web of trust.
	 *
	 * @param key the key for which to find keys that have signed it.
	 * @return the set of keys that have been verified to have signed the given key.
	 *
	 * @see PGPSignature#verifyCertification(String, PGPPublicKey)
	 * @see PGPSignature#verifyCertification(PGPPublicKey, PGPPublicKey)
	 */
	public abstract Set<PGPPublicKey> getVerifiedCertifications(PGPPublicKey key);

	/**
	 * If this key has a revocation signature that is verified to have been signed
	 * by the public key of that revocation signature, this returns the
	 * {@link PGPSignature#getCreationTime() creation time} of that signature,
	 * otherwise it returns <code>null</code>.
	 *
	 * @param key the key to test for revocation.
	 * @return when this key was verifiably revoked, or <code>null</code> if it is
	 *         not revoked.
	 *
	 * @see PGPSignature#getKeyID()
	 * @see PGPPublicKey#hasRevocation()
	 * @see PGPSignature#getCreationTime()
	 * @see PGPSignature#KEY_REVOCATION
	 * @see PGPSignature#SUBKEY_REVOCATION
	 */
	public abstract Date getVerifiedRevocationDate(PGPPublicKey key);

	/**
	 * Returns whether the signature's {@link PGPSignature creation time} is before
	 * the key's {@link #getVerifiedRevocationDate(PGPPublicKey) verified revocation
	 * date}, if that key has one.
	 *
	 * @param signature the signature to test.
	 * @param key       the corresponding key of this signature against which to
	 *                  test.
	 * @return <code>true</code> if the signature was created before the key was
	 *         revoked or if the key is not revoked, <code>false</code> otherwise.
	 *
	 * @throws IllegalArgumentException if the signature's
	 *                                  {@link PGPSignature#getKeyID() key} is not
	 *                                  the same as the key's
	 *                                  {@link PGPPublicKey#getKeyID() key ID}
	 */
	public boolean isCreatedBeforeRevocation(PGPSignature signature, PGPPublicKey key) {
		if (signature.getKeyID() != key.getKeyID()) {
			throw new IllegalArgumentException("The signature's key ID must be the same as the key's key ID"); //$NON-NLS-1$
		}
		Date verifiedRevocationDate = getVerifiedRevocationDate(key);
		if (verifiedRevocationDate != null) {
			long signatureCreationTime = signature.getCreationTime().getTime();
			if (signatureCreationTime >= verifiedRevocationDate.getTime()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the hexadecimal representation of the given bytes.
	 *
	 * @param bytes the bytes to convert to a hexadecimal representation.
	 * @return the hexadecimal representation of the given bytes.
	 */
	public static String toHex(byte[] bytes) {
		return Hex.toHexString(bytes);
	}

	/**
	 * Returns the hexadecimal representation of the given long value, typically a
	 * key ID, padded with leading zeros to a length of 16.
	 *
	 * @param keyID the long value, typically a key ID, to convert to a hexadecimal
	 *              representation.
	 * @return the hexadecimal representation of the given long value, padded with
	 *         leading zeros.
	 */
	public static String toHex(long keyID) {
		return String.format("%1$016X", keyID); //$NON-NLS-1$
	}

	/**
	 * If the signature's {@link PGPSignature#getCreationTime() creation time} is
	 * before the key's {@link PGPPublicKey#getCreationTime() creation time}, this
	 * returns a negative value equal to the number of milliseconds that the
	 * signature was created before the key was created. If the key has a
	 * {@link PGPPublicKey#getValidSeconds() validity period}, i.e., if the key
	 * expires, and the signature's creation time is after the key's expiration,
	 * returns a positive value equal to the number of milliseconds that the
	 * signature was created after the key's expiration. Otherwise, the signature
	 * was created during the period of time that the key was valid and this returns
	 * <code>0</code>.
	 *
	 * @param signature the signature to test.
	 * @param key       the corresponding key of this signature against which to
	 *                  test.
	 * @return a negative value representing the number of milliseconds the
	 *         signature was created before the key was created, a positive value
	 *         representing the number of milliseconds the signature as created
	 *         after the key expired, or <code>0</code> if the signature was created
	 *         during the valid period of the key.
	 * @throws IllegalArgumentException if the signature's
	 *                                  {@link PGPSignature#getKeyID() key} is not
	 *                                  the same as the key's
	 *                                  {@link PGPPublicKey#getKeyID() key ID}
	 */
	public static long compareSignatureTimeToKeyValidityTime(PGPSignature signature, PGPPublicKey key) {
		if (signature.getKeyID() != key.getKeyID()) {
			throw new IllegalArgumentException("The signature's key ID must be the same as the key's key ID"); //$NON-NLS-1$
		}
		long keyCreationTime = key.getCreationTime().getTime();
		long signatureCreationTime = signature.getCreationTime().getTime();
		long delta = signatureCreationTime - keyCreationTime;
		if (delta < 0) {
			return delta;
		}
		long validSeconds = key.getValidSeconds();
		if (validSeconds != 0) {
			delta = signatureCreationTime - (keyCreationTime + validSeconds * 1000);
			if (delta > 0) {
				return delta;
			}
		}
		return 0;
	}
}
