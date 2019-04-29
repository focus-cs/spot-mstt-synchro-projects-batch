package com.schneider.mstt.synchro.projects.dao;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import schneider.mstt.api.exceptions.MSTTSynchroException;

@Configuration
@PropertySource("file:${user.dir}/conf/psconnect.properties")
public class MsttProjectFilterDAO extends JdbcDaoSupport {

    @Value("${mstt.db.url}")
    private String url;
    @Value("${mstt.db.login}")
    private String login;
    @Value("${mstt.db.password}")
    private String password;
    @Value("${driver}")
    private String driver;
    @Value("${mstt.find_id_request}")
    private String findIdRequest;
    @Value("${mstt.find_template_id}")
    private String findTemplateIdRequest;
    @Value("${mstt.find_excluded_proj_id}")
    private String findExcludedProjectIdRequest;
    @Value("${mstt.insert_excluded_proj}")
    private String insertExcludedProjectRequest;
    @Value("${mstt.select_existing_proj}")
    private String selectExistingProjectRequest;
    @Value("${mstt.delete_excluded_proj}")
    private String deleteExcludedProjectRequest;
    @Value("${mstt.find_card_request}")
    private String findCardinalityRequest;
    @Value("${mstt.update_card_request}")
    private String updateCardinalityRequest;

    public MsttProjectFilterDAO() throws IOException {

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new IOException("Verify that jdbc driver is in tomcat classpath <" + driver + ">. for the reason : " + e.getMessage());
        }

        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url, login, password, false);
        setDataSource(dataSource);
    }

    public List<String> getProjectIDsFromInternalID(String internalID) throws DataAccessException {
        return getJdbcTemplate().queryForList(findIdRequest, new Object[]{internalID}, String.class);
    }

    public String getTemplateIDFromName(String templateName) throws DataAccessException {
        return (String) getJdbcTemplate().queryForObject(findTemplateIdRequest, new Object[]{templateName}, String.class);
    }

    public List<String> getExcludedProjectsId(String orga) throws DataAccessException {
        return getJdbcTemplate().queryForList(findExcludedProjectIdRequest, new Object[]{orga}, String.class);
    }

    public void createExcludedProject(String internalID, String owningOrga) throws MSTTSynchroException {
        try {
            getJdbcTemplate().update(insertExcludedProjectRequest, new Object[]{internalID, owningOrga}, new int[]{12, 12});
        } catch (DataAccessException e) {
            throw new MSTTSynchroException("ERROR: cannot insert a new line to 'ExcludedProjects' table", e);
        }
    }

    public void deleteExcludedProjects(String orga) throws MSTTSynchroException {
        try {
            getJdbcTemplate().update(deleteExcludedProjectRequest, new Object[]{orga}, new int[]{12});
        } catch (DataAccessException e) {
            throw new MSTTSynchroException("ERROR: cannot delete excluded Projects", e);
        }
    }

    public List<String> getAlreadyExistingProjects() {
        return getJdbcTemplate().queryForList(selectExistingProjectRequest, String.class);
    }

    public String getCardinality(String spotID, String msttID) throws DataAccessException {

        List<String> result = getJdbcTemplate().queryForList(findCardinalityRequest, new Object[]{spotID, msttID}, String.class);
        if (result != null && !result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

    public void updateCardTable(String oldProjectID, String oldMsttID, String projectID, String id) {

        getJdbcTemplate().update(updateCardinalityRequest, new Object[]{projectID, id, oldProjectID, oldMsttID});
    }
}
