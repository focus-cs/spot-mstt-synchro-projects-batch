package com.schneider.mstt.synchro.projects;

import com.sciforma.psnext.api.PSException;
import com.sciforma.psnext.api.Project;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import schneider.mstt.api.CommObjectDTO;
import schneider.mstt.api.ProjectDTO;

@Configuration
@PropertySource("file:${user.dir}/conf/type.properties")
public class Common {

    @Autowired
    Environment env;

    private static final Logger LOG = LogManager.getLogger(Common.class);

    /**
     * File name to save last process date
     */
    private static final String LAST_DATE_FILE = "LastDate";

    /**
     * Format date to save date in %LAST_PROCESS_DATE_FILE_NAME%
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     *
     */
    public static final int ERROR_EXIST_CODE = 2;

    /**
     *
     */
    final transient private File lsatDateFile;

    /**
     *
     */
    public Common() {
        lsatDateFile = new File(LAST_DATE_FILE);
    }

    /**
     *
     * @return @throws IOException
     * @throws ParseException
     */
    public String getLastProcessDate() throws IOException, ParseException {
        String result = null;

        // If the file exist than get the last date
        if (lsatDateFile.exists()) {
            final InputStream ips = new FileInputStream(lsatDateFile);

            final InputStreamReader ipsr = new InputStreamReader(ips);

            try (BufferedReader buff = new BufferedReader(ipsr)) {
                result = buff.readLine();

                final SimpleDateFormat aDateformat = new SimpleDateFormat(DATE_FORMAT);

                aDateformat.parse(result);
            }
        } else // create a new file and write in it the now date.
        {

            result = convertDateToString(new Date());

            writeLineToFile(result);

        }
        return result;
    }

    private void writeLineToFile(final String str) throws IOException {

        // create new file if not exist
        if ((!lsatDateFile.exists()) && (!lsatDateFile.createNewFile())) {
            throw new IOException("ERROR : Cannot create the file << " + LAST_DATE_FILE + " >>");
        }

        final FileWriter fileWriter = new FileWriter(lsatDateFile);

        try (BufferedWriter output = new BufferedWriter(fileWriter)) {
            output.write(str, 0, str.length());
            output.flush();
        }

    }

    public CommObjectDTO updateProjectFields(Project psNextProj, ProjectDTO projectDTO, String operation, Boolean changeID) {
        CommObjectDTO resultUpdate = new CommObjectDTO();

        if ("Deactivate".equals(operation)) {
            Calendar cal = Calendar.getInstance();
            String time = new SimpleDateFormat("yyyyMMdd", new Locale("Locale.US")).format(cal.getTime());
            String newID = projectDTO.getProjectID() + "_" + time;
            String newSpotInID = projectDTO.getInternalID() + "_" + time;

            updateStringField(psNextProj, newSpotInID, resultUpdate, operation, "SPoT Internal ID");
            updateStringField(psNextProj, newID, resultUpdate, operation, "ID");

            return resultUpdate;
        }

        if ("Set".equals(operation)) {
            updateStringField(psNextProj, projectDTO.getInternalID(), resultUpdate, operation, "SPoT Internal ID");
            updateDateField(psNextProj, projectDTO.getStartConstraint(), resultUpdate, operation, "Start Constraint");
        }

        if (changeID) {
            updateStringField(psNextProj, projectDTO.getProjectID(), resultUpdate, operation, "ID");
            updateStringField(psNextProj, projectDTO.getName(), resultUpdate, operation, "Name");
        }

        updateStringField(psNextProj, projectDTO.getProgramCode(), resultUpdate, operation, "Program code");

        if ("NONE".equals(projectDTO.getSharepointCategory())) {
            updateStringField(psNextProj, getMsttType(projectDTO.getType()), resultUpdate, operation, "Project Type");
        } else {
            updateStringField(psNextProj, projectDTO.getType(), resultUpdate, operation, "Project Type");
        }

        updateStringField(psNextProj, projectDTO.getStatus(), resultUpdate, operation, "Status");
        updateStringField(psNextProj, Boolean.valueOf(projectDTO.getLocalFlag()) ? "Local" : "Global", resultUpdate, operation, "Scope");

        updateIntegerField(psNextProj, projectDTO.getBuRanking(), resultUpdate, operation, "BU Ranking");

        updateStringField(psNextProj, projectDTO.getOwningOrga(), resultUpdate, operation, "Portfolio Folder");
        updateStringField(psNextProj, projectDTO.getManager1(), resultUpdate, operation, "Manager 1");
        updateStringField(psNextProj, projectDTO.getPmo(), resultUpdate, operation, "PMO 1");

        updateDateField(psNextProj, projectDTO.getOpenDate(), resultUpdate, operation, "OPEN");
        updateDateField(psNextProj, projectDTO.getSelectDate(), resultUpdate, operation, "SELECT");
        updateDateField(psNextProj, projectDTO.getDoDate(), resultUpdate, operation, "Do");
        updateDateField(psNextProj, projectDTO.getProduceDate(), resultUpdate, operation, "Produce");
        updateDateField(psNextProj, projectDTO.getImplementDate(), resultUpdate, operation, "Implement");
        updateDateField(psNextProj, projectDTO.getSellDate(), resultUpdate, operation, "Sell");

        updateStringField(psNextProj, projectDTO.getSolutionCode(), resultUpdate, operation, "Solution code");
        updateStringField(psNextProj, projectDTO.getOcpProcess(), resultUpdate, operation, "OCP Process");

        updateDateField(psNextProj, projectDTO.getCloseDate(), resultUpdate, operation, "Close");

        return resultUpdate;
    }

