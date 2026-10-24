# MP3 encoder notices

This directory vendors **LAME 3.100**, an MP3 encoder distributed under the
GNU Library General Public License, version 2 (LGPL-2.0). The complete source
and license text are in `lame/` and `COPYING.LGPL-2.0.txt`; upstream source:
<https://sourceforge.net/projects/lame/files/lame/3.100/lame-3.100.tar.gz>.
The application links it as the separate `mp3encoder` shared library, and this
module makes no modifications to LAME source.

LAME upstream notes that some jurisdictions may claim software patents; users
should obtain their own legal advice. This module uses the encoder library only
and does not link LAME's optional MPGLIB decoder.

Android configuration: optional MPGLIB decoder disabled in config.h; scalar portable encoder enabled for all ABIs. Upstream encoder algorithm sources unchanged.
