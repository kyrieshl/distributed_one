package com.litemall.distributed_one;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages={"com.litemall.distributed_one","org.linlinjava.litemall.db"})
@MapperScan("org.linlinjava.litemall.db.dao")
public class DistributedOneApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistributedOneApplication.class, args);
	}
}
