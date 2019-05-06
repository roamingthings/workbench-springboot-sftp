package de.roamingthings.workbench.sftp

import de.roamingthings.workbench.sftp.configuration.SftpProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

private const val SFTP_PORT = 22

@ActiveProfiles("test", "embeddedSftp")
@SpringBootTest
class SimpleEmbeddedSftpTest {

    @Autowired
    lateinit var sftpProperties: SftpProperties

    @Autowired
    lateinit var simpleSftpService: SimpleSftpService


    @Test
    fun `should list file on server`() {
        sftpProperties.port = 2222

        val folderContentList = simpleSftpService.retrieveRemoteFolderList()

        assertThat(folderContentList)
                .extracting("filename")
                .contains("shared-file.txt")
    }
}
