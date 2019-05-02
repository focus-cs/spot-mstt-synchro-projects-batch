package com.schneider.mstt.synchro.projects.service;

import com.schneider.mstt.synchro.projects.Common;
import com.schneider.mstt.synchro.projects.dao.MsttProjectFilterDAO;
import com.schneider.mstt.synchro.projects.exceptions.LoginException;
import com.sciforma.psnext.api.AccessException;
import com.sciforma.psnext.api.LockException;
import com.sciforma.psnext.api.PSException;
import com.sciforma.psnext.api.Project;
import com.sciforma.psnext.api.Session;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.dao.DataAccessException;
import schneider.mstt.api.CommObjectDTO;
import schneider.mstt.api.PSNextManager;
import schneider.mstt.api.ProjectDTO;

@Configuration
@PropertySource("file:${user.dir}/conf/psconnect.properties")
public class MSTTService {

    private static final String HEADER = "-------------------------------------------------------------------------";

    protected static final Logger LOG = Logger.getLogger(MSTTService.class);

    @Value("${mstt.psnext.url}")
    private String msttUrl;
    @Value("${mstt.psnext.login}")
    private String msttLogin;
    @Value("${mstt.psnext.password}")
    private String msttPassword;

    @Autowired
    private MsttProjectFilterDAO msttProjectFilterDAO;
    @Autowired
    private Common common;

    private transient PSNextManager pSNextManagerMstt;

    private List<Project> projectsToPublish = new ArrayList();

    public List<CommObjectDTO> updateProjects(List<ProjectDTO> projectsToUpdate) throws LoginException {

        List<CommObjectDTO> results = new ArrayList<>();

        if (projectsToUpdate != null && !projectsToUpdate.isEmpty()) {

            Session session = null;
            try {
                session = new Session(msttUrl);
                session.login(msttLogin, msttPassword.toCharArray());
            } catch (PSException e) {
                LOG.fatal(e);
                e.printStackTrace();
                throw new LoginException("The API can not connect to the MSTT server");
            }

            // Init pSNextManager instance
            pSNextManagerMstt = new PSNextManager(session);

            for (ProjectDTO projectDTO : projectsToUpdate) {

                results.add(updateProject(projectDTO));

            }

        }

        return results;
    }

