package com.realestate.app.util;

import com.realestate.app.model.Agent;

import java.util.ArrayList;
import java.util.List;

public class AgentBST {
    private AgentNode root;

    public AgentBST() {
        root = null;
    }

    public void insert(Agent agent) {
        if (agent == null || agent.getId() == null || agent.getId().isBlank()) {
            return;
        }
        root = insertRec(root, agent);
    }

    private AgentNode insertRec(AgentNode node, Agent agent) {
        if (node == null) {
            return new AgentNode(agent);
        }

        int comparison = agent.getId().compareTo(node.getAgent().getId());
        if (comparison < 0) {
            node.setLeft(insertRec(node.getLeft(), agent));
        } else if (comparison > 0) {
            node.setRight(insertRec(node.getRight(), agent));
        } else {
            node.setAgent(agent);
        }
        return node;
    }

    public Agent search(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        AgentNode result = searchRec(root, id);
        return result != null ? result.getAgent() : null;
    }

    private AgentNode searchRec(AgentNode node, String id) {
        if (node == null || node.getAgent().getId().equals(id)) {
            return node;
        }

        if (id.compareTo(node.getAgent().getId()) < 0) {
            return searchRec(node.getLeft(), id);
        }
        return searchRec(node.getRight(), id);
    }

    public void delete(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        root = deleteRec(root, id);
    }

    private AgentNode deleteRec(AgentNode node, String id) {
        if (node == null) {
            return null;
        }

        int comparison = id.compareTo(node.getAgent().getId());
        if (comparison < 0) {
            node.setLeft(deleteRec(node.getLeft(), id));
            return node;
        }

        if (comparison > 0) {
            node.setRight(deleteRec(node.getRight(), id));
            return node;
        }

        if (node.getLeft() == null) {
            return node.getRight();
        }

        if (node.getRight() == null) {
            return node.getLeft();
        }

        AgentNode successor = findMinNode(node.getRight());
        node.setAgent(successor.getAgent());
        node.setRight(deleteRec(node.getRight(), successor.getAgent().getId()));
        return node;
    }

    public List<Agent> inOrderTraversal() {
        List<Agent> ordered = new ArrayList<>();
        inOrderRec(root, ordered);
        return ordered;
    }

    private void inOrderRec(AgentNode node, List<Agent> ordered) {
        if (node == null) {
            return;
        }

        inOrderRec(node.getLeft(), ordered);
        ordered.add(node.getAgent());
        inOrderRec(node.getRight(), ordered);
    }

    private AgentNode findMinNode(AgentNode node) {
        AgentNode current = node;
        while (current != null && current.getLeft() != null) {
            current = current.getLeft();
        }
        return current;
    }
}
