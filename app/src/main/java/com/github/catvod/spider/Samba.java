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
    // 💡 已按照您的要求：写死为您家里的真实局域网配置
    // ==========================================
    private static final String SMB_IP = "192.168.2.1";       // 您的路由器或电脑内网IP
    private static final String SHARE_NAME = "mine";         // 您的免密共享文件夹名称

    @Override
    public void init(Context context, String ext) {
        // 初始化放行
    }

    @Override
    public String homeContent(boolean filter) {
        try {
            JSONObject result = new JSONObject();
            
            // 1. 组装电视端分类导航
            JSONArray classes = new JSONArray();
            JSONObject clz = new JSONObject();
            clz.put("type_id", "1");
            clz.put("type_name", "📂本地局域网: " + SHARE_NAME);
            classes.put(clz);
            result.put("class", classes);
            
            // 2. 核心：通过新版内置的 smbj 库，动态扫描免密共享文件夹
            JSONArray list = new JSONArray();
            SMBClient client = new SMBClient();
            
            try (Connection connection = client.connect(SMB_IP)) {
                // 采用标准匿名（免密）上下文登录
                AuthenticationContext ac = AuthenticationContext.anonymous();
                Session session = connection.authenticate(ac);
                
                try (DiskShare share = (DiskShare) session.connectShare(SHARE_NAME)) {
                    // 实时遍历该共享文件夹下的所有物理文件
                    for (FileIdBothDirectoryInformation f : share.list("")) {
                        String name = f.getFileName();
                        
                        // 过滤掉系统自带的隐藏干扰项
                        if (name.equals(".") || name.equals("..") || name.startsWith(".")) continue;
                        
                        JSONObject vod = new JSONObject();
                        vod.put("vod_id", name); // 将真实文件名作为ID传递
                        vod.put("vod_name", name); // 在电视屏幕上显示真实电影名称
                        
                        // 判断是文件夹还是普通视频文件（16代表文件夹属性）
                        if (f.getFileAttributes() == 16) {
                            vod.setVodPic("https://icons8.com");
                            vod.put("vod_remarks", "文件夹");
                        } else {
                            vod.put("vod_pic", "https://icons8.com");
                            vod.put("vod_remarks", "电影/视频");
                        }
                        list.put(vod);
                    }
                }
            } catch (Exception smbException) {
                // 如果局域网连接失败，在电视上吐出错误提示卡片方便排查
                JSONObject errorVod = new JSONObject();
                errorVod.put("vod_id", "error");
                errorVod.put("vod_name", "❌ 局域网连接失败，请检查 192.168.2.1 是否开启共享");
                errorVod.put("vod_pic", "");
                errorVod.put("vod_remarks", smbException.getMessage());
                list.put(errorVod);
            }
            
            result.put("list", list);
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, Object extend) {
        return homeContent(filter); // 切换分类时同步触发实时扫描
    }

    @Override
    public String detailContent(List<String> ids) {
        try {
            String fileName = ids.get(0);
            JSONObject result = new JSONObject();
            JSONArray list = new JSONArray();
            
            JSONObject vod = new JSONObject();
            vod.put("vod_id", fileName);
            vod.put("vod_name", fileName);
            vod.put("vod_pic", "https://icons8.com");
            vod.put("vod_play_from", "SMB直解通道");
            
            // 拼接纯净的无密码原生 smb 串流路径投喂给播放器
            String finalPlayUrl = "smb://" + SMB_IP + "/" + SHARE_NAME + "/" + fileName;
            vod.put("vod_play_url", "立即播放$" + finalPlayUrl);
            
            list.put(vod);
            result.put("list", list);
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
