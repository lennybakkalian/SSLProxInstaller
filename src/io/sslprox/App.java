package io.sslprox;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JFrame;
import javax.swing.JLabel;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class App {

	public static Map<String, String> argMap = new HashMap<String, String>() {
		{
			// default args
			put("api", "https://api.sslprox.io");
			put("dest", "");
			put("update", "all");
		}
	};

	public static JLabel status;

	public static void main(String[] args) {
		try {
			for (String a : args) {
				String[] ap = a.split("=");
				argMap.put(ap[0], ap.length == 2 ? ap[1] : null);
			}

			JFrame frame = new JFrame("SSLProx installer");
			frame.setAlwaysOnTop(true);
			status = new JLabel("Loading...");
			frame.add(status);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(400, 90);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);

			File destDir = new File(argMap.get("dest"));
			File libDir = new File(destDir.getAbsoluteFile() + "/sslprox_lib");
			File sslproxFile = new File(destDir.getAbsoluteFile() + "/sslprox.jar");
			if (!libDir.exists() || !libDir.isDirectory())
				libDir.mkdir();

			setStatus("Fetch newest version...");

			HttpResponse<JsonNode> versionRes = Unirest.get(argMap.get("api") + "/config/version").asJson();
			if (versionRes.getStatus() != 200)
				throw new Exception("Cannot fetch version config");
			JSONObject res = versionRes.getBody().getObject();
			int version = res.getInt("version");
			int build = res.getInt("build");
			String env = res.getString("env");

			String newestVersion = version + "." + build + "_" + env;

			appendStatus("found: " + newestVersion);

			System.out.println("Newest version: " + newestVersion);

			boolean update = true;
			try {
				if (sslproxFile.exists()) {
					// TODO: load current version
					ZipFile zipFile = new ZipFile(sslproxFile);
					Enumeration<? extends ZipEntry> entries = zipFile.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						if (entry.getName().equals("version.conf")) {
							System.out.println("Read version.conf");
							InputStream stream = zipFile.getInputStream(entry);
							BufferedReader br = new BufferedReader(new InputStreamReader(stream));
							StringBuilder sb = new StringBuilder();
							String ln;
							int fVersion = -1;
							int fBuild = -1;
							String fEnv = "";
							while ((ln = br.readLine()) != null) {
								String[] lnArgs = ln.split("=");
								switch (lnArgs[0].toLowerCase()) {
								case "version":
									fVersion = Integer.valueOf(lnArgs[1]);
									break;
								case "build":
									fBuild = Integer.valueOf(lnArgs[1]);
									break;
								case "env":
									fEnv = lnArgs[1];
									break;
								}
							}
							update = false;
							String curVersion = fVersion + "." + fBuild + "_" + fEnv;
							System.out.println("Current version: " + curVersion);
							if (fVersion != version)
								update = true;
							if ((fBuild != build || !fEnv.equals(env)) && argMap.get("update").equals("all"))
								update = true;
							if (update) {
								System.out.println("Update from " + curVersion + " => " + newestVersion);
							}
							br.close();
						}
					}

					zipFile.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (update) {
				if (sslproxFile.exists())
					sslproxFile.delete();
				System.out.println("[DL] Latest sslprox file");
				setStatus("Download latest SSLProx release...");
				HttpResponse<File> fileRes = Unirest.get(argMap.get("api") + "/public/files/download/latest")
						.asFile(sslproxFile.getAbsolutePath());
			}

			// load libraries
			HttpResponse<JsonNode> libRes = Unirest.get(argMap.get("api") + "/public/files").asJson();
			JSONArray libFiles = libRes.getBody().getArray();

			List<String> newFiles = new ArrayList<String>();
			libFiles.forEach(f -> newFiles.add(((JSONObject) f).getString("name")));
			List<String> oldFiles = new ArrayList<String>();
			for (File f : libDir.listFiles())
				oldFiles.add(f.getName());

			List<String> dlFiles = U.merge(oldFiles, newFiles, (a, b) -> a.equals(b), null, r -> {
				// remove
				File remFile = new File(libDir.getAbsoluteFile() + "/" + r);
				if (remFile.exists()) {
					System.out.println("[DL] Remove: " + remFile.getAbsolutePath());
					setStatus("Remove: " + remFile.getName());
					remFile.delete();
				}
			});

			dlFiles.forEach(name -> {
				// check if file already downloaded
				File targetLib = new File(libDir.getAbsolutePath() + "/" + name);
				if (targetLib.exists()) {
					System.out.println("[DL] Skip library: " + targetLib.getAbsolutePath());
					setStatus("Skip " + targetLib.getName());
				} else {
					// download file to lib dir
					setStatus("Download " + targetLib.getName());
					System.out.print("[DL] Download library " + targetLib.getAbsolutePath());
					HttpResponse<File> dlFileRes = Unirest.get(argMap.get("api") + "/public/files/download/" + name)
							.asFile(targetLib.getAbsolutePath());
					System.out.println(" DONE!");
				}
			});

			setStatus("Update done! - Start SSLProx...");
			
			// TODO: exec jar with downloaded jre
		} catch (Exception e) {
			e.printStackTrace();
			U.popup("Error", e.getMessage());
		}
	}

	public static void setStatus(String text) {
		U.swing(() -> status.setText(text));
	}

	public static void appendStatus(String text) {
		U.swing(() -> status.setText(status.getText() + " " + text));
	}

}
