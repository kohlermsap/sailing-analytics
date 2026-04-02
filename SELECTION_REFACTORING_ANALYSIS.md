# LeaderboardPanel Selection State Refactoring Analysis

## Current Architecture

### Three Selection Models in Play

1. **`leaderboardSelectionModel`** (LeaderboardPanel:239)
   - Type: `MultiSelectionModel<LeaderboardRowDTO>`
   - Created at line 527
   - Attached to the CellTable at line 547
   - Has a `selectionChangeHandler` that syncs TO CompetitorSelectionProvider (lines 530-544)

2. **`SelectionCheckboxColumn.selectionModel`** (SelectionCheckboxColumn:51)
   - Type: `RefreshableMultiSelectionModel<T>`
   - Created by SelectionCheckboxColumn constructor
   - **Currently NOT used by the LeaderboardPanel's CellTable**
   - Has display.flush() callbacks for UI updates

3. **`CompetitorSelectionProvider`**
   - Application-level selection state
   - Both LeaderboardPanel and SelectionCheckboxColumn listen to it
   - Notifies listeners via `addedToSelection()` / `removedFromSelection()`

### Current Selection Flow Problems

#### Flow 1: User clicks checkbox in UI
```
User clicks checkbox
  ↓
SelectionCheckboxColumn handles click event
  ↓
leaderboardSelectionModel.setSelected() called  ← Uses WRONG selection model!
  ↓
selectionChangeHandler fires (lines 530-544)
  ↓
competitorSelectionProvider.setSelection() called
  ↓
CompetitorSelectionProvider fires addedToSelection/removedFromSelection
  ↓
BOTH LeaderboardPanel AND SelectionCheckboxColumn receive callback:
  - LeaderboardPanel.addedToSelection() (line 3393)
      → calls leaderboardSelectionModel.setSelected()
  - SelectionCheckboxColumn.addedToSelection() (line 2160)
      → calls getSelectionModel().setSelected()  ← Different model!
```

**Problem**: Two selection models exist but only one (leaderboardSelectionModel) is attached to the table. The SelectionCheckboxColumn's RefreshableMultiSelectionModel is created but orphaned.

#### Flow 2: CompetitorSelectionProvider changes externally
```
External change to CompetitorSelectionProvider
  ↓
Fires addedToSelection/removedFromSelection
  ↓
BOTH listeners receive callback:
  - LeaderboardPanel.addedToSelection() (line 3393)
      → leaderboardSelectionModel.setSelected(row, true)
      → Could trigger selectionChangeHandler → infinite loop potential!
  - SelectionCheckboxColumn.addedToSelection() (line 2160)
      → getSelectionModel().setSelected(row, true) ← Orphaned model!
```

**Problem**: LeaderboardPanel guards against recursion by temporarily removing the handler (line 2712), but this is complex. SelectionCheckboxColumn updates an unused selection model.

### Inconsistencies Found

1. **Line 527**: LeaderboardPanel creates its own `MultiSelectionModel`
2. **Line 547**: Attaches that model to the table, ignoring SelectionCheckboxColumn's model
3. **Line 2163/2174**: SelectionCheckboxColumn callbacks update its own orphaned model
4. **Line 3396/3404**: LeaderboardPanel callbacks update the table's model
5. **SelectionCheckboxColumn.getValue()** (line 2140): Checks CompetitorSelectionProvider directly, NOT its own selection model!

## Proposed Refactoring

### Goal
Use **ONLY** the `RefreshableMultiSelectionModel` from SelectionCheckboxColumn as the single source of truth, synchronized bidirectionally with CompetitorSelectionProvider.

### Step-by-Step Plan

#### Step 1: Use SelectionCheckboxColumn's Model as Table's Model

**File**: `LeaderboardPanel.java`

