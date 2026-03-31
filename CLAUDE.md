# Claude Code Instructions for sailing-analytics

## Java Coding Style Preferences

### Variable Declarations
- **Always use `final` where possible** for local variables, parameters, and fields
- Prefer immutability

### Method Structure
- **No early returns** - avoid guard clauses with early return statements
- **No additional empty lines in the middle of methods** - keep method bodies compact without extra blank lines breaking up the logic
- Empty lines are acceptable between methods and between logical sections at the class level, but not within method implementations

### Example - Preferred Style
```java
public void processItem(final Item item) {
    final boolean isValid = item != null;
    if (isValid) {
        final String result = item.process();
        saveResult(result);
    }
}
```

### Example - Avoid
```java
public void processItem(Item item) {  // Missing final
    if (item == null) {
        return;  // Early return - avoid this
    }

    String result = item.process();  // Missing final, unnecessary blank line above

    saveResult(result);  // Unnecessary blank line above
}
```

## Git Commit Messages
- Follow existing repository convention: start with bug/issue number (e.g., "bug6214: description")
- Use descriptive commit messages explaining what changed and why
