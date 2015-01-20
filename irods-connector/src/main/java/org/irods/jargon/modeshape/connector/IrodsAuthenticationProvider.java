/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.SimpleCredentials;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.AuthenticationException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.JargonRuntimeException;
import org.irods.jargon.core.pub.IRODSFileSystemSingletonWrapper;
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
	private final String host = "fedzone1.irods.org";
	private final int port = 1247;
	private final String zone = "fedZone1";

	/**
	 * 
	 */
	public IrodsAuthenticationProvider() {
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

}