**Current code** (lines 525-547):
```java
selectionCheckboxColumn = new LeaderboardSelectionCheckboxColumn(competitorSelectionProvider);
leaderboardTable.setWidth("100%");
leaderboardSelectionModel = new MultiSelectionModel<LeaderboardRowDTO>();  // ← REMOVE THIS
selectionChangeHandler = new Handler() {
    @Override
    public void onSelectionChange(SelectionChangeEvent event) {
        List<CompetitorDTO> selection = new ArrayList<>();
        for (LeaderboardRowDTO row : getSelectedRows()) {
            selection.add(row.competitor);
        }
        LeaderboardPanel.this.competitorSelectionProvider.setSelection(selection,
                /* listenersNotToNotify */LeaderboardPanel.this);
        if (blurInOnSelectionChanged > 0) {
            blurInOnSelectionChanged--;
            blurFocusedElementAfterSelectionChange();
        }
    }
};
leaderboardAsTableSelectionModelRegistration = leaderboardSelectionModel
        .addSelectionChangeHandler(selectionChangeHandler);
leaderboardTable.setSelectionModel(leaderboardSelectionModel, selectionCheckboxColumn.getSelectionManager());
```

**Refactored**:
```java
selectionCheckboxColumn = new LeaderboardSelectionCheckboxColumn(competitorSelectionProvider);
leaderboardTable.setWidth("100%");

// Use the SelectionCheckboxColumn's model as THE selection model
leaderboardSelectionModel = selectionCheckboxColumn.getSelectionModel();

selectionChangeHandler = new Handler() {
    @Override
    public void onSelectionChange(SelectionChangeEvent event) {
        if (updatingSelectionFromProvider) {
            // Guard: don't sync back to provider if we're currently syncing FROM provider
            return;
        }
        List<CompetitorDTO> selection = new ArrayList<>();
        for (LeaderboardRowDTO row : getSelectedRows()) {
            selection.add(row.competitor);
        }
        LeaderboardPanel.this.competitorSelectionProvider.setSelection(selection,
                /* listenersNotToNotify */LeaderboardPanel.this);
        if (blurInOnSelectionChanged > 0) {
            blurInOnSelectionChanged--;
            blurFocusedElementAfterSelectionChange();
        }
    }
};
leaderboardAsTableSelectionModelRegistration = leaderboardSelectionModel
        .addSelectionChangeHandler(selectionChangeHandler);
leaderboardTable.setSelectionModel(leaderboardSelectionModel, selectionCheckboxColumn.getSelectionManager());
```

**Changes**:
- Line 527: Change from `new MultiSelectionModel<>()` to `selectionCheckboxColumn.getSelectionModel()`
- Add guard flag `updatingSelectionFromProvider` to prevent recursion
- Declare field: `private boolean updatingSelectionFromProvider = false;`

#### Step 2: Update Field Declaration

**Current** (line 239):
```java
private final MultiSelectionModel<LeaderboardRowDTO> leaderboardSelectionModel;
```

**Refactored**:
```java
private final RefreshableMultiSelectionModel<LeaderboardRowDTO> leaderboardSelectionModel;
```

Add import:
```java
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
```

#### Step 3: Fix LeaderboardPanel's Selection Callbacks

**Current** (lines 3393-3406):
```java
@Override
public void addedToSelection(CompetitorDTO competitor) {
    LeaderboardRowDTO row = getRow(competitor.getIdAsString());
    if (row != null) {
        leaderboardSelectionModel.setSelected(row, true);
    }
}

@Override
public void removedFromSelection(CompetitorDTO competitor) {
    LeaderboardRowDTO row = getRow(competitor.getIdAsString());
    if (row != null) {
        leaderboardSelectionModel.setSelected(row, false);
    }
}
```

**Refactored**:
```java
@Override
public void addedToSelection(CompetitorDTO competitor) {
    LeaderboardRowDTO row = getRow(competitor.getIdAsString());
    if (row != null) {
        updatingSelectionFromProvider = true;
        try {
            leaderboardSelectionModel.setSelected(row, true);
        } finally {
            updatingSelectionFromProvider = false;
        }
    }
}

@Override
public void removedFromSelection(CompetitorDTO competitor) {
    LeaderboardRowDTO row = getRow(competitor.getIdAsString());
    if (row != null) {
        updatingSelectionFromProvider = true;
        try {
            leaderboardSelectionModel.setSelected(row, false);
        } finally {
            updatingSelectionFromProvider = false;
        }
    }
}
```

