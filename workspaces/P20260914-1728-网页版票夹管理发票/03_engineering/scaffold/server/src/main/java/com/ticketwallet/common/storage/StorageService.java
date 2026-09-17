package com.ticketwallet.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 本地卷存储（StorageService 抽象，演进路径 S3Provider 切换，业务代码零改动——tech-stack §6.1）。
 * 键格式：{yyyyMM}/{uuid}.{ext}，根目录 dev=./data/attachments（生产 /data/attachments 卷挂载）。
 */
@Service
public class StorageService {

    private final Path root;

    public StorageService(@Value("${app.storage.root:./data/attachments}") String root) throws IOException {
        this.root = Path.of(root);
        Files.createDirectories(this.root);
    }

    public String put(String key, InputStream in) throws IOException {
        Path target = root.resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    public Path get(String key) {
        return root.resolve(key);
    }

    public boolean delete(String key) {
        try {
            return Files.deleteIfExists(root.resolve(key));
        } catch (IOException e) {
            return false;
        }
    }
}
