# Executed small-font and varied public-scene study

Run41830346993; sourcecba7533f. This recovers already completed inference, not a rerun. The workflow failed because a copied43-case interaction verifier was applied after the two study methods; the study itself verifies two methods and149 measurements. No production OCR change, training, physical phone acceptance or final APK qualification.

72 authored font/script/size/polarity inputs, each measured twice; not144 independent scenes. Five public inputs from three original iOS screenshots: two complete photo/dialog screenshots, one selected-row crop, two no-text nature-photo crops. Not positive street-sign OCR or untouched heldout data.

Timings are emulator wall-clock inference, counterbalanced order in font pairs; first call includes cold provisioning. Decode/synthesis/upscale/scoring excluded. CER keeps characters/symbols; empty-reference denominator is one. Whole-image CER does not erase script/status-icon inventions. Public text-input metrics exclude the empty-reference controls; the aggregate public_scenes mean mixes positive CER with empty-reference edit counts and is NOT an accuracy percentage. Non-Latin-letter counts exclude digits; indic-glyph counts also catch invented Kannada/Devanagari digits/marks.

```json
{
  "schema": 1,
  "run": 41830346993,
  "source": "cba7533f2a1a58fd90fed1a54d36a3bd68ab960d",
  "production_changed": false,
  "model_changed": false,
  "physical_acceptance": false,
  "final_acceptance": false,
  "paired_inputs": 72,
  "font_measurements": 144,
  "public_scene_inputs": 5,
  "candidate_better": 5,
  "candidate_worse": 7,
  "candidate_equal": 60,
  "baseline": {
    "count": 72,
    "exact": 66,
    "mean_cer": 0.012668350168350169,
    "median_ms": 211.5,
    "max_ms": 854
  },
  "candidate4x": {
    "count": 72,
    "exact": 63,
    "mean_cer": 0.021938131313131316,
    "median_ms": 273.0,
    "max_ms": 870
  },
  "public_scenes": {
    "count": 5,
    "exact": 1,
    "mean_cer": 115.0145053041784,
    "median_ms": 1442,
    "max_ms": 3955
  },
  "public_scene_non_latin_letter_outputs": 2,
  "negative_controls": 2,
  "negative_false_positives": 2,
  "public_text_inputs": {
    "count": 3,
    "exact": 1,
    "mean_cer": 0.02417550696398932,
    "median_ms": 1294,
    "max_ms": 1442
  },
  "public_scene_indic_glyph_outputs": 3
}
```

## Public inputs: every hypothesis, including failures

