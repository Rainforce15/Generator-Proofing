# Generator Proofing


[![Version](https://img.shields.io/jetbrains/plugin/v/22079-generator-proofing)](https://plugins.jetbrains.com/plugin/22079-generator-proofing)


This is a simple IntelliJ Plugin preventing you from committing code in partially generated files. This is realised
as an Inspection Tool which scans the file for begin/end-tag comments and marks any VCS change outside the given tags as an
error.
