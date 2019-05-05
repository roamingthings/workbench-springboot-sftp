package de.roamingthings.workbench.sftp

import de.roamingthings.workbench.sftp.configuration.SftpConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(SftpConfiguration::class)
class Application

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