| Input | Reference | Actual output | CER | ms | Non-Latin letters |
|---|---|---|---:|---:|---:|
| ios-vision-1 | No SIM 1:19 PM 100% Detection Results Label: Beach, Confidence: 0.9846113 Label: Rock, Confidence: 0.9304716 Label: Sky, Confidence: 0.9121827 Label: Cliff, Confidence: 0.7825736 Label: Sand, Confidence: 0.75200534 OK | No SIM &gt;                   1:19 PM                    100% 853)<br><br>Detection Results<br><br>Label: Beach, Confidence: 0.9846113<br>Label: Rock, Confidence: 0.9304716<br>Label: Sky, Confidence: 0.9121827<br>Label: Cliff, Confidence: 0.7825736<br><br>Label: Sand, Confidence: 0.75200534<br><br>OK | 0.032258 | 1442 | 0 |
| ios-vision-2 | ODT, custom, single, labeling | ODT, custom, single, labeling | 0.000000 | 243 | 0 |
| ios-vision-3 | No SIM 1:18 PM 100% Detect Next image Detection Results Frame: (444.0, 104.0, 1125.0, 1225.0) Label 0: Ardea cinerea (Grey Heron), 0.91796875, 567 OK | No SIM =                     1:18 PM                      100% ೫2)<br><br>Detect Next image<br><br>Detection Results<br><br>Frame: (444.0, 104.0, 1125.0, 1225.0)<br>Label 0: Ardea cinerea (Grey Heron),<br>0.91796875, 567<br><br>OK | 0.040268 | 1294 | 0 |
| heron-no-text |  | neal<br><br>4<br><br>a,<br><br>&#124;   ।<br><br>विफल जज<br><br>ಹ್‌<br><br>ಸ್ವಾ ,<br><br>ಟ್‌<br><br>~<br><br>ಹ&quot;<br><br>poe<br><br>_<br><br>=<br><br>Zz<br><br>528<br><br>ಫೆ<br><br>ಬ<br><br>ಯ<br><br>AS<br><br>ನ್ನ್ನ<br><br>ದಾ<br><br>ಫಾರ.<br><br>ಯು<br><br>ರ್ಟ<br><br>ಕಾಫ್‌<br><br>a<br><br>——<br><br>ae<br><br>&gt;<br><br>‘er<br><br>=<br><br>=<br><br>=<br><br>Ss<br><br>SS<br><br>23.<br><br>&#124;<br><br>ಯ<br><br>ತ<br><br>a<br><br>=<br><br>ಹ<br><br>ತೊ<br><br>२८<br><br>~~,<br><br>=<br><br>ಜಾವ<br><br>&#124;<br><br>ವಾ್‌<br><br>ಇತು ಎಂ<br><br>ಸ<br><br>27-25<br><br>ns<br><br>ಆ<br><br>ಆ<br><br>—<br><br>on<br><br>ಹಾಗಾ ರಾಗ<br><br>se<br><br>ಜತ<br><br>ತ್‌್‌<br><br>ಬ<br><br>Lge eee,<br><br>—<br><br>ತೆ<br><br>eS,<br><br>a)<br><br>Pace<br><br>ಕ್‌<br><br>=,<br><br>is<br><br>ರಪ ದಾ<br><br>==&gt;<br><br>&gt;&gt; हे.<br><br>a,<br><br>—<br><br>ಇ<br><br>ಹಾಮ್‌<br><br>od<br><br>ae<br><br>बज<br><br>Siok<br><br>So eet<br><br>ee<br><br>¥<br><br>nee.<br><br>Se<br><br>ಇಲಾ ತ್ತ<br><br>=<br><br>Se<br><br>a<br><br>at<br><br>aad<br><br>५+ ६२६४५<br><br>11<br><br>ಮ ತ<br><br>——<br><br>ie<br><br>ಯಾ<br><br>ಇ.<br><br>ತ್ಯಾ<br><br>a<br><br>aw<br><br>Ss<br><br>a en<br><br>a.<br><br>~<br><br>oe<br><br>Be<br><br>Sees | 392.000000 | 3955 | 66 |
| beach-no-text |  | ತ<br><br>ತ್‌<br><br>ಅತ<br><br>+s<br><br>e.<br><br>ms<br><br>a<br><br>ee i<br><br>ಸ<br><br>ಷು<br><br>ದಾರಾ<br><br>ಹಿಂ लि:<br><br>ಅ<br><br>oe<br><br>SEE<br><br>——<br><br>2<br><br>FLIES<br><br>sec<br><br>ae ವಾಂಟ್‌<br><br>ea<br><br>ss<br><br>ಇಸ<br><br>Sas<br><br>Fane<br><br>=<br><br>=<br><br>aN<br><br>or<br><br>ಸಕ್ಕ<br><br>ಕ<br><br>aie<br><br>ಇ.<br><br>ಗದ<br><br>ee<br><br>ರಾರಾ<br><br>Se:<br><br>he<br><br>=e<br><br>ರ್‌<br><br>ಮ್ರ<br><br>=<br><br>ಜಾ<br><br>ಜಾ<br><br>Tas<br><br>a<br><br>ಪಾರಾದ ಸ ನ ದೂ<br><br>a<br><br>aS ಕ್‌ | 183.000000 | 1934 | 36 |

## Paired authored small-font inputs: all72 pairs

