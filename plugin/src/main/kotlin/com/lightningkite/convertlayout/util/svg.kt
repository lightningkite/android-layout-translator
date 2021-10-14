package com.lightningkite.convertlayout.util

import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.io.File

fun File.convertSvgToPng(out: File = File.createTempFile(this.nameWithoutExtension, ".png")): File {
    out.outputStream().use { outStream ->
        this.inputStream().use { inStream ->
            try {
                PNGTranscoder().transcode(TranscoderInput(inStream), TranscoderOutput(outStream))
            } catch(e: Exception) {
                println("Failed to translate $this")
                throw Exception("Failed to translate $this", e)
            }
        }
        outStream.flush()
    }
    return out
}