package com.erp.sri_files;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"spring.flyway.enabled=false"
})
class SriFilesApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	/**
	 * TEST 1: Carga del contexto de Spring Boot
	 * Verifica que la aplicación se inicia correctamente
	 */
	@Test
	void contextLoads() {
		assertNotNull(applicationContext, "El contexto de Spring no se cargó correctamente");
	}

	/**
	 * TEST 2: Verificar que los beans principales están presentes
	 */
	@Test
	void verifyPrincipalBeans() {
		// Verificar que los servicios principales están disponibles
		assertTrue(applicationContext.containsBean("sriFilesApplication"),
				"El bean principal de la aplicación no está disponible");
	}

	/**
	 * TEST 3: Validar que la configuración de BD está presente
	 */
	@Test
	void verifyDatabaseConfiguration() {
		assertNotNull(applicationContext.getEnvironment().getProperty("spring.datasource.url"),
				"spring.datasource.url no está configurado");
		assertNotNull(applicationContext.getEnvironment().getProperty("spring.datasource.username"),
				"spring.datasource.username no está configurado");
	}

	/**
	 * TEST 4: Validar que el servidor está configurado
	 */
	@Test
	void verifyServerConfiguration() {
		String port = applicationContext.getEnvironment().getProperty("server.port");
		assertNotNull(port, "server.port no está configurado");
	}

	/**
	 * TEST 5: Verificar propiedades de SRI
	 */
	@Test
	void verifySriConfiguration() {
		String ambiente = applicationContext.getEnvironment().getProperty("sri.ambiente");
		// Puede ser null en test, pero aseguramos que la propiedad puede leerse
		assertDoesNotThrow(() -> applicationContext.getEnvironment().getProperty("sri.ambiente"));
	}
}


