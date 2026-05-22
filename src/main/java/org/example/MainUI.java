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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/** JavaFX dashboard and control console for the broker-routed car negotiation demo. */
public class MainUI extends Application {
    private TextArea logArea = new TextArea();
    private TextArea rawLogArea = new TextArea();
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
    private boolean simulationStarted = false;
    private boolean isAutoPlay = false;
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
    private final Set<String> registeredBuyerNames = new HashSet<>();
    private final Set<String> registeredDealerNames = new HashSet<>();
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
    private List<String> failedDeals = new ArrayList<>();
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
    private static final String SOFT_SHADOW = "dropshadow(gaussian, rgba(30,64,175,0.10), 14, 0, 0, 4)";
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

    /** Available visualiser modes inside the dashboard. */
    private enum VisualiserView {
        MARKET,
        SESSION,
        AGENT
    }

    /** Event types recorded for visualising negotiation trajectories. */
    private enum TrajectoryEvent {
        START,
        OFFER,
        COUNTER,
        ACCEPT,
        WALKAWAY,
        PRICE_UPDATE
    }

    /** One plotted price event for a session or agent trajectory. */
    private static class TrajectoryPoint {
        private final int cycle;
        private final double price;
        private final String agent;
        private final String sessionId;
        private final String car;
        private final TrajectoryEvent event;

        /** Captures one price, actor, session, car, and event for charting. */
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

    /** Parsed broker metadata for one negotiation session. */
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
        private Integer outcomeCycle;
        private String failureReason;

        /** Creates metadata for a session id before all broker fields are known. */
        private SessionMeta(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    /** Launches the JavaFX application. */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
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
                rawLogArea.appendText(formattedMsg);
                rawLogArea.setScrollTop(Double.MAX_VALUE);

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
                    TimelineLogEntry timelineEntry = formatTimelineLogEntry(msg);
                    if (timelineEntry.visibleInTimeline) {
                        logArea.appendText(timestamp + timelinePrefix(timelineEntry.category)
                                + timelineEntry.message + "\n");
                        logArea.setScrollTop(Double.MAX_VALUE);
                    }
                    if (dashboardEventsArea != null && timelineEntry.visibleOnDashboard) {
                        dashboardEventsArea.appendText(timestamp + timelinePrefix(timelineEntry.category)
                                + timelineEntry.message + "\n");
                        dashboardEventsArea.setScrollTop(Double.MAX_VALUE);
                    }
                }

