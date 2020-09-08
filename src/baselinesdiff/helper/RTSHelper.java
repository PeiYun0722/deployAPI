/*
* Licensed Materials - Property of IBM
* 5725-B69 5655-Y17 5724-Y00 5724-Y17 5655-V84
* Copyright IBM Corp. 1987, 2015. All Rights Reserved.
*
* Note to U.S. Government Users Restricted Rights: 
* Use, duplication or disclosure restricted by GSA ADP Schedule 
* Contract with IBM Corp.
*/

package baselinesdiff.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.ecore.EReference;

import ilog.rules.teamserver.brm.IlrBOMPathEntry;
import ilog.rules.teamserver.brm.IlrBranch;
import ilog.rules.teamserver.brm.IlrBrmPackage;
import ilog.rules.teamserver.brm.IlrDependency;
import ilog.rules.teamserver.brm.IlrPackageElement;
import ilog.rules.teamserver.brm.IlrProjectBOMEntry;
import ilog.rules.teamserver.brm.IlrProjectElement;
import ilog.rules.teamserver.brm.IlrProjectInfo;
import ilog.rules.teamserver.brm.IlrRule;
import ilog.rules.teamserver.brm.IlrRulePackage;
import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.brm.IlrScopeElement;
import ilog.rules.teamserver.model.IlrApplicationException;
import ilog.rules.teamserver.model.IlrCommitableObject;
import ilog.rules.teamserver.model.IlrDefaultSearchCriteria;
import ilog.rules.teamserver.model.IlrElementDetails;
import ilog.rules.teamserver.model.IlrModelConstants;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.permissions.IlrRoleRestrictedPermissionException;

/**
 * A helper that interacts with the Rule Team Server model.
 */

public class RTSHelper {

	static String RULE_PACKAGE = "rulePackage";
	static String RULE_PROJECT = "project";
	static String PLATFORM = "platform:/";


 	/**
 	 * The elements of the projects.
     *
 	 */
 	public static HashMap<String,IlrProjectElement> getProjectElements (IlrSession session, IlrRuleProject project, int scope) throws IlrRoleRestrictedPermissionException, IlrObjectNotFoundException {
		IlrBrmPackage brm = session.getBrmPackage();
		// Set the filter features
		List<EReference> features = new ArrayList<EReference> ();
		// Filter on the project element
		features.add(brm.getProjectElement_Project());
		List<IlrRuleProject> values = new ArrayList<IlrRuleProject> ();
		// Set the desired project
		values.add(project);
		// Focus the search only on project elements
		IlrDefaultSearchCriteria searchCriteria =
			new IlrDefaultSearchCriteria(brm.getProjectElement(),
										features,
										values,
										null,
										scope,
										null,
										true);
		// Execute the query
		List<?> list = session.findElements(searchCriteria);
		// Store retrieved elements
		HashMap<String,IlrProjectElement> ret = new HashMap<String,IlrProjectElement>();
		Iterator<?> iter = list.iterator();
		while (iter.hasNext()) {
			IlrProjectElement element = (IlrProjectElement) iter.next();
			String name = getQualifiedName(session, element);
			ret.put(name, element);
		}
		return ret;
	}

	/**
	 * Returns a customized name of an element. If the element is a scoped element,
	 * generate a name as the concatenation of the rule task name and the rule name.
	 *
	 * @param element The element.
	 * @return The rule name or the rule task name.
	 * @throws IlrObjectNotFoundException
	 */
	private static String getElementName (IlrElementDetails element) throws IlrObjectNotFoundException {
		if (element==null) return null;
		// Get the name
		String name = element.getName();
		// Return it when one is found
		if (name!=null) return name;
		// Handle ruleflow task names
		if (element instanceof IlrScopeElement) {
			IlrScopeElement scope = (IlrScopeElement) element;
			String ruletaskname = "'" + scope.getRuleTaskName() +  "'";
			IlrRule rule = scope.getRule();
			if (rule!=null) {
				return ruletaskname + "." +  rule.getName();
			}
			return ruletaskname;
		}
		return null;
	}

	/**
	 * Gets the fully-qualified name of an element.
	 *
	 * @param session The session to Rule Team Server.
	 * @param elt The element.
	 * @return The fully-qualified name of an element.
	 * @throws IlrObjectNotFoundException
	 */
	public static String getQualifiedName (IlrSession session, IlrElementDetails elt) throws IlrObjectNotFoundException {
		// Return a fully-qualified name of an element
		// For a rule named 'myRule' located in the folder 'myFolder/mySubFolder' this will return
		// brm.ActionRule:myFolder/mySubFolder/myRule
		String ret = null;
		String prefix = elt.getType() + ":" ;
		if (elt instanceof IlrPackageElement) {
			IlrRulePackage folder = ((IlrPackageElement)elt).getRulePackage();
			java.lang.String[] paths = session.getHierarchyPath(folder);
			if (paths==null) return prefix + elt.getName();
			if (paths.length==0) return prefix + elt.getName();
			StringBuffer sb = new StringBuffer (prefix);
			for (int i = 0; i < paths.length; i++) {
				sb.append(paths[i]).append("/");
			}
			sb.append(elt.getName());
			ret = sb.toString();
		} else  {
			ret = prefix + elt.getName();
		}
		return ret;
	}




