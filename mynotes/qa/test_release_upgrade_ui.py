import unittest
from mynotes.tools.ci.ui import coordinates
class UpgradeUi(unittest.TestCase):
    def xml(self,nodes):return '<hierarchy>'+nodes+'</hierarchy>'
    def node(self,text='Title',bounds='[10,20][110,80]',enabled='true'):return f'<node text="{text}" bounds="{bounds}" enabled="{enabled}" />'
    def test_exact_enabled_unique_target_coordinates(self):self.assertEqual((60,50),coordinates(self.xml(self.node()+self.node('other')),'Title'))
    def test_ambiguous_missing_disabled_and_empty_targets_rejected(self):
        for nodes in ['',self.node()*2,self.node(enabled='false'),self.node(bounds='[0,0][0,0]'),self.node(bounds='bad')]:
            with self.assertRaises(ValueError):coordinates(self.xml(nodes),'Title')
