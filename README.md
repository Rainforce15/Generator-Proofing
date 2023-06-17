# Generator Proofing

This is a simple IntelliJ Plugin that prevents you from committing code in partially generated files. This is realised
as an Inspection Tool which scans the file for begin/end-tag comments and marks any change outside the given tags as an
error.

It was originally written for a very specific internal use case, and is not configurable so far (planned milestone).