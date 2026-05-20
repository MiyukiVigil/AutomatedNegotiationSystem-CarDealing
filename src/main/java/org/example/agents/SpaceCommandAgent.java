package org.example.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

/** Short-lived helper agent that sends one control ACL message to a target agent. */
public class SpaceCommandAgent extends Agent {
    /** Reads command arguments, sends the ACL control message, and terminates. */
    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args == null || args.length == 0 || args[0] == null) {
            doDelete();
            return;
        }

        String command = String.valueOf(args[0]).trim();
        String receiver = args.length > 1 && args[1] != null ? String.valueOf(args[1]).trim() : "space";
        String content = args.length > 2 && args[2] != null ? String.valueOf(args[2]).trim() : "";
        ACLMessage control = new ACLMessage(ACLMessage.INFORM);
        control.addReceiver(new AID(receiver, AID.ISLOCALNAME));
        control.setOntology(command);
        control.setContent(content);
        send(control);

        doDelete();
    }
}
