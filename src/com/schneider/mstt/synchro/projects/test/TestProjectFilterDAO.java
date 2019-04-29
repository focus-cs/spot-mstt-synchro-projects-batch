package com.schneider.mstt.synchro.projects.test;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.schneider.mstt.synchro.projects.dao.SpotProjectFilterDAO;

public class TestProjectFilterDAO {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() throws Exception {
        SpotProjectFilterDAO ProjectFilterDAO = new SpotProjectFilterDAO();

        List<String> IDs = ProjectFilterDAO.getModifedProjectIDsAfterDate("2011-08-08 16:30:42");

        for (String id : IDs) {
            Logger.getRootLogger().info(id);
        }
    }

}
