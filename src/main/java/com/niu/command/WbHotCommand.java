package com.niu.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.niu.anno.Command;
import com.niu.bean.WbHotObject;
import com.niu.util.ApiUtil;
import com.niu.util.SeleniumUtil;
import com.niu.util.SkikoUtil;
import jakarta.annotation.PostConstruct;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.utils.ExternalResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 微博热搜
 *
 * @authoer:hff
 * @Date 2023/9/6 11:39
 */
@Command
@Component
public class WbHotCommand implements BotCommand {

    private Cache<Integer, WbHotObject> hotCache;

    private final Duration EXPIRE_TIME = Duration.ofMinutes(30L);

    @PostConstruct
    public void init(){
        hotCache = CacheBuilder.newBuilder().expireAfterWrite(EXPIRE_TIME).build();
    }


    @Override
    public String command() {
        return "wbhot";
    }

    @Override
    public Message execute(Member sender, MessageChain messageChain, Contact contact,String...args) {
        try{
            if (args!=null&&args.length>0){
                WbHotObject wb = hotCache.getIfPresent(Integer.valueOf(args[0]));
                if (wb==null){
                    hotCache.putAll(ApiUtil.getWbHot());
                    wb = hotCache.getIfPresent(Integer.valueOf(args[0]));
                }
                if (wb==null){
                    throw new NumberFormatException();
                }
                InputStream inputStream = SeleniumUtil.screenshot(wb.getUrl());
                if (inputStream!=null){
                    Image image = ExternalResource.uploadAsImage(inputStream, contact);
                    return new MessageChainBuilder().append(image).build();
                }
                return new MessageChainBuilder().append("啊？").build();
            }
            if (hotCache.size()==0){
                hotCache.putAll(ApiUtil.getWbHot());
            }
            List<String> drawList = new ArrayList<>();
            for (long i = 1L; i <= hotCache.size(); i++) {
                WbHotObject wb = hotCache.getIfPresent((int) i);
                if (wb!=null){
                    drawList.add(i+"."+wb.getTitle());
                    drawList.add(wb.getHot());
                }
            }
            String fileName = SkikoUtil.drawTextLine(drawList);
            ExternalResource externalResource = ExternalResource.create(new File("./"+fileName));
            Image image = contact.uploadImage(externalResource);
            externalResource.close();
            return new MessageChainBuilder().append(image).build();

        }catch (NumberFormatException e){
            return new MessageChainBuilder().append("憋在这理发店").build();
        }catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
