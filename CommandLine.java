package com.veivo.core.util;

import java.io.File;
import java.io.IOException;

import com.veivo.core.portlet.Constant;
import com.veivo.rest.v1.pack.App;

public class CommandLine {
	public static void sendEmail(String from, String to, String subject, String content) {
		String[] cmd = {"sendemail",from,to,subject,content};
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getPreviewPath(String videoPath) {
		String imagePath = parseImagePath(videoPath);
		String cmd = "ffmpeg -i "+videoPath+" -ss 1 -vframes 1 -r 1 -ac 1 -ab 2 -s 80*80 -f image2 "+imagePath;
    	exec(cmd);
		return imagePath;
	}
	
	public static void packToZipFile(String home) {
		String zipFile = home + App.ZIP_FILE;
		String indexFile = home + App.INDEX_FILE;
		String configFile = home + App.CONFIG_FILE;
		String iconFile = home + App.ICON;
		String cmd = "zip " + zipFile + " " + indexFile
				+ " " + configFile + " " + iconFile; 
		exec(cmd);
	}
	
	public static void copyImgToIcon(String iconsrc, String destination) {
		String cmd = "cp " + iconsrc + " " + destination;
		exec(cmd);
	}
	
	private static void exec(String cmd) {
		try {
			Process process=Runtime.getRuntime().exec(cmd);
			//block this thread until the operation done
			process.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static String parseImagePath(String videoPath) {
		String[] path = videoPath.split("/");
		int len = path.length;
		String filename = path[len-1].split("\\.")[0];
		//TODO 改为相对路径
		File file = new File(Constant.UPLOADATTACHMENTS+"/preview");
		if(!file.exists()){
			file.mkdirs();
		}
		String imagePath = Constant.UPLOADATTACHMENTS+"/preview/"+filename+".jpg";
		return imagePath;
	}
}
