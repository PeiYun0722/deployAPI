package Function;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import baselinesdiff.DifferenceBaseline;

public class ApiCaller {

	public static String user_DC;
	public static String password_DC;
	public static String user_RES;
	public static String password_RES;
	public static String ip_DC;
	public static String ip_resUAT;
	public static String ip_resProd;
	public static String filePath;
	public static Logger logger = Logger.getLogger(ApiCaller.class);

	public static void init() {
		Properties prop = new Properties();
		InputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream(
					new File(DifferenceBaseline.class.getResource("/properties/odmAPI.properties").getFile())));
			prop.load(in);
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

		user_DC = prop.getProperty("odmDC.user");
		password_DC = prop.getProperty("odmDC.password");
		user_RES = prop.getProperty("odmRES.user");
		password_RES = prop.getProperty("odmRES.password");
		ip_DC = prop.getProperty("odmDC.ip");
		ip_resUAT = prop.getProperty("resUAT.ip");
		ip_resProd = prop.getProperty("resProd.ip");
		filePath = prop.getProperty("out.filePath");

	}

	public static String getRuleApp(String name, String srNo) throws ClientProtocolException, IOException {
		logger.info("呼叫取得RuleApp保存檔API...");
		init();
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user_DC, password_DC);
		provider.setCredentials(AuthScope.ANY, credentials);
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
		String dsId = getDecisioServiceId(client, name);
		logger.info("取得決策服務ID成功！");
		String dpId = getDeploymentId(client, dsId);
		logger.info("取得部屬ID成功！");
		HttpGet httpGet = new HttpGet("http://" + ip_DC + ":9081/decisioncenter-api/v1/deployments/" + dpId
				+ "/download?includeXOMInArchive=true");
		httpGet.setHeader("Accept", "application/octet-stream");
		HttpResponse httpResponse = client.execute(httpGet);
		HttpEntity httpEntity = httpResponse.getEntity();
		File file = new File("D:\\OdmTemp\\" + srNo + "\\" + name + "App.jar");
		FileOutputStream fos = new FileOutputStream(file);
		httpEntity.writeTo(fos);
		fos.flush();
		fos.close();
		Model.Response response = new Model.Response("000", "取得差異清單、差異報表和RuleApp保存檔成功！");
		logger.info("取得差異清單、差異報表和RuleApp保存檔成功！");
		ObjectMapper objectMapper = new ObjectMapper();
		String resultJSON = objectMapper.writeValueAsString(response);
		return resultJSON;
	}

	public static String getDecisioServiceId(CloseableHttpClient client, String name)
			throws ClientProtocolException, IOException {
		logger.info("取得決策服務ID...");
		HttpGet httpGet = new HttpGet("http://" + ip_DC + ":9081/decisioncenter-api/v1/decisionservices");
		httpGet.setHeader("Accept", "application/json");
		HttpResponse httpResponse = client.execute(httpGet);
		HttpEntity httpEntity = httpResponse.getEntity();
		String httpEntityStr = EntityUtils.toString(httpEntity, "UTF-8");
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(httpEntityStr);
		JsonNode elementNode = rootNode.path("elements");
		Iterator<JsonNode> iterator = elementNode.elements();
		String id = "";
		while (iterator.hasNext()) {
			JsonNode element = iterator.next();
			String projectName = element.path("name").asText();
			if (projectName.equals(name)) {
				id = element.path("id").asText();
				break;
			}
		}
		return id;
	}

	public static String getDeploymentId(CloseableHttpClient client, String id)
			throws ClientProtocolException, IOException {
		logger.info("取得部屬ID...");
		HttpGet httpGet = new HttpGet(
				"http://" + ip_DC + ":9081/decisioncenter-api/v1/decisionservices/" + id + "/deployments");
		httpGet.setHeader("Accept", "application/json");
		HttpResponse httpResponse = client.execute(httpGet);
		HttpEntity httpEntity = httpResponse.getEntity();
		String httpEntityStr = EntityUtils.toString(httpEntity, "UTF-8");
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(httpEntityStr);
		String deploymentId = rootNode.path("elements").get(0).path("id").asText();
		return deploymentId;
	}

	public static String deployRuleApp(String OUTPUT_FILE_PATH, String goal)
			throws ClientProtocolException, IOException {
		logger.info("呼叫部屬RuleApp保存檔API...");
		init();
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user_RES, password_RES);
		provider.setCredentials(AuthScope.ANY, credentials);
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
		File deployFile = null;
		File outputDirFile = new File(OUTPUT_FILE_PATH);
		File[] outputFiles = outputDirFile.listFiles();
		for (File f : outputFiles) {
			String fileName = f.getName();
			String fileType = fileName.substring(fileName.lastIndexOf("."), fileName.length());
			if (fileType.equals(".jar")) {
				deployFile = f;
				break;
			}
		}
		HttpPost httpPost;
		if (goal.equals("UAT")) {
			// localhost can change to UAT IP
			httpPost = new HttpPost("http://" + ip_resUAT + ":9080/res/api/v1/ruleapps");
		} else {
			// localhost can change to Prod IP
			httpPost = new HttpPost("http://" + ip_resProd + ":9080/res/api/v1/ruleapps");
		}
		httpPost.setHeader("Content-type", "application/octet-stream");
		httpPost.setHeader("Accept", "application/json");
		httpPost.setEntity(new FileEntity(deployFile));
		HttpResponse httpResponse = client.execute(httpPost);
		HttpEntity httpEntity = httpResponse.getEntity();
		String httpEntityStr = EntityUtils.toString(httpEntity, "UTF-8");
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(httpEntityStr);
		String resultJSON = "";
		if (rootNode.path("succeeded").asText().equals("true")) {
			if (goal.equals("Prod")) {
				Model.Response response = new Model.Response("000", "部屬RuleApp保存檔至Prod成功！");
				logger.info("部屬RuleApp保存檔至Prod成功！");
				resultJSON = objectMapper.writeValueAsString(response);
				for (File f : outputFiles) {
					f.delete();
				}
				outputDirFile.delete();
				logger.info("檔案已刪除！");
				System.out.println("檔案已刪除！");
			} else {
				Model.Response response = new Model.Response("000", "部屬RuleApp保存檔至UAT成功！");
				logger.info("部屬RuleApp保存檔至UAT成功！");
				resultJSON = objectMapper.writeValueAsString(response);
			}
		} else {
			if (goal.equals("Prod")) {
				Model.Response response = new Model.Response("005", "部屬RuleApp保存檔至Prod失敗！");
				logger.error("部屬RuleApp保存檔至Prod失敗！");
				logger.error(httpEntityStr);
				resultJSON = objectMapper.writeValueAsString(response);
			} else {
				Model.Response response = new Model.Response("005", "部屬RuleApp保存檔至UAT失敗！");
				logger.error("部屬RuleApp保存檔至UAT失敗！");
				logger.error(httpEntityStr);
				resultJSON = objectMapper.writeValueAsString(response);
			}
		}
		return resultJSON;
	}

	public static String deleteRuleApp_Prod(String projectName) throws ClientProtocolException, IOException {
		logger.info("呼叫退版Prod API...");
		init();
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user_RES, password_RES);
		provider.setCredentials(AuthScope.ANY, credentials);
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
		String ruleAppName = projectName+"App";
		String ruleAppId = getRuleAppId(client, ruleAppName, "Prod");
		String ruleSetId = getRuleSetId(client, ruleAppId, "Prod");
		// localhost can change to Prod IP
		HttpDelete httpDelete = new HttpDelete("http://" + ip_resProd + ":9080/res/api/v1/ruleapps/" + ruleSetId);
		httpDelete.setHeader("Accept", "application/json");
		HttpResponse httpResponse = client.execute(httpDelete);
		HttpEntity httpEntity = httpResponse.getEntity();
		String httpEntityStr = EntityUtils.toString(httpEntity, "UTF-8");
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(httpEntityStr);
		String resultJSON = "";
		if (rootNode.path("succeeded").asText().equals("true")) {
			Model.Response response = new Model.Response("000", "退版Prod成功！");
			logger.info("退版Prod成功！");
			resultJSON = objectMapper.writeValueAsString(response);
		} else {
			Model.Response response = new Model.Response("006", "退版Prod失敗！");
			logger.error("退版Prod失敗！");
			logger.error(httpEntityStr);
			resultJSON = objectMapper.writeValueAsString(response);
		}
		return resultJSON;
	}

	public static String deleteRuleApp_UAT(String projectName, String OUTPUT_FILE_PATH)
			throws ClientProtocolException, IOException {
		logger.info("呼叫退版UAT API...");
		init();
		File outputDirFile = new File(OUTPUT_FILE_PATH);
		ObjectMapper objectMapper = new ObjectMapper();
		String resultJSON = "";
		if(!outputDirFile.exists()) {
			Model.Response response = new Model.Response("002", "參數錯誤，請檢查檔案路徑："+OUTPUT_FILE_PATH+"是否存在！");
			logger.error("參數錯誤，請檢查檔案路徑："+OUTPUT_FILE_PATH+"是否存在！");
			resultJSON = objectMapper.writeValueAsString(response);
			return resultJSON;
		}
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user_RES, password_RES);
		provider.setCredentials(AuthScope.ANY, credentials);
		CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
		String ruleAppName = projectName+"App";
		String ruleAppId = getRuleAppId(client, ruleAppName, "UAT");
		logger.info("取得Rule App ID成功！");
		String ruleSetId = getRuleSetId(client, ruleAppId, "UAT");
		logger.info("取得Rule Set ID成功！");
		// localhost can change to UAT IP
		HttpDelete httpDelete = new HttpDelete("http://" + ip_resUAT + ":9080/res/api/v1/ruleapps/" + ruleSetId);
		httpDelete.setHeader("Accept", "application/json");
		HttpResponse httpResponse = client.execute(httpDelete);
		HttpEntity httpEntity = httpResponse.getEntity();
		String httpEntityStr = EntityUtils.toString(httpEntity, "UTF-8");
		JsonNode rootNode = objectMapper.readTree(httpEntityStr);
		File[] outputFiles = outputDirFile.listFiles();
		if (rootNode.path("succeeded").asText().equals("true")) {
			Model.Response response = new Model.Response("000", "退版UAT成功！");
			logger.info("退版UAT成功！");
			resultJSON = objectMapper.writeValueAsString(response);
			for (File f : outputFiles) {
				f.delete();
			}
			outputDirFile.delete();
			logger.info("檔案已刪除！");
		} else {
			Model.Response response = new Model.Response("006", "退版UAT失敗！");
			logger.error("退版UAT失敗！");
			logger.error(httpEntityStr);
			resultJSON = objectMapper.writeValueAsString(response);
		}
		return resultJSON;
	}

	public static String getRuleAppId(CloseableHttpClient client, String ruleAppName, String goal)
			throws ClientProtocolException, IOException {
		logger.info("取得RuleApp ID...");
		HttpGet httpGet = null;
		if (goal.equals("UAT")) {
			httpGet = new HttpGet(
					"http://" + ip_resUAT + ":9080/res/api/v1/ruleapps/" + ruleAppName + "/highest?parts=none");
		} else {
			httpGet = new HttpGet(
					"http://" + ip_resProd + ":9080/res/api/v1/ruleapps/" + ruleAppName + "/highest?parts=none");
		}
		httpGet.setHeader("Accept", "application/json");
		HttpResponse httpResponse = client.execute(httpGet);
		HttpEntity httpEntity = httpResponse.getEntity();
		String httpEntityStr = EntityUtils.toString(httpEntity, "UTF-8");
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(httpEntityStr);
		String ruleAppId = rootNode.path("id").asText();
		return ruleAppId;
	}

	public static String getRuleSetId(CloseableHttpClient client, String ruleAppId, String goal)
			throws ParseException, IOException {
		logger.info("取得RuleSet ID...");
		HttpGet httpGet = null;
		if (goal.equals("UAT")) {
			httpGet = new HttpGet(
					"http://" + ip_resUAT + ":9080/res/api/v1/ruleapps/" + ruleAppId + "/rulesets?parts=version");
		} else {
			httpGet = new HttpGet(
					"http://" + ip_resProd + ":9080/res/api/v1/ruleapps/" + ruleAppId + "/rulesets?parts=version");
		}
		httpGet.setHeader("Accept", "application/json");
		HttpResponse httpResponse = client.execute(httpGet);
		HttpEntity httpEntity = httpResponse.getEntity();
		String httpEntityStr = EntityUtils.toString(httpEntity, "UTF-8");
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(httpEntityStr);
		Iterator<JsonNode> iterator = rootNode.elements();
		int maxVersionNo = 0;
		int maxIndex = 0;
		int i = 0;
		while (iterator.hasNext()) {
			JsonNode ruleset = iterator.next();
			String version = ruleset.path("version").asText();
			if (Integer.parseInt(version.substring(version.indexOf(".") + 1)) > maxVersionNo) {
				maxVersionNo = Integer.parseInt(version.substring(version.indexOf(".") + 1));
				maxIndex = i;
			}
			i++;
		}
		String ruleSetId = rootNode.get(maxIndex).path("id").asText();
		return ruleSetId;

	}

}
