package com.vogmod.audio;

import com.vogmod.Vog;
import com.vogmod.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AudioPlayer {

    private final Vog plugin;
    private final ConfigManager config;
    private final Path tempDir;
    private final Map<UUID, Integer> playerEchoCount = new HashMap<>();
    private String currentPackUrl;

    public AudioPlayer(Vog plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        
        try {
            this.tempDir = Files.createTempDirectory("vog");
        } catch (IOException e) {
            throw new RuntimeException("Could not create temp directory", e);
        }
    }

    public void playTts(String text, Runnable onComplete) {
        CompletableFuture.runAsync(() -> {
            Path packDir = tempDir.resolve("pack");
            Path oggFile = packDir.resolve("assets/vog/sounds/tts.ogg");
            
            try {
                Files.createDirectories(packDir.resolve("assets/vog/sounds"));
                
                // Generate TTS (now outputs OGG)
                boolean generated = generateTts(text, oggFile);
                
                if (!generated) {
                    plugin.getLogger().severe("TTS generation failed!");
                    if (onComplete != null) plugin.getServer().getScheduler().runTask(plugin, onComplete);
                    return;
                }

                plugin.getLogger().info("Creating resource pack...");
                
                // Create pack
                Path packZip = createPack(packDir);
                
                // Upload to temp host or use local server
                String packUrl = uploadPack(packZip);
                
                if (packUrl == null) {
                    plugin.getLogger().severe("Failed to host resource pack!");
                    if (onComplete != null) plugin.getServer().getScheduler().runTask(plugin, onComplete);
                    return;
                }

                setCurrentPackUrl(packUrl);
                
                // Send pack and play
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sendResourcePackAndPlay();
                    if (onComplete != null) onComplete.run();
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("TTS error: " + e.getMessage());
                e.printStackTrace();
                if (onComplete != null) plugin.getServer().getScheduler().runTask(plugin, onComplete);
            }
        });
    }

    private boolean generateTts(String text, Path outputFile) {
        HttpURLConnection conn = null;
        try {
            text = text.replaceAll("[^a-zA-Z0-9\\s.,!?']", "");
            if (text.isEmpty()) {
                plugin.getLogger().warning("TTS text empty after sanitization");
                return false;
            }
            
            plugin.getLogger().info("Generating TTS for: " + text);
            
            // Try multiple TTS services
            String[][] ttsUrls = {
                // Google Translate TTS
                {"https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&q=PLACEHOLDER&tl=en", "google"},
                // TTS API (free tier)
                {"https://api.ttsmatt.repl.co/tts?text=PLACEHOLDER&voice=en", "ttsmatt"},
                // Responsive Voice
                {"https://code.responsivevoice.org/getvoice.php?v=UK+EnglishMale&t=PLACEHOLDER", "responsivevoice"}
            };
            
            boolean success = false;
            for (String[] ttsConfig : ttsUrls) {
                String urlTemplate = ttsConfig[0];
                String serviceName = ttsConfig[1];
                
                String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
                String urlStr = urlTemplate.replace("PLACEHOLDER", encoded);
                
                plugin.getLogger().info("Trying TTS service: " + serviceName);
                
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                
                plugin.getLogger().info("TTS response code: " + conn.getResponseCode());
                
                if (conn.getResponseCode() != 200) {
                    plugin.getLogger().warning("TTS (" + serviceName + ") failed with code: " + conn.getResponseCode());
                    conn.disconnect();
                    continue;
                }
                
                int contentLength = conn.getContentLength();
                plugin.getLogger().info("TTS content length: " + contentLength);
                
                if (contentLength > 0 && contentLength < 1000) {
                    plugin.getLogger().warning("TTS (" + serviceName + ") response too small, likely error page");
                    conn.disconnect();
                    continue;
                }
                
                // Download as MP3 first
                Path mp3File = tempDir.resolve("tts_input.mp3");
                try (InputStream is = conn.getInputStream();
                     OutputStream os = Files.newOutputStream(mp3File)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
                
                plugin.getLogger().info("Downloaded MP3, size: " + Files.size(mp3File));
                
                // Convert MP3 to OGG using FFmpeg
                success = convertToOgg(mp3File, outputFile);
                
                if (!success) {
                    plugin.getLogger().warning("FFmpeg conversion failed for " + serviceName);
                    conn.disconnect();
                } else {
                    plugin.getLogger().info("TTS (" + serviceName + ") success!");
                    break;
                }
            }
            
            return success;
            
        } catch (Exception e) {
            plugin.getLogger().warning("TTS error: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    
    private boolean convertToOgg(Path mp3File, Path oggFile) {
        // Find FFmpeg
        String[] ffmpegPaths = {
            "ffmpeg",
            "/usr/bin/ffmpeg",
            "/usr/local/bin/ffmpeg",
            "/opt/homebrew/bin/ffmpeg"
        };
        
        String ffmpegCmd = null;
        for (String path : ffmpegPaths) {
            if (Files.exists(Path.of(path)) || path.equals("ffmpeg")) {
                // Check if it's callable by trying a simple test
                ffmpegCmd = path;
                break;
            }
        }
        
        if (ffmpegCmd == null) {
            plugin.getLogger().warning("FFmpeg not found - checking PATH...");
            // Try to find in PATH
            try {
                ProcessBuilder pb = new ProcessBuilder("which", "ffmpeg");
                Process p = pb.start();
                if (p.waitFor() == 0) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String line = br.readLine();
                        if (line != null && !line.isEmpty()) {
                            ffmpegCmd = line.trim();
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }
        
        if (ffmpegCmd == null) {
            plugin.getLogger().severe("FFmpeg not installed! Sound will not work. Install with: sudo pacman -S ffmpeg");
            return false;
        }
        
        plugin.getLogger().info("Converting to OGG with: " + ffmpegCmd);
        
        try {
            // Convert MP3 to OGG Vorbis
            ProcessBuilder pb = new ProcessBuilder(
                ffmpegCmd,
                "-i", mp3File.toString(),
                "-acodec", "libvorbis",
                "-q:a", "4",
                "-y",
                oggFile.toString()
            );
            pb.redirectErrorStream(true);
            
            Process p = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = p.waitFor();
            plugin.getLogger().info("FFmpeg exit code: " + exitCode);
            
            if (exitCode == 0 && Files.exists(oggFile) && Files.size(oggFile) > 1000) {
                plugin.getLogger().info("OGG conversion success, size: " + Files.size(oggFile));
                return true;
            } else {
                plugin.getLogger().warning("FFmpeg conversion failed. Output:\n" + output);
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("FFmpeg error: " + e.getMessage());
            return false;
        }
    }
    private Path createPack(Path packDir) throws IOException {
        Path packMcmeta = packDir.resolve("pack.mcmeta");
        // pack_format: 34 = 1.21.1-1.21.3, 45 = 1.21.4+
        String mcmeta = """
            {
                "pack": {
                    "pack_format": 45,
                    "description": "Vog - Voice of God TTS"
                }
            }
            """;
        Files.writeString(packMcmeta, mcmeta);
        
        // Create sounds.json to register the sound
        Path soundsJson = packDir.resolve("assets/vog/sounds.json");
        String sounds = """
            {
                "tts": {
                    "sounds": [
                        {
                            "name": "vog/tts",
                            "stream": true
                        }
                    ]
                }
            }
            """;
        Files.writeString(soundsJson, sounds);
        
        Path packZip = tempDir.resolve("vog_pack.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(packZip))) {
            addFileToZip(zos, packDir.resolve("pack.mcmeta"), "pack.mcmeta");
            addFileToZip(zos, packDir.resolve("assets/vog/sounds.json"), "assets/vog/sounds.json");
            addFileToZip(zos, packDir.resolve("assets/vog/sounds/tts.ogg"), 
                "assets/vog/sounds/tts.ogg");
        }
        
        return packZip;
    }

    private void addFileToZip(ZipOutputStream zos, Path file, String name) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        Files.copy(file, zos);
        zos.closeEntry();
    }

    private String uploadPack(Path packZip) {
        plugin.getLogger().info("Attempting to upload resource pack...");
        
        // Try file.io - simple temp file hosting
        try {
            String uploadUrl = "https://file.io";
            
            String boundary = Long.toHexString(System.currentTimeMillis());
            HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write("Content-Disposition: form-data; name=\"file\"; filename=\"vog.zip\"\r\n".getBytes());
                os.write("Content-Type: application/zip\r\n\r\n".getBytes());
                Files.copy(packZip, os);
                os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            }
            
            int responseCode = conn.getResponseCode();
            plugin.getLogger().info("file.io response code: " + responseCode);
            
            if (responseCode == 200) {
                // Parse JSON response to get URL
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    
                    String resp = response.toString();
                    plugin.getLogger().info("file.io response: " + resp.substring(0, Math.min(200, resp.length())));
                    
                    // Check if response is HTML (error page) instead of JSON
                    if (resp.trim().startsWith("<")) {
                        plugin.getLogger().warning("file.io returned HTML (likely error), falling back to local");
                    } else {
                        // Extract "link" from JSON: {"success":true,"link":"https://..."}
                        int linkStart = resp.indexOf("\"link\":\"") + 8;
                        int linkEnd = resp.indexOf("\"", linkStart);
                        if (linkStart > 7 && linkEnd > linkStart) {
                            String url = resp.substring(linkStart, linkEnd);
                            plugin.getLogger().info("file.io upload success: " + url);
                            return url;
                        }
                    }
                }
            }
            
            plugin.getLogger().warning("file.io upload failed, response: " + responseCode);
            
        } catch (Exception e) {
            plugin.getLogger().warning("file.io upload error: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Fallback: use local server approach
        plugin.getLogger().info("Falling back to local server...");
        return serveLocally(packZip);
    }

    private String serveLocally(Path packZip) {
        // For local serving, we need the server's accessible IP
        // This works for LAN players or if you have port forwarding
        
        try {
            // Check if we should try to detect public IP
            String configuredIp = plugin.getConfig().getString("server-ip", "auto");
            
            String serverIp;
            if (!configuredIp.equals("auto")) {
                serverIp = configuredIp;
            } else {
                // Check server's bind address - if bound to 127.0.0.1, use localhost
                String serverBind = plugin.getServer().getIp();
                if (serverBind != null && !serverBind.isEmpty() && serverBind.equals("127.0.0.1")) {
                    serverIp = "127.0.0.1";
                    plugin.getLogger().info("Server bound to localhost, using 127.0.0.1 for resource pack");
                } else {
                    // Try to get public IP from api.ipify.org
                    try {
                        URL whatIsMyIp = new URL("https://api.ipify.org");
                        HttpURLConnection conn = (HttpURLConnection) whatIsMyIp.openConnection();
                        conn.setConnectTimeout(5000);
                        try (BufferedReader br = new BufferedReader(
                                new InputStreamReader(conn.getInputStream()))) {
                            serverIp = br.readLine().trim();
                            plugin.getLogger().info("Detected public IP: " + serverIp);
                        }
                    } catch (Exception e) {
                        // Fallback to local IP
                        try (DatagramSocket socket = new DatagramSocket()) {
                            socket.connect(new InetSocketAddress("8.8.8.8", 80));
                            serverIp = socket.getLocalAddress().getHostAddress();
                            plugin.getLogger().warning("Could not detect public IP, using local: " + serverIp);
                        }
                    }
                }
            }
            
            // Use a simple embedded HTTP server on a random port
            return startTempServer(packZip, serverIp);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Local serve error: " + e.getMessage());
            return null;
        }
    }

    private String startTempServer(Path packZip, String serverIp) {
        try {
            // Find available port
            ServerSocket ss = new ServerSocket(0);
            int port = ss.getLocalPort();
            ss.close();
            
            // Copy pack to a known location for serving
            Path servePath = Paths.get(plugin.getDataFolder().getParent(), "vog_serve.zip");
            Files.copy(packZip, servePath, StandardCopyOption.REPLACE_EXISTING);
            
            // The URL players will use (use detected/configured IP)
            String url = "http://" + serverIp + ":" + port + "/vog_serve.zip";
            
            // Use CountDownLatch to signal when server is ready
            CountDownLatch serverReady = new CountDownLatch(1);
            
            // Start a simple server in a thread
            // Bind to 0.0.0.0 to accept connections from any interface
            Thread serverThread = new Thread(() -> {
                try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))) {
                    server.setSoTimeout(300000); // 5 min timeout
                    serverReady.countDown(); // Signal we're listening
                    
                    while (!Thread.currentThread().isInterrupted()) {
                        try (Socket client = server.accept();
                             InputStream is = client.getInputStream();
                             OutputStream os = client.getOutputStream()) {
                            
                            // Simple HTTP response
                            byte[] data = Files.readAllBytes(servePath);
                            String response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/zip\r\n" +
                                "Content-Length: " + data.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n";
                            
                            os.write(response.getBytes());
                            os.write(data);
                            os.flush();
                        } catch (SocketTimeoutException e) {
                            break;
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("HTTP server error: " + e.getMessage());
                }
            });
            
            serverThread.setDaemon(true);
            serverThread.start();
            
            // Wait for server to be ready (max 5 seconds)
            boolean ready = serverReady.await(5, TimeUnit.SECONDS);
            if (!ready) {
                plugin.getLogger().warning("HTTP server may not be ready");
            }
            
            plugin.getLogger().info("Serving pack at: " + url);
            return url;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start temp server: " + e.getMessage());
            return null;
        }
    }

    private void sendResourcePackAndPlay() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) return;

        String packUrl = currentPackUrl;
        plugin.getLogger().info("Sending resource pack to " + players.size() + " players: " + packUrl);
        
        for (Player player : players) {
            player.setResourcePack(packUrl);
            plugin.getLogger().info("Sent pack to: " + player.getName() + " - waiting for pack to load...");
        }

        // DON'T play immediately - wait for resource pack to load via event
        plugin.getLogger().info("Waiting for resource pack to load (will play when loaded event fires)...");
    }
    
    public void onPackLoaded(Player player) {
        plugin.getLogger().info("Pack loaded event received for " + player.getName() + " - playing sound now!");
        playGodSoundForPlayer(player);
    }
    
    private void playGodSoundForPlayer(Player player) {
        Location playLocation = player.getLocation();
        String soundName = "vog:tts";
        
        plugin.getLogger().info("Playing sound for " + player.getName() + " at player location");
        
        // First, test with a vanilla sound that definitely works
        plugin.getLogger().info("TEST: Playing vanilla bell sound first...");
        player.playSound(playLocation, "entity.wither.break", 1.0f, 1.0f);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("TEST: Now playing custom sound...");
            player.playSound(playLocation, soundName, 1.0f, 1.0f);
        }, 40L);
        
        int echoCount = config.getEchoCount();
        int echoDelay = config.getEchoDelayMs();
        double volumeDecay = config.getEchoVolumeDecay();
        
        // Echo effects for vanilla sound
        for (int i = 1; i <= echoCount; i++) {
            final int echoNum = i;
            double volume = config.getVolume() * Math.pow(volumeDecay, i);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.playSound(playLocation, "entity.wither.break", (float) volume, 1.0f);
            }, (echoDelay * echoNum) / 50L + 40L);
        }
    }


    private void setCurrentPackUrl(String url) {
        this.currentPackUrl = url;
    }

    private void playGodSound() {
        // Use a location in the sky that's within world bounds
        // 1.21 world height is 384, center sky is around y=320
        Location skyLocation = new Location(Bukkit.getWorlds().get(0), 0, 320, 0);
        String soundName = "vog:tts";
        
        if (config.isDebugEnabled()) {
            plugin.getLogger().info("Attempting to play sound: " + soundName + " at " + skyLocation);
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (config.isDebugEnabled()) {
                plugin.getLogger().info("Playing sound for: " + player.getName());
            }
            
            // First, test with a vanilla sound to confirm audio works
            if (config.shouldTestVanillaSound()) {
                plugin.getLogger().info("TESTING: Playing vanilla sound 'entity.lightning.impact' first...");
                player.playSound(player.getLocation(), "entity.lightning.impact", 1.0f, 1.0f);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getLogger().info("TESTING: Now playing custom sound...");
                    player.playSound(player.getLocation(), soundName, 1.0f, 1.0f);
                    if (config.isDebugEnabled()) {
                        plugin.getLogger().info("Sound played for " + player.getName());
                    }
                }, 20L); // 1 second delay
            } else {
                player.playSound(skyLocation, soundName, 1.0f, 1.0f);
                if (config.isDebugEnabled()) {
                    plugin.getLogger().info("Sound played for " + player.getName());
                }
            }

            int echoCount = config.getEchoCount();
            int echoDelay = config.getEchoDelayMs();
            double volumeDecay = config.getEchoVolumeDecay();

            for (int i = 1; i <= echoCount; i++) {
                final int echoNum = i;
                double volume = config.getVolume() * Math.pow(volumeDecay, i);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.playSound(skyLocation, soundName, (float) volume, 1.0f);
                }, (echoDelay * echoNum) / 50L);
            }
        }

        plugin.getLogger().info("TTS sound sequence complete!");
    }

    public void shutdown() {
        // Cleanup if needed
    }
}
