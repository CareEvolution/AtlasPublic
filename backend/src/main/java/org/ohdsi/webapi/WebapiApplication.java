package org.ohdsi.webapi;

import com.cosium.spring.data.jpa.entity.graph.repository.support.EntityGraphJpaRepositoryFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = { ErrorMvcAutoConfiguration.class })
@ConfigurationPropertiesScan
@EntityScan(basePackages = "org.ohdsi.webapi")
@EnableJpaRepositories(
		basePackages = "org.ohdsi.webapi",
		entityManagerFactoryRef = "entityManagerFactory",
		repositoryFactoryBeanClass = EntityGraphJpaRepositoryFactoryBean.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class WebapiApplication {
	public static void main(String[] args) {
		SpringApplication.run(WebapiApplication.class, args);
	}
}
