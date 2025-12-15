package com.bolingcavalry.service;

import dev.langchain4j.service.TokenStream;
import java.lang.reflect.Method;

public class TokenStreamTest {
    public static void main(String[] args) {
        System.out.println("TokenStream类名: " + TokenStream.class.getName());
        System.out.println("TokenStream方法列表:");
        Method[] methods = TokenStream.class.getMethods();
        for (Method method : methods) {
            System.out.println("- " + method.getName());
            for (Class<?> paramType : method.getParameterTypes()) {
                System.out.println("  参数: " + paramType.getName());
            }
            System.out.println("  返回类型: " + method.getReturnType().getName());
        }
    }
}