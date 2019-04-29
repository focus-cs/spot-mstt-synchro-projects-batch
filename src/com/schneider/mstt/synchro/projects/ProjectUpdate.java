package com.schneider.mstt.synchro.projects;

import com.schneider.mstt.synchro.projects.service.MSTTService;
import com.schneider.mstt.synchro.projects.dao.SpotProjectFilterDAO;
import com.sciforma.psnext.api.AccessException;
import com.sciforma.psnext.api.PSException;
import com.sciforma.psnext.api.Project;
import com.sciforma.psnext.api.Session;
import fr.sciforma.psconnect.service.exception.BusinessException;
import fr.sciforma.psconnect.service.exception.TechnicalException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import schneider.mstt.api.CommObjectDTO;
import schneider.mstt.api.PSNextManager;
import schneider.mstt.api.ProjectDTO;

@Configuration
@PropertySource("file:${user.dir}/conf/psconnect.properties")
public class ProjectUpdate {

    protected static final Logger LOG = Logger.getLogger(Runner.class);
    private final static String NUMBER_FORMAT = "%02d";

    @Value("${spot.psnext.url}")
    private String spotUrl;
    @Value("${spot.psnext.login}")
    private String spotLogin;
    @Value("${spot.psnext.password}")
    private String spotPassword;

    @Autowired
    private MSTTService msttService;

    /**
     * For save application messages.
     */
    private final transient List<String> messages;

    private transient SpotProjectFilterDAO spotProjectFilterDAO;

    private transient PSNextManager pSNextManagerSpot;

    private transient Session session;

    public ProjectUpdate() {
        // Init logs
        messages = new ArrayList<>();
    }

    /**
     *
     * Method executed before process method.
     */
    protected void init() {

        // Init projectFilterDAO instance
        try {
            spotProjectFilterDAO = new SpotProjectFilterDAO();
        } catch (IOException | ClassNotFoundException e) {
            LOG.fatal(e);
            close(Common.ERROR_EXIST_CODE);
        }

    }

    // -------------------------------------------------------------------------------------------------------------
    public void process() {
        int processStat = 0;
        //
        init();

        // Getting last process date
        String date = null;
        try {
            date = Common.getInstance().getLastProcessDate();
            LOG.debug("Last date=" + date);
        } catch (IOException | ParseException e) {
            LOG.fatal("Error : cannot get the last update process date : ", e);
            close(Common.ERROR_EXIST_CODE);
        }

        // get SPOT Project ids where project is modified after date
        List<String> modifiedProjectIds = null;
        try {
            modifiedProjectIds = spotProjectFilterDAO.getModifedProjectIDsAfterDate(date);
            LOG.debug("modifiedProjectIds size=" + modifiedProjectIds.size());
        } catch (Exception e) {
            LOG.fatal(e);
            close(Common.ERROR_EXIST_CODE);
        }

        if (modifiedProjectIds != null && !modifiedProjectIds.isEmpty()) {

            // Init session
            try {
                session = new Session(spotUrl);
                session.login(spotLogin, spotPassword.toCharArray());
            } catch (PSException e) {
                messages.add("The API can not connect to the SPOT server");
                LOG.fatal(e);
                e.printStackTrace();
                close(Common.ERROR_EXIST_CODE);
            }

            // Init pSNextManager instance
            pSNextManagerSpot = new PSNextManager(session);

            List<ProjectDTO> projectsToUpdate = new ArrayList<>();
            
            for (String projectId : modifiedProjectIds) {
                final Project pSNextProj = pSNextManagerSpot.getProjectById(projectId);

                ProjectDTO projectDTO;
                try {
                    // Parse PSNext project to ProjectDTO
                    projectDTO = pSNextManagerSpot.spotProjectToProjectDTO(pSNextProj);
                } catch (PSException | BusinessException | TechnicalException e) {
                    LOG.error("Error : cannot convert The SPOT project to ProjectDTO", e);
                    processStat = Common.ERROR_EXIST_CODE;
                    continue;
                }
                
                projectsToUpdate.add(projectDTO);

//                CommObjectDTO result;
//                try {
//                    // Call MSTT server to update project
//                    final String projectToSend = projectDTO.archive();
//                    LOG.debug("Sending project : " + projectToSend);
//                    System.out.println("Sending at: " + new Date());
//                    result = msttService.updateProject(projectToSend, refresh);
//                    System.out.println("Sent at: " + new Date());
//                    LOG.debug("Project sent : " + projectToSend);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    LOG.error(e);
//                    processStat = Common.ERROR_EXIST_CODE;
//                    continue;
//                }
//
//                if (result != null) {
//                    processStat = getMaxLevel(processStat, result.getStatus());
//
//                    result.getMessages().forEach((msg) -> {
//                        messages.add(msg);
//                    });
//                }
            }
            
            msttService.updateProjects(projectsToUpdate);

            System.out.println("Publishing projects: " + new Date());
            msttService.publishProjects();
            System.out.println("Project published: " + new Date());

            try {
                Common.getInstance().updateLastProcessDate();
            } catch (IOException e) {
                LOG.error(e);
                close(Common.ERROR_EXIST_CODE);
            }
        }

        close(processStat);
    }

    /**
     * Method executed after process method.
     *
     * @throws PSException
     * @throws AccessException
     */
    protected void close(final int status) {
        if ((session != null) && (session.isLoggedIn())) {
            try {
                session.logout();
            } catch (Exception e) {
                LOG.error(e);
            }
        }

        final File currentDir = new File("").getAbsoluteFile();
        final String rootDir = currentDir.getParent();

        // Computing output file name
        final Calendar cal = Calendar.getInstance();
        final String pref = cal.get(Calendar.YEAR) + "-" + String.format(NUMBER_FORMAT, (cal.get(Calendar.MONTH) + 1)) + "-"
                + String.format(NUMBER_FORMAT, cal.get(Calendar.DATE)) + "_" + String.format(NUMBER_FORMAT, cal.get(Calendar.HOUR_OF_DAY)) + "-"
                + String.format(NUMBER_FORMAT, cal.get(Calendar.MINUTE)) + "-" + String.format(NUMBER_FORMAT, cal.get(Calendar.SECOND));

        final File newDir = new File(rootDir + "\\log\\" + pref);
        newDir.mkdir();

        final String tracesFilename = newDir.getAbsolutePath() + "\\trace-spot-mstt-synchro-projects_" + pref + ".txt";
        try {
            final FileWriter tracefile = new FileWriter(tracesFilename);
            for (int i = 0; i < messages.size(); i++) {
                tracefile.write((String) messages.get(i) + "\n");
            }
            tracefile.close();
        } catch (IOException e) {
            LOG.warn("could not write trace file", e);
        }

        System.exit(status);
    }

    /**
     *
     * @param level1
     * @param level2
     * @return
     */
    private int getMaxLevel(final int level1, final int level2) {
        return level1 >= level2 ? level1 : level2;
    }

}
