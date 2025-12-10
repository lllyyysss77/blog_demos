/*
 * @Author: 程序员欣宸 zq2599@gmail.com
 * @Date: 2025-11-28 10:29:00
 * @LastEditors: 程序员欣宸 zq2599@gmail.com
 * @LastEditTime: 2025-11-28 10:46:00
 * @FilePath: /langchain4j-totorials/demo-with-spring-boot/src/main/java/com/bolingcavalry/service/Assistant.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.bolingcavalry.service;

import dev.langchain4j.service.*;

public interface Assistant {
    /**
     * 最简单的对话，只返回助手的回答，不包含任何额外信息
     * 
     * @param userMessage 用户消息
     * @return 助手生成的回答
     */
    String simpleChat(String userMessage);

    /**
     * 带记忆的对话，返回助手的回答
     * 
     * @param memoryId    记忆ID
     * @param userMessage 用户消息
     * @return 助手生成的回答
     */
    String chatByMemoryId(@MemoryId int memoryId, @UserMessage String userMessage);
}