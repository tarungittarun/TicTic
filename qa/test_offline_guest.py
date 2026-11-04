import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from mynotes.tools.ci import offline_guest as guest

class OfflineGuest(unittest.TestCase):
 def test_disabled_wifi_is_not_enough_if_default_network_exists(self):
  self.assertFalse(guest.network_off('Wi-Fi is disabled','Active default network: 100'))
  self.assertFalse(guest.network_off('Wi-Fi is enabled','Active default network: none'))
  self.assertTrue(guest.network_off('Wi-Fi is disabled','Active default network: none'))
 def test_physical_device_never_rooted(self):
  with patch.object(guest,'adb',return_value='0') as adb:
   with self.assertRaisesRegex(RuntimeError,'emulator'):guest.configure()
   self.assertEqual(1,adb.call_count)
 def test_installed_notes_blocks_bootstrap(self):
  with patch.object(guest,'adb',side_effect=['1','package:/data/app/notes.apk']) as adb:
   with self.assertRaisesRegex(RuntimeError,'populated'):guest.configure()
   self.assertFalse(any(c.args==('root',) for c in adb.call_args_list))
 def response(self,*args):
  if args==('shell','getprop','ro.kernel.qemu'):return '1'
  if args[:3]==('shell','pm','path'):return ''
  if args[:3]==('shell','settings','get'):return '1'
  if args==('shell','dumpsys','wifi'):return 'Wi-Fi is disabled'
  if args==('shell','dumpsys','connectivity'):return 'Active default network: none'
  return ''
 def test_three_actual_state_checks_and_unroot_before_proof(self):
  with tempfile.TemporaryDirectory() as temp,patch.object(guest,'adb',side_effect=self.response) as adb,patch.object(guest,'wait_uid') as uid,patch.object(guest.time,'sleep'):
   proof=guest.configure(Path(temp)/'proof.json')
   self.assertEqual(3,proof['consecutive_checks']);self.assertEqual(2000,proof['final_shell_uid'])
   self.assertEqual([0,2000],[c.args[0] for c in uid.call_args_list])
   self.assertNotIn('svc',[str(c.args) for c in adb.call_args_list])
 def test_failed_broadcast_never_creates_success_proof_and_still_unroots(self):
  def fail(*args):
   if 'broadcast' in args:raise RuntimeError('broadcast failed')
   return self.response(*args)
  with tempfile.TemporaryDirectory() as temp,patch.object(guest,'adb',side_effect=fail) as adb,patch.object(guest,'wait_uid'):
   path=Path(temp)/'proof.json'
   with self.assertRaisesRegex(RuntimeError,'broadcast failed'):guest.configure(path)
   self.assertFalse(path.exists());self.assertEqual(('unroot',),adb.call_args.args)
