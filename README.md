# <img src="./src/main/resources/META-INF/pluginIcon.svg" alt="logo"> Generator Proofing


[![Version](https://img.shields.io/jetbrains/plugin/v/22079-generator-proofing)](https://plugins.jetbrains.com/plugin/22079-generator-proofing)


This is a simple IntelliJ Java Plugin preventing you from committing code in partially generated files, realised
as an Inspection Tool which scans the file for begin/end-tag comments and marks any VCS change outside the given tags as an
error.

## Usage

### Basic configuration

The plugin is intended for generated code that is marked with a header comment and contains implementation begin and end tags:

```java
// Generated file

public class MyClass {
    public int add(int a, int b) {
        var result = 0;
        // IMPLEMENTATION BEGIN
        result = a + b; // only content between the tags can be edited
        // IMPLEMENTATION END
        return result; // changing this line would cause an error
    }
}
```

The specific trigger strings have to be configured in the inspection settings under `Settings... > Editor > Inspections > Java > Probable bugs > Change in generated code`.
E.g. for above file to work the fields would be configured as:

Header pattern: `// Generated file`  
Begin pattern: `// IMPLEMENTATION BEGIN`  
End pattern: `// IMPLEMENTATION END`

### Additional features

The plugin can be told to ignore the currently marked file by using the Quick Fix feature (either by pressing `Alt+Enter` or clicking the light bulb icon) and selecting `Ignore file until restart/changes have been rolled back...`.
This will stop the inspection from marking any current changes to the file as errors until the IDE is restarted or all changes have been reverted.
