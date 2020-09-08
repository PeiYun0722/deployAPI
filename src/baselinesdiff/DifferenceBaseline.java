/*
* Licensed Materials - Property of IBM
* 5725-B69 5655-Y17 5724-Y00 5724-Y17 5655-V84
* Copyright IBM Corp. 1987, 2015. All Rights Reserved.
*
* Note to U.S. Government Users Restricted Rights: 
* Use, duplication or disclosure restricted by GSA ADP Schedule 
* Contract with IBM Corp.
*/

package baselinesdiff;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;

import baselinesdiff.helper.JsonParse;
import baselinesdiff.helper.ToolHelper;
import baselinesdiff.helper.TxtHelper;
import baselinesdiff.model.Baseline;
import ilog.rules.teamserver.brm.IlrActionRule;
import ilog.rules.teamserver.brm.IlrBaseline;
import ilog.rules.teamserver.brm.IlrDecisionTable;
import ilog.rules.teamserver.model.BranchHelper;
import ilog.rules.teamserver.model.Change;
import ilog.rules.teamserver.model.IlrApplicationException;
import ilog.rules.teamserver.model.IlrConnectException;
import ilog.rules.teamserver.model.IlrElementDetails;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.IlrSessionHelper;
import ilog.rules.teamserver.model.MergeOperation;
import ilog.rules.teamserver.model.permissions.IlrPermissionException;

/**
 * Defines the class that computes the differences between Rule Team Server
 * baselines.
 */
public class DifferenceBaseline {

	public final static String ActionRule = "brm.ActionRule";
	public final static String DecisionTable = "brm.DecisionTable";
	public final static String ActionRuleTitle = "[動作規則內容差異]";
	public final static String DecisionTablePrecondTitle = "[決策表前置條件差異]";
	public final static String DecisionTableTitle = "[決策表內容差異]";
	public final static String RulePath = "[規則路徑異動]";
	public static String ErrorMesage = "";
	public static Logger logger = Logger.getLogger(DifferenceBaseline.class);
	public static String user;
	public static String password;
	public static String url;
	public static String datasource;
	public static String projectName;
	public static String baseline_M;
	public static String baseline_U;
	public static String filePath;
	public static String diffReportName;
	public static String ChecklistName;

	public static TxtHelper diffTxtHelper = new TxtHelper();
	public static TxtHelper listTxtHelper = new TxtHelper();

	public static ArrayList<String> ruleDefIdList = null;
	public static String[] word = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q",
			"r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
			"M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "?", "/" };

	/*
	 * public static void main(String propertiesName, String srNo) throws Exception
	 * {
	 * 
	 * init(propertiesName, srNo); File outputDirFile = new File(filePath); if
	 * (!outputDirFile.exists()) { outputDirFile.mkdirs(); }
	 * diffTxtHelper.initWriteFile(filePath + File.separator + diffReportName + "_"
	 * + getDate() + ".txt"); listTxtHelper.initWriteFile(filePath + File.separator
	 * + ChecklistName + "_" + getDate() + ".txt"); getActionRule();
	 * diffTxtHelper.writeText("\n"); Thread.sleep(1000); getDecisionTable();
	 * diffTxtHelper.closeText(); listTxtHelper.closeText();
	 * 
	 * }
	 */
	public static String doMergeProject(String propertiesName, String srNo) throws Exception {
		init(propertiesName, srNo);
		File outputDirFile = new File(filePath);
		if (!outputDirFile.exists()) {
			outputDirFile.mkdirs();
		}
		diffTxtHelper.initWriteFile(filePath + File.separator + diffReportName + "_" + getDate() + ".txt");
		listTxtHelper.initWriteFile(filePath + File.separator + ChecklistName + "_" + getDate() + ".txt");
		getActionRule();
		diffTxtHelper.writeText("\n");
		Thread.sleep(1000);
		getDecisionTable();
		diffTxtHelper.closeText();
		listTxtHelper.closeText();
		return ErrorMesage;
	}

