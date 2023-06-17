# Generator Proofing

This is a simple IntelliJ Plugin preventing you from committing code in partially generated files. This is realised
as an Inspection Tool which scans the file for begin/end-tag comments and marks any VCS change outside the given tags as an
error.
