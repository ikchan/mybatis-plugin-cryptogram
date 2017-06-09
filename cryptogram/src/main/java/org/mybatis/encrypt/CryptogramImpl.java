package org.mybatis.encrypt;

import java.io.UnsupportedEncodingException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class CryptogramImpl implements Cryptogram {
	private static final Logger logger = LogManager.getLogger(CryptogramImpl.class);

	private String iv;
	private Key secretKeySpec;

	public CryptogramImpl(String key) {
		this.iv = key.substring(0, 16);

		byte[] keyBytes = new byte[16];
		byte[] utf8KeyBytes = null;

		try {
			utf8KeyBytes = key.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		int len = utf8KeyBytes.length;
		if (len > keyBytes.length) {
			len = keyBytes.length;
		}

		System.arraycopy(utf8KeyBytes, 0, keyBytes, 0, len);
		SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

		this.secretKeySpec = secretKeySpec;
	}

	@Override
	public String encrypt(Object word) throws Exception {
		if (word == null) {
			return null;
		}

		return encrypt(String.valueOf(word));
	}

	@Override
	public String decrypt(Object word) throws Exception {
		if (word == null) {
			return null;
		}

		return decrypt(String.valueOf(word));
	}

	private String encrypt(String word) throws Exception {
		if (StringUtils.isEmpty(word)) {
			return "";
		}

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, this.secretKeySpec, new IvParameterSpec(this.iv.getBytes()));

		byte[] encrypted = cipher.doFinal(word.getBytes("UTF-8"));
		String encrypWord = new String(Base64.encodeBase64(encrypted));

		// logger.debug("############################################################");
		// logger.debug(" Encrypt : " + word + " ---> " + encrypWord);
		// logger.debug("############################################################");

		return encrypWord;
	}

	private String decrypt(String word) throws Exception {
		if (StringUtils.isEmpty(word)) {
			return "";
		}

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, this.secretKeySpec, new IvParameterSpec(this.iv.getBytes("UTF-8")));

		byte[] decrypted = Base64.decodeBase64(word.getBytes());

		String decryptWord = new String(cipher.doFinal(decrypted), "UTF-8");

		// logger.debug("############################################################");
		// logger.debug(" Decrypt : " + word + " ---> " + decryptWord);
		// logger.debug("############################################################");

		return decryptWord;
	}

	public static void main(String[] args) throws Exception {
		String loginidx = "MEM201605090243";
		String key = "aes256-test-key!!";

		Cryptogram cryptogram = new CryptogramImpl(key);

		String encryptWord = cryptogram.encrypt(loginidx);
		// logger.debug(encryptWord);

		String decryptWord = cryptogram.decrypt(encryptWord);
		// logger.debug(decryptWord);
	}
}
