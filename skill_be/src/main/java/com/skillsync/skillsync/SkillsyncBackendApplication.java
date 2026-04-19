package com.skillsync.skillsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication()
@EnableScheduling
public class SkillsyncBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkillsyncBackendApplication.class, args);
	}

}
