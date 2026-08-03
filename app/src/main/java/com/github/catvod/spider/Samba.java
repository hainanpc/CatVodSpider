package com.github.catvod.spider;

import android.content.Context;
import com.github.catvod.crawler.Spider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;

public class Samba extends Spider {
    
    // ==========================================
    // 💡 在这里直接写死你的局域网信息
    // ==========================================
    private static final String SMB_IP = "192.168.1.100";      // 填你电脑的真实局域网 IP
    private static final String SHARE_NAME = "Movies";         // 填你电脑的真实共享文件夹名称

    @Override
    public void init(Context context, String ext) {
        super.init(context, ext);
    }

    @Override
    public String homeContent(boolean filter) {
        try {
            JSONObject result = new JSONObject();
            
            // 1. 全新架构的分类注入
            JSONArray classes = new JSONArray();
            JSONObject clz = new JSONObject();
            clz.put("type_id", "1");
            clz.put("type_name", "局域网视频浏览");
            classes.put(clz);
            result.put("class", classes);
            
            // 2. 注入局域网虚拟测试卡片列表
            JSONArray list = new JSONArray();
            for (int i = 1; i <= 5; i++) {
                JSONObject vod = new JSONObject();
                vod.put("vod_id", "video_" + i + ".mp4");
                vod.put("vod_name", "局域网电影测试通道 " + i);
                vod.put("vod_pic", "https://icons8.com");
                vod.put("vod_remarks", "超清本地流");
                list.put(vod);
            }
            result.put("list", list);
            
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, Map<String, String> extend) {
        // 核心修复点：将原来的 HashMap 改为标准的 Map，完美契合官方最新接口定义
        return homeContent(filter);
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
            vod.put("vod_play_from", "局域网直接解码");
            
            // 核心强行注入：将绝对合规的 SMB 物理路径直接硬塞给播放器
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
