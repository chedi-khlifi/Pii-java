package com.mindforge.architect.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EmotionsController implements Initializable {

    // ── FXML Injects ──────────────────────────────────────────────────────────
    @FXML private ImageView cameraView;
    @FXML private Label     statusLabel;
    @FXML private Button    btnStartCamera;
    @FXML private Button    btnCapture;

    @FXML private VBox        resultBox;
    @FXML private Label       emotionLabel;
    @FXML private Label       confidenceLabel;
    @FXML private ProgressBar confidenceBar;

    @FXML private VBox              insightBox;
    @FXML private Label             insightLabel;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML private Button btnReanalyze;
    @FXML private Button btnCopyInsight;
    @FXML private Button btnSpeak;

    @FXML private VBox  historyBox;
    @FXML private Label historyLabel;

    // ── OpenCV ────────────────────────────────────────────────────────────────
    private VideoCapture             capture;
    private CascadeClassifier        faceDetector;
    private ScheduledExecutorService timer;
    private boolean                  cameraActive = false;
    private Mat                      currentFrame;
    private Rect                     lastFace;

    // ── Groq ──────────────────────────────────────────────────────────────────
    private static final String GROQ_API_KEY = resolveGroqKey();
    private static String resolveGroqKey() {
        String env = System.getenv("GROQ_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        try (java.io.InputStream is = EmotionsController.class
                .getResourceAsStream("/config.properties")) {
            if (is != null) {
                java.util.Properties p = new java.util.Properties();
                p.load(is);
                String val = p.getProperty("groq.api.key", "");
                if (!val.isBlank()) return val.trim();
            }
        } catch (Exception ignored) {}
        return "";
    }
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama-3.3-70b-versatile";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    // ── State ─────────────────────────────────────────────────────────────────
    private String lastInsightText = "";
    private String lastEmotion     = "";
    private double lastConfidence  = 0;
    private final List<Map<String, String>> emotionHistory = new ArrayList<>();

    // ── Speech process (for stop support) ────────────────────────────────────
    private Process speechProcess = null;
    private volatile boolean isSpeaking = false;
    @FXML private VBox      chatMessagesBox;
    @FXML private TextField chatInputField;
    @FXML private ScrollPane chatScrollPane;
    private final List<Map<String, String>> chatHistory = new ArrayList<>();

    // ── OpenCV load status ────────────────────────────────────────────────────
    private static final boolean OPENCV_LOADED;

    static {
        boolean loaded = false;
        try {
            // Try the system library first (works when -Djava.library.path is set)
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            loaded = true;
        } catch (UnsatisfiedLinkError e1) {
            // Fallback: try loading from the known install path on Windows
            try {
                System.load("C:\\opencv\\build\\java\\x64\\opencv_java4120.dll");
                loaded = true;
            } catch (UnsatisfiedLinkError e2) {
                // OpenCV not available — feature will be disabled gracefully
                System.err.println("[EmotionsController] OpenCV native library not found. " +
                        "Camera features will be disabled. Add -Djava.library.path=C:\\opencv\\build\\java\\x64 to VM options.");
            }
        }
        OPENCV_LOADED = loaded;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (resultBox  != null) { resultBox.setVisible(false);  resultBox.setManaged(false);  }
        if (insightBox != null) { insightBox.setVisible(false); insightBox.setManaged(false); }
        if (historyBox != null) { historyBox.setVisible(false); historyBox.setManaged(false); }
        if (btnReanalyze   != null) btnReanalyze.setDisable(true);
        if (btnCopyInsight != null) btnCopyInsight.setDisable(true);
        if (btnSpeak       != null) btnSpeak.setDisable(true);

        if (!OPENCV_LOADED) {
            if (statusLabel != null)
                statusLabel.setText("⚠️ Camera unavailable — OpenCV native library not found.\n" +
                        "Add  -Djava.library.path=C:\\opencv\\build\\java\\x64  to VM options.\n" +
                        "You can still use the manual mood buttons below.");
            if (btnStartCamera != null) btnStartCamera.setDisable(true);
            if (btnCapture     != null) btnCapture.setDisable(true);
            return;
        }

        try {
            Path cascadePath = extractCascade();
            faceDetector = new CascadeClassifier(cascadePath.toString());
            statusLabel.setText(faceDetector.empty()
                    ? "❌ Failed to load face detector"
                    : "✅ Face detector ready. Start camera to begin.");
        } catch (Exception e) {
            statusLabel.setText("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Camera
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void startCamera() {
        if (!OPENCV_LOADED) {
            statusLabel.setText("⚠️ Camera unavailable — OpenCV not loaded.");
            return;
        }
        if (!cameraActive) {
            capture = new VideoCapture(0);
            if (capture.isOpened()) {
                cameraActive = true;
                btnStartCamera.setText("⏹ Stop Camera");
                btnCapture.setDisable(false);
                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(this::grabFrame, 0, 33, TimeUnit.MILLISECONDS);
            } else {
                statusLabel.setText("❌ Cannot open webcam");
            }
        } else {
            stopCamera();
        }
    }

    private void grabFrame() {
        Mat frame = new Mat();
        if (capture == null || !capture.read(frame) || frame.empty()) return;

        currentFrame = frame.clone();

        // ── Convert to gray for detection (raw, no equalizeHist) ─────────────
        // IMPORTANT: we do NOT equalizeHist here because estimateEmotion applies
        // CLAHE internally. Double-processing destroys the regional contrast
        // differences the scoring algorithm depends on.
        Mat grayRaw = new Mat();
        Imgproc.cvtColor(frame, grayRaw, Imgproc.COLOR_BGR2GRAY);

        // For face *detection* only, use an equalized copy so Haar works well
        Mat grayForDetection = new Mat();
        Imgproc.equalizeHist(grayRaw, grayForDetection);

        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(
                grayForDetection, faces, 1.1, 3, 0, new Size(80, 80), new Size());

        Rect[] faceArray = faces.toArray();
        if (faceArray.length > 0) {
            lastFace = faceArray[0];
            Imgproc.rectangle(frame, lastFace.tl(), lastFace.br(),
                    new Scalar(0, 255, 0), 2);

            // Pass raw gray to estimateEmotion — CLAHE is applied inside
            String detected = estimateEmotion(grayRaw, lastFace);
            Imgproc.putText(frame, detected,
                    new Point(lastFace.x, lastFace.y - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 255, 0), 2);

            Platform.runLater(() ->
                    statusLabel.setText("👤 Face detected — " + detected));
        } else {
            lastFace = null;
            Platform.runLater(() -> statusLabel.setText("🔍 Looking for face..."));
        }

        Image img = matToImage(frame);
        Platform.runLater(() -> cameraView.setImage(img));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Emotion Estimation  (scoring-based, CLAHE-enhanced)
    // ═════════════════════════════════════════════════════════════════════════
    private String estimateEmotion(Mat grayRaw, Rect face) {
        // 1. Crop and resize face region
        Mat faceROI = new Mat(grayRaw, face);
        Mat resized = new Mat();
        Imgproc.resize(faceROI, resized, new Size(64, 64));

        // 2. CLAHE for local contrast enhancement (handles lighting variation)
        Mat enhanced = new Mat();
        org.opencv.imgproc.CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
        clahe.apply(resized, enhanced);

        int h = enhanced.rows(); // 64
        int w = enhanced.cols(); // 64

        // ── 3. Region means ───────────────────────────────────────────────────
        double foreheadMean = regionMean(enhanced, 0,              h / 4,             0,     w    );
        double eyeMean      = regionMean(enhanced, h / 4,          (int)(h * 0.45),   0,     w    );
        double leftEye      = regionMean(enhanced, h / 4,          (int)(h * 0.45),   0,     w / 2);
        double rightEye     = regionMean(enhanced, h / 4,          (int)(h * 0.45),   w / 2, w    );
        double noseMean     = regionMean(enhanced, (int)(h * 0.45),(int)(h * 0.60),   0,     w    );
        double philtrumMean = regionMean(enhanced, (int)(h * 0.60),(int)(h * 0.75),   0,     w    );
        double mouthMean    = regionMean(enhanced, (int)(h * 0.75),(int)(h * 0.90),   w / 4, w * 3 / 4);

        // ── 4. Derived metrics ────────────────────────────────────────────────
        double stdDev        = calculateStdDev(enhanced);
        double eyeAsymmetry  = Math.abs(leftEye - rightEye);  // anger / anxiety
        double browLift      = foreheadMean - eyeMean;         // + = raised brows (surprise)
        double mouthOpenness = mouthMean    - philtrumMean;    // + = open mouth  (happy/surprise)
        double lipTension    = philtrumMean - mouthMean;       // + = tight lips  (sad/angry)

        // ── 5. Edge densities ─────────────────────────────────────────────────
        // Mouth edges — open mouth / teeth = high density → happy / surprised
        double mouthEdgeDensity = edgeDensity(
                enhanced, (int)(h * 0.72), (int)(h * 0.92), w / 5, w * 4 / 5,
                50, 150);

        // Brow edges — furrowed brows = high density → angry / anxious
        double browEdgeDensity = edgeDensity(
                enhanced, h / 8, h / 3, 0, w,
                40, 120);

        // ── 6. Debug (remove in production) ──────────────────────────────────
        System.out.printf(
                "[emotion] std=%.1f eyeAsym=%.1f browLift=%.1f mouthOpen=%.1f " +
                        "lipTens=%.1f mouthEdge=%.1f browEdge=%.1f%n",
                stdDev, eyeAsymmetry, browLift, mouthOpenness,
                lipTension, mouthEdgeDensity, browEdgeDensity);

        // ── 7. Scoring ────────────────────────────────────────────────────────
        Map<String, Double> scores = new LinkedHashMap<>();

        // HAPPY: visible teeth/open mouth, raised cheeks, high mouth edge density
        scores.put("happy",
                score(mouthEdgeDensity > 6,  2.5) +
                        score(mouthEdgeDensity > 12, 1.5) +   // extra for wide smile
                        score(mouthOpenness > 3,     1.5) +
                        score(stdDev > 18,           0.5)
        );

        // SAD: tight/drooping lips, low contrast, brows pulled down
        scores.put("sad",
                score(lipTension > 5,        2.0) +
                        score(noseMean < eyeMean,    1.0) +
                        score(stdDev < 17,           1.5) +
                        score(browLift < -2,         1.5)
        );

        // ANGRY: furrowed brows, eye asymmetry, tight lips
        scores.put("angry",
                score(browEdgeDensity > 8,   2.5) +
                        score(eyeAsymmetry > 5,      1.5) +
                        score(lipTension > 4,        1.0) +
                        score(stdDev > 20,           0.5)
        );

        // SURPRISED: raised brows + open mouth (both required for high score)
        scores.put("surprised",
                score(browLift > 6,          2.5) +
                        score(mouthEdgeDensity > 8,  2.0) +
                        score(browEdgeDensity > 6,   1.0) +
                        score(mouthOpenness > 5,     1.0)
        );

        // ANXIOUS: eye asymmetry, moderate brow furrow, tension without full anger
        scores.put("anxious",
                score(eyeAsymmetry > 6,                      2.0) +
                        score(browEdgeDensity > 5,                   1.5) +
                        score(lipTension > 2,                        1.0) +
                        score(stdDev > 17 && stdDev < 26,            0.5)
        );

        // NEUTRAL: fixed baseline — wins only when no other emotion has strong signals
        scores.put("neutral", 1.8);

        // ── 8. Pick winner ────────────────────────────────────────────────────
        String winner = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("neutral");

        System.out.println("[emotion] scores=" + scores + " → " + winner);
        return winner;
    }

    /** Returns value if condition is true, else 0. Keeps scoring lines readable. */
    private double score(boolean condition, double value) {
        return condition ? value : 0.0;
    }

    /** Mean pixel value of a rectangular sub-region. */
    private double regionMean(Mat mat, int r1, int r2, int c1, int c2) {
        return Core.mean(mat.submat(r1, r2, c1, c2)).val[0];
    }

    /** Mean Canny edge response over a rectangular sub-region. */
    private double edgeDensity(Mat mat, int r1, int r2, int c1, int c2,
                               double threshold1, double threshold2) {
        Mat region = mat.submat(r1, r2, c1, c2);
        Mat edges  = new Mat();
        Imgproc.Canny(region, edges, threshold1, threshold2);
        return Core.mean(edges).val[0];
    }

    private double calculateStdDev(Mat mat) {
        MatOfDouble mean   = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(mat, mean, stddev);
        return stddev.get(0, 0)[0];
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Capture & Analyse
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void captureAndAnalyze() {
        if (!OPENCV_LOADED) {
            statusLabel.setText("⚠️ Camera unavailable — OpenCV not loaded.");
            return;
        }
        if (lastFace == null || currentFrame == null) {
            statusLabel.setText("❌ No face detected. Position your face in the camera.");
            return;
        }
        stopCamera();

        // Use raw gray (no equalizeHist) — same as grabFrame
        Mat grayRaw = new Mat();
        Imgproc.cvtColor(currentFrame, grayRaw, Imgproc.COLOR_BGR2GRAY);

        String emotion    = estimateEmotion(grayRaw, lastFace);
        double confidence = calculateConfidence(grayRaw, lastFace);

        lastEmotion    = emotion;
        lastConfidence = confidence;

        addToHistory(emotion, confidence);
        showResult(emotion, confidence);
        fetchGroqInsight(emotion);
    }

    private double calculateConfidence(Mat gray, Rect face) {
        double faceSizeRatio = (face.width * (double) face.height)
                / (gray.width() * (double) gray.height());
        double base          = Math.min(0.92, 0.45 + faceSizeRatio * 3.5);
        double clarity       = Math.min(0.15, calculateStdDev(new Mat(gray, face)) / 180.0);
        return Math.min(0.96, base + clarity);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Result display
    // ═════════════════════════════════════════════════════════════════════════
    private void showResult(String emotion, double confidence) {
        Platform.runLater(() -> {
            resultBox.setVisible(true);
            resultBox.setManaged(true);

            emotionLabel.setText(emotion.toUpperCase());
            confidenceLabel.setText(String.format("Confidence: %.1f%%", confidence * 100));
            confidenceBar.setProgress(confidence);

            String color = emotionColor(emotion);
            emotionLabel.setStyle(
                    "-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            confidenceBar.setStyle("-fx-accent: " + color + "; -fx-pref-height: 8px;");
        });
    }

    private String emotionColor(String emotion) {
        return switch (emotion) {
            case "happy"     -> "#2E7D32";
            case "sad"       -> "#1565C0";
            case "angry"     -> "#C62828";
            case "surprised" -> "#F57F17";
            case "anxious"   -> "#E65100";
            default          -> "#757575";
        };
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Groq API — Insight
    // ═════════════════════════════════════════════════════════════════════════
    private void fetchGroqInsight(String emotion) {
        Platform.runLater(() -> {
            insightBox.setVisible(true);
            insightBox.setManaged(true);
            if (loadingIndicator != null) loadingIndicator.setVisible(true);
            insightLabel.setText("✨ Generating your personalised mental health insight...");
            if (btnReanalyze   != null) btnReanalyze.setDisable(true);
            if (btnCopyInsight != null) btnCopyInsight.setDisable(true);
            if (btnSpeak       != null) btnSpeak.setDisable(true);
        });

        String systemPrompt =
            "You are a compassionate, evidence-based mental health companion. " +
            "Your role is to provide warm, non-judgmental, actionable support. " +
            "Never diagnose. Always encourage professional help for serious concerns. " +
            "Keep each section concise — 1-2 sentences max.";

        String userPrompt =
            "The user's current emotional state is: " + emotion.toUpperCase() + ".\n\n" +
            "Provide a personalised mental health insight using EXACTLY this format " +
            "(no markdown, no asterisks, no extra lines):\n\n" +
            "VALIDATION: <One warm sentence acknowledging and normalising their feeling>\n" +
            "INSIGHT: <One sentence explaining what this emotion often signals psychologically>\n" +
            "BREATHE: <A specific breathing technique with counts, e.g. 4-7-8 method>\n" +
            "REFRAME: <A cognitive reframing thought to shift perspective>\n" +
            "ACTION: <One immediate grounding action they can do right now>\n" +
            "SONG: <A song title and artist that matches or soothes this mood>\n" +
            "QUOTE: <An uplifting quote with author name>\n" +
            "JOURNAL: <A reflective journaling question to explore this emotion deeper>\n" +
            "REMINDER: <A short self-compassion reminder sentence>";

        JSONObject body = new JSONObject();
        body.put("model", GROQ_MODEL);
        body.put("max_tokens", 600);
        body.put("temperature", 0.7);
        body.put("messages", new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", systemPrompt))
                .put(new JSONObject().put("role", "user").put("content", userPrompt)));

        String bodyStr = body.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type",  "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .header("Accept",        "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyStr, StandardCharsets.UTF_8))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> Platform.runLater(() -> handleGroqResponse(r)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (loadingIndicator != null) loadingIndicator.setVisible(false);
                        insightLabel.setText("❌ Network error: " + ex.getCause().getMessage());
                    });
                    return null;
                });
    }

    private void handleGroqResponse(HttpResponse<String> response) {
        if (loadingIndicator != null) loadingIndicator.setVisible(false);

        if (response.statusCode() == 200) {
            try {
                String content = new JSONObject(response.body())
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                lastInsightText = formatInsight(content);
                insightLabel.setText(lastInsightText);

                if (btnReanalyze   != null) btnReanalyze.setDisable(false);
                if (btnCopyInsight != null) btnCopyInsight.setDisable(false);
                if (btnSpeak       != null) btnSpeak.setDisable(false);

            } catch (Exception e) {
                insightLabel.setText("❌ Parse error: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            String raw = response.body();
            String detail = "";
            try { detail = new JSONObject(raw).optString("error", raw); } catch (Exception ignored) { detail = raw; }
            insightLabel.setText("❌ Groq error " + response.statusCode() + ": " + detail);
            System.err.println("[Groq] HTTP " + response.statusCode() + " — " + raw);
        }
    }

    private String formatInsight(String raw) {
        Map<String, String> prefixes = new LinkedHashMap<>();
        prefixes.put("VALIDATION", "💙");
        prefixes.put("INSIGHT",    "🔍");
        prefixes.put("BREATHE",    "🌬️");
        prefixes.put("REFRAME",    "🔄");
        prefixes.put("ACTION",     "⚡");
        prefixes.put("SONG",       "🎵");
        prefixes.put("QUOTE",      "📖");
        prefixes.put("JOURNAL",    "📓");
        prefixes.put("REMINDER",   "🌟");

        StringBuilder out = new StringBuilder();
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            boolean matched = false;
            for (Map.Entry<String, String> e : prefixes.entrySet()) {
                String key = e.getKey() + ":";
                if (line.toUpperCase().startsWith(key)) {
                    out.append(e.getValue()).append("  ")
                       .append(line.substring(key.length()).trim())
                       .append("\n\n");
                    matched = true;
                    break;
                }
            }
            if (!matched) out.append(line).append("\n");
        }
        return out.toString().trim();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Mental Health Chat
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void sendChatMessage() {
        if (chatInputField == null || chatMessagesBox == null) return;
        String userText = chatInputField.getText().trim();
        if (userText.isEmpty()) return;
        chatInputField.clear();

        // Add user bubble
        appendChatBubble(userText, true);

        // Add to history
        Map<String, String> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userText);
        chatHistory.add(userMsg);

        // Show typing indicator
        Label typing = new Label("💬 Thinking...");
        typing.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px; -fx-padding: 4 12;");
        chatMessagesBox.getChildren().add(typing);
        scrollChatToBottom();

        // Build messages array with system prompt + history
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content",
                    "You are a warm, empathetic mental health companion named MindForge AI. " +
                    "You provide supportive, evidence-based emotional support. " +
                    "You listen actively, validate feelings, and offer practical coping strategies. " +
                    "You never diagnose mental illness. For serious concerns (self-harm, crisis), " +
                    "you always recommend professional help and provide crisis resources. " +
                    "Keep responses concise (2-4 sentences), warm, and conversational. " +
                    "Current user emotion context: " + (lastEmotion.isEmpty() ? "unknown" : lastEmotion) + "."));

        for (Map<String, String> msg : chatHistory) {
            messages.put(new JSONObject()
                    .put("role", msg.get("role"))
                    .put("content", msg.get("content")));
        }

        JSONObject body = new JSONObject();
        body.put("model", GROQ_MODEL);
        body.put("max_tokens", 300);
        body.put("temperature", 0.8);
        body.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type",  "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .header("Accept",        "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> Platform.runLater(() -> {
                    chatMessagesBox.getChildren().remove(typing);
                    if (r.statusCode() == 200) {
                        try {
                            String reply = new JSONObject(r.body())
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");
                            appendChatBubble(reply, false);
                            Map<String, String> assistantMsg = new LinkedHashMap<>();
                            assistantMsg.put("role", "assistant");
                            assistantMsg.put("content", reply);
                            chatHistory.add(assistantMsg);
                            // Keep history to last 20 messages to avoid token overflow
                            if (chatHistory.size() > 20) chatHistory.remove(0);
                        } catch (Exception e) {
                            appendChatBubble("Sorry, I had trouble understanding that. Please try again.", false);
                        }
                    } else {
                        appendChatBubble("I'm having trouble connecting right now. Please try again in a moment.", false);
                        System.err.println("[Chat] HTTP " + r.statusCode() + " — " + r.body());
                    }
                    scrollChatToBottom();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        chatMessagesBox.getChildren().remove(typing);
                        appendChatBubble("Connection error. Please check your internet and try again.", false);
                    });
                    return null;
                });
    }

    private void appendChatBubble(String text, boolean isUser) {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(380);
        bubble.setPadding(new Insets(10, 14, 10, 14));

        if (isUser) {
            bubble.setStyle(
                "-fx-background-color: #534AB7; -fx-text-fill: white;" +
                "-fx-background-radius: 16 16 2 16; -fx-font-size: 13px;");
        } else {
            bubble.setStyle(
                "-fx-background-color: white; -fx-text-fill: #333333;" +
                "-fx-background-radius: 16 16 16 2; -fx-font-size: 13px;" +
                "-fx-border-color: #e8e8e8; -fx-border-width: 0.5; -fx-border-radius: 16 16 16 2;");
        }

        row.getChildren().add(bubble);
        chatMessagesBox.getChildren().add(row);
    }

    private void scrollChatToBottom() {
        if (chatScrollPane != null) {
            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Feature buttons
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void reanalyze() {
        if (!lastEmotion.isEmpty()) {
            fetchGroqInsight(lastEmotion);
        } else {
            statusLabel.setText("⚠️ No emotion detected yet. Capture first.");
        }
    }

    @FXML
    private void copyInsight() {
        if (lastInsightText.isEmpty()) return;
        StringSelection sel = new StringSelection(lastInsightText);
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(sel, null);
        statusLabel.setText("📋 Insight copied to clipboard!");
    }

    @FXML
    private void speakInsight() {
        // If already speaking — stop it
        if (isSpeaking) {
            stopSpeech();
            return;
        }

        if (lastInsightText.isEmpty()) return;

        String os        = System.getProperty("os.name").toLowerCase();
        String cleanText = lastInsightText
                .replaceAll("[^\\x00-\\x7F]", "")
                .replaceAll("\n+", ". ")
                .trim();

        isSpeaking = true;
        if (btnSpeak != null) {
            Platform.runLater(() -> {
                btnSpeak.setText("⏹ Stop");
                btnSpeak.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #B71C1C;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
            });
        }
        statusLabel.setText("🔊 Speaking insight...");

        new Thread(() -> {
            try {
                ProcessBuilder pb;
                if (os.contains("win")) {
                    String psCmd = String.format(
                            "Add-Type -AssemblyName System.Speech; " +
                            "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                            "$s.Speak('%s');",
                            cleanText.replace("'", "").replace("\"", ""));
                    pb = new ProcessBuilder("powershell", "-NoProfile", "-Command", psCmd);
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("say", cleanText);
                } else {
                    pb = new ProcessBuilder("espeak", cleanText);
                }
                pb.redirectErrorStream(true);
                speechProcess = pb.start();
                speechProcess.waitFor();
            } catch (InterruptedException ignored) {
                // stopped intentionally
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("⚠️ TTS error: " + e.getMessage()));
            } finally {
                isSpeaking = false;
                speechProcess = null;
                Platform.runLater(() -> {
                    statusLabel.setText("✅ Done speaking.");
                    if (btnSpeak != null) {
                        btnSpeak.setText("🔊 Read Aloud");
                        btnSpeak.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100;" +
                                "-fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
                    }
                });
            }
        }, "tts-thread").start();
    }

    private void stopSpeech() {
        isSpeaking = false;
        if (speechProcess != null) {
            speechProcess.destroyForcibly();
            speechProcess = null;
        }
        // On Windows, also kill any lingering powershell TTS processes
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            try {
                new ProcessBuilder("powershell", "-NoProfile", "-Command",
                        "Get-Process -Name powershell -ErrorAction SilentlyContinue | " +
                        "Where-Object { $_.MainWindowTitle -eq '' } | Stop-Process -Force")
                        .start();
            } catch (Exception ignored) {}
        }
        Platform.runLater(() -> {
            statusLabel.setText("⏹ Stopped.");
            if (btnSpeak != null) {
                btnSpeak.setText("🔊 Read Aloud");
                btnSpeak.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Emotion History
    // ═════════════════════════════════════════════════════════════════════════
    private void addToHistory(String emotion, double confidence) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("emotion",    emotion);
        entry.put("confidence", String.format("%.0f%%", confidence * 100));
        entry.put("time",       LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        emotionHistory.add(0, entry);
        if (emotionHistory.size() > 10) emotionHistory.remove(emotionHistory.size() - 1);
        Platform.runLater(this::renderHistory);
    }

    private void renderHistory() {
        if (historyBox == null || historyLabel == null) return;
        if (emotionHistory.isEmpty()) {
            historyBox.setVisible(false);
            historyBox.setManaged(false);
            return;
        }
        historyBox.setVisible(true);
        historyBox.setManaged(true);

        StringBuilder sb = new StringBuilder();
        for (Map<String, String> entry : emotionHistory) {
            String emo  = entry.get("emotion");
            String conf = entry.get("confidence");
            String time = entry.get("time");
            sb.append(String.format("%s %-10s  %s  %s  %s\n",
                    emotionEmoji(emo), emo.toUpperCase(),
                    conf, buildBar(conf), time));
        }
        historyLabel.setText(sb.toString().trim());
        historyLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
    }

    private String buildBar(String confidenceStr) {
        int pct    = Integer.parseInt(confidenceStr.replace("%", ""));
        int filled = pct / 10;
        return "█".repeat(filled) + "░".repeat(10 - filled);
    }

    private String emotionEmoji(String emotion) {
        return switch (emotion) {
            case "happy"     -> "😊";
            case "sad"       -> "😢";
            case "angry"     -> "😠";
            case "surprised" -> "😲";
            case "anxious"   -> "😰";
            default          -> "😐";
        };
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Manual mood buttons
    // ═════════════════════════════════════════════════════════════════════════
    @FXML private void setMoodHappy()     { manualMood("happy",     0.85); }
    @FXML private void setMoodNeutral()   { manualMood("neutral",   0.70); }
    @FXML private void setMoodSad()       { manualMood("sad",       0.80); }
    @FXML private void setMoodAngry()     { manualMood("angry",     0.75); }
    @FXML private void setMoodAnxious()   { manualMood("anxious",   0.78); }
    @FXML private void setMoodSurprised() { manualMood("surprised", 0.80); }

    private void manualMood(String emotion, double confidence) {
        stopCamera();
        lastEmotion    = emotion;
        lastConfidence = confidence;
        addToHistory(emotion, confidence);
        showResult(emotion, confidence);
        fetchGroqInsight(emotion);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Navigation
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void goBack() {
        stopCamera();
        stopSpeech();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/mindforge/fxml/profile.fxml"));
            Parent root  = loader.load();
            Scene  scene = new Scene(root, 800, 640);

            URL css = getClass().getResource("/com/mindforge/css/style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setTitle("MindForge - Profile");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════════════
    private void stopCamera() {
        cameraActive = false;
        if (timer != null) {
            timer.shutdown();
            try {
                if (!timer.awaitTermination(100, TimeUnit.MILLISECONDS))
                    timer.shutdownNow();
            } catch (InterruptedException e) {
                timer.shutdownNow();
            }
        }
        if (capture != null && capture.isOpened()) capture.release();
        Platform.runLater(() -> {
            btnStartCamera.setText("▶ Start Camera");
            btnCapture.setDisable(true);
            cameraView.setImage(null);
        });
    }

    private Image matToImage(Mat mat) {
        int w = mat.width(), h = mat.height(), ch = mat.channels();
        WritableImage image  = new WritableImage(w, h);
        javafx.scene.image.PixelWriter writer = image.getPixelWriter();
        byte[] data = new byte[w * h * ch];
        mat.get(0, 0, data);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = (y * w + x) * ch;
                writer.setColor(x, y, Color.rgb(
                        data[i + 2] & 0xFF,
                        data[i + 1] & 0xFF,
                        data[i]     & 0xFF));
            }
        }
        return image;
    }

    private Path extractCascade() throws Exception {
        String name = "haarcascade_frontalface_default.xml";
        try (var is = getClass().getResourceAsStream("/" + name)) {
            if (is == null) throw new RuntimeException(
                    "Cascade not found. Place it at src/main/resources/" + name);
            Path tmp = Files.createTempFile("cascade", ".xml");
            Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();
            return tmp;
        }
    }
}