package com.skillsync.skillsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

<<<<<<< HEAD:src/main/java/com/skillsync/skillsync/SkillsyncBackendApplication.java
@SpringBootApplication
=======
@SpringBootApplication()
@EnableScheduling
>>>>>>> origin/quokka:skill_be/src/main/java/com/skillsync/skillsync/SkillsyncBackendApplication.java
public class SkillsyncBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkillsyncBackendApplication.class, args);
	}

}
