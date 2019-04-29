package com.schneider.mstt.synchro.projects.test;

import org.apache.log4j.Logger;

import com.schneider.mstt.synchro.projects.exceptions.BadSessionException;
import com.schneider.mstt.synchro.projects.exceptions.ErrorLoggingOutException;
import com.schneider.mstt.synchro.projects.exceptions.LoginException;
import com.sciforma.psnext.api.AccessException;
import com.sciforma.psnext.api.PSException;
import com.sciforma.psnext.api.Session;

public class SessionContainer {

    private static Session aSession = null;

    private static final Logger LOG = Logger.getLogger(SessionContainer.class);

    /**
     *
     * @param url
     * @param login
     * @param password
     * @throws BadSessionException
     * @throws LoginException
     */
    public static void createSession(final String url, final String login, final String password) throws BadSessionException, LoginException {
        if (aSession == null) {
            try {
                aSession = new Session(url);
            } catch (PSException e) {
                LOG.error(e);
                throw new BadSessionException(e);
            }
            try {
                aSession.login(login, password.toCharArray());
            } catch (PSException e) {
                LOG.error(e);
                throw new LoginException(e);
            }
        }
    }

    public static Session getSession() {
        return aSession;
    }

    /**
     * @throws ErrorLoggingOutException
     *
     */
    public static void logOut() throws ErrorLoggingOutException {
        try {
            aSession.logout();
        } catch (AccessException e) {
            throw new ErrorLoggingOutException("Error logging out", e);
        } catch (PSException e) {
            throw new ErrorLoggingOutException("Error logging out", e);
        }
    }

}
