# Third-Party Cypher Grammar Assets

This repository vendors the following Cypher parser grammar files:

- `src/main/antlr4/org/neo4j/cypher/internal/parser/v5/Cypher5Lexer.g4`
- `src/main/antlr4/org/neo4j/cypher/internal/parser/v5/Cypher5Parser.g4`

Source:

- Neo4j Community Edition repository:
  - `/community/cypher/front-end/parser/v5/parser/src/main/antlr4/org/neo4j/cypher/internal/parser/v5/`

License:

- Apache License 2.0
- See original headers in the `.g4` files and https://www.apache.org/licenses/LICENSE-2.0

Notes:

- Only grammar assets are reused.
- Neo4j Scala front-end/runtime modules are not linked as dependencies.
