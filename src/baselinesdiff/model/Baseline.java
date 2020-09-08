/*
* Licensed Materials - Property of IBM
* 5725-B69 5655-Y17 5724-Y00 5724-Y17 5655-V84
* Copyright IBM Corp. 1987, 2015. All Rights Reserved.
*
* Note to U.S. Government Users Restricted Rights: 
* Use, duplication or disclosure restricted by GSA ADP Schedule 
* Contract with IBM Corp.
*/

package baselinesdiff.model;

import java.io.IOException;

import baselinesdiff.Connection;
import baselinesdiff.factory.ProjectFactory;
import ilog.rules.teamserver.brm.IlrBaseline;
import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.model.IlrApplicationException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.IlrSessionHelper;


/**
 * Represents a project in Rule Team Server.
 *
 */
public class Baseline {
	/**
	 * The project name.
	 */
	String projectname;

	/**
	 * The baseline.
	 */
	String baselinename;

	/**
	 * The baseline.
	 */
	IlrBaseline baseline;

	/**
	 * The underlying session to Rule Team Server.
	 */
	IlrSession session;

	/**
	 * The Rule Team Server project.
	 */
	IlrRuleProject project;

	/**
	 * The connection used.
	 */
	Connection connection;



	/**
	 * Creates an instance of a baseline and eventually creates it.
	 */
	public Baseline(String projectname, String baseline, Connection connection) throws Exception {
		super();
		this.projectname = projectname;
		this.baselinename = baseline;
		this.connection=connection;
		this.session = connection.getSession();
		setCurrentProject();
	}

	@SuppressWarnings("unused")
	private Baseline() {
	}

	/**
	 * @return The current session to Rule Team Server.
	 */
	public  IlrSession getSession () {
		return session;
	}

	/**
	 * Sets the baseline to the session.
	 * @throws IlrApplicationException
	 * @throws IOException
	 */
	private void setCurrentProject() throws  Exception  {
		project = (IlrRuleProject) IlrSessionHelper.getProjectNamed(session, projectname);
		if (project == null) {
			ProjectFactory projectFactory  = new ProjectFactory (session);
			projectFactory.createProject (projectname);
			project = (IlrRuleProject) IlrSessionHelper.getProjectNamed(session, projectname);
		}
		if (baselinename.equalsIgnoreCase(".")) {
		  baseline = IlrSessionHelper.getCurrentBaseline(session, project);
		} else {
		  baseline = IlrSessionHelper.getBaselineNamed(session, project, baselinename);
		}
		if (baseline==null) {
			throw new IllegalArgumentException ("Baseline " + baselinename + " does not exists");
		}
		session.setWorkingBaseline(baseline);
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Project : " + projectname + " Baseline : " + baselinename + " " + connection.toString();
	}
	
	//==========================================================================
	
	public IlrBaseline getBaseline() {
		return baseline;
	}

	public void setBaseline(IlrBaseline baseline) {
		this.baseline = baseline;
	}

	public IlrRuleProject getProject() {
		return project;
	}

	public void setProject(IlrRuleProject project) {
		this.project = project;
	}

	public void setSession(IlrSession session) {
		this.session = session;
	}

}
