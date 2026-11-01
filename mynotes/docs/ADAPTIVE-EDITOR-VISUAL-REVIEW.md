# Adaptive editor visual review — 2026-11-01

## Scope and provenance

Agent visual review of **144 synthetic native captures** (36 scenes × four profiles),
from run **41288909525**, source **abfa0ba8**. Reviewed as twelve temporary contact
sheets, with individual rail/Format captures also inspected. The contact sheets are
local review aids, not new native evidence. Originals and source-pinned manifests:
`design-2026-10-29/native/41288909525/`. This failed candidate is **not** represented
as qualified. Candidate4 changes test preparation and CI/docs, not production UI;
its independent native qualification remains required.

Profiles: minimum API26 light portrait; current API36 light portrait with motion;
API34 dark portrait at 200% font; API34 light landscape. Dark 200% **combined with
landscape** was not a fifth tested profile. Screenshots are downscaled JPEG review
previews, not evidence of the maximum resolution of user attachment zoom.

## Scenes examined per profile

- Controls: adaptive Notes rails, adaptive Doc rails, Notes Format/numbering,
  Format, Format with keyboard, Insert, Hyperlink form, checklist options, content
  pen, editable table, table options, Find with keyboard.
- Navigation/surfaces: home, Library, Notes workspace, Doc workspace, editor,
  editor with keyboard, backgrounds, labels, settings, storage/recovery,
  backups, diagnostics.
- Content/dialogs: drawing, drawing tools/options, drawing attached to an editor
  (`image-editor` fixture), diagram/options, shape picker, reminder date,
  voice inbox, OCR review, Trash confirmation, About.

## Observations

- Light surfaces separate the writing paper from app chrome; dark surfaces retain
  a distinct paper and panel. Modal surfaces dim their parent and have clear
  headings/close or cancel controls. Format is an attached secondary panel rather
  than an unrelated screen. Parent writing/selection remains visible where space
  permits.
- Notes and Doc editor rails free the landscape writing width; navigation/history
  are on the left and content/format tools on the right. Portrait actions remain
  below the writing surface and above the keyboard. Notes More is top-right in
  portrait, not a duplicate bottom command.
- Short landscape keyboard viewports require vertical rail scrolling; not every
  tool can be simultaneously visible. Large-font grids, Format, Library, settings
  and option sheets also use scrolling rather than shrinking text/touch targets.
  Horizontal drawing-tool strips intentionally scroll. These are not claims that
  every command fits on one screen.
- Format has clear paragraph, font, size/stepper and numbering group boundaries.
  Large-font capture shows numbered selected lines and legible controls. Some
  captures show a scrolled portion, not missing controls; native tests check the
  applicable actions separately. Clear is inline with the five style controls.
- Insert groups visual content, text capture, and lists/reminders. Hyperlink is
  named consistently and has display text, address and explicit Save. The Hyperlink
  fixture rejects a file URL, then edits the address without saving: the previous
  validation indicator is still visible. This is not a completed-save state.
  Doc uses the same Insert component, with document-specific commands;
  the `capture` screenshot is the Notes instance, not a Doc screenshot.
- Table cells are distinct editable areas without text-field underlines; pen and
  scoped three-dot menus are distinguishable from content/item grips. Native
  interaction tests, not these static images, establish edit/copy/cut/drag behavior.
- Drawing and diagram retain their specialized canvas toolbars; the new dual
  rails apply to Notes/Doc writing editors. Their option surfaces and shape picker
  remain bounded/scrollable in landscape and large text. Voice, OCR, reminder,
  recovery and destructive-confirmation surfaces have clear separate scopes.
- Diagnostics is opt-in. Backup and About copy explicitly disclose original-audio
  and other backup omissions. Permanent Trash deletion has a warning and explicit
  destructive action; it is not a one-tap accidental purge.

## Limits / remaining acceptance

No new visual overlap or unreadable-text blocker was identified in the reviewed
captures. This is an agent screenshot review, **not physical-phone, OEM, novice or
all-combinations usability acceptance**. Native tests establish only their exact
inventories/profiles and source. Actual attachment fidelity also depends on stored
pixels: the full stored-resolution zoom avoids using the small inline thumbnail,
but cannot restore detail discarded during earlier import. Historical private
uploads were reviewed locally and are not included in synthetic public fixtures.

## Final candidate follow-up

Run **41291286599**, source **0fbf43f6fe41671e987024c01abfc1b6d5232347**,
evidence commit **13560101** completed SUCCESS. Inspected another **48 final-source
control captures** (the twelve Controls scenes above × four profiles) as four local
contact sheets. Rails, Format/numbering, Insert, Hyperlink, pen, table and Find remained
consistent with the broader review; no new visual blocker identified. Production
runtime/domain/resources are unchanged from the candidate3 review source (verified by
Git diff); candidate4/5 corrections were test preparation and CI/docs only.
All896 native outcomes passed with no missing profiles, and336 native JPEGs are preserved
in `design-2026-10-29/native/41291286599/`. The physical/OEM/novice and combined-profile
limits above still apply. No public release approval is implied.
