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

import ilog.rules.teamserver.brm.IlrBrmPackage;
import ilog.rules.teamserver.brm.IlrRule;
import ilog.rules.teamserver.brm.IlrRuleArtifactTag;
import ilog.rules.teamserver.model.IlrCommitableObject;
import ilog.rules.teamserver.model.IlrDefaultSearchCriteria;
import ilog.rules.teamserver.model.IlrElementDetails;
import ilog.rules.teamserver.model.IlrElementHandle;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.permissions.IlrRoleRestrictedPermissionException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EReference;

public class Factory {

	/**
	 * A session to Rule Team Server.
	 */
	IlrSession session;

	/**
	 * Creates an instance of a factory.
	 *
	 * @param session The session.
	 */
	public Factory(IlrSession session) {
		this.session = session;
	}

	/**
	 * Retrieve a rule using its name.
	 *
	 * @param name The name of the rule.
	 * @throws IlrRoleRestrictedPermissionException
	 * @throws IlrObjectNotFoundException
	 */
	public IlrRule getRule(String name)	throws IlrRoleRestrictedPermissionException,IlrObjectNotFoundException {
		IlrBrmPackage brm = session.getBrmPackage();
		// Set the features to filter.
		List<EAttribute> features = new ArrayList<EAttribute>();
		// Here the filter is only based on the name
		features.add(brm.getModelElement_Name());
		// Set the values of the features
		List<String> values = new ArrayList<String>();
		// Here the name must be equal to the one passed in the parameter
		values.add(name);
		// The search applies only on rule objects
		IlrDefaultSearchCriteria searchCriteria = new IlrDefaultSearchCriteria(brm.getRule(), features, values);
		// Execute the search
		List<?> list = session.findElements(searchCriteria);
		// Retrieve the first element
		IlrRule rule = (IlrRule) list.get(0);
		return rule;
	}

	/**
	 * Sets the definition of a rule.
	 */
	public static IlrElementDetails setDefinition(IlrSession session, IlrCommitableObject co, IlrElementHandle definition, String body) throws IlrObjectNotFoundException {
		IlrBrmPackage brm = session.getModelInfo().getBrmPackage();
		// Retrieve the feature corresponding to the "definition" of the rule
		EReference rule_definition = brm.getRuleArtifact_Definition();
		// Create one if it does not yet exist
		if (definition == null) definition = (IlrElementHandle)session.createElement(brm.getDefinition());
		IlrElementDetails defDetails = session.getElementDetails(definition);
		// Set the value
		defDetails.setRawValue(brm.getDefinition_Body(), body);
		// Add a modified element to the commit wrapper
		co.addModifiedElement(rule_definition, defDetails);
		return defDetails;
	}

	/**
	 * Removes a tag from the element.
	 */
	public static void removeTag(IlrSession session, IlrCommitableObject co, List<IlrRuleArtifactTag> tags, String name) throws IlrObjectNotFoundException {
		IlrBrmPackage brm = session.getModelInfo().getBrmPackage();
		// Loop thru the tags , and find the one we have to delete
		Iterator<IlrRuleArtifactTag> iter = tags.iterator();
		while (iter.hasNext()) {
			 IlrRuleArtifactTag tag = iter.next();
			 if (tag.getName().equals(name)) {
				 co.addDeletedElement(brm.getRuleArtifact_Tags(), tag);
			 }
		}
	}

	/**
	 * Adds a tag to an element.
	 */
	public static IlrElementDetails addTag(IlrSession session, IlrCommitableObject co, String name, String value) throws IlrObjectNotFoundException {
		IlrBrmPackage brm = session.getModelInfo().getBrmPackage();
		// Retrieve the "tags" feature
		EReference rule_tag = brm.getRuleArtifact_Tags();
		// Retrieve the "tag" name feature
		EAttribute tag_name = brm.getTag_Name();
		// Retrieve the "tag" value feature
		EAttribute tag_value = brm.getTag_Value();
		// Create a "tag" element
		IlrElementHandle tag = session.createElement(brm.getRuleArtifactTag());
		IlrElementDetails tagDetails = session.getElementDetails(tag);
		// Set the tag name and value
		tagDetails.setRawValue(tag_name, name);
		tagDetails.setRawValue(tag_value, value);
		// The "tags" feature has been modified
		co.addModifiedElement(rule_tag, tagDetails);
		return tagDetails;
	}


}
