package com.skillsync.skillsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.skillsync.skillsync", "ai"})
@EnableScheduling
public class SkillsyncBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkillsyncBackendApplication.class, args);
	}

}
