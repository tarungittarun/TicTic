# Round20 synthetic capture inspection

- API36 portrait grid: correct fixture visible, column headings,3 rows, Unicode
  values and scrollable table. Large-font/dark grid: correct fixture visible with
  readable themed ink; long names are ellipsized and data row remains available.
- Landscape `spreadsheet-grid-viewport.jpg` still shows Home. It is NOT visual
  grid approval despite attached/visible fixture assertions. Startup close fixed
  some captures, not every foreground/compositor race. Follow-up must also check
  window focus and the current UI hierarchy before capture. Keep this bad capture
  as evidence; do not present the capture issue as completely fixed.
- Image-selection large-font portrait and landscape captures show the synthetic
  image, selected25, count1/4 and appropriate horizontal/side controls. Portrait
  large-font action/tools rows scroll horizontally; landscape primary actions are
  above scrollable secondary controls. The same-dialog two-way rotation test
  independently checks view identity, words, normalized crop and action visibility.

Synthetic boxes include a fabricated emoji label to exercise Unicode preservation,
NOT an emoji-recognition accuracy result. No screenshots are from a user phone.
