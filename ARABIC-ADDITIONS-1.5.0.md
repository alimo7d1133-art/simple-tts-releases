# الأصوات العربية القديمة: نتيجة البحث

لم يُعثر في المصادر المفحوصة على **نموذج عربي كلاسيكي إضافي جديد** يجمع بين ملف صوت قابل للتشغيل محليًا، واختلاف حقيقي عن الموجود، وترخيص واضح لإعادة التوزيع. لا يعني هذا استحالة وجود نموذج آخر؛ لكنه يمنع إضافة أسماء أو وعود غير مثبتة.

تبقى الخيارات العربية الأربعة: MBROLA AR1 وAR2، وFestival Ziad HTS، وFlite ASC. زياد وASC مدرّبان على Arabic Speech Corpus نفسه، فلا يُعدّان متحدثين مسجلين مختلفين لمجرد اختلاف المحرك.

## لماذا لم تُضف الأسماء المتداولة؟

- [Nawar / Festival](https://github.com/linuxscout/festival-tts-arabic-voices): النموذج المنشور هو `ara_norm_ziad_hts` الموجود فعلًا؛ وحاوية [Nawar Docker](https://github.com/nawarhalabi/festival-tts-arabic-voices-docker) تغلّف الصوت نفسه.
- [Flite / SASSC](https://github.com/asrajeh/arabic-tts): الملفات المنشورة تتضمن نموذج ASC الموجود، بينما [سكربت SASSC](https://raw.githubusercontent.com/asrajeh/arabic-tts/master/install_sassc_voice.sh) يحتاج أرشيفًا خارجيًا غير منشور ضمن المستودع. سكربت التدريب أو corpus وحده ليس حزمة صوت جاهزة ولا تصريحًا بنشرها.
- [MARYTTS العربي](https://github.com/HaraldBerthelsen/marytts-lang-ar): يصرّح المشروع بأنه لا يعمل حاليًا، ولا يقدّم حزمة صوت عربية جاهزة لإضافتها. نشر ورقة بحثية أو عينات WAV لا يمنح تلقائيًا حقوق توزيع نموذج.
- [MBROLA](https://github.com/numediart/MBROLA-voices): القاعدتان العربيتان المتحققتان AR1 وAR2 موجودتان بالفعل. لا تُعدّ قواعد لغات أخرى أصواتًا عربية. تظل [شروط البيانات](https://github.com/numediart/MBROLA-voices/blob/master/LICENSE.md) محفوظة: التوزيع المجاني مع الإشعارات والاستخدام مع MBROLA؛ البيع أو الإدماج بمنتج مباع يحتاج إذن مالك القاعدة.

يمكن من حيث المبدأ تدريب نموذج جديد من [Arabic Speech Corpus المرخّص CC BY 4.0](https://en.arabicspeechcorpus.com/)، لكن ذلك مشروع تدريب جديد وليس إضافة صوت قديم جاهز.

## الاستبعادات

لم تُضف eSpeak أو RHVoice أو IBM TTS أو DECtalk. لم يُستخدم أرشيف Saeeda/Acapela ولا صوت ملكي بلا رخصة توزيع واضحة، ولا نماذج ClaritySynth العصبية أو formant. لم تُسمّ تنويعات الطبقة أو نسخ النموذج الواحد أصواتًا جديدة.

أُضيف Open JTalk بصوت Nitech الياباني الحقيقي. تخص هذه الإضافة اليابانية فقط ولا تغيّر النتيجة العربية؛ راجع `TEST-RESULTS.txt` لما اختُبر وحدوده.
