package de.roamingthings.workbench.sftp

import com.jcraft.jsch.ChannelSftp.LsEntry
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.stereotype.Service

@Service
class SimpleSftpService(val cachedSessionFactory: SessionFactory<LsEntry>) {

    fun retrieveRemoteFolderList(path: String): Array<out LsEntry> {

        return cachedSessionFactory.session.list(path)
    }
}