    private CommObjectDTO updateProject(ProjectDTO projectDTO) {
        CommObjectDTO result = new CommObjectDTO();
        boolean hasChanged = false;

        LOG.debug("Trying to update project : " + projectDTO);

        String spotInternalID = projectDTO.getInternalID();
        String oldProjectID = projectDTO.getProjectID();
        String projectName = projectDTO.getName();
        result.addMessage(HEADER);
        result.addMessage(String.valueOf(String.format("Updating project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName)) + ", timestamp: " + new Date());
        List<String> projectIDs = null;
        try {
            result.addMessage("Timestamp before getting Project internal IDs from Spot: " + new Date());
            projectIDs = msttProjectFilterDAO.getProjectIDsFromInternalID(spotInternalID);
            result.addMessage("Timestamp after getting Project internal IDs from Spot: " + new Date());
        } catch (DataAccessException e) {
            e.printStackTrace();
            result.addMessage(String.format("ERROR when trying to get project SPoT Internal ID='%s'", e.getMessage()));
            result.addMessage(String.format("ERROR : the API Cannot find a project with the following SPoT Internal ID='%s'", spotInternalID));
            result.setStatus(2);
            return result;
        }

        //////////////////////
        if (projectIDs != null && !projectIDs.isEmpty()) {

            for (String projectId : projectIDs) {

                result.addMessage("Looping on project id " + projectId + " (obtained from iid " + spotInternalID + "): " + new Date());
                Project pSNextProj = pSNextManagerMstt.getProjectById(projectId);

                if (pSNextProj != null) {

                    result.addMessage("Timestamp before getting portfolio name: " + new Date());
                    String portfolioFolderName = pSNextManagerMstt.getPortfolioFolderName(projectDTO.getOwningOrgaCode(), "Description");
                    result.addMessage("Timestamp after getting portfolio name: " + new Date());
                    LOG.debug("PortFolio Folder = " + portfolioFolderName);
                    projectDTO.setOwningOrga(portfolioFolderName);
                    String operation = "update";

                    try {
                        result.addMessage("Timestamp before opening project : " + new Date());
                        pSNextProj.open(false);
                        result.addMessage("Timestamp after opening project : " + new Date());
                    } catch (PSException e) {
                        StringBuffer strBu;
                        try {
                            strBu = new StringBuffer(String.format("ERROR : the API cannot open the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>. Original message: " + e.getMessage(), spotInternalID, pSNextProj.getStringField("ID"), pSNextProj.getStringField("Name")));
                        } catch (PSException e1) {
                            strBu = new StringBuffer(String.format("ERROR : the API cannot open the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>> Original message: " + e1.getMessage(), spotInternalID, oldProjectID, projectName));
                        }
                        if (e instanceof LockException) {
                            strBu.append(", Locked by <")
                                    .append(((LockException) e).getLockingUser())
                                    .append(">");
                        }
                        result.addMessage(strBu.toString());
                        result.setStatus(2);
                        try {
                            pSNextProj.close();
                        } catch (PSException e2) {
                            result.addMessage(String.format("ERROR : the API cannot close the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName));
                            result.setStatus(2);
                        }
                    }

                    try {
                        try {
                            String oldMsttID = "";
                            try {
                                oldMsttID = pSNextProj.getStringField("ID");
                            } catch (PSException e) {
                                result.addMessage(String.format("ERROR : Cannot read the project ID <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName));
                                LOG.error(e);
                                result.setStatus(2);
                            }
                            result.addMessage("Timestamp before getting cardinality: " + new Date());
                            String cardinality = msttProjectFilterDAO.getCardinality(spotInternalID, oldMsttID);
                            result.addMessage("Timestamp after getting cardinality: " + new Date());
                            Boolean changeID = "1-1".equals(cardinality);
                            result.addMessage("Timestamp before updating project fields: " + new Date());
                            CommObjectDTO resultUpdate = common.updateProjectFields(pSNextProj, projectDTO, operation, changeID);
                            result.addMessage("Timestamp after getting cardinality: " + new Date());
                            result.getMessages().addAll(resultUpdate.getMessages());
                            result.setStatus(resultUpdate.getStatus());
                            if (result.getStatus() >= 2) {
                                break;
                            }
                            try {
                                result.addMessage("Timestamp before saving project " + pSNextProj + ": " + new Date());
                                result.addMessage("Project has changed: " + pSNextProj.hasChanged());
                                hasChanged = pSNextProj.hasChanged();
                                if (hasChanged) {
                                    pSNextProj.save();
                                }
                                result.addMessage("Timestamp after saving " + pSNextProj + ": " + new Date());
                                try {
                                    projectsToPublish.add(pSNextProj);
                                    result.addMessage("Timestamp before updating card table: " + new Date());
                                    msttProjectFilterDAO.updateCardTable(spotInternalID, oldMsttID, projectDTO.getProjectID(), pSNextProj.getStringField("ID"));
                                    result.addMessage("Timestamp after updating card table: " + new Date());
                                } catch (PSException e) {
                                    result.addMessage(String.format("ERROR : Cannot publish the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName));
                                    LOG.error(e);
                                    result.setStatus(2);
                                }
                            } catch (PSException e) {
                                result.addMessage(String.format("ERROR : Cannot save the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>. Original message: " + e.getMessage(), spotInternalID, oldProjectID, projectName));
                                LOG.error(e);
                                result.setStatus(2);
                            }
                        } catch (DataAccessException e) {
                            result.addMessage(String.valueOf(String.format("ERROR : Cannot save the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName)) + ": " + e.getMessage());
                            LOG.error(e);
                            result.setStatus(2);
                            try {
                                pSNextProj.close();
                                break;
                            } catch (PSException e3) {
                                result.addMessage(String.format("ERROR : the API cannot close the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName));
                                LOG.error(e3);
                                result.setStatus(2);
                                return result;
                            }
                        }
                    } catch (Throwable throwable) {
                        try {
                            pSNextProj.close();
                        } catch (PSException e) {
                            result.addMessage(String.format("ERROR : the API cannot close the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName));
                            LOG.error(e);
                            result.setStatus(2);
                            return result;
                        }
                        throw throwable;
                    }

                } else {
                    result.addMessage(String.format("ERROR : the API cannot find a project with the following SPoT Internal ID='%s'", spotInternalID));
                    result.setStatus(2);
                }
            }

        } else {
            result.addMessage(String.format("ERROR : the API cannot find a project that with the following SPoT Internal ID='%s'", spotInternalID));
            result.setStatus(2);
        }
        //////////////////////

        if (projectIDs == null || projectIDs.isEmpty()) {
            result.addMessage(String.format("ERROR : the API cannot find a project that with the following SPoT Internal ID='%s'", spotInternalID));
            result.setStatus(2);
            return result;
        }

        result.addMessage("Timestamp before looping on project ids: " + new Date());

        for (String projectId : projectIDs) {
            result.addMessage("Looping on project id " + projectId + " (obtained from iid " + spotInternalID + "): " + new Date());
            Project pSNextProj = pSNextManagerMstt.getProjectById(projectId);
            if (pSNextProj == null) {
                result.addMessage(String.format("ERROR : the API cannot find a project with the following SPoT Internal ID='%s'", spotInternalID));
                result.setStatus(2);
                return result;
            }
            result.addMessage("Timestamp before getting portfolio name: " + new Date());
            String portfolioFolderName = pSNextManagerMstt.getPortfolioFolderName(projectDTO.getOwningOrgaCode(), "Description");
            result.addMessage("Timestamp after getting portfolio name: " + new Date());
            LOG.debug("PortFolio Folder = " + portfolioFolderName);
            projectDTO.setOwningOrga(portfolioFolderName);
            String operation = "update";
            try {
                result.addMessage("Timestamp before opening project : " + new Date());
                pSNextProj.open(false);
                result.addMessage("Timestamp after opening project : " + new Date());
            } catch (PSException e) {
                StringBuffer strBu;
                try {
                    strBu = new StringBuffer(String.format("ERROR : the API cannot open the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>. Original message: " + e.getMessage(), spotInternalID, pSNextProj.getStringField("ID"), pSNextProj.getStringField("Name")));
                } catch (PSException e1) {
                    strBu = new StringBuffer(String.format("ERROR : the API cannot open the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>> Original message: " + e1.getMessage(), spotInternalID, oldProjectID, projectName));
                }
                if (e instanceof LockException) {
                    strBu.append(", Locked by <")
                            .append(((LockException) e).getLockingUser())
                            .append(">");
                }
                result.addMessage(strBu.toString());
                result.setStatus(2);
                CommObjectDTO commObjectDTO = result;
                try {
                    pSNextProj.close();
                } catch (PSException e2) {
                    result.addMessage(String.format("ERROR : the API cannot close the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName));
                    LOG.error(e2);
                    result.setStatus(2);
                    return result;
                }
                return commObjectDTO;
            }
            try {
                try {
                    String oldMsttID = "";
                    try {
                        oldMsttID = pSNextProj.getStringField("ID");
                    } catch (PSException e) {
                        result.addMessage(String.format("ERROR : Cannot read the project ID <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName));
                        LOG.error(e);
                        result.setStatus(2);
                    }
                    result.addMessage("Timestamp before getting cardinality: " + new Date());
                    String cardinality = msttProjectFilterDAO.getCardinality(spotInternalID, oldMsttID);
                    result.addMessage("Timestamp after getting cardinality: " + new Date());
                    Boolean changeID = "1-1".equals(cardinality);
                    result.addMessage("Timestamp before updating project fields: " + new Date());
                    CommObjectDTO resultUpdate = common.updateProjectFields(pSNextProj, projectDTO, operation, changeID);
                    result.addMessage("Timestamp after getting cardinality: " + new Date());
                    result.getMessages().addAll(resultUpdate.getMessages());
                    result.setStatus(resultUpdate.getStatus());
                    if (result.getStatus() >= 2) {
                        break;
                    }
                    try {
                        result.addMessage("Timestamp before saving project " + pSNextProj + ": " + new Date());
                        result.addMessage("Project has changed: " + pSNextProj.hasChanged());
                        hasChanged = pSNextProj.hasChanged();
                        if (hasChanged) {
                            pSNextProj.save();
                        }
                        result.addMessage("Timestamp after saving " + pSNextProj + ": " + new Date());
                        try {
                            this.projectsToPublish.add(pSNextProj);
                            result.addMessage("Timestamp before updating card table: " + new Date());
                            msttProjectFilterDAO.updateCardTable(spotInternalID, oldMsttID, projectDTO.getProjectID(), pSNextProj.getStringField("ID"));
                            result.addMessage("Timestamp after updating card table: " + new Date());
                        } catch (PSException e) {
                            result.addMessage(String.format("ERROR : Cannot publish the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName));
                            LOG.error(e);
                            result.setStatus(2);
                        }
                    } catch (PSException e) {
                        result.addMessage(String.format("ERROR : Cannot save the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>. Original message: " + e.getMessage(), spotInternalID, oldProjectID, projectName));
                        LOG.error(e);
                        result.setStatus(2);
                    }
                } catch (DataAccessException e) {
                    result.addMessage(String.valueOf(String.format("ERROR : Cannot save the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName)) + ": " + e.getMessage());
                    LOG.error(e);
                    result.setStatus(2);
                    try {
                        pSNextProj.close();
                        break;
                    } catch (PSException e3) {
                        result.addMessage(String.format("ERROR : the API cannot close the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName));
                        LOG.error(e3);
                        result.setStatus(2);
                        return result;
                    }
                }
            } catch (Throwable throwable) {
                try {
                    pSNextProj.close();
                } catch (PSException e) {
                    result.addMessage(String.format("ERROR : the API cannot close the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName));
                    LOG.error(e);
                    result.setStatus(2);
                    return result;
                }
                throw throwable;
            }

            try {
                pSNextProj.close();
            } catch (PSException e) {
                result.addMessage(String.format("ERROR : the API cannot close the project <<SPoT Internal ID:'%s', ID:'%s', Name :'%s'>>", spotInternalID, oldProjectID, projectName));
                LOG.error(e);
                result.setStatus(2);
                return result;
            }

            if (result.getStatus() >= 2 || !operation.equals("Deactivate")) {
                continue;
            }

            try {
                result.addMessage("Timestamp before deactivating project: " + new Date());
                pSNextProj.deactivate();
                result.addMessage("Timestamp after deactivating table: " + new Date());
                result.addMessage("Deactivate project");
                continue;
            } catch (PSException e) {
                result.addMessage("ERROR : The API Cannot deactivate project.");
                LOG.error(e);
                result.setStatus(2);
                return result;
            }
        }
        String success = "Project updated successfully";
        if (!hasChanged) {
            success = "Project was not updated, because its content did not change.";
        }
        String lastMsg = result.getStatus() == 0 ? success : "Project updated with warning";
        result.addMessage(lastMsg);
        return result;
    }

    public CommObjectDTO publishProjects() {
        CommObjectDTO result = new CommObjectDTO();
        result.addMessage(HEADER);
        result.addMessage("Publishing projects, timestamp: " + new Date());
        try {
            result.addMessage("Timestamp before publish: " + new Date());
            for (Project project : projectsToPublish) {
                project.publish();
            }
        } catch (AccessException e) {
            e.printStackTrace();
        } catch (PSException e) {
            e.printStackTrace();
        }
        result.addMessage("Timestamp after publish: " + new Date());
        String lastMsg = result.getStatus() == 0 ? "Project published successfully" : "Project published with warning";
        result.addMessage(lastMsg);
        return result;
    }

}
