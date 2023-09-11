from neo4j import GraphDatabase


class Driver():
    # Driver abstraction used to access the Neo4j database
    # With read and write transactions that can be parametized

    def __init__(self, uri, user, password):
        self._driver = GraphDatabase.driver(uri, auth=(user, password))

    def close(self):
        self._driver.close()

    def read_transaction(self, transaction_function, parameters=None):
        with self._driver.session() as session:
            if parameters:
                return session.execute_read(transaction_function, parameters)

            return session.execute_read(transaction_function)
