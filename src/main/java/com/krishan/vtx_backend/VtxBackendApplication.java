package com.krishan.vtx_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VtxBackendApplication {

	public static void main(String[] args) {
		String url = System.getenv("SPRING_DATASOURCE_URL");
		String user = System.getenv("SPRING_DATASOURCE_USERNAME");
		String pass = System.getenv("SPRING_DATASOURCE_PASSWORD");
		System.out.println("DEBUG_ENV_URL=[" + url + "] len=" + (url == null ? -1 : url.length()));
		System.out.println("DEBUG_ENV_USER=[" + user + "] len=" + (user == null ? -1 : user.length()));
		System.out.println("DEBUG_ENV_PASS_LEN=" + (pass == null ? -1 : pass.length()));
		System.out.println("DEBUG_ENV_PASS_BYTES=" + (pass == null ? "null" : java.util.Arrays.toString(pass.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
		SpringApplication.run(VtxBackendApplication.class, args);
	}

}
