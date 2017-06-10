package org.mybatis.custom.plugin;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mybatis.encrypt.Cryptogram;
import org.mybatis.encrypt.CryptogramImpl;

@Intercepts({ 
	@Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class, Integer.class }),
	@Signature(type = ResultSetHandler.class, method = "handleResultSets", args = { Statement.class })
})
public class CryptogramInterceptor implements Interceptor {
	private static final Logger logger = LogManager.getLogger(CryptogramInterceptor.class);

	private static boolean isEncrypt;
	private static boolean isDecrypt;
	private static Map<String, String> encryptKeywordMap;
	private static Map<String, String> decryptKeywordMap;
	private static Cryptogram cryptogram = new CryptogramImpl("aes256-test-key!!");
	private static final String REGULAR_EXPRESSION = "[?]";

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		String invocationMethodName = invocation.getMethod().getName();

//		logger.debug("########################################");
//		logger.debug("Invocation method name : " + invocationMethodName);

		if ("prepare".equals(invocationMethodName)) {
			if (isEncrypt) {
				StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
				BoundSql boundSql = statementHandler.getBoundSql();
				List<ParameterMapping> parameterMappinges = boundSql.getParameterMappings();
				String sql = statementHandler.getBoundSql().getSql();
				String encryptSql = new String(sql);
				MetaObject metaObject = MetaObject.forObject(statementHandler, new DefaultObjectFactory(),
						new DefaultObjectWrapperFactory(), new DefaultReflectorFactory());

				Object parameterObject = statementHandler.getParameterHandler().getParameterObject();
				if (parameterObject instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> parameters = (Map<String, Object>) parameterObject;
					for (ParameterMapping parameterMapping : parameterMappinges) {
						String key = parameterMapping.getProperty();
						Object value = parameters.get(key);

						String encryptPiece = null;
						boolean isNumber = Pattern.matches("[0-9]+", String.valueOf(value));
						if (isNumber) {
							if (encryptKeywordMap.containsKey(key)) {
								encryptPiece = cryptogram.encrypt(value);
//								logger.debug("########################################");
//								logger.debug("Encrypt : " + value + " ---> " + encryptPiece);
							} else {
								encryptPiece = String.valueOf(value);
							}

							parameters.put(key, encryptPiece);
							encryptSql = encryptSql.replaceFirst(REGULAR_EXPRESSION, encryptPiece);
						} else {
							if (encryptKeywordMap.containsKey(key)) {
								encryptPiece = cryptogram.encrypt(value);
//								logger.debug("########################################");
//								logger.debug("Encrypt : " + value + " ---> " + encryptPiece);
							} else {
								encryptPiece = String.valueOf(value);
							}

							parameters.put(key, "'" + encryptPiece + "'");
							encryptSql = encryptSql.replaceFirst(REGULAR_EXPRESSION, "'" + encryptPiece + "'");
						}
					}

					metaObject.setValue("delegate.boundSql.sql", encryptSql.toString());
//					logger.debug(encryptSql.toString());
				} else {
				}
			}

			return invocation.proceed();
		} else if ("handleResultSets".equals(invocationMethodName)) {
			Object invocationObject = invocation.proceed();

			if (isDecrypt) {
//				logger.debug("########################################");
//				logger.debug(" Invocation Object : " + invocationObject.getClass().getName());

				if (invocationObject instanceof List) {
					List<Object> resultSetList = new ArrayList<>();

					for (Object invocationItem : (List<?>) invocationObject) {
						if (invocationItem instanceof Map) {
							@SuppressWarnings("unchecked")
							Map<String, Object> resultSetItem = (Map<String, Object>) invocationItem;
							Iterator<String> iterator = resultSetItem.keySet().iterator();
							while (iterator.hasNext()) {
								String key = iterator.next();
								Object value = resultSetItem.get(key);

								String decryptPiece = null;
								if (decryptKeywordMap.containsKey(key)) {
									decryptPiece = cryptogram.decrypt(value);
									resultSetItem.put(key, decryptPiece);

//									logger.debug("########################################");
//									logger.debug("Decrypt : " + value + " ---> " + decryptPiece);
								} else {
									resultSetItem.put(key, String.valueOf(value));
								}
							}

							resultSetList.add(resultSetItem);

							continue;
						} else if (invocationItem instanceof String) {
							String resultSetItem = (String) invocationItem;
							String decryptPiece = null;
							if (decryptKeywordMap.containsKey(resultSetItem)) {
								decryptPiece = cryptogram.decrypt(resultSetItem);

//								logger.debug("########################################");
//								logger.debug("Decrypt : " + resultSetItem + " ---> " + decryptPiece);
							} else {
								decryptPiece = String.valueOf(resultSetItem);
							}

							resultSetList.add(decryptPiece);

							continue;
						} else {
							resultSetList.add(invocationItem);

							continue;
						}
					}

					return resultSetList;
				} else if (invocationObject instanceof Map) {
					Map<String, Object> resultSetMap = new HashMap<>();

					@SuppressWarnings("unchecked")
					Map<String, Object> invocationMap = (Map<String, Object>) invocationObject;
					Iterator<String> iterator = invocationMap.keySet().iterator();
					while (iterator.hasNext()) {
						String key = iterator.next();
						Object value = invocationMap.get(key);

						String decryptPiece = null;
						if (decryptKeywordMap.containsKey(key)) {
							decryptPiece = cryptogram.decrypt(value);

							resultSetMap.put(key, decryptPiece);
//							logger.debug("########################################");
//							logger.debug("Decrypt : " + value + " ---> " + decryptPiece);
						} else {
							resultSetMap.put(key, String.valueOf(value));
						}
					}

					return resultSetMap;
				} else if (invocationObject instanceof String) {
					String invocationString = (String) invocationObject;
					if (decryptKeywordMap.containsKey(invocationString)) {
						return cryptogram.decrypt(invocationString);
					} else {
						return String.valueOf(invocationString);
					}
				} else if (invocationObject instanceof Integer) {
					return invocationObject;
				} else {
					return invocationObject;
				}
			} else {
				return invocationObject;
			}
		} else {
			return invocation.proceed();
		}
	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {
		String encryptKeyword = properties.getProperty("encryptKeyword") != null ? String.valueOf(properties.getProperty("encryptKeyword")) : "";
		String decryptKeyword = properties.getProperty("decryptKeyword") != null ? String.valueOf(properties.getProperty("decryptKeyword")) : "";
		String[] arrayEncryptKeyword = encryptKeyword.split("[,]");
		String[] arrayDecryptKeyword = decryptKeyword.split("[,]");
		isEncrypt = arrayEncryptKeyword.length > 0 ? true : false;
		isDecrypt = arrayDecryptKeyword.length > 0 ? true : false;

		encryptKeywordMap = new HashMap<>();
		for (String item : arrayEncryptKeyword) {
			encryptKeywordMap.put(item.trim(), item.trim());
		}

		decryptKeywordMap = new HashMap<>();
		for (String item : arrayDecryptKeyword) {
			decryptKeywordMap.put(item.trim(), item.trim());
		}
	}
}