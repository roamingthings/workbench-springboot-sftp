package de.roamingthings.workbench.sftp

import com.jcraft.jsch.ChannelSftp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.test.context.ActiveProfiles
import java.lang.Thread.sleep

private const val SFTP_PORT = 22

@ActiveProfiles("test", "embeddedSftp")
@SpringBootTest
class SimpleEmbeddedSftpTest {

    @Autowired
    lateinit var simpleSftpService: SimpleSftpService

    @Autowired
    lateinit var cachedSessionFactory: SessionFactory<ChannelSftp.LsEntry>


    @BeforeEach
    fun setUp() {
        cachedSessionFactory.session.mkdir("foos")
        for (count in 1..10) {
            cachedSessionFactory.session.write("HelloWorld".byteInputStream(), "foos/foo$count.txt")
        }
    }

    @AfterEach
    fun tearDown() {
        cachedSessionFactory.session.remove("foos/foo*.txt")
        cachedSessionFactory.session.rmdir("foos")

    }

    @Test
    fun `should list file on server`() {
        val folderContentList = simpleSftpService.retrieveRemoteFolderList("/foos")

        // Pause to let integration do it's work
        sleep(15_000)

        assertThat(folderContentList)
                .extracting("filename")
                .contains("foo1.txt")
    }
}
