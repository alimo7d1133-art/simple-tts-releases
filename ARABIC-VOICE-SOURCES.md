# Arabic classic voice sources

## Ziad HTS

Source: https://github.com/linuxscout/festival-tts-arabic-voices (master, checked out in this work).
The upstream README explicitly identifies one Arabic HMM/HTS voice ara_norm_ziad_hts, trained on Nawar Arabic Speech Corpus. Runtime model: voices/ara_norm_ziad_hts/hts/ara_norm_ziad_hts.htsvoice. Header: HTS_VOICE_VERSION 1.0, sample rate 48000, frame period240, MCP/LF0/LPF streams. Root LICENSE is GPL3; there is no separately overriding license in model folder or release ZIP. The model and all upstream scripts/notices are included with this source distribution. Authors.md credits Taha Zerrouki, Oussama Heminna and Karim Hemina. Scheme templates have additional preserved permissive upstream copyright notices.

The original corpus owner explicitly links this exact Festival repository as the corpus demo and thanks its authors: https://en.arabicspeechcorpus.com/ . This is stronger provenance than merely GitHub's automatic license label.

## ASC Flite

Source: https://github.com/asrajeh/arabic-tts . Root LICENSE is MIT, Copyright (c) 2019 Abdullah Alrajeh. The repository includes the actual 4534171-byte kacst_ar_asc.flitevox and a Festival Clustergen archive. These are two formats of one ASC model, not two recorded speakers. Runtime language tag is cmu_grapheme_lang. Scripts install_asc_voice.sh and tts.sh demonstrate local Flite/Festival usage.

SASSC is referenced in tts.sh/build_sassc_voice.sh, but no built SASSC Flitevox/Festvox voice is included in the checked tree. It depends on external training material and is not shipped as a pretend selectable voice.

## Corpus attribution and distribution basis

Arabic Speech Corpus by Nawar Halabi, University of Southampton: https://en.arabicspeechcorpus.com/ . The owner states CC BY 4.0 and allows the additional18-minute corpus in applications. https://creativecommons.org/licenses/by/4.0/ ; full legal text: https://creativecommons.org/licenses/by/4.0/legalcode.txt . CC BY4 allows reproduction and adapted materials with attribution, license link, modification notices and no additional restrictions. It is not a blanket license for unrelated publicity/privacy rights or an endorsement.

The application source is AGPL-3.0-or-later and is distributed with the APK. This avoids the closed-source integration issue raised by GPL3 model/code. Keep each component's own license; do not relabel corpus rights as AGPL or MIT. Public redistribution is based on these published licenses and provenance, not a claim of a separate legal clearance or a warranty.

ASC and Ziad use different synthesis methods/models trained on the same corpus; four Arabic menu options including existing MBROLA AR1/AR2 do not mean four independently recorded speakers.

## Runtime

Festival and Edinburgh Speech Tools permissive licenses: https://www.cstr.ed.ac.uk/projects/festival/ . Festival uses Scheme front-end plus its embedded hts_engine. Upstream instructions use festival/text2wave, not a generic Flitevox loader for the Ziad model. Android requires native cross-compilation and local Scheme/model data; no web service is required.

Upstream LTS rules expect shadda immediately after a consonant, before its vowel. Canonical Unicode ordering can put the vowel first and trigger “no rule matches”. FestivalPlugin.normalizeDiacritics reorders this mark sequence only for synthesis; the visible text and trained model are not changed. This is tested with manual and Rawi-generated vocalization.

## Exclusions

No eSpeak, RHVoice, IBM TTS or DECtalk is added. No placeholder voice entry or MP3 recording is used as a generative voice model. Existing MBROLA AR1/AR2 redistribution restrictions are unchanged (gratis redistribution, notices; sale requires appropriate permission).

## متابعة البحث عن خيارات إضافية

لم يُتحقق في المصادر المفحوصة من نموذج عربي قديم إضافي يجمع ملف runtime جاهزًا وحق إعادة توزيع واضحًا. لا يُعد ذلك إثباتًا لعدم وجود أي نموذج آخر في العالم.

- [asrajeh/arabic-tts](https://github.com/asrajeh/arabic-tts): ASC الموزع موجود أصلًا. تنسيقا Flite وFestvox لهذا النموذج لا يجعلان منه متحدثين مستقلين. توجد أمثلة وسكربتات SASSC، لكن سكربت التثبيت يتوقع festvox_kacst_ar_sassc_cg.tar.gz غير الموجود ضمن الملفات الموزعة، والأرشيف الموجود يحتوي kacst_ar_asc. سكربت SASSC يحتاج corpus محليًا وبناء نموذج جديد؛ لذلك لم يُضف خيار غير عامل.
- [marytts-lang-ar](https://github.com/HaraldBerthelsen/marytts-lang-ar): دعم لغة/phonemizer تحت Unlicense، وليس نموذج صوت. README يحذر بأنه غير عامل.
- [بحث MARY العربي المنشور](https://airccse.org/journal/jcsit/0810ijcsit02.pdf): وصف أكاديمي لا يرفق قاعدة صوت جديدة قابلة للتنزيل وترخيصها. استخدام MBROLA في البحث ليس مانعًا بحد ذاته؛ المانع غياب نموذج جديد وحقوقه، ولا نستخدم eSpeak.
- [نموذج Festival البحثي لعام 2004](https://www.researchgate.net/publication/253194329_A_Prototype_of_an_Arabic_Diphone_Speech_Synthesizer_in_Festival): لا تتوفر حزمة نموذج وترخيص قابلان للتحقق في السجل المفتوح.

لهذا بقيت الخيارات العربية أربعة: MBROLA AR1 وAR2، وFestival Ziad HTS، وFlite ASC. العدد أربعة خيارات تشغيل، لا أربعة متحدثين مسجلين مختلفين؛ النموذجان الأخيران مبنيان على Arabic Speech Corpus نفسه. لم يُغيّر الجنس أو الطبقة لتوليد أسماء أصوات إضافية.
