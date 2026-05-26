package cn.hsine.mem0.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application for mem0 server.
 * Provides REST API for memory management with authentication and configuration.
 *
 * @author MoBai

 */
@SpringBootApplication(scanBasePackages = "cn.hsine.mem0")
@MapperScan(basePackages = {"cn.hsine.mem0.core.repository", "cn.hsine.mem0.server.repository"})
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
