"""Source boundaries only: real responsiveness and appearance require Android/phone checks."""
from pathlib import Path
import unittest

JAVA=Path(__file__).resolve().parents[1]/'runtime/src/main/java/app/space/notes/runtime'

class PhysicalFeedbackLibraryTest(unittest.TestCase):
    def test_private_folder_listing_never_queries_a_document_provider(self):
        s=(JAVA/'NotesRepository.java').read_text().split('public List<String> listPrivateFolders(',1)[1].split('public List<String> listFolders(',1)[0]
        self.assertNotIn('folders().list()',s)
        self.assertIn('n.folder',s)
        self.assertIn('root.listFiles()',s)
    def test_folder_and_voice_private_reads_do_not_wait_for_storage_writes(self):
        s=(JAVA/'NotesController.java').read_text()
        folder=s.split('private void renderFolders(boolean moving,Runnable parent)',1)[1].split('private void renderModelInfo()',1)[0]
        self.assertIn('folderReader.execute(',folder)
        self.assertIn('repository.listPrivateFolders(folderSnapshot)',folder)
        self.assertLess(folder.index('Your folders'),folder.index('repository.listFolders(folderSnapshot)'))
        self.assertIn('token!=folderPageSequence',folder)
        self.assertIn('pageBack=()->renderFolders(false,parent)',folder)
        inbox=s.split('private void refreshVoiceInbox()',1)[1].split('public void reviewVoiceTranscript',1)[0]
        self.assertIn('voiceReader.execute(',inbox)
        self.assertIn('Math.min(allIds.size(),voiceVisibleLimit)',inbox)
        self.assertIn('Show more recordings',inbox)
        self.assertIn('inboxToken!=voiceInboxSequence',inbox)
        self.assertIn('folderReader.shutdownNow();voiceReader.shutdownNow()',s)
    def test_playback_options_start_collapsed_without_removing_audio_actions(self):
        s=(JAVA/'VoicePlaybackCard.java').read_text()
        self.assertIn('details.setVisibility(GONE)',s)
        for action in ['Recording options','Export recording','Retry transcript','Open note','Boost quiet playback (+6 dB)']:
            self.assertIn(action,s)
        self.assertIn('details.addView(actions)',s)
        self.assertIn('details.addView(boostControl)',s)