| ID / condition | Reference | Baseline output | 4x output | CER base /4x | ms base /4x |
|---|---|---|---|---:|---:|
| font-0 sans-serif 8px light | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 854 / 765 |
| font-1 sans-serif 8px light | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 368 / 461 |
| font-2 sans-serif 8px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 404 / 870 |
| font-3 sans-serif 8px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 205 / 507 |
| font-4 sans-serif 8px dark | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 227 / 291 |
| font-5 sans-serif 8px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.100000 / 0.000000 | 240 / 297 |
| font-6 sans-serif 10px light | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 266 / 740 |
| font-7 sans-serif 10px light | भारत नमस्ते | भारत नमस्ते | आरत नमस्ते | 0.000000 / 0.181818 | 229 / 322 |
| font-8 sans-serif 10px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 223 / 411 |
| font-9 sans-serif 10px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 225 / 505 |
| font-10 sans-serif 10px dark | भारत नमस्ते | भारत नमस्ते | आरत नमस्ते | 0.000000 / 0.181818 | 245 / 329 |
| font-11 sans-serif 10px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 188 / 216 |
| font-12 sans-serif 12px light | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 193 / 220 |
| font-13 sans-serif 12px light | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 200 / 272 |
| font-14 sans-serif 12px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 225 / 329 |
| font-15 sans-serif 12px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 200 / 230 |
| font-16 sans-serif 12px dark | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 261 / 288 |
| font-17 sans-serif 12px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 192 / 281 |
| font-18 sans-serif 16px light | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 192 / 227 |
| font-19 sans-serif 16px light | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 205 / 240 |
| font-20 sans-serif 16px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 239 / 302 |
| font-21 sans-serif 16px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 252 / 249 |
| font-22 sans-serif 16px dark | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 307 / 293 |
| font-23 sans-serif 16px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 279 / 374 |
| font-24 serif 8px light | English meeting at 10 AM | English meeting at 10AM | English meeting at 10 AM | 0.041667 / 0.000000 | 222 / 454 |
| font-25 serif 8px light | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 200 / 511 |
| font-26 serif 8px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 259 / 267 |
| font-27 serif 8px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 347 / 358 |
| font-28 serif 8px dark | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 188 / 231 |
| font-29 serif 8px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.100000 / 0.000000 | 234 / 228 |
| font-30 serif 10px light | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 272 / 208 |
| font-31 serif 10px light | भारत नमस्ते | भारत नमस्ते | wea नमस्ते | 0.000000 / 0.363636 | 202 / 261 |
| font-32 serif 10px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 181 / 324 |
| font-33 serif 10px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 186 / 238 |
| font-34 serif 10px dark | भारत नमस्ते | भारत नमस्ते | आरत नमस्ते | 0.000000 / 0.181818 | 197 / 268 |
| font-35 serif 10px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 181 / 213 |
| font-36 serif 12px light | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 187 / 213 |
| font-37 serif 12px light | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 200 / 268 |
| font-38 serif 12px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 215 / 318 |
| font-39 serif 12px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 193 / 226 |
| font-40 serif 12px dark | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 244 / 354 |
| font-41 serif 12px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 222 / 275 |
| font-42 serif 16px light | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 194 / 224 |
| font-43 serif 16px light | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 199 / 236 |
| font-44 serif 16px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 224 / 262 |
| font-45 serif 16px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 192 / 228 |
| font-46 serif 16px dark | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 235 / 257 |
| font-47 serif 16px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 207 / 268 |
| font-48 monospace 8px light | English meeting at 10 AM | English meeting at 10 AN | English meeting at 10 AN | 0.041667 / 0.041667 | 312 / 361 |
| font-49 monospace 8px light | भारत नमस्ते | भारत ret | भारत नमस्ते | 0.545455 / 0.000000 | 210 / 337 |
| font-50 monospace 8px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 218 / 242 |
| font-51 monospace 8px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 189 / 215 |
| font-52 monospace 8px dark | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 223 / 261 |
| font-53 monospace 8px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 209 / 270 |
| font-54 monospace 10px light | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 188 / 214 |
| font-55 monospace 10px light | भारत नमस्ते | भारत नमस्ते | wea नमस्ते | 0.000000 / 0.363636 | 198 / 254 |
| font-56 monospace 10px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 177 / 313 |
| font-57 monospace 10px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 190 / 220 |
| font-58 monospace 10px dark | भारत नमस्ते | भारत नमस्ते | आरत नमस्ते | 0.000000 / 0.181818 | 202 / 263 |
| font-59 monospace 10px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 181 / 210 |
| font-60 monospace 12px light | English meeting at 10 AM | English meeting at 10 All | English meeting at 10 Al | 0.083333 / 0.041667 | 189 / 473 |
| font-61 monospace 12px light | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 196 / 275 |
| font-62 monospace 12px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 219 / 330 |
| font-63 monospace 12px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AN | 0.000000 / 0.041667 | 324 / 377 |
| font-64 monospace 12px dark | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 233 / 276 |
| font-65 monospace 12px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 210 / 275 |
| font-66 monospace 16px light | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 194 / 224 |
| font-67 monospace 16px light | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 241 / 262 |
| font-68 monospace 16px light | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 227 / 274 |
| font-69 monospace 16px dark | English meeting at 10 AM | English meeting at 10 AM | English meeting at 10 AM | 0.000000 / 0.000000 | 198 / 241 |
| font-70 monospace 16px dark | भारत नमस्ते | भारत नमस्ते | भारत नमस्ते | 0.000000 / 0.000000 | 242 / 276 |
| font-71 monospace 16px dark | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | ಕನ್ನಡ ಭಾಷೆ | 0.000000 / 0.000000 | 213 / 275 |

## Adoption

Study only. Do not adopt a4x global rewrite from these paired fixtures. Any improvement needs source-only eligibility, readable-native-script preservation, varied positive text photos, original full-corpus regression and honest latency/miss accounting. Existing OCR remains unchanged.
