# MLOps Practical 5 — Feature Stores

## Aim
To implement a simple feature store that supports feature registration, data ingestion, offline feature storage, online feature retrieval, and feature reuse in machine learning workflows.

## Technologies Used
- Python
- Pandas
- Git and GitHub

## Features
1. Register feature definitions in a repository.
2. Validate incoming customer data.
3. Store historical features in an offline store.
4. Maintain the latest features in an online store.
5. Retrieve features for model training and inference.
6. Reuse customer features for churn prediction and offer recommendation workflows.

## How to Run

1. Install Python.
2. Install the dependencies:

   ```bash
   pip install -r requirements.txt
   ```

3. Run the program:

   ```bash
   python feature_store.py
   ```

## Expected Output
The program displays the registered feature repository, historical customer features, the latest features for customer 101, and the inputs used by two example model workflows.

## Conclusion
This practical demonstrates the basic working of a feature store for organizing, storing, retrieving, and reusing machine learning features.
