package de.roamingthings.workbench.sftp

import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ListSftpController(private val simpleSftpService: SimpleSftpService) {

    @GetMapping("/ls")
    fun listSftp(): ResponseEntity<List<String>> {
        val list = simpleSftpService.retrieveRemoteFolderList("/foos")
        return ok(list.map { it.filename.toString() })
    }
}
