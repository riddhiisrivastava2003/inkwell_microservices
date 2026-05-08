package com.inkwell.post_category_tag_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PostCategoryTagServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PostCategoryTagServiceApplication.class, args);
	}

}
