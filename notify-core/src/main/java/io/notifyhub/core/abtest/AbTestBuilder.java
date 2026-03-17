package io.notifyhub.core.abtest;

import io.notifyhub.core.NotificationBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A/B test builder for notification variants.
 *
 * <pre>{@code
 * notify.to(user).via(EMAIL)
 *     .abTest("welcome-experiment")
 *         .variant("A", b -> b.template("welcome-v1"))
 *         .variant("B", b -> b.template("welcome-v2"))
 *         .split(50, 50)
 *     .send();
 * }</pre>
 */
public class AbTestBuilder {

    private final NotificationBuilder parent;
    private final String experimentName;
    private final String recipient;
    private final List<VariantEntry> variants = new ArrayList<>();

    public AbTestBuilder(NotificationBuilder parent, String experimentName, String recipient) {
        this.parent = parent;
        this.experimentName = experimentName;
        this.recipient = recipient;
    }

    public AbTestBuilder variant(String name, Consumer<NotificationBuilder> configurator) {
        variants.add(new VariantEntry(name, configurator, 1)); // default weight 1
        return this;
    }

    /**
     * Set explicit weights for variants. The number of weights must match the number of variants.
     * Weights are relative (e.g., 50, 50 means 50/50 split; 70, 30 means 70/30).
     */
    public NotificationBuilder split(int... weights) {
        if (weights.length != variants.size()) {
            throw new IllegalArgumentException(
                "Number of weights (" + weights.length + ") must match number of variants (" + variants.size() + ")");
        }
        for (int i = 0; i < weights.length; i++) {
            variants.set(i, new VariantEntry(variants.get(i).name, variants.get(i).configurator, weights[i]));
        }

        // Select variant based on deterministic hash
        String selectedVariantName = selectVariant();
        VariantEntry selected = variants.stream()
            .filter(v -> v.name.equals(selectedVariantName))
            .findFirst()
            .orElseThrow();

        // Apply the selected variant's configuration to the parent builder
        selected.configurator.accept(parent);
        return parent;
    }

    /**
     * Deterministically select a variant based on hash of experimentName + recipient.
     */
    String selectVariant() {
        int hash = deterministicHash(experimentName + "|" + (recipient != null ? recipient : ""));
        int totalWeight = variants.stream().mapToInt(v -> v.weight).sum();
        int bucket = Math.abs(hash % totalWeight);

        int cumulative = 0;
        for (VariantEntry v : variants) {
            cumulative += v.weight;
            if (bucket < cumulative) {
                return v.name;
            }
        }
        return variants.get(variants.size() - 1).name;
    }

    private static int deterministicHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16) | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
        } catch (Exception e) {
            return input.hashCode(); // fallback
        }
    }

    String getExperimentName() { return experimentName; }
    List<VariantEntry> getVariants() { return List.copyOf(variants); }

    record VariantEntry(String name, Consumer<NotificationBuilder> configurator, int weight) {}
}
