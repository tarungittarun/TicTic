"""Conservative command accounting: never infer full coverage from screenshots."""
import re

def command_coverage(commands,features):
 result=[]
 for command in commands:
  token=re.compile(r'(?<![\w:-])'+re.escape(command['id'])+r'(?=[;\s]|$)')
  matches=[f for f in features if token.search(f.get('observed',''))]
  statuses={f['status'] for f in matches}
  status=('functional_readback_pass' if 'functional_pass' in statuses else
          'invoked_ui_inspected' if 'screen_inspected' in statuses else
          'blocked_or_unverified' if matches else 'inventoried_only')
  result.append(dict(command,status=status,evidence_features=[f['feature'] for f in matches]))
 return result

def command_brief(command_id,name):
 """Describe intended feature separately from what this harness actually proved."""
 if command_id.startswith('editor:toggle-highlight'):
  color=command_id.removeprefix('editor:toggle-highlight').lstrip('-') or 'default'
  return f'Apply or remove {color} text highlighting in a Markdown note. The readback check does not by itself certify the rendered color.'
 if command_id.startswith('editor:set-heading-'):
  return 'Set the selected paragraph to the named Markdown heading level, changing its place in the note outline.'
 if command_id.startswith('editor:table-'):
  return name+'. This table-editor action changes rows, columns or alignment in the active Markdown table.'
 groups={
  'editor':'An editor action for the current note, selection, caret or formatting. Some actions require a particular editing context.',
  'workspace':'A file/tab/workspace action that changes how notes are opened, arranged, renamed, shared or exported.',
  'file-explorer':'An action in the vault file browser for creating, locating or organizing local files and folders.',
  'global-search':'Search across the vault rather than just the currently open note.',
  'switcher':'Quickly locate and open a note by its name.',
  'graph':'Visualize relationships created by links between notes; the local graph focuses on a single note.',
  'backlink':'Find other notes that refer to the current note, including linked and unlinked mentions where offered.',
  'outgoing-links':'Inspect links going from the current note to other notes.',
  'canvas':'Arrange text cards, files and connecting edges on a spatial canvas.',
  'bases':'Create and manipulate structured views of vault files using note properties; it is not a separate cloud database.',
  'properties':'Inspect structured note metadata, either for one note or across the vault.',
  'tag-pane':'Browse tags used in notes and navigate matching content.',
  'footnotes':'Inspect footnotes associated with the current note.',
  'daily-notes':'Create or navigate date-based daily notes using the configured format and folder.',
  'templates':'Insert reusable note content or date/time values; template insertion needs a configured template folder.',
  'note-composer':'Split selected content into another note or merge note content; these are editing operations, not backups.',
  'command-palette':'Find and invoke named application commands from a searchable list.',
  'bookmarks':'Save shortcuts to files, headings, searches or views for later navigation.',
  'outline':'Navigate the heading structure of the current note.',
  'file-recovery':'Inspect local snapshots of note edits; this differs from cloud version history and the file trash.',
  'sync':'An Obsidian Sync account/service action. The cloud service is not end-to-end verified in this report.',
  'publish':'An Obsidian Publish account/service action. Website publication is not end-to-end verified here.',
  'audio-recorder':'Record microphone input into a vault attachment. Entry/permission screenshots are not recording-quality proof.',
  'workspaces':'Save or restore the arrangement of open views; a layout is not a note-content backup.',
  'random-note':'Open a randomly selected existing note.',
  'zk-prefixer':'Create a note with a unique, typically time-based name.',
  'slides':'Present suitably separated Markdown sections as slides.',
  'theme':'Change the application appearance or switch its light/dark scheme.',
  'mobile':'Configure a mobile-specific toolbar or quick-action entry.',
  'app':'An application-level navigation, settings or configuration action.'}
 return name+'. '+groups.get(command_id.split(':')[0], 'A built-in action exposed in this inspected mobile configuration; use the coverage status to distinguish intent from verified behavior.')

def normalize_requested_variants(features):
 """Do not label a portrait capture as successful landscape evidence."""
 result=[dict(row) for row in features]
 blocked={row['feature'].removeprefix('Appearance variant: '):row.get('observed','') for row in result if row['feature'].startswith('Appearance variant: ') and row['status']=='blocked'}
 for row in result:
  if row['feature'].startswith('Canvas variant: '):
   request=row['feature'].removeprefix('Canvas variant: ')
   if request in blocked:
    row['feature']='Canvas: requested '+request+' (not achieved)'
    row['status']='blocked'
    row['observed']='Requested orientation/theme was not achieved: '+blocked[request]+'. Actual screenshot retained; this is not landscape proof.'
 return result
