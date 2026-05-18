package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.example.agents.AppConfig;
import org.example.agents.NegotiationConfig;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

// Builds the JavaFX dashboard and launches JADE agents for the car negotiation demo.
public class MainUI extends Application {
    private TextArea logArea = new TextArea();
    private Label buyerCountLabel = new Label("0");
    private Label dealerCountLabel = new Label("0");
    private Label transactionCountLabel = new Label("0");
    private Label failedDealsCountLabel = new Label("0");
    private Label revenueLabel = new Label("RM 0.00");
    private ContainerController cc;
    private UILogger appLogger;
    private int buyerCount = 0;
    private int dealerCount = 0;
    private int dealsClosed = 0;
    private int failedDealsCount = 0;
    private double totalRevenue = 0;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private Label dealerStatusLabel = new Label();
    private Label updateBuyerStatus = new Label();
    private int currentCycle = 0;
    private final Map<String, SessionMeta> sessionMetaMap = new HashMap<>();
    private final Map<String, List<TrajectoryPoint>> sessionPoints = new HashMap<>();
    private final Map<String, List<TrajectoryPoint>> agentPoints = new HashMap<>();
    private final Map<String, Double> sessionLastPrice = new HashMap<>();
    private final Map<String, ListingViewModel> listingModelMap = new LinkedHashMap<>();
    private VisualiserView activeVisualiserView = VisualiserView.MARKET;
    private final Map<VisualiserView, Button> visualiserButtons = new HashMap<>();
    private StackPane visualiserContentPane;
    private VBox marketVisualiserPane;
    private VBox sessionVisualiserPane;
    private VBox agentVisualiserPane;
    private ScrollPane marketVisualiserScroll;
    private ScrollPane sessionVisualiserScroll;
    private ScrollPane agentVisualiserScroll;
    private ComboBox<String> visualiserSessionSelect;
    private ComboBox<String> visualiserAgentTypeSelect;
    private Button playPauseBtn;
    private boolean isAutoPlay = true;
    private ComboBox<String> strategyChoice;
    private ComboBox<String> switchStrategyChoice;
    private TextField deadlineCyclesField;
    private TextField buyerStartPercentField;
    private TextField reservePercentField;
    private TextField maxRoundsField;
    private TextField retryLimitField;
    private TextField stuckRoundsField;
    private TextField strategySwitchCycleField;
    private TextField manualDealerNameField;
    private TextField manualDealerPriceField;
    private Label negotiationControlStatusLabel = new Label("No waiting buyers");
    private List<String> waitingBuyerAgents = new ArrayList<>();
    private List<String> buyerAgents = new ArrayList<>();
    private List<String> dealerAgents = new ArrayList<>();
    private javafx.collections.ObservableList<String> manualBuyerAgents = javafx.collections.FXCollections
            .observableArrayList();
    private ComboBox<String> manualBuyerSelect;
    private TextArea manualLogArea;
    private ComboBox<String> manualDealerSelect;
    private TextField manualFirstOfferField;
    private Button manualSendFirstOfferBtn;
    private TextField manualCounterPriceField;
    private Button manualSendCounterBtn;
    private Button manualAcceptDealBtn;
    private Button manualWalkAwayBtn;
    private final Map<String, Integer> failureReasonCounts = new LinkedHashMap<>();
    private TextArea failureReportArea = new TextArea();
    private TextArea failuresArea = new TextArea();
    private TextArea sessionsArea = new TextArea();
    private TextArea dashboardEventsArea;
    private Label activeSessionsLabel = new Label("0");
    private Label activeSessionsLabelMini = new Label("0");
    private Label fixedFeesLabel = new Label("RM 0");
    private Label fixedFeesLabelMini = new Label("RM 0");
    private Label commissionLabel = new Label("RM 0");
    private Label commissionLabelMini = new Label("RM 0");
    private double totalFixedFees = 0;
    private double totalCommission = 0;
    private int activeSessions = 0;
    private final AtomicLong commandAgentCounter = new AtomicLong();
    private final AtomicLong demoScenarioCounter = new AtomicLong();
    private final AppConfig appConfig = AppConfig.defaults();
    private StackPane workspacePane;
    private final Map<String, Button> navigationButtons = new HashMap<>();
    private static final Pattern RM_AMOUNT_PATTERN = Pattern.compile("RM\\s*(\\d+)");

    // Bright academic demo palette
    private static final String PRIMARY_BLUE = "#1e3a8a";
    private static final String ACCENT_BLUE = "#2563eb";
    private static final String SUCCESS_GREEN = "#16a34a";
    private static final String WARNING_ORANGE = "#f59e0b";
    private static final String ERROR_RED = "#e11d48";
    private static final String LIGHT_GRAY = "#eff6ff";
    private static final String DARK_TEXT = "#111827";
    private static final String TEXT_MUTED = "#475569";
    private static final String SURFACE = "#ffffff";
    private static final String SURFACE_ALT = "#f8fafc";
    private static final String BORDER_SUBTLE = "#bfdbfe";
    private static final String FONT_FAMILY = "'Poppins', 'Segoe UI', Arial";
    private static final String FONT_WEIGHT_MEDIUM = "500";
    private static final String CARD_SHADOW = "dropshadow(gaussian, rgba(15,23,42,0.10), 18, 0, 0, 6)";
    private static final String PANEL_STYLE = "-fx-background-color: " + SURFACE + "; -fx-background-radius: 14;"
            + "-fx-border-color: " + BORDER_SUBTLE + "; -fx-border-width: 1; -fx-border-radius: 14;"
            + "-fx-effect: " + CARD_SHADOW + ";";
    private static final String SOFT_PANEL_STYLE = "-fx-background-color: " + SURFACE_ALT + "; -fx-background-radius: 12;"
            + "-fx-border-color: #dbeafe; -fx-border-width: 1; -fx-border-radius: 12;";

    // Popular Car Models Database
    private static final String[] CAR_MODELS = {
            "Toyota Camry", "Toyota Corolla", "Toyota Fortuner", "Toyota Vios", "Toyota Innova",
            "Honda Civic", "Honda Accord", "Honda CR-V", "Honda City", "Honda Jazz",
            "Nissan Almera", "Nissan X-Trail", "Nissan Navara", "Nissan Qashqai",
            "Mazda 3", "Mazda CX-5", "Mazda CX-9", "Mazda 6",
            "Hyundai Elantra", "Hyundai Santa Fe", "Hyundai Tucson", "Hyundai i10",
            "Kia Cerato", "Kia Sportage", "Kia Niro", "Kia Seltos",
            "BMW X5", "BMW 3 Series", "BMW 5 Series", "BMW X3",
            "Mercedes C-Class", "Mercedes GLC", "Mercedes E-Class", "Mercedes GLE",
            "Proton X70", "Proton X90", "Proton Saga", "Proton Persona",
            "Perodua Myvi", "Perodua Alza", "Perodua Ativa", "Perodua Aruz",
            "Ford EcoSport", "Ford Ranger", "Ford Everest",
            "Suzuki Swift", "Suzuki Ertiga", "Suzuki Vitara"
    };

    // Identifies which analytics visualiser panel is currently visible.
    private enum VisualiserView {
        MARKET,
        SESSION,
        AGENT
    }

    // Categorizes logged negotiation events for charting and session reconstruction.
    private enum TrajectoryEvent {
        START,
        OFFER,
        COUNTER,
        ACCEPT,
        WALKAWAY,
        PRICE_UPDATE
    }

    // Captures one price or outcome point parsed from the live negotiation log.
    private static class TrajectoryPoint {
        private final int cycle;
        private final double price;
        private final String agent;
        private final String sessionId;
        private final String car;
        private final TrajectoryEvent event;

        // Creates a chartable trajectory point for a session or standalone agent update.
        private TrajectoryPoint(int cycle, double price, String agent, String sessionId, String car,
                TrajectoryEvent event) {
            this.cycle = cycle;
            this.price = price;
            this.agent = agent;
            this.sessionId = sessionId;
            this.car = car;
            this.event = event;
        }
    }

    // Stores parsed metadata for a broker session across multiple log events.
    private static class SessionMeta {
        private final String sessionId;
        private String buyer;
        private String dealer;
        private String car;
        private Integer buyerReserve;
        private Integer dealerReserve;
        private Integer firstOffer;
        private String outcomeStatus;
        private Double outcomePrice;
        private String failureReason;

