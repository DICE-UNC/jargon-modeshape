/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import org.irods.jargon.core.connection.IRODSAccount;
import org.modeshape.jcr.security.SecurityContext;

/**
 * iRODS specific security context for use in custom ModeShape authenticator
 * 
 * @author Mike
 * 
 */
public class IrodsSecurityContext implements SecurityContext {

	private final IRODSAccount irodsAccount;

	/**
	 * Default constructor takes the iRODS account used to log in
	 * 
	 * @param irodsAccount
	 *            {@link IRODSAccount} associated with user
	 */
	public IrodsSecurityContext(final IRODSAccount irodsAccount) {
		if (irodsAccount == null) {
			throw new IllegalArgumentException("null irodsAccount");
		}
		this.irodsAccount = irodsAccount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.security.SecurityContext#isAnonymous()
	 */
	@Override
	public boolean isAnonymous() {
		return irodsAccount.isAnonymousAccount();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.security.SecurityContext#getUserName()
	 */
	@Override
	public String getUserName() {
		return irodsAccount.getUserName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.security.SecurityContext#hasRole(java.lang.String)
	 */
	@Override
	public boolean hasRole(String roleName) {
		// TODO: equate user group to role? Noop for now.
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.security.SecurityContext#logout()
	 */
	@Override
	public void logout() {
		// does nothing

	}

	public IRODSAccount getIrodsAccount() {
		return irodsAccount;
	}

}
