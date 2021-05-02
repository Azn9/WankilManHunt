package dev.azn9.wankilhunter.util;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class IOUtil {

    public static void download(String url, File file) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(new URL(url).openStream());
        FileOutputStream outputStream = new FileOutputStream(file);
        FileChannel fileChannel = outputStream.getChannel();

        fileChannel.transferFrom(channel, 0, Long.MAX_VALUE);
    }

    public static void decompressGzip(File source, File target) throws IOException {
        Path sourcePath = source.toPath();
        Path targetPath = target.toPath();

        if (Files.notExists(sourcePath)) {
            throw new IOException("File doesn't exists!");
        }

        try (InputStream inputStream = Files.newInputStream(sourcePath);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
             GzipCompressorInputStream compressorInputStream = new GzipCompressorInputStream(bufferedInputStream);
             TarArchiveInputStream archiveInputStream = new TarArchiveInputStream(compressorInputStream)) {

            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                // create a new path, zip slip validate
                Path newPath = zipSlipProtect(entry, targetPath);

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    // check parent folder again
                    Path parent = newPath.getParent();
                    if (parent != null) {
                        if (Files.notExists(parent)) {
                            Files.createDirectories(parent);
                        }
                    }

                    // copy TarArchiveInputStream to Path newPath
                    Files.copy(archiveInputStream, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Path zipSlipProtect(ArchiveEntry entry, Path targetDir) throws IOException {
        Path targetDirResolved = targetDir.resolve(entry.getName());
        Path normalizePath = targetDirResolved.normalize();

        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad entry: " + entry.getName());
        }

        return normalizePath;
    }
}