**Changes**:
- Set guard flag before updating selection model
- Ensures selectionChangeHandler won't sync back to CompetitorSelectionProvider

#### Step 4: Remove SelectionCheckboxColumn's Redundant Listener

**File**: `LeaderboardPanel.java`, inner class `LeaderboardSelectionCheckboxColumn`

**Current** (lines 2120-2136):
```java
protected LeaderboardSelectionCheckboxColumn(final CompetitorSelectionProvider competitorSelectionProvider) {
    super(style.getTableresources().cellTableStyle().cellTableCheckboxSelected(),
            style.getTableresources().cellTableStyle().cellTableCheckboxDeselected(),
            style.getTableresources().cellTableStyle().cellTableCheckboxColumnCell(),
            new EntityIdentityComparator<LeaderboardRowDTO>() {
                @Override
                public boolean representSameEntity(LeaderboardRowDTO dto1, LeaderboardRowDTO dto2) {
                    return dto1.competitor.getIdAsString().equals(dto2.competitor.getIdAsString());
                }

                @Override
                public int hashCode(LeaderboardRowDTO t) {
                    return t.competitor.getIdAsString().hashCode();
                }
            }, getData(), leaderboardTable);
    competitorSelectionProvider.addCompetitorSelectionChangeListener(this);  // ← REMOVE THIS
}
```

**Refactored**:
```java
protected LeaderboardSelectionCheckboxColumn(final CompetitorSelectionProvider competitorSelectionProvider) {
    super(style.getTableresources().cellTableStyle().cellTableCheckboxSelected(),
            style.getTableresources().cellTableStyle().cellTableCheckboxDeselected(),
            style.getTableresources().cellTableStyle().cellTableCheckboxColumnCell(),
            new EntityIdentityComparator<LeaderboardRowDTO>() {
                @Override
                public boolean representSameEntity(LeaderboardRowDTO dto1, LeaderboardRowDTO dto2) {
                    return dto1.competitor.getIdAsString().equals(dto2.competitor.getIdAsString());
                }

                @Override
                public int hashCode(LeaderboardRowDTO t) {
                    return t.competitor.getIdAsString().hashCode();
                }
            }, getData(), leaderboardTable);
    // REMOVED: competitorSelectionProvider.addCompetitorSelectionChangeListener(this);
    // LeaderboardPanel is now the ONLY listener that syncs selection state
}
```

**Current** (lines 2160-2176, now unnecessary):
```java
@Override
public void addedToSelection(CompetitorDTO competitor) {
    final LeaderboardRowDTO row = getRow(competitor.getIdAsString());
    if (row != null) {
        getSelectionModel().setSelected(row, true);
    }
}

@Override
public void removedFromSelection(CompetitorDTO competitor) {
    final LeaderboardRowDTO row = getRow(competitor.getIdAsString());
    if (row != null) {
        getSelectionModel().setSelected(row, false);
    }
}
```

**Refactored**: Remove these methods entirely. The SelectionCheckboxColumn no longer needs to implement `CompetitorSelectionChangeListener`.

**Also remove** (line 2119):
```java
// FROM:
private class LeaderboardSelectionCheckboxColumn
        extends com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn<LeaderboardRowDTO>
        implements CompetitorSelectionChangeListener {

// TO:
private class LeaderboardSelectionCheckboxColumn
        extends com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn<LeaderboardRowDTO> {
```

Remove the empty listener method implementations at lines 2144-2154.

#### Step 5: Simplify updateSelection()

**Current** (lines 2707-2721):
```java
private void updateSelection(LeaderboardRowDTO row) {
    final boolean shallBeSelected = competitorSelectionProvider.isSelected(row.competitor);
    if (leaderboardAsTableSelectionModelRegistration != null) {
        // suspend selection events while actively adjusting the leaderboardSelectionModel to match the
        // competitorSelectionProvider
        leaderboardAsTableSelectionModelRegistration.removeHandler();
        leaderboardAsTableSelectionModelRegistration = null;
    }
    if (leaderboardSelectionModel.isSelected(row) != shallBeSelected) {
        leaderboardSelectionModel.setSelected(row, shallBeSelected);
    }
    // register the selection change handler again
    leaderboardAsTableSelectionModelRegistration = leaderboardTable.getSelectionModel()
            .addSelectionChangeHandler(selectionChangeHandler);
}
```

