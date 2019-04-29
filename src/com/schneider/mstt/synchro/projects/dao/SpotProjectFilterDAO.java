package com.schneider.mstt.synchro.projects.dao;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@Configuration
@PropertySource("file:${user.dir}/conf/psconnect.properties")
public class SpotProjectFilterDAO extends JdbcDaoSupport {

    @Value("${spot.db.url}")
    private String url;
    @Value("${spot.db.login}")
    private String login;
    @Value("${spot.db.password}")
    private String password;
    @Value("${driver}")
    private String driver;
    @Value("${spot.find_ids_request}")
    private String request;

    /**
     * @throws IOException
     * @throws ClassNotFoundException
     *
     */
    public SpotProjectFilterDAO() throws IOException, ClassNotFoundException {
        super();

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("verifier que le driver jdbc est dans le classpath de tomcat <" + driver + ">.", e);
        }

        final SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url, login, password, false);

        setDataSource(dataSource);
    }

    /**
     * 
     *
     * @param owningOrga
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<String> getModifedProjectIDsAfterDate(final String date) {
        return getJdbcTemplate().queryForList(request, new Object[]{date, date}, String.class);
    }

}
