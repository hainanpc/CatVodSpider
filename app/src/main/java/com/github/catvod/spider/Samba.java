package com.github.catvod.spider;

import android.content.Context;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderResult;
import com.github.catvod.pojo.Vod;
import com.github.catvod.pojo.Class;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import java.util.ArrayList;
import java.util.List;

public class Samba extends Spider {
    
    // ==========================================
    // 💡 在这里直接写死你的局域网信息（免密登录）
    // ==========================================
    private static final String SMB_IP = "192.168.2.1";      // 填你电脑的局域网 IP
    private static final String SHARE_NAME = "mine";         // 填你电脑的共享文件夹名称
    private static final String PLAY_FROM = "局域网";

    @Override
    public void init(Context context, String ext) {
        super.init(context, ext);
        // 彻底停用 ext 传递，防止外部干扰
    }

    @Override
    public String homeContent(boolean filter) {
        List<Class> classes = new ArrayList<>();
        List<Vod> list = new ArrayList<>();
        
        // 默认主页直接展示你写死的这个共享文件夹
        classes.add(new Class("1", SHARE_NAME));
        
        SMBClient client = new SMBClient();
        try (Connection connection = client.connect(SMB_IP)) {
            // 免密/匿名登录，传入空凭证
            AuthenticationContext ac = AuthenticationContext.anonymous();
            Session session = connection.authenticate(ac);
            
            try (DiskShare share = (DiskShare) session.connectShare(SHARE_NAME)) {
                // 遍历共享文件夹下的根目录文件
                for (FileIdBothDirectoryInformation f : share.list("")) {
                    String name = f.getFileName();
                    if (name.equals(".") || name.equals("..")) continue;
                    
                    Vod vod = new Vod();
                    vod.setVodId(name); // 以文件名作为唯一的 ID
                    vod.setVodName(name);
                    
                    if (f.getFileAttributes() == 16) { // 16 代表是文件夹目录
                        vod.setVodPic("https://icons8.com");
                        vod.setVodTag("文件夹");
                    } else {
                        vod.setVodPic("https://icons8.com");
                        vod.setVodTag("视频文件");
                    }
                    list.add(vod);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SpiderResult.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, java.util.HashMap<String, String> extend) {
        return homeContent(filter); // 免去复杂逻辑，直接复用主页刷新
    }

    @Override
    public String detailContent(List<String> ids) {
        String fileName = ids.get(0);
        List<Vod> list = new ArrayList<>();
        
        Vod vod = new Vod();
        vod.setVodId(fileName);
        vod.setVodName(fileName);
        vod.setVodPic("https://icons8.com");
        vod.setVodPlayFrom(PLAY_FROM);
        
        // 核心亮点：直接强行把拼好的 smb:// 协议路径扔给播放器，彻底绕过 OkHttp 的限制
        String finalPlayUrl = "smb://" + SMB_IP + "/" + SHARE_NAME + "/" + fileName;
        vod.setVodPlayUrl(fileName + "$" + finalPlayUrl);
        
        list.add(vod);
        return SpiderResult.string(list);
    }
}