    private void updateStringField(Project psNextProj, String value, CommObjectDTO resultUpdate, String operation, String fieldName) {
        try {
            if (!psNextProj.getStringField(fieldName).equals(value)) {
                psNextProj.setStringField(fieldName, value);
            }
        } catch (PSException e) {
            resultUpdate.addMessage(String.format("WARNING : The API cannot %s the << %s >> field with the following value '%s'", new Object[]{operation, fieldName, value}));
            LOG.error(e);
            resultUpdate.setStatus(1);
        }
    }

    private void updateIntegerField(Project psNextProj, Integer value, CommObjectDTO resultUpdate, String operation, String fieldName) {
        try {
            if (psNextProj.getIntField(fieldName) != value) {
                psNextProj.setIntField(fieldName, value);
            }
        } catch (PSException e) {
            resultUpdate.addMessage(String.format("WARNING : The API cannot %s the << %s >> field with the following value '%s'", new Object[]{operation, fieldName, value}));
            LOG.error(e);
            resultUpdate.setStatus(1);
        }
    }

    private void updateDateField(Project psNextProj, Date date, CommObjectDTO resultUpdate, String operation, String fieldName) {
        try {
            if (!psNextProj.getDateField(fieldName).equals(date)) {
                psNextProj.setDateField(fieldName, date);
            }
        } catch (PSException e) {
            resultUpdate.addMessage(String.format("WARNING : The API cannot %s the << %s >> field with the following value '%s'", new Object[]{operation, fieldName, date}));
            LOG.error(e);
            resultUpdate.setStatus(1);
        }
    }

    private String getMsttType(String spotType) {

        Map<String, Object> map = new HashMap();
        for (Iterator it = ((AbstractEnvironment) env).getPropertySources().iterator(); it.hasNext();) {
            PropertySource propertySource = (PropertySource) it.next();
            if (propertySource instanceof MapPropertySource) {
                map.putAll(((MapPropertySource) propertySource).getSource());
            }
        }

        if (map.containsKey(spotType)) {
            return String.valueOf(map.get(spotType));
        } else {
            return spotType;
        }

    }

    /**
     * Write the now date in %LAST_PROCESS_DATE_FILE_NAME%
     *
     * @throws IOException
     *
     */
    public void updateLastProcessDate() throws IOException {
        writeLineToFile(convertDateToString(new Date()));
    }

    /**
     * Convert input date to string format year+month+day
     *
     * @param dateToConvert
     * @return
     */
    public String convertDateToString(final Date dateToConvert) {
        final SimpleDateFormat aDateformat = new SimpleDateFormat(DATE_FORMAT);

        return aDateformat.format(dateToConvert);
    }

    public void saveLogs(List<String> logs, String userID) {
        try {
            Properties pro = readProperties("/admin.log.properties");
            String outDir = pro.getProperty("outputDir");
            if (!outDir.endsWith(File.separator)) {
                outDir = outDir + "/";
            }

            Calendar cal = Calendar.getInstance();
            String pref = cal.get(1) + "-" + String.format("%02d", new Object[]{Integer.valueOf(cal.get(2) + 1)}) + "-" + String.format("%02d", new Object[]{Integer.valueOf(cal.get(5))}) + "_"
                    + String.format("%02d", new Object[]{Integer.valueOf(cal.get(11))}) + "-" + String.format("%02d", new Object[]{Integer.valueOf(cal.get(12))}) + "-" + String.format("%02d", new Object[]{Integer.valueOf(cal.get(13))});

            File outPutDir = new File(outDir);
            boolean dirCreated = outPutDir.exists();

            if (!dirCreated) {
                if (!(dirCreated = outPutDir.mkdir())) {
                    LOG.warn("could not make dir : " + outDir);
                }
            }

            if (dirCreated) {
                FileWriter tracefile = new FileWriter(outDir + userID + "_" + pref + ".log");

                for (String line : logs) {
                    tracefile.write(line + "\n");
                }
                tracefile.close();
            }

        } catch (IOException e) {
            LOG.warn("could not write trace file", e);
        }
    }

    public Properties readProperties(String path)
            throws IOException {
        Properties properties = new Properties();
        InputStream resourceAsStream = Common.class.getResourceAsStream(path);
        if (resourceAsStream == null) {
            throw new IOException("<" + path + "> not found in classpath.");
        }
        properties.load(resourceAsStream);
        return properties;
    }

}