	/**
	 * Sets dependencies to the project associated with the session.
	 *
	 * @param session The session.
	 * @param destProjects The projects dependencies.
	 * @param destBaselines The associated baselines.
	 * @throws IlrApplicationException
	 */
	public static void setBaselineDependencies(IlrSession session, String [] destProjects, String [] destBaselines) throws IlrApplicationException {
		IlrBranch current = (IlrBranch)session.getWorkingBaseline();
		// Get the representation of the model object 'Project Info'
		IlrProjectInfo projectInfo = session.getWorkingBaseline().getProjectInfo();
		// Get the BRM package
		IlrBrmPackage brm = session.getModelInfo().getBrmPackage();
		// Retrieve its project dependencies
		List<?> deps = session.getElementsFromReference(projectInfo,brm.getProjectInfo_Dependencies(), IlrModelConstants.ELEMENT_DETAILS);
		// Create a commitable object. Hold element to commit in the database along
		// with its details and/or a list of its contained elements to add or delete during the commit.
		IlrCommitableObject co = new IlrCommitableObject(projectInfo);
		// Clear the dependencies of the current project before adding the passed ones
		for (Iterator<?> iterator = deps.iterator(); iterator.hasNext();) {
			// Get the current dependency
			IlrElementDetails dependency = (IlrElementDetails) iterator.next();
			// Remove it
			co.addDeletedElement(brm.getProjectInfo_Dependencies(),	dependency);
			String name = "" + dependency.getRawValue(brm.getDependency_ProjectName());
			// Remove the BOM path entry of the current dependency
			removeProjectBOMPathEntry(projectInfo, co, name);
		}
		// Persist the work to the database
		session.commit(current, co);
		// Add the dependencies
		addBaselineDependencies(session, destProjects, destBaselines);
	}

	/**
	 * Removes the BOM path entry associated with the project.
	 */
	private  static  void removeProjectBOMPathEntry(IlrProjectInfo projectInfo,IlrCommitableObject cProjectInfo, String projectName) throws IlrApplicationException {
		// Retrieve the current session
		IlrSession session = projectInfo.getSession();
		// Get the BRM package
		IlrBrmPackage brmPackage = session.getModelInfo().getBrmPackage();
		// Retrieve all BOM path entries of the given project
		List<?> bomPathEntries = projectInfo.getBomPathEntries();
		if (bomPathEntries != null) {
			for (Iterator<?> bomPathEntryIter = bomPathEntries.iterator(); bomPathEntryIter.hasNext();) {
				IlrBOMPathEntry entry = (IlrBOMPathEntry) bomPathEntryIter.next();
				if (projectName.equals(((IlrProjectBOMEntry) entry).getProjectName())) {
					// Register this BOM path entry as a deleted one
					cProjectInfo.addDeletedElement(brmPackage.getProjectInfo_BomPathEntries(), entry);
					break;
				}
			}
		}
	}

	/**
	 * Adds dependencies to the project associated with the session.
	 */
	private static void addBaselineDependencies (IlrSession session, String [] destProjects, String [] destBaselines) throws IlrApplicationException {
		IlrBranch current = (IlrBranch)session.getWorkingBaseline();
		// Get the representation of the model object 'Project Info'
		IlrProjectInfo projectInfo = session.getWorkingBaseline().getProjectInfo();
		// Get the BOM path entries
		List<?> bomPathEntries = projectInfo.getBomPathEntries();
		int index = bomPathEntries.size();
		// Get the BRM package
		IlrBrmPackage brm = session.getModelInfo().getBrmPackage();
		// Create a commitable object. Hold element to commit in the database along
		// with its details and/or a list of its contained elements to add or delete during the commit.
		IlrCommitableObject co = new IlrCommitableObject(projectInfo);
		for (int i = 0; i < destProjects.length; i++) {
			// Retrieve the details of newly created dependencies
			IlrDependency depDetails = (IlrDependency) session.getElementDetails(session.createElement(brm.getDependency()));
			// Set the dependencies name
			depDetails.setRawValue(brm.getDependency_ProjectName(), destProjects[i]);
			// Set the dependencies baseline name
			depDetails.setRawValue(brm.getDependency_BaselineName(), destBaselines[i]);
			// Add this dependency as a modified element
			co.addModifiedElement(brm.getProjectInfo_Dependencies(), depDetails);
			// Add the corresponding BOM path entry
			addProjectBOMPathEntry( projectInfo, co, destProjects[i], i+index);
		}
		// Persist the work in the database
		session.commit(current, co);
	}


	/**
	 * Adds the BOM path entry associated with the project.
	 */
	private static void addProjectBOMPathEntry(IlrProjectInfo projectInfo, IlrCommitableObject cProjectInfo, String projectName, int order) throws IlrApplicationException {
		// Retrieve the current session
		IlrSession session = projectInfo.getSession();
		// Get the BRM package
		IlrBrmPackage brmPackage = session.getModelInfo().getBrmPackage();
		// Retrieve the details of a newly created BOM path entry
		IlrProjectBOMEntry entry = (IlrProjectBOMEntry) session.getElementDetails(session.createElement(brmPackage.getProjectBOMEntry()));
		if (entry != null) {
			// Set the  name, URL, and order properties
			entry.setRawValue(brmPackage.getProjectBOMEntry_ProjectName(), projectName);
			entry.setRawValue(brmPackage.getBOMPathEntry_Url(), PLATFORM + projectName);
			entry.setRawValue(brmPackage.getBOMPathEntry_Order(), new Integer(order));
			// Add this BOM path entry as a modified element
			cProjectInfo.addModifiedElement(brmPackage.getProjectInfo_BomPathEntries(), entry);
		}
	}


}