                if (isBuyerReg) {
                    registerBuyerInDashboard(extractQuotedName(msg));
                }
                if (isDealerReg) {
                    registerDealerInDashboard(extractQuotedName(msg));
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
                    failedDeals.add(formattedMsg);
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

    /** Converts one raw agent/broker log message into the readable Timeline feed. */
    private TimelineLogEntry formatTimelineLogEntry(String msg) {
        if (msg == null || msg.isBlank()) {
            return hiddenTimeline(LogCategory.DEBUG, "");
        }

        if (msg.contains("STATUS: Negotiation resumed.")) {
            return isBrokerMessage(msg)
                    ? visibleTimeline(LogCategory.SYSTEM, simulationStateSummary("resumed"), true)
                    : hiddenTimeline(LogCategory.DEBUG, msg);
        }
        if (msg.contains("STATUS: Negotiation paused.")) {
            return isBrokerMessage(msg)
                    ? visibleTimeline(LogCategory.SYSTEM, simulationStateSummary("paused"), true)
                    : hiddenTimeline(LogCategory.DEBUG, msg);
        }
        if (msg.contains("Cycle Shift:")) {
            return visibleTimeline(LogCategory.CYCLE, "Cycle " + valueAfterLastSpace(msg), false);
        }
        if (msg.contains("Price updated") || msg.contains("Willing to pay")) {
            return hiddenTimeline(LogCategory.DEBUG, msg);
        }

        if (msg.contains("[BROKER] SESSION START:")) {
            return visibleTimeline(LogCategory.SESSION, formatSessionStartTimeline(msg), true);
        }
        if (msg.contains("[BROKER] INVITE:")) {
            return visibleTimeline(LogCategory.SESSION, formatBrokerInviteTimeline(msg), true);
        }
        if (msg.contains("[BROKER] RELAY COUNTER:")) {
            return visibleTimeline(LogCategory.COUNTER, formatBrokerCounterTimeline(msg), true);
        }
        if (msg.contains("[BROKER] RELAY OFFER:")) {
            return visibleTimeline(LogCategory.OFFER, formatBrokerOfferTimeline(msg), true);
        }
        if (msg.contains("[BROKER] DEAL SETTLED:")) {
            return visibleTimeline(LogCategory.DEAL, formatDealSettledTimeline(msg), true);
        }
        if (msg.contains("[BROKER] NO DEAL:")) {
            return visibleTimeline(LogCategory.FAILURE, formatNoDealTimeline(msg), true);
        }
        if (msg.contains("[BROKER] FEE CHARGED:")) {
            return visibleTimeline(LogCategory.REVENUE, formatFeeTimeline(msg), true);
        }
        if (msg.contains("[BROKER] REVENUE:")) {
            return visibleTimeline(LogCategory.REVENUE, formatRevenueTimeline(msg), true);
        }
        if (msg.contains("[BROKER] PERFORMANCE:")) {
            return hiddenTimeline(LogCategory.DEBUG, msg);
        }
        if (msg.contains("[BROKER] SEARCH:")) {
            return visibleTimeline(LogCategory.SEARCH, stripBrokerPrefix(msg), false);
        }
        if (msg.contains("[BROKER] LISTING:")) {
            return visibleTimeline(LogCategory.SEARCH, formatListingTimeline(msg), false);
        }
        if (msg.contains("[BROKER] === BROKER ONLINE ===")) {
            return visibleTimeline(LogCategory.SYSTEM, "Broker online and ready to route negotiations.", true);
        }
        if (msg.contains("[BROKER] Fixed Negotiation Fee:")) {
            return visibleTimeline(LogCategory.SYSTEM, stripBrokerPrefix(msg), true);
        }
        if (msg.contains("Initializing Space Control")) {
            return visibleTimeline(LogCategory.SYSTEM, "Space Control online and ready to advance cycles.", true);
        }

        if (msg.contains("NEGOTIATION: Starting with")) {
            return visibleTimeline(LogCategory.SESSION, formatBuyerNegotiationStartTimeline(msg), false);
        }
        if (msg.contains("STATUS: Shortlist received")) {
            return visibleTimeline(LogCategory.SEARCH, agentName(msg) + " received dealer shortlist and is choosing.", false);
        }
        if (msg.contains("STATUS: All dealers' reserve prices exceed budget")) {
            return visibleTimeline(LogCategory.FAILURE, agentName(msg) + " cannot afford any matching dealer reserve.", false);
        }
        if (msg.contains("AGREED:")) {
            return visibleTimeline(LogCategory.OFFER, agentName(msg) + " accepted the counter and sent a final offer.", false);
        }
        if (msg.contains("STATUS: Negotiation dragging. Accelerated offer")) {
            return visibleTimeline(LogCategory.OFFER, formatAccelerationTimeline(msg), false);
        }
        if (msg.contains("SUCCESS!")) {
            return hiddenTimeline(LogCategory.DEBUG, msg);
        }
        if (msg.contains("DEAL CLOSED:")) {
            return hiddenTimeline(LogCategory.DEBUG, msg);
        }
        if (msg.contains("Buyer '") && msg.contains("added")) {
            return visibleTimeline(LogCategory.SYSTEM, msg, false);
        }
        if (msg.contains("Dealer '") && msg.contains("listed")) {
            return visibleTimeline(LogCategory.SYSTEM, msg, false);
        }
        if (msg.contains("Demo scenario")) {
            return visibleTimeline(LogCategory.SYSTEM, msg, true);
        }

        boolean visibleStatus = msg.contains("STATUS:") || msg.contains("RESET:") || msg.contains("Sniffer");
        return new TimelineLogEntry(visibleStatus ? LogCategory.SYSTEM : LogCategory.DEBUG, msg, visibleStatus, false);
    }

    /** Creates a visible timeline entry. */
    private TimelineLogEntry visibleTimeline(LogCategory category, String message, boolean dashboard) {
        return new TimelineLogEntry(category, message, true, dashboard);
    }

    /** Creates a raw-only timeline entry. */
    private TimelineLogEntry hiddenTimeline(LogCategory category, String message) {
        return new TimelineLogEntry(category, message, false, false);
    }

    /** Returns a consistent prefix for readable timeline categories. */
    private String timelinePrefix(LogCategory category) {
        return "[" + categoryLabel(category) + "] ";
    }

    /** Converts a category enum into a compact label. */
    private String categoryLabel(LogCategory category) {
        switch (category) {
            case SYSTEM:
                return "System";
            case SEARCH:
                return "Search";
            case SESSION:
                return "Session";
            case OFFER:
                return "Offer";
            case COUNTER:
                return "Counter";
            case DEAL:
                return "Deal";
            case FAILURE:
                return "No Deal";
            case REVENUE:
                return "Revenue";
            case CYCLE:
                return "Cycle";
            default:
                return "Debug";
        }
    }

    /** Returns true when the raw message came from the broker. */
    private boolean isBrokerMessage(String msg) {
        return msg.contains("[BROKER]");
    }

    /** Builds one collapsed pause/resume summary from current UI registration state. */
    private String simulationStateSummary(String state) {
        return "Simulation " + state + ": broker, " + buyerAgents.size() + " buyers, "
                + dealerAgents.size() + " dealers.";
    }

    /** Formats a broker session-start line for humans. */
    private String formatSessionStartTimeline(String msg) {
        String payload = substringAfter(msg, "SESSION START:");
        String sessionId = extractSessionId(payload);
        String buyer = extractBrokerField(payload, "Buyer");
        String dealer = extractBrokerField(payload, "Dealer");
        String car = extractBrokerField(payload, "Car");
        String firstOffer = extractBrokerField(payload, "FirstOffer");
        String buyerReserve = extractBrokerField(payload, "BuyerReserve");
        String dealerReserve = extractBrokerField(payload, "DealerReserve");
        return "Session opened: " + valueOrNA(buyer) + " -> " + valueOrNA(dealer)
                + " for " + valueOrNA(car) + ", first offer " + valueOrNA(firstOffer)
                + " [" + valueOrNA(sessionId) + "]"
                + " | buyer budget " + valueOrNA(buyerReserve)
                + (dealerReserve != null ? " | dealer reserve " + dealerReserve : "");
    }

    /** Formats the broker's dealer invitation line. */
    private String formatBrokerInviteTimeline(String msg) {
        String dealer = valueAfter(msg, "offer to ");
        if (dealer.contains("|")) {
            dealer = dealer.substring(0, dealer.indexOf('|')).trim();
        }
        Integer price = extractLastMoneyValue(msg);
        return "Broker invited " + valueOrNA(dealer) + " with the first offer"
                + (price != null ? " " + money(price) : "") + ".";
    }

    /** Formats a dealer-to-buyer counter routed through the broker. */
    private String formatBrokerCounterTimeline(String msg) {
        String payload = substringAfter(msg, "RELAY COUNTER:");
        String sessionId = extractSessionId(payload);
        String dealer = extractBrokerField(payload, "Dealer");
        String buyer = extractBrokerField(payload, "Buyer");
        Integer price = extractLastMoneyValue(payload);
        String round = extractRound(payload);
        return "Round " + valueOrNA(round) + " counter: " + valueOrNA(dealer) + " -> "
                + valueOrNA(buyer) + ", " + money(price) + " [" + valueOrNA(sessionId) + "]";
    }

    /** Formats a buyer-to-dealer offer routed through the broker. */
    private String formatBrokerOfferTimeline(String msg) {
        String payload = substringAfter(msg, "RELAY OFFER:");
        String sessionId = extractSessionId(payload);
        String buyer = extractBrokerField(payload, "Buyer");
        String dealer = extractBrokerField(payload, "Dealer");
        Integer price = extractLastMoneyValue(payload);
        return "Buyer offer routed: " + valueOrNA(buyer) + " -> " + valueOrNA(dealer)
                + ", " + money(price) + " [" + valueOrNA(sessionId) + "]";
    }

    /** Formats a broker settlement line. */
    private String formatDealSettledTimeline(String msg) {
        String payload = substringAfter(msg, "DEAL SETTLED:");
        String sessionId = extractSessionId(payload);
        String buyer = extractBrokerField(payload, "Buyer");
        String dealer = extractBrokerField(payload, "Dealer");
        String car = extractBrokerField(payload, "Car");
        String price = extractBrokerField(payload, "Price");
        String commission = extractBrokerField(payload, "Commission");
        return "Deal settled: " + valueOrNA(buyer) + " bought " + valueOrNA(car)
                + " from " + valueOrNA(dealer) + " for " + valueOrNA(price)
                + " [" + valueOrNA(sessionId) + "]"
                + (commission != null ? " | commission " + commission : "");
    }

    /** Formats a broker failure/no-deal line. */
    private String formatNoDealTimeline(String msg) {
        String payload = substringAfter(msg, "NO DEAL:");
        String sessionId = extractSessionId(payload);
        String reason = extractBrokerField(payload, "Reason");
        String buyer = extractBrokerField(payload, "Buyer");
        String dealer = extractBrokerField(payload, "Dealer");
        String car = extractBrokerField(payload, "Car");
        String parties = valueOrNA(buyer) + (dealer != null ? " with " + dealer : "");
        return "No deal: " + parties + " for " + valueOrNA(car) + " [" + valueOrNA(sessionId)
                + "] | reason " + humanFailureReason(reason);
    }

    /** Formats a fixed session-fee line. */
    private String formatFeeTimeline(String msg) {
        Integer fee = extractFirstMoneyValue(msg);
        Integer running = extractLastMoneyValue(msg);
        return "Session fee charged: " + money(fee)
                + (running != null ? " | running broker revenue " + money(running) : "");
    }

    /** Formats a broker commission/revenue line. */
    private String formatRevenueTimeline(String msg) {
        Integer commission = extractFirstMoneyValue(substringAfter(msg, "REVENUE:"));
        Integer total = extractLastMoneyValue(msg);
        return "Commission earned: " + money(commission)
                + (total != null ? " | total broker revenue " + money(total) : "");
    }

    /** Formats a broker inventory listing line. */
    private String formatListingTimeline(String msg) {
        String listing = stripBrokerPrefix(msg).replace("LISTING:", "").trim();
        return "Dealer listing registered: " + listing;
    }

    /** Formats a buyer's first selected dealer line. */
    private String formatBuyerNegotiationStartTimeline(String msg) {
        String agent = agentName(msg);
        String body = substringAfter(msg, "NEGOTIATION:");
        return agent + " selected dealer - " + body.replace("Starting with ", "");
    }

    /** Formats a buyer acceleration line. */
    private String formatAccelerationTimeline(String msg) {
        Integer price = extractLastMoneyValue(msg);
        return agentName(msg) + " raised their offer to " + money(price) + " to keep negotiation moving.";
    }

    /** Removes the broker source prefix from one message. */
    private String stripBrokerPrefix(String msg) {
        return msg.replace("[BROKER]", "").trim();
    }

    /** Extracts the local agent name before the first colon. */
    private String agentName(String msg) {
        int colon = msg.indexOf(':');
        return colon > 0 ? msg.substring(0, colon).trim() : "Agent";
    }

    /** Returns the text after a marker, or the whole message when absent. */
    private String substringAfter(String msg, String marker) {
        int idx = msg.indexOf(marker);
        return idx >= 0 ? msg.substring(idx + marker.length()).trim() : msg.trim();
    }

    /** Returns text after a marker for simple prose messages. */
    private String valueAfter(String msg, String marker) {
        int idx = msg.indexOf(marker);
        return idx >= 0 ? msg.substring(idx + marker.length()).trim() : "";
    }

    /** Returns the final whitespace-delimited token from a message. */
    private String valueAfterLastSpace(String msg) {
        int idx = msg.lastIndexOf(' ');
        return idx >= 0 ? msg.substring(idx + 1).trim() : msg.trim();
    }

    /** Extracts the displayed round number from broker relay text. */
    private String extractRound(String payload) {
        Matcher matcher = Pattern.compile("\\(Round\\s+(\\d+)\\)").matcher(payload);
        return matcher.find() ? matcher.group(1) : "?";
    }

    /** Extracts the first RM amount from text. */
    private Integer extractFirstMoneyValue(String payload) {
        Matcher matcher = RM_AMOUNT_PATTERN.matcher(payload);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    /** Ingests one broker or agent log message into trajectory state. */
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

    /** Stores a trajectory point under its session and agent indexes. */
    private void storeTrajectoryPoint(TrajectoryPoint point) {
        if (point.sessionId != null) {
            sessionPoints.computeIfAbsent(point.sessionId, k -> new ArrayList<>()).add(point);
            sessionLastPrice.put(point.sessionId, point.price);
        }
        if (point.agent != null && !point.agent.isEmpty()) {
            agentPoints.computeIfAbsent(point.agent, k -> new ArrayList<>()).add(point);
        }
    }

    /** Parses session start input into UI state. */
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

    /** Parses broker trajectory point input into UI state. */
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
            meta.outcomeCycle = currentCycle;
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
            meta.outcomeCycle = currentCycle;
            meta.failureReason = extractReason(payload);
            return new TrajectoryPoint(currentCycle, lastPrice != null ? lastPrice : 0, meta.buyer, sessionId, meta.car,
                    TrajectoryEvent.WALKAWAY);
        }

        return null;
    }

