package com.campus.poi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类。
 * Spring Boot 的入口，等价于 Flask 里 `if __name__ == '__main__': app.run()`。
 * 内嵌了 Tomcat，所以不用另外装服务器，直接 run 就能访问 8080。
 */
@SpringBootApplication
public class PoiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PoiApplication.class, args);
        System.out.println("Java 版已启动：http://127.0.0.1:8080");
    }
}
