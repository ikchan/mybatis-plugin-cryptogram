package org.mybatis.encrypt;

public interface Cryptogram {
	public String encrypt(Object word) throws Exception;
	public String decrypt(Object word) throws Exception;
}
