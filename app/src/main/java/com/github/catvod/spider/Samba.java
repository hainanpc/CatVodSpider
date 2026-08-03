package com.github.catvod.spider;

import android.content.Context;
import com.github.catvod.crawler.Spider;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

public class Samba extends Spider {
    
    // ==========================================
    // 💡 目标局域网配置（192.168.2.1 / mine）
    // ==========================================
    private static final String SMB_IP = "192.168.2.1";       
    private static final String SHARE_NAME = "mine";         

    @Override
    public void init(Context context, String ext) {
        // 允许初始化
    }

    // 辅助动态扫描核心：传入什么相对路径，就扫描什么目录
    private String scanDirectory(String relativePath) {
        try {
            JSONObject result = new JSONObject();
            JSONArray list = new JSONArray();
            
            // 每次扫描都动态注入分类
            JSONArray classes = new JSONArray();
            JSONObject clz = new JSONObject();
            clz.put("type_id", "1");
            clz.put("type_name", "📂当前目录: /" + relativePath);
            classes.put(clz);
            result.put("class", classes);

            SMBClient client = new SMBClient();
            try (Connection connection = client.connect(SMB_IP)) {
                AuthenticationContext ac = AuthenticationContext.anonymous(); // 匿名登录
                Session session = connection.authenticate(ac);
                
                try (DiskShare share = (DiskShare) session.connectShare(SHARE_NAME)) {
                    // 实时扫描指定目录下的子文件
                    for (FileIdBothDirectoryInformation f : share.list(relativePath)) {
                        String name = f.getFileName();
                        if (name.equals(".") || name.equals("..") || name.startsWith(".")) continue;
                        
                        JSONObject vod = new JSONObject();
                        
                        // 关键：组装全路径作为ID。如果是子文件夹里的文件，ID形如 "电影/2026/战狼.mp4"
                        String fullPath = relativePath.isEmpty() ? name : relativePath + "/" + name;
                        vod.put("vod_id", fullPath); 
                        vod.put("vod_name", name); 
                        
                        if (f.getFileAttributes() == 16) { // 16代表子文件夹
                            vod.put("vod_pic", "https://icons8.com");
                            vod.put("vod_remarks", "📁 文件夹(点击进入)");
                        } else {
                            vod.put("vod_pic", "https://icons8.com");
                            vod.put("vod_remarks", "🎬 视频媒体");
                        }
                        list.put(vod);
                    }
                }
            } catch (Exception smbException) {
                JSONObject errorVod = new JSONObject();
                errorVod.put("vod_id", "error");
                errorVod.put("vod_name", "❌ 无法读取目录: " + relativePath);
                errorVod.put("vod_pic", "");
                errorVod.put("vod_remarks", smbException.getMessage());
                list.put(errorVod);
            }
            
            result.put("list", list);
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String homeContent(boolean filter) {
        return scanDirectory(""); // 电视开机，默认扫描 mine 盘的根目录
    }

    @Override
    public String homeVideoContent() {
        return scanDirectory("");
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, java.util.HashMap<String, String> extend) {
        return scanDirectory(""); 
    }

    public String categoryContent(String tid, String pg, boolean filter, Object extend) {
        return scanDirectory("");
    }

    @Override
    public String detailContent(List<String> ids) {
        try {
            String pathId = (ids != null && !ids.isEmpty()) ? ids.get(0) : "";
            
            // 💡 突破点：如果用户点击的是一个文件夹（通过路径后缀或属性判断），
            // 我们直接在详情页里动态把该文件夹下的所有子文件，当作“选集/剧集”全部吐出来！
            JSONObject result = new JSONObject();
            JSONArray list = new JSONArray();
            
            JSONObject vod = new JSONObject();
            vod.put("vod_id", pathId);
            vod.put("vod_name", pathId);
            vod.put("vod_pic", "https://icons8.com");
            vod.put("vod_play_from", "局域网视频列表");
            
            // 我们在详情页展开内部文件
            StringBuilder playUrls = new StringBuilder();
            
            SMBClient client = new SMBClient();
            try (Connection connection = client.connect(SMB_IP)) {
                AuthenticationContext ac = AuthenticationContext.authenticate(ac);
                Session session = connection.authenticate(ac);
                try (DiskShare share = (DiskShare) session.connectShare(SHARE_NAME)) {
                    
                    // 尝试去列出用户点击的这个路径
                    for (FileIdBothDirectoryInformation f : share.list(pathId)) {
                        String subName = f.getFileName();
                        if (subName.equals(".") || subName.equals("..")) continue;
                        
                        if (f.getFileAttributes() != 16) { // 只提取里面的视频文件作为播放集数
                            String fileFullPath = pathId.isEmpty() ? subName : pathId + "/" + subName;
                            String playProtocolUrl = "smb://" + SMB_IP + "/" + SHARE_NAME + "/" + fileFullPath;
                            
                            // 拼装成 TVBox 剧集格式： 集数名字 $ 物理真实smb流播放地址
                            playUrls.append(subName).append("$").append(playProtocolUrl).append("#");
                        }
                    }
                }
            } catch (Exception e) {
                // 如果点击的本身就是一个纯视频文件，直接输出自己即可
                String playProtocolUrl = "smb://" + SMB_IP + "/" + SHARE_NAME + "/" + pathId;
                playUrls.append("立即播放$").append(playProtocolUrl);
            }
            
            String urlResult = playUrls.toString();
            if (urlResult.endsWith("#")) urlResult = urlResult.substring(0, urlResult.length() - 1);
            
            vod.put("vod_play_url", urlResult);
            list.put(vod);
            result.put("list", list);
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
