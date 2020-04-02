package com.gsww.ga;

import com.gsww.ga.common.config.StartupConfig;
import com.gsww.ga.db.connections.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Connection;
import java.sql.SQLException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DataImporterApplicationTests {

	@Before
	public void setUp() throws Exception {
		StartupConfig.generateInstance("");
	}

	@Test
	public void contextLoads() {
	}

	@Test
	public void getConnection() throws Exception {
		StartupConfig.generateInstance("");
		Connection connection = ConnectionFactory.getConnection("com.cloudera.impala.jdbc41.Driver", "jdbc:impala://192.168.6.64:21050/default;AuthMech=0;", "", "");
	}

}
