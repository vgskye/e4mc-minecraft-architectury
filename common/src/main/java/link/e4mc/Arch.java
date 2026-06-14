package link.e4mc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for detecting the current platform's architecture and OS,
 * and for pre-loading native QUIC libraries on platforms not covered by the
 * e4mc natives CDN.
 *
 * <p>The e4mc natives CDN at {@code https://natives.e4mc.link/} currently
 * only provides x86_64 Linux builds of {@code libnetty_quiche.so}. This
 * class attempts to download a compatible native library from GitHub
 * Releases when running on other architectures (e.g. aarch64 Linux/Android).
 */
public class Arch {
    private static final Logger LOGGER = LoggerFactory.getLogger(E4mcClient.MOD_ID);

    /** Cached result - computed once at first call */
    private static Boolean cachedSupported = null;
    private static String cachedOsName = null;
    private static String cachedOsArch = null;
    private static final AtomicBoolean nativeLoadAttempted = new AtomicBoolean(false);

    /**
     * URL for the GitHub release where aarch64 native libraries are hosted.
     * Update this when publishing new native library releases.
     */
    private static final String NATIVES_RELEASE_BASE =
            "https://github.com/vgskye/e4mc-minecraft-architectury/releases/download";

    /** Tag used for native library releases */
    private static final String NATIVES_TAG = "natives-v1";

    public static boolean isPlatformSupported() {
        if (cachedSupported == null) {
            computePlatform();
        }
        return cachedSupported;
    }

    public static String getOsName() {
        if (cachedOsName == null) {
            computePlatform();
        }
        return cachedOsName;
    }

    public static String getOsArch() {
        if (cachedOsArch == null) {
            computePlatform();
        }
        return cachedOsArch;
    }

    /**
     * Attempt to pre-load the native QUIC library for this platform.
     * This should be called early, before any QUIC operations.
     * <p>
     * For platforms where the CDN already provides the library (x86_64 Linux),
     * this is a no-op. For aarch64 Linux and Android, this will attempt to
     * download and load the native library from the GitHub releases.
     *
     * @return true if the native library was loaded successfully (or was
     *         not needed), false if loading failed
     */
    public static boolean tryLoadNativeLibrary() {
        if (!nativeLoadAttempted.compareAndSet(false, true)) {
            return cachedSupported != null && cachedSupported;
        }

        // Ensure platform info is computed
        if (cachedSupported == null) {
            computePlatform();
        }

        // If already supported (x86_64 Linux), no need to pre-load
        if (cachedSupported) {
            return true;
        }

        // Only attempt to load for aarch64/arm64 platforms
        String osArch = cachedOsArch;
        boolean isAarch64 = osArch.equals("aarch64") || osArch.equals("arm64");

        if (!isAarch64) {
            LOGGER.debug("Skipping native library pre-load: unsupported architecture '{}'", osArch);
            return false;
        }

        try {
            Path libFile = downloadNativeLibrary(osArch);
            if (libFile != null) {
                System.load(libFile.toAbsolutePath().toString());
                LOGGER.info("Successfully loaded native QUIC library from {}", libFile);
                cachedSupported = true;
                return true;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to download/load native QUIC library for {}: {}",
                    osArch, e.getMessage());
            LOGGER.debug("Native library load failure details:", e);
        }

        return false;
    }

    /**
     * Download the appropriate native library for this platform.
     */
    private static Path downloadNativeLibrary(String osArch) throws Exception {
        boolean isAndroid = cachedOsName.contains("android");

        String fileName;
        if (isAndroid) {
            fileName = "libnetty_quiche_aarch64_android.so";
        } else {
            fileName = "libnetty_quiche_linux_aarch_64.so";
        }

        // Try multiple download URLs
        String[] urls = {
                // Primary: GitHub releases
                NATIVES_RELEASE_BASE + "/" + NATIVES_TAG + "/" + fileName,
                // Fallback: direct from the CDN (architecture-specific)
                "https://natives.e4mc.link/" + fileName,
        };

        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "e4mc-natives");
        Files.createDirectories(tempDir);

        Path destFile = tempDir.resolve(fileName);

        // If already downloaded and valid, reuse it
        if (Files.exists(destFile) && Files.size(destFile) > 0) {
            LOGGER.info("Using cached native library: {}", destFile);
            return destFile;
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        for (String url : urls) {
            LOGGER.info("Attempting to download native library from: {}", url);
            try {
                HttpRequest request = HttpRequest.newBuilder(new URI(url)).build();
                HttpResponse<Path> response = client.send(request,
                        HttpResponse.BodyHandlers.ofFile(destFile, StandardCopyOption.REPLACE_EXISTING));

                if (response.statusCode() == 200 && Files.size(destFile) > 0) {
                    LOGGER.info("Downloaded native library ({} bytes) from {}",
                            Files.size(destFile), url);
                    return destFile;
                } else {
                    LOGGER.warn("Failed to download from {}: HTTP {}", url, response.statusCode());
                    try { Files.deleteIfExists(destFile); } catch (IOException ignored) {}
                }
            } catch (Exception e) {
                LOGGER.warn("Download attempt failed for {}: {}", url, e.getMessage());
                try { Files.deleteIfExists(destFile); } catch (IOException ignored) {}
            }
        }

        return null;
    }

    private static void computePlatform() {
        String osName = System.getProperty("os.name", "unknown").toLowerCase();
        String osArch = System.getProperty("os.arch", "unknown").toLowerCase();

        // Detect Android
        String javaVmName = System.getProperty("java.vm.name", "").toLowerCase();
        String javaRuntimeName = System.getProperty("java.runtime.name", "").toLowerCase();

        cachedOsName = System.getProperty("os.name", "unknown");
        cachedOsArch = System.getProperty("os.arch", "unknown");

        boolean isAndroid = osName.contains("android")
                || javaVmName.contains("dalvik")
                || javaRuntimeName.contains("android")
                || System.getProperty("java.vendor", "").toLowerCase().contains("android");

        // Normalize architecture
        boolean isX86_64 = osArch.equals("amd64") || osArch.equals("x86_64");
        boolean isAarch64 = osArch.equals("aarch64") || osArch.equals("arm64");
        boolean isArm = osArch.startsWith("arm") && !isAarch64;
        boolean isX86 = osArch.equals("x86") || osArch.equals("i386") || osArch.equals("i686");

        if (isAndroid) {
            // Android: only attempt aarch64 for now
            cachedSupported = false;
            LOGGER.info("Detected Android on {}. Native QUIC library will be downloaded if available.", osArch);
        } else if (!osName.contains("linux")) {
            cachedSupported = false;
        } else if (isX86_64) {
            cachedSupported = true; // CDN has x86_64 Linux builds
        } else if (isAarch64) {
            cachedSupported = false; // Will try to download
            LOGGER.info("Detected aarch64 Linux. Native QUIC library will be downloaded if available.");
        } else if (isArm || isX86) {
            cachedSupported = false;
        } else {
            cachedSupported = false;
        }
    }

    public static String getPlatformString() {
        if (cachedOsName == null) {
            computePlatform();
        }
        String osName = cachedOsName;
        String osArch = cachedOsArch;
        String osVersion = System.getProperty("os.version", "unknown");
        String javaVersion = System.getProperty("java.version", "unknown");
        String javaVendor = System.getProperty("java.vendor", "unknown");

        return String.format("%s %s (%s) | Java %s (%s)",
                osName, osVersion, osArch, javaVersion, javaVendor);
    }
}
