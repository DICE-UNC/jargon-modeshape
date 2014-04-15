package org.irods.jargon.modeshape.connector.unittest;

import org.irods.jargon.modeshape.connector.IRODSWriteableConnectorRepoCustomAuthTest;
import org.irods.jargon.modeshape.connector.IRODSWriteableConnectorRepoTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ IRODSWriteableConnectorRepoTest.class,
		IRODSWriteableConnectorRepoCustomAuthTest.class })
public class AllTests {

}
