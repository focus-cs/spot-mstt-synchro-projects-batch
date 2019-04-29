package com.schneider.mstt.test.utils;

import java.text.DecimalFormat;

import org.junit.Assert;

import com.schneider.mstt.synchro.projects.exceptions.BadSessionException;
import com.schneider.mstt.synchro.projects.exceptions.LoginException;
import com.schneider.mstt.synchro.projects.test.SessionContainer;
import com.sciforma.psnext.api.PSException;
import com.sciforma.psnext.api.Project;

public class Utils {

    public static final String URL = "http://192.168.101.246:8080/psnext";

    public static final String LOGIN = "psnextadmin";

    public static final String PASSWORD = "a";

    /**
     *
     */
    static public void login() {
        // Creation de la session dans une classe utilitaire a part
        // But : restreindre les appels d'ouverture de session
        try {
            SessionContainer.createSession(URL, LOGIN, PASSWORD);
        } catch (BadSessionException e) {
            Assert.fail("Bad session  " + e.getMessage());
        } catch (LoginException e1) {
            Assert.fail("Login Exception " + e1.getMessage());
        }
    }

    static public Project createProjectForTest() throws PSException {
        String projectId = IdGenerator.getRandomId("project", 6);

        Project testProject = new Project(projectId, projectId, Project.VERSION_WORKING);

        testProject.saveAs(projectId, projectId, Project.VERSION_WORKING);

        return testProject;
    }

    /**
     *
     * @param testProject
     * @throws PSException
     */
    static public void cleanProject(Project testProject) throws PSException {
        testProject.save();
        testProject.close();
        testProject.deactivate();
    }

    /**
     *
     */
    static public String percent(double val, double total) {
        double resultat = val / total;
        double resultatFinal = resultat * 100;
        DecimalFormat df = new DecimalFormat("###.##");
        return df.format(resultatFinal) + "%";

    }
}
