package com.organization.accessgrid;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.security.SecureRandom;

/**
 * Client-side crypto helpers for the SmartTap reveal endpoint.
 *
 * <p>The server returns the template's private key encrypted under
 * {@code ECDH-ES + HKDF-SHA256 + AES-256-GCM}. This class generates the local
 * P-256 keypair, exposes the public key as PEM, and decrypts the server's
 * envelope so the plaintext private key is reconstructed without leaving the
 * caller's process.
 */
final class SmartTapRevealCrypto {
    private static final String CURVE_NAME = "P-256";
    private static final String HKDF_INFO = "accessgrid-smart-tap-reveal-v1";
    private static final int AES_KEY_LENGTH = 32;
    private static final int SHARED_SECRET_LENGTH = 32;
    private static final int GCM_TAG_BITS = 128;

    private SmartTapRevealCrypto() {}

    static final class GeneratedKeyPair {
        final AsymmetricCipherKeyPair keyPair;
        final String publicKeyPem;

        GeneratedKeyPair(AsymmetricCipherKeyPair keyPair, String publicKeyPem) {
            this.keyPair = keyPair;
            this.publicKeyPem = publicKeyPem;
        }
    }

    static GeneratedKeyPair generateP256KeyPair() {
        try {
            X9ECParameters curve = ECNamedCurveTable.getByName(CURVE_NAME);
            ECDomainParameters domain = new ECDomainParameters(
                curve.getCurve(), curve.getG(), curve.getN(), curve.getH(), curve.getSeed());

            ECKeyPairGenerator gen = new ECKeyPairGenerator();
            gen.init(new ECKeyGenerationParameters(domain, new SecureRandom()));
            AsymmetricCipherKeyPair pair = gen.generateKeyPair();

            // SubjectPublicKeyInfo PEM (matches OpenSSL's `public_to_pem` on the server).
            org.bouncycastle.asn1.x509.SubjectPublicKeyInfo spki =
                org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(pair.getPublic());
            StringWriter sw = new StringWriter();
            try (JcaPEMWriter writer = new JcaPEMWriter(sw)) {
                writer.writeObject(spki);
            }
            return new GeneratedKeyPair(pair, sw.toString());
        } catch (Exception e) {
            throw new AccessGridClient.AccessGridException("Failed to generate P-256 keypair", e);
        }
    }

    static byte[] decryptEnvelope(
            AsymmetricCipherKeyPair localKeyPair,
            String serverEphemeralPublicKeyPem,
            byte[] iv,
            byte[] ciphertext,
            byte[] tag) {
        ECPublicKeyParameters serverPublicKey = readEcPublicKeyFromPem(serverEphemeralPublicKeyPem);

        byte[] sharedSecret = computeSharedSecret(localKeyPair, serverPublicKey);
        byte[] aesKey = deriveAesKey(sharedSecret);
        return decryptAesGcm(aesKey, iv, ciphertext, tag);
    }

    private static ECPublicKeyParameters readEcPublicKeyFromPem(String pem) {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            if (obj instanceof org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) {
                return (ECPublicKeyParameters) org.bouncycastle.crypto.util.PublicKeyFactory
                    .createKey((org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) obj);
            }
            throw new AccessGridClient.InvalidEnvelopeException(
                "ephemeral_public_key is not a SubjectPublicKeyInfo PEM");
        } catch (AccessGridClient.InvalidEnvelopeException e) {
            throw e;
        } catch (Exception e) {
            throw new AccessGridClient.InvalidEnvelopeException(
                "Failed to parse server ephemeral public key", e);
        }
    }

    private static byte[] computeSharedSecret(AsymmetricCipherKeyPair local, ECPublicKeyParameters serverPub) {
        ECDHBasicAgreement agreement = new ECDHBasicAgreement();
        agreement.init(local.getPrivate());
        java.math.BigInteger shared = agreement.calculateAgreement(serverPub);
        return toFixedBytes(shared, SHARED_SECRET_LENGTH);
    }

    private static byte[] toFixedBytes(java.math.BigInteger value, int length) {
        byte[] unsigned = value.toByteArray();
        if (unsigned.length == length) return unsigned;
        if (unsigned.length == length + 1 && unsigned[0] == 0) {
            byte[] trimmed = new byte[length];
            System.arraycopy(unsigned, 1, trimmed, 0, length);
            return trimmed;
        }
        if (unsigned.length > length) {
            throw new AccessGridClient.InvalidEnvelopeException(
                "ECDH shared secret exceeds expected length");
        }
        byte[] padded = new byte[length];
        System.arraycopy(unsigned, 0, padded, length - unsigned.length, unsigned.length);
        return padded;
    }

    private static byte[] deriveAesKey(byte[] sharedSecret) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(sharedSecret, new byte[0], HKDF_INFO.getBytes(StandardCharsets.UTF_8)));
        byte[] aesKey = new byte[AES_KEY_LENGTH];
        hkdf.generateBytes(aesKey, 0, aesKey.length);
        return aesKey;
    }

    private static byte[] decryptAesGcm(byte[] aesKey, byte[] iv, byte[] ciphertext, byte[] tag) {
        try {
            GCMBlockCipher gcm = new GCMBlockCipher(new AESEngine());
            gcm.init(false, new AEADParameters(new KeyParameter(aesKey), GCM_TAG_BITS, iv, new byte[0]));

            // BouncyCastle expects ciphertext||tag concatenated.
            byte[] combined = new byte[ciphertext.length + tag.length];
            System.arraycopy(ciphertext, 0, combined, 0, ciphertext.length);
            System.arraycopy(tag, 0, combined, ciphertext.length, tag.length);

            byte[] out = new byte[gcm.getOutputSize(combined.length)];
            int len = gcm.processBytes(combined, 0, combined.length, out, 0);
            len += gcm.doFinal(out, len);

            if (len == out.length) return out;
            byte[] trimmed = new byte[len];
            System.arraycopy(out, 0, trimmed, 0, len);
            return trimmed;
        } catch (Exception e) {
            throw new AccessGridClient.DecryptException(
                "AES-GCM decryption failed (auth tag verification)", e);
        }
    }
}
