from extraction.driver import Driver

from neo4j import Session, Record


class Database():
    # A class to use the driver abstraction and execute the queries needed to the extraction
    # The explanation for each query can be found in the Query file

    def __init__(self, driver: Driver):
        self._driver = driver

    def get_labels(self) -> list[str]:
        return self._driver.read_transaction(Database._get_labels)

    @staticmethod
    def _get_labels(tx: Session) -> list[str]:
        db_result = tx.run("call db.labels")
        return [record['label'] for record in db_result]

    def get_relationship_types(self) -> list[str]:
        return self._driver.read_transaction(Database._get_relationship_types)

    @staticmethod
    def _get_relationship_types(tx: Session) -> list[str]:
        db_result = tx.run("call db.relationshipTypes")
        return [record['relationshipType'] for record in db_result]

    def get_nodes_by_labels(self, labels: list[str]) -> list[Record]:
        query = "match p = (node)-->()-->() where "
        for _, value in enumerate(labels):
            query = query + f"'{value}' in labels(node) and "
        query = query + \
            f"size(labels(node))={len(labels)} return node"

        return self._driver.read_transaction(Database._get_nodes_by_label, query)

    @staticmethod
    def _get_nodes_by_label(tx: Session, query: str) -> list[Record]:
        return [record for record in tx.run(query)]

    def get_relationships_types_by_id(self, node_id: str):
        params = {'id': node_id}
        return self._driver.read_transaction(Database._get_relationships_types_by_id, params)

    @staticmethod
    def _get_relationships_types_by_id(tx: Session, params: dict[str, str]) -> list[Record]:
        db_result = tx.run(
            "match (node)-[relationship]->(end_node) where id(node)=$id return type(relationship) as relationship, labels(end_node) as labels", params)
        return [record for record in db_result]

    def get_relationships_by_type(self, typed):
        params = {'type': typed}
        return self._driver.read_transaction(Database._get_relationships_by_type, params)

    @staticmethod
    def _get_relationships_by_type(tx: Session, params: dict[str, str]) -> list[Record]:
        db_result = tx.run(
            "match ()-[relationship]-() where type(relationship)=$type return relationship", params)
        return [record for record in db_result]