        // Creates metadata storage for a session id before all fields are known.
        private SessionMeta(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    // Launches the JavaFX application.
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    // Starts JADE, builds the JavaFX scene, and wires log-driven dashboard updates.
    public void start(Stage stage) throws Exception {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        cc = rt.createMainContainer(p);
        loadFonts();

        UILogger logger = msg -> {
            String timestamp = "[" + LocalTime.now().format(timeFormatter) + "] ";
            final String formattedMsg = timestamp + msg + "\n";

            boolean isBuyerReg = msg.contains("Buyer") && (msg.contains("registered") || msg.contains("added"));
            boolean isDealerReg = msg.contains("Dealer") && msg.contains("listed");
            boolean isSetupMsg = msg.contains("BROKER ONLINE") || msg.contains("Fixed Negotiation Fee")
                    || msg.contains("Initializing Space Control");
            boolean isCycleShift = msg.contains("Cycle Shift:");
            boolean isSessionStart = msg.contains("[BROKER] SESSION START:");
            boolean isFeeCharged = msg.contains("[BROKER] FEE CHARGED:");
            boolean isDealSettled = msg.contains("[BROKER] DEAL SETTLED:");
            boolean isRevenue = msg.contains("[BROKER] REVENUE:");
            boolean isNoDeal = msg.contains("[BROKER] NO DEAL:");
            boolean isPerformance = msg.contains("[BROKER] PERFORMANCE:");
            boolean isRelay = msg.contains("[BROKER] RELAY") || msg.contains("[BROKER] INVITE:");
            boolean isPriceUpdate = msg.contains("RM") && (msg.contains("Price updated") ||
                    msg.contains("Willing to pay") ||
                    msg.contains("DEAL CLOSED") ||
                    msg.contains("SUCCESS!"));
            boolean isTrajectoryEvent = isSessionStart || isDealSettled || isNoDeal
                    || msg.contains("[BROKER] RELAY COUNTER") || msg.contains("[BROKER] RELAY OFFER")
                    || isPriceUpdate;
            boolean isNegotiationAction = isDealSettled || isNoDeal || isRelay || isPriceUpdate
                    || msg.contains("STATUS:") || msg.contains("AGREED") || msg.contains("NEGOTIATION:");

            Platform.runLater(() -> {
                if (msg.contains("[MANUAL_PROMPT]")) {
                    handleManualPromptLog(msg);
                    return;
                }
                if (msg.contains(": Terminating")) {
                    unregisterTerminatedAgent(msg);
                }

                if (isSetupMsg || isBuyerReg || isDealerReg || isCycleShift
                        || isSessionStart || isFeeCharged || isDealSettled
                        || isRevenue || isNoDeal || isPerformance || isNegotiationAction) {
                    logArea.appendText(formattedMsg);
                    if (dashboardEventsArea != null && (isSessionStart || isDealSettled || isNoDeal || isRelay
                            || isPerformance || isFeeCharged)) {
                        dashboardEventsArea.appendText(formattedMsg);
                        dashboardEventsArea.setScrollTop(Double.MAX_VALUE);
                    }
                }

                if (isBuyerReg) {
                    buyerCount++;
                    buyerCountLabel.setText(String.valueOf(buyerCount));
                    updateBuyerStatus();
                }
                if (isDealerReg) {
                    dealerCount++;
                    dealerCountLabel.setText(String.valueOf(dealerCount));
                    updateDealerStatus();
                }

                if (isSessionStart) {
                    activeSessions++;
                    activeSessionsLabel.setText(String.valueOf(activeSessions));
                    activeSessionsLabelMini.setText(String.valueOf(activeSessions));
                    sessionsArea.appendText(formattedMsg);
                }

                if (isFeeCharged) {
                    try {
                        Matcher matcher = RM_AMOUNT_PATTERN.matcher(msg);
                        if (matcher.find()) {
                            double amt = Double.parseDouble(matcher.group(1));
                            totalFixedFees += amt;
                            totalRevenue += amt;
                            fixedFeesLabel.setText("RM " + (int) totalFixedFees);
                            fixedFeesLabelMini.setText("RM " + (int) totalFixedFees);
                            revenueLabel.setText(String.format("RM %.2f", totalRevenue));
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (isDealSettled) {
                    dealsClosed++;
                    transactionCountLabel.setText(String.valueOf(dealsClosed));
                    if (activeSessions > 0)
                        activeSessions--;
                    activeSessionsLabel.setText(String.valueOf(activeSessions));
                    activeSessionsLabelMini.setText(String.valueOf(activeSessions));
                    sessionsArea.appendText(formattedMsg);
                }

                if (isRevenue) {
                    try {
                        int plusRm = msg.indexOf("+RM");
                        if (plusRm >= 0) {
                            Matcher matcher = RM_AMOUNT_PATTERN.matcher(msg.substring(plusRm));
                            if (matcher.find()) {
                                double commission = Double.parseDouble(matcher.group(1));
                                totalCommission += commission;
                                totalRevenue += commission;
                                commissionLabel.setText("RM " + (int) totalCommission);
                                commissionLabelMini.setText("RM " + (int) totalCommission);
                                revenueLabel.setText(String.format("RM %.2f", totalRevenue));
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (isNoDeal) {
                    failedDealsCount++;
                    failedDealsCountLabel.setText(String.valueOf(failedDealsCount));
                    if (activeSessions > 0)
                        activeSessions--;
                    activeSessionsLabel.setText(String.valueOf(activeSessions));
                    activeSessionsLabelMini.setText(String.valueOf(activeSessions));
                    recordFailureReport(msg);
                    failuresArea.appendText(formattedMsg);
                    sessionsArea.appendText(formattedMsg);
                }

                if (isCycleShift) {
                    try {
                        currentCycle = Integer.parseInt(msg.substring(msg.lastIndexOf(" ") + 1).trim());
                    } catch (Exception e) {
                        currentCycle++;
                    }
                }

                if (isTrajectoryEvent) {
                    ingestTrajectoryEvent(msg);
                }
            });
        };
        appLogger = logger;

        cc.createNewAgent("broker", "org.example.agents.BrokerAgent", new Object[] { logger }).start();
        cc.createNewAgent("space", "org.example.agents.SpaceControl", new Object[] { logger }).start();

        VBox mainContent = createMainContent(logger);

        Scene scene = new Scene(mainContent, 1500, 900);
        scene.setFill(Color.web(LIGHT_GRAY));

        stage.setScene(scene);
        stage.setTitle("Automated Car Negotiation System - Multi-Agent Platform");
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();
    }

    // Parses a log line into trajectory state used by the negotiation visualiser.
    private void ingestTrajectoryEvent(String msg) {
        try {
            SessionMeta meta = parseSessionStart(msg);
            if (meta != null) {
                sessionMetaMap.put(meta.sessionId, meta);
                if (meta.firstOffer != null) {
                    TrajectoryPoint point = new TrajectoryPoint(currentCycle, meta.firstOffer, meta.buyer,
                            meta.sessionId, meta.car, TrajectoryEvent.START);
                    storeTrajectoryPoint(point);
                }
                refreshNegotiationVisualiser();
                return;
            }

            TrajectoryPoint brokerPoint = parseBrokerTrajectoryPoint(msg);
            if (brokerPoint != null) {
                storeTrajectoryPoint(brokerPoint);
                refreshNegotiationVisualiser();
                return;
            }

            TrajectoryPoint agentPoint = parseAgentPricePoint(msg);
            if (agentPoint != null) {
                storeTrajectoryPoint(agentPoint);
                refreshNegotiationVisualiser();
            }
        } catch (Exception e) {
            System.err.println("Trajectory ingest error: " + e.getMessage());
        }
    }

    // Stores a trajectory point by session and by agent for later chart rendering.
    private void storeTrajectoryPoint(TrajectoryPoint point) {
        if (point.sessionId != null) {
            sessionPoints.computeIfAbsent(point.sessionId, k -> new ArrayList<>()).add(point);
            sessionLastPrice.put(point.sessionId, point.price);
        }
        if (point.agent != null && !point.agent.isEmpty()) {
            agentPoints.computeIfAbsent(point.agent, k -> new ArrayList<>()).add(point);
        }
    }

    // Extracts broker session-start metadata from a log message.
    private SessionMeta parseSessionStart(String msg) {
        int idx = msg.indexOf("SESSION START:");
        if (idx < 0)
            return null;
        String payload = msg.substring(idx + "SESSION START:".length()).trim();
        String[] parts = payload.split("\\|");
        if (parts.length == 0)
            return null;
        String sessionId = parts[0].trim();
        if (sessionId.isEmpty())
            return null;

        SessionMeta meta = new SessionMeta(sessionId);
        for (int i = 1; i < parts.length; i++) {
            String segment = parts[i].trim();
            int eq = segment.indexOf('=');
            if (eq < 0)
                continue;
            String key = segment.substring(0, eq).trim();
            String value = segment.substring(eq + 1).trim();
            if ("Buyer".equals(key)) {
                meta.buyer = value;
            } else if ("Dealer".equals(key)) {
                meta.dealer = value;
            } else if ("Car".equals(key)) {
                meta.car = value;
            } else if ("FirstOffer".equals(key)) {
                meta.firstOffer = parseMoneyValue(value);
            } else if ("BuyerReserve".equals(key)) {
                meta.buyerReserve = parseMoneyValue(value);
            } else if ("DealerReserve".equals(key)) {
                meta.dealerReserve = parseMoneyValue(value);
            }
        }
        return meta;
    }

    // Converts broker relay, settlement, or no-deal logs into session trajectory points.
    private TrajectoryPoint parseBrokerTrajectoryPoint(String msg) {
        if (!msg.contains("[BROKER]"))
            return null;

        if (msg.contains("RELAY COUNTER:")) {
            String payload = msg.substring(msg.indexOf("RELAY COUNTER:") + 14).trim();
            String sessionId = extractSessionId(payload);
            Integer price = extractLastMoneyValue(payload);
            if (sessionId == null || price == null)
                return null;
            SessionMeta meta = sessionMetaMap.computeIfAbsent(sessionId, SessionMeta::new);
            hydrateSessionMeta(meta, payload);
            return new TrajectoryPoint(currentCycle, price, meta.dealer, sessionId, meta.car,
                    TrajectoryEvent.COUNTER);
        }

        if (msg.contains("RELAY OFFER:")) {
            String payload = msg.substring(msg.indexOf("RELAY OFFER:") + 12).trim();
            String sessionId = extractSessionId(payload);
            Integer price = extractLastMoneyValue(payload);
            if (sessionId == null || price == null)
                return null;
            SessionMeta meta = sessionMetaMap.computeIfAbsent(sessionId, SessionMeta::new);
            hydrateSessionMeta(meta, payload);
            return new TrajectoryPoint(currentCycle, price, meta.buyer, sessionId, meta.car,
                    TrajectoryEvent.OFFER);
        }

        if (msg.contains("DEAL SETTLED:")) {
            String payload = msg.substring(msg.indexOf("DEAL SETTLED:") + 13).trim();
            String sessionId = extractSessionId(payload);

            Integer price = null;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Price=RM\\s*(\\d+)").matcher(payload);
            if (m.find()) {
                price = Integer.parseInt(m.group(1));
            } else {
                price = extractLastMoneyValue(payload);
            }

            if (sessionId == null || price == null)
                return null;
            SessionMeta meta = sessionMetaMap.computeIfAbsent(sessionId, SessionMeta::new);
            hydrateSessionMeta(meta, payload);
            meta.outcomeStatus = "ACCEPTED";
            meta.outcomePrice = price.doubleValue();
            meta.failureReason = null;
            return new TrajectoryPoint(currentCycle, price, meta.dealer, sessionId, meta.car,
                    TrajectoryEvent.ACCEPT);
        }

        if (msg.contains("NO DEAL:")) {
            String payload = msg.substring(msg.indexOf("NO DEAL:") + 8).trim();
            String sessionId = extractSessionId(payload);
            if (sessionId == null)
                return null;
            SessionMeta meta = sessionMetaMap.computeIfAbsent(sessionId, SessionMeta::new);
            hydrateSessionMeta(meta, payload);
            Double lastPrice = sessionLastPrice.get(sessionId);
            meta.outcomeStatus = "NO DEAL";
            meta.outcomePrice = lastPrice;
            meta.failureReason = extractReason(payload);
            return new TrajectoryPoint(currentCycle, lastPrice != null ? lastPrice : 0, meta.buyer, sessionId, meta.car,
                    TrajectoryEvent.WALKAWAY);
        }

        return null;
    }

    // Converts buyer/dealer price update logs into standalone agent trajectory points.
    private TrajectoryPoint parseAgentPricePoint(String msg) {
        if (!msg.contains(":") || !msg.contains("RM"))
            return null;
        if (!(msg.contains("Price updated") || msg.contains("Willing to pay")))
            return null;

        int colon = msg.indexOf(":");
        if (colon <= 0)
            return null;
        String agentName = msg.substring(0, colon).trim();
        boolean knownAgent = false;
        synchronized (dealerAgents) {
            if (dealerAgents.contains(agentName))
                knownAgent = true;
        }
        synchronized (buyerAgents) {
            if (buyerAgents.contains(agentName))
                knownAgent = true;
        }
        if (!knownAgent)
            return null;

        Integer price = extractLastMoneyValue(msg);
        if (price == null)
            return null;

        String car = null;
        int forIdx = msg.indexOf(" for ");
        if (forIdx >= 0) {
            car = msg.substring(forIdx + 5).trim();
            int cut = car.indexOf(" (");
            if (cut >= 0) {
                car = car.substring(0, cut).trim();
            }
        }

        return new TrajectoryPoint(currentCycle, price, agentName, null, car, TrajectoryEvent.PRICE_UPDATE);
    }

    // Fills missing session metadata from a broker payload string.
    private void hydrateSessionMeta(SessionMeta meta, String payload) {
        if (hydrateSessionMetaFromFields(meta, payload)) {
            return;
        }
        String[] segments = payload.split("\\|");
        for (String segment : segments) {
            String seg = segment.trim();
            if (seg.startsWith("Buyer=")) {
                meta.buyer = seg.substring(6).trim();
            }
            if (seg.contains("Buyer=")) {
                int idx = seg.indexOf("Buyer=");
                String buyer = seg.substring(idx + 6).trim();
                if (buyer.contains("→")) {
                    buyer = buyer.substring(buyer.indexOf("→") + 1).trim();
                } else if (buyer.contains("->")) {
                    buyer = buyer.substring(buyer.indexOf("->") + 2).trim();
                }
                meta.buyer = buyer;
            }
            if (seg.startsWith("Dealer=")) {
                String dealer = seg.substring(7).trim();
                if (dealer.contains("→")) {
                    dealer = dealer.substring(0, dealer.indexOf("→")).trim();
                } else if (dealer.contains("->")) {
                    dealer = dealer.substring(0, dealer.indexOf("->")).trim();
                }
                meta.dealer = dealer;
            }
            if (seg.startsWith("Car=")) {
                meta.car = seg.substring(4).trim();
            }
            if (seg.startsWith("Price=")) {
                Integer price = parseMoneyValue(seg.substring(6).trim());
                if (price != null) {
                    meta.outcomePrice = price.doubleValue();
                }
            }
            if (seg.startsWith("Reason=")) {
                meta.failureReason = seg.substring(7).trim();
            }
        }
    }

    // Reads pipe-delimited key/value fields into session metadata and reports whether anything changed.
    private boolean hydrateSessionMetaFromFields(SessionMeta meta, String payload) {
        String buyer = extractBrokerField(payload, "Buyer");
        String dealer = extractBrokerField(payload, "Dealer");
        String car = extractBrokerField(payload, "Car");
        String price = extractBrokerField(payload, "Price");
        String reason = extractBrokerField(payload, "Reason");
        boolean hydrated = false;

        if (buyer != null) {
            meta.buyer = buyer;
            hydrated = true;
        }
        if (dealer != null) {
            meta.dealer = dealer;
            hydrated = true;
        }
        if (car != null) {
            meta.car = car;
            hydrated = true;
        }
        if (price != null) {
            Integer parsedPrice = parseMoneyValue(price);
            if (parsedPrice != null) {
                meta.outcomePrice = parsedPrice.doubleValue();
                hydrated = true;
            }
        }
        if (reason != null) {
            meta.failureReason = reason;
            hydrated = true;
        }
        return hydrated;
    }

    // Extracts a named broker field from pipe-delimited log payload text.
    private String extractBrokerField(String payload, String key) {
        String prefix = key + "=";
        String[] segments = payload.split("\\|");
        for (String segment : segments) {
            String seg = segment.trim();
            int idx = seg.indexOf(prefix);
            if (idx < 0) {
                continue;
            }
            String value = seg.substring(idx + prefix.length()).trim();
            value = trimBrokerRelationship(value);
            value = stripKnownFieldPrefix(value);
            return value.isBlank() ? null : value;
        }
        return null;
    }

    // Trims a broker relationship field at the next known key marker.
    private String trimBrokerRelationship(String value) {
        String cleaned = value == null ? "" : value.trim();
        String[] arrows = { "\u2192", "\u00e2\u2020\u2019", "->" };
        for (String arrow : arrows) {
            int arrowIdx = cleaned.indexOf(arrow);
            if (arrowIdx >= 0) {
                cleaned = cleaned.substring(0, arrowIdx).trim();
            }
        }
        return cleaned;
    }

    // Removes repeated broker field prefixes from a parsed value.
    private String stripKnownFieldPrefix(String value) {
        String cleaned = value == null ? "" : value.trim();
        String[] prefixes = { "Buyer=", "Dealer=", "Car=", "Price=", "Reason=" };
        boolean changed;
        do {
            changed = false;
            for (String prefix : prefixes) {
                if (cleaned.startsWith(prefix)) {
                    cleaned = cleaned.substring(prefix.length()).trim();
                    changed = true;
                }
            }
        } while (changed);
        return cleaned;
    }

    // Aggregates session data into a shape directly consumed by charts and tables.
    private static class SessionViewModel {
        private String sessionId;
        private String buyer;
        private String dealer;
        private String car;
        private Integer listPrice;
        private Integer buyerReserve;
        private Integer dealerReserve;
        private Integer firstOffer;
        private Double latestPrice;
        private String outcome;
        private String failureReason;
        private int rounds;
        private double totalConcession;
        private final List<TrajectoryPoint> points = new ArrayList<>();
    }

    // Aggregates per-agent outcomes and concession metrics for visualisation.
    private static class AgentViewModel {
        private String name;
        private String type;
        private int sessions;
        private int accepted;
        private int rejected;
        private int pending;
        private double averageConcession;
    }

    // Represents one listing row with live inventory and active-buyer status.
    private static class ListingViewModel {
        private String car;
        private String dealer;
        private Integer listPrice;
        private Integer reserve;
        private int activeBuyers;
        private String status;
    }

    // Extracts a failure reason from a broker no-deal payload.
    private String extractReason(String payload) {
        String[] segments = payload.split("\\|");
        for (String segment : segments) {
            String seg = segment.trim();
            if (seg.startsWith("Reason=")) {
                return seg.substring(7).trim();
            }
        }
        return null;
    }

    // Extracts the leading session id from a broker payload.
    private String extractSessionId(String payload) {
        int pipe = payload.indexOf('|');
        String raw = pipe >= 0 ? payload.substring(0, pipe).trim() : payload.trim();
        return raw.isEmpty() ? null : raw;
    }

    // Parses the first RM amount in text, returning null when none is present.
    private Integer parseMoneyValue(String value) {
        Matcher matcher = RM_AMOUNT_PATTERN.matcher(value);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    // Parses the last RM amount in text for logs that contain multiple prices.
    private Integer extractLastMoneyValue(String payload) {
        Matcher matcher = RM_AMOUNT_PATTERN.matcher(payload);
        Integer amount = null;
        while (matcher.find()) {
            amount = Integer.parseInt(matcher.group(1));
        }
        return amount;
    }

    // Loads bundled Poppins font variants used by the JavaFX interface.
    private void loadFonts() {
        loadFontResource("/fonts/Poppins-Regular.ttf");
        loadFontResource("/fonts/Poppins-Medium.ttf");
        loadFontResource("/fonts/Poppins-SemiBold.ttf");
        loadFontResource("/fonts/Poppins-Bold.ttf");
    }

    // Loads one font resource path and ignores missing resources gracefully.
    private void loadFontResource(String path) {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream == null) {
                System.err.println("Font not found: " + path);
                return;
            }
            Font.loadFont(stream, 12);
        } catch (IOException e) {
            System.err.println("Failed to load font " + path + ": " + e.getMessage());
        }
    }

    // Creates the main shell containing the sidebar and active workspace.
    private VBox createMainContent(UILogger logger) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + LIGHT_GRAY + "; -fx-font-family: " + FONT_FAMILY
                + "; -fx-font-weight: " + FONT_WEIGHT_MEDIUM + ";");

        workspacePane = new StackPane();
        workspacePane.setStyle("-fx-background-color: transparent;");
        workspacePane.getChildren().addAll(
                createWorkspaceView("Dashboard", createBrokerView()),
                createWorkspaceView("Participants", createParticipantsView(logger)),
                createWorkspaceView("Manual Negotiation", createManualPlayView()),
                createWorkspaceView("Sessions", createSessionsView()),
                createWorkspaceView("Analytics", createMarketAnalysisView()),
                createWorkspaceView("Logs", createLogsView()));

        VBox sidebar = createSidebar();
        HBox shell = new HBox(0, sidebar, workspacePane);
        shell.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");
        HBox.setHgrow(workspacePane, Priority.ALWAYS);
        VBox.setVgrow(shell, Priority.ALWAYS);

        root.getChildren().addAll(createAppHeader(), createActionBar(), shell);
        showWorkspace("Dashboard");
        return root;
    }

    // Wraps a workspace node and tags it with the navigation key.
    private Node createWorkspaceView(String key, Node content) {
        StackPane wrapper = new StackPane(content);
        wrapper.setUserData(key);
        wrapper.setVisible(false);
        wrapper.setManaged(false);
        StackPane.setMargin(content, new Insets(0));
        return wrapper;
    }

    // Applies shared visual behavior to scroll panes.
    private void polishScrollPane(ScrollPane scroll) {
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;"
                + "-fx-border-color: transparent; -fx-padding: 0;");
    }

    // Returns the shared TextArea CSS, optionally using a monospace font.
    private String textAreaStyle(boolean monospace) {
        String font = monospace ? "'JetBrains Mono', 'Consolas', 'Courier New'" : FONT_FAMILY;
        return "-fx-font-size: 12; -fx-font-family: " + font + "; -fx-font-weight: " + FONT_WEIGHT_MEDIUM + ";"
                + "-fx-control-inner-background: " + SURFACE + ";"
                + "-fx-text-fill: " + DARK_TEXT + ";"
                + "-fx-highlight-fill: #bfdbfe;"
                + "-fx-border-color: " + BORDER_SUBTLE + "; -fx-border-width: 1;"
                + "-fx-border-radius: 10; -fx-background-radius: 10;"
                + "-fx-padding: 8;";
    }

    // Creates a standard styled panel container.
    private VBox createPanel(double spacing, Insets padding) {
        VBox panel = new VBox(spacing);
        panel.setPadding(padding);
        panel.setStyle(PANEL_STYLE);
        return panel;
    }

    // Builds the left navigation sidebar and its workspace buttons.
    private VBox createSidebar() {
        navigationButtons.clear();

        Label brand = new Label("Broker Console");
        brand.setStyle("-fx-font-size: 16; -fx-font-weight: 800; -fx-text-fill: white;");
        Label sub = new Label("Car negotiation demo");
        sub.setStyle("-fx-font-size: 11; -fx-text-fill: #bfdbfe;");
        VBox brandBox = new VBox(2, brand, sub);
        brandBox.setPadding(new Insets(6, 8, 18, 8));

        VBox nav = new VBox(8);
        nav.getChildren().addAll(
                createNavigationButton("Dashboard", "Overview, KPIs, graph"),
                createNavigationButton("Participants", "Buyers and dealers"),
                createNavigationButton("Manual Negotiation", "Guided negotiation"),
                createNavigationButton("Sessions", "Deals and fees"),
                createNavigationButton("Analytics", "Settings and protocol"),
                createNavigationButton("Logs", "Activity and failures"));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label footer = new Label("Poppins UI\nJADE ACL routed");
        footer.setStyle("-fx-font-size: 11; -fx-text-fill: #bfdbfe; -fx-line-spacing: 2;");

        VBox sidebar = new VBox(8, brandBox, nav, spacer, footer);
        sidebar.setPadding(new Insets(18, 14, 18, 14));
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(220);
        sidebar.setStyle("-fx-background-color: linear-gradient(to bottom, #1e3a8a, #312e81);"
                + "-fx-border-color: #c7d2fe; -fx-border-width: 0 1 0 0;");
        return sidebar;
    }

    // Creates a sidebar navigation button for one workspace.
    private Button createNavigationButton(String title, String subtitle) {
        Button btn = new Button(title + "\n" + subtitle);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        btn.setStyle(navButtonStyle(false));
        btn.setOnAction(e -> showWorkspace(title));
        navigationButtons.put(title, btn);
        return btn;
    }

    // Returns sidebar navigation CSS for active or inactive state.
    private String navButtonStyle(boolean active) {
        return "-fx-font-size: 12; -fx-font-family: " + FONT_FAMILY + "; -fx-font-weight: 700;"
                + "-fx-padding: 10 12; -fx-background-radius: 10; -fx-border-radius: 10;"
                + "-fx-background-color: " + (active ? "white" : "rgba(255,255,255,0.10)") + ";"
                + "-fx-text-fill: " + (active ? "#1e3a8a" : "white") + ";"
                + "-fx-cursor: hand; -fx-line-spacing: 2;";
    }

    // Shows the selected workspace and refreshes navigation button state.
    private void showWorkspace(String key) {
        if (workspacePane == null) {
            return;
        }
        for (Node child : workspacePane.getChildren()) {
            boolean selected = key.equals(String.valueOf(child.getUserData()));
            child.setVisible(selected);
            child.setManaged(selected);
        }
        for (Map.Entry<String, Button> entry : navigationButtons.entrySet()) {
            entry.getValue().setStyle(navButtonStyle(entry.getKey().equals(key)));
        }
        refreshNegotiationVisualiser();
    }

    // Builds the top application header and live cycle label.
    private VBox createAppHeader() {
        Region stripe = new Region();
        stripe.setPrefWidth(5);
        stripe.setMinWidth(5);
        stripe.setStyle("-fx-background-color: " + ACCENT_BLUE + ";");

        Label title = new Label("Automated Car Negotiation System");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");
        Label subtitle = new Label(
                "JADE broker-routed marketplace  ·  session-based negotiation  ·  real-time metrics");
        subtitle.setStyle("-fx-font-size: 12; -fx-text-fill: " + TEXT_MUTED + ";");
        VBox titleBox = new VBox(3, title, subtitle);
        titleBox.setPadding(new Insets(0, 0, 0, 14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label cycleLabel = new Label("Cycle: 0");
        cycleLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 700; -fx-text-fill: white; "
                + "-fx-background-color: " + PRIMARY_BLUE + "; -fx-background-radius: 20; -fx-padding: 4 14;");
        logArea.textProperty().addListener((o, ov, nv) -> cycleLabel.setText("Cycle: " + currentCycle));

        HBox row = new HBox(0, stripe, titleBox, spacer, cycleLabel);
        row.setPadding(new Insets(14, 20, 14, 0));
        row.setStyle("-fx-background-color: " + SURFACE + "; -fx-border-color: " + BORDER_SUBTLE
                + "; -fx-border-width: 0 0 1 0;");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return new VBox(row);
    }

    // Toggles the market clock between automatic and paused modes.
    private void toggleAutoplay() {
        isAutoPlay = !isAutoPlay;
        if (playPauseBtn != null) {
            playPauseBtn.setText(isAutoPlay ? "Pause" : "Resume");
        }
        sendSpaceCommand(isAutoPlay ? "RESUME" : "PAUSE");
    }

    // Refreshes the manual start status text from waiting buyer count.
    private void updateNegotiationControlStatus() {
        if (negotiationControlStatusLabel == null) {
            return;
        }

        int waitingCount = waitingBuyerAgents.size();
        int totalCount = buyerAgents.size();
        String state = isAutoPlay ? "running" : "paused";
        negotiationControlStatusLabel.setText("Buyers: " + totalCount + " total | "
                + waitingCount + " waiting | Simulation " + state);
    }

    // Logs a UI-originated message through the shared application logger.
    private void loggerLog(String message) {
        String timestamp = "[" + LocalTime.now().format(timeFormatter) + "] ";
        logArea.appendText(timestamp + "[UI] " + message + "\n");
    }

    // Removes terminated buyers or dealers from UI tracking lists.
    private void unregisterTerminatedAgent(String msg) {
        int colon = msg.indexOf(':');
        if (colon <= 0) {
            return;
        }
        String agentName = msg.substring(0, colon).trim();
        synchronized (buyerAgents) {
            buyerAgents.remove(agentName);
        }
        synchronized (dealerAgents) {
            dealerAgents.remove(agentName);
        }
        waitingBuyerAgents.remove(agentName);
        manualBuyerAgents.remove(agentName);
        updateNegotiationControlStatus();
    }

    // Records a failed negotiation log entry and updates reason counts.
    private void recordFailureReport(String msg) {
        String payload = msg.contains("NO DEAL:")
                ? msg.substring(msg.indexOf("NO DEAL:") + "NO DEAL:".length()).trim()
                : msg;
        String reason = extractBrokerField(payload, "Reason");
        if (reason == null || reason.isBlank()) {
            reason = "UNKNOWN";
        }
        failureReasonCounts.put(reason, failureReasonCounts.getOrDefault(reason, 0) + 1);
        if (failureReportArea != null) {
            failureReportArea.setText(buildFailureReport(payload, reason));
            failureReportArea.setScrollTop(0);
        }
    }

    // Builds the text report shown in the failed-negotiations view.
    private String buildFailureReport(String latestPayload, String latestReason) {
        StringBuilder report = new StringBuilder();
        report.append("Failure summary\n");
        report.append("----------------\n");
        report.append("Total failed negotiations: ").append(failedDealsCount).append("\n\n");
        for (Map.Entry<String, Integer> entry : failureReasonCounts.entrySet()) {
            report.append("- ").append(humanFailureReason(entry.getKey()))
                    .append(": ").append(entry.getValue()).append("\n");
        }
        report.append("\nLatest failure\n");
        report.append("--------------\n");
        report.append("Reason: ").append(humanFailureReason(latestReason)).append("\n");
        appendFailureField(report, latestPayload, "Buyer");
        appendFailureField(report, latestPayload, "Dealer");
        appendFailureField(report, latestPayload, "Car");
        appendFailureField(report, latestPayload, "Budget");
        String sessionId = extractSessionId(latestPayload);
        if (sessionId != null) {
            report.append("Session: ").append(sessionId).append("\n");
        }
        report.append("\nMeaning\n");
        report.append("-------\n");
        report.append(explainFailureReason(latestReason)).append("\n");
        return report.toString();
    }

    // Appends a broker payload field to the failure report when present.
    private void appendFailureField(StringBuilder report, String payload, String key) {
        String value = extractBrokerField(payload, key);
        if (value != null && !value.isBlank()) {
            report.append(key).append(": ").append(value).append("\n");
        }
    }

    // Converts a protocol failure reason into a human-readable title.
    private String humanFailureReason(String reason) {
        return reason == null ? "Unknown" : reason.replace('_', ' ').toLowerCase();
    }

    // Explains what a protocol failure reason means for the user.
    private String explainFailureReason(String reason) {
        if ("BUDGET_TOO_LOW".equals(reason)) {
            return "The buyer budget was below every matching dealer reserve price, so no session fee was charged.";
        }
        if ("NO_MATCHING_CAR".equals(reason)) {
            return "The broker could not find a listed car matching the buyer request after retries.";
        }
        if ("MAX_ROUNDS_REACHED".equals(reason)) {
            return "The buyer and dealer could not agree within the configured round limit.";
        }
        if ("DEALER_SOLD_OUT".equals(reason)) {
            return "The dealer sold the available stock while another buyer was still negotiating.";
        }
        if (reason != null && reason.startsWith("DEALER_REJECTED")) {
            return "The dealer declined to engage because the first offer terms were too weak.";
        }
        if ("USER_STOPPED".equals(reason)) {
            return "The user stopped the buyer from the toolbar.";
        }
        if ("TIMEOUT".equals(reason)) {
            return "The broker closed a session that stayed open past the timeout.";
        }
        return "The broker recorded the session as failed; check the raw failure log for the exact context.";
    }

    // Sends a command ontology to the SpaceControl agent through a short-lived command agent.
    private void sendSpaceCommand(String command) {
        try {
            cc.createNewAgent(nextAgentName("space-command"), "org.example.agents.SpaceCommandAgent",
                    new Object[] { command, "space", "" }).start();
        } catch (Exception e) {
            System.err.println("Error sending command to SpaceControl: " + e.getMessage());
        }
    }

    // Sends a manual PRICE_ADJUSTMENT command to a dealer agent.
    private void sendDealerPriceAdjustment(String dealerName, String price) {
        try {
            cc.createNewAgent(nextAgentName("dealer-command"), "org.example.agents.SpaceCommandAgent",
                    new Object[] { "PRICE_ADJUSTMENT", dealerName, price }).start();
        } catch (Exception e) {
            showAlert("❌ Error sending price adjustment: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Sends a command with empty content to a named agent.
    private void sendAgentCommand(String agentName, String command) {
        sendAgentCommand(agentName, command, "");
    }

    // Sends a command and content payload to a named agent through SpaceCommandAgent.
    private void sendAgentCommand(String agentName, String command, String content) {
        try {
            cc.createNewAgent(nextAgentName("agent-command"), "org.example.agents.SpaceCommandAgent",
                    new Object[] { command, agentName, content }).start();
        } catch (Exception e) {
            showAlert("❌ Error sending command to " + agentName + ": " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Launches the JADE Sniffer agent against active negotiation participants.
    private void launchSniffer(UILogger logger) {
        try {
            String target = buildSnifferTargets();
            if (!hasRegisteredNegotiationAgents()) {
                showAlert("Create demo or custom buyer/dealer agents before opening Sniffer. "
                        + "Sniffer only preloads agents that already exist.", Alert.AlertType.INFORMATION);
                logger.log("STATUS: Sniffer not launched: create buyers/dealers first, then open Sniffer before Start.");
                return;
            }
            cc.createNewAgent(nextAgentName("sniffer"), "jade.tools.sniffer.Sniffer",
                    new Object[] { target }).start();
            logger.log("STATUS: JADE Sniffer launched for: " + target);
        } catch (Exception e) {
            logger.log("STATUS: Sniffer not launched: " + e.getMessage());
        }
    }

    // Returns whether any buyer or dealer agents are available for sniffing.
    private boolean hasRegisteredNegotiationAgents() {
        synchronized (dealerAgents) {
            if (!dealerAgents.isEmpty()) {
                return true;
            }
        }
        synchronized (buyerAgents) {
            return !buyerAgents.isEmpty();
        }
    }

    // Builds the semicolon-separated Sniffer target list.
    private String buildSnifferTargets() {
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        targets.add("broker");
        targets.add("space");
        synchronized (dealerAgents) {
            targets.addAll(dealerAgents);
        }
        synchronized (buyerAgents) {
            targets.addAll(buyerAgents);
        }
        return String.join(";", targets);
    }

    // Generates a unique agent name for a prefix using the current timestamp.
    private String nextAgentName(String prefix) {
        return prefix + "-" + System.nanoTime() + "-" + commandAgentCounter.incrementAndGet();
    }

    // Builds the workspace that contains dealer and buyer creation forms.
    private VBox createParticipantsView(UILogger logger) {
        VBox page = createPage("Participants", "Create dealers first, then add buyers into the broker-routed market.");
        HBox columns = new HBox(22);
        columns.setPadding(new Insets(2, 2, 18, 2));
        columns.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        VBox dealer = createDealerView(logger);
        VBox buyer = createBuyerView(logger);
        dealer.setPadding(new Insets(0));
        buyer.setPadding(new Insets(0));
        dealer.setStyle("-fx-background-color: transparent;");
        buyer.setStyle("-fx-background-color: transparent;");

        columns.getChildren().addAll(dealer, buyer);
        HBox.setHgrow(dealer, Priority.ALWAYS);
        HBox.setHgrow(buyer, Priority.ALWAYS);
        dealer.setMinWidth(440);
        buyer.setMinWidth(440);
        dealer.setMaxWidth(Double.MAX_VALUE);
        buyer.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scroll = new ScrollPane(columns);
        polishScrollPane(scroll);
        page.getChildren().add(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return page;
    }

    // Builds the workspace containing activity and failure logs.
    private VBox createLogsView() {
        VBox page = createPage("Logs", "Monitor broker activity and failed negotiations in one place.");
        HBox columns = new HBox(18);
        columns.setMinHeight(0);

        VBox activity = createActivityLogView();
        VBox failures = createFailuresView();
        activity.setPadding(new Insets(0));
        failures.setPadding(new Insets(0));
        activity.setStyle("-fx-background-color: transparent;");
        failures.setStyle("-fx-background-color: transparent;");
        activity.setMinWidth(520);
        failures.setMinWidth(460);
        failures.setPrefWidth(520);

        columns.getChildren().addAll(activity, failures);
        HBox.setHgrow(activity, Priority.ALWAYS);
        HBox.setHgrow(failures, Priority.ALWAYS);
        VBox.setVgrow(activity, Priority.ALWAYS);
        VBox.setVgrow(failures, Priority.ALWAYS);
        page.getChildren().add(columns);
        VBox.setVgrow(columns, Priority.ALWAYS);
        return page;
    }

    // Creates a page container with a standard title and subtitle header.
    private VBox createPage(String title, String subtitle) {
        VBox page = new VBox(18);
        page.setPadding(new Insets(24));
        page.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 25; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 500; -fx-text-fill: " + TEXT_MUTED + ";");
        VBox header = new VBox(3, titleLabel, subtitleLabel);
        header.setPadding(new Insets(0, 0, 2, 0));
        page.getChildren().add(header);
        return page;
    }

    // Builds the broker dashboard with metrics, visualiser, feed, and checklist.
    private VBox createBrokerView() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label headerLabel = new Label("Marketplace Dashboard");
        headerLabel.setStyle("-fx-font-size: 22; -fx-font-weight: 700; -fx-text-fill: " + PRIMARY_BLUE + ";");
        Label subLabel = new Label("Live broker metrics · negotiation trajectory · quick-start guide");
        subLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + TEXT_MUTED + ";");
        VBox hdr = new VBox(2, headerLabel, subLabel);

        HBox statsRow1 = new HBox(12,
                createStatCard("🧑 Buyers", buyerCountLabel, ACCENT_BLUE),
                createStatCard("🚗 Dealers", dealerCountLabel, WARNING_ORANGE),
                createStatCard("🔁 Active Sessions", activeSessionsLabel, "#8b5cf6"));
        HBox statsRow2 = new HBox(12,
                createStatCard("✅ Deals Closed", transactionCountLabel, SUCCESS_GREEN),
                createStatCard("❌ Failed Deals", failedDealsCountLabel, ERROR_RED),
                createStatCard("💰 Total Revenue", revenueLabel, "#ec4899"));
        HBox statsRow3 = new HBox(12,
                createStatCard("💵 Fixed Fees", fixedFeesLabel, "#06b6d4"),
                createStatCard("Commission (5% deals)", commissionLabel, SUCCESS_GREEN));
        for (HBox row : new HBox[] { statsRow1, statsRow2, statsRow3 }) {
            for (javafx.scene.Node n : row.getChildren())
                HBox.setHgrow(n, Priority.ALWAYS);
        }
        VBox statsSection = new VBox(10, statsRow1, statsRow2, statsRow3);

        dashboardEventsArea = new TextArea();
        dashboardEventsArea.setEditable(false);
        dashboardEventsArea.setWrapText(true);
        dashboardEventsArea.setPrefRowCount(9);
        dashboardEventsArea.setStyle(textAreaStyle(true));

        VBox brokerFeed = createPanel(10, new Insets(16));
        brokerFeed.getChildren().addAll(createSectionLabel("Broker event feed"), dashboardEventsArea);

        VBox checklist = new VBox(8,
                createSectionLabel("Demo checklist"),
                createChecklistItem("1. Register dealers or use Demo Setup"),
                createChecklistItem("2. Add waiting buyers"),
                createChecklistItem("3. Press Start and watch broker-routed offers"),
                createChecklistItem("4. Use Session Detail to explain a deal"));
        checklist.setPadding(new Insets(16));
        checklist.setStyle("-fx-background-color: #ecfeff; -fx-background-radius: 14;"
                + "-fx-border-color: #67e8f9; -fx-border-radius: 14; -fx-border-width: 1;");

        VBox chartSection = createNegotiationVisualiser();


        VBox sideCol = new VBox(14, brokerFeed, checklist);
        sideCol.setMinWidth(300);
        sideCol.setPrefWidth(340);
        sideCol.setMaxWidth(380);

        HBox bodyRow = new HBox(16, chartSection, sideCol);
        HBox.setHgrow(chartSection, Priority.ALWAYS);
        VBox.setVgrow(chartSection, Priority.ALWAYS);
        VBox.setVgrow(bodyRow, Priority.ALWAYS);

        box.getChildren().addAll(hdr, statsSection, bodyRow);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    // Builds the tabbed negotiation visualiser section for market, session, and agent views.
    private VBox createNegotiationVisualiser() {
        visualiserButtons.clear();

        Label title = new Label("Negotiation Visualiser");
        title.setStyle("-fx-font-size: 15; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");
        Label hint = new Label("Market, session, and agent views built from live broker-routed negotiation logs.");
        hint.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_MUTED + ";");
        VBox titleBox = new VBox(2, title, hint);

        HBox tabs = new HBox(6,
                createVisualiserTab("Market View", VisualiserView.MARKET),
                createVisualiserTab("Session View", VisualiserView.SESSION),
                createVisualiserTab("Agent View", VisualiserView.AGENT));
        tabs.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, titleBox, spacer, tabs);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        marketVisualiserPane = new VBox(14);
        sessionVisualiserPane = new VBox(14);
        agentVisualiserPane = new VBox(14);
        marketVisualiserScroll = createVisualiserScroll(marketVisualiserPane);
        sessionVisualiserScroll = createVisualiserScroll(sessionVisualiserPane);
        agentVisualiserScroll = createVisualiserScroll(agentVisualiserPane);
        visualiserContentPane = new StackPane(marketVisualiserScroll, sessionVisualiserScroll, agentVisualiserScroll);
        visualiserContentPane.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        visualiserContentPane.setMinHeight(360);
        visualiserContentPane.setPrefHeight(660);
        VBox.setVgrow(visualiserContentPane, Priority.ALWAYS);

        VBox section = new VBox(12, header, visualiserContentPane);
        section.setPadding(new Insets(16));
        section.setStyle(PANEL_STYLE);
        VBox.setVgrow(section, Priority.ALWAYS);

        showVisualiserView(VisualiserView.MARKET);
        refreshNegotiationVisualiser();
        return section;
    }

    // Wraps a visualiser pane in a consistently styled scroll pane.
    private ScrollPane createVisualiserScroll(VBox content) {
        content.setFillWidth(true);
        content.setMinWidth(0);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setMinHeight(340);
        scroll.setPrefHeight(660);
        polishScrollPane(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    // Creates a visualiser tab button bound to one view.
    private Button createVisualiserTab(String text, VisualiserView view) {
        Button button = new Button(text);
        button.setStyle(visualiserTabStyle(false));
        button.setOnAction(e -> showVisualiserView(view));
        visualiserButtons.put(view, button);
        return button;
    }

    // Returns CSS for selected or unselected visualiser tabs.
    private String visualiserTabStyle(boolean active) {
        return "-fx-font-size: 12; -fx-font-family: " + FONT_FAMILY + "; -fx-font-weight: 800;"
                + "-fx-padding: 7 12; -fx-background-radius: 8; -fx-border-radius: 8;"
                + "-fx-background-color: " + (active ? ACCENT_BLUE : SURFACE_ALT) + ";"
                + "-fx-text-fill: " + (active ? "white" : TEXT_MUTED) + ";"
                + "-fx-border-color: " + (active ? ACCENT_BLUE : BORDER_SUBTLE) + "; -fx-cursor: hand;";
    }

    // Switches the visible visualiser pane and refreshes tab styling.
    private void showVisualiserView(VisualiserView view) {
        activeVisualiserView = view;
        if (marketVisualiserScroll != null) {
            marketVisualiserScroll.setVisible(view == VisualiserView.MARKET);
            marketVisualiserScroll.setManaged(view == VisualiserView.MARKET);
        }
        if (sessionVisualiserScroll != null) {
            sessionVisualiserScroll.setVisible(view == VisualiserView.SESSION);
            sessionVisualiserScroll.setManaged(view == VisualiserView.SESSION);
        }
        if (agentVisualiserScroll != null) {
            agentVisualiserScroll.setVisible(view == VisualiserView.AGENT);
            agentVisualiserScroll.setManaged(view == VisualiserView.AGENT);
        }
        for (Map.Entry<VisualiserView, Button> entry : visualiserButtons.entrySet()) {
            entry.getValue().setStyle(visualiserTabStyle(entry.getKey() == view));
        }
        refreshNegotiationVisualiser();
    }

    // Rebuilds all visualiser panes from the latest parsed trajectory data.
    private void refreshNegotiationVisualiser() {
        if (visualiserContentPane == null) {
            return;
        }
        if (activeVisualiserView == VisualiserView.MARKET) {
            renderMarketVisualiser();
        } else if (activeVisualiserView == VisualiserView.SESSION) {
            renderSessionVisualiser();
        } else {
            renderAgentVisualiser();
        }
    }

    // Renders market-level metrics, listing status, and aggregate charts.
    private void renderMarketVisualiser() {
        if (marketVisualiserPane == null) {
            return;
        }
        marketVisualiserPane.getChildren().clear();
        List<SessionViewModel> sessions = buildSessionViewModels();
        List<ListingViewModel> listings = buildListingViewModels(sessions);

        int noDeals = 0;
        int accepted = 0;
        double dealTotal = 0;
        int dealRounds = 0;
        for (SessionViewModel session : sessions) {
            if ("ACCEPTED".equals(session.outcome)) {
                accepted++;
                dealTotal += session.latestPrice != null ? session.latestPrice : 0;
                dealRounds += session.rounds;
            } else if (!"NEGOTIATING".equals(session.outcome)) {
                noDeals++;
            }
        }
        double avgSettlement = accepted == 0 ? 0 : dealTotal / accepted;
        double avgRounds = accepted == 0 ? 0 : (double) dealRounds / accepted;

        HBox metrics = new HBox(10,
                createVisualMetric("Active sessions", String.valueOf(activeSessions), "currently negotiating"),
                createVisualMetric("Deals closed", String.valueOf(dealsClosed), accepted + " accepted in visualiser"),
                createVisualMetric("No-deals", String.valueOf(noDeals), "failed / timeout / walkaway"),
                createVisualMetric("Avg. settlement", money(avgSettlement), "successful deals"),
                createVisualMetric("Broker revenue", String.format("RM %.0f", totalRevenue), "fees + commissions"),
                createVisualMetric("Avg. rounds", String.format("%.1f", avgRounds), "accepted sessions"));
        for (Node node : metrics.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        VBox charts = new VBox(14,
                createChartCard("Price distribution per car model", createPriceDistributionChart(sessions),
                        createChartLegend(
                                createLegendSwatch("List price", "#f45a2a"),
                                createLegendSwatch("First offer", WARNING_ORANGE),
                                createLegendSwatch("Final deal", "#4caf50"))),
                createChartCard("Average concession by round", createConcessionTrendChart(sessions),
                        createChartLegend(
                                createLegendSwatch("Buyer movement", ACCENT_BLUE),
                                createLegendSwatch("Dealer movement", WARNING_ORANGE))));
        for (Node chartCard : charts.getChildren()) {
            VBox.setVgrow(chartCard, Priority.NEVER);
        }

        marketVisualiserPane.getChildren().addAll(metrics, charts,
                createTableCard("Live listing board", createListingBoard(listings)));
    }

    // Renders one selected negotiation session with timeline and round details.
    private void renderSessionVisualiser() {
        if (sessionVisualiserPane == null) {
            return;
        }
        sessionVisualiserPane.getChildren().clear();
        List<SessionViewModel> sessions = buildSessionViewModels();
        List<String> ids = new ArrayList<>();
        for (SessionViewModel session : sessions) {
            ids.add(session.sessionId);
        }

        String current = visualiserSessionSelect != null ? visualiserSessionSelect.getValue() : null;
        visualiserSessionSelect = new ComboBox<>();
        visualiserSessionSelect.getItems().setAll(ids);
        if (current != null && ids.contains(current)) {
            visualiserSessionSelect.setValue(current);
        } else if (!ids.isEmpty()) {
            visualiserSessionSelect.setValue(ids.get(0));
        }
        visualiserSessionSelect.setStyle(comboBoxStyle());
        visualiserSessionSelect.setOnAction(e -> renderSessionVisualiser());

        HBox controls = new HBox(10, makeSmallLabel("Session:"), visualiserSessionSelect);
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        sessionVisualiserPane.getChildren().add(controls);

        SessionViewModel selected = null;
        for (SessionViewModel session : sessions) {
            if (session.sessionId.equals(visualiserSessionSelect.getValue())) {
                selected = session;
                break;
            }
        }
        if (selected == null) {
            sessionVisualiserPane.getChildren().add(createEmptyState("No sessions yet. Run Demo Setup, then press Start."));
            return;
        }

        Label badge = createBadge(selected.outcome, outcomeColor(selected.outcome));
        HBox metrics = new HBox(10,
                createVisualMetric("List price", money(selected.listPrice), "highest dealer ask"),
                createVisualMetric("Reserve price", money(selected.dealerReserve), "dealer minimum"),
                createVisualMetric("Current / final", money(selected.latestPrice), "latest brokered offer"),
                createVisualMetric("Total concession", money(selected.totalConcession), "list minus latest"),
                createVisualMetric("Rounds", String.valueOf(selected.rounds), "broker messages"));
        for (Node node : metrics.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        HBox titleRow = new HBox(8,
                makeSmallLabel(selected.sessionId + " | " + valueOrNA(selected.buyer) + " / "
                        + valueOrNA(selected.dealer) + " | " + valueOrNA(selected.car)),
                badge);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        sessionVisualiserPane.getChildren().addAll(titleRow, metrics,
                createChartCard("Offer timeline", createOfferTimelineChart(selected),
                        createChartLegend(
                                createLegendSwatch("List price", "#94a3b8"),
                                createLegendSwatch("Buyer reserve", "#64748b"),
                                createLegendSwatch("Dealer reserve", WARNING_ORANGE),
                                createLegendSwatch("Buyer offer", ACCENT_BLUE),
                                createLegendSwatch("Dealer counter", SUCCESS_GREEN))),
                createTableCard("Round-by-round log", createRoundLog(selected)));
    }

    // Renders agent-level outcome and concession summaries.
    private void renderAgentVisualiser() {
        if (agentVisualiserPane == null) {
            return;
        }
        agentVisualiserPane.getChildren().clear();
        try {
            String current = visualiserAgentTypeSelect != null ? visualiserAgentTypeSelect.getValue() : "All agents";
            visualiserAgentTypeSelect = new ComboBox<>();
            visualiserAgentTypeSelect.getItems().addAll("All agents", "Buyers", "Dealers");
            visualiserAgentTypeSelect.setValue(current != null ? current : "All agents");
            visualiserAgentTypeSelect.setStyle(comboBoxStyle());
            visualiserAgentTypeSelect.setOnAction(e -> renderAgentVisualiser());

            HBox controls = new HBox(10, makeSmallLabel("Type:"), visualiserAgentTypeSelect);
            controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            agentVisualiserPane.getChildren().add(controls);

            List<AgentViewModel> allAgents = buildAgentViewModels();
            List<AgentViewModel> agents = new ArrayList<>(allAgents);
            String filter = visualiserAgentTypeSelect.getValue();
            agents.removeIf(agent -> "Buyers".equals(filter) && !"buyer".equals(agent.type)
                    || "Dealers".equals(filter) && !"dealer".equals(agent.type));

            Node performanceContent = agents.isEmpty()
                    ? createEmptyState(agentEmptyMessage(filter, allAgents.isEmpty()))
                    : createAgentPerformanceList(agents);
            Node outcomeContent = agents.isEmpty()
                    ? createEmptyState("No outcome data to chart for this filter.")
                    : createAgentOutcomeSummary(agents);
            Node concessionContent = agents.isEmpty()
                    ? createEmptyState("No concession movement to chart for this filter.")
                    : createAgentConcessionChart(agents);

            HBox top = new HBox(14,
                    createTableCard("Agent performance", performanceContent),
                    createChartCard("Negotiation outcomes by agent", outcomeContent));
            HBox.setHgrow(top.getChildren().get(0), Priority.ALWAYS);
            HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS);

            agentVisualiserPane.getChildren().addAll(top,
                    createChartCard("Concession strategy", concessionContent));
        } catch (Exception ex) {
            System.err.println("Agent View render failed: " + ex.getMessage());
            ex.printStackTrace(System.err);
            agentVisualiserPane.getChildren().clear();
            agentVisualiserPane.getChildren().add(createEmptyState(
                    "Agent View failed to render: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())));
        }
    }

    // Creates a compact metric tile for visualiser dashboards.
    private VBox createVisualMetric(String title, String value, String sub) {
        VBox card = new VBox(3);
        card.setMinWidth(120);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle(SOFT_PANEL_STYLE);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-weight: 700;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 20; -fx-text-fill: " + DARK_TEXT + "; -fx-font-weight: 800;");
        Label subLabel = new Label(sub);
        subLabel.setStyle("-fx-font-size: 10; -fx-text-fill: " + TEXT_MUTED + ";");
        card.getChildren().addAll(titleLabel, valueLabel, subLabel);
        return card;
    }

    // Creates a chart card without a legend.
    private VBox createChartCard(String title, Node chart) {
        return createChartCard(title, chart, null);
    }

    // Creates a chart card with an optional legend node.
    private VBox createChartCard(String title, Node chart, Node legend) {
        VBox card = new VBox(8);
        card.getChildren().addAll(createSectionLabel(title), chart);
        if (legend != null) {
            card.getChildren().add(legend);
        }
        card.setPadding(new Insets(14));
        double chartHeight = chart instanceof Region ? ((Region) chart).getPrefHeight() : Region.USE_COMPUTED_SIZE;
        if (chartHeight == Region.USE_COMPUTED_SIZE || chartHeight < 1) {
            chartHeight = chart instanceof Chart ? 300 : 220;
        }
        double cardHeight = chart instanceof Chart ? chartHeight + (legend != null ? 76 : 48) : Region.USE_COMPUTED_SIZE;
        card.setMinHeight(chart instanceof Chart ? cardHeight : 260);
        card.setPrefHeight(chart instanceof Chart ? cardHeight : Region.USE_COMPUTED_SIZE);
        card.setStyle(SOFT_PANEL_STYLE);
        if (chart instanceof Chart) {
            chart.setManaged(true);
            chart.setVisible(true);
        }
        VBox.setVgrow(chart, chart instanceof Chart ? Priority.NEVER : Priority.ALWAYS);
        return card;
    }

    // Places chart legend items in a shared horizontal layout.
    private HBox createChartLegend(Node... items) {
        HBox legend = new HBox(16, items);
        legend.setAlignment(javafx.geometry.Pos.CENTER);
        legend.setPadding(new Insets(4, 0, 0, 0));
        legend.setMinHeight(24);
        legend.setStyle("-fx-background-color: transparent;");
        return legend;
    }

    // Creates a styled card for table-like visualiser content.
    private VBox createTableCard(String title, Node content) {
        VBox card = new VBox(8, createSectionLabel(title), content);
        card.setPadding(new Insets(14));
        card.setStyle(SOFT_PANEL_STYLE);
        return card;
    }

    // Applies shared sizing, legend, and axis styling to a JavaFX chart.
    private void configureChart(Chart chart, Axis<?> xAxis, Axis<?> yAxis, double height) {
        chart.setAnimated(false);
        chart.setMinWidth(0);
        chart.setMinHeight(height);
        chart.setPrefHeight(height);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.setPadding(new Insets(8, 12, 8, 8));
        chart.setLegendSide(Side.BOTTOM);
        chart.setStyle("-fx-font-family: " + FONT_FAMILY + ";"
                + "-fx-text-fill: " + DARK_TEXT + ";"
                + "-fx-background-color: transparent;");
        configureAxis(xAxis);
        configureAxis(yAxis);
    }

    // Applies shared tick-label styling to a chart axis.
    private void configureAxis(Axis<?> axis) {
        axis.setVisible(true);
        axis.setManaged(true);
        axis.setTickLabelsVisible(true);
        axis.setTickMarkVisible(true);
        axis.setStyle("-fx-tick-label-fill: " + TEXT_MUTED + ";"
                + "-fx-font-size: 10;"
                + "-fx-text-fill: " + DARK_TEXT + ";");
    }

    // Bounds a number axis around observed values with a readable tick unit.
    private void boundNumberAxis(NumberAxis axis, List<Double> values, double fallbackMin, double fallbackMax) {
        if (values.isEmpty()) {
            axis.setAutoRanging(false);
            axis.setLowerBound(fallbackMin);
            axis.setUpperBound(fallbackMax);
            axis.setTickUnit(Math.max(1, (fallbackMax - fallbackMin) / 5));
            return;
        }
        double min = Collections.min(values);
        double max = Collections.max(values);
        if (Math.abs(max - min) < 1) {
            max = min + 1;
        }
        double span = max - min;
        double padding = Math.max(span * 0.12, 1000);
        double lower = Math.max(0, min - padding);
        double upper = max + padding;
        double tick = niceTick((upper - lower) / 5.0);
        axis.setAutoRanging(false);
        axis.setLowerBound(Math.floor(lower / tick) * tick);
        axis.setUpperBound(Math.ceil(upper / tick) * tick);
        axis.setTickUnit(tick);
    }

    // Rounds a rough tick interval to a clean chart tick value.
    private double niceTick(double roughTick) {
        if (roughTick <= 0) {
            return 1;
        }
        double exponent = Math.pow(10, Math.floor(Math.log10(roughTick)));
        double fraction = roughTick / exponent;
        double niceFraction;
        if (fraction <= 1) {
            niceFraction = 1;
        } else if (fraction <= 2) {
            niceFraction = 2;
        } else if (fraction <= 5) {
            niceFraction = 5;
        } else {
            niceFraction = 10;
        }
        return niceFraction * exponent;
    }

    // Installs a tooltip on a chart data node once JavaFX creates it.
    private void installTooltip(XYChart.Data<?, ?> data, String text) {
        Platform.runLater(() -> {
            Node node = data.getNode();
            if (node != null) {
                Tooltip tooltip = new Tooltip(text);
                tooltip.setShowDelay(javafx.util.Duration.millis(80));
                Tooltip.install(node, tooltip);
            }
        });
    }

    // Applies stroke color and optional dash styling to a line chart series.
    private void styleLineSeries(LineChart<?, ?> chart, XYChart.Series<?, ?> series, String color, boolean dashed) {
        Platform.runLater(() -> {
            Node line = series.getNode();
            if (line != null) {
                line.setStyle("-fx-stroke: " + color + "; -fx-stroke-width: " + (dashed ? "1.5" : "2.5")
                        + (dashed ? "; -fx-stroke-dash-array: 8 6;" : ";"));
            }
            for (XYChart.Data<?, ?> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-background-color: " + color + ", white;"
                            + "-fx-background-radius: 6px;"
                            + "-fx-padding: " + (dashed ? "3px" : "4px") + ";");
                }
            }
            chart.requestLayout();
        });
    }

    // Creates the market chart comparing list price, first offer, and final/latest price.
    private BarChart<String, Number> createPriceDistributionChart(List<SessionViewModel> sessions) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Car model / session");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price (RM)");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        configureChart(chart, xAxis, yAxis, 300);
        chart.setLegendVisible(false);
        chart.setCategoryGap(20);
        chart.setBarGap(4);

        XYChart.Series<String, Number> list = new XYChart.Series<>();
        list.setName("List price");
        XYChart.Series<String, Number> first = new XYChart.Series<>();
        first.setName("First offer");
        XYChart.Series<String, Number> finalDeal = new XYChart.Series<>();
        finalDeal.setName("Final deal");

        List<Double> yValues = new ArrayList<>();
        for (SessionViewModel session : sessions) {
            String label = shortLabel(session.car, session.sessionId, 14);
            String tooltipBase = "Session: " + session.sessionId + "\nCar: " + valueOrNA(session.car)
                    + "\nBuyer: " + valueOrNA(session.buyer) + "\nDealer: " + valueOrNA(session.dealer);
            if (session.listPrice != null) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(label, session.listPrice);
                list.getData().add(data);
                yValues.add(session.listPrice.doubleValue());
                installTooltip(data, tooltipBase + "\nList price: " + money(session.listPrice));
            }
            if (session.firstOffer != null) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(label, session.firstOffer);
                first.getData().add(data);
                yValues.add(session.firstOffer.doubleValue());
                installTooltip(data, tooltipBase + "\nFirst offer: " + money(session.firstOffer));
            }
            if ("ACCEPTED".equals(session.outcome) && session.latestPrice != null) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(label, session.latestPrice);
                finalDeal.getData().add(data);
                yValues.add(session.latestPrice);
                installTooltip(data, tooltipBase + "\nFinal deal: " + money(session.latestPrice));
            }
        }
        chart.getData().addAll(list, first, finalDeal);
        boundNumberAxis(yAxis, yValues, 0, 200000);
        return chart;
    }

    // Creates an aggregate chart of buyer and dealer price movement over rounds.
    private LineChart<Number, Number> createConcessionTrendChart(List<SessionViewModel> sessions) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Round");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Concession (RM)");
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        configureChart(chart, xAxis, yAxis, 300);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);

        XYChart.Series<Number, Number> buyer = new XYChart.Series<>();
        buyer.setName("Buyer movement");
        XYChart.Series<Number, Number> dealer = new XYChart.Series<>();
        dealer.setName("Dealer movement");
        Map<Integer, double[]> buyerAgg = new HashMap<>();
        Map<Integer, double[]> dealerAgg = new HashMap<>();

        for (SessionViewModel session : sessions) {
            List<TrajectoryPoint> pts = session.points;
            for (int i = 1; i < pts.size(); i++) {
                TrajectoryPoint prev = pts.get(i - 1);
                TrajectoryPoint cur = pts.get(i);
                if (isOutcomeEvent(cur.event)) {
                    continue;
                }
                double delta = Math.abs(cur.price - prev.price);
                Map<Integer, double[]> target = isDealerPoint(cur, session) ? dealerAgg : buyerAgg;
                double[] agg = target.computeIfAbsent(i, k -> new double[2]);
                agg[0] += delta;
                agg[1]++;
            }
        }
        addAverageSeriesData(buyer, buyerAgg);
        addAverageSeriesData(dealer, dealerAgg);
        chart.getData().addAll(buyer, dealer);
        List<Double> yValues = new ArrayList<>();
        for (XYChart.Data<Number, Number> data : buyer.getData()) {
            yValues.add(data.getYValue().doubleValue());
            installTooltip(data, "Buyer average movement\nRound: " + data.getXValue()
                    + "\nConcession: " + money(data.getYValue().doubleValue()));
        }
        for (XYChart.Data<Number, Number> data : dealer.getData()) {
            yValues.add(data.getYValue().doubleValue());
            installTooltip(data, "Dealer average movement\nRound: " + data.getXValue()
                    + "\nConcession: " + money(data.getYValue().doubleValue()));
        }
        boundNumberAxis(yAxis, yValues, 0, 50000);
        styleLineSeries(chart, buyer, ACCENT_BLUE, false);
        styleLineSeries(chart, dealer, WARNING_ORANGE, false);
        return chart;
    }

    // Creates the selected session's round-by-round offer timeline.
    private LineChart<String, Number> createOfferTimelineChart(SessionViewModel session) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Round");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price (RM)");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        configureChart(chart, xAxis, yAxis, 360);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);

        XYChart.Series<String, Number> buyer = new XYChart.Series<>();
        buyer.setName(valueOrNA(session.buyer) + " offers");
        XYChart.Series<String, Number> dealer = new XYChart.Series<>();
        dealer.setName(valueOrNA(session.dealer) + " counters");
        XYChart.Series<String, Number> list = new XYChart.Series<>();
        list.setName("List price");
        XYChart.Series<String, Number> buyerReserve = new XYChart.Series<>();
        buyerReserve.setName("Buyer reserve");
        XYChart.Series<String, Number> reserve = new XYChart.Series<>();
        reserve.setName("Dealer reserve");

        List<Double> yValues = new ArrayList<>();
        for (int i = 0; i < session.points.size(); i++) {
            TrajectoryPoint point = session.points.get(i);
            String round = "R" + (i + 1);
            if (isBuyerPoint(point, session)) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(round, point.price);
                buyer.getData().add(data);
                yValues.add(point.price);
                installTooltip(data, "Buyer offer\nRound: " + round + "\nPrice: " + money(point.price));
            } else if (isDealerPoint(point, session)) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(round, point.price);
                dealer.getData().add(data);
                yValues.add(point.price);
                installTooltip(data, "Dealer counter\nRound: " + round + "\nPrice: " + money(point.price));
            }
            if (session.listPrice != null) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(round, session.listPrice);
                list.getData().add(data);
                yValues.add(session.listPrice.doubleValue());
                installTooltip(data, "List price baseline\nPrice: " + money(session.listPrice));
            }
            if (session.buyerReserve != null) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(round, session.buyerReserve);
                buyerReserve.getData().add(data);
                yValues.add(session.buyerReserve.doubleValue());
                installTooltip(data, "Buyer reserve baseline\nPrice: " + money(session.buyerReserve));
            }
            if (session.dealerReserve != null) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(round, session.dealerReserve);
                reserve.getData().add(data);
                yValues.add(session.dealerReserve.doubleValue());
                installTooltip(data, "Dealer reserve baseline\nPrice: " + money(session.dealerReserve));
            }
        }
        chart.getData().addAll(list, buyerReserve, reserve, buyer, dealer);
        boundNumberAxis(yAxis, yValues, 0, 200000);
        styleLineSeries(chart, list, "#94a3b8", true);
        styleLineSeries(chart, buyerReserve, "#64748b", true);
        styleLineSeries(chart, reserve, WARNING_ORANGE, true);
        styleLineSeries(chart, buyer, ACCENT_BLUE, false);
        styleLineSeries(chart, dealer, SUCCESS_GREEN, false);
        return chart;
    }

    // Builds a ranked summary of agent outcomes and pending sessions.
    private Node createAgentOutcomeSummary(List<AgentViewModel> agents) {
        List<AgentViewModel> ranked = new ArrayList<>(agents);
        ranked.sort((a, b) -> {
            int sessionsCompare = Integer.compare(b.sessions, a.sessions);
            if (sessionsCompare != 0) {
                return sessionsCompare;
            }
            int acceptedCompare = Integer.compare(b.accepted, a.accepted);
            if (acceptedCompare != 0) {
                return acceptedCompare;
            }
            return a.name.compareToIgnoreCase(b.name);
        });

        AgentViewModel selected = ranked.get(0);
        VBox detail = new VBox(5,
                makeSmallLabel("Selected agent"),
                createOutcomeDetailLine(selected.name, selected.type),
                createOutcomeDetailLine("Sessions", String.valueOf(selected.sessions)),
                createOutcomeDetailLine("Accepted / no-deal / pending",
                        selected.accepted + " / " + selected.rejected + " / " + selected.pending),
                createOutcomeDetailLine("Close rate", String.format("%.0f%%",
                        selected.sessions == 0 ? 0 : (double) selected.accepted / selected.sessions * 100)),
                createOutcomeDetailLine("Avg. concession", money(selected.averageConcession)));
        detail.setPadding(new Insets(10));
        detail.setStyle("-fx-background-color: " + SURFACE_ALT + "; -fx-background-radius: 8;");

        HBox legend = new HBox(10,
                createLegendSwatch("Accepted", SUCCESS_GREEN),
                createLegendSwatch("No-deal", ERROR_RED),
                createLegendSwatch("Pending", WARNING_ORANGE));
        legend.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox rows = new VBox(8);
        int limit = Math.min(8, ranked.size());
        for (int i = 0; i < limit; i++) {
            rows.getChildren().add(createOutcomeSummaryRow(ranked.get(i)));
        }
        if (ranked.size() > limit) {
            AgentViewModel others = new AgentViewModel();
            others.name = "Others (" + (ranked.size() - limit) + ")";
            others.type = "mixed";
            for (int i = limit; i < ranked.size(); i++) {
                AgentViewModel agent = ranked.get(i);
                others.sessions += agent.sessions;
                others.accepted += agent.accepted;
                others.rejected += agent.rejected;
                others.pending += agent.pending;
            }
            rows.getChildren().add(createOutcomeSummaryRow(others));
        }

        VBox box = new VBox(10, detail, legend, rows);
        box.setMinHeight(260);
        box.setPrefHeight(300);
        return box;
    }

    // Creates one label/value row for the outcome summary details.
    private HBox createOutcomeDetailLine(String label, String value) {
        Label left = new Label(label + ":");
        left.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-weight: 700;");
        Label right = new Label(value);
        right.setWrapText(true);
        right.setStyle("-fx-font-size: 12; -fx-text-fill: " + DARK_TEXT + "; -fx-font-weight: 800;");
        HBox row = new HBox(6, left, right);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    // Creates a colored legend swatch with its label.
    private HBox createLegendSwatch(String label, String color) {
        Region swatch = new Region();
        swatch.setMinSize(10, 10);
        swatch.setPrefSize(10, 10);
        swatch.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3;");
        Label text = new Label(label);
        text.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_MUTED + ";");
        return new HBox(5, swatch, text);
    }

    // Creates one stacked outcome row for an agent.
    private HBox createOutcomeSummaryRow(AgentViewModel agent) {
        int total = Math.max(1, agent.accepted + agent.rejected + agent.pending);
        Label name = new Label(shortLabel(agent.name, agent.name, 18));
        name.setMinWidth(150);
        name.setStyle("-fx-font-size: 12; -fx-font-weight: 800; -fx-text-fill: " + DARK_TEXT + ";");
        Tooltip.install(name, new Tooltip(agent.name + "\n" + agent.type));

        HBox stack = new HBox(0,
                outcomeSegment(agent.accepted, total, SUCCESS_GREEN),
                outcomeSegment(agent.rejected, total, ERROR_RED),
                outcomeSegment(agent.pending, total, WARNING_ORANGE));
        stack.setMinWidth(220);
        stack.setPrefWidth(220);
        stack.setMaxWidth(220);
        stack.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 999;");

        Label counts = new Label(agent.accepted + " won | " + agent.rejected + " no-deal | " + agent.pending + " pending");
        counts.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_MUTED + ";");
        HBox row = new HBox(10, name, stack, counts);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 0, 5, 0));
        return row;
    }

    // Creates a proportional colored segment for the outcome bar.
    private Region outcomeSegment(int value, int total, String color) {
        Region segment = new Region();
        double width = value == 0 ? 0 : Math.max(8, 220.0 * value / total);
        segment.setMinWidth(width);
        segment.setPrefWidth(width);
        segment.setMaxWidth(width);
        segment.setMinHeight(12);
        segment.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 999;");
        return segment;
    }

    // Creates a chart of average concession movement by agent.
    private BarChart<String, Number> createAgentConcessionChart(List<AgentViewModel> agents) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Agent");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Avg movement (RM)");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        configureChart(chart, xAxis, yAxis, 300);
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<AgentViewModel> ranked = new ArrayList<>(agents);
        ranked.sort((a, b) -> Double.compare(b.averageConcession, a.averageConcession));
        List<Double> yValues = new ArrayList<>();
        int limit = Math.min(8, ranked.size());
        for (int i = 0; i < limit; i++) {
            AgentViewModel agent = ranked.get(i);
            XYChart.Data<String, Number> data = new XYChart.Data<>(shortLabel(agent.name, agent.name, 12),
                    agent.averageConcession);
            series.getData().add(data);
            yValues.add(agent.averageConcession);
            installTooltip(data, agent.name + "\nAvg movement: " + money(agent.averageConcession));
        }
        if (ranked.size() > limit) {
            double total = 0;
            int count = 0;
            for (int i = limit; i < ranked.size(); i++) {
                total += ranked.get(i).averageConcession;
                count++;
            }
            double average = count == 0 ? 0 : total / count;
            XYChart.Data<String, Number> data = new XYChart.Data<>("Others", average);
            series.getData().add(data);
            yValues.add(average);
            installTooltip(data, "Others (" + count + " agents)\nAvg movement: " + money(average));
        }
        chart.getData().add(series);
        boundNumberAxis(yAxis, yValues, 0, 25000);
        return chart;
    }

    // Creates the market listing table shown in the visualiser.
    private GridPane createListingBoard(List<ListingViewModel> listings) {
        GridPane grid = tableGrid();
        addTableHeader(grid, 0, "Car", "Dealer", "List price", "Reserve", "Buyers", "Status");
        int row = 1;
        for (ListingViewModel listing : listings) {
            addTableRow(grid, row++,
                    valueOrNA(listing.car),
                    valueOrNA(listing.dealer),
                    money(listing.listPrice),
                    money(listing.reserve),
                    String.valueOf(listing.activeBuyers),
                    listing.status);
        }
        if (listings.isEmpty()) {
            addTableRow(grid, 1, "No listings yet", "Register dealers", "-", "-", "-", "listed");
        }
        return grid;
    }

    // Creates a compact table of offer/counter rounds for one session.
    private GridPane createRoundLog(SessionViewModel session) {
        GridPane grid = tableGrid();
        addTableHeader(grid, 0, "Round", "Agent", "Action", "Offer", "Delta");
        double prev = 0;
        for (int i = 0; i < session.points.size(); i++) {
            TrajectoryPoint point = session.points.get(i);
            double delta = i == 0 ? 0 : point.price - prev;
            prev = point.price;
            addTableRow(grid, i + 1,
                    String.valueOf(i + 1),
                    valueOrNA(point.agent),
                    actionLabel(point.event),
                    money(point.price),
                    i == 0 ? "-" : signedMoney(delta));
        }
        return grid;
    }

    // Creates the agent performance list with close-rate progress bars.
    private Node createAgentPerformanceList(List<AgentViewModel> agents) {
        VBox list = new VBox(8);
        if (agents.isEmpty()) {
            list.getChildren().add(createEmptyState("No agents yet. Use Demo Setup or add buyers/dealers."));
            return list;
        }
        for (AgentViewModel agent : agents) {
            double closeRate = agent.sessions == 0 ? 0 : (double) agent.accepted / agent.sessions;
            Label avatar = new Label(initials(agent.name));
            avatar.setMinSize(34, 34);
            avatar.setAlignment(javafx.geometry.Pos.CENTER);
            avatar.setStyle("-fx-background-radius: 999; -fx-background-color: "
                    + ("buyer".equals(agent.type) ? "#dbeafe" : "#dcfce7")
                    + "; -fx-text-fill: " + ("buyer".equals(agent.type) ? ACCENT_BLUE : SUCCESS_GREEN)
                    + "; -fx-font-weight: 800;");
            ProgressBar bar = new ProgressBar(closeRate);
            bar.setMaxWidth(Double.MAX_VALUE);
            Label meta = new Label(agent.sessions + " sessions | " + agent.accepted + " accepted | "
                    + agent.rejected + " no-deal | " + agent.pending + " pending");
            meta.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_MUTED + ";");
            Label name = new Label(agent.name + "  " + agent.type);
            name.setStyle("-fx-font-size: 13; -fx-font-weight: 800; -fx-text-fill: " + DARK_TEXT + ";");
            VBox body = new VBox(3, name, bar, meta);
            HBox.setHgrow(body, Priority.ALWAYS);
            HBox row = new HBox(10, avatar, body);
            row.setPadding(new Insets(8));
            row.setStyle("-fx-background-color: " + SURFACE_ALT + "; -fx-background-radius: 8;");
            list.getChildren().add(row);
        }
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setMinHeight(220);
        scroll.setPrefHeight(360);
        scroll.setMaxHeight(380);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;"
                + "-fx-border-color: transparent; -fx-padding: 0;");
        return scroll;
    }

    // Returns the empty-state message for the selected agent filter.
    private String agentEmptyMessage(String filter, boolean noAgentsExist) {
        if (noAgentsExist) {
            return "No agents yet. Use Demo Setup or add buyers/dealers.";
        }
        if ("Buyers".equals(filter)) {
            return "No buyers match this filter.";
        }
        if ("Dealers".equals(filter)) {
            return "No dealers match this filter.";
        }
        return "No agents match this filter.";
    }

    // Creates a shared grid layout for dashboard tables.
    private GridPane tableGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(4));
        return grid;
    }

    // Adds a styled header row to a dashboard table grid.
    private void addTableHeader(GridPane grid, int row, String... labels) {
        for (int col = 0; col < labels.length; col++) {
            Label label = new Label(labels[col]);
            label.setStyle("-fx-font-size: 11; -fx-font-weight: 800; -fx-text-fill: " + TEXT_MUTED + ";");
            grid.add(label, col, row);
        }
    }

    // Adds a styled data row to a dashboard table grid.
    private void addTableRow(GridPane grid, int row, String... values) {
        for (int col = 0; col < values.length; col++) {
            Label label = new Label(values[col]);
            label.setWrapText(true);
            label.setStyle("-fx-font-size: 12; -fx-text-fill: " + DARK_TEXT + ";");
            grid.add(label, col, row);
        }
    }

    // Creates a muted empty-state label for visualiser panels.
    private Node createEmptyState(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 13; -fx-text-fill: " + TEXT_MUTED + "; -fx-padding: 18;"
                + "-fx-background-color: " + SURFACE_ALT + "; -fx-background-radius: 8;");
        return label;
    }

    // Builds sorted session view models from parsed metadata and trajectory points.
    private List<SessionViewModel> buildSessionViewModels() {
        List<String> ids = new ArrayList<>(sessionMetaMap.keySet());
        for (String id : sessionPoints.keySet()) {
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        Collections.sort(ids);
        List<SessionViewModel> models = new ArrayList<>();
        for (String id : ids) {
            SessionMeta meta = sessionMetaMap.get(id);
            SessionViewModel vm = new SessionViewModel();
            vm.sessionId = id;
            vm.buyer = meta != null ? meta.buyer : null;
            vm.dealer = meta != null ? meta.dealer : null;
            vm.car = meta != null ? meta.car : null;
            vm.buyerReserve = meta != null ? meta.buyerReserve : null;
            vm.dealerReserve = meta != null ? meta.dealerReserve : null;
            vm.firstOffer = meta != null ? meta.firstOffer : null;
            vm.outcome = meta != null && meta.outcomeStatus != null ? meta.outcomeStatus : "NEGOTIATING";
            vm.failureReason = meta != null ? meta.failureReason : null;
            vm.points.addAll(sessionPoints.getOrDefault(id, Collections.emptyList()));
            vm.points.sort(Comparator.comparingInt(p -> p.cycle));
            applyListingData(vm);
            vm.rounds = vm.points.size();
            for (TrajectoryPoint point : vm.points) {
                if (vm.car == null) vm.car = point.car;
                vm.latestPrice = point.price;
                if (isOutcomeEvent(point.event)) vm.outcome = point.event == TrajectoryEvent.ACCEPT ? "ACCEPTED" : "NO DEAL";
            }
            applyListingData(vm);
            if (meta != null && meta.outcomePrice != null) vm.latestPrice = meta.outcomePrice;
            if (vm.listPrice == null) {
                vm.listPrice = maxPointPrice(vm.points);
            }
            if (vm.listPrice != null && vm.latestPrice != null) vm.totalConcession = Math.max(0, vm.listPrice - vm.latestPrice);
            models.add(vm);
        }
        return models;
    }

    // Adds listing price and reserve data to a session model when available.
    private void applyListingData(SessionViewModel vm) {
        if (vm == null || vm.dealer == null || vm.car == null) {
            return;
        }
        ListingViewModel listing = listingModelMap.get(listingKey(vm.dealer, vm.car));
        if (listing == null) {
            return;
        }
        if (vm.listPrice == null) {
            vm.listPrice = listing.listPrice;
        }
        if (vm.dealerReserve == null) {
            vm.dealerReserve = listing.reserve;
        }
    }

    // Returns the highest price observed in a point list, or null when empty.
    private Integer maxPointPrice(List<TrajectoryPoint> points) {
        if (points == null || points.isEmpty()) {
            return null;
        }
        double max = 0;
        for (TrajectoryPoint point : points) {
            max = Math.max(max, point.price);
        }
        return max > 0 ? (int) Math.round(max) : null;
    }

    // Builds per-agent metrics from sessions, listings, and parsed agent names.
    private List<AgentViewModel> buildAgentViewModels() {
        List<SessionViewModel> sessions = buildSessionViewModels();
        Set<String> names = new LinkedHashSet<>();
        Set<String> dealerNames = new HashSet<>();

        addCleanAgentNames(names, buyerAgents);
        addCleanAgentNames(names, dealerAgents);
        addCleanAgentNames(dealerNames, dealerAgents);
        addCleanAgentNames(names, agentPoints.keySet());
        for (SessionViewModel session : sessions) {
            String buyer = cleanAgentName(session.buyer);
            String dealer = cleanAgentName(session.dealer);
            if (buyer != null) {
                names.add(buyer);
            }
            if (dealer != null) {
                names.add(dealer);
                dealerNames.add(dealer);
            }
        }

        List<String> sortedNames = new ArrayList<>(names);
        sortedNames.sort((a, b) -> {
            boolean aDealer = dealerNames.contains(a);
            boolean bDealer = dealerNames.contains(b);
            if (aDealer != bDealer) {
                return aDealer ? 1 : -1;
            }
            return a.compareToIgnoreCase(b);
        });

        List<AgentViewModel> models = new ArrayList<>();
        for (String name : sortedNames) {
            AgentViewModel vm = new AgentViewModel();
            vm.name = name;
            vm.type = dealerNames.contains(name) ? "dealer" : "buyer";
            for (SessionViewModel session : sessions) {
                String buyer = cleanAgentName(session.buyer);
                String dealer = cleanAgentName(session.dealer);
                if (!name.equals(buyer) && !name.equals(dealer)) continue;
                vm.sessions++;
                if ("ACCEPTED".equals(session.outcome)) {
                    vm.accepted++;
                } else if ("NEGOTIATING".equals(session.outcome)) {
                    vm.pending++;
                } else {
                    vm.rejected++;
                }
            }
            vm.averageConcession = averageMovement(agentPoints.getOrDefault(name, Collections.emptyList()));
            models.add(vm);
        }
        return models;
    }

    // Adds non-empty cleaned agent names from a source collection into a target set.
    private void addCleanAgentNames(Set<String> target, Iterable<String> source) {
        for (String name : source) {
            String cleaned = cleanAgentName(name);
            if (cleaned != null) {
                target.add(cleaned);
            }
        }
    }

    // Strips JADE address suffixes and whitespace from an agent name.
    private String cleanAgentName(String name) {
        if (name == null) {
            return null;
        }
        String cleaned = stripKnownFieldPrefix(trimBrokerRelationship(name)).trim();
        if (cleaned.endsWith(" buyer")) {
            cleaned = cleaned.substring(0, cleaned.length() - " buyer".length()).trim();
        } else if (cleaned.endsWith(" dealer")) {
            cleaned = cleaned.substring(0, cleaned.length() - " dealer".length()).trim();
        }
        return cleaned.isBlank() ? null : cleaned;
    }

    // Builds listing models with active buyer counts and sold-out state from an existing session snapshot.
    private List<ListingViewModel> buildListingViewModels(List<SessionViewModel> sessions) {
        Map<String, ListingViewModel> listings = new LinkedHashMap<>();
        for (Map.Entry<String, ListingViewModel> entry : listingModelMap.entrySet()) {
            ListingViewModel copy = copyListing(entry.getValue());
            copy.activeBuyers = 0;
            listings.put(entry.getKey(), copy);
        }

        Map<String, Set<String>> activeBuyersByListing = new HashMap<>();
        for (SessionViewModel session : sessions) {
            if (session.dealer == null || session.dealer.isBlank() || session.car == null || session.car.isBlank()) {
                continue;
            }
            String key = listingKey(session.dealer, session.car);
            ListingViewModel vm = listings.computeIfAbsent(key, k -> {
                ListingViewModel listing = new ListingViewModel();
                listing.dealer = session.dealer;
                listing.car = session.car;
                listing.status = "listed";
                return listing;
            });
            if (vm.listPrice == null && session.listPrice != null) vm.listPrice = session.listPrice;
            if (vm.reserve == null && session.dealerReserve != null) vm.reserve = session.dealerReserve;
            if ("NEGOTIATING".equals(session.outcome)) {
                activeBuyersByListing.computeIfAbsent(key, k -> new HashSet<>())
                        .add(session.buyer != null && !session.buyer.isBlank() ? session.buyer : session.sessionId);
                vm.status = "negotiating";
            } else if ("ACCEPTED".equals(session.outcome) && !"negotiating".equals(vm.status)) {
                vm.status = "closed";
            }
        }
        for (Map.Entry<String, Set<String>> entry : activeBuyersByListing.entrySet()) {
            ListingViewModel listing = listings.get(entry.getKey());
            if (listing != null) {
                listing.activeBuyers = entry.getValue().size();
            }
        }
        return new ArrayList<>(listings.values());
    }

    // Copies a stored listing model so rendering cannot mutate the cache.
    private ListingViewModel copyListing(ListingViewModel source) {
        ListingViewModel copy = new ListingViewModel();
        copy.car = source.car;
        copy.dealer = source.dealer;
        copy.listPrice = source.listPrice;
        copy.reserve = source.reserve;
        copy.activeBuyers = source.activeBuyers;
        copy.status = source.status;
        return copy;
    }

    // Records or updates a dealer listing from UI-created dealer data.
    private void recordDealerListing(String dealer, String car, int price, int stock, NegotiationConfig config) {
        ListingViewModel listing = new ListingViewModel();
        listing.dealer = dealer;
        listing.car = car;
        listing.listPrice = price;
        listing.reserve = (int) (price * config.getDealerReservePercent());
        listing.status = stock > 0 ? "listed" : "sold out";
        listingModelMap.put(listingKey(dealer, car), listing);
        refreshNegotiationVisualiser();
    }

    // Builds the map key used to identify a dealer/car listing.
    private String listingKey(String dealer, String car) {
        return valueOrNA(dealer) + "|" + valueOrNA(car);
    }

    // Returns whether a trajectory point belongs to the buyer side of a session.
    private boolean isBuyerPoint(TrajectoryPoint point, SessionViewModel session) {
        if (point.event == TrajectoryEvent.START || point.event == TrajectoryEvent.OFFER) return true;
        return session != null && session.buyer != null && session.buyer.equals(point.agent);
    }

    // Returns whether a trajectory point belongs to the dealer side of a session.
    private boolean isDealerPoint(TrajectoryPoint point, SessionViewModel session) {
        if (point.event == TrajectoryEvent.COUNTER || point.event == TrajectoryEvent.ACCEPT) return true;
        return session != null && session.dealer != null && session.dealer.equals(point.agent);
    }

    // Returns whether an event represents a terminal negotiation outcome.
    private boolean isOutcomeEvent(TrajectoryEvent event) {
        return event == TrajectoryEvent.ACCEPT || event == TrajectoryEvent.WALKAWAY;
    }

    // Adds averaged aggregate values to a numeric chart series.
    private void addAverageSeriesData(XYChart.Series<Number, Number> series, Map<Integer, double[]> values) {
        List<Integer> keys = new ArrayList<>(values.keySet());
        Collections.sort(keys);
        for (Integer key : keys) {
            double[] agg = values.get(key);
            if (agg[1] > 0) series.getData().add(new XYChart.Data<>(key, agg[0] / agg[1]));
        }
    }

    // Calculates average absolute price movement between consecutive points.
    private double averageMovement(List<TrajectoryPoint> points) {
        if (points.size() < 2) return 0;
        List<TrajectoryPoint> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.comparingInt(p -> p.cycle));
        double total = 0;
        for (int i = 1; i < sorted.size(); i++) total += Math.abs(sorted.get(i).price - sorted.get(i - 1).price);
        return total / (sorted.size() - 1);
    }

    // Returns shared CSS for compact ComboBox controls.
    private String comboBoxStyle() {
        return "-fx-font-size: 12; -fx-font-family: " + FONT_FAMILY + "; -fx-background-color: " + SURFACE
                + "; -fx-border-color: " + BORDER_SUBTLE
                + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 2 6;";
    }

    // Creates a small muted label for controls and metadata.
    private Label makeSmallLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12; -fx-font-weight: 700; -fx-text-fill: " + TEXT_MUTED + ";");
        return label;
    }

    // Creates a colored status badge label.
    private Label createBadge(String text, String color) {
        Label badge = new Label(text);
        badge.setStyle("-fx-font-size: 11; -fx-font-weight: 800; -fx-text-fill: white;"
                + "-fx-background-color: " + color + "; -fx-background-radius: 6; -fx-padding: 3 8;");
        return badge;
    }

    // Returns the display color associated with an outcome label.
    private String outcomeColor(String outcome) {
        if ("ACCEPTED".equals(outcome)) return SUCCESS_GREEN;
        if ("NEGOTIATING".equals(outcome)) return WARNING_ORANGE;
        return ERROR_RED;
    }

    // Shortens a display label using the default maximum length.
    private String shortLabel(String value, String fallback) {
        return shortLabel(value, fallback, 18);
    }

    // Shortens a display label to a supplied maximum length.
    private String shortLabel(String value, String fallback, int maxLength) {
        String label = value != null && !value.isBlank() ? value : fallback;
        if (label == null) {
            return "N/A";
        }
        return label.length() > maxLength ? label.substring(0, Math.max(1, maxLength - 1)) + "..." : label;
    }

    // Converts a trajectory event into a table-friendly action label.
    private String actionLabel(TrajectoryEvent event) {
        if (event == TrajectoryEvent.START) return "initial offer";
        if (event == TrajectoryEvent.OFFER) return "buyer offer";
        if (event == TrajectoryEvent.COUNTER) return "dealer counter";
        if (event == TrajectoryEvent.ACCEPT) return "accept";
        if (event == TrajectoryEvent.WALKAWAY) return "no deal";
        return "price update";
    }

    // Builds initials for an agent avatar label.
    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.split("[-_\\s]+");
        String first = parts.length > 0 && !parts[0].isBlank() ? parts[0].substring(0, 1) : "";
        String second = parts.length > 1 && !parts[1].isBlank() ? parts[1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }

    // Formats a nullable integer as Malaysian Ringgit text.
    private String money(Integer value) {
        return value == null ? "N/A" : "RM " + value;
    }

    // Formats a nullable double as Malaysian Ringgit text.
    private String money(Double value) {
        return value == null ? "N/A" : String.format("RM %.0f", value);
    }

    // Formats a primitive amount as Malaysian Ringgit text.
    private String money(double value) {
        return String.format("RM %.0f", value);
    }

    // Formats a signed Malaysian Ringgit delta.
    private String signedMoney(double value) {
        return (value >= 0 ? "+" : "-") + money(Math.abs(value));
    }


    // Refreshes the dealer count status label.
    private void updateDealerStatus() {
        if (dealerCount == 0) {
            dealerStatusLabel
                    .setText("Go to Dealer Portal → Register at least ONE dealer with car inventory (Required first!)");
            dealerStatusLabel.setStyle("-fx-font-size: 13; -fx-text-fill: " + ERROR_RED + "; -fx-font-weight: bold;");
        } else {
            dealerStatusLabel.setText("/ " + dealerCount + " dealer agent(s) registered - Ready to accept buyers!");
            dealerStatusLabel
                    .setStyle("-fx-font-size: 13; -fx-text-fill: " + SUCCESS_GREEN + "; -fx-font-weight: bold;");
        }
    }

    // Refreshes the buyer count status label.
    private void updateBuyerStatus() {
        if (buyerCount == 0) {
            updateBuyerStatus.setText("Go to Buyer Portal → Register buyer(s) with desired car & budget");
            updateBuyerStatus.setStyle("-fx-font-size: 13; -fx-text-fill: " + ERROR_RED + "; -fx-font-weight: bold;");
        } else {
            updateBuyerStatus.setText("/ " + buyerCount + " buyer agent(s) registered - Ready to accept dealers!");
            updateBuyerStatus
                    .setStyle("-fx-font-size: 13; -fx-text-fill: " + SUCCESS_GREEN + "; -fx-font-weight: bold;");
        }
    }

    // Creates a dashboard stat card bound to an existing value label.
    private VBox createStatCard(String title, Label valueLabel, String color) {
        VBox card = new VBox(4);
        card.setMinHeight(96);
        card.setPadding(new Insets(16, 18, 14, 18));
        card.setStyle("-fx-background-color: " + SURFACE + "; -fx-background-radius: 14;"
                + "-fx-border-color: #dbeafe #dbeafe " + color + " #dbeafe;"
                + "-fx-border-width: 1 1 4 1;"
                + "-fx-border-radius: 14; -fx-effect: " + CARD_SHADOW + ";");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-weight: 600;");
        valueLabel.setStyle("-fx-font-size: 28; -fx-font-weight: 700; -fx-text-fill: " + color + ";");
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    // Creates a bold section label used inside panels.
    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");
        return label;
    }

    // Creates a checklist label for the dashboard guidance panel.
    private Label createChecklistItem(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12; -fx-font-weight: 600; -fx-text-fill: " + DARK_TEXT + ";"
                + "-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 8 10;");
        return label;
    }

    // Builds the buyer registration form and validation flow.
    private VBox createBuyerView(UILogger logger) {
        VBox box = new VBox(18);
        box.setPadding(new Insets(4));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label headerLabel = new Label("Buyer Portal");
        headerLabel.setStyle("-fx-font-size: 22; -fx-font-weight: 700; -fx-text-fill: " + PRIMARY_BLUE + ";");
        Label subLabel = new Label(
                "Register a buyer agent with a desired car and maximum budget. The broker will find matching dealers.");
        subLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + TEXT_MUTED + "; -fx-wrap-text: true;");

        // Prerequisite banner
        HBox prereqBanner = new HBox(8);
        prereqBanner.setPadding(new Insets(12, 16, 12, 16));
        prereqBanner.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        prereqBanner.setStyle("-fx-background-color: #fff7ed; -fx-background-radius: 10;"
                + "-fx-border-color: #fdba74; -fx-border-radius: 10; -fx-border-width: 1;");
        Label prereqIcon = new Label("!");
        prereqIcon.setStyle("-fx-font-size: 11; -fx-font-weight: 800; -fx-text-fill: #c2410c;");
        Label prereqText = new Label("Register at least one dealer first so the broker can return matching listings.");
        prereqText.setStyle("-fx-font-size: 12; -fx-text-fill: #92400e; -fx-font-weight: 600;");
        prereqBanner.getChildren().addAll(prereqIcon, prereqText);

        // Form card
        VBox card = createPanel(16, new Insets(22));

        Label formTitle = new Label("Add Buyer Agent");
        formTitle.setStyle("-fx-font-size: 16; -fx-font-weight: 700; -fx-text-fill: " + DARK_TEXT + ";");

        GridPane form = new GridPane();
        configurePortalForm(form);

        TextField buyerName = createStyledTextField("e.g., Ali Hassan");
        ComboBox<String> carModel = createStyledCarComboBox();
        TextField budget = createStyledTextField("e.g., 120000");

        GridPane.setHgrow(buyerName, Priority.ALWAYS);
        GridPane.setHgrow(carModel, Priority.ALWAYS);
        GridPane.setHgrow(budget, Priority.ALWAYS);
        buyerName.setMaxWidth(Double.MAX_VALUE);
        carModel.setMaxWidth(Double.MAX_VALUE);
        budget.setMaxWidth(Double.MAX_VALUE);

        form.add(makeFieldLabel("Buyer Name", "Unique agent identifier"), 0, 0);
        form.add(buyerName, 1, 0);
        form.add(makeFieldLabel("Desired Car", "Car model to search for"), 0, 1);
        form.add(carModel, 1, 1);
        form.add(makeFieldLabel("Max Budget (RM)", "Upper limit — buyer opens at ~70%"), 0, 2);
        form.add(budget, 1, 2);

        CheckBox manualControlCheck = new CheckBox("Manual Negotiation Mode (Wait for my input)");
        manualControlCheck.setStyle("-fx-font-size: 13; -fx-text-fill: " + DARK_TEXT + ";");
        form.add(manualControlCheck, 1, 3);

        Button addBuyerBtn = createStyledButton("Add Buyer Agent", ACCENT_BLUE);
        addBuyerBtn.setMaxWidth(Double.MAX_VALUE);
        addBuyerBtn.setOnAction(e -> {
            String name = buyerName.getText().trim();
            String car = carModel.getValue() != null ? carModel.getValue() : "";
            String budgetStr = budget.getText().trim();
            if (name.isEmpty() || car.isEmpty() || budgetStr.isEmpty()) {
                showAlert("⚠️ All fields are required!", Alert.AlertType.WARNING);
                return;
            }
            if (dealerCount == 0) {
                showAlert("❌ No dealers registered!\nPlease register a dealer first.", Alert.AlertType.ERROR);
                return;
            }
            try {
                double b = Double.parseDouble(budgetStr);
                if (b <= 0) {
                    showAlert("❌ Budget must be > 0", Alert.AlertType.WARNING);
                    return;
                }
                boolean isManual = manualControlCheck.isSelected();
                cc.createNewAgent(name, "org.example.agents.BuyerAgent",
                        new Object[] { car, budgetStr, logger, buildNegotiationConfig(), true, isManual }).start();
                buyerAgents.add(name);
                if (isManual) {
                    manualBuyerAgents.add(name);
                    if (manualBuyerSelect != null && manualBuyerSelect.getValue() == null) {
                        manualBuyerSelect.setValue(name);
                    }
                }
                waitingBuyerAgents.add(name);
                updateNegotiationControlStatus();
                refreshNegotiationVisualiser();
                logger.log("Buyer '" + name + "' added — " + car + " @ RM" + budgetStr);
                buyerName.clear();
                carModel.setValue(null);
                budget.clear();
                showAlert("✅ Buyer " + name + " added. Press ▶ Start.", Alert.AlertType.INFORMATION);
            } catch (NumberFormatException ex) {
                showAlert("❌ Budget must be a valid number.", Alert.AlertType.ERROR);
            } catch (Exception ex) {
                logger.log("❌ Error: " + ex.getMessage());
                showAlert("❌ " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        form.add(addBuyerBtn, 1, 4);

        card.getChildren().addAll(formTitle, form);
        box.getChildren().addAll(headerLabel, subLabel, prereqBanner, card);
        VBox.setVgrow(card, Priority.SOMETIMES);
        return box;
    }

    // Creates a form field label with title and helper text.
    private VBox makeFieldLabel(String title, String subtitle) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13; -fx-text-fill: " + DARK_TEXT + "; -fx-font-weight: 600;");
        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_MUTED + ";");
        return new VBox(2, titleLbl, subLbl);
    }

    // Applies shared spacing and column sizing to buyer/dealer forms.
    private void configurePortalForm(GridPane form) {
        form.setHgap(24);
        form.setVgap(16);
        form.setMaxWidth(Double.MAX_VALUE);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(150);
        labels.setPrefWidth(172);
        labels.setMaxWidth(190);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setFillWidth(true);
        form.getColumnConstraints().setAll(labels, fields);
    }

    // Builds the horizontal action bar for simulation controls.
    private HBox createActionBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(12, 20, 12, 20));
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #e0f2fe; "
                + "-fx-border-color: #bae6fd; -fx-border-width: 0 0 1 0;");

        Button demoBtn = createBarButton("Demo Setup", PRIMARY_BLUE);
        Button startBtn = createBarButton("Start", SUCCESS_GREEN);
        playPauseBtn = createBarButton("Pause", WARNING_ORANGE);
        Button stepBtn = createBarButton("Step Cycle", ACCENT_BLUE);
        Button stopBtn = createBarButton("Stop", ERROR_RED);
        Button sniffBtn = createBarButton("Sniffer", "#4f46e5");

        demoBtn.setOnAction(e -> createDemoScenario());
        startBtn.setOnAction(e -> {
            if (waitingBuyerAgents.isEmpty()) {
                showAlert("No waiting buyers.", Alert.AlertType.INFORMATION);
                return;
            }
            for (String b : new ArrayList<>(waitingBuyerAgents))
                sendAgentCommand(b, "START_NEGOTIATION");
            loggerLog("Started " + waitingBuyerAgents.size() + " buyer(s).");
            waitingBuyerAgents.clear();
            isAutoPlay = true;
            playPauseBtn.setText("Pause");
            sendSpaceCommand("RESUME");
            updateNegotiationControlStatus();
            refreshNegotiationVisualiser();
        });
        playPauseBtn.setOnAction(e -> {
            toggleAutoplay();
            updateNegotiationControlStatus();
        });
        stepBtn.setOnAction(e -> sendSpaceCommand("STEP"));
        stopBtn.setOnAction(e -> {
            for (String b : new ArrayList<>(buyerAgents)) {
                sendAgentCommand(b, "STOP_NEGOTIATION");
            }
            waitingBuyerAgents.clear();
            loggerLog("Stop sent to buyer agents.");
            updateNegotiationControlStatus();
            refreshNegotiationVisualiser();
        });
        sniffBtn.setOnAction(e -> launchSniffer(msg -> logArea.appendText(msg + "\n")));

        // Tick positions 0-6 map to: 0.25×, 0.5×, 1× (default), 2×, 5×
        // Delay in ms: 4000, 2000, 1000, 500, 200, 100, 50
        long[] speedDelays = { 4000, 2000, 1000, 500, 200, 100, 50 };
        String[] speedLabels = { "0.25×", "0.5×", "1×", "2×", "5×", "10×", "20×" };

        Slider speedSlider = new Slider(0, speedDelays.length - 1, 2); // default = index 2 → 1000 ms
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setSnapToTicks(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(false);
        speedSlider.setPrefWidth(130);
        speedSlider.setStyle("-fx-padding: 0 4;");

        Label speedLabel = new Label("1×");
        speedLabel.setStyle("-fx-font-size: 11; -fx-font-weight: 700; -fx-text-fill: " + TEXT_MUTED
                + "; -fx-min-width: 34; -fx-alignment: center;");
        Tooltip speedTip = new Tooltip("Cycle delay: 1000 ms");
        Tooltip.install(speedSlider, speedTip);

        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int idx = (int) Math.round(newVal.doubleValue());
            idx = Math.max(0, Math.min(idx, speedDelays.length - 1));
            long delayMs = speedDelays[idx];
            speedLabel.setText(speedLabels[idx]);
            speedTip.setText("Cycle delay: " + delayMs + " ms");
            sendSpeedCommand(delayMs);
        });

        Label speedIconLabel = new Label("Speed");
        speedIconLabel.setStyle("-fx-font-size: 13; -fx-text-fill: " + TEXT_MUTED + ";");
        HBox speedBox = new HBox(4, speedIconLabel, speedSlider, speedLabel);
        speedBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        speedBox.setPadding(new Insets(0, 6, 0, 6));

        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep.setPadding(new Insets(0, 4, 0, 4));
        Separator sep2 = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep2.setPadding(new Insets(0, 4, 0, 4));

        negotiationControlStatusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + TEXT_MUTED + ";");
        updateNegotiationControlStatus();

        updateDealerStatus();
        updateBuyerStatus();

        bar.getChildren().addAll(demoBtn, startBtn, playPauseBtn, stepBtn, stopBtn, sniffBtn,
                sep2, speedBox,
                sep, negotiationControlStatusLabel);
        return bar;
    }

    // Sends a SET_SPEED command to SpaceControl with the desired cycle delay.
    private void sendSpeedCommand(long delayMs) {
        try {
            cc.createNewAgent(nextAgentName("speed-command"), "org.example.agents.SpaceCommandAgent",
                    new Object[] { "SET_SPEED", "space", String.valueOf(delayMs) }).start();
        } catch (Exception e) {
            System.err.println("Error sending SET_SPEED: " + e.getMessage());
        }
    }

    // Creates a colored button for the action bar.
    private Button createBarButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-font-size: 12; -fx-font-family: " + FONT_FAMILY + "; -fx-font-weight: 800; -fx-padding: 8 16; "
                + "-fx-background-color: " + color + "; -fx-text-fill: white; "
                + "-fx-background-radius: 10; -fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, rgba(30,64,175,0.16), 8, 0, 0, 2);");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    // Creates a repeatable set of demo dealers and buyers.
    private void createDemoScenario() {
        long demoId = demoScenarioCounter.incrementAndGet();
        NegotiationConfig config = buildDemoNegotiationConfig();

        try {
            String[][] dealers = new String[][] {
                    { "DemoAutoA-" + demoId, "Toyota Camry", "100000", "2" },
                    { "DemoAutoB-" + demoId, "Toyota Camry", "96000", "2" },
                    { "DemoAutoC-" + demoId, "Toyota Camry", "92000", "1" },
                    { "BudgetCars-" + demoId, "Honda Civic", "87000", "2" },
                    { "FamilyDrive-" + demoId, "Honda CR-V", "145000", "1" },
                    { "TruckHub-" + demoId, "Toyota Fortuner", "180000", "1" }
            };
            for (String[] dealer : dealers) {
                createDemoDealer(dealer[0], dealer[1], dealer[2], dealer[3], config);
            }

            String[][] buyers = new String[][] {
                    { "DemoBuyerPremium-" + demoId, "Toyota Camry", "116000" },
                    { "DemoBuyerStubborn-" + demoId, "Toyota Camry", "108000" },
                    { "DemoBuyerTight-" + demoId, "Toyota Camry", "98000" },
                    { "DemoBuyerCivic-" + demoId, "Honda Civic", "102000" },
                    { "DemoBuyerSUV-" + demoId, "Honda CR-V", "150000" },
                    { "DemoBuyerStretch-" + demoId, "Toyota Fortuner", "168000" },
                    { "DemoBuyerBudget-" + demoId, "Toyota Camry", "65000" },
                    { "DemoBuyerOverdrive-" + demoId, "Toyota Camry", "112000" }
            };
            for (String[] buyer : buyers) {
                createDemoBuyer(buyer[0], buyer[1], buyer[2], config);
            }

            updateNegotiationControlStatus();
            refreshNegotiationVisualiser();

            loggerLog("Demo scenario " + demoId + " added: " + dealers.length + " dealers and " + buyers.length
                    + " waiting buyers. Press Start to stress test negotiation and strategy switching.");
            showAlert("Demo scenario added. Press Start to begin negotiation.", Alert.AlertType.INFORMATION);
        } catch (Exception ex) {
            showAlert("❌ Error creating demo scenario: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Builds a fixed strategy config used by the demo scenario.
    private NegotiationConfig buildDemoNegotiationConfig() {
        NegotiationConfig base = buildNegotiationConfig();
        return new NegotiationConfig(
                base.getStrategy(),
                Math.max(base.getDeadlineCycles(), 30),
                Math.min(base.getBuyerStartPercent(), 0.62),
                Math.max(base.getDealerReservePercent(), 0.78),
                Math.max(base.getMaxRoundsPerDealer(), 6),
                Math.max(base.getMaxSearchRetries(), 1),
                Math.max(base.getStuckRoundsBeforeAcceleration(), 1),
                base.getManualDealerTargetPercent(),
                base.getStrategySwitchCycle() > 0 ? Math.min(base.getStrategySwitchCycle(), 2) : 2,
                base.getSwitchStrategy() == base.getStrategy() ? NegotiationConfig.Strategy.CONCEDER
                        : base.getSwitchStrategy());
    }

    // Starts a dealer agent for the demo scenario and records its listing.
    private void createDemoDealer(String name, String car, String price, String stock, NegotiationConfig config)
            throws Exception {
        cc.createNewAgent(name, "org.example.agents.DealerAgent",
                new Object[] { car, price, stock, appLogger, config }).start();
        loggerLog("Dealer '" + name + "' listed " + car + " @ RM" + price + " | Stock: " + stock);
        dealerAgents.add(name);
        recordDealerListing(name, car, Integer.parseInt(price), Integer.parseInt(stock), config);
    }

    // Starts a buyer agent for the demo scenario.
    private void createDemoBuyer(String name, String car, String budget, NegotiationConfig config) throws Exception {
        cc.createNewAgent(name, "org.example.agents.BuyerAgent",
                new Object[] { car, budget, appLogger, config, true }).start();
        buyerAgents.add(name);
        waitingBuyerAgents.add(name);
        loggerLog("Buyer '" + name + "' added and waiting - " + car + " budget RM" + budget);
        refreshNegotiationVisualiser();
    }

    // Builds the dealer registration form and validation flow.
    private VBox createDealerView(UILogger logger) {
        VBox box = new VBox(18);
        box.setPadding(new Insets(4));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label headerLabel = new Label("Dealer Portal");
        headerLabel.setStyle("-fx-font-size: 22; -fx-font-weight: 700; -fx-text-fill: " + PRIMARY_BLUE + ";");
        Label subLabel = new Label(
                "List vehicle inventory with price and stock before buyers start negotiating.");
        subLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + TEXT_MUTED + "; -fx-wrap-text: true;");

        VBox infoBanner = new VBox();
        infoBanner.setPadding(new Insets(12, 16, 12, 16));
        infoBanner.setStyle(
                "-fx-background-color: #eff6ff; -fx-background-radius: 12; -fx-border-color: #93c5fd; -fx-border-radius: 12; -fx-border-width: 1;");
        Label infoText = new Label("Dealer listings are registered with the broker and shown to matching buyers.");
        infoText.setStyle("-fx-font-size: 12; -fx-text-fill: #0c4a6e; -fx-font-weight: 600; -fx-wrap-text: true;");
        infoBanner.getChildren().add(infoText);
        VBox formSection = createPanel(18, new Insets(22));

        Label formTitle = new Label("Register and List New Car");
        formTitle.setStyle("-fx-font-size: 16; -fx-font-weight: 700; -fx-text-fill: " + DARK_TEXT + ";");

        GridPane form = new GridPane();
        configurePortalForm(form);

        TextField dealerName = createStyledTextField("e.g., GreenCars Sdn Bhd");
        ComboBox<String> carModel = createStyledCarComboBox();
        TextField retailPrice = createStyledTextField("e.g., 150000");
        TextField stockField = createStyledTextField("e.g., 3");

        GridPane.setHgrow(dealerName, Priority.ALWAYS);
        GridPane.setHgrow(carModel, Priority.ALWAYS);
        GridPane.setHgrow(retailPrice, Priority.ALWAYS);
        GridPane.setHgrow(stockField, Priority.ALWAYS);
        dealerName.setMaxWidth(Double.MAX_VALUE);
        carModel.setMaxWidth(Double.MAX_VALUE);
        retailPrice.setMaxWidth(Double.MAX_VALUE);
        stockField.setMaxWidth(Double.MAX_VALUE);

        form.add(makeFieldLabel("Dealer Name", "Unique seller agent name"), 0, 0);
        form.add(dealerName, 1, 0);
        form.add(makeFieldLabel("Car Model", "Vehicle listed with broker"), 0, 1);
        form.add(carModel, 1, 1);
        form.add(makeFieldLabel("Retail Price (RM)", "Dealer starting ask"), 0, 2);
        form.add(retailPrice, 1, 2);
        form.add(makeFieldLabel("Stock Quantity", "Available units"), 0, 3);
        form.add(stockField, 1, 3);

        Button addDealerBtn = createStyledButton("List Car", WARNING_ORANGE);
        addDealerBtn.setMaxWidth(Double.MAX_VALUE);

        addDealerBtn.setOnAction(e -> {
            String name = dealerName.getText().trim();
            String car = carModel.getValue() != null ? carModel.getValue() : "";
            String price = retailPrice.getText().trim();
            String stock = stockField.getText().trim();

            if (name.isEmpty() || car.isEmpty() || price.isEmpty() || stock.isEmpty()) {
                showAlert("⚠️ All fields are required!", Alert.AlertType.WARNING);
                return;
            }

            try {
                // Validate price is numeric
                double priceAmount = Double.parseDouble(price);
                if (priceAmount <= 0) {
                    showAlert("❌ Price must be greater than 0", Alert.AlertType.WARNING);
                    return;
                }
                int stockAmount = Integer.parseInt(stock);
                if (stockAmount <= 0) {
                    showAlert("❌ Stock must be at least 1", Alert.AlertType.WARNING);
                    return;
                }

                NegotiationConfig config = buildNegotiationConfig();
                cc.createNewAgent(name, "org.example.agents.DealerAgent",
                        new Object[] { car, price, stock, logger, config }).start();
                logger.log("Dealer '" + name + "' listed " + car + " @ RM" + price + " | Stock: " + stock);
                dealerAgents.add(name);
                recordDealerListing(name, car, (int) priceAmount, stockAmount, config);
                dealerName.clear();
                carModel.setValue(null);
                retailPrice.clear();
                stockField.clear();
                showAlert("✅ Dealer " + name + " registered with " + stock + " unit(s)!", Alert.AlertType.INFORMATION);
            } catch (NumberFormatException ex) {
                showAlert("❌ Price and Stock must be valid numbers", Alert.AlertType.ERROR);
            } catch (Exception ex) {
                logger.log("❌ Error creating dealer: " + ex.getMessage());
                showAlert("❌ Error: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        form.add(addDealerBtn, 1, 4);

        formSection.getChildren().addAll(formTitle, form);

        box.getChildren().addAll(headerLabel, subLabel, infoBanner, formSection);
        VBox.setVgrow(formSection, Priority.SOMETIMES);

        return box;
    }

    // Builds the negotiation settings workspace.
    private VBox createMarketAnalysisView() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label headerLabel = new Label("Market Analytics");
        headerLabel.setStyle("-fx-font-size: 25; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");

        box.getChildren().addAll(headerLabel, createSimulationControlPanel());

        return box;
    }

    // Builds strategy, timing, retry, and manual dealer override controls.
    private VBox createSimulationControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle(PANEL_STYLE);

        Label title = new Label("Negotiation Settings");
        title.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: " + PRIMARY_BLUE + ";");
        NegotiationConfig defaults = NegotiationConfig.defaults();

        strategyChoice = new ComboBox<>();
        strategyChoice.getItems().addAll("BOULWARE", "CONCEDER", "LINEAR");
        strategyChoice.setValue(defaults.getStrategy().name());
        strategyChoice.setPrefWidth(180);
        strategyChoice.setStyle(comboBoxStyle());

        switchStrategyChoice = new ComboBox<>();
        switchStrategyChoice.getItems().addAll("BOULWARE", "CONCEDER", "LINEAR");
        switchStrategyChoice.setValue(defaults.getSwitchStrategy().name());
        switchStrategyChoice.setPrefWidth(180);
        switchStrategyChoice.setStyle(comboBoxStyle());

        deadlineCyclesField = createStyledTextField(String.valueOf(defaults.getDeadlineCycles()));
        deadlineCyclesField.setText(String.valueOf(defaults.getDeadlineCycles()));
        strategySwitchCycleField = createStyledTextField(String.valueOf(defaults.getStrategySwitchCycle()));
        strategySwitchCycleField.setText(String.valueOf(defaults.getStrategySwitchCycle()));
        buyerStartPercentField = createStyledTextField(String.valueOf((int) (defaults.getBuyerStartPercent() * 100)));
        buyerStartPercentField.setText(String.valueOf((int) (defaults.getBuyerStartPercent() * 100)));
        reservePercentField = createStyledTextField(String.valueOf((int) (defaults.getDealerReservePercent() * 100)));
        reservePercentField.setText(String.valueOf((int) (defaults.getDealerReservePercent() * 100)));
        maxRoundsField = createStyledTextField(String.valueOf(defaults.getMaxRoundsPerDealer()));
        maxRoundsField.setText(String.valueOf(defaults.getMaxRoundsPerDealer()));
        retryLimitField = createStyledTextField(String.valueOf(defaults.getMaxSearchRetries()));
        retryLimitField.setText(String.valueOf(defaults.getMaxSearchRetries()));
        stuckRoundsField = createStyledTextField(String.valueOf(defaults.getStuckRoundsBeforeAcceleration()));
        stuckRoundsField.setText(String.valueOf(defaults.getStuckRoundsBeforeAcceleration()));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Strategy:"), 0, 0);
        grid.add(strategyChoice, 1, 0);
        grid.add(new Label("Deadline cycles:"), 2, 0);
        grid.add(deadlineCyclesField, 3, 0);
        grid.add(new Label("Switch at cycle:"), 0, 1);
        grid.add(strategySwitchCycleField, 1, 1);
        grid.add(new Label("Then use strategy:"), 2, 1);
        grid.add(switchStrategyChoice, 3, 1);
        grid.add(new Label("Buyer start % of budget:"), 0, 2);
        grid.add(buyerStartPercentField, 1, 2);
        grid.add(new Label("Dealer reserve % of price:"), 2, 2);
        grid.add(reservePercentField, 3, 2);
        grid.add(new Label("Max rounds / dealer:"), 0, 3);
        grid.add(maxRoundsField, 1, 3);
        grid.add(new Label("Search retries:"), 2, 3);
        grid.add(retryLimitField, 3, 3);
        grid.add(new Label("Stuck rounds (accelerate):"), 0, 4);
        grid.add(stuckRoundsField, 1, 4);

        Label manualTitle = new Label("Manual Dealer Price Override");
        manualTitle.setStyle("-fx-font-size: 13; -fx-font-weight: 700; -fx-text-fill: " + TEXT_MUTED + ";");
        manualDealerNameField = createStyledTextField("Dealer agent name (e.g. GreenCars)");
        manualDealerPriceField = createStyledTextField("New asking price (e.g. 95000)");
        Button adjustPriceBtn = createStyledButton("Send Override", SUCCESS_GREEN);
        adjustPriceBtn.setOnAction(e -> {
            String dealer = manualDealerNameField.getText().trim();
            String price = manualDealerPriceField.getText().trim();
            if (dealer.isEmpty() || price.isEmpty()) {
                showAlert("Dealer name and price are required.", Alert.AlertType.WARNING);
                return;
            }
            try {
                Integer.parseInt(price);
                sendDealerPriceAdjustment(dealer, price);
                showAlert("Price override sent to " + dealer, Alert.AlertType.INFORMATION);
            } catch (NumberFormatException ex) {
                showAlert("Price must be a valid integer.", Alert.AlertType.ERROR);
            }
        });
        HBox manualControls = new HBox(12, manualDealerNameField, manualDealerPriceField, adjustPriceBtn);
        panel.getChildren().addAll(title, grid, new Separator(), manualTitle, manualControls);
        return panel;
    }

    // Reads the current strategy form fields into a NegotiationConfig.
    private NegotiationConfig buildNegotiationConfig() {
        if (strategyChoice == null) {
            return NegotiationConfig.defaults();
        }

        try {
            NegotiationConfig.Strategy strategy = NegotiationConfig.Strategy.valueOf(strategyChoice.getValue());
            int deadline = Math.max(1, Integer.parseInt(deadlineCyclesField.getText().trim()));
            double buyerStart = percentFieldToRatio(buyerStartPercentField, 70);
            double reserve = percentFieldToRatio(reservePercentField, 70);
            int maxRounds = Math.max(1, Integer.parseInt(maxRoundsField.getText().trim()));
            int retryLimit = Math.max(0, Integer.parseInt(retryLimitField.getText().trim()));
            int stuckRounds = Math.max(1, Integer.parseInt(stuckRoundsField.getText().trim()));
            int switchCycle = Math.max(0, Integer.parseInt(strategySwitchCycleField.getText().trim()));
            NegotiationConfig.Strategy switchStrategy = NegotiationConfig.Strategy
                    .valueOf(switchStrategyChoice.getValue());
            return new NegotiationConfig(strategy, deadline, buyerStart, reserve, maxRounds, retryLimit,
                    stuckRounds, 1.0, switchCycle, switchStrategy);
        } catch (Exception e) {
            showAlert("Invalid negotiation settings. Defaults will be used.", Alert.AlertType.WARNING);
            return NegotiationConfig.defaults();
        }
    }

    // Converts a percentage text field into a ratio, falling back on invalid input.
    private double percentFieldToRatio(TextField field, int fallbackPercent) {
        String raw = field.getText().trim();
        double value = raw.isEmpty() ? fallbackPercent : Double.parseDouble(raw);
        value = Math.max(1, Math.min(100, value));
        return value / 100.0;
    }

    // Builds the full activity log workspace with copy and clear controls.
    private VBox createActivityLogView() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(4));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label headerLabel = new Label("System Activity Log");
        headerLabel.setStyle("-fx-font-size: 22; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");

        TextArea fullLogArea = new TextArea();
        fullLogArea.setEditable(false);
        fullLogArea.setWrapText(true);
        fullLogArea.setStyle(textAreaStyle(true));
        fullLogArea.setPrefRowCount(30);

        logArea.textProperty().addListener((obs, oldVal, newVal) -> {
            fullLogArea.setText(newVal);
            fullLogArea.setScrollTop(Double.MAX_VALUE);
        });

        HBox controlBox = new HBox(12);
        controlBox.setPadding(new Insets(15));
        controlBox.setStyle(SOFT_PANEL_STYLE);

        Button copyBtn = createStyledButton("Copy Log", ACCENT_BLUE);
        copyBtn.setPrefWidth(120);
        copyBtn.setOnAction(e -> {
            var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            var content = new javafx.scene.input.ClipboardContent();
            content.putString(logArea.getText());
            clipboard.setContent(content);
            showAlert("Log copied to clipboard!", Alert.AlertType.INFORMATION);
        });

        Button clearBtn = createStyledButton("Clear Log", ERROR_RED);
        clearBtn.setPrefWidth(120);
        clearBtn.setOnAction(e -> {
            logArea.clear();
            fullLogArea.clear();
        });

        controlBox.getChildren().addAll(copyBtn, clearBtn);

        box.getChildren().addAll(headerLabel, fullLogArea, controlBox);
        VBox.setVgrow(fullLogArea, Priority.ALWAYS);

        return box;
    }

    // Builds the failed-negotiations workspace with summary and export controls.
    private VBox createFailuresView() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(4));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label headerLabel = new Label("Failed Negotiations");
        headerLabel.setStyle("-fx-font-size: 22; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");

        failureReportArea.setEditable(false);
        failureReportArea.setWrapText(true);
        failureReportArea.setText("Failure summary\n----------------\nNo failed negotiations yet.");
        failureReportArea.setStyle(textAreaStyle(true));
        failureReportArea.setPrefRowCount(11);
        failureReportArea.setMinHeight(180);

        failuresArea.setEditable(false);
        failuresArea.setWrapText(true);
        failuresArea.setStyle(textAreaStyle(true));
        failuresArea.setPrefRowCount(18);

        HBox controlBox = new HBox(12);
        controlBox.setPadding(new Insets(10));
        controlBox.setStyle(SOFT_PANEL_STYLE);

        Button copyBtn = createStyledButton("Copy Failures", ACCENT_BLUE);
        copyBtn.setOnAction(e -> {
            var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            var content = new javafx.scene.input.ClipboardContent();
            content.putString(failuresArea.getText());
            clipboard.setContent(content);
            showAlert("Failures copied to clipboard!", Alert.AlertType.INFORMATION);
        });

        Button clearBtn = createStyledButton("Clear Failures", ERROR_RED);
        clearBtn.setOnAction(e -> {
            failuresArea.clear();
            failureReportArea.setText("Failure summary\n----------------\nNo failed negotiations yet.");
            failureReasonCounts.clear();
        });

        controlBox.getChildren().addAll(copyBtn, clearBtn);

        box.getChildren().addAll(headerLabel, failureReportArea, createSectionLabel("Raw failure log"),
                failuresArea, controlBox);
        VBox.setVgrow(failuresArea, Priority.ALWAYS);
        return box;
    }

    // Builds the live sessions workspace with event history and summary metrics.
    private VBox createSessionsView() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label header = new Label("Negotiation Sessions");
        header.setStyle("-fx-font-size: 25; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");

        HBox miniStats = new HBox(16);
        VBox activeCard = createStatCard("Active", activeSessionsLabelMini, "#8b5cf6");
        VBox feesCard = createStatCard("💵 Fixed Fees", fixedFeesLabelMini, "#06b6d4");
        VBox commCard = createStatCard("Commission (5% deals)", commissionLabelMini, SUCCESS_GREEN);
        for (VBox c : new VBox[] { activeCard, feesCard, commCard }) {
            c.setPrefWidth(180);
            miniStats.getChildren().add(c);
        }

        Label hint = new Label(
                "Each row below is a SESSION START, DEAL SETTLED, or NO DEAL event logged by the broker.");
        hint.setStyle("-fx-font-size: 12; -fx-text-fill: " + TEXT_MUTED + "; -fx-wrap-text: true;");
        hint.setMaxWidth(Double.MAX_VALUE);

        sessionsArea.setEditable(false);
        sessionsArea.setWrapText(true);
        sessionsArea.setStyle(textAreaStyle(true));

        HBox ctrlBox = new HBox(12);
        ctrlBox.setPadding(new Insets(12));
        ctrlBox.setStyle(SOFT_PANEL_STYLE);
        Button clearBtn = createStyledButton("Clear", ERROR_RED);
        clearBtn.setOnAction(e -> sessionsArea.clear());
        Button copyBtn = createStyledButton("Copy", ACCENT_BLUE);
        copyBtn.setOnAction(e -> {
            var cb = javafx.scene.input.Clipboard.getSystemClipboard();
            var content = new javafx.scene.input.ClipboardContent();
            content.putString(sessionsArea.getText());
            cb.setContent(content);
            showAlert("Session log copied!", Alert.AlertType.INFORMATION);
        });
        ctrlBox.getChildren().addAll(copyBtn, clearBtn);

        box.getChildren().addAll(header, miniStats, hint, sessionsArea, ctrlBox);
        VBox.setVgrow(sessionsArea, Priority.ALWAYS);
        return box;
    }

    // Builds the manual negotiation control workspace.
    private VBox createManualPlayView() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label title = new Label("🎮 Manual Mode");
        title.setStyle("-fx-font-size: 25; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");

        HBox topBox = new HBox(15);
        topBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        manualBuyerSelect = new ComboBox<>(manualBuyerAgents);
        manualBuyerSelect.setPromptText("Select Manual Buyer");
        manualBuyerSelect.setPrefWidth(200);
        manualBuyerSelect.setStyle(comboBoxStyle());
        topBox.getChildren().addAll(new Label("Controlling:"), manualBuyerSelect);

        manualLogArea = new TextArea();
        manualLogArea.setEditable(false);
        manualLogArea.setWrapText(true);
        manualLogArea.setStyle(textAreaStyle(true));

        VBox actionPanel = new VBox(10);
        actionPanel.setPadding(new Insets(18));
        actionPanel.setStyle(PANEL_STYLE);

        Label actionTitle = new Label("Action Panel");
        actionTitle.setStyle("-fx-font-weight: bold;");

        HBox shortlistBox = new HBox(10);
        shortlistBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        manualDealerSelect = new ComboBox<>();
        manualDealerSelect.setPromptText("Select Dealer");
        manualDealerSelect.setStyle(comboBoxStyle());
        manualFirstOfferField = createStyledTextField("First Offer RM");
        manualSendFirstOfferBtn = createStyledButton("Send First Offer", ACCENT_BLUE);
        manualSendFirstOfferBtn.setDisable(true);
        shortlistBox.getChildren().addAll(manualDealerSelect, manualFirstOfferField, manualSendFirstOfferBtn);

        HBox counterBox = new HBox(10);
        counterBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        manualCounterPriceField = createStyledTextField("Counter RM");
        manualSendCounterBtn = createStyledButton("Send Counter", ACCENT_BLUE);
        manualAcceptDealBtn = createStyledButton("Accept Offer", SUCCESS_GREEN);
        manualWalkAwayBtn = createStyledButton("Walk Away", ERROR_RED);

        manualSendCounterBtn.setDisable(true);
        manualAcceptDealBtn.setDisable(true);
        manualWalkAwayBtn.setDisable(true);

        counterBox.getChildren().addAll(manualCounterPriceField, manualSendCounterBtn, manualAcceptDealBtn,
                manualWalkAwayBtn);

        actionPanel.getChildren().addAll(actionTitle, shortlistBox, counterBox);

        manualSendFirstOfferBtn.setOnAction(e -> {
            String dealer = manualDealerSelect.getValue();
            String offer = manualFirstOfferField.getText().trim();
            String buyer = manualBuyerSelect.getValue();
            if (buyer == null || dealer == null || offer.isEmpty() || !isPositiveInteger(offer)) {
                showAlert("Select a manual buyer, select a dealer, and enter a positive first offer.",
                        Alert.AlertType.WARNING);
                return;
            }
            sendAgentCommand(buyer, "MANUAL_ACTION", "SHORTLIST;" + dealer + ";" + offer);
            manualLogArea.appendText("\n[YOU] Picked " + dealer + " with RM " + offer);
            manualSendFirstOfferBtn.setDisable(true);
        });

        manualSendCounterBtn.setOnAction(e -> {
            String offer = manualCounterPriceField.getText().trim();
            String buyer = manualBuyerSelect.getValue();
            if (buyer == null || !isPositiveInteger(offer)) {
                showAlert("Select a manual buyer and enter a positive counter price.", Alert.AlertType.WARNING);
                return;
            }
            sendAgentCommand(buyer, "MANUAL_ACTION", "COUNTER;" + offer);
            manualLogArea.appendText("\n[YOU] Countered RM " + offer);
            disableCounterControls();
        });

        manualAcceptDealBtn.setOnAction(e -> {
            String offer = manualCounterPriceField.getText().trim();
            String buyer = manualBuyerSelect.getValue();
            if (buyer == null || !isPositiveInteger(offer)) {
                showAlert("Select a manual buyer and enter a positive accepted price.", Alert.AlertType.WARNING);
                return;
            }
            sendAgentCommand(buyer, "MANUAL_ACTION", "ACCEPT;" + offer);
            manualLogArea.appendText("\n[YOU] Accepted RM " + offer);
            disableCounterControls();
        });

        manualWalkAwayBtn.setOnAction(e -> {
            String buyer = manualBuyerSelect.getValue();
            if (buyer == null) {
                showAlert("Select a manual buyer before walking away.", Alert.AlertType.WARNING);
                return;
            }
            sendAgentCommand(buyer, "MANUAL_ACTION", "WALKAWAY;");
            manualLogArea.appendText("\n[YOU] Walked away.");
            disableCounterControls();
        });

        box.getChildren().addAll(title, topBox, manualLogArea, actionPanel);
        VBox.setVgrow(manualLogArea, Priority.ALWAYS);
        return box;
    }

    // Disables manual counter controls until a buyer session is ready.
    private void disableCounterControls() {
        manualSendCounterBtn.setDisable(true);
        manualAcceptDealBtn.setDisable(true);
        manualWalkAwayBtn.setDisable(true);
    }

    // Returns whether a string is a positive integer.
    private boolean isPositiveInteger(String value) {
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // Parses buyer manual-mode prompts and updates the manual control panel.
    private void handleManualPromptLog(String msg) {
        try {
            int promptIdx = msg.indexOf("[MANUAL_PROMPT]");
            if (promptIdx == -1)
                return;

            String beforePrompt = msg.substring(0, promptIdx);
            int colonIdx = beforePrompt.lastIndexOf(":");
            if (colonIdx == -1)
                return;
            String agentName = beforePrompt.substring(0, colonIdx).trim();

            String payload = msg.substring(promptIdx + 16).trim();

            if (payload.startsWith("SHORTLIST:")) {
                String csv = payload.substring(10);
                manualLogArea.appendText("\n\n[" + agentName + "] Received Shortlist Options:\n");
                for (String option : csv.split(",")) {
                    if (option.isEmpty())
                        continue;
                    String[] parts = option.split(":");
                    manualLogArea.appendText("  - " + parts[0] + " (Listed: RM" + parts[1] + ")\n");
                }
                if (agentName.equals(manualBuyerSelect.getValue())) {
                    manualDealerSelect.getItems().clear();
                    for (String d : csv.split(",")) {
                        if (!d.isEmpty())
                            manualDealerSelect.getItems().add(d.split(":")[0]);
                    }
                    manualSendFirstOfferBtn.setDisable(false);
                }
            } else if (payload.startsWith("COUNTER:")) {
                String[] p = payload.substring(8).split(":");
                String dealer = p[0];
                String price = p[1];
                manualLogArea.appendText("\n[" + agentName + "] " + dealer + " counters RM " + price);

                if (agentName.equals(manualBuyerSelect.getValue())) {
                    manualCounterPriceField.setText(price);
                    manualSendCounterBtn.setDisable(false);
                    manualAcceptDealBtn.setDisable(false);
                    manualWalkAwayBtn.setDisable(false);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing manual prompt: " + e.getMessage());
        }
    }

    // Creates a shared styled text field with prompt text.
    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMinHeight(38);
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setStyle(
                "-fx-font-size: 14; -fx-font-family: " + FONT_FAMILY + "; -fx-font-weight: " + FONT_WEIGHT_MEDIUM
                        + "; -fx-padding: 10 12; " +
                        "-fx-border-color: " + BORDER_SUBTLE + "; -fx-border-radius: 8; -fx-border-width: 1; " +
                        "-fx-control-inner-background: " + SURFACE + "; -fx-background-radius: 8; " +
                        "-fx-prompt-text-fill: #94a3b8;");
        return tf;
    }

    // Creates a shared styled car-model combo box.
    private ComboBox<String> createStyledCarComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(CAR_MODELS);
        comboBox.setEditable(true);
        comboBox.setPrefWidth(300);
        comboBox.setMinHeight(38);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setStyle(
                "-fx-font-size: 14; -fx-font-family: " + FONT_FAMILY + "; -fx-font-weight: " + FONT_WEIGHT_MEDIUM
                        + "; -fx-padding: 6 10; " +
                        "-fx-border-color: " + BORDER_SUBTLE + "; -fx-border-radius: 8; -fx-border-width: 1; " +
                        "-fx-control-inner-background: " + SURFACE + "; -fx-background-radius: 8;");
        comboBox.setPromptText("Select or type car model...");

        return comboBox;
    }

    // Creates a shared styled form button.
    private Button createStyledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setMinHeight(38);
        btn.setStyle(
                "-fx-font-size: 14; -fx-font-family: " + FONT_FAMILY + "; -fx-font-weight: 600; -fx-padding: 10 22; " +
                        "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 10; -fx-border-radius: 10; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.14), 10, 0, 0, 2);");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.88));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    // Shows a modal validation or status alert.
    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "❌ Error" : "ℹ️ Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Returns a display fallback when a text value is blank.
    private String valueOrNA(String value) {
        return value != null && !value.isBlank() ? value : "N/A";
    }

    // Allows agents and UI helpers to publish log messages into the JavaFX dashboard.
    public interface UILogger {
        // Publishes one log message to the UI.
        void log(String message);
    }
}
