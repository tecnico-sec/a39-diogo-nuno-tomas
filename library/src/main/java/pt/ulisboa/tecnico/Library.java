package pt.ulisboa.tecnico;

import io.vavr.control.Either;
import org.apache.commons.lang3.ArrayUtils;
import pt.ulisboa.tecnico.aux.Cryptography;
import pt.ulisboa.tecnico.aux.Keys;

import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayOutputStream;

import java.util.Arrays;
import java.util.Optional;

import static pt.ulisboa.tecnico.aux.Constants.*;
import static pt.ulisboa.tecnico.aux.Conversion.*;

public class Library {

    final private Keys keys;

    final private Cryptography crypto;

    public Library(String secretKeyPath) throws Exception {
        keys = new Keys(secretKeyPath);
        crypto = new Cryptography();
    }

    public Either<String, byte[]> protect(byte[] input) {
        try {
            return Either.right(doProtect(input));
        } catch (Exception e) {
            return Either.left((e.getMessage()));
        }
    }

    public Either<String, byte[]> unprotect(byte[] input) {
        try {
            return Either.right(doUnprotect(input));
        } catch (Exception e) {
            return Either.left(e.getMessage());
        }
    }

    public boolean check(byte[] input) {
        return unprotect(input).isRight();
    }

    private byte[] doProtect(byte[] input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        if (!write(keys.getIv().getIV(), output)) throw new Exception("Check the initialization vector");

        ByteArrayOutputStream payload = new ByteArrayOutputStream();

        if (!write(intToBytes(input.length), payload)) throw new Exception("Check the payload length");
        if (!write(input, payload)) throw new Exception("Check the payload");

        int randomNumber = crypto.getRandomNumber();
        int sequenceNumber = crypto.getAndSetSequenceNumber();

        if (!write(intToBytes(randomNumber), payload)) throw new Exception("Check the random number");
        if (!write(intToBytes(sequenceNumber), payload)) throw new Exception("Check the sequence number");

        byte[] digestEncrypted = crypto.asymEncrypt(crypto.digest(payload.toByteArray()), keys.privateKey).orElseThrow(
            () -> new Exception("Check the asymmetric encryption method"));

        if (!write(intToBytes(digestEncrypted.length), payload)) throw new Exception(
            "Check the length of digestEncrypted");
        if (!write(digestEncrypted, payload)) throw new Exception(
            "Check the digestEncrypted");

        byte[] encryptedPayload = crypto.symEncrypt(payload.toByteArray(), keys.secretKey, keys.iv).orElseThrow(
            () -> new Exception("Check the symmetric encryption method"));

        if (!write(encryptedPayload, output)) throw new Exception("Check the encrypted payload");

        return output.toByteArray();
    }

    private byte[] doUnprotect(byte[] input) throws Exception {
        byte[] iv = read(input, 0, 16).orElseThrow(() -> new Exception("Check the initialization vector"));

        byte[] encryptedPayload = read(input, 16, input.length - 16).orElseThrow(
            () -> new Exception("Check the encrypted payload"));

        byte[] payload = crypto.symDecrypt(encryptedPayload, keys.secretKey, new IvParameterSpec(iv)).orElseThrow(
            () -> new Exception("Check the symmetric decryption method"));

        int payloadLength = bytesToInt(payload, 0).orElseThrow(() -> new Exception("Check the payload length"));
        byte[] data = read(payload, INT_SIZE, payloadLength).orElseThrow(() -> new Exception("Check the payload"));

        int randomNumber = bytesToInt(payload, INT_SIZE + payloadLength).orElseThrow(
            () -> new Exception("Check the random number"));
        int sequenceNumber = bytesToInt(payload, INT_SIZE + payloadLength + INT_SIZE).orElseThrow(
            () -> new Exception("Check the sequence number"));

        int digestEncryptStart = INT_SIZE + payloadLength + INT_SIZE + INT_SIZE;
        int digestEncryptedLength = bytesToInt(payload, digestEncryptStart).orElseThrow(
            () -> new Exception("Check the length of digestEncrypted"));

        byte[] digestEncrypted = read(payload, digestEncryptStart + INT_SIZE, digestEncryptedLength)
            .orElseThrow(() -> new Exception("Check the digestEncrypted"));

        byte[] digestDecrypted = crypto.asymDecrypt(digestEncrypted, keys.publicKey).orElseThrow(
            () -> new Exception("Check the asymmetric decryption method"));
        byte[] digestCalculated = crypto.digest(Arrays.copyOfRange(payload, 0, digestEncryptStart));

        if (!Arrays.equals(digestDecrypted, digestCalculated)) {
            throw new Exception("Digests don't match");
        }

        return data;
    }

    private Optional<byte[]> read(byte[] input, int start, int length) {
        try {
            return Optional.of(Arrays.copyOfRange(input, start, start + length));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private boolean write(byte[] input, ByteArrayOutputStream output) {
        try {
            output.write(input);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
