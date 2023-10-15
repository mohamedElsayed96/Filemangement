package com.gizasystems.filemanagement.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Getter
@Setter
@PropertySource("${spring.config.location}/modules-filesystem.config.properties")
@Profile({"filesystem"})
@ConfigurationProperties("filesystem")
public class FileSystemConfig {



    private long maxFileSize;
    private String storagePath;
    private String metaStoragePath;
}
