package org.mybatis.mapper;

import java.util.Map;

import org.apache.ibatis.exceptions.PersistenceException;

public interface UserMapper {
	public Map<String, Object> select(Map<String, Object> parameter) throws PersistenceException;
	public void insert(Map<String, Object> parameter) throws PersistenceException;
}
