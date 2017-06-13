
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
import org.mybatis.mapper.CustomerMapper;

public class NormalExecutor {
	private static final Logger logger = LogManager.getLogger(NormalExecutor.class);
	private static SqlSessionFactory sqlSessionFactory;

	static {
		try {
			String resource = "mybatis/config-mybatis.xml";
			Reader reader = Resources.getResourceAsReader(resource);
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SqlSession sqlSession = sqlSessionFactory.openSession();

		try {
			CustomerMapper customerMapper = sqlSession.getMapper(CustomerMapper.class);

			String id = "MY" + Calendar.getInstance().getTime().getMinutes() + Calendar.getInstance().getTime().getSeconds();

			Map<String, Object> dto = new HashMap<String, Object>();
			dto.put("id", id);
			dto.put("name", "Richard");
			dto.put("email", "richard@gmail.com");
			customerMapper.insert(dto);

			dto = new HashMap<String, Object>();
			dto.put("id", id);
			dto = customerMapper.select(dto);

			logger.trace("############################################################");
			logger.trace("# Select Data : " + dto.get("name"));
			logger.trace("# Select Data : " + dto.get("email"));
			logger.trace("############################################################");

			sqlSession.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlSession.close();
		}
	}
}
