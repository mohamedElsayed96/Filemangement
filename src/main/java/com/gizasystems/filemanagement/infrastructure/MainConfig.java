package com.gizasystems.filemanagement.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Set;

@Configuration
@Getter
@Setter
@PropertySource("${spring.config.location}/modules-main.config.properties")
public class MainConfig {


    @Value("${max_file_size}")
    private long maxFileSize;
    @Value("${allowed_mime_types}")
    private Set<String> allowedMimeTypes;

}
