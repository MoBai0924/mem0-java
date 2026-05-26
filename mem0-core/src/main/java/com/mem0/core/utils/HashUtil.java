package com.mem0.core.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    /**
     * 生成 MD5 哈希（和 Python hashlib.md5 完全一致）
     */
    public static String md5Hex(String text) {
        try {
            // 1. 获取 MD5 算法实例
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            // 2. 编码（UTF-8）+ 计算哈希
            byte[] hashBytes = md5.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // 3. 转 16 进制字符串（小写，和 Python hexdigest() 一致）
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }
}
