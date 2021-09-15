package com.lightningkite.convertlayout

import java.io.File

data class MacLocation(val file: File, val destination: String) {
    fun push() {
        ProcessBuilder()
            .command("scp", "-r", file.absolutePath, "mac:$destination")
            .inheritIO()
            .start()
            .waitFor()
    }
    fun pull() {
        ProcessBuilder()
            .command("scp", "-r", "mac:$destination", file.absolutePath)
            .inheritIO()
            .start()
            .waitFor()
    }
    fun run(command: String) {
        ProcessBuilder()
            .command("ssh", "-t", "mac", "cd $destination; zsh --login")
            .inheritIO()
            .start()
            .waitFor()
    }
}