    /** Parses agent price point input into UI state. */
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

    /** Hydrates session metadata from broker payload text. */
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
                if (buyer.contains("->")) {
                    buyer = buyer.substring(buyer.indexOf("->") + 2).trim();
                }
                meta.buyer = buyer;
            }
            if (seg.startsWith("Dealer=")) {
                String dealer = seg.substring(7).trim();
                if (dealer.contains("->")) {
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

    /** Hydrates session metadata from pipe-delimited broker fields. */
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

    /** Extracts the broker field value from text. */
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

    /** Removes relationship arrows and field fragments from parsed broker names. */
    private String trimBrokerRelationship(String value) {
        String cleaned = value == null ? "" : value.trim();
        String[] arrows = { "\u2192", "->" };
        for (String arrow : arrows) {
            int arrowIdx = cleaned.indexOf(arrow);
            if (arrowIdx >= 0) {
                cleaned = cleaned.substring(0, arrowIdx).trim();
            }
        }
        return cleaned;
    }

    /** Removes known broker field prefixes from a parsed value. */
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

    /** Derived view model used by the Session and Market visualisers. */
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

    /** Derived view model used by the Agent visualiser. */
    private static class AgentViewModel {
        private String name;
        private String type;
        private int sessions;
        private int accepted;
        private int rejected;
        private int pending;
        private double averageDealPrice;
        private double averageConcession;
    }

    /** Derived view model used by the Live Listing Board. */
    private static class ListingViewModel {
        private String car;
        private String dealer;
        private Integer listPrice;
        private Integer reserve;
        private int activeBuyers;
        private String status;
    }

    /** Extracts the reason value from text. */
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

    /** Extracts the session id value from text. */
    private String extractSessionId(String payload) {
        int pipe = payload.indexOf('|');
        String raw = pipe >= 0 ? payload.substring(0, pipe).trim() : payload.trim();
        return raw.isEmpty() ? null : raw;
    }

    /** Parses money value input into UI state. */
    private Integer parseMoneyValue(String value) {
        Matcher matcher = RM_AMOUNT_PATTERN.matcher(value);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    /** Extracts the final RM amount from text. */
    private Integer extractLastMoneyValue(String payload) {
        Matcher matcher = RM_AMOUNT_PATTERN.matcher(payload);
        Integer amount = null;
        while (matcher.find()) {
            amount = Integer.parseInt(matcher.group(1));
        }
        return amount;
    }

    /** Loads the fonts resource. */
    private void loadFonts() {
        loadFontResource("/fonts/Poppins-Regular.ttf");
        loadFontResource("/fonts/Poppins-Medium.ttf");
        loadFontResource("/fonts/Poppins-SemiBold.ttf");
        loadFontResource("/fonts/Poppins-Bold.ttf");
    }

    /** Loads one font resource when it is available. */
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

    /** Creates the main content UI component. */
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
                createWorkspaceView("Settings", createMarketAnalysisView()),
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

    /** Creates the workspace view UI component. */
    private Node createWorkspaceView(String key, Node content) {
        StackPane wrapper = new StackPane(content);
        wrapper.setUserData(key);
        wrapper.setVisible(false);
        wrapper.setManaged(false);
        StackPane.setMargin(content, new Insets(0));
        return wrapper;
    }

    /** Applies shared dashboard scroll-pane styling. */
    private void polishScrollPane(ScrollPane scroll) {
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;"
                + "-fx-border-color: transparent; -fx-padding: 0;");
    }

    /** Builds the shared text-area CSS string. */
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

    /** Creates the panel UI component. */
    private VBox createPanel(double spacing, Insets padding) {
        VBox panel = new VBox(spacing);
        panel.setPadding(padding);
        panel.setStyle(PANEL_STYLE);
        return panel;
    }

    /** Creates the sidebar UI component. */
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
                createNavigationButton("Dashboard", "Dashboard:", "Overview, KPIs, Graph"),
                createNavigationButton("Participants", "Participants Portal:", "Buyers and Dealers"),
                createNavigationButton("Manual Negotiation", "Manual Mode:", "Buyers and Dealers"),
                createNavigationButton("Sessions", "Sessions:", "History and Details"),
                createNavigationButton("Settings", "Negotiation Settings:", "Strategy Controls"),
                createNavigationButton("Logs", "Logs:", "Activity and Failures"));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox sidebar = new VBox(8, brandBox, nav, spacer);
        sidebar.setPadding(new Insets(18, 14, 18, 14));
        double sidebarWidth = 230;
        sidebar.setMinWidth(sidebarWidth);
        sidebar.setPrefWidth(sidebarWidth);
        sidebar.setMaxWidth(sidebarWidth);
        sidebar.setStyle("-fx-background-color: linear-gradient(to bottom, #1e3a8a, #312e81);"
                + "-fx-border-color: #c7d2fe; -fx-border-width: 0 1 0 0;");
        return sidebar;
    }

    /** Creates a navigation button for a workspace key and display label. */
    private Button createNavigationButton(String key, String title, String subtitle) {
        Button btn = new Button(title + "\n" + subtitle);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        btn.setStyle(navButtonStyle(false));
        btn.setOnAction(e -> showWorkspace(key));
        navigationButtons.put(key, btn);
        return btn;
    }

    /** Builds the sidebar navigation button CSS string. */
    private String navButtonStyle(boolean active) {
        return "-fx-font-size: 12; -fx-font-family: " + FONT_FAMILY + "; -fx-font-weight: 700;"
                + "-fx-padding: 10 12; -fx-background-radius: 10; -fx-border-radius: 10;"
                + "-fx-background-color: " + (active ? "white" : "rgba(255,255,255,0.10)") + ";"
                + "-fx-text-fill: " + (active ? "#1e3a8a" : "white") + ";"
                + "-fx-cursor: hand; -fx-line-spacing: 2;";
    }

    /** Shows the workspace view or prompt. */
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

