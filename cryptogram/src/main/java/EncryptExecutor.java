
import java.io.IOException;
import java.io.Reader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.encrypt.Cryptogram;
import org.mybatis.encrypt.CryptogramImpl;
import org.mybatis.mapper.UserMapper;

public class EncryptExecutor {
	private static final Logger logger = LogManager.getLogger(EncryptExecutor.class); 
	private static String key = "aes256-test-key!!";
	private static Cryptogram cryptogram = null;
	private static SqlSessionFactory sqlSessionFactory;

	static {
		try {
			String resource = "resources/mybatis/config-mybatis.xml";
			Reader reader = Resources.getResourceAsReader(resource);
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
			cryptogram = new CryptogramImpl(key);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SqlSession sqlSession = sqlSessionFactory.openSession();

		try {
			String id = "MY" + Calendar.getInstance().getTime().getHours() + Calendar.getInstance().getTime().getMinutes();

			UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

			Map<String, Object> dto = new HashMap<String, Object>();
			dto.put("id", id);
			dto.put("name", cryptogram.encrypt("Richard"));
			dto.put("email", cryptogram.encrypt("richard@gmail.com"));
			userMapper.insert(dto);

			dto = new HashMap<String, Object>();
			dto.put("id", id);
			dto = userMapper.select(dto);

			logger.trace("############################################################");
			logger.trace("# Select Data : " + cryptogram.decrypt(dto.get("name")));
			logger.trace("# Select Data : " + cryptogram.decrypt(dto.get("email")));
			logger.trace("############################################################");

			sqlSession.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlSession.close();
		}
	}
}
