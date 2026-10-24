# صوتا MBROLA في ClaritySynth

يقتصر الطلب على MBROLA؛ لم تُضف أصوات ClaritySynth العصبية أو الفورمانت. لا يزيل ذلك المحركات الموجودة أصلًا في Old TTS 1.3.1.

## US1 وs1

المعرّف الموجود في فهرس ClaritySynth هو `us1`، ولم يُعثر على ملف أو معرّف مستقل `s1`، لذلك لا نفترض أنه المقصود دون تأكيد. ملف US1 مطابق تمامًا لملف Old TTS الموجود: 7,238,094 بايت، SHA-256:

`9f1cd90de6334f43cb4f7348cacd0806fb414b0fbc762492bde7000b9e192a9a`

لم تُضف نسخة مكررة أو اسم مستعار. ترخيص US1 الأصلي محفوظ؛ يجيز التوزيع المجاني بشروطه، ولا يجيز تضمينه في منتج يُباع دون إذن المالك.

## سعيدة SAEDA

الملف الفعلي اسمه `saeda`، ومساره داخل ClaritySynth:

`addon/synthDrivers/claritysynthmbrola/mbrola/espeak/espeak-ng-data/mbrola/saeda`

حجمه 11,233,449 بايت، SHA-256:

`3163aa13c7385907c0fe2472dfcca4c7e19702692df8465d0471d1a47e7334c`

إنها قاعدة MBROLA مختلفة عن AR1 وAR2، وليست نموذجًا عصبيًا. ينسبها [README](https://github.com/riadassoum/ClaritySynth/blob/main/README.md) إلى BrightSpeech، ويصرح بأن كل مكوّن مضمّن يحتفظ بترخيصه الخاص.

أُعيد فحص [LICENSE](https://github.com/riadassoum/ClaritySynth/blob/main/LICENSE): يجيز الكود تحت GPL-2.0-or-later، لكنه يستثني إعادة ترخيص مكونات الطرف الثالث صراحة: **Bundled third-party components retain their own licenses**. صفحة `addon/doc/en/readme.html` التي يحيل إليها لا تقدم ترخيصًا خاصًا بسعيدة. لم يُعثر على إذن أصلي يغطي إعادة توزيع قاعدة BrightSpeech داخل تطبيق عام.

لذلك لم تُضمّن سعيدة في APK أو المصدر العام. هذا نقص في دليل الإذن، لا حكم بأن كل استخدام ممنوع. وجود الملف في مستودع مفتوح لا يثبت أن صاحب القاعدة أجاز إعادة ترخيصه. يمكن إعادة النظر عند توفير ترخيص أصلي أو إذن إعادة توزيع يغطيها. لم تُنتج عينة منها، ولم يُنقل غلاف eSpeak أو phonemizer التابع له إلى التطبيق.

تم فحص الفهرس عند commit `a652065ca770a9414f341323b9aa616bfbaebd56`.

## الأرشيف المقدم لاحقًا باسم سعيدة

اقتصر فحصه على الترويسات والسلاسل وreadelf، ولم يُشغّل. يشير libttsacattsandroidextended.so ورموز BABILE ومنها BABILE_checkVoiceLicense إلى عائلة Acapela/Acatt Android. بيانات ar_sa* ذات ترويسات DICA/PST/BNXF، وليست قاعدة MBROLA ذات ترويسة MBROLA2.x. اسم ar_sa_salma.pst يرجح Salma، ولا يثبت Saeda أو تسجيلًا سعوديًا بعينه. لم يتضمن الأرشيف ترخيصًا ظاهرًا؛ [صفحة SDK الرسمية](https://www.acapela-group.com/solutions/acapela-tts-for-android/) تفرق بين ترخيص التطوير وترخيص التوزيع التجاري ذي الإتاوات. وافق الطلب على استبعاد الأرشيف. لم تُنسخ أي مكتبة أو قاعدة منه إلى APK أو المصدر العام أو عينات الاستماع.
