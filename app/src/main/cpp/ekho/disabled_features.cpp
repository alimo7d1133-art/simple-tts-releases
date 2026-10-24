/* Simple TTS Cantonese-only port, AGPL-3.0-or-later.
 * The optional English synthesizers and network services are deliberately absent.
 * These non-advertised API paths fail rather than substituting another engine.
 * Upstream Ekho core remains GPL-2.0-or-later, selected under GPLv3 when linked.
 */
#include "ekho.h"
#include "ekho_impl.h"
#include <stdexcept>
namespace ekho {
SynthCallback* Ekho::synthCallback=nullptr;
void EkhoImpl::initEnglish(){gEkho=this;}
void EkhoImpl::setEnglishSpeed(int){}
const char* EkhoImpl::getEnglishPcm(std::string,int& size){size=0;throw std::runtime_error("English synthesis is not included in this Cantonese-only port");}
int EkhoImpl::startServer(int){return -1;}
int EkhoImpl::request(std::string,int,Command,std::string,std::string){return -1;}
}
