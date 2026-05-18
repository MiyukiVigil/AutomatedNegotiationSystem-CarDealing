// Coordinates market time by broadcasting cycle updates to registered negotiation agents.
// Pausing lets the UI add several agents before the next shared negotiation cycle advances.

package org.example.agents;

import jade.core.*;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import org.example.MainUI.UILogger;
import java.util.HashSet;
import java.util.Set;

// Owns the market clock and control commands used by buyers, dealers, and the UI.
public class SpaceControl extends Agent {
    private final Set<AID> activeAgents = new HashSet<>();
    private final Set<AID> completeAgents = new HashSet<>();
    private int cycleCount = 0;
    private boolean isPaused = false;
    private boolean cycleAdvancePending = false;
    // Inter-cycle delay in milliseconds, mutable so the UI speed slider can adjust it at runtime.
    private long autoCycleDelayMs = 1000;
    private UILogger logger;

    // Initializes the control loop that handles registration, pause/resume, stepping, and speed commands.
    protected void setup() {
        if (getArguments() != null && getArguments().length > 0) {
            this.logger = (UILogger) getArguments()[0];
        }

        log("Initializing Space Control");

        // Add behavior to JADE
        addBehaviour(
                new CyclicBehaviour() {
                    public void action() {
                        ACLMessage msg = receive();
                        if (msg != null) {
                            String ontology = msg.getOntology();

                            if ("REGISTER".equals(ontology)) {
                                activeAgents.add(msg.getSender());
                                log("Registered: " + msg.getSender().getLocalName());

                                if (!isPaused && activeAgents.size() == 1 && cycleCount == 0) {
                                    broadcastCycle(1);
                                }
                            } else if ("DEREGISTER".equals(ontology)) {
                                activeAgents.remove(msg.getSender());
                                // keep cycling after an agent deregisters if others are still active
                                if (!isPaused && !activeAgents.isEmpty()) {
                                    log("Agent left market. Continuing cycle for remaining agents.");
                                    scheduleCycleAdvance();
                                }
                            } else if ("ACTION_COMPLETED".equals(ontology)) {
                                if (!isPaused) {
                                    log("Market Action Detected! Scheduling next cycle.");
                                    scheduleCycleAdvance();
                                } else {
                                    log("Market Action Detected, but system is PAUSED. Standing by for manual input.");
                                }
                            } else if ("PAUSE".equals(ontology)) {
                                isPaused = true;
                                log("Space Control paused.");
                            } else if ("RESUME".equals(ontology)) {
                                isPaused = false;
                                log("Space Control resumed.");
                                if (!activeAgents.isEmpty()) {
                                    scheduleCycleAdvance();
                                }
                            } else if ("STEP".equals(ontology)) {
                                if (!activeAgents.isEmpty()) {
                                    cycleAdvancePending = false;
                                    broadcastCycle(1);
                                }
                            } else if ("SET_SPEED".equals(ontology)) {
                                try {
                                    long newDelay = Long.parseLong(msg.getContent().trim());
                                    if (newDelay >= 50) {
                                        autoCycleDelayMs = newDelay;
                                        log("Cycle delay set to " + autoCycleDelayMs + " ms.");
                                    }
                                } catch (NumberFormatException ignored) {
                                    log("SET_SPEED: invalid content '" + msg.getContent() + "' — ignored.");
                                }
                            }
                        } else {
                            block();
                        }
                    }
                });
    }

    // Schedules the next automatic cycle once all current market actions have completed.
    private void scheduleCycleAdvance() {
        if (cycleAdvancePending || activeAgents.isEmpty()) {
            return;
        }

        cycleAdvancePending = true;
        addBehaviour(new WakerBehaviour(this, autoCycleDelayMs) {
            protected void onWake() {
                cycleAdvancePending = false;
                if (!isPaused && !activeAgents.isEmpty()) {
                    broadcastCycle(1);
                }
            }
        });
    }

    // Advances the market clock and sends CYCLE_UPDATE to every active agent.
    private void broadcastCycle(int increment) {
        cycleCount += increment;

        ACLMessage updateCycle = new ACLMessage(ACLMessage.PROPAGATE);
        updateCycle.setOntology("CYCLE_UPDATE");
        updateCycle.setContent(String.valueOf(cycleCount));

        for (jade.core.AID agent : activeAgents) {
            updateCycle.addReceiver(agent);
        }

        send(updateCycle);
        log("Cycle Shift: " + cycleCount);
    }

    // Sends SpaceControl messages to the UI logger when one is configured.
    private void log(String message) {
        if (logger != null) {
            logger.log(message);
        }
    }
}
