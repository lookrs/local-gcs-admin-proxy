package org.llk.gcsAdmin;

import org.llk.gcsAdmin.service.GcsUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class GcsAdminApplication {
	public static void main(String[] args) throws IOException {
		SpringApplication.run(GcsAdminApplication.class, args);
	}

}
