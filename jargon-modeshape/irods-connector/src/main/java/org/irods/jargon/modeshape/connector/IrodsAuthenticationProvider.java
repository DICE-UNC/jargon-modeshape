/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import javax.annotation.PostConstruct;

import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.SimpleCredentials;
import org.apache.commons.lang3.StringUtils;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.AuthenticationException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.JargonRuntimeException;
import org.irods.jargon.core.pub.IRODSFileSystemSingletonWrapper;
import static org.irods.jargon.modeshape.connector.IRODSWriteableConnector.log;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.security.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom iRODS based authentication provider
 * 
 * @author Mike Conway - DICE
 * 
 */
public class IrodsAuthenticationProvider implements AuthenticationProvider {

	static Logger log = LoggerFactory
			.getLogger(IrodsAuthenticationProvider.class);

	/**
	 * TODO: where to get preset info?
	 */
	private String host;
	private int port;
	private String zone;
    
	/**
	 * 
	 */
	public IrodsAuthenticationProvider() {
        
        
        
        Properties irodsConfigParams = getIRODSPropertiesFile();
        if (!irodsConfigParams.isEmpty()){
            host = irodsConfigParams.getProperty("host");
            port = Integer.parseInt(irodsConfigParams.getProperty("port"));
            zone = irodsConfigParams.getProperty("zone");
        } else {
            log.error("irodsConfigParams is empty and failed to initialize the irodsaccount");
            throw new RuntimeException("IRODS account was not initialized by the properties file");
        }
        
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.security.AuthenticationProvider#authenticate(javax.
	 * jcr.Credentials, java.lang.String, java.lang.String,
	 * org.modeshape.jcr.ExecutionContext, java.util.Map)
	 */
	@Override
	public ExecutionContext authenticate(Credentials credentials,
			String repositoryName, String workspaceName,
			ExecutionContext repositoryContext,
			Map<String, Object> sessionAttributes) {

		log.info("authenticate()");

		// FIXME: of course remove this!
		log.info("credentials:{}", credentials);

		IRODSAccount irodsAccount = null;
		if (credentials instanceof SimpleCredentials) {
			SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
			log.info("simple credential");
			try {
				irodsAccount = IRODSAccount.instance(host, port,
						simpleCredentials.getUserID(), new String(
								simpleCredentials.getPassword()), "", zone, "");
			} catch (JargonException e) {
				log.error("unable to create irodsAccount", e);
				throw new JargonRuntimeException(e);
			}

		} else if (credentials instanceof GuestCredentials) {
			log.info("guest credentials");
			try {
				irodsAccount = IRODSAccount.instanceForAnonymous(host, port,
						"", zone, "");
			} catch (JargonException e) {
				log.error("unable to create irodsAccount", e);
				throw new JargonRuntimeException(e);
			}
		} else {
			throw new JargonRuntimeException("unknown credentials type");
		}

		try {
			log.info("authenticate....");
			IRODSFileSystemSingletonWrapper.instance()
					.getIRODSAccessObjectFactory()
					.authenticateIRODSAccount(irodsAccount);

			IrodsSecurityContext irodsSecurityContext = new IrodsSecurityContext(
					irodsAccount);
			return repositoryContext.with(irodsSecurityContext);

		} catch (AuthenticationException ae) {
			log.error("authentication exception", ae);
			return null;
		} catch (JargonException e) {
			log.error("general JargonException during authentication", e);
			throw new JargonRuntimeException(
					"general exception during authentication");
		}

	}
    
    
    
    private Properties getIRODSPropertiesFile() {

        Properties gfJvmProps = System.getProperties();
        Properties irodslConfigProps = new Properties();

        if (gfJvmProps.containsKey("irods.config.file")) {
            String irodsConfigFileName
                    = gfJvmProps.getProperty("irods.config.file");

            if (StringUtils.isNotBlank(irodsConfigFileName)) {
                // load the configuration file
                log.info("irodsConfigFileName={}", irodsConfigFileName);

                InputStream is = null;
//                File irodsConfigFile = null;
                
                try {
//                    irodsConfigFile = new File(irodsConfigFileName);
                    is = new FileInputStream(new File(irodsConfigFileName));
                    
                    irodslConfigProps.load(is);

                    for (String key : irodslConfigProps.stringPropertyNames()) {
                        log.debug(
                                "key={}:value={}", new Object[]{key,
                                    irodslConfigProps.getProperty(key)});
                    }

//
//                } catch (FileNotFoundException ex) {
//                    log.warn("specified config file was not found", ex);
                } catch (IOException ex) {
                    log.warn("IO error occurred", ex);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ex) {
                            log.warn("failed to close the opened local config file", ex);
                        }
                    }
                }

            } else {
                // irodsConfigFileName is null or empty
                log.error("irodsConfigFileName is null or empty");
            }
        } else {
            // no entry within jvm options
            log.error("irods.config.file is not included in the JVM options");

        }
        return irodslConfigProps;
    }
    
    

}