    /** Creates the app header UI component. */
    private VBox createAppHeader() {
        Region stripe = new Region();
        stripe.setPrefWidth(5);
        stripe.setMinWidth(5);
        stripe.setStyle("-fx-background-color: " + ACCENT_BLUE + ";");

        Label title = new Label("Automated Car Negotiation System");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");
        Label subtitle = new Label(
                "JADE broker-routed marketplace    session-based negotiation    real-time metrics");
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

    /** Toggles automatic cycle advancement. */
    private void toggleAutoplay() {
        if (!simulationStarted) {
            return;
        }
        isAutoPlay = !isAutoPlay;
        updatePlaybackButtonState();
        sendSimulationPauseCommand(isAutoPlay);
    }

    /** Updates the pause/resume button label and availability. */
    private void updatePlaybackButtonState() {
        if (playPauseBtn == null) {
            return;
        }
        playPauseBtn.setText(isAutoPlay ? "Pause" : "Resume");
        playPauseBtn.setDisable(!simulationStarted);
        playPauseBtn.setOpacity(simulationStarted ? 1.0 : 0.45);
    }

    /** Sends pause or resume to the cycle controller and active negotiation agents. */
    private void sendSimulationPauseCommand(boolean resume) {
        sendSpaceCommand(resume ? "RESUME" : "PAUSE");
        String agentCommand = resume ? "RESUME_NEGOTIATION" : "PAUSE_NEGOTIATION";
        sendBrokerCommand(agentCommand);
        for (String buyer : new ArrayList<>(buyerAgents)) {
            sendAgentCommand(buyer, agentCommand);
        }
        for (String dealer : new ArrayList<>(dealerAgents)) {
            sendAgentCommand(dealer, agentCommand);
        }
    }

    /** Human-facing category for one raw ACL/demo log message. */
    private enum LogCategory {
        SYSTEM,
        SEARCH,
        SESSION,
        OFFER,
        COUNTER,
        DEAL,
        FAILURE,
        REVENUE,
        CYCLE,
        DEBUG
    }

    /** Presentation result for the readable timeline feed. */
    private static class TimelineLogEntry {
        private final LogCategory category;
        private final String message;
        private final boolean visibleInTimeline;
        private final boolean visibleOnDashboard;

        /** Stores one formatted timeline line and where it should be displayed. */
        private TimelineLogEntry(LogCategory category, String message, boolean visibleInTimeline,
                boolean visibleOnDashboard) {
            this.category = category;
            this.message = message;
            this.visibleInTimeline = visibleInTimeline;
            this.visibleOnDashboard = visibleOnDashboard;
        }
    }

    /** Updates the negotiation control status display state. */
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

    /** Writes a message to the UI logger when available. */
    private void loggerLog(String message) {
        String timestamp = "[" + LocalTime.now().format(timeFormatter) + "] ";
        String formatted = timestamp + "[UI] " + message + "\n";
        logArea.appendText(timestamp + "[System] " + message + "\n");
        rawLogArea.appendText(formatted);
        logArea.setScrollTop(Double.MAX_VALUE);
        rawLogArea.setScrollTop(Double.MAX_VALUE);
    }

    /** Removes a terminated agent from dashboard registration state. */
    private void unregisterTerminatedAgent(String msg) {
        int colon = msg.indexOf(':');
        if (colon <= 0) {
            return;
        }
        String agentName = msg.substring(0, colon).trim();
        boolean wasBuyer;
        boolean wasDealer;
        synchronized (buyerAgents) {
            wasBuyer = buyerAgents.remove(agentName);
        }
        synchronized (dealerAgents) {
            wasDealer = dealerAgents.remove(agentName);
        }
        waitingBuyerAgents.remove(agentName);
        manualBuyerAgents.remove(agentName);
        if (registeredBuyerNames.remove(agentName) || wasBuyer) {
            buyerCount = Math.max(0, buyerCount - 1);
            buyerCountLabel.setText(String.valueOf(buyerCount));
            updateBuyerStatus();
        }
        if (registeredDealerNames.remove(agentName) || wasDealer) {
            dealerCount = Math.max(0, dealerCount - 1);
            dealerCountLabel.setText(String.valueOf(dealerCount));
            updateDealerStatus();
            removeDealerListings(agentName);
        }
        updateNegotiationControlStatus();
        refreshNegotiationVisualiser();
    }

    /** Registers a buyer name in dashboard state. */
    private void registerBuyerInDashboard(String agentName) {
        if (agentName != null && !registeredBuyerNames.add(agentName)) {
            return;
        }
        buyerCount++;
        buyerCountLabel.setText(String.valueOf(buyerCount));
        updateBuyerStatus();
    }

    /** Registers a dealer name in dashboard state. */
    private void registerDealerInDashboard(String agentName) {
        if (agentName != null && !registeredDealerNames.add(agentName)) {
            return;
        }
        dealerCount++;
        dealerCountLabel.setText(String.valueOf(dealerCount));
        updateDealerStatus();
    }

    /** Extracts the quoted name value from text. */
    private String extractQuotedName(String msg) {
        int firstQuote = msg.indexOf('\'');
        if (firstQuote < 0) {
            return null;
        }
        int secondQuote = msg.indexOf('\'', firstQuote + 1);
        if (secondQuote <= firstQuote + 1) {
            return null;
        }
        return msg.substring(firstQuote + 1, secondQuote);
    }

    /** Records the failure report data for reporting. */
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

    /** Builds the failure report data model. */
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

    /** Appends a parsed failure field to the failure report. */
    private void appendFailureField(StringBuilder report, String payload, String key) {
        String value = extractBrokerField(payload, key);
        if (value != null && !value.isBlank()) {
            report.append(key).append(": ").append(value).append("\n");
        }
    }

    /** Converts a failure reason code into a readable label. */
    private String humanFailureReason(String reason) {
        return reason == null ? "Unknown" : reason.replace('_', ' ').toLowerCase();
    }

    /** Explains a failure reason for dashboard reporting. */
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

    /** Sends a control command to SpaceControl. */
    private void sendSpaceCommand(String command) {
        try {
            cc.createNewAgent(nextAgentName("space-command"), "org.example.agents.SpaceCommandAgent",
                    new Object[] { command, "space", "" }).start();
        } catch (Exception e) {
            System.err.println("Error sending command to SpaceControl: " + e.getMessage());
        }
    }

    /** Sends a control command to BrokerAgent. */
    private void sendBrokerCommand(String command) {
        try {
            cc.createNewAgent(nextAgentName("broker-command"), "org.example.agents.SpaceCommandAgent",
                    new Object[] { command, "broker", "" }).start();
        } catch (Exception e) {
            System.err.println("Error sending command to BrokerAgent: " + e.getMessage());
        }
    }

    /** Sends the dealer price adjustment command message. */
    private void sendDealerPriceAdjustment(String dealerName, String price) {
        try {
            cc.createNewAgent(nextAgentName("dealer-command"), "org.example.agents.SpaceCommandAgent",
                    new Object[] { "PRICE_ADJUSTMENT", dealerName, price }).start();
        } catch (Exception e) {
            showAlert("Error sending price adjustment: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /** Sends a command message to a named JADE agent. */
    private void sendAgentCommand(String agentName, String command) {
        sendAgentCommand(agentName, command, "");
    }

    /** Sends a command message to a named JADE agent. */
    private void sendAgentCommand(String agentName, String command, String content) {
        try {
            cc.createNewAgent(nextAgentName("agent-command"), "org.example.agents.SpaceCommandAgent",
                    new Object[] { command, agentName, content }).start();
        } catch (Exception e) {
            showAlert("Error sending command to " + agentName + ": " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /** Clears agents, logs, metrics, and visualiser state for a new run. */
    private void clearSession() {
        Set<String> agentsToKill = new LinkedHashSet<>();
        synchronized (buyerAgents) {
            agentsToKill.addAll(buyerAgents);
        }
        synchronized (dealerAgents) {
            agentsToKill.addAll(dealerAgents);
        }
        agentsToKill.addAll(waitingBuyerAgents);
        agentsToKill.addAll(registeredBuyerNames);
        agentsToKill.addAll(registeredDealerNames);

        for (String agentName : agentsToKill) {
            killAgentIfPresent(agentName);
        }

        sendBrokerCommand("RESET_SESSION");
        sendSpaceCommand("RESET_SESSION");

        buyerCount = 0;
        dealerCount = 0;
        dealsClosed = 0;
        failedDealsCount = 0;
        totalRevenue = 0;
        totalFixedFees = 0;
        totalCommission = 0;
        activeSessions = 0;
        currentCycle = 0;
        simulationStarted = false;
        isAutoPlay = false;

        buyerAgents.clear();
        dealerAgents.clear();
        waitingBuyerAgents.clear();
        registeredBuyerNames.clear();
        registeredDealerNames.clear();
        manualBuyerAgents.clear();
        failedDeals.clear();
        failureReasonCounts.clear();
        sessionMetaMap.clear();
        sessionPoints.clear();
        agentPoints.clear();
        sessionLastPrice.clear();
        listingModelMap.clear();

        buyerCountLabel.setText("0");
        dealerCountLabel.setText("0");
        transactionCountLabel.setText("0");
        failedDealsCountLabel.setText("0");
        revenueLabel.setText("RM 0.00");
        activeSessionsLabel.setText("0");
        activeSessionsLabelMini.setText("0");
        fixedFeesLabel.setText("RM 0");
        fixedFeesLabelMini.setText("RM 0");
        commissionLabel.setText("RM 0");
        commissionLabelMini.setText("RM 0");
        updatePlaybackButtonState();

        logArea.clear();
        rawLogArea.clear();
        if (dashboardEventsArea != null) dashboardEventsArea.clear();
        if (failuresArea != null) failuresArea.clear();
        if (sessionsArea != null) sessionsArea.clear();
        if (failureReportArea != null) {
            failureReportArea.setText("Failure summary\n----------------\nNo failed negotiations yet.");
        }
        if (manualLogArea != null) manualLogArea.clear();
        if (manualBuyerSelect != null) manualBuyerSelect.setValue(null);
        if (manualDealerSelect != null) {
            manualDealerSelect.setValue(null);
            manualDealerSelect.getItems().clear();
        }
        if (visualiserSessionSelect != null) {
            visualiserSessionSelect.setValue(null);
            visualiserSessionSelect.getItems().clear();
        }

        updateBuyerStatus();
        updateDealerStatus();
        updateNegotiationControlStatus();
        refreshNegotiationVisualiser();
        loggerLog("Session cleared. Broker, space, agents, logs, metrics, and visualisers reset.");
    }

    /** Terminates a JADE agent when it exists. */
    private void killAgentIfPresent(String agentName) {
        if (agentName == null || agentName.isBlank()) {
            return;
        }
        try {
            cc.getAgent(agentName).kill();
        } catch (Exception ignored) {
        }
    }

    /** Launches the JADE sniffer for registered demo agents. */
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

    /** Returns true when any buyer or dealer is registered. */
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

    /** Builds the sniffer targets data model. */
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

    /** Generates the next unique UI-created agent name. */
    private String nextAgentName(String prefix) {
        return prefix + "-" + System.nanoTime() + "-" + commandAgentCounter.incrementAndGet();
    }

    /** Creates the participants view UI component. */
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

    /** Creates the logs view UI component. */
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

    /** Creates the page UI component. */
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

    /** Creates the broker view UI component. */
    private VBox createBrokerView() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label headerLabel = new Label("Marketplace Dashboard");
        headerLabel.setStyle("-fx-font-size: 22; -fx-font-weight: 700; -fx-text-fill: " + PRIMARY_BLUE + ";");
        Label subLabel = new Label("Live broker metrics - negotiation trajectory - quick-start guide");
        subLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + TEXT_MUTED + ";");
        VBox hdr = new VBox(2, headerLabel, subLabel);

        HBox statsRow1 = new HBox(12,
                createStatCard("Active Buyers", buyerCountLabel, ACCENT_BLUE),
                createStatCard("Active Dealers", dealerCountLabel, WARNING_ORANGE),
                createStatCard("Active Sessions", activeSessionsLabel, "#8b5cf6"));
        HBox statsRow2 = new HBox(12,
                createStatCard("Deals Closed", transactionCountLabel, SUCCESS_GREEN),
                createStatCard("Failed Deals", failedDealsCountLabel, ERROR_RED),
                createStatCard("Total Revenue", revenueLabel, "#ec4899"));
        HBox statsRow3 = new HBox(12,
                createStatCard("Fixed Fees", fixedFeesLabel, "#06b6d4"),
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

    /** Creates the negotiation visualiser UI component. */
    private VBox createNegotiationVisualiser() {
        visualiserButtons.clear();

        Label title = new Label("Negotiation Visualiser");
        title.setStyle("-fx-font-size: 15; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");
        Label hint = new Label("Market, session, and agent views built from live broker-routed negotiation logs.");
        hint.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_MUTED + ";");
        VBox titleBox = new VBox(2, title, hint);

        HBox tabs = new HBox(6,
                createVisualiserTab("Session View", VisualiserView.SESSION),
                createVisualiserTab("Market View", VisualiserView.MARKET),
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

        showVisualiserView(VisualiserView.SESSION);
        refreshNegotiationVisualiser();
        return section;
    }

    /** Creates the visualiser scroll UI component. */
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

    /** Creates the visualiser tab UI component. */
    private Button createVisualiserTab(String text, VisualiserView view) {
        Button button = new Button(text);
        button.setStyle(visualiserTabStyle(false));
        button.setOnAction(e -> showVisualiserView(view));
        visualiserButtons.put(view, button);
        return button;
    }

    /** Builds the visualiser tab button CSS string. */
    private String visualiserTabStyle(boolean active) {
        return "-fx-font-size: 12; -fx-font-family: " + FONT_FAMILY + "; -fx-font-weight: 800;"
                + "-fx-padding: 7 12; -fx-background-radius: 8; -fx-border-radius: 8;"
                + "-fx-background-color: " + (active ? ACCENT_BLUE : SURFACE_ALT) + ";"
                + "-fx-text-fill: " + (active ? "white" : TEXT_MUTED) + ";"
                + "-fx-border-color: " + (active ? ACCENT_BLUE : BORDER_SUBTLE) + "; -fx-cursor: hand;";
    }

    /** Shows the selected visualiser view. */
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

    /** Refreshes whichever visualiser view is active. */
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

    /** Renders the Market View visualiser content. */
    private void renderMarketVisualiser() {
        if (marketVisualiserPane == null) {
            return;
        }
        marketVisualiserPane.getChildren().clear();
        List<SessionViewModel> sessions = buildSessionViewModels();
        List<ListingViewModel> listings = buildListingViewModels();

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

    /** Renders the Session View visualiser content. */
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

    /** Renders the Agent View visualiser content. */
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

    /** Creates the visual metric UI component. */
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

    /** Creates the chart card UI component. */
    private VBox createChartCard(String title, Node chart) {
        return createChartCard(title, chart, null);
    }

    /** Creates the chart card UI component. */
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

    /** Creates the chart legend UI component. */
    private HBox createChartLegend(Node... items) {
        HBox legend = new HBox(16, items);
        legend.setAlignment(javafx.geometry.Pos.CENTER);
        legend.setPadding(new Insets(4, 0, 0, 0));
        legend.setMinHeight(24);
        legend.setStyle("-fx-background-color: transparent;");
        return legend;
    }

    /** Creates the table card UI component. */
    private VBox createTableCard(String title, Node content) {
        VBox card = new VBox(8, createSectionLabel(title), content);
        card.setPadding(new Insets(14));
        card.setStyle(SOFT_PANEL_STYLE);
        return card;
    }

    /** Applies shared visibility, sizing, and axis styling to a chart. */
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

    /** Applies shared readable styling to a chart axis. */
    private void configureAxis(Axis<?> axis) {
        axis.setVisible(true);
        axis.setManaged(true);
        axis.setTickLabelsVisible(true);
        axis.setTickMarkVisible(true);
        axis.setStyle("-fx-tick-label-fill: " + TEXT_MUTED + ";"
                + "-fx-font-size: 10;"
                + "-fx-text-fill: " + DARK_TEXT + ";");
    }

    /** Bounds a numeric axis around visible values with padding. */
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

    /** Calculates a readable tick interval for chart axes. */
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

    /** Installs a tooltip on a chart data node after JavaFX creates it. */
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

    /** Applies stroke color and dash styling to a line-chart series. */
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

    /** Creates the price distribution chart UI component. */
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

    /** Creates the concession trend chart UI component. */
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

    /** Creates the offer timeline chart UI component. */
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

    /** Creates the agent outcome summary UI component. */
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

    /** Creates the outcome detail line UI component. */
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

    /** Creates the legend swatch UI component. */
    private HBox createLegendSwatch(String label, String color) {
        Region swatch = new Region();
        swatch.setMinSize(10, 10);
        swatch.setPrefSize(10, 10);
        swatch.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3;");
        Label text = new Label(label);
        text.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_MUTED + ";");
        return new HBox(5, swatch, text);
    }

    /** Creates the outcome summary row UI component. */
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

    /** Formats one outcome count segment for agent summaries. */
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

    /** Creates the agent concession chart UI component. */
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

    /** Creates the listing board UI component. */
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

    /** Creates the round log UI component. */
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

    /** Creates the agent performance list UI component. */
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

    /** Builds the visible empty-state panel for Agent View. */
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

    /** Creates the shared table-like grid layout. */
    private GridPane tableGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(4));
        return grid;
    }

    /** Adds a styled header row to a GridPane table. */
    private void addTableHeader(GridPane grid, int row, String... labels) {
        for (int col = 0; col < labels.length; col++) {
            Label label = new Label(labels[col]);
            label.setStyle("-fx-font-size: 11; -fx-font-weight: 800; -fx-text-fill: " + TEXT_MUTED + ";");
            grid.add(label, col, row);
        }
    }

    /** Adds one styled data row to a GridPane table. */
    private void addTableRow(GridPane grid, int row, String... values) {
        for (int col = 0; col < values.length; col++) {
            Label label = new Label(values[col]);
            label.setWrapText(true);
            label.setStyle("-fx-font-size: 12; -fx-text-fill: " + DARK_TEXT + ";");
            grid.add(label, col, row);
        }
    }

    /** Creates the empty state UI component. */
    private Node createEmptyState(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 13; -fx-text-fill: " + TEXT_MUTED + "; -fx-padding: 18;"
                + "-fx-background-color: " + SURFACE_ALT + "; -fx-background-radius: 8;");
        return label;
    }

    /** Builds the session view models data model. */
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

    /** Copies listing fields into a derived listing view model. */
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

    /** Returns the highest price recorded for a session. */
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

    /** Builds the agent view models data model. */
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
            double acceptedTotal = 0;
            for (SessionViewModel session : sessions) {
                String buyer = cleanAgentName(session.buyer);
                String dealer = cleanAgentName(session.dealer);
                if (!name.equals(buyer) && !name.equals(dealer)) continue;
                vm.sessions++;
                if ("ACCEPTED".equals(session.outcome)) {
                    vm.accepted++;
                    acceptedTotal += session.latestPrice != null ? session.latestPrice : 0;
                } else if ("NEGOTIATING".equals(session.outcome)) {
                    vm.pending++;
                } else {
                    vm.rejected++;
                }
            }
            vm.averageDealPrice = vm.accepted == 0 ? 0 : acceptedTotal / vm.accepted;
            vm.averageConcession = averageMovement(agentPoints.getOrDefault(name, Collections.emptyList()));
            models.add(vm);
        }
        return models;
    }

    /** Adds sanitized agent names from a source collection. */
    private void addCleanAgentNames(Set<String> target, Iterable<String> source) {
        for (String name : source) {
            String cleaned = cleanAgentName(name);
            if (cleaned != null) {
                target.add(cleaned);
            }
        }
    }

    /** Normalizes broker-parsed agent names for display and matching. */
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

    /** Builds the listing view models data model. */
    private List<ListingViewModel> buildListingViewModels() {
        Map<String, ListingViewModel> listings = new LinkedHashMap<>();
        for (Map.Entry<String, ListingViewModel> entry : listingModelMap.entrySet()) {
            ListingViewModel copy = copyListing(entry.getValue());
            copy.activeBuyers = 0;
            listings.put(entry.getKey(), copy);
        }

        Map<String, Set<String>> activeBuyersByListing = new HashMap<>();
        for (SessionViewModel session : buildSessionViewModels()) {
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

    /** Creates a mutable copy of a listing view model. */
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

    /** Records the dealer listing data for reporting. */
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

    /** Removes listings owned by a dealer from dashboard state. */
    private void removeDealerListings(String dealer) {
        listingModelMap.entrySet().removeIf(entry -> dealer.equals(entry.getValue().dealer));
    }

    /** Builds the stable dealer/car key for listing aggregation. */
    private String listingKey(String dealer, String car) {
        return valueOrNA(dealer) + "|" + valueOrNA(car);
    }

    /** Returns true when a trajectory point belongs to the buyer path. */
    private boolean isBuyerPoint(TrajectoryPoint point, SessionViewModel session) {
        if (point.event == TrajectoryEvent.START || point.event == TrajectoryEvent.OFFER) return true;
        return session != null && session.buyer != null && session.buyer.equals(point.agent);
    }

    /** Returns true when a trajectory point belongs to the dealer path. */
    private boolean isDealerPoint(TrajectoryPoint point, SessionViewModel session) {
        if (point.event == TrajectoryEvent.COUNTER || point.event == TrajectoryEvent.ACCEPT) return true;
        return session != null && session.dealer != null && session.dealer.equals(point.agent);
    }

    /** Returns true when an event represents a terminal session outcome. */
    private boolean isOutcomeEvent(TrajectoryEvent event) {
        return event == TrajectoryEvent.ACCEPT || event == TrajectoryEvent.WALKAWAY;
    }

    /** Adds averaged concession points to a chart series. */
    private void addAverageSeriesData(XYChart.Series<Number, Number> series, Map<Integer, double[]> values) {
        List<Integer> keys = new ArrayList<>(values.keySet());
        Collections.sort(keys);
        for (Integer key : keys) {
            double[] agg = values.get(key);
            if (agg[1] > 0) series.getData().add(new XYChart.Data<>(key, agg[0] / agg[1]));
        }
    }

    /** Calculates average movement between consecutive trajectory points. */
    private double averageMovement(List<TrajectoryPoint> points) {
        if (points.size() < 2) return 0;
        List<TrajectoryPoint> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.comparingInt(p -> p.cycle));
        double total = 0;
        for (int i = 1; i < sorted.size(); i++) total += Math.abs(sorted.get(i).price - sorted.get(i - 1).price);
        return total / (sorted.size() - 1);
    }

    /** Returns the shared ComboBox style string. */
    private String comboBoxStyle() {
        return "-fx-font-size: 12; -fx-font-family: " + FONT_FAMILY + "; -fx-background-color: " + SURFACE
                + "; -fx-border-color: " + BORDER_SUBTLE
                + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 2 6;";
    }

    /** Creates a compact label for legends and helper text. */
    private Label makeSmallLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12; -fx-font-weight: 700; -fx-text-fill: " + TEXT_MUTED + ";");
        return label;
    }

    /** Creates the badge UI component. */
    private Label createBadge(String text, String color) {
        Label badge = new Label(text);
        badge.setStyle("-fx-font-size: 11; -fx-font-weight: 800; -fx-text-fill: white;"
                + "-fx-background-color: " + color + "; -fx-background-radius: 6; -fx-padding: 3 8;");
        return badge;
    }

    /** Selects a display color for an outcome status. */
    private String outcomeColor(String outcome) {
        if ("ACCEPTED".equals(outcome)) return SUCCESS_GREEN;
        if ("NEGOTIATING".equals(outcome)) return WARNING_ORANGE;
        return ERROR_RED;
    }

    /** Shortens a label while preserving recognizable text. */
    private String shortLabel(String value, String fallback) {
        return shortLabel(value, fallback, 18);
    }

    /** Shortens a label while preserving recognizable text. */
    private String shortLabel(String value, String fallback, int maxLength) {
        String label = value != null && !value.isBlank() ? value : fallback;
        if (label == null) {
            return "N/A";
        }
        return label.length() > maxLength ? label.substring(0, Math.max(1, maxLength - 1)) + "..." : label;
    }

    /** Converts a trajectory event into a round-log action label. */
    private String actionLabel(TrajectoryEvent event) {
        if (event == TrajectoryEvent.START) return "initial offer";
        if (event == TrajectoryEvent.OFFER) return "buyer offer";
        if (event == TrajectoryEvent.COUNTER) return "dealer counter";
        if (event == TrajectoryEvent.ACCEPT) return "accept";
        if (event == TrajectoryEvent.WALKAWAY) return "no deal";
        return "price update";
    }

    /** Builds display initials from an agent name. */
    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.split("[-_\\s]+");
        String first = parts.length > 0 && !parts[0].isBlank() ? parts[0].substring(0, 1) : "";
        String second = parts.length > 1 && !parts[1].isBlank() ? parts[1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }

    /** Formats a price value as Malaysian Ringgit. */
    private String money(Integer value) {
        return value == null ? "N/A" : "RM " + value;
    }

    /** Formats a price value as Malaysian Ringgit. */
    private String money(Double value) {
        return value == null ? "N/A" : String.format("RM %.0f", value);
    }

    /** Formats a price value as Malaysian Ringgit. */
    private String money(double value) {
        return String.format("RM %.0f", value);
    }

    /** Formats a signed price delta as Malaysian Ringgit. */
    private String signedMoney(double value) {
        return (value >= 0 ? "+" : "-") + money(Math.abs(value));
    }


    /** Updates the dealer status display state. */
    private void updateDealerStatus() {
        if (dealerCount == 0) {
            dealerStatusLabel
                    .setText("Go to Dealer Portal -> Register at least ONE dealer with car inventory (Required first!)");
            dealerStatusLabel.setStyle("-fx-font-size: 13; -fx-text-fill: " + ERROR_RED + "; -fx-font-weight: bold;");
        } else {
            dealerStatusLabel.setText("/ " + dealerCount + " dealer agent(s) registered - Ready to accept buyers!");
            dealerStatusLabel
                    .setStyle("-fx-font-size: 13; -fx-text-fill: " + SUCCESS_GREEN + "; -fx-font-weight: bold;");
        }
    }

    /** Updates the buyer status display state. */
    private void updateBuyerStatus() {
        if (buyerCount == 0) {
            updateBuyerStatus.setText("Go to Buyer Portal -> Register buyer(s) with desired car & budget");
            updateBuyerStatus.setStyle("-fx-font-size: 13; -fx-text-fill: " + ERROR_RED + "; -fx-font-weight: bold;");
        } else {
            updateBuyerStatus.setText("/ " + buyerCount + " buyer agent(s) registered - Ready to accept dealers!");
            updateBuyerStatus
                    .setStyle("-fx-font-size: 13; -fx-text-fill: " + SUCCESS_GREEN + "; -fx-font-weight: bold;");
        }
    }

    /** Creates the stat card UI component. */
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

    /** Creates the section label UI component. */
    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");
        return label;
    }

    /** Creates the checklist item UI component. */
    private Label createChecklistItem(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12; -fx-font-weight: 600; -fx-text-fill: " + DARK_TEXT + ";"
                + "-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 8 10;");
        return label;
    }

