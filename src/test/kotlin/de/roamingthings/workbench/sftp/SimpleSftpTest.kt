package de.roamingthings.workbench.sftp

import de.roamingthings.workbench.sftp.configuration.SftpConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait.forListeningPort
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File

private const val SFTP_PORT = 22

class KDockerComposeContainer(composeFiles: File) : DockerComposeContainer<KDockerComposeContainer>(composeFiles)

@Testcontainers
@SpringBootTest
class SimpleSftpTest {

    @Autowired
    lateinit var sftpConfiguration: SftpConfiguration

    @Autowired
    lateinit var simpleSftpService: SimpleSftpService


    companion object {
        @Container
        val sftp = KDockerComposeContainer(File("sftp-server/docker-compose-integration-test.yml"))
                .withExposedService("sftp", SFTP_PORT, forListeningPort())
    }

    @Test
    fun `should list file on server`() {
        sftpConfiguration.remoteHost = sftp.getServiceHost("sftp", SFTP_PORT)
        sftpConfiguration.remotePort = sftp.getServicePort("sftp", SFTP_PORT)

        val folderContentList = simpleSftpService.retrieveRemoteFolderList()

        assertThat(folderContentList)
                .extracting("filename")
                .contains("shared-file.txt")
    }
}