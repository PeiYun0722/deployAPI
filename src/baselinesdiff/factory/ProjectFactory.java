/*
* Licensed Materials - Property of IBM
* 5725-B69 5655-Y17 5724-Y00 5724-Y17 5655-V84
* Copyright IBM Corp. 1987, 2015. All Rights Reserved.
*
* Note to U.S. Government Users Restricted Rights: 
* Use, duplication or disclosure restricted by GSA ADP Schedule 
* Contract with IBM Corp.
*/

package baselinesdiff.factory;

import baselinesdiff.helper.RTSHelper;
import ilog.rules.teamserver.brm.IlrBaseline;
import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.model.IlrApplicationException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.IlrSessionHelper;
public class ProjectFactory extends Factory  {
	public static String LOANVALIDATION_RULES = "loanvalidation-rules";
	/**
	 * A factory to manage projects.
	 *
	 * @param session The session.
	 */
	public ProjectFactory(IlrSession session) {
		super(session);
	}


	/**
	 * Creates a project.
	 *
	 * @param name The name of the project to create.
	 * @throws IlrApplicationException
	 */
	public void createProject (String name) throws IlrApplicationException {
		// Retrieve the project
		IlrRuleProject project = (IlrRuleProject) IlrSessionHelper.getProjectNamed(session, name);
		// Delete the project if it already exists
		if (project != null) {
			try {
				session.setWorkingBaseline(null);
				session.eraseProject(project);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		// Create the project, and set the current baseline
		project = (IlrRuleProject) IlrSessionHelper.createRuleProject(session, name);
		IlrBaseline currentBaseline = IlrSessionHelper.getCurrentBaseline(session, project);
	    session.setWorkingBaseline(currentBaseline);
	    // get loanvalidation-rules project
	    IlrRuleProject loanvalidation = (IlrRuleProject) IlrSessionHelper.getProjectNamed(session, LOANVALIDATION_RULES);
	    // A BOM is needed so it is retrieved from LOANVALIDATION_RULES
	    RTSHelper.setBaselineDependencies(session, new String [] {LOANVALIDATION_RULES}, 
					      new String [] {loanvalidation.getCurrentBaseline().getName()});
	}

}