	public static void init(String propertiesName, String srNo) {

		Properties prop = new Properties();
		InputStream in;
		try {
			// in = new BufferedInputStream(new FileInputStream(new File("src/properties/" +
			// propertiesName + ".properties")));
			in = new BufferedInputStream(new FileInputStream(new File(
					DifferenceBaseline.class.getResource("/properties/" + propertiesName + ".properties").getFile())));
			prop.load(in);

		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
			diffTxtHelper.writeText("無Properties設定檔 : " + e);
			logger.error("無Properties設定檔");
			logger.error(e.getMessage(), e);
			ErrorMesage = "差異報表明細內容有誤！";
		} catch (IOException e) {
			System.out.println(e.getMessage());
			diffTxtHelper.writeText("Properties讀取錯誤 : " + e);
			logger.error("Properties讀取錯誤");
			logger.error(e.getMessage(), e);
			ErrorMesage = "差異報表明細內容有誤！";
		}

		user = prop.getProperty("odmDC.user");
		password = prop.getProperty("odmDC.password");
		url = prop.getProperty("odmDC.url");
		datasource = prop.getProperty("odmDC.datasource");
		projectName = prop.getProperty("odmDC.projectName");
		baseline_M = prop.getProperty("odmDC.baseline_M");
		baseline_U = prop.getProperty("odmDC.baseline_U");
		filePath = prop.getProperty("out.filePath") + "/" + srNo;
		diffReportName = prop.getProperty("out.report.name.diff");
		ChecklistName = prop.getProperty("out.report.name.list");

	}

	public static void getActionRule() throws Exception {
		diffTxtHelper.writeText("[[動作規則差異清單]]");
		diffTxtHelper.writeText("=================================================================================");
		Connection currentConnection = new Connection(user, password, url, datasource);
		Baseline currentbaseline = new Baseline(projectName, baseline_M, currentConnection);
		Baseline otherbaseline = new Baseline(projectName, baseline_U, currentConnection);
		getDifference(currentbaseline.getBaseline(), otherbaseline.getBaseline(), currentConnection, ActionRule);

	}

	public static void getDecisionTable() throws Exception {
		diffTxtHelper.writeText("[[決策表差異清單]]");
		diffTxtHelper.writeText("=================================================================================");
		Connection currentConnection = new Connection(user, password, url, datasource);
		Baseline currentbaseline = new Baseline(projectName, baseline_M, currentConnection);
		Baseline otherbaseline = new Baseline(projectName, baseline_U, currentConnection);
		getDifference(currentbaseline.getBaseline(), otherbaseline.getBaseline(), currentConnection, DecisionTable);

	}

