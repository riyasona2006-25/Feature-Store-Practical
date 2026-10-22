
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FeatureStore {

    // Store registered feature definitions
    private final Map<String, Map<String, Object>> repository =
            new LinkedHashMap<>();

    // Offline store keeps all historical records
    private final Map<String, List<Map<String, Object>>> offlineStore =
            new LinkedHashMap<>();

    // Online store keeps the latest record for each entity
    private final Map<String, Map<Object, Map<String, Object>>> onlineStore =
            new LinkedHashMap<>();

    // Register a feature group
    public void registerFeature(
            String name,
            String entityKey,
            List<String> columns,
            String description) {

        if (repository.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Feature group '" + name + "' is already registered.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("entity_key", entityKey);
        metadata.put("columns", new ArrayList<>(columns));
        metadata.put("description", description);
        metadata.put(
                "registered_at",
                LocalDateTime.now().format(
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        repository.put(name, metadata);

        System.out.println("Registered feature group: " + name);
    }

    // Validate and ingest records into offline and online stores
    public void ingest(
            String name,
            List<Map<String, Object>> records) {

        if (!repository.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Register '" + name + "' before ingestion.");
        }

        Map<String, Object> metadata = repository.get(name);
        String entityKey = (String) metadata.get("entity_key");

        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) metadata.get("columns");

        List<String> required = new ArrayList<>();
        required.add(entityKey);
        required.addAll(columns);

        List<Map<String, Object>> data = new ArrayList<>();

        for (Map<String, Object> record : records) {
            List<String> missing = new ArrayList<>();

            for (String column : required) {
                if (!record.containsKey(column)) {
                    missing.add(column);
                }
            }

            if (!missing.isEmpty()) {
                throw new IllegalArgumentException(
                        "Missing required columns: " + missing);
            }

            if (record.get(entityKey) == null) {
                throw new IllegalArgumentException(
                        "Entity key cannot contain missing values.");
            }

            // Keep only the registered columns
            Map<String, Object> selected = new LinkedHashMap<>();

            for (String column : required) {
                selected.put(column, record.get(column));
            }

            data.add(selected);
        }

        // Offline store: append every ingested record
        offlineStore.computeIfAbsent(
                name, k -> new ArrayList<>());

        offlineStore.get(name).addAll(data);

        // Online store: keep the last record for each entity
        onlineStore.computeIfAbsent(
                name, k -> new LinkedHashMap<>());

        Map<Object, Map<String, Object>> online =
                onlineStore.get(name);

        for (Map<String, Object> row : data) {
            Object key = row.get(entityKey);
            online.put(key, new LinkedHashMap<>(row));
        }

        System.out.println(
                "Ingested " + data.size()
                        + " rows into '" + name + "'.");
    }

    // Retrieve historical features for training
    public List<Map<String, Object>> getOfflineFeatures(
            String name, List<Object> entityIds) {

        if (!offlineStore.containsKey(name)) {
            throw new IllegalArgumentException(
                    "No offline data found for '" + name + "'.");
        }

        String entityKey = (String)
                repository.get(name).get("entity_key");

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : offlineStore.get(name)) {
            if (entityIds == null
                    || entityIds.contains(row.get(entityKey))) {
                result.add(new LinkedHashMap<>(row));
            }
        }

        return result;
    }

    // Retrieve the latest features for one entity
    public Map<String, Object> getOnlineFeatures(
            String name, Object entityId) {

        if (!onlineStore.containsKey(name)) {
            throw new IllegalArgumentException(
                    "No online data found for '" + name + "'.");
        }

        Map<String, Object> result =
                onlineStore.get(name).get(entityId);

        return result == null
                ? null
                : new LinkedHashMap<>(result);
    }

    // Display registered feature definitions
    public void showRepository() {
        System.out.println(repository);
    }

    // Helper method to create a customer record
    private static Map<String, Object> customer(
            int id, int orders, double average, int days) {

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("customer_id", id);
        row.put("total_orders", orders);
        row.put("avg_order_value", average);
        row.put("days_since_last_order", days);

        return row;
    }

    public static void main(String[] args) {

        // 1. Create the feature store
        FeatureStore store = new FeatureStore();

        // 2. Register customer features
        store.registerFeature(
                "customer_features",
                "customer_id",
                Arrays.asList(
                        "total_orders",
                        "avg_order_value",
                        "days_since_last_order"),
                "Purchase activity features for customer models");

        // 3. Prepare customer data
        List<Map<String, Object>> customerData =
                Arrays.asList(
                        customer(101, 5, 450.0, 4),
                        customer(102, 2, 220.0, 20),
                        customer(103, 9, 780.0, 2),
                        customer(101, 6, 470.0, 1));

        store.ingest("customer_features", customerData);

        // 4. Display registered definitions
        System.out.println("\nFEATURE REPOSITORY");
        store.showRepository();

        // 5. Retrieve offline features for training
        System.out.println("\nOFFLINE FEATURES FOR TRAINING");

        List<Map<String, Object>> trainingData =
                store.getOfflineFeatures(
                        "customer_features",
                        Arrays.<Object>asList(101, 102, 103));

        for (Map<String, Object> row : trainingData) {
            System.out.println(row);
        }

        // 6. Retrieve latest online features for customer 101
        System.out.println("\nONLINE FEATURES FOR CUSTOMER 101");

        Map<String, Object> onlineFeatures =
                store.getOnlineFeatures("customer_features", 101);

        System.out.println(onlineFeatures);

        // 7. Reuse features in two model workflows
        System.out.println("\nMODEL A (CHURN) INPUT: "
                + onlineFeatures);

        System.out.println("\nMODEL B (OFFER RECOMMENDATION) INPUT: "
                + onlineFeatures);
    }
}