**Refactored**:
```java
private void updateSelection(LeaderboardRowDTO row) {
    final boolean shallBeSelected = competitorSelectionProvider.isSelected(row.competitor);
    updatingSelectionFromProvider = true;
    try {
        if (leaderboardSelectionModel.isSelected(row) != shallBeSelected) {
            leaderboardSelectionModel.setSelected(row, shallBeSelected);
        }
    } finally {
        updatingSelectionFromProvider = false;
    }
}
```

**Changes**:
- Removed handler removal/re-registration complexity
- Use guard flag instead
- Much simpler and clearer

#### Step 6: Fix getValue() in LeaderboardSelectionCheckboxColumn

**Current** (lines 2139-2141):
```java
@Override
public Boolean getValue(LeaderboardRowDTO row) {
    return competitorSelectionProvider.isSelected(row.competitor);
}
```

**Refactored**:
```java
@Override
public Boolean getValue(LeaderboardRowDTO row) {
    // Use the selection model as the source of truth, not CompetitorSelectionProvider
    return getSelectionModel().isSelected(row);
}
```

**Rationale**: The selection model should be the single source of truth for rendering. It stays synchronized with CompetitorSelectionProvider via the LeaderboardPanel's listeners.

### Summary of Changes

1. **LeaderboardPanel.java line 527**: Change to use `selectionCheckboxColumn.getSelectionModel()`
2. **LeaderboardPanel.java line 239**: Change type to `RefreshableMultiSelectionModel`
3. **LeaderboardPanel.java**: Add field `private boolean updatingSelectionFromProvider = false;`
4. **LeaderboardPanel.java lines 530-544**: Add guard check in selectionChangeHandler
5. **LeaderboardPanel.java lines 3393-3406**: Add guard flag around setSelected calls
6. **LeaderboardPanel.java lines 2707-2721**: Simplify updateSelection() using guard flag
7. **LeaderboardPanel.LeaderboardSelectionCheckboxColumn line 2135**: Remove listener registration
8. **LeaderboardPanel.LeaderboardSelectionCheckboxColumn line 2119**: Remove `implements CompetitorSelectionChangeListener`
9. **LeaderboardPanel.LeaderboardSelectionCheckboxColumn lines 2144-2176**: Remove listener methods
10. **LeaderboardPanel.LeaderboardSelectionCheckboxColumn line 2140**: Use selection model instead of provider

## Benefits

1. **Single Selection Model**: RefreshableMultiSelectionModel is the only selection model
2. **No Orphaned State**: Eliminates the unused selection model in SelectionCheckboxColumn
3. **Simpler Synchronization**: Single bidirectional sync point between model and provider
4. **No Handler Removal**: Guard flag is simpler than removing/re-adding handlers
5. **Clearer Ownership**: LeaderboardPanel owns synchronization, SelectionCheckboxColumn just renders

## Testing Strategy

1. Test user clicking checkboxes - selection should sync to CompetitorSelectionProvider
2. Test external CompetitorSelectionProvider changes - should update checkboxes
3. Test rapid clicking - no infinite loops or race conditions
4. Test with filtering - filtered rows should maintain correct selection state
5. Test leaderboard updates - selection should persist across data refreshes

## Potential Risks

1. **GWT Selection Model Flush Timing**: GWT selection models flush changes at end of event loop. The guard flag approach should handle this correctly since the flag is set synchronously.

2. **Multiple LeaderboardPanels**: If multiple LeaderboardPanels share the same CompetitorSelectionProvider, each will receive callbacks. This should work fine as each has its own guard flag.

3. **RefreshableMultiSelectionModel.refreshSelectionModel()**: This method has its own `dontCheckSelectionState` guard (RefreshableMultiSelectionModel.java line 131). Verify this doesn't interfere with our guard flag.

## Future Improvements

Consider enhancing RefreshableMultiSelectionModel to accept a callback for selection changes, eliminating the need for the external SelectionChangeHandler and making the synchronization more encapsulated within the model itself.
