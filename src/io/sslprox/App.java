package io.sslprox;

import java.util.HashMap;
import java.util.Map;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

public class App {

	public static Map<String, String> argMap = new HashMap<String, String>() {
		{
			// default args
			put("api", "https://api.sslprox.io");
			put("storage_api", "https://sslprox.io");
			put("dest", "/");
		}
	};

	public static void main(String[] args) {
		try {
			for (String a : args) {
				String[] ap = a.split("=");
				argMap.put(ap[0], ap.length == 2 ? ap[1] : null);
			}

			HttpResponse<JsonNode> versionRes = Unirest.get(argMap.get("api") + "/config/version").asJson();
			if (versionRes.getStatus() != 200)
				throw new Exception("Cannot fetch version config");
			JSONObject res = versionRes.getBody().getObject();
			int version = res.getInt("version");
			int build = res.getInt("build");
			String env = res.getString("env");

			System.out.println("Newest version: " + version + "." + build + "_" + env);
			
			// TODO: load current version
			
			
			// load libraries
			//HttpResponse<JsonNode> 
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
