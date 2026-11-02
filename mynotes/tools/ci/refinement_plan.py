"""Method-balanced exhaustive qualification shards, including the large journey class."""
from mynotes.tools.ci.test_plan import cases
FOCUSED = ['app.space.notes.runtime.PdfPageOcrTest','app.space.notes.runtime.ViewedFileJourneysTest','app.space.notes.runtime.WhisperConnectionJourneysTest','app.space.notes.runtime.RepositoryConcurrencyJourneysTest','app.space.notes.runtime.ImageSelectionTest','app.space.notes.runtime.NotesMotionTest','app.space.notes.runtime.DrawingToolsTest','app.space.notes.runtime.DiagramResizeViewTest','app.space.notes.DiagramJourneysTest','app.space.notes.CoreJourneysTest','app.space.notes.EnvironmentQualificationTest','app.space.notes.PdfExportTest','app.space.notes.StylePresetStoreTest','app.space.notes.HistoryRecoveryTest','app.space.notes.RealFolderStorageTest','app.space.notes.StructuredCodecTest']

def selected_cases(plan, group, shard=-1):
    if group not in {'all','focused'} or shard not in {-1,0,1}:
        raise ValueError('Unknown qualification selection')
    selected=cases(plan['android'] if group=='all' else {k:plan['android'][k] for k in FOCUSED})
    if shard == -1:
        return selected
    # Keep the four independently verified PDF fixtures together, with no duplicate execution.
    pdf=[case for case in selected if case.startswith('app.space.notes.PdfExportTest#')]
    rest=[case for case in selected if case not in pdf]
    return sorted(rest[shard::2]+(pdf if shard==0 else []))
