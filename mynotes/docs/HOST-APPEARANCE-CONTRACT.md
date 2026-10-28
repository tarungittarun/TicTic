# Standalone and future MySpace host contract

This is an adapter contract, not a claim that the absent MySpace integration has been certified.

- Standalone `NotesActivity` owns its System/Light/Dark preference, explicit Android native themes and window-bar contrast. Recreation retains the normal note snapshot, filter state and private drawing draft.
- An embedded `NotesHost` **manages appearance by default**. The standalone preference never overrides it. Implement `appearanceMode()` as `system`, `light` or `dark` and recreate the controller when the host changes mode.
- Supply a context using the host's corresponding native Android theme (dialogs, keyboard and selection surfaces remain native). Supply optional `palette()` tokens for background, surface, ink, secondary ink, outline and accents. Keep text/background contrast accessible; Notes does not correct an invalid host palette.
- Notes chrome, neutral cards, paper backgrounds and diagram canvas consume the host design tokens. Explicit user-selected note/shape colours remain note data, not globally overwritten by a host theme. PDF stays printable light paper.
- Implement OS capabilities through `NotesHost` (pickers, voice capture, share/export, reminders, widgets). Default unavailable-capability callbacks are safe messages, not silent success. Image ingestion copies metadata-stripped pixels; it does not depend on permanent source URI grants.
- Keep `getFilesDir` and namespaced `mynotes-*` preferences stable across standalone/suite migrations; switching package IDs or storage contexts is not an automatic data migration. Notes uses no calculator globals and this work does not modify calculator code.

Native contract coverage checks that an embedding host's light policy wins over a standalone dark preference and its supplied palette reaches the shell and native writing fields. This does not substitute for testing the actual future MySpace adapter, its theme context, migration or lifecycle.
