/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.util.Map;

import javax.jcr.Credentials;

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.security.AuthenticationProvider;

/**
 * Custom iRODS based authentication provider
 * 
 * @author Mike Conway - DICE
 * 
 */
public class IrodsAuthenticationProvider implements AuthenticationProvider {

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
		// TODO Auto-generated method stub
		return null;
	}

}
