package baselinesdiff.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONArray;
//import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JsonParse {
	public static int DecisionTableDeep = 0;
	public static int DecisionTableRow = 0;
	public static String fristKey = "C0";

	/**
	 * do 決策表規則條件內容
	 * 
	 * @param ruleMap 規則暫存Map
	 */
	public static void processRule(JSONObject partition, TreeMap<String, ArrayList<String>> ruleMap) {

		List<JSONObject> jsonList = new ArrayList<JSONObject>();

		DecisionTableDeep = 0;
		boolean isFrist = true;
		ArrayList<String> defIdList = ruleMap.get("defIdList");
//	    System.out.println("processRule partition:"+partition);
//	    System.out.println("processRule ruleMap:"+ruleMap);
		for (String defId : defIdList) {

			DecisionTableRow = 0;
			if (isFrist) {
				paraseFrist(partition, jsonList);
				isFrist = false;
//			  System.out.println("processRule isFrist jsonList:"+jsonList);  
			} else {
				parase(partition, jsonList, defId, 0);
//			  System.out.println("processRule notFrist jsonList:"+jsonList);  
			}
			DecisionTableDeep++;
		}

		ruleMap.remove("defIdList");

		for (Entry<String, ArrayList<String>> entry : ruleMap.entrySet()) {
			ArrayList<String> value = entry.getValue();
			String key = entry.getKey();
			for (JSONObject json : jsonList) {
				JSONObject temp = json.getJSONObject(key);
				value.add(getValue(temp));
			}
			ruleMap.put(key, value);
		}

	}

	/**
	 * do 決策表規則動作
	 * 
	 * @param actMap 動作暫存Map
	 */
	public static void processAct(JSONObject partition, TreeMap<String, ArrayList<String>> actMap) {

		Object jsonObj = new JSONTokener(partition.get("Condition").toString()).nextValue();
		if (jsonObj instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) jsonObj;
			for (int k = 0; k < jsonArray.length(); k++) {
				JSONObject jsonObject = jsonArray.getJSONObject(k);

				if (jsonObject.has("ActionSet")) {
					JSONObject actionSetObj = jsonObject.getJSONObject("ActionSet");
					getRuleAct(actionSetObj, actMap);
				} else {
					processAct(jsonObject.optJSONObject("Partition"), actMap);
				}
			}
		} else if (jsonObj instanceof JSONObject) {

			JSONObject jsonObject = (JSONObject) jsonObj;
			if (jsonObject.has("ActionSet")) {
				JSONObject actionSetObj = jsonObject.getJSONObject("ActionSet");
				getRuleAct(actionSetObj, actMap);
			} else {
				processAct(jsonObject.optJSONObject("Partition"), actMap);
			}
		}
	}

	/**
	 * get 取得決策表動作內容
	 * 
	 * @param actMap 暫存Map
	 */
	public static void getRuleAct(JSONObject actionSetObj, TreeMap<String, ArrayList<String>> actMap) {
		String defId = "";
		String text = "  ";
		Object jsonObj = new JSONTokener(actionSetObj.get("Action").toString()).nextValue();
		if (jsonObj instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) jsonObj;
			for (int k = 0; k < jsonArray.length(); k++) {
				JSONObject parameterObject = jsonArray.getJSONObject(k);
				defId = parameterObject.optString("DefId");
				text = parseExpression(parameterObject);

				if (actMap.containsKey(defId)) {
					ArrayList<String> tempList = actMap.get(defId);
					tempList.add(text);
					actMap.put(defId, tempList);
				}
			}
		} else if (jsonObj instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject) jsonObj;
			defId = jsonObject.optString("DefId");
			text = parseExpression(jsonObject);
			if (actMap.containsKey(defId)) {
				ArrayList<String> tempList = actMap.get(defId);
				tempList.add(text);
				actMap.put(defId, tempList);
			}
		}
	}

	/**
	 * do 處理決策表標題內容
	 */
	public static void paraseFrist(JSONObject partition, List<JSONObject> temp) {

		if (temp.size() == 0) {
			String defId = partition.getString("DefId");
			Object jsonObj = new JSONTokener(partition.get("Condition").toString()).nextValue();
			if (jsonObj instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray) jsonObj;
				for (int k = 0; k < jsonArray.length(); k++) {
					JSONObject parameterObject = jsonArray.getJSONObject(k);
					JSONObject tempJson = new JSONObject();
					if (parameterObject.has("Expression")) {
						try {
							tempJson.put(defId, parameterObject.getJSONObject("Expression"));
						} catch (Exception e) {
							System.out.println("取得Expression錯誤 : " + e.getMessage());
							tempJson.put(defId,
									getPartition(defId).getJSONObject("Condition").getJSONObject("Expression"));
						}
					}
					temp.add(tempJson);
				}
			} else if (jsonObj instanceof JSONObject) {
				JSONObject jsonObject = (JSONObject) jsonObj;
				JSONObject tempJson = new JSONObject();
				tempJson.put(defId, jsonObject.getJSONObject("Expression"));
				temp.add(tempJson);
			}
		}
	}

	public static void parase(JSONObject partition, List<JSONObject> temp, String defId, int deep) {
		deep++;
		Object jsonObj = new JSONTokener(partition.get("Condition").toString()).nextValue();
		if (jsonObj instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) jsonObj;
			for (int k = 0; k < jsonArray.length(); k++) {
				JSONObject jsonObject = jsonArray.getJSONObject(k);

				if (jsonObject.has("Partition")
						&& jsonObject.getJSONObject("Partition").getString("DefId").equals(defId)) {
					JSONObject tempPartition = jsonObject.getJSONObject("Partition");
					getArrayParam(tempPartition, temp, defId, deep);
				} else if (jsonObject.has("Partition") && !(DecisionTableDeep == deep)) {
					parase(jsonObject.getJSONObject("Partition"), temp, defId, deep);
				} else {
					JSONObject tempPartition = getPartition(defId);
					getArrayParam(tempPartition, temp, defId, deep);
				}
			}
		} else if (jsonObj instanceof JSONObject) {

			JSONObject jsonObject = (JSONObject) jsonObj;
			if (jsonObject.has("Partition") && jsonObject.getJSONObject("Partition").getString("DefId").equals(defId)) {
				JSONObject tempPartition = jsonObject.getJSONObject("Partition");
				getArrayParam(tempPartition, temp, defId, deep);
			} else if (jsonObject.has("Partition") && !(DecisionTableDeep == deep)) {
				parase(jsonObject.getJSONObject("Partition"), temp, defId, deep);
			} else {
				JSONObject tempPartition = getPartition(defId);
				getArrayParam(tempPartition, temp, defId, deep);
			}
		}
	}

	/**
	 * @param deep 欄位深度
	 * @param temp 暫存JSONObject列表
	 */
	private static void getArrayParam(JSONObject partition, List<JSONObject> temp, String defId, int deep) {
		Object jsonObj = new JSONTokener(partition.get("Condition").toString()).nextValue();
		//System.out.println("jsonObj:" + jsonObj);
		if (jsonObj instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) jsonObj;
			for (int k = 0; k < jsonArray.length(); k++) {
				JSONObject parameterObject = jsonArray.getJSONObject(k);
				if(DecisionTableRow>=temp.size()) {DecisionTableRow=temp.size()-1;}
				JSONObject intertObject = new JSONObject(temp.get(DecisionTableRow).toString());
				if (!(k == jsonArray.length() - 1))
					intert(temp, intertObject, DecisionTableRow + 1);
				JSONObject expression = null;
				try {
					expression = parameterObject.getJSONObject("Expression");
					// System.out.println("expression:"+expression);

				} catch (Exception e) {
					System.out.println("取得Expression錯誤 : " + e.getMessage());
					expression = getPartition(defId).getJSONObject("Condition").getJSONObject("Expression");
				}
				// System.out.println("temp:"+temp);
				temp.get(DecisionTableRow).put(defId, expression);
				DecisionTableRow++;
			}
		} else if (jsonObj instanceof JSONObject) {

			JSONObject jsonObject = (JSONObject) jsonObj;
			JSONObject expression = null;
			try {
				expression = jsonObject.getJSONObject("Expression");
				// System.out.println("expression(object):"+expression);

			} catch (Exception e) {
				System.out.println("取得Expression錯誤 : " + e.getMessage());
				expression = getPartition(defId).getJSONObject("Condition").getJSONObject("Expression");
			}
			// System.out.println("temp(object):"+temp);
			if(DecisionTableRow>=temp.size()) {DecisionTableRow=temp.size()-1;}

			temp.get(DecisionTableRow).put(defId, expression);
			DecisionTableRow++;
		}
	}

	/**
	 * @param defId defId
	 * @return String 回傳決策表規則條件為空白的內容
	 */
	private static JSONObject getPartition(String defId) {
		JSONObject partition = new JSONObject();
		JSONObject expression = new JSONObject();
		JSONObject condition = new JSONObject();

		expression.put("Param", "  ");
		condition.put("Expression", expression);
		partition.put("DefId", defId);
		partition.put("Condition", condition);

		return partition;
	}

	/**
	 * @param insertJson 新增的JSONObject
	 * @param row        第幾筆之後的位置
	 */
	public static void intert(List<JSONObject> jsons, JSONObject insertJson, int row) {
		List<JSONObject> tempObj = new ArrayList<JSONObject>();
		int count = 1;
		for (JSONObject json : jsons) {
			if (count == row)
				tempObj.add(insertJson);
			tempObj.add(json);
			count++;
		}
		jsons.clear();
		jsons.addAll(tempObj);

	}

	/**
	 * @return String 取得決策表規則條件內容
	 */
	public static String getValue(JSONObject json) {
		String value = json.optString("Param");
		String operator = json.optString("Text");
		String returnStr = replaceOperator(operator + value);

		return returnStr.equals("") ? "  " : returnStr;
	}

	/**
	 * @return String 取代運算子
	 */
	public static String replaceOperator(String operator) {
		operator = operator.replaceAll("\\[一個 物件\\]是\\[一些 物件\\]中的一個", "in");
		operator = operator.replaceAll("\\[一個 物件\\]是\\[一個 物件\\]", "等於");
		operator = operator.replaceAll("\\[一個 物件\\]不是\\[一個 物件\\]", "不等於");
		operator = operator.replaceAll("\\[一個 物件\\]不是\\[一些 物件\\]中的一個", "不在");
		return operator;
	}

	/**
	 * @return String 取得決策表執行動作內容
	 */
	public static String parseExpression(JSONObject condition) {
		String paramStr = " ";

		JSONObject expression = condition.optJSONObject("Expression");
		if (expression == null) {
			return paramStr;
		}

		Object jsonObj = new JSONTokener(expression.get("Param").toString()).nextValue();
		paramStr = getParam(jsonObj);

		return paramStr;
	}

	/**
	 * @return String 取得決策表Param
	 */
	public static String getParam(Object jsonObj) {
		String paramStr = "";
		if (jsonObj instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) jsonObj;
			for (int k = 0; k < jsonArray.length(); k++) {
				paramStr = paramStr + jsonArray.get(k);
				if (jsonArray.length() - 1 > k) {
					paramStr = paramStr + ",";
				}
			}
		} else if (jsonObj instanceof String) {
			paramStr = (String) jsonObj;
		} else if (jsonObj instanceof Double) {
			paramStr = jsonObj.toString();
		} else if (jsonObj instanceof Integer) {
			paramStr = jsonObj.toString();
		}
		return paramStr;
	}
}