    /** Creates the buyer view UI component. */
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
        form.add(makeFieldLabel("Max Budget (RM)", "Upper limit - buyer opens at ~70%"), 0, 2);
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
                showAlert("All fields are required!", Alert.AlertType.WARNING);
                return;
            }
            if (dealerCount == 0) {
                showAlert("No dealers registered!\nPlease register a dealer first.", Alert.AlertType.ERROR);
                return;
            }
            try {
                double b = Double.parseDouble(budgetStr);
                if (b <= 0) {
                    showAlert("Budget must be > 0", Alert.AlertType.WARNING);
                    return;
                }
                boolean isManual = manualControlCheck.isSelected();
                cc.createNewAgent(name, "org.example.agents.BuyerAgent",
                        new Object[] { car, budgetStr, logger, buildNegotiationConfig(), true, isManual }).start();
                buyerAgents.add(name);
                registerBuyerInDashboard(name);
                if (isManual) {
                    manualBuyerAgents.add(name);
                    if (manualBuyerSelect != null && manualBuyerSelect.getValue() == null) {
                        manualBuyerSelect.setValue(name);
                    }
                }
                waitingBuyerAgents.add(name);
                updateNegotiationControlStatus();
                refreshNegotiationVisualiser();
                logger.log("Buyer '" + name + "' added - " + car + " @ RM" + budgetStr);
                buyerName.clear();
                carModel.setValue(null);
                budget.clear();
                showAlert("Buyer " + name + " added. Press Start.", Alert.AlertType.INFORMATION);
            } catch (NumberFormatException ex) {
                showAlert("Budget must be a valid number.", Alert.AlertType.ERROR);
            } catch (Exception ex) {
                logger.log("Error: " + ex.getMessage());
                showAlert(ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        form.add(addBuyerBtn, 1, 4);

        card.getChildren().addAll(formTitle, form);
        box.getChildren().addAll(headerLabel, subLabel, prereqBanner, card);
        VBox.setVgrow(card, Priority.SOMETIMES);
        return box;
    }

    /** Creates a standardized form-field label. */
    private VBox makeFieldLabel(String title, String subtitle) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13; -fx-text-fill: " + DARK_TEXT + "; -fx-font-weight: 600;");
        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 11; -fx-text-fill: " + TEXT_MUTED + ";");
        return new VBox(2, titleLbl, subLbl);
    }

    /** Applies shared spacing and sizing to a participant form. */
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

    /** Horizontal toolbar replacing the old collapsible sidebar. */
    private HBox createActionBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(12, 20, 12, 20));
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #e0f2fe; "
                + "-fx-border-color: #bae6fd; -fx-border-width: 0 0 1 0;");

        Button demoBtn = createBarButton("Demo Setup", PRIMARY_BLUE);
        Button startBtn = createBarButton("Start Buyers", SUCCESS_GREEN);
        playPauseBtn = createBarButton(isAutoPlay ? "Pause" : "Resume", WARNING_ORANGE);
        Button stepBtn = createBarButton("Step Cycle", ACCENT_BLUE);
        Button stopBtn = createBarButton("Stop", ERROR_RED);
        Button clearSessionBtn = createBarButton("Clear Session", "#64748b");
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
            simulationStarted = true;
            isAutoPlay = true;
            updatePlaybackButtonState();
            sendSimulationPauseCommand(true);
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
        clearSessionBtn.setOnAction(e -> clearSession());
        sniffBtn.setOnAction(e -> launchSniffer(msg -> {
            logArea.appendText("[System] " + msg + "\n");
            rawLogArea.appendText(msg + "\n");
        }));

        // Tick positions 0-6 map to speed multipliers.
        // Delay in ms: 4000, 2000, 1000, 500, 200, 100, 50
        long[] speedDelays = { 4000, 2000, 1000, 500, 200, 100, 50 };
        String[] speedLabels = { "0.25x", "0.5x", "1x", "2x", "5x", "10x", "20x" };

        Slider speedSlider = new Slider(0, speedDelays.length - 1, 2);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setSnapToTicks(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(false);
        speedSlider.setPrefWidth(130);
        speedSlider.setStyle("-fx-padding: 0 4;");

        Label speedLabel = new Label("1x");
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
        updatePlaybackButtonState();
        updateNegotiationControlStatus();

        updateDealerStatus();
        updateBuyerStatus();

        bar.getChildren().addAll(demoBtn, startBtn, playPauseBtn, stepBtn, stopBtn, clearSessionBtn, sniffBtn,
                sep2, speedBox,
                sep, negotiationControlStatusLabel);
        return bar;
    }

    /**
     * Sends a SET_SPEED command to SpaceControl with the desired cycle delay in
     * milliseconds.
     */
    private void sendSpeedCommand(long delayMs) {
        try {
            cc.createNewAgent(nextAgentName("speed-command"), "org.example.agents.SpaceCommandAgent",
                    new Object[] { "SET_SPEED", "space", String.valueOf(delayMs) }).start();
        } catch (Exception e) {
            System.err.println("Error sending SET_SPEED: " + e.getMessage());
        }
    }

    /** Creates the bar button UI component. */
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

    /** Creates the demo scenario UI component. */
    private void createDemoScenario() {
        long demoId = demoScenarioCounter.incrementAndGet();
        NegotiationConfig config = buildDemoNegotiationConfig();

        try {
            String[][] dealers = new String[][] {
                    { "DemoAutoA-" + demoId, "Toyota Camry", "100000", "6" },
                    { "BudgetCars-" + demoId, "Honda Civic", "87000", "3" },
                    { "FamilyDrive-" + demoId, "Honda CR-V", "145000", "3" }
            };
            for (String[] dealer : dealers) {
                createDemoDealer(dealer[0], dealer[1], dealer[2], dealer[3], config);
            }

            String[][] buyers = new String[][] {
                    { "DemoBuyerPremium-" + demoId, "Toyota Camry", "116000" },
                    { "DemoBuyerStubborn-" + demoId, "Toyota Camry", "108000" },
                    { "DemoBuyerTight-" + demoId, "Toyota Camry", "98000" },
                    { "DemoBuyerCivic-" + demoId, "Honda Civic", "97000" },
                    { "DemoBuyerSUV-" + demoId, "Honda CR-V", "155000" },
                    { "DemoBuyerStretch-" + demoId, "Honda CR-V", "165000" },
                    { "DemoBuyerBudget-" + demoId, "Toyota Camry", "65000" }
            };
            for (String[] buyer : buyers) {
                createDemoBuyer(buyer[0], buyer[1], buyer[2], config);
            }
            createDemoBuyer("DemoBuyerOverdrive-" + demoId, "Toyota Camry", "102000",
                    buildOverdriveNegotiationConfig(config));
            int demoBuyerCount = buyers.length + 1;

            updateNegotiationControlStatus();
            refreshNegotiationVisualiser();

            loggerLog("Demo scenario " + demoId + " added: " + dealers.length + " well-stocked dealers and "
                    + demoBuyerCount + " waiting buyers. Press Start to stress test negotiation and strategy switching.");
            showAlert("Demo scenario added. Press Start to begin negotiation.", Alert.AlertType.INFORMATION);
        } catch (Exception ex) {
            showAlert("Error creating demo scenario: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /** Builds the demo negotiation config data model. */
    private NegotiationConfig buildDemoNegotiationConfig() {
        NegotiationConfig base = buildNegotiationConfig();
        return new NegotiationConfig(
                base.getStrategy(),
                Math.max(base.getDeadlineCycles(), 30),
                Math.min(base.getBuyerStartPercent(), 0.62),
                Math.max(base.getDealerReservePercent(), 0.78),
                Math.max(base.getMaxRoundsPerDealer(), 10),
                Math.max(base.getMaxSearchRetries(), 1),
                Math.max(base.getStuckRoundsBeforeAcceleration(), 1),
                base.getManualDealerTargetPercent(),
                base.getStrategySwitchCycle() > 0 ? Math.min(base.getStrategySwitchCycle(), 6) : 6,
                base.getSwitchStrategy() == base.getStrategy() ? NegotiationConfig.Strategy.CONCEDER
                        : base.getSwitchStrategy());
    }

    /** Builds the overdrive negotiation config data model. */
    private NegotiationConfig buildOverdriveNegotiationConfig(NegotiationConfig base) {
        return new NegotiationConfig(
                NegotiationConfig.Strategy.LINEAR,
                Math.max(24, Math.min(base.getDeadlineCycles(), 30)),
                Math.max(base.getBuyerStartPercent(), 0.70),
                base.getDealerReservePercent(),
                Math.max(base.getMaxRoundsPerDealer(), 12),
                base.getMaxSearchRetries(),
                2,
                base.getManualDealerTargetPercent(),
                6,
                NegotiationConfig.Strategy.CONCEDER);
    }

    /** Creates the demo dealer UI component. */
    private void createDemoDealer(String name, String car, String price, String stock, NegotiationConfig config)
            throws Exception {
        cc.createNewAgent(name, "org.example.agents.DealerAgent",
                new Object[] { car, price, stock, appLogger, config }).start();
        loggerLog("Dealer '" + name + "' listed " + car + " @ RM" + price + " | Stock: " + stock);
        dealerAgents.add(name);
        registerDealerInDashboard(name);
        recordDealerListing(name, car, Integer.parseInt(price), Integer.parseInt(stock), config);
    }

    /** Creates the demo buyer UI component. */
    private void createDemoBuyer(String name, String car, String budget, NegotiationConfig config) throws Exception {
        cc.createNewAgent(name, "org.example.agents.BuyerAgent",
                new Object[] { car, budget, appLogger, config, true }).start();
        buyerAgents.add(name);
        waitingBuyerAgents.add(name);
        registerBuyerInDashboard(name);
        updateNegotiationControlStatus();
        loggerLog("Buyer '" + name + "' added and waiting - " + car + " budget RM" + budget);
        refreshNegotiationVisualiser();
    }

    /** Creates the dealer view UI component. */
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
                showAlert("All fields are required!", Alert.AlertType.WARNING);
                return;
            }

            try {
                // Validate price is numeric
                double priceAmount = Double.parseDouble(price);
                if (priceAmount <= 0) {
                    showAlert("Price must be greater than 0", Alert.AlertType.WARNING);
                    return;
                }
                int stockAmount = Integer.parseInt(stock);
                if (stockAmount <= 0) {
                    showAlert("Stock must be at least 1", Alert.AlertType.WARNING);
                    return;
                }

                NegotiationConfig config = buildNegotiationConfig();
                cc.createNewAgent(name, "org.example.agents.DealerAgent",
                        new Object[] { car, price, stock, logger, config }).start();
                logger.log("Dealer '" + name + "' listed " + car + " @ RM" + price + " | Stock: " + stock);
                dealerAgents.add(name);
                registerDealerInDashboard(name);
                recordDealerListing(name, car, (int) priceAmount, stockAmount, config);
                dealerName.clear();
                carModel.setValue(null);
                retailPrice.clear();
                stockField.clear();
                showAlert("Dealer " + name + " registered with " + stock + " unit(s)!", Alert.AlertType.INFORMATION);
            } catch (NumberFormatException ex) {
                showAlert("Price and Stock must be valid numbers", Alert.AlertType.ERROR);
            } catch (Exception ex) {
                logger.log("Error creating dealer: " + ex.getMessage());
                showAlert("Error: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        form.add(addDealerBtn, 1, 4);

        formSection.getChildren().addAll(formTitle, form);

        box.getChildren().addAll(headerLabel, subLabel, infoBanner, formSection);
        VBox.setVgrow(formSection, Priority.SOMETIMES);

        return box;
    }

    /** Creates the market analysis view UI component. */
    private VBox createMarketAnalysisView() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label headerLabel = new Label("Negotiation Settings");
        headerLabel.setStyle("-fx-font-size: 25; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");

        box.getChildren().addAll(headerLabel, createSimulationControlPanel());
        return box;
    }

    /** Creates the simulation control panel UI component. */
    private VBox createSimulationControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle(PANEL_STYLE);

        Label title = new Label("Strategy Defaults");
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

    /** Builds the negotiation config data model. */
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

    /** Converts a percent text field into a decimal ratio. */
    private double percentFieldToRatio(TextField field, int fallbackPercent) {
        String raw = field.getText().trim();
        double value = raw.isEmpty() ? fallbackPercent : Double.parseDouble(raw);
        value = Math.max(1, Math.min(100, value));
        return value / 100.0;
    }

    /** Creates the activity log view UI component. */
    private VBox createActivityLogView() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(4));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label headerLabel = new Label("System Timeline");
        headerLabel.setStyle("-fx-font-size: 22; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle(textAreaStyle(true));
        logArea.setPrefRowCount(30);

        rawLogArea.setEditable(false);
        rawLogArea.setWrapText(true);
        rawLogArea.setStyle(textAreaStyle(true));
        rawLogArea.setPrefRowCount(30);

        TabPane logTabs = new TabPane();
        logTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab timelineTab = new Tab("Timeline", logArea);
        Tab rawTab = new Tab("Raw ACL Log", rawLogArea);
        logTabs.getTabs().addAll(timelineTab, rawTab);

        HBox controlBox = new HBox(12);
        controlBox.setPadding(new Insets(15));
        controlBox.setStyle(SOFT_PANEL_STYLE);

        Button copyTimelineBtn = createStyledButton("Copy Timeline", ACCENT_BLUE);
        copyTimelineBtn.setPrefWidth(150);
        copyTimelineBtn.setOnAction(e -> {
            var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            var content = new javafx.scene.input.ClipboardContent();
            content.putString(logArea.getText());
            clipboard.setContent(content);
            showAlert("Timeline copied to clipboard!", Alert.AlertType.INFORMATION);
        });

        Button copyRawBtn = createStyledButton("Copy Raw", ACCENT_BLUE);
        copyRawBtn.setPrefWidth(120);
        copyRawBtn.setOnAction(e -> {
            var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            var content = new javafx.scene.input.ClipboardContent();
            content.putString(rawLogArea.getText());
            clipboard.setContent(content);
            showAlert("Raw ACL log copied to clipboard!", Alert.AlertType.INFORMATION);
        });

        Button clearBtn = createStyledButton("Clear Log", ERROR_RED);
        clearBtn.setPrefWidth(120);
        clearBtn.setOnAction(e -> {
            logArea.clear();
            rawLogArea.clear();
        });

        controlBox.getChildren().addAll(copyTimelineBtn, copyRawBtn, clearBtn);

        box.getChildren().addAll(headerLabel, logTabs, controlBox);
        VBox.setVgrow(logTabs, Priority.ALWAYS);
        return box;
    }

    /** Creates the failures view UI component. */
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
            failedDeals.clear();
            failureReasonCounts.clear();
        });

        controlBox.getChildren().addAll(copyBtn, clearBtn);

        box.getChildren().addAll(headerLabel, failureReportArea, createSectionLabel("Raw failure log"),
                failuresArea, controlBox);
        VBox.setVgrow(failuresArea, Priority.ALWAYS);
        return box;
    }

    /** Creates the sessions view with broker session start, settlement, and failure logs. */
    private VBox createSessionsView() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label header = new Label("Negotiation Sessions");
        header.setStyle("-fx-font-size: 25; -fx-font-weight: 800; -fx-text-fill: " + PRIMARY_BLUE + ";");

        // Mini stat row
        HBox miniStats = new HBox(16);
        VBox activeCard = createStatCard("Active", activeSessionsLabelMini, "#8b5cf6");
        VBox feesCard = createStatCard(" Fixed Fees", fixedFeesLabelMini, "#06b6d4");
        VBox commCard = createStatCard("Commission (5% deals)", commissionLabelMini, SUCCESS_GREEN);
        for (VBox c : new VBox[] { activeCard, feesCard, commCard }) {
            c.setPrefWidth(180);
            miniStats.getChildren().add(c);
        }

        // Hint label
        Label hint = new Label(
                "Each row below is a SESSION START, DEAL SETTLED, or NO DEAL event logged by the broker.");
        hint.setStyle("-fx-font-size: 12; -fx-text-fill: " + TEXT_MUTED + "; -fx-wrap-text: true;");
        hint.setMaxWidth(Double.MAX_VALUE);

        // Session event log
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

    /** Creates the manual play view UI component. */
    private VBox createManualPlayView() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: " + LIGHT_GRAY + ";");

        Label title = new Label(" Manual Mode");
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

        // Shortlist controls
        HBox shortlistBox = new HBox(10);
        shortlistBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        manualDealerSelect = new ComboBox<>();
        manualDealerSelect.setPromptText("Select Dealer");
        manualDealerSelect.setStyle(comboBoxStyle());
        manualFirstOfferField = createStyledTextField("First Offer RM");
        manualSendFirstOfferBtn = createStyledButton("Send First Offer", ACCENT_BLUE);
        manualSendFirstOfferBtn.setDisable(true);
        shortlistBox.getChildren().addAll(manualDealerSelect, manualFirstOfferField, manualSendFirstOfferBtn);

        // Negotiation controls
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

        // Actions
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

    /** Disables manual counter controls when no counter is active. */
    private void disableCounterControls() {
        manualSendCounterBtn.setDisable(true);
        manualAcceptDealBtn.setDisable(true);
        manualWalkAwayBtn.setDisable(true);
    }

    /** Returns true when text represents an integer greater than zero. */
    private boolean isPositiveInteger(String value) {
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Handles the manual prompt log event. */
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

    /** Creates the styled text field UI component. */
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

    /** Creates the styled car combo box UI component. */
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

    /** Creates the styled button UI component. */
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

    /** Shows a modal alert with the provided message and type. */
    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Error" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Returns a display-safe value or N/A. */
    private String valueOrNA(String value) {
        return value != null && !value.isBlank() ? value : "N/A";
    }

    public interface UILogger {
        void log(String message);
    }
}

