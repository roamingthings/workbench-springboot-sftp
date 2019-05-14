package de.roamingthings.workbench.sftp

import com.jcraft.jsch.ChannelSftp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait.forListeningPort
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File

private const val SFTP_PORT = 22

class KDockerComposeContainer(composeFiles: File) : DockerComposeContainer<KDockerComposeContainer>(composeFiles)

@ActiveProfiles("test", "postgres")
@Testcontainers
@SpringBootTest
class SimpleSftpTest {

    @Autowired
    lateinit var simpleSftpService: SimpleSftpService

    @Autowired
    lateinit var sftpSessionFactory: DefaultSftpSessionFactory

    @Autowired
    lateinit var cachedSessionFactory: SessionFactory<ChannelSftp.LsEntry>

    companion object {
        @Container
        val sftp = KDockerComposeContainer(File("sftp-server/docker-compose-integration-test.yml"))
                .withExposedService("sftp", SFTP_PORT, forListeningPort())
    }

    @BeforeEach
    fun setUp() {
        sftpSessionFactory.setHost(sftp.getServiceHost("sftp", SFTP_PORT))
        sftpSessionFactory.setPort(sftp.getServicePort("sftp", SFTP_PORT))

        cachedSessionFactory.session.mkdir("share/foos")
        for (count in 1..10) {
            cachedSessionFactory.session.write("HelloWorld".byteInputStream(), "foos/foo$count.txt")
        }
    }

    @AfterEach
    fun tearDown() {
        cachedSessionFactory.session.remove("share/foos/foo*.txt")
        cachedSessionFactory.session.rmdir("share/foos")
    }

    @Test
    fun `should list file on server`() {
        val folderContentList = simpleSftpService.retrieveRemoteFolderList("/share/foos")

        // Pause to let integration do it's work
        Thread.sleep(15_000)

        assertThat(folderContentList)
                .extracting("filename")
                .contains("foo1.txt")
    }
}