	public static void getDifference(IlrBaseline main, IlrBaseline uat, Connection connection, String inType)
			throws IlrApplicationException, IOException {

		try {
			List<MergeOperation> mergeOperations;
			doChecklist(inType, "", "", "", "");
			mergeOperations = BranchHelper.getMergeOperations(connection.getSession(), main, uat);

			List<MergeOperation> mergeOperationsToCommit = new ArrayList<MergeOperation>();
			IlrSession session = connection.getSession();
			for (MergeOperation mergeOperation : mergeOperations) {
				if (mergeOperation.isRightChange()) {// 取得右分支有異動之規則
					mergeOperationsToCommit.add(mergeOperation);
				}

				if (mergeOperation.isConflict()) {// 衝突
					Change change = mergeOperation.getRightChange();
					String status = change.getStatus().toString().trim();
					String type = change.getElementHandle().getType().toString().trim();
					session.setWorkingBaseline(main);
					IlrActionRule ilrActionRule_R = (IlrActionRule) session
							.getElementDetails(change.getElementHandle());
					IlrElementDetails element_R = ilrActionRule_R.getDefinition();

					String[] mainPaths = IlrSessionHelper.getPath(session, change.getElementHandle(), main);
					String[] uatPaths = IlrSessionHelper.getPath(session, change.getElementHandle(), uat);
					String ruleName = ilrActionRule_R.getName();
					String mainPath = getPath(mainPaths);
					String uatPath = getPath(uatPaths);

					diffTxtHelper.writeText("規則狀態 : " + status);
					diffTxtHelper.writeText("規則名稱 : " + ruleName);
					diffTxtHelper.writeText("規則路徑 : " + uatPath);
					checkPath(mainPath, uatPath);// 檢查規則路徑異動
					doChecklist("LIST", status, ruleName, mainPath, uatPath);
				}
			}

			for (MergeOperation mergeOperation : mergeOperationsToCommit) {

				Change change = mergeOperation.getRightChange();
				String status = change.getStatus().toString().trim();
				String type = change.getElementHandle().getType().toString().trim();

				// System.out.println("status:"+status);
				// System.out.println("type:"+type);

				if (type.equals(ActionRule) && type.equals(inType)) {// 處理ActionRule
					// 處理更新
					if (status.equals("Update")) {
						session.setWorkingBaseline(main);
						IlrActionRule ilrActionRule_R = (IlrActionRule) session
								.getElementDetails(change.getElementHandle());
						IlrElementDetails element_R = ilrActionRule_R.getDefinition();

						String[] mainPaths = IlrSessionHelper.getPath(session, change.getElementHandle(), main);
						String[] uatPaths = IlrSessionHelper.getPath(session, change.getElementHandle(), uat);
						String ruleName = ilrActionRule_R.getName();
						String mainPath = getPath(mainPaths);
						String uatPath = getPath(uatPaths);

						diffTxtHelper.writeText("規則狀態 : " + status);
						diffTxtHelper.writeText("規則名稱 : " + ruleName);
						diffTxtHelper.writeText("規則路徑 : " + uatPath);
						checkPath(mainPath, uatPath);// 檢查規則路徑異動
						doChecklist("LIST", status, ruleName, mainPath, uatPath);

						String R_Body = element_R.getRawValue(session.getBrmPackage().getDefinition_Body()).toString();
						List<String> R_BodyList = Arrays.asList(R_Body.split("\n"));
						for (int k = 0; k < R_BodyList.size(); k++)
							R_BodyList.set(k, R_BodyList.get(k).trim());

						if (status.equals("Update")) {

							session.setWorkingBaseline(uat);
							IlrActionRule ilrActionRule_L = (IlrActionRule) session
									.getElementDetails(change.getElementHandle());
							IlrElementDetails element_L = ilrActionRule_L.getDefinition();
							String L_Body = element_L.getRawValue(session.getBrmPackage().getDefinition_Body())
									.toString();
							List<String> L_BodyList = Arrays.asList(L_Body.split("\n"));
							for (int k = 0; k < L_BodyList.size(); k++)
								L_BodyList.set(k, L_BodyList.get(k).trim());

							ToolHelper.getDiff(R_BodyList, L_BodyList, ActionRuleTitle);
						}
						diffTxtHelper.writeText(
								"+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
						diffTxtHelper.writeText("\r");
					}

					// 處理新增
					if (status.equals("Add")) {
						session.setWorkingBaseline(uat);
						IlrActionRule ilrActionRule_R = (IlrActionRule) session
								.getElementDetails(change.getElementHandle());
						IlrElementDetails element_R = ilrActionRule_R.getDefinition();
						String R_Body = element_R.getRawValue(session.getBrmPackage().getDefinition_Body()).toString();

						String[] uatPaths = IlrSessionHelper.getPath(session, change.getElementHandle(), uat);

						String ruleName = ilrActionRule_R.getName();
						String uatPath = getPath(uatPaths);

						diffTxtHelper.writeText("規則狀態 : " + status);
						diffTxtHelper.writeText("規則名稱 : " + ruleName);
						diffTxtHelper.writeText("規則路徑 : " + uatPath);
						diffTxtHelper.writeText("新增規則內容 : ");
						diffTxtHelper.writeText(R_Body);

						diffTxtHelper.writeText(
								"+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
						diffTxtHelper.writeText("\r");

						doChecklist("LIST", status, ruleName, "", uatPath);
					}

					// 處理刪除
					if (status.equals("Delete")) {
						session.setWorkingBaseline(main);
						IlrActionRule ilrDecisionTable_L = (IlrActionRule) session
								.getElementDetails(change.getElementHandle());
						IlrElementDetails element_L = ilrDecisionTable_L.getDefinition();
						String L_Body = element_L.getRawValue(session.getBrmPackage().getDefinition_Body()).toString();

						String[] mainPaths = IlrSessionHelper.getPath(session, change.getElementHandle(), main);
						String ruleName = ilrDecisionTable_L.getName();
						String mainPath = getPath(mainPaths);

						diffTxtHelper.writeText("規則狀態 : " + status);
						diffTxtHelper.writeText("規則名稱 : " + ruleName);
						diffTxtHelper.writeText("規則路徑 : " + mainPath);
						diffTxtHelper.writeText("刪除規則內容 : ");
						diffTxtHelper.writeText(L_Body);

						diffTxtHelper.writeText(
								"+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
						diffTxtHelper.writeText("\r");

						doChecklist("LIST", status, ruleName, mainPath, "");
					}
				}

				if (type.equals(DecisionTable) && type.equals(inType)) {// 處理DecisionTable
					// 處理更新
					if (status.equals("Update")) {
						session.setWorkingBaseline(main);
						IlrDecisionTable ilrDecisionTable_R = (IlrDecisionTable) session
								.getElementDetails(change.getElementHandle());
						IlrElementDetails element_R = ilrDecisionTable_R.getDefinition();
						String[] mainPaths = IlrSessionHelper.getPath(session, change.getElementHandle(), main);
						String[] uatPaths = IlrSessionHelper.getPath(session, change.getElementHandle(), uat);
						String ruleName = ilrDecisionTable_R.getName();
						String mainPath = getPath(mainPaths);
						String uatPath = getPath(uatPaths);

						diffTxtHelper.writeText("規則狀態 : " + status);
						diffTxtHelper.writeText("規則名稱 : " + ruleName);
						diffTxtHelper.writeText("規則路徑 : " + uatPath);
						checkPath(mainPath, uatPath);// 檢查規則路徑異動
						doChecklist("LIST", status, ruleName, mainPath, uatPath);
						// System.out.println("uatPath:"+uatPath);
						String reXmlToJson = "";
						System.out.println(ruleName);

						reXmlToJson = element_R.getRawValue(session.getBrmPackage().getDefinition_Body()).toString();

						JSONObject product = null;
						try {
							product = XML.toJSONObject(replaceXmlToJson(reXmlToJson.trim()));// xml
							System.out.println("R: "+reXmlToJson);
							// System.out.println("product:"+product);
							List<String> R_PreconditionList = processPreconditions(product);
							List<String> R_BodyList = getAllDoc(product);
							if (status.equals("Update")) {
								session.setWorkingBaseline(uat);
								IlrDecisionTable ilrDecisionTable_L = (IlrDecisionTable) session
										.getElementDetails(change.getElementHandle());
								IlrElementDetails element_L = ilrDecisionTable_L.getDefinition();
								reXmlToJson = element_L.getRawValue(session.getBrmPackage().getDefinition_Body())
										.toString();
								System.out.println("L: "+reXmlToJson);

								product = XML.toJSONObject(replaceXmlToJson(reXmlToJson.trim()));

								List<String> L_PreconditionList = processPreconditions(product);
								List<String> L_BodyList = getAllDoc(product);

								ToolHelper.getDiff(R_PreconditionList, L_PreconditionList, DecisionTablePrecondTitle);
								ToolHelper.getDiff(R_BodyList, L_BodyList, DecisionTableTitle);
							}
						} catch (Exception e) {
							System.out.println(e);
							diffTxtHelper.writeText("規則[" + ruleName + "] 比對差異過程未完成，請另行檢查。");
							logger.error("規則[" + ruleName + "] 比對差異過程未完成，請另行檢查。");
							logger.error(e.getMessage(), e);
							ErrorMesage = "差異報表明細內容有誤！";
						}

						diffTxtHelper.writeText(
								"+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
						diffTxtHelper.writeText("\r");
					}

					// 處理新增
					if (status.equals("Add")) {
						session.setWorkingBaseline(uat);
						IlrDecisionTable ilrDecisionTable_R = (IlrDecisionTable) session
								.getElementDetails(change.getElementHandle());
						IlrElementDetails element_R = ilrDecisionTable_R.getDefinition();
						String[] uatPaths = IlrSessionHelper.getPath(session, change.getElementHandle(), uat);
						String ruleName = ilrDecisionTable_R.getName();
						String uatPath = getPath(uatPaths);
						String reXmlToJson = "";
						JSONObject product = null;
						reXmlToJson = element_R.getRawValue(session.getBrmPackage().getDefinition_Body()).toString();
						product = XML.toJSONObject(replaceXmlToJson(reXmlToJson.trim()));

						List<String> L_PreconditionList = processPreconditions(product);
						List<String> L_BodyList = getAllDoc(product);

						diffTxtHelper.writeText("規則狀態 : " + status);
						diffTxtHelper.writeText("規則名稱 : " + ruleName);
						diffTxtHelper.writeText("規則路徑 : " + uatPath);
						diffTxtHelper.writeText("新增規則內容 : ");
						for (String str : L_PreconditionList) {
							diffTxtHelper.writeText(str);
						}
						for (String str : L_BodyList) {
							diffTxtHelper.writeText(str);
						}
						diffTxtHelper.writeText(
								"+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
						diffTxtHelper.writeText("\r");

						doChecklist("LIST", status, ruleName, "", uatPath);
					}

					// 處理刪除
					if (status.equals("Delete")) {
						session.setWorkingBaseline(main);
						IlrDecisionTable ilrDecisionTable_L = (IlrDecisionTable) session
								.getElementDetails(change.getElementHandle());
						IlrElementDetails element_L = ilrDecisionTable_L.getDefinition();
						String[] mainPaths = IlrSessionHelper.getPath(session, change.getElementHandle(), main);
						String ruleName = ilrDecisionTable_L.getName();
						String mainPath = getPath(mainPaths);
						String reXmlToJson = "";
						JSONObject product = null;
						reXmlToJson = element_L.getRawValue(session.getBrmPackage().getDefinition_Body()).toString();
						product = XML.toJSONObject(replaceXmlToJson(reXmlToJson.trim()));

						List<String> L_PreconditionList = processPreconditions(product);
						List<String> L_BodyList = getAllDoc(product);

						diffTxtHelper.writeText("規則狀態 : " + status);
						diffTxtHelper.writeText("規則名稱 : " + ruleName);
						diffTxtHelper.writeText("規則路徑 : " + mainPath);
						diffTxtHelper.writeText("刪除規則內容 : ");
						for (String str : L_PreconditionList) {
							diffTxtHelper.writeText(str);
						}
						for (String str : L_BodyList) {
							diffTxtHelper.writeText(str);
						}

						diffTxtHelper.writeText(
								"+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
						diffTxtHelper.writeText("\r");

						doChecklist("LIST", status, ruleName, mainPath, "");
					}
				}
			}
			doChecklist("END", "", "", "", "");
		} catch (IlrObjectNotFoundException e) {
			//Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		} catch (IlrPermissionException e) {
			//Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		} catch (IlrConnectException e) {
			//Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		} catch (Exception e) {
			//Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}

	// 取得規則路徑
	public static String getPath(String[] paths) {
		String rulePath = "";
		if (paths == null)
			return "";
		for (String path : paths) {
			rulePath = rulePath + path + "/";
		}
		return rulePath.substring(0, rulePath.length() - 1);
	}

	// 取得決策表所有內容
	public static List<String> getAllDoc(JSONObject product) {
		TreeMap<String, ArrayList<String>> ruleMap = new TreeMap<String, ArrayList<String>>();
		TreeMap<String, ArrayList<String>> actMap = new TreeMap<String, ArrayList<String>>();
		processTitle(product, ruleMap, actMap);// 取得決策表欄位

		processRule(product, ruleMap, actMap);// 取得決策表內容
		List<String> bodyList = new ArrayList<String>();

		for (String groupKey : ruleDefIdList) {
			// for(String groupKey : ruleMap.keySet()){
			ArrayList<String> tempBodyList = ruleMap.get(groupKey);
			if (bodyList.size() == 0) {
				for (String str : tempBodyList) {
					bodyList.add("|" + str + "|");
				}
			} else {
				for (int k = 0; k < bodyList.size(); k++) {
					if (k > tempBodyList.size())
						bodyList.set(k, bodyList.get(k) + "DATA ERROR" + "|");
					else
						bodyList.set(k, bodyList.get(k) + tempBodyList.get(k) + "|");
				}
			}
		}

		for (String groupKey : actMap.keySet()) {
			ArrayList<String> tempBodyList = actMap.get(groupKey);
			if (bodyList.size() != 0 && tempBodyList.size() != 0) {

				for (int k = 0; k < bodyList.size(); k++) {
					if (k > tempBodyList.size())
						bodyList.set(k, bodyList.get(k) + "DATA ERROR" + "|");
					else
						bodyList.set(k, bodyList.get(k) + tempBodyList.get(k) + "|");
				}
			}
		}

		return bodyList;
	}

	// 處理決策表前置條件
	public static List<String> processPreconditions(JSONObject product) {

		JSONObject body = product.getJSONObject("DT").getJSONObject("Body");
		List<String> bodyList = new ArrayList<String>();

		if (body.has("Preconditions")) {
			String text = body.getJSONObject("Preconditions").optString("Text");
			text = text.replaceAll("#13;", "");// 去除前置條件多餘文字
			bodyList = Arrays.asList(text.replaceAll("\r", "").split("\n"));
		} else {
			bodyList.add("");
		}
		return bodyList;
	}

	// 處理決策表欄位
	public static void processTitle(JSONObject product, TreeMap<String, ArrayList<String>> ruleMap,
			TreeMap<String, ArrayList<String>> actMap) {

		JSONObject structure = product.getJSONObject("DT").getJSONObject("Body").getJSONObject("Structure");

		JSONObject conditionDefinitions = structure.getJSONObject("ConditionDefinitions");
		JSONObject actionDefinitions = structure.getJSONObject("ActionDefinitions");

		// 條件
		Object conditionDef = new JSONTokener(conditionDefinitions.get("ConditionDefinition").toString()).nextValue();
		getTitleDef(conditionDef, ruleMap, true);
		// 動作
		Object actionDef = new JSONTokener(actionDefinitions.get("ActionDefinition").toString()).nextValue();
		getTitleDef(actionDef, actMap, false);
	}

	// 處理決策表規則
	public static void processRule(JSONObject product, TreeMap<String, ArrayList<String>> ruleMap,
			TreeMap<String, ArrayList<String>> actMap) {
		JSONObject partition = product.getJSONObject("DT").getJSONObject("Body").getJSONObject("Contents")
				.getJSONObject("Partition");
		ruleDefIdList = ruleMap.get("defIdList");
		JsonParse.processRule(partition, ruleMap);
		JsonParse.processAct(partition, actMap);
	}

	// 取得欄位內容
	public static void getTitleDef(Object jsonObj, TreeMap<String, ArrayList<String>> ruleMap, boolean needDefId) {
		String defId = "";
		String text = "";
		ArrayList<String> defIdList = new ArrayList<String>();
		if (jsonObj instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) jsonObj;
			for (int k = 0; k < jsonArray.length(); k++) {
				JSONObject parameterObject = jsonArray.getJSONObject(k);
				defId = parameterObject.optString("Id");
				text = parameterObject.getJSONObject("ExpressionDefinition").optString("Text");
				defIdList.add(defId);
				if (!ruleMap.containsKey(defId)) {
					ArrayList<String> tempList = new ArrayList<String>();
					tempList.add(text);
					ruleMap.put(defId, tempList);
				} else {
					ArrayList<String> tempList = ruleMap.get(defId);
					tempList.add(text);
					ruleMap.put(defId, tempList);
				}
			}
			if (needDefId)
				ruleMap.put("defIdList", defIdList);
		} else if (jsonObj instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject) jsonObj;
			defId = jsonObject.optString("Id");
			text = jsonObject.getJSONObject("ExpressionDefinition").optString("Text");
			ArrayList<String> tempList = new ArrayList<String>();
			tempList.add(text);
			ruleMap.put(defId, tempList);

			defIdList.add(defId);
			if (needDefId)
				ruleMap.put("defIdList", defIdList);
		}
	}

	// 檢查規則路徑
	public static void checkPath(String mainPath, String uatPath) {
		if (!mainPath.equals(uatPath)) {
			diffTxtHelper.writeText(RulePath);
			diffTxtHelper.writeText("異動前路徑 : " + mainPath);
			diffTxtHelper.writeText("異動後路徑 : " + uatPath);
		}
	}

	// US
	// public static String replaceXmlToJson(String xml) {
	// xml = xml.replaceAll("\\<\\!\\[CDATA\\[", "");
	// xml = xml.replaceAll("\\]\\]\\>", "");
	// xml = xml.replaceAll("<a string>", "[a string]");
	// xml = xml.replaceAll("<binding type>", "[binding type]");
	// xml = xml.replaceAll("<binding>", "[binding]");
	// xml = xml.replaceAll(" ", " ");
	// xml = xml.replaceAll("<>", "");
	// xml = xml.replaceAll("&", "");
	// return xml;
	// }

	// TW
	public static String replaceXmlToJson(String xml) {
		xml = xml.replaceAll("\\<\\!\\[CDATA\\[", "");
		xml = xml.replaceAll("\\]\\]\\>", "");
		xml = xml.replaceAll("<一個 簡單日期>", "[一個 簡單日期]");
		xml = xml.replaceAll("<一個 日期>", "[一個 日期]");
		xml = xml.replaceAll("<一些 字串>", "[一些 字串]");
		xml = xml.replaceAll("<一些 數字>", "[一些 數字]");
		xml = xml.replaceAll("<一些 物件>", "[一些 物件]");
		xml = xml.replaceAll("<一個 字串>", "[一個 字串]");
		xml = xml.replaceAll("<一個 數字>", "[一個 數字]");
		xml = xml.replaceAll("<一個 物件>", "[一個 物件]");
		xml = xml.replaceAll("<照會代碼>", "[照會代碼]");
		xml = xml.replaceAll("<照會說明>", "[照會說明]");
		xml = xml.replaceAll("<一個 布林類型>", "[一個 布林類型]");
		xml = xml.replaceAll("<數字>", "[數字]");
		xml = xml.replaceAll("<日期>", "[日期]");
		xml = xml.replaceAll("<布林值>", "[布林值]");
		xml = xml.replaceAll("<字串一>", "[字串一]");
		xml = xml.replaceAll("<字串二>", "[字串二]");
		xml = xml.replaceAll("<被除數>", "[被除數]");
		xml = xml.replaceAll("<除數>", "[除數]");
		xml = xml.replaceAll("<出生年月日>", "[出生年月日]");
		xml = xml.replaceAll("<處理日期>", "[處理日期]");
		xml = xml.replaceAll("<基金檔次>", "[基金檔次]");
		xml = xml.replaceAll("<新公會明細>", "[新公會明細]");
		xml = xml.replaceAll("<身高>", "[身高]");
		xml = xml.replaceAll("<體重>", "[體重]");
		xml = xml.replaceAll("<姓名>", "[姓名]");
		xml = xml.replaceAll("<被保人的ID>", "[被保人的ID]");
		xml = xml.replaceAll("<天數>", "[天數]");
		xml = xml.replaceAll("<月數>", "[月數]");
		xml = xml.replaceAll("<>", "");
		xml = xml.replaceAll("&", "");
		xml = xml.replaceAll("  ", " ");
		xml = xml.replaceAll(">=", "大於等於");
		xml = xml.replaceAll("<=", "小於等於");
		// xml = xml.replaceAll("<Expression/>",
		// "<Expression><Parm></Parm></Expression>");
		xml = replaceLessXmlToJson(xml, word);
		return xml;
	}

	public static String replaceLessXmlToJson(String xml, String[] chars) {

		for (int i = 0; i < chars.length; i++) {
			if (chars[i].equals("?")) {
				xml = xml.replaceAll("<\\?", ">>\\?");
			} else {
				xml = xml.replaceAll("<" + chars[i], ">>" + chars[i]);
			}
		}
		xml = xml.replaceAll("<", "小於");
		for (int i = 0; i < chars.length; i++) {
			if (chars[i].equals("?")) {
				xml = xml.replaceAll(">>\\?", "<\\?");
			} else {
				xml = xml.replaceAll(">>" + chars[i], "<" + chars[i]);
			}
		}

		return xml;
	}

	public static String getDate() {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String str = format.format(new Date());
		return str;
	}

	// 寫入差異清單
	public static void doChecklist(String type, String status, String ruleName, String mainPath, String uatPath) {

		if (type.equals(ActionRule)) {
			listTxtHelper.writeText("[動作規則異動清單]");
			listTxtHelper
					.writeText("=================================================================================");
			listTxtHelper.writeText(StringUtils.rightPad("規則狀態", 8) + StringUtils.rightPad("規則名稱", 36)
					+ StringUtils.rightPad("規則原路徑", 75) + "規則新路徑");
		}

		if (type.equals(DecisionTable)) {
			listTxtHelper.writeText("[決策表異動清單]");
			listTxtHelper
					.writeText("=================================================================================");
			listTxtHelper.writeText(StringUtils.rightPad("規則狀態", 8) + StringUtils.rightPad("規則名稱", 36)
					+ StringUtils.rightPad("規則原路徑", 75) + "規則新路徑");
		}

		if (type.equals("LIST")) {
			int statusNum = 4;
			int ruleNameNum = 10;
			int mainPathNum = 20;
			try {
				statusNum = 12 - status.getBytes("gbk").length;
				ruleNameNum = 40 - ruleName.getBytes("gbk").length;
				mainPathNum = 80 - mainPath.getBytes("gbk").length;

			} catch (UnsupportedEncodingException e1) {
				System.out.println(e1.getMessage());
			}

			listTxtHelper.writeText(status + String.format("%" + statusNum + "s", "") + ruleName
					+ String.format("%" + ruleNameNum + "s", "") + mainPath + String.format("%" + mainPathNum + "s", "")
					+ uatPath);

		}

		if (type.equals("END")) {
			listTxtHelper
					.writeText("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
		}

	}

}
